package dev.jdtech.jellyfin

import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.jdtech.jellyfin.databinding.ActivityTrailerBinding

class TrailerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrailerBinding
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityTrailerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val trailerUrl = intent.getStringExtra("trailerUrl") ?: run {
            finish()
            return
        }

        setupWebView(trailerUrl)
        setupBackPressHandler()
        
        // Hide system UI for immersive experience
        hideSystemUI()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    customView != null -> {
                        binding.webView.webChromeClient?.onHideCustomView()
                    }
                    binding.webView.canGoBack() -> {
                        binding.webView.goBack()
                    }
                    else -> {
                        finish()
                    }
                }
            }
        })
    }

    private fun setupWebView(trailerUrl: String) {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            
            webViewClient = WebViewClient()
            
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (customView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    
                    customView = view
                    customViewCallback = callback
                    
                    binding.customViewContainer.apply {
                        visibility = View.VISIBLE
                        addView(view)
                    }
                    binding.webView.visibility = View.GONE
                }
                
                override fun onHideCustomView() {
                    binding.customViewContainer.removeView(customView)
                    customView = null
                    binding.customViewContainer.visibility = View.GONE
                    binding.webView.visibility = View.VISIBLE
                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null
                }
            }
            
            // Convert YouTube URL to embed URL if needed
            val embedUrl = convertToEmbedUrl(trailerUrl)
            loadUrl(embedUrl)
        }
        
        binding.closeButton.setOnClickListener {
            finish()
        }
    }

    private fun convertToEmbedUrl(url: String): String {
        // Extract video ID from various YouTube URL formats
        val videoId = when {
            url.contains("youtube.com/watch?v=") -> {
                url.substringAfter("watch?v=").substringBefore("&")
            }
            url.contains("youtu.be/") -> {
                url.substringAfter("youtu.be/").substringBefore("?")
            }
            url.contains("youtube.com/embed/") -> {
                return url // Already in embed format
            }
            else -> return url // Unknown format, try to load as-is
        }
        
        return "https://www.youtube.com/embed/$videoId?autoplay=1&modestbranding=1&rel=0"
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
