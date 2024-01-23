package dev.jdtech.jellyfin

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class CastModule {

    @Provides
    fun castManager(castManager: NoOpCastManager): CastManager = castManager
}