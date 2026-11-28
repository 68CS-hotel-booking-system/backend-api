# Nhân sự khách sạn và quyền vận hành

## Quy tắc phân công

- Nhân sự theo từng khách sạn được lưu trong bảng `hotel_members` với vai trò `HOTEL_MANAGER` (quản lý khách sạn) hoặc `RECEPTIONIST` (lễ tân). Đây là vai trò cấp khách sạn, không phải role hệ thống trong bảng `roles`.
- Chủ khách sạn (`owner_user_id`) được xem, thêm, thu hồi và kích hoạt lại cả `HOTEL_MANAGER` và `RECEPTIONIST` của khách sạn mình sở hữu.
- `HOTEL_MANAGER` đang hoạt động chỉ được quản lý `RECEPTIONIST` trong cùng khách sạn, không được thêm `HOTEL_MANAGER` và không được tự sửa phân công của chính mình.
- `RECEPTIONIST` và `ADMIN` không có quyền quản lý nhân sự (`403`). Người không thuộc khách sạn nhận `404`.
- Tài khoản được thêm phải đang hoạt động (`ACTIVE`), đã xác minh email, không phải chủ sở hữu khách sạn, không phải chính người thao tác và không có role `ADMIN`; vi phạm trả `422 STAFF_ACCOUNT_NOT_ELIGIBLE` gắn lỗi vào trường `email`.
- Mỗi tài khoản chỉ có tối đa một phân công đang hoạt động (`ACTIVE`) tại cùng một khách sạn; trùng trả `409 HOTEL_MEMBER_ALREADY_EXISTS`. Thêm lại cùng vai trò đã thu hồi sẽ kích hoạt lại bản ghi cũ.
- Cập nhật trạng thái (`ACTIVE` / `REVOKED`) yêu cầu `expectedVersion`; lệch phiên bản trả `409 HOTEL_MEMBER_VERSION_CONFLICT`. Không hỗ trợ đổi vai trò trực tiếp (thu hồi rồi phân công vai trò mới).

## Bảng API

Tất cả endpoint yêu cầu đăng nhập và trả header `Cache-Control: no-store`.

| Method | Path | Quyền / Chức năng |
| --- | --- | --- |
| `GET` | `/api/v1/management/staff-hotels?page&size` | Danh sách khách sạn người gọi được quản lý nhân sự kèm cờ `canAssignManager` |
| `GET` | `/api/v1/management/hotels/{hotelId}/members?page&size` | Danh sách nhân sự theo khách sạn (kèm họ tên, email, vai trò, trạng thái, `version`) |
| `POST` | `/api/v1/management/hotels/{hotelId}/members` | Thêm nhân sự `{email, memberRole}` (trả `201`) |
| `PATCH` | `/api/v1/management/hotels/{hotelId}/members/{memberId}` | Cập nhật trạng thái `{status: "ACTIVE"\|"REVOKED", expectedVersion}` |
| `GET` | `/api/v1/me/workplaces` | Danh sách khách sạn và vai trò đang hoạt động của người gọi |

## Quyền vận hành của nhân viên

- Nhân viên có phân công đang hoạt động được truy cập khu vực vận hành cho đúng khách sạn được giao:
  - `GET /api/v1/management/hotels` và `GET /api/v1/management/hotels/{hotelId}` (trả thêm `accessLevel`: `ADMIN`, `OWNER`, `MANAGER`, `RECEPTIONIST`).
  - `GET /api/v1/management/hotels/{hotelId}/bookings`, `/{bookingId}`, `GET .../rooms`, `GET .../room-types`.
  - Nghiệp vụ lưu trú (`room-assignments`, `check-in`, `check-out`, `late-arrival`, `no-show`, `readiness`) và xem `room-blocks`, `room-calendar`.
- Không mở quyền ghi `room-blocks`, quản lý voucher, hoàn tiền, khiếu nại, doanh thu/báo cáo hay hợp đồng cho nhân viên.
- Khi bị thu hồi (`REVOKED`), nhân viên mất quyền vận hành ngay lập tức.
