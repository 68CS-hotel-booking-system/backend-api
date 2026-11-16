# Báo cáo thay đổi backend voucher

**Ngày lập báo cáo** 16/11/2026

**Phạm vi** Phần voucher trong working tree của `backend-api`, đối chiếu với commit `76b94dd`. Tổng cộng **43 file**, gồm **23 file mới** và **20 file sửa**.

## Hành vi theo vòng đời booking

1. **Báo giá.** Backend tính lại giá lưu trú, kiểm tra mã và trả tiền trước giảm, khoản giảm và tổng phải trả. Bước này chưa giữ lượt.
2. **Tạo booking.** Backend khóa bản ghi voucher trước khi kiểm tra hạn mức tổng và theo khách. Booking, giữ phòng và lượt `APPLIED` được ghi trong cùng giao dịch. Nếu giữ phòng thất bại, giao dịch rollback cả booking và khóa tạo đơn. Bản ghi áp mã lưu mã, khoản giảm và nguồn tài trợ tại thời điểm đặt.
3. **Gửi lại yêu cầu.** Khi cùng khách gửi lại cùng nội dung và cùng `Idempotency-Key`, backend trả booking đã tạo. Dùng lại khóa với nội dung khác trả `409 BOOKING_REQUEST_CONFLICT`.
4. **Hủy hoặc hết hạn.** Booking chờ thanh toán chuyển sang `CANCELLED` hoặc `EXPIRED`, phòng được trả và lượt voucher chuyển sang `CANCELLED`. Tác vụ định kỳ xử lý đơn có voucher đã quá hạn. Hạn mức đã loại lượt giữ quá hạn ngay khi tính, kể cả trước lần quét tiếp theo.

Voucher hỗ trợ giảm theo phần trăm hoặc số tiền cố định, mức chi tối thiểu và trần giảm. Giảm theo phần trăm được làm tròn xuống đến đồng. Tổng phải trả sau giảm phải lớn hơn 0. Phạm vi trả lượt của đợt triển khai này là booking chờ thanh toán. Trả lượt sau thanh toán thuộc luồng hoàn tiền.

## Hợp đồng API

| Phương thức | Đường dẫn | Thay đổi |
| --- | --- | --- |
| POST | `/api/v1/bookings/voucher-quote` | Nhận kỳ lưu trú, danh sách phòng và `voucherCode`, trả báo giá sau giảm. |
| POST | `/api/v1/bookings` | Nhận thêm `voucherCode` và header tùy chọn `Idempotency-Key`. Nếu gửi `quotedTotalAmount`, gửi tổng sau giảm. |
| POST | `/api/v1/bookings/{bookingId}/cancel` | Người đặt hủy booking chờ thanh toán của mình. |
| POST và PUT | `/api/v1/admin/vouchers/{code}` | Quản trị tạo và cập nhật mã toàn hệ thống. |
| POST và PUT | `/api/v1/management/hotels/{hotelId}/vouchers/{code}` | Quản trị, chủ khách sạn hoặc quản lý được phân công tạo và cập nhật mã của khách sạn. Chủ khách sạn và quản lý dùng nguồn tài trợ `HOTEL`. |

## Thay đổi cơ sở dữ liệu

| Bảng mới | Mục đích |
| --- | --- |
| `promotions` | Lưu cấu hình mã, phạm vi, hiệu lực, mức giảm, hạn mức và nguồn tài trợ. |
| `promotion_redemptions` | Lưu một bản ghi áp mã cho mỗi booking, có trạng thái và thời điểm trả lượt. |
| `booking_creation_requests` | Ràng buộc khóa tạo đơn theo khách và liên kết với booking tương ứng. |

## Danh sách file thay đổi

### API và tài liệu

| File | Loại | Nội dung thay đổi |
| --- | --- | --- |
| [README.md](README.md) | Sửa | Ghi hợp đồng API voucher, quyền, tham số cấu hình và cách xử lý lượt. |
| [BookingController.java](src/main/java/com/team/hotelbooking/booking/api/controller/BookingController.java) | Sửa | Nhận header `Idempotency-Key` khi tạo booking. |
| [BookingCheckoutController.java](src/main/java/com/team/hotelbooking/booking/api/controller/BookingCheckoutController.java) | Mới | Thêm endpoint báo giá voucher và hủy booking cho người đặt. |
| [VoucherManagementController.java](src/main/java/com/team/hotelbooking/booking/api/controller/VoucherManagementController.java) | Mới | Thêm endpoint tạo và cập nhật voucher toàn hệ thống hoặc theo khách sạn. |
| [CreateBookingRequest.java](src/main/java/com/team/hotelbooking/booking/api/dto/request/CreateBookingRequest.java) | Sửa | Nhận `voucherCode`, chuyển dữ liệu tạo đơn và kiểm tra phần tử trong danh sách phòng. |
| [QuoteVoucherRequest.java](src/main/java/com/team/hotelbooking/booking/api/dto/request/QuoteVoucherRequest.java) | Mới | Kiểm tra dữ liệu kỳ lưu trú và mã voucher, sau đó chuyển sang đầu vào tính giá. |
| [SaveVoucherRequest.java](src/main/java/com/team/hotelbooking/booking/api/dto/request/SaveVoucherRequest.java) | Mới | Kiểm tra và chuyển cấu hình giảm giá sang command. |
| [VoucherQuoteResponse.java](src/main/java/com/team/hotelbooking/booking/api/dto/response/VoucherQuoteResponse.java) | Mới | Trả mã, tiền tệ, tiền trước giảm, khoản giảm và tiền phải trả. |
| [SaveVoucherResponse.java](src/main/java/com/team/hotelbooking/booking/api/dto/response/SaveVoucherResponse.java) | Mới | Trả ID, phạm vi khách sạn, mã và trạng thái voucher. |
| [BookingExceptionHandler.java](src/main/java/com/team/hotelbooking/booking/api/exception/BookingExceptionHandler.java) | Sửa | Ánh xạ lỗi voucher, hủy đơn và trùng khóa tạo đơn sang HTTP. |

