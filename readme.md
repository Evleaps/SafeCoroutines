# SafeCoroutines

[Readme on russian | Readme на русском](readme_ru.md)

## Introduction

The project uses the ext functions of CoroutinesUtils.kt. In this file, analogues of the functions launch and
withContext

- launch(Dispatchers.IO) -> launchIO
- launch(Dispatchers.Main) -> launchMain
- withContext(Dispatchers.IO) -> withIO
- withContext(Dispatchers.Main) -> withMain

Calls to the standard library are recommended to be prohibited by custom rules Detekt:

- [NeedToUseCustomWithContextRule](src/main/java/detekt/NeedToUseCustomWithContextRule.kt)
- [NeedToUseCustomLaunchRule](src/main/java/detekt/NeedToUseCustomLaunchRule.kt)

## Why is it necessary to use these extensions?

Because the standard approach does not oblige us to handle errors, moreover, error handling in
Kotlin is difficult because of its non-obviousness. The try-catch block won't catch the error in IO and the application doesn't care
will fall, and the use of CoroutineExceptionHandler on the whole class leads to the fact that an error in one
curutin can break all other coroutines in the scope. To avoid this, you must either create
different scopes, or different instances of CoroutineExceptionHandler and this large number of approaches do not
guarantees a unified approach to their processing

## Problems

1. The contract does not oblige to process the fact of an error in the coroutine, which means that someone can neglect processing in vain
2. try-catch does not catch all errors and this leads to crashes
3. It is necessary to pass the parameters of the stream and the error handler to the constructor, this complicates readability
   code
4. There is no single approach to organizing asynchronous work
5. The error will come in a random thread, therefore, UI changes will lead to a crash
6. Using a single CoroutineExceptionHandler for the entire class or in MainClass will abort operations in
   this scope

## Solution

1. We use our own extensions which internally create a coroutineExceptionHandler - **therefore, any coroutine must handle the fact of an error, even if it does not need to be reacted to**
2. Extensions are similar to subscribeBy - **Familiar syntax from RxJava is clearer**
3. No need to pass anything to the constructor - **cleaner and simpler code**
4. A unified approach - **minimization of errors due to incorrect processing or its absence**
5. In onError, the error will always come in the Main thread - ** so you can update the UI **
6. Each error is isolated - **means we will not interrupt an important operation running in parallel in
   same scope**

## Samples

**Before | Old**

```kotlin
class MyViewModel {

    private val myExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        withContext(Dispatchers.Main) { // because you cannot update the UI in the IO thread
            view.showError()
        }
    }

    fun foo() {
        launch(Dispatchers.IO, myExceptionHandler) {
            val campaign = marketingCampaignsInteractor
                .getMarketingCampaign(MarketingCampaignType.FAVORITES_CASHBACK.codeName)
                .awaitSingleOrNull()

            if (campaign is FavoritesCashbackCampaign) {
                val percent = loyaltyProgramInteractor.getFavoritesCashbackPercentage()
                withContext(Dispatchers.Main) { // because you cannot update the UI in the IO thread
                    view.setLikeCashbackPercent(percent)
                }
            }
        }
    }
}
```

**After | new**

```kotlin
class MyViewModel {

    fun foo() {
        launchIO(
            safeAction = {
                val campaign = marketingCampaignsInteractor
                    .getMarketingCampaign(MarketingCampaignType.FAVORITES_CASHBACK.codeName)
                    .awaitSingleOrNull()

                if (campaign is FavoritesCashbackCampaign) {
                    val percent = loyaltyProgramInteractor.getFavoritesCashbackPercentage()
                    withMain { // because you cannot update the UI in the IO thread
                        view.setLikeCashbackPercent(percent)
                    }
                }
            },
            onError = {
                // не нужно делать withMain так как ошибка всегда в Main потоке
                view.showError()
            }
        )
    }
}

```

## Detekt custom rules

In order for all team members to use a unified approach to creating coroutines and error handling, 
I strongly advise you to include these rules in the list of Detekt custom rules. 

If your project have not Detekt: 
- Detekt is a static analysis tool that can automate your routine work as a code reviewer.
- [GitHub](https://github.com/detekt/detekt)
- [Site](https://detekt.dev/)
- [How implement custom rules (Russian, please, use google translate)](https://habr.com/en/company/citymobil/blog/565402/)

Just insert the 2 rules you saw above in `DetektCustomRuleSetProvider` and the rules will work for you.

## Testing

If you need to test coroutines with stream redefinition, as we did for Rx, then there is no need for coroutines in such
a file, because we can use [kotlinx-coroutines-test](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/) which overrides threads for testing.

I mainly based on information from [this article on Medium](https://medium.com/swlh/unit-testing-with-kotlin-coroutines-the-android-way-19289838d257)
.

## How it works?

**PS: Firstly need to add dependency**

```groovy
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0'
```

**Test**

```kotlin
@Before
fun setUp() {
    Dispatchers.setMain(StandardTestDispatcher())
}

@After
fun tearDown() {
    Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
}

// тесты

```

## Test samples

**ViewModel** or something like that

```kotlin
class LikeViewModel : CoroutineScope {

    // without clear because just for test
    private val viewModelContext: Job = SupervisorJob()
    override val coroutineContext = viewModelContext + Dispatchers.Main

    fun runSmthInIO(): Job {
        return launchIO(
            safeAction = {
                delay(3_000L)
                onSuccess()
            },
            onError = ::onError
        )
    }

    fun runSmthInMain(): Job {
        return launchMain(
            safeAction = {
                delay(4_000L)
                onSuccess()
            },
            onError = ::onError
        )
    }

    @VisibleForTesting
    fun onSuccess() {}
    private fun onError(t: Throwable) {}

    private var isFlagEnabled = false

    suspend fun checkThatFlagTrue(): Boolean {
        withIO {
            delay(5_000)
            isFlagEnabled = true
        }
        return isFlagEnabled
    }
}
```

**Tests**

```kotlin

@OptIn(ExperimentalCoroutinesApi::class)
class LikeViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
    }

    @Test
    fun `WHEN call fun in IO THEN onSuccess called`() = runBlocking {
        val viewModel: LikeViewModel = spy(LikeViewModel())

        val executionTime = measureTimeMillis {
            viewModel.runSmthInIO().join()
            verify(viewModel, times(1)).onSuccess()
        }

        print("runSmthInIO: Execution Time: $executionTime")
    }

    @Test
    fun `WHEN call fun in Main THEN onSuccess called`() = runTest {
        val viewModel: LikeViewModel = spy(LikeViewModel())

        val executionTime = measureTimeMillis {
            viewModel.runSmthInMain().join()
            verify(viewModel, times(1)).onSuccess()
        }

        print("Execution Time: $executionTime")
    }

    @Test
    fun `WHEN checkThatFlagTrue called THEN it must return true`() = runBlocking {
        val mainViewModel = LikeViewModel()

        val executionTime = measureTimeMillis {
            val isTrue = mainViewModel.checkThatFlagTrue()
            assertTrue(isTrue)
        }

        print("Execution Time: $executionTime")
    }
}
```