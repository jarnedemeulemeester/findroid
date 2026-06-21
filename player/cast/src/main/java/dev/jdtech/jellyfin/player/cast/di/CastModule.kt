package dev.jdtech.jellyfin.player.cast.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.player.cast.CastManager
import dev.jdtech.jellyfin.player.cast.CastManagerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CastModule {
    @Binds
    @Singleton
    abstract fun bindCastManager(
        castManagerImpl: CastManagerImpl
    ): CastManager
}
