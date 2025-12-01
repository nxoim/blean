package com.nxoim.blean.ui.fullscreenMedia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class UserIdlingDetectionState(
    private val idleTimeout: Duration,
    private val coroutineScope: CoroutineScope,
    private val enabled: () -> Boolean
) {
    var isIdle by mutableStateOf(false)
        private set

    private var delayJob = createSetToTrueWithDelayJob()

    val detectionModifier = Modifier.pointerInput(Unit) {
        while (true) {
            awaitPointerEventScope { awaitPointerEvent(pass = PointerEventPass.Initial) }

            delayJob.cancelAndJoin()
            isIdle = false

            delayJob = createSetToTrueWithDelayJob()
        }
    }

    fun switch() {
        isIdle = !isIdle

        coroutineScope.launch {
            delayJob.cancelAndJoin()
            delayJob = createSetToTrueWithDelayJob()
        }
    }

    private fun createSetToTrueWithDelayJob() = coroutineScope.launch {
        delay(idleTimeout)
        if (isActive && enabled()) isIdle = true
    }
}

@Composable
fun rememberUserIdlingDetectionState(
    idleTimeout: Duration = 3.seconds,
    enabled: () -> Boolean = { true }
): UserIdlingDetectionState {
    val coroutineScope = rememberCoroutineScope()

    return remember {
        UserIdlingDetectionState(idleTimeout, coroutineScope, enabled)
    }
}

fun Modifier.userIdleDetection(
    state: UserIdlingDetectionState
) = then(state.detectionModifier)