package com.citymobil.aymaletdinov

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import launchSafe

class LaunchSafeBuilder(private val scope: CoroutineScope) {

    private var onError: suspend (Throwable) -> Unit = {}
    private var dispatcher: CoroutineDispatcher = Dispatchers.Main
    private var errorDispatcher: CoroutineDispatcher = Dispatchers.Main

    fun onError( onError: suspend (Throwable) -> Unit) = apply { this.onError = onError }
    fun launchOn(dispatcher: CoroutineDispatcher) = apply { this.dispatcher = dispatcher }
    fun errorOn(dispatcher: CoroutineDispatcher) = apply { this.errorDispatcher = dispatcher }

    fun launch(safeAction: suspend () -> Unit): Job {
        return scope.launchSafe(
            safeAction = safeAction,
            onError = onError,
            dispatcher = dispatcher,
            errorDispatcher = errorDispatcher
        )
    }
}
