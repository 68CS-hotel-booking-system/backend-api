# Báo cáo vận hành

## API

- `GET /api/v1/management/reports/operations?from=YYYY-MM-DD&to=YYYY-MM-DD&hotelId=<uuid tùy chọn>`
- Quyền: tài khoản đang hoạt động, đã xác minh email, có role `ADMIN` hoặc `HOTEL_OWNER`. Trả `Cache-Control: no-store`.
- Phạm vi dữ liệu:
  - `ADMIN`: toàn bộ khách sạn trên hệ thống.
  - `HOTEL_OWNER`: chỉ các khách sạn thuộc quyền sở hữu hiện tại. Truyền `hotelId` ngoài phạm vi trả `404`.
- Giới hạn khoảng ngày: bắt buộc có `from` và `to`, `from <= to` và khoảng thời gian tối đa `366` ngày; vi phạm trả `400 INVALID_DATE_RANGE`.

## Định nghĩa chỉ số

Mọi mốc ngày tính theo múi giờ `Asia/Ho_Chi_Minh`, bao gồm cả ngày `from` và `to`.

| Chỉ số | Cách tính |
| --- | --- |
| `availableRoomNights` | Tổng số đêm phòng vật lý đang vận hành trong `[from, to]` (`operational_from <= d` và `operational_until IS NULL OR operational_until > d`). **Không trừ `room_blocks`.** |
| `soldRoomNights` | Tổng `booking_items.quantity` trên `booking_night_prices.stay_date` thuộc `[from, to]` của các đơn có trạng thái `CONFIRMED`, `CHECKED_IN`, `CHECKED_OUT`. |
| `roomRevenue` | Tổng `quantity * unit_price` (tiền phòng **trước giảm giá**, đơn vị VND) theo cùng tập đêm đã bán. |
| `occupancyRate` | `soldRoomNights / availableRoomNights` (giá trị `0..1`, làm tròn 4 chữ số thập phân; `null` nếu `availableRoomNights = 0`). |
| `adr` | `roomRevenue / soldRoomNights` (VND, làm tròn xuống; `null` nếu `soldRoomNights = 0`). |
| `revpar` | `roomRevenue / availableRoomNights` (VND, làm tròn xuống; `null` nếu `availableRoomNights = 0`). |
| `completedRevenue` | Tổng `bookings.total_amount` của các đơn `CHECKED_OUT` có ngày `check_out` thuộc `[from, to]`. |
| `newBookings` | Số đơn có `created_at` (giờ VN) thuộc `[from, to]`, loại trừ `PENDING_PAYMENT` và `EXPIRED`. |
| `cancelledBookings` | Số lượt chuyển sang `CANCELLED` trong `booking_status_history` có `created_at` (giờ VN) thuộc `[from, to]`. |
| `noShowBookings` | Số đơn trạng thái `NO_SHOW` có ngày `check_in` thuộc `[from, to]`. |

## Cấu trúc phản hồi

- `totals`: tổng hợp toàn khoảng `[from, to]`.
- `months`: đầy đủ từng tháng `YYYY-MM` trong `[from, to]` (tháng không phát sinh giao dịch vẫn trả giá trị `0`).
- `hotels`: phân rã theo từng khách sạn chịu bộ lọc `hotelId`.
- `scopeHotels`: toàn bộ khách sạn trong phạm vi quyền của người gọi để dựng bộ lọc trên FE.
