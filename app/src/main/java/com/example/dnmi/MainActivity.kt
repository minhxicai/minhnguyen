package com.example.dnmi

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var root: View
    private lateinit var webView: WebView

    /** Theme do phía web báo lên qua cầu JS; null = trang chưa gửi, dùng uiMode hệ thống. */
    private var webTheme: String? = null

    /** Chỉ nhận evaluateJavascript sau khi trang đã nạp xong tài liệu. */
    private var pageReady = false

    private var lastInsetsScript: String? = null
    private var lastBottomInset = -1

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // targetSdk 35: Android 15 ép edge-to-edge; bật tường minh để máy cũ cũng nhất quán
        WindowCompat.setDecorFitsSystemWindows(window, false)
        transparentSystemBars()
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)
        webView = findViewById(R.id.webView)
        applyWindowChrome()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = true
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }

        // Giao diện đã tự xử lý sáng/tối bằng CSS nên tắt cơ chế làm tối tự động của WebView
        disableForceDark()

        // app.js gọi AppHost.setTheme(theme) mỗi lần đổi theme để native khớp màu nền/status bar
        webView.addJavascriptInterface(ThemeBridge(), BRIDGE_NAME)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                // Chỉ cho phép tài nguyên nội bộ trong assets
                return !url.startsWith(LOCAL_PREFIX)
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                // Tài liệu mới: mọi biến CSS đã đặt trước đó bị xoá
                pageReady = false
            }

            override fun onPageFinished(view: WebView, url: String) {
                pageReady = true
                resendInsets()
            }
        }

        installInsetListener()
        restoreOrLoad(savedInstanceState)

        // Nút Back: quay lại lịch sử trang nếu có, ngược lại thoát app
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // uiMode nằm trong configChanges nên Activity không bị tạo lại: tự cập nhật nền.
        // Khi web đã báo theme thì applyWindowChrome giữ nguyên lựa chọn của người dùng.
        applyWindowChrome()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        webView.resumeTimers()
        webView.onResume()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private fun restoreOrLoad(state: Bundle?) {
        val restored = state != null && webView.restoreState(state) != null
        if (!restored) {
            webView.loadUrl(START_URL)
        }
    }

    /* ---------- Edge-to-edge / window insets ---------- */

    @Suppress("DEPRECATION")
    private fun transparentSystemBars() {
        // Android 15 đã bỏ màu thanh hệ thống (luôn trong suốt); máy cũ cần đặt tay
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
    }

    private fun installInsetListener() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            // Đáy: đệm trực tiếp lên WebView để bàn phím thu nhỏ viewport (giữ auto-scroll focus)
            applyBottomInset(maxOf(bars.bottom, ime.bottom))

            // Đỉnh/trái/phải: đẩy sang CSS để app-bar sticky phủ màu dưới status bar
            pushCssInsets(bars.top, bars.right, bars.left)

            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun applyBottomInset(bottomPx: Int) {
        if (lastBottomInset == bottomPx) return
        lastBottomInset = bottomPx
        webView.setPadding(0, 0, 0, bottomPx)
    }

    private fun pushCssInsets(topPx: Int, rightPx: Int, leftPx: Int) {
        val script = insetScript(topPx, rightPx, leftPx)
        if (script == lastInsetsScript) return
        lastInsetsScript = script
        if (pageReady) {
            webView.evaluateJavascript(script, null)
        }
    }

    private fun resendInsets() {
        val script = lastInsetsScript ?: return
        if (pageReady) {
            webView.evaluateJavascript(script, null)
        }
    }

    /** Insets là pixel vật lý, CSS px của WebView = dp, nên phải chia cho density. */
    private fun insetScript(topPx: Int, rightPx: Int, leftPx: Int): String {
        val density = resources.displayMetrics.density.let { if (it > 0f) it else 1f }
        return buildString {
            append("(function(){var s=document.documentElement.style;")
            append("s.setProperty('--inset-top','").append(cssPx(topPx, density)).append("');")
            append("s.setProperty('--inset-right','").append(cssPx(rightPx, density)).append("');")
            append("s.setProperty('--inset-left','").append(cssPx(leftPx, density)).append("');")
            append("})();")
        }
    }

    private fun cssPx(px: Int, density: Float): String {
        val value = Math.round(px / density * 100f) / 100f
        return "$value" + "px"
    }

    /* ---------- Đồng bộ theme native <-> web ---------- */

    private inner class ThemeBridge {
        @JavascriptInterface
        fun setTheme(theme: String?) {
            runOnUiThread { onWebTheme(theme) }
        }
    }

    private fun onWebTheme(theme: String?) {
        val normalized = when (theme) {
            THEME_DARK -> THEME_DARK
            THEME_LIGHT -> THEME_LIGHT
            else -> return
        }
        if (webTheme == normalized) return
        webTheme = normalized
        applyWindowChrome()
    }

    private fun applyWindowChrome() {
        // Màu nền gắn với theme của web; trước khi web báo thì bám uiMode hệ thống
        val colorRes = when (webTheme) {
            THEME_DARK -> R.color.app_background_dark
            THEME_LIGHT -> R.color.app_background
            else -> R.color.app_background_system
        }

        val dark = when (webTheme) {
            THEME_DARK -> true
            THEME_LIGHT -> false
            else -> isSystemDark()
        }

        val color = ContextCompat.getColor(this, colorRes)

        // Nền lộ ra ở vùng inset và trong lúc WebView vẽ lại
        root.setBackgroundColor(color)
        webView.setBackgroundColor(color)
        window.decorView.setBackgroundColor(color)

        // Icon status/navigation bar tương phản với nền web
        WindowInsetsControllerCompat(window, root).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    private fun isSystemDark(): Boolean {
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    @Suppress("DEPRECATION")
    private fun disableForceDark() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webView.settings.forceDark = WebSettings.FORCE_DARK_OFF
        }
    }

    private companion object {
        const val START_URL = "file:///android_asset/index.html"
        const val LOCAL_PREFIX = "file:///android_asset/"
        const val BRIDGE_NAME = "AppHost"
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"
    }
}
