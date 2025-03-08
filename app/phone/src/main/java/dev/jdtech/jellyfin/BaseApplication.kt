package dev.jdtech.jellyfin

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import dev.jdtech.jellyfin.core.services.ConnectivityService
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class BaseApplication : Application(), Configuration.Provider, ImageLoaderFactory {
    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val mode = when (appPreferences.getValue(appPreferences.theme)) {
                "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        if (appPreferences.getValue(appPreferences.dynamicColors)) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }

        startService(Intent(applicationContext, ConnectivityService::class.java))
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .diskCachePolicy(if (appPreferences.getValue(appPreferences.imageCache)) CachePolicy.ENABLED else CachePolicy.DISABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(appPreferences.getValue(appPreferences.imageCacheSize) * 1024L * 1024)
                    .build()
            }
            .build()
    }
}
