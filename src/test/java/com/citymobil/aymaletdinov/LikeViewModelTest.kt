package com.citymobil.aymaletdinov

import com.citymobil.aymaletdinov.example.LikeViewModel
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import launchIO
import launchMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * If you don't want to use ExperimentalApi use launchSafe.
 * @see {SafeCoroutinesExt#launchSafe}
 *
 * Note: If you are using IntelliJ IDEA you must change the IntelliJ settings:
 * Preferences -> Build, Execution, Deployment -> Gradle -> 'Run Tests Using' from 'Gradle (default)' to 'IntelliJ IDEA'.
 */
@ExperimentalCoroutinesApi
internal class LikeViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
    }

    @Test
    fun `WHEN call fun in IO THEN onSuccess called`() = runTest {
        val viewModel: LikeViewModel = spyk(LikeViewModel())

        val executionTime = measureTimeMillis {
            viewModel.runSmthInIO().join()
            verify { viewModel.onSuccess() }
        }

        print("runSmthInIO: Execution Time: $executionTime")
    }

    @Test
    fun `WHEN fill repository in different coroutines with IO THEN repository contain all`() = runTest {
        val repository = mutableListOf<String>()

        launchIO(
            safeAction = {
                repository.add("Joshua")
            },
            onError = { /* nothing */ }
        )

        launchIO(
            safeAction = {
                repository.add("Roman")
            },
            onError = { /* nothing */ }
        )

        assertEquals(listOf("Joshua", "Roman"), repository)
    }

    @Test
    fun `WHEN fill repository in different coroutines with Main THEN repository contain all`() = runTest {
        val repository = mutableListOf<String>()

        launchMain(
            safeAction = {
                repository.add("Joshua")
            },
            onError = { /* nothing */ }
        )

        launchMain(
            safeAction = {
                repository.add("Roman")
            },
            onError = { /* nothing */ }
        )

        advanceUntilIdle() // https://developer.android.com/kotlin/coroutines/test
        assertEquals(listOf("Joshua", "Roman"), repository)
    }

    @Test
    fun `WHEN call fun in Main THEN onSuccess called`() = runTest {
        val viewModel: LikeViewModel = spyk(LikeViewModel())

        val executionTime = measureTimeMillis {
            viewModel.runSmthInMain().join()
            verify { viewModel.onSuccess() }
        }

        print("Execution Time: $executionTime")
    }

    @Test
    fun `WHEN call launchBuilder in IO THEN onSuccess called`() = runTest {
        val viewModel: LikeViewModel = spyk(LikeViewModel())

        val executionTime = measureTimeMillis {
            viewModel.runLaunchBuilder(isNeedError = false).join()
            verify { viewModel.onSuccess() }
        }

        print("Execution Time: $executionTime")
    }

    @Test
    fun `WHEN call launchBuilder in IO with error THEN onError called`() = runTest {
        val viewModel: LikeViewModel = spyk<LikeViewModel>()

        val executionTime = measureTimeMillis {
            viewModel.runLaunchBuilder(isNeedError = true).join()
            verify { viewModel.onError(any()) }
        }

        print("Execution Time: $executionTime")
    }


    @Test
    fun `WHEN checkThatFlagTrue called THEN it must return true`() = runTest {
        val mainViewModel = LikeViewModel()

        val executionTime = measureTimeMillis {
            val isTrue = mainViewModel.checkThatFlagTrue()
            assert(isTrue)
        }

        print("Execution Time: $executionTime")
    }
}
