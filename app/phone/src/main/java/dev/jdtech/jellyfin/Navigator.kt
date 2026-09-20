package dev.jdtech.jellyfin

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/** Handles navigation events (forward and back) by updating the [NavigationState]. */
class Navigator(private val state: NavigationState) {
    /**
     * Navigates to [route]. If [route] is a top level route, the active back stack is switched to
     * its stack (state is retained). Otherwise, [route] is pushed onto the active back stack.
     */
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    /**
     * Navigates back. If the active back stack is at its base (a top level route), switches to the
     * start route's stack instead.
     */
    fun goBack() {
        val currentStack =
            state.backStacks[state.topLevelRoute]
                ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    /** Resets all back stacks to their roots and switches to the start route. */
    fun navigateHome() {
        state.backStacks.values.forEach { stack -> stack.truncateTo(1) }
        state.topLevelRoute = state.startRoute
    }

    /**
     * Navigates to [route], reusing an existing instance on the active back stack if present
     * (everything above it is popped). Otherwise, a new instance is pushed.
     */
    fun navigateOrReuse(route: NavKey) {
        val currentStack =
            state.backStacks[state.topLevelRoute]
                ?: error("Stack for ${state.topLevelRoute} not found")
        val index = currentStack.indexOf(route)
        if (index >= 0) {
            currentStack.truncateTo(index + 1)
        } else {
            currentStack.add(route)
        }
    }

    private fun NavBackStack<NavKey>.truncateTo(size: Int) {
        while (this.size > size) {
            removeAt(lastIndex)
        }
    }
}
