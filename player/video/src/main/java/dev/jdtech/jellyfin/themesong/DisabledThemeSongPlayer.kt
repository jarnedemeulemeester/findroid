package dev.jdtech.jellyfin.themesong

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisabledThemeSongPlayer @Inject constructor() : ThemeSongPlayer {
    override fun play(mediaUri: String) {
        // Nothing to do
    }

    override fun pause() {
        // Nothing to do
    }

    override fun stop() {
        // Nothing to do
    }
}
