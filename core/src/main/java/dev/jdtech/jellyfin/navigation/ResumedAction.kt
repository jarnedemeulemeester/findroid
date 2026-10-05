package dev.jdtech.jellyfin.navigation

import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Returns an action that only runs [calculation] while the nearest entry's lifecycle is in the
 * `RESUMED` state. This mirrors the former Navigation 2 `safeNavigate` guard, preventing navigation
 * while a transition is still in progress.
 *
 * This is the argument-taking companion to [androidx.lifecycle.compose.dropUnlessResumed], which
 * only accepts a `() -> Unit`. Call it from within a navigation entry so it observes that entry's
 * [Lifecycle] rather than the host/`NavDisplay` lifecycle.
 */
@CheckResult
@Composable
fun <T> resumedAction(calculation: @DisallowComposableCalls (T) -> Unit): (T) -> Unit {
    val lifecycleOwner = LocalLifecycleOwner.current
    return { argument ->
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
            calculation(argument)
        }
    }
}

@CheckResult
@Composable
fun <T, U> resumedAction(calculation: @DisallowComposableCalls (T, U) -> Unit): (T, U) -> Unit {
    val lifecycleOwner = LocalLifecycleOwner.current
    return { t, u ->
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
            calculation(t, u)
        }
    }
}

@CheckResult
@Composable
fun <T, U, V> resumedAction(
    calculation: @DisallowComposableCalls (T, U, V) -> Unit
): (T, U, V) -> Unit {
    val lifecycleOwner = LocalLifecycleOwner.current
    return { t, u, v ->
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
            calculation(t, u, v)
        }
    }
}
