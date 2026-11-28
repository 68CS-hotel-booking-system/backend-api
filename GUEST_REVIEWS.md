# Đánh giá sau lưu trú

Khách sở hữu đơn `CHECKED_OUT`, tài khoản đang hoạt động và đã xác minh email, được gửi một đánh giá cho mỗi đơn. Điểm là số nguyên 1–5; nhận xét bắt buộc, tối đa 2000 ký tự. Khóa booking khi gửi và unique `booking_id` trong DB ngăn gửi trùng, kể cả đồng thời. Gửi lại trả `409 GUEST_REVIEW_ALREADY_EXISTS`.

Đánh giá lưu snapshot tên rút gọn, không thay đổi theo hồ sơ tài khoản sau này. Nội dung/điểm của khách không sửa sau khi gửi, được bảo vệ cả tại database. API công khai không trả customerId, bookingId, mã đơn, email, số điện thoại hoặc quyền quản lý. Nội dung người dùng hiển thị dưới dạng văn bản thuần trên FE.

| API | Chức năng |
| --- | --- |
| `GET /api/v1/bookings/{bookingId}/review` | Người đặt xem quyền đánh giá và đánh giá đã gửi |
| `POST /api/v1/bookings/{bookingId}/review` | Người đặt gửi `{rating, content}`; trả 201 |
| `GET /api/v1/hotels/{hotelId}/guest-reviews` | Danh sách công khai, chỉ khách sạn ACTIVE |
| `GET /api/v1/management/hotels/{hotelId}/guest-reviews` | Owner hiện tại hoặc admin xem danh sách theo khách sạn |
| `PUT /api/v1/management/hotels/{hotelId}/guest-reviews/{reviewId}/reply` | Owner hiện tại gửi/sửa phản hồi công khai |

Danh sách phân trang từ 0 (`page`, `size`), sắp xếp mới nhất rồi ID. `rating` lọc 1–5; danh sách quản lý có thêm `replied=true/false`. `summary` luôn tính trên toàn bộ đánh giá của khách sạn, gồm tổng số, trung bình làm tròn một chữ số thập phân và số lượt từng mức sao. Chưa có đánh giá thì trung bình là null.

Phản hồi nhận `{content, expectedVersion}`: nội dung bắt buộc tối đa 2000 ký tự. Kiểm vai trò và chủ khách sạn thật trong DB; admin chỉ xem. Owner cũ mất quyền khi chuyển chủ. Mỗi lần lưu phản hồi ghi người cập nhật, thời điểm, tăng version; cập nhật cạnh tranh trả `409 GUEST_REVIEW_VERSION_CONFLICT`. Sửa phản hồi không sửa nội dung/điểm của khách.

Khách sạn ngừng hoạt động không có danh sách đánh giá công khai, nhưng owner vẫn xem và phản hồi trong workspace. Hết hạn hợp đồng không xóa lịch sử đánh giá; khả năng công khai tuân theo trạng thái ACTIVE như trang chi tiết khách sạn hiện tại.

FE: nút “Đánh giá kỳ nghỉ” tại đơn đã trả phòng; form cũng nằm trong chi tiết đơn. Owner xem/phản hồi tại tab Đánh giá của workspace khách sạn. Trang chi tiết chỗ nghỉ hiển thị điểm trung bình, số lượt, nhận xét và phản hồi. Các trạng thái tải/lỗi/rỗng lấy từ API, không thay bằng dữ liệu mẫu; cache cá nhân được tách theo tài khoản.