### Nghiệp vụ booking và voucher

| File | Loại | Nội dung thay đổi |
| --- | --- | --- |
| [CreateBookingCommand.java](src/main/java/com/team/hotelbooking/booking/application/command/CreateBookingCommand.java) | Sửa | Thêm mã voucher và khóa tạo đơn, giữ constructor cho caller hiện có. |
| [SaveVoucherCommand.java](src/main/java/com/team/hotelbooking/booking/application/command/SaveVoucherCommand.java) | Mới | Định nghĩa dữ liệu cấu hình, phạm vi và người thao tác. |
| [BookingCreationRequestPort.java](src/main/java/com/team/hotelbooking/booking/application/port/BookingCreationRequestPort.java) | Mới | Khai báo cổng ghi nhận khóa tạo đơn và tìm booking đã tạo. |
| [BookingHotelPort.java](src/main/java/com/team/hotelbooking/booking/application/port/BookingHotelPort.java) | Sửa | Khai báo kiểm tra quyền quản lý voucher theo khách sạn. |
| [BookingInventoryPort.java](src/main/java/com/team/hotelbooking/booking/application/port/BookingInventoryPort.java) | Sửa | Khai báo thao tác trả phòng khi hủy hoặc hết hạn. |
| [SaveVoucherResult.java](src/main/java/com/team/hotelbooking/booking/application/result/SaveVoucherResult.java) | Mới | Định nghĩa kết quả tạo hoặc cập nhật voucher. |
| [VoucherQuoteResult.java](src/main/java/com/team/hotelbooking/booking/application/result/VoucherQuoteResult.java) | Mới | Định nghĩa kết quả tính giá sau giảm. |
| [VoucherApplicationService.java](src/main/java/com/team/hotelbooking/booking/application/service/VoucherApplicationService.java) | Mới | Kiểm tra điều kiện sử dụng và hạn mức tổng, hạn mức từng khách. |
| [CancelBookingUseCase.java](src/main/java/com/team/hotelbooking/booking/application/usecase/CancelBookingUseCase.java) | Mới | Khai báo thao tác hủy booking của người đặt. |
| [QuoteVoucherUseCase.java](src/main/java/com/team/hotelbooking/booking/application/usecase/QuoteVoucherUseCase.java) | Mới | Khai báo thao tác báo giá với voucher. |
| [SaveVoucherUseCase.java](src/main/java/com/team/hotelbooking/booking/application/usecase/SaveVoucherUseCase.java) | Mới | Khai báo thao tác lưu cấu hình voucher. |
| [CancelBookingService.java](src/main/java/com/team/hotelbooking/booking/application/usecase/impl/CancelBookingService.java) | Mới | Kiểm tra chủ đơn và trạng thái, đóng booking chờ thanh toán rồi trả phòng và lượt. |
| [CreateBookingService.java](src/main/java/com/team/hotelbooking/booking/application/usecase/impl/CreateBookingService.java) | Sửa | Xử lý yêu cầu lặp, tính lại giá, áp mã và ghi lượt cùng giao dịch giữ phòng. |
| [QuoteVoucherService.java](src/main/java/com/team/hotelbooking/booking/application/usecase/impl/QuoteVoucherService.java) | Mới | Dùng giá lưu trú từ backend để tính số tiền sau giảm. |
| [SaveVoucherService.java](src/main/java/com/team/hotelbooking/booking/application/usecase/impl/SaveVoucherService.java) | Mới | Kiểm tra quyền, phạm vi, nguồn tài trợ và tính hợp lệ của cấu hình. |
| [BookingErrorCode.java](src/main/java/com/team/hotelbooking/booking/domain/exception/BookingErrorCode.java) | Sửa | Khai báo mã lỗi voucher, hủy booking và xung đột khóa tạo đơn. |
| [Voucher.java](src/main/java/com/team/hotelbooking/booking/domain/model/Voucher.java) | Mới | Chuẩn hóa mã, kiểm tra hiệu lực, tính tiền giảm và áp trần. |
| [BookingRepository.java](src/main/java/com/team/hotelbooking/booking/domain/repository/BookingRepository.java) | Sửa | Khai báo khóa booking và đóng booking chờ thanh toán. |
| [VoucherRepository.java](src/main/java/com/team/hotelbooking/booking/domain/repository/VoucherRepository.java) | Mới | Khai báo đọc cấu hình, đếm lượt, ghi lượt và trả lượt. |

