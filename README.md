# Hotel Booking Backend

Backend đặt phòng khách sạn dùng Spring Boot, Java 21 và PostgreSQL, tổ chức theo Modular Monolith.

## Yêu cầu

- JDK 21, Maven.
- Docker có Docker Compose, Docker Engine đang chạy.

## Chạy local

Thực hiện tại thư mục gốc dự án bằng PowerShell:

1. Tạo `.env` từ file mẫu, điền thông tin database và mật khẩu local:

```powershell
Copy-Item .env.example .env
```

2. Khởi động PostgreSQL và backend:

```powershell
docker compose up -d --wait
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Profile `local` đọc `.env` tại thư mục làm việc và bật Swagger. Khi chạy bằng IntelliJ, vào
**Run → Edit Configurations**, đặt **Active profiles** là `local` và **Working directory** là thư mục gốc dự án.

Nếu chưa bật `local` và chưa cung cấp biến môi trường, ứng dụng có thể báo `'url' must start with "jdbc"`.

Với `SERVER_PORT=8080`:

- [Health](http://localhost:8080/actuator/health)
- [Swagger UI](http://localhost:8080/swagger-ui/index.html)

## Thanh toán VNPAY sandbox

Đã có API tạo/đọc giao dịch, ký URL, nhận IPN và xác nhận booking/tồn phòng trong cùng transaction.
Mặc định tắt; bật bằng `VNPAY_ENABLED=true` cùng merchant sandbox trong `.env`.
Xem [hướng dẫn cấu hình, API và test sandbox từng bước](docs/api/vnpay-sandbox.md).

## Format và kiểm tra

```powershell
mvn fmt:format
mvn verify
```

`verify` kiểm tra format, compile, chạy test và đóng gói. Test tích hợp cần Docker để Testcontainers tạo PostgreSQL
riêng.

## Cấu trúc

- Module: `identity`, `hotel`, `inventory`, `pricing`, `booking`, `payment`, `media`.
- Mỗi module chia thành `api`, `application`, `domain`, `infrastructure`.
- `shared`: response, xử lý lỗi, security và cấu hình chung.

## Hồ sơ xét duyệt khách sạn

[Hợp đồng API xét duyệt](docs/api/hotel-review-requests.md): Owner gửi snapshot, Admin xét duyệt,
quyền truy cập, lỗi, ví dụ request/response và tương thích với API approve cũ.

## Voucher

| Method | Endpoint | Hành vi |
| --- | --- | --- |
| POST | `/api/v1/bookings/voucher-quote` | Tài khoản đăng nhập gửi `hotelId`, `checkIn`, `checkOut`, `items`, `voucherCode` và giờ nhận/trả dự kiến nếu có. Trả tiền trước giảm, khoản giảm và tiền phải trả. Báo giá chưa giữ lượt. |
| POST | `/api/v1/bookings` | Có thể gửi thêm `voucherCode`. Nếu gửi `quotedTotalAmount`, dùng tổng sau giảm. Gửi lại cùng nội dung với cùng header `Idempotency-Key` sẽ nhận lại đơn cũ. Dùng lại khóa cho nội dung khác trả `409`. |
| POST | `/api/v1/bookings/{bookingId}/cancel` | Người đặt hủy đơn chờ thanh toán. Hủy và hết hạn trả phòng lẫn lượt voucher. |
| POST / PUT | `/api/v1/admin/vouchers/{code}` | Quản trị tạo hoặc cập nhật voucher toàn hệ thống. |
| POST / PUT | `/api/v1/management/hotels/{hotelId}/vouchers/{code}` | Quản trị, chủ khách sạn hoặc quản lý được phân công tạo hoặc cập nhật voucher của khách sạn. Quản lý và chủ khách sạn dùng nguồn tài trợ `HOTEL`. |

Body cấu hình gồm `name`, `discountType`, `startsAt`, `endsAt` và một trong `percentageOff` hoặc `fixedAmount` tương ứng với `PERCENTAGE` hoặc `FIXED_AMOUNT`. Có thể cấu hình `maxDiscountAmount`, `minBookingAmount`, `usageLimit`, `perUserLimit`, `status` và `fundingSource`. Mã mặc định ở trạng thái `DRAFT`. Đặt `ACTIVE` để khách có thể dùng trong khoảng hiệu lực. `PUT` thay toàn bộ cấu hình, giữ mã và phạm vi lấy từ URL.

Tiền giảm phần trăm được làm tròn xuống đến đồng. Mọi voucher phải tạo khoản giảm dương và giữ tổng phải trả lớn hơn 0. Hạn mức tính cả đơn đang giữ chỗ còn hạn và đơn đã dùng mã. Việc trả lượt sau khi đã thanh toán thuộc luồng hoàn tiền.

## Trạng thái

[Tài liệu tổng thể đề tài](docs/project-overview.md): giới thiệu, actor chính, nghiệp vụ từng phần, quy trình và phạm vi MVP.

[Kế hoạch 5 sprint](docs/five-sprint-plan.md): mỗi sprint 1 tuần; Sprint 1 đã chốt đề tài/DB/nghiệp vụ/hệ thống, Sprint 2–4 code, Sprint 5 test/deploy và bàn giao.

[File import Jira](docs/jira/README.md): 5 CSV riêng theo sprint, kèm trạng thái Done/To Do và hướng dẫn ánh xạ field, gán người/sprint sau import.

[Hướng dẫn CI/CD lên VPS Ubuntu](docs/ci-cd-setup.md): mẫu Dockerfile, GitHub Actions, GHCR, Compose staging và SSH secrets để tự cấu hình.

[API contract](docs/api/README.md), [OpenAPI YAML](docs/api/openapi.yaml) và [Convention](docs/convention.md): URL, request/response mẫu, quyền, lỗi và quy ước chung. [Phân công API](docs/api/work-allocation.md) bàn giao trong ba tuần lập trình tương ứng Sprint 2–4; Sprint 5 ổn định hệ thống, deploy và kiểm thử tổng thể.

Tài liệu nghiệp vụ đang đề xuất: [Phạm vi, vai trò và quy tắc theo module](docs/business/module-scope-and-rules-draft.md).
Bản nháp đã ghi nhận một phần quyết định nghiệp vụ; nội dung còn lại chờ duyệt, không phản ánh chức năng đã triển khai.

[Tài liệu database](docs/database/README.md) đã cập nhật theo DBML 37 bảng ngày 21/10/2026; đây là thiết kế được cung cấp, chưa phải schema đã chạy migration.

Backend xác thực bằng JWT. Flyway áp dụng schema trong `src/main/resources/db/migration` khi ứng dụng khởi động. Hibernate kiểm tra mapping với `ddl-auto: validate`.
