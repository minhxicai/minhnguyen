package com.MiYoungcode.htmlcss;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {

    // Khoá cho SharedPreferences
    private static final String PREFS = "html_css_editor_prefs";
    private static final String KEY_HTML = "saved_html";
    private static final String KEY_CSS = "saved_css";

    private WebView webEditor;
    private TextView tabHtml, tabCss, tvTitle;
    private Button btnRun;
    private SharedPreferences prefs;

    // Tài liệu hiện tại
    private String htmlContent = "";
    private String cssContent = "";
    private boolean isShowingCss = false;
    private boolean editorReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        webEditor = findViewById(R.id.webEditor);
        tabHtml = findViewById(R.id.tabHtml);
        tabCss = findViewById(R.id.tabCss);
        tvTitle = findViewById(R.id.tvTitle);
        btnRun = findViewById(R.id.btnRun);

        // Khôi phục tiến trình đã lưu
        htmlContent = prefs.getString(KEY_HTML, defaultHtml());
        cssContent = prefs.getString(KEY_CSS, defaultCss());

        setupWebView();
        setupListeners();
    }

    private void setupWebView() {
        WebSettings s = webEditor.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        webEditor.setWebViewClient(new WebViewClient());
        // Cầu nối JS -> Java để auto-save
        webEditor.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        webEditor.loadUrl("file:///android_asset/editor/index.html");
    }

    private void setupListeners() {
        webEditor.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                editorReady = true;
                loadCurrentIntoEditor();
            }
        });

        tabHtml.setOnClickListener(v -> switchTab(false));
        tabCss.setOnClickListener(v -> switchTab(true));
        btnRun.setOnClickListener(v -> runInChrome());
    }

    // Chuyển tab HTML / CSS
    private void switchTab(boolean toCss) {
        if (isShowingCss == toCss) return;
        // lưu nội dung tab hiện tại trước khi chuyển
        saveCurrentTab();
        isShowingCss = toCss;
        loadCurrentIntoEditor();
        // cập nhật màu tab
        if (toCss) {
            tabCss.setBackgroundColor(0xFF1E1E1E);
            tabCss.setTextColor(0xFFFFFFFF);
            tabHtml.setBackgroundColor(0xFF2D2D2D);
            tabHtml.setTextColor(0xFFAAAAAA);
            tvTitle.setText("style.css");
        } else {
            tabHtml.setBackgroundColor(0xFF1E1E1E);
            tabHtml.setTextColor(0xFFFFFFFF);
            tabCss.setBackgroundColor(0xFF2D2D2D);
            tabCss.setTextColor(0xFFAAAAAA);
            tvTitle.setText("index.html");
        }
    }

    private void loadCurrentIntoEditor() {
        if (!editorReady) return;
        String content = isShowingCss ? cssContent : htmlContent;
        String mode = isShowingCss ? "css" : "htmlmixed";
        // escape bằng cách dùng JSON string đơn giản
        String js = "window.setCode(" + quoteJS(content) + ", '" + mode + "');";
        webEditor.evaluateJavascript(js, null);
    }

    private void saveCurrentTab() {
        if (!editorReady) return;
        webEditor.evaluateJavascript("window.getCode()", value -> {
            String code = unquoteJS(value);
            if (isShowingCss) {
                cssContent = code;
            } else {
                htmlContent = code;
            }
            persistAll();
        });
    }

    // Lưu vào SharedPreferences
    private void persistAll() {
        prefs.edit()
                .putString(KEY_HTML, htmlContent)
                .putString(KEY_CSS, cssContent)
                .apply();
    }

    // Mở Chrome xem trước
    private void runInChrome() {
        // Lưu lại nội dung hiện tại trước khi chạy
        saveCurrentTab();

        String fullHtml = buildFullHtml();
        try {
            File cacheDir = new File(getCacheDir(), "preview");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File outFile = new File(cacheDir, "preview.html");
            FileOutputStream fos = new FileOutputStream(outFile);
            OutputStreamWriter w = new OutputStreamWriter(fos, "UTF-8");
            w.write(fullHtml);
            w.flush();
            w.close();
            fos.close();

            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", outFile);

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "text/html");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Ghép HTML + CSS thành 1 trang hoàn chỉnh
    private String buildFullHtml() {
        String bodyHtml = htmlContent;
        // nếu người dùng đã có thẻ <html> hoàn chỉnh thì chèn CSS vào <head>
        String cssTag = "<style>\n" + cssContent + "\n</style>";
        if (bodyHtml.toLowerCase().contains("<head>")) {
            return bodyHtml.replaceFirst("(?i)<head>", "<head>\n" + cssTag + "\n");
        }
        return "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + cssTag + "\n</head>\n<body>\n" + bodyHtml + "\n</body>\n</html>";
    }

    // ===== Bridge để JS gọi auto-save =====
    private class AndroidBridge {
        @JavascriptInterface
        public void onCodeChanged(String value) {
            runOnUiThread(() -> {
                if (isShowingCss) cssContent = value; else htmlContent = value;
                persistAll();
            });
        }
    }

    // Escape chuỗi cho JS
    private String quoteJS(String s) {
        if (s == null) s = "";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private String unquoteJS(String jsValue) {
        if (jsValue == null || jsValue.equals("null")) return "";
        // evaluateJavascript trả về JSON string, ví dụ "\"abc\""
        if (jsValue.length() >= 2 && jsValue.charAt(0) == '"') {
            jsValue = jsValue.substring(1, jsValue.length() - 1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < jsValue.length(); i++) {
                char c = jsValue.charAt(i);
                if (c == '\\' && i + 1 < jsValue.length()) {
                    char n = jsValue.charAt(++i);
                    switch (n) {
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        default: sb.append(n);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        return jsValue;
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentTab();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveCurrentTab();
    }

    // ===== Nội dung mẫu mặc định =====
    private String defaultHtml() {
        return "<h1>Xin chào!</h1>\n<p>Bắt đầu viết HTML &amp; CSS ở đây.</p>\n<button>Bấm tôi</button>";
    }

    private String defaultCss() {
        return "body {\n  font-family: sans-serif;\n  padding: 20px;\n  background: #f5f5f5;\n  color: #333;\n}\nh1 { color: #0E639C; }\nbutton {\n  padding: 10px 16px;\n  background: #0E639C;\n  color: white;\n  border: none;\n  border-radius: 6px;\n}";
    }
}
