package com.citymobil.aymaletdinov

import com.citymobil.aymaletdinov.example.LikeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito.*
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
internal class LikeViewModelTest {

    val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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