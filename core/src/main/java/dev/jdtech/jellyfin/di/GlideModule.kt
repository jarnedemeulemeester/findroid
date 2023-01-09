package dev.jdtech.jellyfin.di

import android.content.Context
import androidx.preference.PreferenceManager
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy.NONE
import com.bumptech.glide.load.engine.DiskCacheStrategy.RESOURCE
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import dev.jdtech.jellyfin.Constants
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

@GlideModule
class GlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val use = preferences.getBoolean(Constants.PREF_IMAGE_CACHE, true)

        if (use) {
            val sizeMb = preferences.getString(Constants.PREF_IMAGE_CACHE_SIZE, Constants.DEFAULT_CACHE_SIZE.toString())!!.toIntOrNull() ?: Constants.DEFAULT_CACHE_SIZE
            val sizeB = 1024L * 1024 * sizeMb
            Timber.d("Setting image cache to use $sizeMb MB of disk space")

            builder.setDiskCache(InternalCacheDiskCacheFactory(context, sizeB))
            builder.caching(enabled = true)
        } else {
            builder.caching(enabled = false)
            Timber.d("Image cache disabled. Clearing all persisted data.")

            MainScope().launch {
                GlideApp.getPhotoCacheDir(context)?.also {
                    if (it.exists()) it.deleteRecursively()
                }
            }
        }
    }

    private fun GlideBuilder.caching(enabled: Boolean) {
        setDefaultRequestOptions(
            RequestOptions().diskCacheStrategy(
                if (enabled) RESOURCE else NONE
            )
        )
    }
}
