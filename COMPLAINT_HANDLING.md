# Xử lý khiếu nại

## Phạm vi

- Khách vẫn gửi khiếu nại qua `POST /api/v1/bookings/{bookingId}/complaints` và xem tại `/api/v1/me/complaints`.
- Admin xem/xử lý toàn hệ thống. Owner chỉ xem/xử lý khiếu nại gắn khách sạn hiện thuộc mình, gồm khách sạn ngừng bán hoặc hết hợp đồng. Khi khách sạn đổi chủ, quyền xử lý chuyển theo chủ hiện tại.
- API quản lý đối chiếu tài khoản đang hoạt động, đã xác minh email và role thực trong DB. Khách/nhân viên không được dùng API này. Đối tượng ngoài phạm vi trả 404; tài khoản thiếu quyền trả 403.
- Liên hệ người đặt được lấy từ snapshot booking, không lấy người lưu trú hay profile hiện tại.

## API quản lý

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| GET | `/api/v1/management/complaints?page=0&size=10` | Danh sách mới nhất trước; lọc `status`, `hotelId`; tìm `keyword` theo mã đơn, tên người đặt hoặc nội dung (tối đa 100 ký tự) |
| GET | `/api/v1/management/complaints/{id}` | Chi tiết, liên hệ người đặt và lịch sử |
| POST | `/api/v1/management/complaints/{id}/actions` | Cập nhật trạng thái và ghi phản hồi công khai |

```json
{
  "status": "IN_REVIEW",
  "response": "Khách sạn đã tiếp nhận và đang kiểm tra thông tin.",
  "expectedVersion": 0
}
```

- `OPEN` chỉ chuyển sang `IN_REVIEW`.
- `IN_REVIEW` có thể giữ nguyên để gửi phản hồi tiếp, hoặc chuyển `RESOLVED`/`REJECTED` để đóng.
- `RESOLVED`/`REJECTED` không sửa hoặc mở lại. Theo quy tắc cũ, booking có thể tạo khiếu nại mới sau khi khiếu nại trước đã đóng.
- Mỗi hành động yêu cầu phản hồi sau trim dài 1–2000 ký tự và `expectedVersion` không âm.
- Phiên bản cũ trả 409 `COMPLAINT_VERSION_CONFLICT`; chuyển trạng thái không hợp lệ trả 409 `INVALID_COMPLAINT_TRANSITION`. FE giữ nội dung khi lỗi, yêu cầu tải phiên bản mới trước khi gửi lại khi xung đột.
- Cập nhật trạng thái/phiên bản và chèn lịch sử trong cùng transaction; cập nhật có điều kiện version để chỉ một request cùng phiên bản thành công. Ghi lịch sử thất bại sẽ rollback trạng thái.
- Lịch sử lưu ID người thực hiện, vai trò thực, trạng thái trước/sau, phản hồi, thời điểm từ Clock và phiên bản. API công khai vai trò người phản hồi, không công khai ID người thực hiện.
- `ComplaintResponse` bổ sung `version`, `history`. Danh sách/response tạo mới trả `history: []`; API chi tiết trả toàn bộ lịch sử theo version tăng dần.

## Giao diện

`/complaints` dành cho admin/owner có tìm kiếm, lọc trạng thái, phân trang, lỗi/thử lại và liên kết chi tiết `/complaints/{id}`. Trang chi tiết có nội dung, liên hệ người đặt, lịch sử và form xử lý. Khách xem phản hồi trong chi tiết tại `/my-complaints`.

Migration mới `V20261124180000000__complaint_handling_history.sql` tạo bảng `complaint_actions` và index truy vấn; không thay đổi migration cũ, không tự thêm lịch sử cho khiếu nại đã có.

Không triển khai đính kèm bằng chứng, thông báo email hoặc tự động hoàn tiền/bồi thường. Đóng khiếu nại không thay đổi booking, giao dịch thanh toán hay khoản chia doanh thu.

## Kiểm thử

`ComplaintHandlingIntegrationTests` kiểm phạm vi admin/owner, JWT role cũ, tài khoản không hoạt động/chưa xác minh, quyền sau đổi chủ, vòng đời/phản hồi, quyền đọc của khách, tìm ký tự đặc biệt, phân trang, version conflict, hai người cập nhật cùng lúc và rollback khi ghi lịch sử lỗi. FE có kiểm thử API/schema, màn hình đóng khiếu nại, phản hồi dạng văn bản và kiểm tra trình duyệt với API fixture.
