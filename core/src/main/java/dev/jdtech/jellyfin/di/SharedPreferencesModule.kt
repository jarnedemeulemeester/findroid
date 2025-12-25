package dev.jdtech.jellyfin.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedPreferencesModule {
    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext application: Context): SharedPreferences {
        return application.getSharedPreferences(
            application.packageName + "_preferences",
            Context.MODE_PRIVATE,
        )
    }
}
