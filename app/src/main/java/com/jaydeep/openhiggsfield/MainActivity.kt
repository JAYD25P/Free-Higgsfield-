package com.jaydeep.openhiggsfield

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var progress: ProgressBar
    private lateinit var addressLabel: TextView
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    private val fileRequestCode = 4102
    private val prefs by lazy { getSharedPreferences("studio", MODE_PRIVATE) }
    private val startUrl: String get() = prefs.getString("instance_url", DEFAULT_URL) ?: DEFAULT_URL

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF09090F.toInt()
        window.navigationBarColor = 0xFF09090F.toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF09090F.toInt()) }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(10), dp(8))
            setBackgroundColor(0xFF11111B.toInt())
        }
        val brand = TextView(this).apply {
            text = "OPENHIGGSFIELD"
            textSize = 16f
            setTextColor(0xFFF4F1FF.toInt())
            letterSpacing = 0.08f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        addressLabel = TextView(this).apply { text = "AI STUDIO"; textSize = 10f; setTextColor(0xFFA78BFA.toInt()) }
        val brandColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(brand); addView(addressLabel) }
        val reload = Button(this).apply {
            text = "↻"; textSize = 21f; setTextColor(0xFFEDE9FE.toInt())
            setBackgroundColor(android.graphics.Color.TRANSPARENT); setOnClickListener { webView.reload() }
        }
        val settings = Button(this).apply {
            text = "⋮"; textSize = 22f; setTextColor(0xFFEDE9FE.toInt())
            setBackgroundColor(android.graphics.Color.TRANSPARENT); setOnClickListener { showSettings() }
        }
        toolbar.addView(brandColumn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        toolbar.addView(reload, LinearLayout.LayoutParams(dp(44), dp(44)))
        toolbar.addView(settings, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(toolbar)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(0xFFA78BFA.toInt())
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(0xFF242434.toInt())
        }
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        swipe = SwipeRefreshLayout(this).apply {
            setColorSchemeColors(0xFFA78BFA.toInt())
            setOnRefreshListener { webView.reload() }
        }
        webView = WebView(this).apply {
            setBackgroundColor(0xFF09090F.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.allowFileAccess = false
            settings.allowContentAccess = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = true
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    val scheme = uri.scheme ?: return true
                    if (scheme == "https" || scheme == "http") {
                        val host = uri.host.orEmpty()
                        val configuredHost = Uri.parse(startUrl).host
                        if (host == "openhiggsfield.ai" || host.endsWith(".openhiggsfield.ai") ||
                            host == "higgsfield.ai" || host.endsWith(".higgsfield.ai") || host == configuredHost) return false
                    }
                    if (scheme == "mailto" || scheme == "tel") runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    else if (scheme == "intent") runCatching { startActivity(Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)) }
                    return true
                }
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    swipe.isRefreshing = false
                    addressLabel.text = Uri.parse(url).host ?: "AI STUDIO"
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                    if (request.isForMainFrame) {
                        swipe.isRefreshing = false
                        Toast.makeText(this@MainActivity, "Page couldn't load. Check your connection or studio URL.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                }
                override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
                    uploadCallback?.onReceiveValue(null)
                    uploadCallback = filePathCallback
                    val intent = fileChooserParams.createIntent().apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }
                    return try {
                        startActivityForResult(intent, fileRequestCode)
                        true
                    } catch (_: ActivityNotFoundException) {
                        uploadCallback = null
                        Toast.makeText(this@MainActivity, "No file picker available.", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            }
            setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                try {
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                    val request = DownloadManager.Request(Uri.parse(url))
                        .setMimeType(mimeType)
                        .addRequestHeader("User-Agent", userAgent)
                        .addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url).orEmpty())
                        .setTitle(fileName).setDescription("Downloading from OpenHiggsfield")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                    Toast.makeText(this@MainActivity, "Download started.", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "Download could not be started.", Toast.LENGTH_SHORT).show()
                }
            })
        }
        swipe.addView(webView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        root.addView(swipe, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        if (savedInstanceState == null) webView.loadUrl(startUrl) else webView.restoreState(savedInstanceState)
    }

    private fun showSettings() {
        val field = EditText(this).apply {
            setSingleLine(true); hint = "https://your-openhiggsfield-instance"; setText(startUrl)
            setTextColor(0xFFF4F1FF.toInt()); setHintTextColor(0xFF77778A.toInt()); setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(8), dp(22), 0)
            addView(TextView(this@MainActivity).apply { text = "Studio address"; textSize = 13f; setTextColor(0xFFB9B6C9.toInt()) })
            addView(field)
            addView(TextView(this@MainActivity).apply {
                text = "Use the hosted studio or your own self-hosted HTTPS instance. API credentials are entered in the studio itself."
                textSize = 12f; setTextColor(0xFF9290A3.toInt()); setPadding(0, dp(8), 0, 0)
            })
        }
        AlertDialog.Builder(this).setTitle("Studio settings").setView(wrap)
            .setNeutralButton("Reset") { _, _ -> prefs.edit().remove("instance_url").apply(); webView.loadUrl(DEFAULT_URL) }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save & open") { _, _ ->
                var value = field.text.toString().trim()
                if (!value.startsWith("https://")) value = "https://$value"
                val uri = Uri.parse(value)
                if (uri.host.isNullOrBlank() || uri.scheme != "https") {
                    Toast.makeText(this, "Enter a valid HTTPS studio address.", Toast.LENGTH_LONG).show()
                } else {
                    prefs.edit().putString("instance_url", value.trimEnd('/')).apply()
                    webView.loadUrl(value.trimEnd('/'))
                }
            }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fileRequestCode) {
            val result = if (resultCode == RESULT_OK) {
                data?.clipData?.let { clip -> Array(clip.itemCount) { i -> clip.getItemAt(i).uri } }
                    ?: data?.data?.let { arrayOf(it) }
            } else null
            uploadCallback?.onReceiveValue(result)
            uploadCallback = null
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        if (::webView.isInitialized) webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() {
        uploadCallback?.onReceiveValue(null)
        if (::webView.isInitialized) { webView.stopLoading(); webView.destroy() }
        super.onDestroy()
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    companion object { private const val DEFAULT_URL = "https://openhiggsfield.ai" }
}
