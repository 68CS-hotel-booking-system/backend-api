# Giải thích cơ sở dữ liệu — Hotel Booking

**PostgreSQL · Cập nhật 21/10/2026 · 37 bảng · 300 cột · 57 FK · 62 index có tên · 20 enum.**

## 1. Nguồn mới nhất và cách đọc

Tài liệu theo đúng DBML trong `Pasted text.txt`, attachment `79c06226-bda6-4781-bb17-1facdd65324e` người dùng gửi ngày 21/10/2026. Bản này thay bản export 33 bảng ngày 19/10 làm nguồn của bộ docs database.

SHA-256 file nguồn: `3d74664eabdbbab595638b7c44126ee237a3aabc59437166d88d97e08eb9cfa2`.

| File | Nội dung |
| --- | --- |
| [README.md](README.md) | Tổng quan, phạm vi và các điểm cần sửa/làm rõ |
| [erd.md](erd.md) | Bốn vùng ERD, đủ 57 FK và đúng hành vi delete/update trong DBML |
| [data-dictionary.md](data-dictionary.md) | Đủ 37 bảng, 300 cột, kiểu, nullable/default, FK và 62 index có tên |
| [business-rules.md](business-rules.md) | Quy tắc dữ liệu, voucher đơn giản, công thức tiền và đủ 20 enum nguồn |

**THEO DBML** là cấu trúc có trong file gửi; **NGHIỆP VỤ** là yêu cầu đã trao đổi; **ĐỀ XUẤT/CẦN SỬA** chưa được áp vào DBML. Không đồng nhất ba loại nội dung này.

Nhãn CURRENT/Flyway/PLANNED trong Note chỉ là ghi chú của nguồn. Chưa chạy migration hoặc kiểm tra PostgreSQL thực tế. Các CHECK ghi trong Note không phải CHECK được khai báo: file mới không có khối `checks`. Index có tên không tính các index được PostgreSQL tự tạo bởi PK/UNIQUE cột.

## 2. Các nhóm bảng

| Nhóm | Số bảng | Các bảng |
| --- | ---: | --- |
| Identity | 8 | `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens`, `password_reset_tokens`, `email_verification_tokens` |
| Hotel | 8 | `hotels`, `room_types`, `rooms`, `hotel_members`, `cities`, `amenities`, `hotel_amenities`, `room_type_amenities` |
| Inventory | 3 | `room_inventory`, `room_blocks`, `inventory_reservations` |
| Pricing | 1 | `room_rates` |
| Booking | 5 | `bookings`, `booking_items`, `booking_night_prices`, `booking_status_history`, `room_assignments` |
| Payment | 3 | `payment_attempts`, `payment_webhook_events`, `refunds` |
| Media | 4 | `media_objects`, `hotel_images`, `room_type_images`, `user_images` |
| Review | 1 | `reviews` |
| Promotion | 2 | `promotions`, `promotion_redemptions` |
| Notification | 1 | `notifications` |
| Outbox | 1 | `outbox_events` |

## 3. Mô hình đang có

Khách đặt theo loại phòng và số lượng. `inventory_reservations` giữ tồn theo từng đêm; `room_assignments` gán phòng cụ thể từ `rooms` trước hoặc khi nhận phòng. `room_blocks` đã có `room_id` để chặn phòng vật lý. Gán phòng không trừ tồn hoặc thu tiền lần thứ hai.

