package dev.jdtech.jellyfin.chromecast

import android.view.View
import com.google.android.gms.cast.framework.media.uicontroller.UIController


class MyCustomUIController(private val mView: View) : UIController() {
    override fun onMediaStatusUpdated() {
        // Update the state of mView based on the latest the media status.
        mView.visibility = View.VISIBLE

    }
}
