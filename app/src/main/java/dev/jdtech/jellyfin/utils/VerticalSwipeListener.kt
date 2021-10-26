package dev.jdtech.jellyfin.utils

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import kotlin.math.absoluteValue

/**
 * Never consumes touch events. Can report when swipe starts with onTouch.
 */
internal class VerticalSwipeListener(
    private val onUp: () -> Unit,
    private val onDown: () -> Unit,
    private val onTouch: () -> Unit = {},
    private val threshold: Int = 150,
): View.OnTouchListener {

    private var verticalStart = 0f
    private var distance = 0f
    private var dispatchTouchEvent = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when(event.action) {
            ACTION_DOWN -> verticalStart = event.y
            ACTION_UP -> dispatchTouchEvent = true
            ACTION_MOVE -> event.reportSwipe()
            else -> Unit
        }

        return false
    }

    private fun MotionEvent.reportSwipe() {
        reportFirstTouch()

        distance = verticalStart - y
        if (distance.tooShort()) return

        verticalStart = y

        if (distance > 0) {
            onUp()
        } else {
            onDown()
        }
    }

    private fun reportFirstTouch() {
        if (dispatchTouchEvent) {
            onTouch()
            dispatchTouchEvent = false
        }
    }

    private fun Float.tooShort() = absoluteValue < threshold
}