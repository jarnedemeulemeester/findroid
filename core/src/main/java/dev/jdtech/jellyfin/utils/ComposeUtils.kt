package dev.jdtech.jellyfin.utils

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> ObserveAsEvents(flow: Flow<T>, onEvent: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lifecycleOwner.lifecycle) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect(onEvent)
        }
    }
}

private val DPadEventsKeyCodes = listOf(
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT,
    KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT,
    KeyEvent.KEYCODE_DPAD_UP,
    KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP,
    KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN,
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_NUMPAD_ENTER,
)

/**
 * Handles horizontal (Left & Right) D-Pad Keys and consumes the event(s) so that the focus doesn't
 * accidentally move to another element.
 * */
fun Modifier.handleDPadKeyEvents(
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
) = onPreviewKeyEvent {
    fun onActionUp(block: () -> Unit) {
        if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) block()
    }

    if (DPadEventsKeyCodes.contains(it.nativeKeyEvent.keyCode)) {
        when (it.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                onLeft?.apply {
                    onActionUp(::invoke)
                    return@onPreviewKeyEvent true
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                onRight?.apply {
                    onActionUp(::invoke)
                    return@onPreviewKeyEvent true
                }
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                onEnter?.apply {
                    onActionUp(::invoke)
                    return@onPreviewKeyEvent true
                }
            }
        }
    }
    false
}

/**
 * Handles all D-Pad Keys
 * */
fun Modifier.handleDPadKeyEvents(
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
) = onKeyEvent {
    if (DPadEventsKeyCodes.contains(it.nativeKeyEvent.keyCode) && it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
        when (it.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                onLeft?.invoke().also { return@onKeyEvent true }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                onRight?.invoke().also { return@onKeyEvent true }
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP -> {
                onUp?.invoke().also { return@onKeyEvent true }
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN -> {
                onDown?.invoke().also { return@onKeyEvent true }
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                onEnter?.invoke().also { return@onKeyEvent true }
            }
        }
    }
    false
}
