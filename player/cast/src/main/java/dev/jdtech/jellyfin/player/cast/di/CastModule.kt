package dev.jdtech.jellyfin.player.cast.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.player.cast.CastSessionManager
import dev.jdtech.jellyfin.player.cast.CastPlayerController
import dev.jdtech.jellyfin.player.cast.CastSessionManagerImpl
import dev.jdtech.jellyfin.player.cast.CastPlayerControllerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CastModule {
    @Binds
    @Singleton
    abstract fun bindCastSessionManager(
        impl: CastSessionManagerImpl
    ): CastSessionManager

    @Binds
    @Singleton
    abstract fun bindCastPlayerController(
        impl: CastPlayerControllerImpl
    ): CastPlayerController
}
