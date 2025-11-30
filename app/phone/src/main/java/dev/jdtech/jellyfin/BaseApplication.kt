package dev.jdtech.jellyfin

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import dev.jdtech.jellyfin.network.ProxyOkHttpClientFactory
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import okio.Path.Companion.toOkioPath
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.ExperimentalTime

@HiltAndroidApp
class BaseApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {
    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var proxyOkHttpClientFactory: ProxyOkHttpClientFactory

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
    }

    @OptIn(ExperimentalCoilApi::class, ExperimentalTime::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        // Create proxy-configured OkHttpClient for image loading
        val okHttpClient = proxyOkHttpClientFactory.createClient()

        return ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { okHttpClient },
                        cacheStrategy = { CacheControlCacheStrategy() },
                    ),
                )
                add(SvgDecoder.Factory())
            }
            .diskCachePolicy(if (appPreferences.getValue(appPreferences.imageCache)) CachePolicy.ENABLED else CachePolicy.DISABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(appPreferences.getValue(appPreferences.imageCacheSize) * 1024L * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
