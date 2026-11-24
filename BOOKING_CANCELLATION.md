# Hủy đơn chưa thanh toán

`POST /api/v1/bookings/{bookingId}/cancel` dùng access token của khách sở hữu đơn; không có request body.

- Chỉ chuyển đơn `PENDING_PAYMENT` sang `CANCELLED` và giải phóng giữ phòng.
- Nếu thời điểm xử lý đã đến hạn giữ phòng, chuyển sang `EXPIRED`.
- Không cho hủy đơn đã xác nhận, đang ở, đã trả phòng hoặc no-show.
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

Xung đột trả `409` với mã `BOOKING_NOT_CANCELLABLE` hoặc `BOOKING_PAYMENT_IN_PROGRESS`. FE chỉ hiện nút hủy cho đơn chờ thanh toán và tải lại dữ liệu sau khi xử lý thành công.

Phạm vi hiện tại không bao gồm hủy đơn đã thanh toán, tính phí hủy hay hoàn tiền. Callback thanh toán đến muộn vẫn theo luồng VNPay hiện có; không được xác nhận lại đơn đã hủy/hết hạn.
