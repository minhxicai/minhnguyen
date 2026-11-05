=== QUY TẮC DOMAIN SKILL: MINECRAFT PLUGIN (⛏️) ===
1. CÔNG CỤ ĐƯỢC ƯU TIÊN SỬ DỤNG:
   1. `get_project_summary`
   2. `read_file`
   3. `write_file`
   4. `edit_file`
   5. `diagnose_code`
   6. `build_plugin`
   7. `create_artifact`
2. QUY TẮC BẮT BUỘC:
   • Luôn kiểm tra phiên bản Paper/Spigot API đang sử dụng trong dự án.
   • Không dùng API đã deprecated (lỗi thời) nếu có giải pháp thay thế mới hơn.
   • Mọi lệnh (command) và event listener (sự kiện) phải được đăng ký đầy đủ trong onEnable().
   • Sau khi sửa phải đảm bảo file plugin.yml (hoặc paper-plugin.yml) khớp với class chính (main).
   • Ưu tiên viết mã nguồn sạch, có chú thích ngắn gọn cho các logic quan trọng.
3. TIÊU CHÍ KIỂM CHỨNG (VERIFICATION CHECKLIST) - PHẢI PASS MỚI ĐƯỢC KẾT THÚC:
   [ ] plugin.yml hoặc paper-plugin.yml hợp lệ (đủ name, version, main).
   [ ] Main class tồn tại và kế thừa JavaPlugin đúng quy cách.
   [ ] Không còn lỗi cú pháp Java nghiêm trọng.
   [ ] (Nếu được yêu cầu build) Tệp JAR được biên dịch thành công.
=====================================================

