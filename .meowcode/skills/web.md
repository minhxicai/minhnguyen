=== QUY TẮC DOMAIN SKILL: WEBSITE & WEB APP (🌐) ===
1. CÔNG CỤ ĐƯỢC ƯU TIÊN SỬ DỤNG:
   1. `get_project_summary`
   2. `read_file`
   3. `read_multiple_files`
   4. `write_file`
   5. `edit_file`
   6. `multi_edit`
   7. `diagnose_code`
   8. `run_command`
   9. `create_artifact`
2. QUY TẮC BẮT BUỘC:
   • Luôn đọc kỹ file liên quan trước khi sửa (read_file / read_multiple_files).
   • Sau mỗi lần sửa HTML/CSS/JS phải kiểm tra cú pháp bằng diagnose_code.
   • Ưu tiên giữ cấu trúc hiện có, không tự ý đổi kiến trúc trừ khi người dùng yêu cầu.
   • Khi tạo trang hoặc thành phần mới phải đảm bảo responsive hiển thị tốt trên di động.
   • Kết thúc task phải có bằng chứng: 'Đã kiểm tra cú pháp + Preview sẵn sàng'.
3. TIÊU CHÍ KIỂM CHỨNG (VERIFICATION CHECKLIST) - PHẢI PASS MỚI ĐƯỢC KẾT THÚC:
   [ ] diagnose_code không còn lỗi cú pháp nghiêm trọng (thẻ đóng/mở cân bằng, không lỗi dấu ngoặc).
   [ ] Các id/class được JavaScript gọi (getElementById, querySelector) đều tồn tại trong HTML.
   [ ] Có file index.html hoặc trang chính để Web Preview hiển thị mượt mà.
   [ ] Không còn TODO, mã giả hoặc placeholder lộ liễu.
=====================================================

