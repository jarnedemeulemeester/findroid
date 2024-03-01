package dev.jdtech.jellyfin

import android.view.Menu
import javax.inject.Inject

class NoOpCastManager
@Inject
constructor() : CastManager {
    override fun init(mainActivity: MainActivity) {
    }

    override fun onResume() {
    }

    override fun onStop() {
    }

    override fun addCastMenuItem(menu: Menu, menuItemId: Int) {
        menu.findItem(menuItemId)?.setVisible(false)
    }
}