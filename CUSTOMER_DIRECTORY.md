# Màn hình khách hàng theo vai trò

## Owner — người đặt phòng

`GET /api/v1/management/customers?page=0&size=10&keyword=...`

Chỉ tài khoản HOTEL_OWNER đang hoạt động, đã xác minh email được truy cập. Phạm vi khách sạn lấy từ tài khoản đăng nhập, không nhận ownerId/hotelId để mở rộng phạm vi.

Mỗi `customer_id` có booking tại khách sạn thuộc owner xuất hiện một lần. Họ tên, email và điện thoại lấy từ snapshot **người đặt** của đơn gần nhất trong chính phạm vi này; không lấy người nhận phòng và không dùng đơn tại khách sạn của owner khác. Tổng đơn bao gồm mọi trạng thái, số đơn hoàn thành chỉ tính CHECKED_OUT. Khách sạn đã ngừng mở bán vẫn giữ lịch sử khách hàng cho owner hiện tại.

## Admin — owner đã ký hợp đồng

`GET /api/v1/admin/customers?page=0&size=10&keyword=...`

Chỉ ADMIN đang hoạt động, đã xác minh email được truy cập. Mỗi owner có hợp đồng từng được kích hoạt xuất hiện một lần, kể cả khi hợp đồng hết hạn/chấm dứt. Dự thảo chưa kích hoạt và dự thảo bị chấm dứt không được tính. Danh tính owner lấy từ `owner_user_id` của hợp đồng; thông tin liên hệ hiện tại được đọc qua public contract của Identity. Tài khoản đã xóa mềm không xuất hiện.

Migration `V20261124160000000__track_contract_activation.sql` lưu cờ `has_been_activated` và bảo toàn cờ khi chấm dứt. Backfill chỉ ACTIVE/EXPIRED có bằng chứng trạng thái. TERMINATED cũ chưa rõ lịch sử được để false và chưa đưa vào danh sách, theo quy tắc đã chọn. Không suy diễn dấu kích hoạt từ ngày bắt đầu/kết thúc hay ngày sửa gần nhất.

## Tìm kiếm và bảo vệ dữ liệu

Hai API tìm theo tên/email/số điện thoại, tối đa 100 ký tự; `%`, `_`, `\` được xử lý như ký tự thường. BE phân trang và đếm trên toàn bộ kết quả đủ điều kiện. Response dùng `{message,data}` với PageResponse và `Cache-Control: no-store`.

FE `/customers` tự chọn API theo vai trò, cache tách theo tài khoản và loại danh sách. Không có dữ liệu giả thay cho lỗi API, không có thao tác sửa tài khoản hoặc cấp quyền ở màn hình này.
