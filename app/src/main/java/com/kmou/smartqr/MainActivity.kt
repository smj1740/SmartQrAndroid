package com.kmou.smartqr

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kmou.smartqr.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val SITE_URL = "https://smj1740.github.io/dddff/"
        private val DOWNLOAD_HOOK_JS = """
            (function() {
              if (window.__smartQrDownloadHooked) return;
              window.__smartQrDownloadHooked = true;
              document.addEventListener('click', function(e) {
                var a = e.target && e.target.closest ? e.target.closest('a[download]') : null;
                if (!a) return;
                var href = a.href || '';
                if (href.indexOf('data:') === 0 && window.SmartQrAndroid) {
                  e.preventDefault();
                  e.stopPropagation();
                  window.SmartQrAndroid.downloadDataUrl(href, a.download || 'qr.png');
                }
              }, true);
            })();
        """.trimIndent()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        setupBackNavigation()

        if (savedInstanceState == null) {
            binding.webView.loadUrl(SITE_URL)
        } else {
            binding.webView.restoreState(savedInstanceState)
        }
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView
        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.addJavascriptInterface(AndroidBridge(), "SmartQrAndroid")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                return handleExternalUrl(url)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
                view?.evaluateJavascript(DOWNLOAD_HOOK_JS, null)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.progress = newProgress
                binding.progressBar.visibility =
                    if (newProgress in 1..99) View.VISIBLE else View.GONE
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (url.startsWith("data:")) {
                val name = URLUtil.guessFileName(url, contentDisposition, mimeType)
                saveDataUrl(url, name)
                return@setDownloadListener
            }

            try {
                val request = DownloadManager.Request(Uri.parse(url))
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                request.setMimeType(mimeType)
                request.addRequestHeader("User-Agent", userAgent)
                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                request.setDescription(getString(R.string.download_description))
                request.setTitle(fileName)
                request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
                val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                manager.enqueue(request)
                Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                if (url.startsWith("http")) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } else {
                    Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleExternalUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        val host = uri.host ?: return false

        if (host == "smj1740.github.io") {
            return false
        }

        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun saveDataUrl(dataUrl: String, fileName: String) {
        try {
            val comma = dataUrl.indexOf(',')
            if (comma < 0) {
                throw IllegalArgumentException("Invalid data URL")
            }
            val meta = dataUrl.substring(0, comma)
            val payload = dataUrl.substring(comma + 1)
            val mime = meta.substringAfter("data:").substringBefore(';').ifBlank { "image/png" }
            val bytes = if (meta.contains(";base64")) {
                Base64.decode(payload, Base64.DEFAULT)
            } else {
                Uri.decode(payload).toByteArray(Charsets.UTF_8)
            }
            val safeName = fileName.ifBlank { "qr.png" }.replace(Regex("""[\\/:*?"<>|]"""), "-")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("MediaStore insert failed")
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: throw IllegalStateException("Cannot open output stream")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val outFile = File(dir, safeName)
                FileOutputStream(outFile).use { it.write(bytes) }
            }

            runOnUiThread {
                Toast.makeText(this, R.string.download_saved, Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            runOnUiThread {
                Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun downloadDataUrl(dataUrl: String, fileName: String) {
            saveDataUrl(dataUrl, fileName)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        binding.webView.onResume()
    }

    override fun onPause() {
        binding.webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.webView.apply {
            loadUrl("about:blank")
            stopLoading()
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }
}
