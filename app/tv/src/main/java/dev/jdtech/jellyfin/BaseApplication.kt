package dev.jdtech.jellyfin

import android.app.Application
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
import dagger.hilt.android.HiltAndroidApp
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import okio.Path.Companion.toOkioPath

@HiltAndroidApp
class BaseApplication : Application(), SingletonImageLoader.Factory {
    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()

        Thread {
            try {
                Runtime.getRuntime().exec("logcat -c").waitFor()
                val logDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                logDir.mkdirs()
                var logFile = java.io.File(logDir, "findroid_logs_tv.txt")
                
                try {
                    logFile.createNewFile()
                } catch (e: Exception) {
                    val fallbackDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (fallbackDir != null) {
                        fallbackDir.mkdirs()
                        logFile = java.io.File(fallbackDir, "findroid_logs_tv.txt")
                    }
                }

                val process = Runtime.getRuntime().exec("logcat -v threadtime")
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                val writer = java.io.FileWriter(logFile, true)
                writer.append("--- LOG START ---\n")
                writer.flush()
                var line: String? = reader.readLine()
                while (line != null) {
                    writer.append(line).append("\n")
                    writer.flush()
                    line = reader.readLine()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    @OptIn(ExperimentalCoilApi::class, ExperimentalTime::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(OkHttpNetworkFetcherFactory(cacheStrategy = { CacheControlCacheStrategy() }))
                add(SvgDecoder.Factory())
            }
            .diskCachePolicy(
                if (appPreferences.getValue(appPreferences.imageCache)) CachePolicy.ENABLED
                else CachePolicy.DISABLED
            )
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(
                        appPreferences.getValue(appPreferences.imageCacheSize) * 1024L * 1024
                    )
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
