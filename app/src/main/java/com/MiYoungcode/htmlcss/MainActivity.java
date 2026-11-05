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
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.Window;

import android.widget.PopupMenu;

import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {

    // Khoá cho SharedPreferences
    private static final String PREFS = "html_css_editor_prefs";
    private static final String KEY_HTML = "saved_html";
    private static final String KEY_CSS = "saved_css";
    private static final String KEY_JS = "saved_js";

    private WebView webEditor;
    private TextView tabHtml, tabCss, tabJs, tvTitle;
    private Button btnRun;
    private TextView btnMenu;
    private SharedPreferences prefs;

    // Tài liệu hiện tại
    private String htmlContent = "";
    private String cssContent = "";
    private String jsContent = "";
    // currentTab: 0 = HTML, 1 = CSS, 2 = JS
    private int currentTab = 0;
    private boolean isShowingCss = false;
    private boolean editorReady = false;

    // Mã yêu cầu SAF khi lưu file
    private static final int REQ_SAVE_HTML = 1001;
    private static final int REQ_SAVE_CSS = 1002;
    private static final int REQ_SAVE_FULL = 1003;

    // Mã yêu cầu SAF khi mở/đọc file
    private static final int REQ_OPEN_HTML = 2001;
    private static final int REQ_OPEN_CSS = 2002;
    private static final int REQ_OPEN_FULL = 2003;

    // Cờ: đang lưu HTML thì sau đó hỏi lưu tiếp CSS
    private boolean pendingSaveCss = false;

    // Callback trả nội dung mới nhất của cả 2 tab
    private interface ContentCallback {
        void onContent(String html, String css);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        webEditor = findViewById(R.id.webEditor);
        tabHtml = findViewById(R.id.tabHtml);
        tabCss = findViewById(R.id.tabCss);
        tabJs = findViewById(R.id.tabJs);
        tvTitle = findViewById(R.id.tvTitle);
        btnRun = findViewById(R.id.btnRun);
        btnMenu = findViewById(R.id.btnMenu);

        // Khôi phục tiến trình đã lưu
        htmlContent = prefs.getString(KEY_HTML, defaultHtml());
        cssContent = prefs.getString(KEY_CSS, defaultCss());
        jsContent = prefs.getString(KEY_JS, "");

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

        tabHtml.setOnClickListener(v -> switchTab(0));
        tabCss.setOnClickListener(v -> switchTab(1));
        tabJs.setOnClickListener(v -> switchTab(2));
        btnRun.setOnClickListener(v -> showPreview());
        btnMenu.setOnClickListener(this::showSaveMenu);
    }

    // ===== Menu 3 gạch (góc trái) chứa các tác vụ lưu file =====
    private void showSaveMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Lưu index.html (mã HTML)");
        popup.getMenu().add(0, 2, 1, "Lưu style.css (mã CSS)");
        popup.getMenu().add(0, 3, 2, "Lưu trang hoàn chỉnh (HTML + CSS)");
        popup.getMenu().add(0, 4, 3, "Lưu cả 2 file HTML và CSS");
        popup.getMenu().add(0, 5, 4, "Mở file HTML (.html)");
        popup.getMenu().add(0, 6, 5, "Mở file CSS (.css)");
        popup.getMenu().add(0, 7, 6, "Mở trang hoàn chỉnh (tự tách CSS)");
        popup.getMenu().add(0, 8, 7, "Định dạng code (Format)");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    saveFileDialog(REQ_SAVE_HTML);
                    return true;
                case 2:
                    saveFileDialog(REQ_SAVE_CSS);
                    return true;
                case 3:
                    saveFileDialog(REQ_SAVE_FULL);
                    return true;
                case 4:
                    pendingSaveCss = true;
                    saveFileDialog(REQ_SAVE_HTML);
                    return true;
                case 5:
                    openFileDialog(REQ_OPEN_HTML);
                    return true;
                case 6:
                    openFileDialog(REQ_OPEN_CSS);
                    return true;
                case 7:
                    openFileDialog(REQ_OPEN_FULL);
                    return true;
                case 8:
                    formatCurrentCode();
                    return true;
            }
            return false;
        });
        popup.show();
    }

    // Mở hộp thoại chọn nơi lưu file (SAF - không cần xin quyền)
    private void saveFileDialog(int type) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        if (type == REQ_SAVE_CSS) {
            i.setType("text/css");
            i.putExtra(Intent.EXTRA_TITLE, "style.css");
        } else if (type == REQ_SAVE_HTML) {
            i.setType("text/html");
            i.putExtra(Intent.EXTRA_TITLE, "index.html");
        } else {
            i.setType("text/html");
            i.putExtra(Intent.EXTRA_TITLE, "page.html");
        }
        try {
            startActivityForResult(i, type);
        } catch (Exception e) {
            Toast.makeText(this, "Không mở được trình lưu file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Mở hộp thoại chọn file HTML/CSS để đọc
    private void openFileDialog(int type) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        // Dùng */* kèm EXTRA_MIME_TYPES để lọc đúng định dạng mong muốn
        i.setType("*/*");
        if (type == REQ_OPEN_CSS) {
            i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/css", "text/plain"});
        } else {
            i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/html", "text/plain"});
        }
        try {
            startActivityForResult(i, type);
        } catch (Exception e) {
            Toast.makeText(this, "Không mở được trình chọn file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Đọc nội dung văn bản UTF-8 từ Uri do người dùng chọn
    private String readTextFromUri(Uri uri) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (!first) sb.append('\n');
                sb.append(line);
                first = false;
            }
        } catch (Exception e) {
            return null;
        }
        return sb.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            pendingSaveCss = false;
            return;
        }
        final Uri uri = data.getData();
        // Xử lý mở trang hoàn chỉnh: tự tách thẻ <style> thành tab CSS
        if (requestCode == REQ_OPEN_FULL) {
            String content = readTextFromUri(uri);
            if (content == null) {
                Toast.makeText(this, "Không đọc được file", Toast.LENGTH_LONG).show();
                return;
            }
            String[] parts = splitHtmlCss(content);
            htmlContent = parts[0];
            cssContent = parts[1];
            loadCurrentIntoEditor();
            persistAll();
            Toast.makeText(this, "Đã mở trang và tách CSS", Toast.LENGTH_SHORT).show();
            return;
        }
        // Xử lý mở/đọc file HTML hoặc CSS
        if (requestCode == REQ_OPEN_HTML || requestCode == REQ_OPEN_CSS) {
            String content = readTextFromUri(uri);
            if (content == null) {
                Toast.makeText(this, "Không đọc được file", Toast.LENGTH_LONG).show();
                return;
            }
            if (requestCode == REQ_OPEN_HTML) {
                htmlContent = content;
                if (currentTab != 0) switchTab(0);
                else loadCurrentIntoEditor();
                Toast.makeText(this, "Đã mở file HTML", Toast.LENGTH_SHORT).show();
            } else {
                cssContent = content;
                if (currentTab != 1) switchTab(1);
                else loadCurrentIntoEditor();
                Toast.makeText(this, "Đã mở file CSS", Toast.LENGTH_SHORT).show();
            }
            persistAll();
            return;
        }
        withFreshContent((html, css) -> {
            String text;
            String label;
            if (requestCode == REQ_SAVE_CSS) {
                text = css;
                label = "style.css";
            } else if (requestCode == REQ_SAVE_HTML) {
                text = html;
                label = "index.html";
            } else {
                text = buildFullHtml(html, css);
                label = "trang hoàn chỉnh";
            }
            if (writeTextToUri(uri, text)) {
                Toast.makeText(this, "Đã lưu " + label, Toast.LENGTH_SHORT).show();
            }
            // Nếu chọn "lưu cả 2 file" thì hỏi tiếp vị trí lưu CSS
            if (pendingSaveCss) {
                pendingSaveCss = false;
                saveFileDialog(REQ_SAVE_CSS);
            }
        });
    }

    // Tách nội dung trang hoàn chỉnh thành [phần HTML, phần CSS trong thẻ <style>]
    // Dùng biểu thức đơn giản để lấy khối <style>...</style> đầu tiên
    private String[] splitHtmlCss(String full) {
        String css = "";
        String html = full;
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(?is)<style[^>]*>(.*?)</style>");
            java.util.regex.Matcher m = p.matcher(full);
            if (m.find()) {
                css = m.group(1).trim();
                // Xóa khối <style>...</style> đầu tiên khỏi phần HTML
                html = new StringBuilder(full).delete(m.start(), m.end()).toString().trim();
            }
        } catch (Exception ignored) { }
        return new String[] { html, css };
    }

    // Lấy nội dung mới nhất của tab đang mở rồi trả cả 2 nội dung
    private void withFreshContent(ContentCallback callback) {
        if (!editorReady) {
            callback.onContent(htmlContent, cssContent);
            return;
        }
        webEditor.evaluateJavascript("window.getCode()", value -> {
            String code = unquoteJS(value);
            if (currentTab == 0) {
                htmlContent = code;
            } else if (currentTab == 1) {
                cssContent = code;
            } else {
                jsContent = code;
            }
            persistAll();
            callback.onContent(htmlContent, cssContent);
        });
    }

    // Ghi nội dung UTF-8 vào Uri do người dùng chọn
    private boolean writeTextToUri(Uri uri, String text) {
        try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
            if (os == null) throw new Exception("không mở được file");
            os.write((text == null ? "" : text).getBytes("UTF-8"));
            os.flush();
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi lưu file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    // Chuyển tab: 0 = HTML, 1 = CSS, 2 = JS
    private void switchTab(int tab) {
        if (currentTab == tab) return;
        // lưu nội dung tab hiện tại trước khi chuyển
        saveCurrentTab();
        currentTab = tab;
        loadCurrentIntoEditor();
        // cập nhật màu tab (tab đang mở thì sáng, còn lại tối)
        int activeBg = 0xFF1E1E1E, activeFg = 0xFFFFFFFF;
        int idleBg = 0xFF2D2D2D, idleFg = 0xFFAAAAAA;
        tabHtml.setBackgroundColor(tab == 0 ? activeBg : idleBg);
        tabHtml.setTextColor(tab == 0 ? activeFg : idleFg);
        tabCss.setBackgroundColor(tab == 1 ? activeBg : idleBg);
        tabCss.setTextColor(tab == 1 ? activeFg : idleFg);
        tabJs.setBackgroundColor(tab == 2 ? activeBg : idleBg);
        tabJs.setTextColor(tab == 2 ? activeFg : idleFg);
        tvTitle.setText(tab == 0 ? "index.html" : (tab == 1 ? "style.css" : "script.js"));
    }



    // Nạp nội dung của tab hiện tại vào editor
    private void loadCurrentIntoEditor() {
        if (!editorReady) return;
        String content;
        String mode;
        if (currentTab == 0) {
            content = htmlContent;
            mode = "htmlmixed";
        } else if (currentTab == 1) {
            content = cssContent;
            mode = "css";
        } else {
            content = jsContent;
            mode = "javascript";
        }
        String js = "window.setCode(" + quoteJS(content) + ", '" + mode + "');";
        webEditor.evaluateJavascript(js, null);
    }

    // Lưu nội dung tab hiện tại từ editor vào biến
    private void saveCurrentTab() {
        if (!editorReady) return;
        webEditor.evaluateJavascript("window.getCode()", value -> {
            String code = unquoteJS(value);
            if (currentTab == 0) {
                htmlContent = code;
            } else if (currentTab == 1) {
                cssContent = code;
            } else {
                jsContent = code;
            }
            persistAll();
        });
    }

    // Lưu vào SharedPreferences
    private void persistAll() {
        prefs.edit()
                .putString(KEY_HTML, htmlContent)
                .putString(KEY_CSS, cssContent)
                .putString(KEY_JS, jsContent)
                .apply();
    }

    // ===== Xem trước ngay trong app =====
    // Lấy code đồng bộ từ editor rồi mở khung xem trước
    private void showPreview() {
        if (editorReady) {
            webEditor.evaluateJavascript("window.getCode ? window.getCode() : ''", value -> {
                String code = unquoteJS(value);
                if (currentTab == 0) {
                    htmlContent = code;
                } else if (currentTab == 1) {
                    cssContent = code;
                } else {
                    jsContent = code;
                }
                persistAll();
                openPreviewDialog();
            });
        } else {
            openPreviewDialog();
        }
    }

    // Khung xem trước toàn màn hình bằng WebView
    private void openPreviewDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        int pad = (int) (12 * getResources().getDisplayMetrics().density);

        // Khung dọc: thanh tiêu đề + WebView
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1E1E1E"));

        // Thanh tiêu đề
        android.widget.LinearLayout bar = new android.widget.LinearLayout(this);
        bar.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        bar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.parseColor("#252526"));
        bar.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Xem trước");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        bar.addView(title, new android.widget.LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnClose = new TextView(this);
        btnClose.setText("✕");
        btnClose.setTextColor(Color.WHITE);
        btnClose.setTextSize(18);
        btnClose.setPadding(pad, 0, pad, 0);
        bar.addView(btnClose, new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(bar, new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // WebView hiển thị kết quả
        WebView preview = new WebView(this);
        WebSettings ps = preview.getSettings();
        ps.setJavaScriptEnabled(true);
        ps.setDomStorageEnabled(true);
        ps.setUseWideViewPort(true);
        ps.setLoadWithOverviewMode(true);
        preview.setBackgroundColor(Color.WHITE);

        root.addView(preview, new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        preview.loadDataWithBaseURL(null, buildFullHtml(), "text/html", "UTF-8", null);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(root);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
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
        return buildFullHtml(htmlContent, cssContent);
    }

    private String buildFullHtml(String bodyHtml, String css) {
        return buildFullHtml(bodyHtml, css, jsContent);
    }

    // Ghép HTML + CSS + JS thành 1 trang hoàn chỉnh
    private String buildFullHtml(String bodyHtml, String css, String js) {
        String cssTag = "<style>\n" + css + "\n</style>";
        String jsTag = (js == null || js.trim().isEmpty()) ? "" : "<script>\n" + js + "\n</script>";
        // nếu người dùng đã có thẻ <html> hoàn chỉnh thì chèn CSS/JS vào <head>
        if (bodyHtml.toLowerCase().contains("<head>")) {
            String result = bodyHtml.replaceFirst("(?i)<head>", "<head>\n" + cssTag + "\n");
            if (!jsTag.isEmpty()) {
                result = result.replaceFirst("(?i)</body>", jsTag + "\n</body>");
            }
            return result;
        }
        return "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + cssTag + "\n</head>\n<body>\n" + bodyHtml + "\n" + jsTag + "\n</body>\n</html>";
    }

    // ===== Định dạng code (Beautify) =====
    // Lấy nội dung tab đang hiển thị, format rồi nạp lại vào editor
    private void formatCurrentCode() {
        if (!editorReady) {
            Toast.makeText(this, "Editor chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }
        webEditor.evaluateJavascript("window.getCode()", value -> {
            String raw = unquoteJS(value);
            if (currentTab == 1) {
                raw = beautifyCss(raw);
                cssContent = raw;
            } else if (currentTab == 0) {
                raw = beautifyHtml(raw);
                htmlContent = raw;
            }
            persistAll();
            loadCurrentIntoEditor();
            Toast.makeText(this, "Đã định dạng code", Toast.LENGTH_SHORT).show();
        });
    }

    // Định dạng CSS cơ bản: thụt lề theo cấp ngoặc nhọn, một khai báo mỗi dòng
    private String beautifyCss(String src) {
        StringBuilder out = new StringBuilder();
        int indent = 0;
        StringBuilder token = new StringBuilder();
        boolean inString = false;
        char strChar = 0;
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (inString) {
                token.append(c);
                if (c == strChar) inString = false;
                continue;
            }
            if (c == '"' || c == '\'') {
                inString = true;
                strChar = c;
                token.append(c);
                continue;
            }
            if (c == '{') {
                appendTrimmed(out, token, indent);
                out.append(" {\n");
                indent++;
                token.setLength(0);
            } else if (c == '}') {
                appendTrimmed(out, token, indent);
                indent = Math.max(0, indent - 1);
                out.append(indentStr(indent)).append("}\n");
                token.setLength(0);
            } else if (c == ';') {
                token.append(c);
                appendTrimmed(out, token, indent);
                token.setLength(0);
            } else {
                token.append(c);
            }
        }
        if (token.toString().trim().length() > 0) appendTrimmed(out, token, indent);
        return out.toString().trim();
    }

    // Ghi token đã trim vào out với thụt lề
    private void appendTrimmed(StringBuilder out, StringBuilder token, int indent) {
        String t = token.toString().trim();
        if (t.length() == 0) return;
        out.append(indentStr(indent)).append(t).append("\n");
    }

    private String indentStr(int level) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < level; i++) s.append("  ");
        return s.toString();
    }

    // Định dạng HTML cơ bản: thụt lề theo cặp thẻ đóng/mở
    private String beautifyHtml(String src) {
        String[] lines = src.split("\\r?\\n");
        StringBuilder out = new StringBuilder();
        int indent = 0;
        for (String ln : lines) {
            String t = ln.trim();
            if (t.length() == 0) continue;
            if (t.startsWith("</")) indent = Math.max(0, indent - 1);
            out.append(indentStr(indent)).append(t).append("\n");
            if (t.startsWith("<") && !t.startsWith("</") && !t.startsWith("<!") && !t.endsWith("/>") && !t.matches("<[^>]+>.*</[^>]+>.*") && !t.contains("</")) {
                indent++;
            }
        }
        return out.toString().trim();
    }

    // ===== Bridge để JS gọi auto-save =====
    private class AndroidBridge {
        @JavascriptInterface
        public void onCodeChanged(String value) {
            runOnUiThread(() -> {
                if (currentTab == 0) htmlContent = value;
                else if (currentTab == 1) cssContent = value;
                else jsContent = value;
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
        try {
            Object decoded = new JSONTokener(jsValue).nextValue();
            return decoded.toString();
        } catch (Exception e) {
            if (jsValue.length() >= 2 && jsValue.charAt(0) == '"' 
                    && jsValue.charAt(jsValue.length() - 1) == '"') {
                return jsValue.substring(1, jsValue.length() - 1);
            }
            return jsValue;
        }
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
