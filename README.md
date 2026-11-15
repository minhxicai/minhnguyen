# HTML & CSS Demo — Android WebView

Ứng dụng Android (Kotlin) hiển thị giao diện web tĩnh đóng gói sẵn trong `app/src/main/assets`.
Toàn bộ app chạy offline, không cần quyền mạng.

## Cấu trúc chính
- `app/src/main/java/com/example/dnmi/MainActivity.kt` — Activity duy nhất, nạp WebView từ assets.
- `app/src/main/assets/index.html`, `style.css`, `app.js` — giao diện demo (đổi sáng/tối, bộ đếm, danh sách việc cần làm, lưu bằng `localStorage`).
- `app/src/main/res/` — layout, màu nền theo theme sáng/tối (`values` vs `values-night`), icon launcher.

## Điểm kỹ thuật đáng chú ý
- Nền WebView lấy từ `@color/app_background` nên tự khớp theme sáng/tối của hệ thống (không còn hằng số màu cứng).
- `WebSettings.forceDark = FORCE_DARK_OFF` (API 29+) để CSS tự quản lý dark mode, tránh WebView làm tối/đảo màu giao diện.
- `allowFileAccess` và `allowContentAccess` đều tắt; điều hướng chỉ cho phép tiền tố `file:///android_asset/`.
- Nút Back: quay lại lịch sử WebView nếu có, ngược lại thoát app (`OnBackPressedCallback`).
- Trạng thái WebView được lưu/khôi phục qua `onSaveInstanceState`.

## Kiểm tra cục bộ
```bash
node --check app/src/main/assets/app.js   # cú pháp JS
node tools/xmlcheck.js                    # quét toàn bộ XML trong app/src/main
```

## Biên dịch & Đóng gói
- **Trên điện thoại:** Gradle + Android SDK API 35 cần >4GB RAM và SDK cục bộ nên môi trường này báo KHÔNG HỖ TRỢ.
- **Đóng gói đám mây:** đã có sẵn `.github/workflows/build-apk.yml` (JDK 17 + `./gradlew assembleDebug`). Đồng bộ lên GitHub để nhận APK ở mục Artifacts của lần chạy.

Thông số build: `namespace`/`applicationId` = `com.example.dnmi`, `minSdk 26`, `targetSdk 35`, AGP 8.7.3, Kotlin 2.1.0, Gradle 8.11.1.