`bookings` có tổng trước giảm, khoản giảm và tổng sau giảm. Voucher được cấu hình ở `promotions`, lượt áp và khoản giảm của đơn ở `promotion_redemptions`. Một đơn có tối đa một redemption nhờ UNIQUE `booking_id`. Chi tiết điều kiện/công thức ở [phần voucher](business-rules.md#5-voucher-đơn-giản).

`payment_attempts`, `payment_webhook_events`, `refunds` hỗ trợ theo dõi thu/hoàn tiền và chống lặp. `email_verification_tokens`, `user_images` và enum NO_SHOW đã có trong nguồn mới; không còn mô tả các thành phần này là thiếu bảng như bản 19/10.

Các nhóm giúp đọc thiết kế, không khẳng định module source/API tương ứng đã triển khai.

## 4. Những điểm cần sửa hoặc làm rõ

| Mã | Hiện trạng thật trong DBML | Hướng xử lý đề xuất |
| --- | --- | --- |
| DB-01 | `users.email_verified_at` là `timestamp NOT NULL` | Đổi thành `timestamptz` nullable để tài khoản chưa xác minh có NULL; docs giữ nguyên kiểu nguồn trong từ điển |
| DB-02 | `room_blocks.room_id` và FK ghép đã có; quantity vẫn chỉ not null, Note vẫn nói chưa có phòng vật lý | Chốt một dòng/một phòng, quantity = 1 và sửa Note; chưa tự coi CHECK đó đã tồn tại |
| DB-03 | Một cover/khách sạn, cover/loại phòng và avatar/user chỉ được nhắc trong Note | Cần partial UNIQUE phù hợp; UNIQUE display_order không thay ràng buộc một cover |
| DB-04 | Chưa có ràng buộc chống chồng lịch phân công/bảo trì hoặc số phân công vượt quantity | Hoàn thiện ràng buộc và xử lý đồng thời; đủ số phòng mỗi ngày chưa chứng minh có phòng liên tục cả kỳ |
| DB-05 | Booking chưa có khóa chống tạo trùng, snapshot giờ/chính sách và thông tin đến muộn có cấu trúc | Bổ sung khi triển khai các quy tắc tương ứng; booking_code duy nhất không chống gửi lại cùng yêu cầu |
| DB-06 | FK `promotions.hotel_id` dùng ON DELETE SET NULL, trong khi NULL có nghĩa toàn hệ thống | Không xóa hotel theo cách làm voucher riêng thành voucher toàn hệ thống; đề xuất RESTRICT hoặc cơ chế vô hiệu hóa phù hợp |
| DB-07 | Voucher có amount snapshot nhưng chưa có snapshot mã/điều kiện; usage_limit/per_user_limit cần logic | Giữ tiền đơn đã chấp nhận; không suy ra an toàn hạn mức chỉ từ UNIQUE booking_id |
| DB-08 | Refund, review và redemption có các FK riêng đến booking/user/hotel | Backend hoặc ràng buộc kết hợp kiểm tra đúng người/khách sạn/khoản thu và tổng hoàn |
| DB-09 | `users.avatar_url` còn tồn tại cùng `user_images.is_avatar` | Chọn một nguồn avatar hiện hành; không tự đồng bộ hai nơi mà không có quy tắc |

Các điểm trên được ghi nhận để nhóm duyệt, không sửa file DBML đính kèm hoặc database trong lần cập nhật docs này.

## 5. Liên hệ tài liệu nghiệp vụ

**Bổ sung nghiệp vụ đã chốt ngày 22/10/2026, chưa có trong DBML nguồn:** nền tảng thu hộ/giữ khoản chưa chi và chi trả Owner; thêm `hotel_contracts` cho thỏa thuận hoa hồng riêng, `booking_financials` cho phần tài chính hai bên theo booking; thêm `funding_source` (PLATFORM/HOTEL) cho `promotions` và lưu lại ở `promotion_redemptions`. Xem [mục 5.4 giao dịch ba bên](../business/platform-transaction-rules-proposal.md#54-các-bổ-sung-dữ-liệu-đã-được-chốt). Schema/ràng buộc, chi trả và điều chỉnh cần đặc tả tiếp; giữ nguyên số đếm 37 bảng/từ điển/ERD theo DBML đã nhận, chưa có migration cho các bổ sung này. Đặt cọc một phần tiền chưa được chọn; vẫn thu đủ 100% để xác nhận booking.

[Phạm vi và quy tắc theo module](../business/module-scope-and-rules-draft.md) mô tả vai trò và hành vi mong muốn. Voucher đã được rút gọn theo yêu cầu mới; các lựa chọn như làm tròn, hoàn lượt sau khi đã thanh toán được đánh dấu cần chốt nếu chưa có quyết định.

[Báo cáo đối chiếu ngày 19/10](../business/database-fit-review.md) là lịch sử đánh giá bản 33 bảng, không phải kết luận hiện tại của bản 37 bảng. Ảnh ERD cũ được giữ nguyên, không thay thế DBML mới làm nguồn cấu trúc.