### Hạ tầng, liên module và cơ sở dữ liệu

| File | Loại | Nội dung thay đổi |
| --- | --- | --- |
| [BookingRoomHoldGateway.java](src/main/java/com/team/hotelbooking/booking/BookingRoomHoldGateway.java) | Sửa | Bổ sung hợp đồng trả phòng giữa Booking và Inventory. |
| [BookingConfig.java](src/main/java/com/team/hotelbooking/booking/infrastructure/config/BookingConfig.java) | Sửa | Nối use case, thiết lập biên giao dịch và bật tác vụ xử lý hết hạn. |
| [VoucherHoldExpiryJob.java](src/main/java/com/team/hotelbooking/booking/infrastructure/config/VoucherHoldExpiryJob.java) | Mới | Quét và xử lý theo lô booking có voucher đã quá hạn. |
| [BookingHotelAdapter.java](src/main/java/com/team/hotelbooking/booking/infrastructure/integration/BookingHotelAdapter.java) | Sửa | Dùng quyền từ Hotel Directory để kiểm tra phạm vi quản lý voucher. |
| [BookingInventoryAdapter.java](src/main/java/com/team/hotelbooking/booking/infrastructure/integration/BookingInventoryAdapter.java) | Sửa | Chuyển yêu cầu trả phòng sang cổng Inventory. |
| [BookingRoomHoldGatewayAdapter.java](src/main/java/com/team/hotelbooking/inventory/infrastructure/integration/BookingRoomHoldGatewayAdapter.java) | Sửa | Gọi Inventory với lý do `CANCELLED` hoặc `EXPIRED`. |
| [JpaBookingRepositoryAdapter.java](src/main/java/com/team/hotelbooking/booking/infrastructure/persistence/adapter/JpaBookingRepositoryAdapter.java) | Sửa | Khóa và cập nhật booking chờ thanh toán, ghi lịch sử trạng thái. |
| [BookingJpaRepository.java](src/main/java/com/team/hotelbooking/booking/infrastructure/persistence/repository/BookingJpaRepository.java) | Sửa | Thêm truy vấn khóa ghi theo ID booking. |
| [JdbcVoucherRepositoryAdapter.java](src/main/java/com/team/hotelbooking/booking/infrastructure/persistence/adapter/JdbcVoucherRepositoryAdapter.java) | Mới | Khóa cấu hình khi cần, đếm lượt, lưu cấu hình và ghi hoặc trả lượt. |
| [JdbcBookingCreationRequestAdapter.java](src/main/java/com/team/hotelbooking/booking/infrastructure/persistence/adapter/JdbcBookingCreationRequestAdapter.java) | Mới | Giữ khóa tạo đơn theo khách, đối chiếu hash dữ liệu và trả ID booking cũ. |
| [V20261116100000000__create_voucher_tables.sql](src/main/resources/db/migration/V20261116100000000__create_voucher_tables.sql) | Mới | Tạo bảng cấu hình voucher, lượt áp mã và khóa tạo đơn. |

### Kiểm thử

| File | Loại | Nội dung thay đổi |
| --- | --- | --- |
| [InventoryConcurrencyIntegrationTests.java](src/test/java/com/team/hotelbooking/InventoryConcurrencyIntegrationTests.java) | Sửa | Kiểm tra PostgreSQL cho báo giá, yêu cầu lặp, tranh lượt cuối, rollback, hủy và hết hạn. |
| [BookingControllerResponseTests.java](src/test/java/com/team/hotelbooking/booking/api/controller/BookingControllerResponseTests.java) | Sửa | Cập nhật dữ liệu request và kiểm tra hợp đồng phản hồi tạo đơn hiện có. |
| [BookingServicesTests.java](src/test/java/com/team/hotelbooking/booking/application/usecase/impl/BookingServicesTests.java) | Sửa | Kiểm tra áp trần giảm giá, ghi lượt và bàn giao số tiền sau giảm. |

## Kết quả kiểm thử

- `fmt:check` và `clean verify` thành công. Kết quả Surefire của đợt triển khai có **654 kiểm thử**, **0 lỗi**, **0 thất bại**, **0 bỏ qua**.
- Bộ `InventoryConcurrencyIntegrationTests` chạy **14 kiểm thử** trên PostgreSQL. Ba ca voucher kiểm tra báo giá và hủy, tranh lượt cuối và hết hạn, cùng rollback khi hết phòng và thử lại bằng cùng khóa.
- Báo cáo này đối chiếu kết quả kiểm thử đã lưu và kiểm tra danh sách file với Git. Lượt cập nhật tài liệu này chỉ thêm báo cáo Markdown.
