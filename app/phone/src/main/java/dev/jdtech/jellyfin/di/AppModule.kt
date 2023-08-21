package dev.jdtech.jellyfin.di

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.BaseApplication
import dev.jdtech.jellyfin.themesong.DisabledThemeSongPlayer
import dev.jdtech.jellyfin.themesong.ExoThemeSongPlayer
import dev.jdtech.jellyfin.themesong.ThemeSongPlayer
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideApplication(@ApplicationContext app: Context): BaseApplication {
        return app as BaseApplication
    }

    @Singleton
    @Provides
    fun provideLifecycle(): Lifecycle {
        return ProcessLifecycleOwner.get().lifecycle
    }

    @Provides
    fun bindExoThemeSongPlayer(
        appPreferences: AppPreferences,
        exoThemeSongPlayerProvider: Provider<ExoThemeSongPlayer>,
        disabledThemeSongPlayerProvider: Provider<DisabledThemeSongPlayer>,
    ): ThemeSongPlayer {
        return if (appPreferences.tvThemeSongs) {
            exoThemeSongPlayerProvider.get()
        } else {
            disabledThemeSongPlayerProvider.get()
        }
    }
}
