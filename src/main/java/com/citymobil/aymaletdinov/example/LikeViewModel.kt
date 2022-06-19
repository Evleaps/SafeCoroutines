package com.citymobil.aymaletdinov.example

import kotlinx.coroutines.*
import launchBuilder
import launchIO
import launchMain
import withIO
import java.lang.AssertionError

class LikeViewModel : CoroutineScope {

    // without clear because just for test
    private val viewModelContext: Job = SupervisorJob()
    override val coroutineContext = viewModelContext + Dispatchers.Main

    private enum class Status { SUCCESS, FAIL, NOT_GIVEN }
    private var status = Status.NOT_GIVEN

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

    fun runLaunchBuilder(isNeedError: Boolean): Job {
        return launchBuilder()
            .launchOn(Dispatchers.IO)
            .errorOn(Dispatchers.Main)
            .onError(this::onError)
            .launch {
                if (isNeedError) throw AssertionError()
                else onSuccess()
            }
    }

    // VisibleForTesting
    fun onSuccess() {}
    fun onError(t: Throwable) {
        println("err $t")
    }

    private var isFlagEnabled = false

    suspend fun checkThatFlagTrue(): Boolean {
        withIO {
            delay(5_000)
            isFlagEnabled = true
        }
        return isFlagEnabled
    }
}