# Hủy đơn và hoàn tiền

## Đơn đã thanh toán

Khách được hủy ngay đơn `CONFIRMED` có tiền đã thu, chưa nhận phòng và **trước** `checkIn + expectedCheckInTime` theo `Asia/Ho_Chi_Minh`. Đúng giờ nhận phòng trở đi, hoặc đơn `CHECKED_IN/CHECKED_OUT/NO_SHOW`, không áp dụng. Hệ thống tự xác nhận hủy, không cần owner/admin duyệt; hoàn 100% khoản `SUCCEEDED` thực thu, phí hủy bằng 0.

`GET /api/v1/bookings/{bookingId}/cancellation` trả `canCancel`, `message`, `expectedVersion`, `refundAmount`, `currency`, `cutoff`. Khách khác nhận 404. FE hiển thị chính sách và số tiền trước khi xác nhận.

`POST /api/v1/bookings/{bookingId}/cancel` với đơn đã thanh toán cần body:

```json
{"expectedVersion": 1, "expectedRefundAmount": 1000000, "reason": "Thay đổi lịch trình"}
```

`reason` không bắt buộc, tối đa 1000 ký tự. Version/số tiền phải khớp dữ liệu server; thay đổi hoặc quá hạn trả 409 và phải lấy lại chính sách. Kết quả vẫn có cấu trúc `cancellation` bên dưới, nhưng `refundAmount` là tiền thực thu và `refundStatus` là `PENDING/PROCESSING/COMPLETED`.

Hủy, lịch sử trạng thái, giải phóng tồn và phòng đã gán, tạo khoản hoàn nằm cùng transaction. Khóa booking trước payment để tuần tự với VNPay/check-in. Không đổi snapshot hợp đồng; đơn hủy không phát sinh khoản chia lưu trú.

## Sổ hoàn tiền thủ công

- Mỗi payment có tối đa một khoản `payment_refunds`; giữ giao dịch gốc `SUCCEEDED`, số tiền/currency bất biến.
- VNPay thu tiền nhưng không xác nhận được booking tạo hoàn `UNCONFIRMED_PAYMENT` trong cùng transaction. Callback lặp không tạo thêm; migration đưa các khoản thu cũ chưa xác nhận booking vào sổ chờ hoàn.
- Trạng thái: `PENDING → PROCESSING → COMPLETED`. Chỉ admin thực tế trong DB được cập nhật, owner chỉ xem các khách sạn hiện thuộc mình, kể cả khách sạn ngừng bán. Khách chỉ xem khoản của mình.
- Tiếp nhận cần ghi chú cho khách. Ghi nhận đã hoàn cần mã giao dịch thực tế (duy nhất không phân biệt hoa/thường) và ghi chú; lưu actor/thời điểm/chuyển trạng thái. Mọi cập nhật cần `expectedVersion`; xung đột trả 409.
- Phần mềm không chuyển tiền. Admin hoàn qua kênh thanh toán/ngân hàng bên ngoài rồi ghi nhận kết quả; không cần liên kết thẻ khách.

| API | Quyền |
| --- | --- |
| `GET /api/v1/me/refunds` và `/{id}` | Khách xem khoản của mình |
| `GET /api/v1/management/refunds` và `/{id}` | Admin toàn hệ thống; owner trong phạm vi khách sạn |
| `POST /api/v1/admin/refunds/{id}/actions` | Chỉ admin |

Danh sách nhận `page`, `size`, `status`, `bookingId`, `keyword` (mã đơn/tên người đặt, tối đa 100 ký tự). Không nhận ownerId/customerId từ FE để quyết định phạm vi.

```json
{"status": "PROCESSING", "note": "Đã tiếp nhận và liên hệ người đặt.", "expectedVersion": 0}
```

```json
{"status": "COMPLETED", "reference": "BANK-REF-123", "note": "Đã hoàn tiền cho khách.", "expectedVersion": 1}
```

## Dashboard và FE

`collectedAmount` là tổng thu; `pendingRefundAmount` là tổng đang chờ/xử lý; `refundedAmount` là tổng đã hoàn. `netCollectedAmount = collectedAmount - refundedAmount`, vẫn gồm tiền đang chờ hoàn, không phải số được phép chi owner. Báo cáo giữ phạm vi bên hưởng snapshot hợp đồng lịch sử như hiện tại; doanh thu/hoa hồng chỉ ghi nhận đơn đã trả phòng.

FE có `/refunds` cho admin/owner và `/my-refunds` cho khách. Owner không có form cập nhật. Khách mở chính sách hủy từ Đặt phòng của tôi. Form admin giữ bản nháp khi xung đột và yêu cầu tải lại phiên bản. Các API trả `Cache-Control: no-store`.

## Đơn chưa thanh toán

`POST /api/v1/bookings/{bookingId}/cancel` dùng access token của khách sở hữu đơn; vẫn hỗ trợ không có request body.

- Chỉ chuyển đơn `PENDING_PAYMENT` sang `CANCELLED` và giải phóng giữ phòng.
- Nếu thời điểm xử lý đã đến hạn giữ phòng, chuyển sang `EXPIRED`.
- Đơn đã xác nhận áp dụng chính sách đã thanh toán ở trên; đang ở, đã trả phòng hoặc no-show không được hủy.
- Chặn khi có giao dịch `SUCCEEDED` hoặc phiên thanh toán `PENDING` còn hiệu lực. Hệ thống chưa thu hồi được đường dẫn thanh toán VNPay đã phát hành.
- Khách khác nhận `404`; tài khoản không hoạt động bị từ chối.
- Khóa dòng booking trước khi kiểm tra thanh toán và cập nhật. Thao tác ghi lịch sử, đổi trạng thái và trả phòng nằm trong cùng transaction.
- Gọi lại đơn `CANCELLED`/`EXPIRED` trả trạng thái hiện tại, không thêm lịch sử hoặc trả phòng lần nữa (vẫn kiểm tra rủi ro thanh toán).

Ví dụ `data` thành công:

```json
{
  "status": "CANCELLED",
  "currency": "VND",
  "cancellation": {
    "cancellationFeeAmount": 0,
    "refundAmount": 0,
    "refundStatus": "NOT_REQUIRED"
  }
}
```

Xung đột trả `409` với mã `BOOKING_NOT_CANCELLABLE` hoặc `BOOKING_PAYMENT_IN_PROGRESS`. FE tải lại dữ liệu sau khi xử lý thành công.

Gọi lại đơn `CANCELLED/EXPIRED` có khoản hoàn trả trạng thái và tổng tiền hoàn hiện tại, không tạo thêm lịch sử hay giải phóng tồn lần nữa.
