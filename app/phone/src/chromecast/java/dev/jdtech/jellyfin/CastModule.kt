package dev.jdtech.jellyfin

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.chromecast.ChromecastManager

@Module
@InstallIn(SingletonComponent::class)
class CastModule {

    @Provides
    fun castManager(castManager: ChromecastManager): CastManager = castManager
}