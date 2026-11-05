=== QUY TẮC DOMAIN SKILL: ANDROID APK (🤖) ===
1. CÔNG CỤ ĐƯỢC ƯU TIÊN SỬ DỤNG:
   1. `get_project_summary`
   2. `read_file`
   3. `edit_file`
   4. `multi_edit`
   5. `diagnose_code`
   6. `build_apk`
   7. `create_artifact`
2. QUY TẮC BẮT BUỘC:
   • Không bao giờ sửa AndroidManifest.xml mà không đọc lại toàn bộ tệp.
   • Trước khi build phải đảm bảo cấu trúc Gradle khớp hoàn toàn: settings.gradle, build.gradle gốc, gradle.properties (android.useAndroidX=true), app/build.gradle và AndroidManifest.xml.
   • Mọi thay đổi permission (quyền) phải được giải thích rõ ràng và xin phép nếu nguy hiểm.
   • Sau khi sửa code Kotlin/Java/XML phải chạy diagnose_code trước khi đề xuất build.
   • Khi build_apk thành công phải báo rõ đường dẫn file APK và hỗ trợ cài 1 chạm.
3. TIÊU CHÍ KIỂM CHỨNG (VERIFICATION CHECKLIST) - PHẢI PASS MỚI ĐƯỢC KẾT THÚC:
   [ ] AndroidManifest.xml hợp lệ cú pháp XML và cấu trúc chuẩn.
   [ ] Không còn lỗi cú pháp Kotlin/Java nghiêm trọng.
   [ ] Cấu hình đóng gói .meowcode/build_config.json đầy đủ tên app và mã build.
   [ ] (Nếu người dùng yêu cầu build) File APK được tạo thành công và sẵn sàng cài đặt.
=====================================================

