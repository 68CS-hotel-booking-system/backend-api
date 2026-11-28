# Voucher theo khách sạn

Owner hiện tại (tài khoản hoạt động, đã xác minh email, có HOTEL_OWNER và không có ADMIN) được tạo, xem, tạm dừng/bật lại mã. Admin không quản lý voucher. Mã unique trong từng khách sạn, chuẩn hóa chữ hoa, 3–30 ký tự A–Z/0–9/_/-. Khi chuyển chủ, quyền theo owner hiện tại.

Thông tin ưu đãi giữ cố định sau tạo; thay đổi chương trình bằng mã mới. Trạng thái có thể bật/tắt với expectedVersion. Không xóa mã đã dùng.

## API

- GET /api/v1/management/hotels/{hotelId}/vouchers?page=0&size=10
- POST cùng URL: {code,name,discountType,discountValue,maxDiscountAmount,minOrderAmount,startsAt,endsAt,usageLimit}
- PATCH /api/v1/management/hotels/{hotelId}/vouchers/{voucherId}: {enabled,expectedVersion}
- POST /api/v1/hotels/{hotelId}/quote và POST /api/v1/bookings nhận thêm voucherCode tùy chọn. Không đổi hành vi khi không có mã.

discountType = PERCENT (số nguyên 1–99) hoặc FIXED (VND nguyên dương). maxDiscountAmount tùy chọn cho phần trăm; FIXED không dùng trường này. minOrderAmount áp dụng trên tổng tiền phòng trước giảm; không áp dụng phụ thu. Giảm phần trăm làm tròn xuống VND, giới hạn theo maxDiscountAmount và luôn để lại ít nhất 1 VND tiền phòng. Một đơn dùng một mã; FE không tự tính mức giảm.

startsAt/endsAt là Instant, kiểm tại lúc tạo đơn: startsAt <= now < endsAt. Không phải điều kiện ngày lưu trú. FE nhập giờ Việt Nam rồi gửi UTC. Đơn đã tạo vẫn giữ mức giảm khi mã hết hạn/tạm dừng; thanh toán không kiểm lại chương trình.

## Lượt dùng, giao dịch và tiền

Khi tạo đơn: báo giá lại, insert booking, khóa voucher, kiểm điều kiện/lượt/số tiền giảm, lưu snapshot mã và điều khoản vào voucher_redemptions, chốt hoa hồng và giữ phòng trong cùng transaction. Bất kỳ lỗi nào rollback toàn bộ. Lượt tính gồm USED và RESERVED còn hạn; reservation hết hạn tự ngừng chiếm lượt mà không phụ thuộc scheduler.

Xác nhận thanh toán: khóa booking -> voucher -> lấy giờ mới -> kiểm hạn giữ phòng -> xác nhận tồn kho -> đánh dấu USED, cùng transaction thanh toán. Thứ tự này ngăn việc hết hạn giữa lúc xác nhận làm bán quá lượt. Tạm dừng voucher không hủy reservation đang có.

Hủy chưa thanh toán trả lượt ngay (RESERVED -> RELEASED). Thanh toán thất bại chưa kết thúc đơn vẫn giữ lượt đến hạn để khách thử lại. Đơn xác nhận đã thanh toán rồi hủy vẫn USED, hoàn đúng số tiền thực thu. Callback muộn không thể xác nhận đơn quá hạn; quy trình hoàn tiền hiện có tiếp tục xử lý tiền đến muộn.

Snapshot redemption chỉ được chuyển RESERVED sang USED hoặc RELEASED; DB trigger chặn sửa mã/điều khoản/số tiền/hạn giữ. Idempotency booking so khớp cả mã; gửi lại cùng yêu cầu không giữ lượt hoặc chốt hoa hồng thêm. Mã khác dù bằng tiền vẫn xung đột.

Hoa hồng theo (tiền phòng - discountAmount), dùng snapshot hợp đồng hiện có. VNPay nhận totalAmount sau giảm; hoàn tiền dùng số tiền thanh toán thực tế. FE /vouchers chỉ owner; ô mã tại bước xác nhận, có áp dụng/bỏ mã và xác nhận lại khi giá thay đổi. Khi kết quả tạo đơn chưa xác định, khóa đổi mã và giữ idempotency key để retry an toàn.
