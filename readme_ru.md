# Coroutines

## Введение

В проекте используются ext функции CoroutinesUtils.kt. В этом файле аналоги функций launch и
withContext

- launch(Dispatchers.IO) -> launchIO
- launch(Dispatchers.Main) -> launchMain
- withContext(Dispatchers.IO) -> withIO
- withContext(Dispatchers.Main) -> withMain

Вызовы стандартной библиотеки рекомендую запретить кастомным правилами Detekt:

- NeedToUseCustomWithContextRule
- NeedToUseCustomLaunchRule

## Установка

[![](https://jitpack.io/v/Evleaps/SafeCoroutines.svg)](https://jitpack.io/#Evleaps/SafeCoroutines)

Just add dependency

```groovy
    allprojects {
        repositories {
            // ...
            maven { url 'https://jitpack.io' }
        }
    }

    dependencies {
        implementation 'com.github.Evleaps:SafeCoroutines:'
    }
```

## Почему необходимо использовать свои ext?

Потому что стандартный подход не обязывает нас обрабатывать ошибки, более того, обработка ошибок в
котлине сложна из-за своей неочевидности. Блок try-catch не перехватит ошибку в IO и приложение все равно
упадет, а использование CoroutineExceptionHandler на весь класс приводит к тому, что ошибка в одной
курутине может поломать все остальные корутины в скоупе. Чтобы этого избежать нужно либо создавать
разные скоупы, либо разные инстансы CoroutineExceptionHandler и это большое количество подходов не
гарантирует единого подхода к их обработке.

## Проблемы

1. Контракт не обязывает обрабатывать факт ошибки в корутине, значит кто-то может зря принебречь обработкой;
2. try-catch перехватывает не все ошибки и это приводит к крашам;
3. Нужно передавать в конструктор параметры потока и обработчика ошибок, это усложняет читабельность
   кода;
4. Нет единого подхода к организации асинхронной работы;
5. Ошибка придет в рандомном потоке, следовательно, изменения UI приведут к крашу;
6. Использование единого CoroutineExceptionHandler на весь класс или в MainClass прервет операции в
   этом скоупе;

## Решение

1. Мы используем свои экстеншены которые внутри себя создают coroutineExceptionHandler -
   **следовательно любая корутина обязана обрабатывать факт ошибки, даже если на нее реагировать не нужно**
2. Экстеншены похожи на subscribeBy - **знакомый синтаксис из RxJava понятнее**
3. Не нужно передавать ничего в конструктор - **чище и проще код**
4. Единый подход - **минимизация ошибок из за неправильной обработки или ее отсутствия**
5. В onError ошибка придет всегда в Main потоке - **значит можно обновлять UI**
6. Каждая ошибка изолирована - **значит мы не прервем важную операцию выполняющуюся параллельно в
   том же скоупе**

## Примеры

**Было**

```kotlin
class MyViewModel {

    private val myExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        withContext(Dispatchers.Main) { // так как нельзя обновлять UI в IO потоке
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
                withContext(Dispatchers.Main) { // так как нельзя обновлять UI в IO потоке
                    view.setLikeCashbackPercent(percent)
                }
            }
        }
    }
}
```

**Стало**

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
                    withMain { // так как нельзя обновлять UI в IO потоке
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

Чтобы все участники команды использовали единый подход к созданию сопрограмм и обработке ошибок,
Я настоятельно рекомендую вам включить эти правила в список пользовательских правил Detekt.

Если в вашем проекте нет Detekt:
- Detekt — это инструмент статического анализа, который может автоматизировать вашу рутинную работу на code review.
- [GitHub](https://github.com/detekt/detekt)
- [Site](https://detekt.dev/)
- [Инструкция как подключить custom rules](https://habr.com/en/company/citymobil/blog/565402/)

Просто вставьте 2 правила которые вы видели выше в `DetektCustomRuleSetProvider` и правила начнут работать для вас. 

## Тестирование

Если потребуется протестировать корутины с переопределением потока, так как это мы делали для Rx в
AppSchedulers, то в корутинах в подобном файле нет необходимости, так
как [есть либа](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/) выполняющая
роль переопределения потоков для тестирования.

Информацию в основном я черпал
из [этой статьи на Medium](https://medium.com/swlh/unit-testing-with-kotlin-coroutines-the-android-way-19289838d257).

## Как это работает

**PS: Сначала добавить зависимость**

```groovy
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0'
```

**В тест файле**

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

## Примеры тестирования

**Тестируемые функции**

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

**Тесты**

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

## Не хочу использовать экспериментальное API

Если вы не хотите использовать экспериментальный API из
kotlinx-coroutines-test, вы можете использовать стандартный
подход переопределения Dispatcher прокидывая в каждый вызов launch, 
используя Inject для доставки зависимостей в классы.

```kotlin
inline fun CoroutineScope.launchSafe(
   crossinline safeAction: suspend () -> Unit,
   crossinline onError: (Throwable) -> Unit,
   dispatcher: CoroutineDispatcher, // <-- you must provide some Dispatcher manually
   errorDispatcher: CoroutineDispatcher = Dispatchers.Main
): Job {
   val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
      launch(errorDispatcher) {
         onError.invoke(throwable)
      }
   }

   return this.launch(exceptionHandler + dispatcher) {
      safeAction.invoke()
   }
}
```

Подробные материалы этого метода собраны здесь:
- [Google coroutines-best-practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
- [Medium](https://towardsdev.com/how-to-inject-the-coroutines-dispatchers-into-your-testable-code-5c21d393a99a)
- [GitHub](https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test)

## Лицензия

Copyright 2022 City-mobil, LLD

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Примечание

**(En)**
All information and source code are provided AS-IS, without express or implied warranties.
Use of the source code or parts of it is at your sole discretion and risk.
Citymobil LLC takes reasonable measures to ensure the relevance of the information posted in this repository,
but it does not assume responsibility for maintaining or updating this repository or its parts outside the framework
established by the company independently and without notifying third parties.

**(Ru)**
Вся информация и исходный код предоставляются в исходном виде,
без явно выраженных или подразумеваемых гарантий. Использование исходного кода или его части осуществляются
исключительно по вашему усмотрению и на ваш риск. Компания ООО “Ситимобил” принимает разумные меры для
обеспечения актуальности информации, размещенной в данном репозитории, но она не принимает на себя
ответственности за поддержку или актуализацию данного репозитория или его частей вне рамок,
устанавливаемых компанией самостоятельно и без уведомления третьих лиц.