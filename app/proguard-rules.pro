# Giữ nguyên bridge JS->Java khi bật minify
-keepclassmembers class com.MiYoungcode.htmlcss.MainActivity$AndroidBridge {
    public *;
}
