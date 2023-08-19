package dev.jdtech.jellyfin.themesong

interface ThemeSongPlayer {

    fun play(mediaUri: String)

    fun pause()

    fun stop()
}
