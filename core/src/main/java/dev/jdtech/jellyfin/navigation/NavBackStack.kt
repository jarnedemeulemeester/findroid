package dev.jdtech.jellyfin.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Navigates to [route]: if an equal key is already present on the back stack, the back stack is
 * truncated to it (everything above it is popped); otherwise, [route] is pushed.
 */
fun <T : NavKey> NavBackStack<T>.navigateOrReuse(route: T) {
    val index = indexOf(route)
    if (index >= 0) {
        truncateTo(index + 1)
    } else {
        add(route)
    }
}

/**
 * Removes keys from the top of the back stack until only [size] keys remain. Does nothing if the
 * back stack already contains [size] keys or fewer.
 */
fun <T : NavKey> NavBackStack<T>.truncateTo(size: Int) {
    while (this.size > size) {
        removeAt(lastIndex)
    }
}
