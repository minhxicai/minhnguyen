package com.dohovn.clock;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Màn hình chính: nhúng ứng dụng web (đồng hồ) vào WebView.
 * Toàn bộ giao diện nằm trong assets/www/.
 */
public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);          // App dùng JS
        settings.setDomStorageEnabled(true);          // localStorage
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false); // Chuông tự kêu
        settings.setSupportZoom(false);
        settings.setAllowFileAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        // Giữ điều hướng trong app, không mở trình duyệt ngoài
        webView.setWebViewClient(new WebViewClient());
        webView.setBackgroundColor(0xFF0B1120);   // Khớp màu nền app

        // Ẩn thanh trạng thái để đồng hồ chiếm trọn màn hình
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        webView.loadUrl("file:///android_asset/www/index.html");
    }

    /** Nút Back của Android quay lại trang trước thay vì thoát app */
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
