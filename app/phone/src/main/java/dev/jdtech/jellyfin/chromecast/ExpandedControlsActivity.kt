package dev.jdtech.jellyfin.chromecast

import android.os.Bundle
import android.view.Menu
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity
import dev.jdtech.jellyfin.core.R

class ExpandedControlsActivity : ExpandedControllerActivity() {
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.expanded_controller, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
        return true
    }
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val customButtonView = getButtonImageViewAt(1)
        val myCustomUiController = MyCustomUIController(customButtonView)
        uiMediaController.bindViewToUIController(customButtonView, myCustomUiController)
    }

}
