package dev.jdtech.jellyfin

import android.view.Menu
import androidx.annotation.IdRes

interface CastManager {
    fun init(mainActivity: MainActivity)
    fun onResume()
    fun onStop()
    fun addCastMenuItem(menu: Menu, @IdRes menuItemId: Int)
}