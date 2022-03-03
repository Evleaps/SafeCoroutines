package com.citymobil.aymaletdinov.example

import kotlinx.coroutines.*
import launchIO
import launchMain
import withIO

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

    // VisibleForTesting
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