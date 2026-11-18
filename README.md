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

## Đăng nhập và đăng ký bằng Google

Tạo OAuth Client ID loại **Web application** trong Google Cloud. Thêm chính xác origin của
frontend đang dùng vào **Authorized JavaScript origins**. Khi chạy local, thêm cả
`http://localhost` và `http://localhost:5173`. Với staging, thêm
`https://staging.hotelbooking.id.vn`. Nếu ứng dụng Google đang ở chế độ Testing,
thêm tài khoản kiểm thử vào danh sách Test users. Xem [hướng dẫn của Google](https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid).

Đặt `AUTH_GOOGLE_CLIENT_ID` trong môi trường backend và khởi động lại. Staging lấy giá trị
này từ file `.env` của môi trường thông qua Docker Compose. Frontend đọc cấu hình từ backend
khi mở form. Client ID là cấu hình công khai của nút Google. Nếu để trống, form chỉ hiển thị
đăng nhập và đăng ký bằng email.

Tài khoản mới dùng Gmail hoặc Google Workspace bổ sung số điện thoại trước khi hoàn tất.
Người dùng đã đăng ký bằng mật khẩu tiếp tục đăng nhập bằng mật khẩu hoặc đặt lại mật khẩu.

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

## Trạng thái

[Tài liệu tổng thể đề tài](docs/project-overview.md): giới thiệu, actor chính, nghiệp vụ từng phần, quy trình và phạm vi MVP.

[Kế hoạch 5 sprint](docs/five-sprint-plan.md): mỗi sprint 1 tuần; Sprint 1 đã chốt đề tài/DB/nghiệp vụ/hệ thống, Sprint 2–4 code, Sprint 5 test/deploy và bàn giao.

[File import Jira](docs/jira/README.md): 5 CSV riêng theo sprint, kèm trạng thái Done/To Do và hướng dẫn ánh xạ field, gán người/sprint sau import.

[Hướng dẫn CI/CD lên VPS Ubuntu](docs/ci-cd-setup.md): mẫu Dockerfile, GitHub Actions, GHCR, Compose staging và SSH secrets để tự cấu hình.

[API contract](docs/api/README.md), [OpenAPI YAML](docs/api/openapi.yaml) và [Convention](docs/convention.md): URL, request/response mẫu, quyền, lỗi và quy ước chung. [Phân công API](docs/api/work-allocation.md) bàn giao trong ba tuần lập trình tương ứng Sprint 2–4; Sprint 5 ổn định hệ thống, deploy và kiểm thử tổng thể.

Tài liệu nghiệp vụ đang đề xuất: [Phạm vi, vai trò và quy tắc theo module](docs/business/module-scope-and-rules-draft.md).
Bản nháp đã ghi nhận một phần quyết định nghiệp vụ; nội dung còn lại chờ duyệt, không phản ánh chức năng đã triển khai.

[Tài liệu database](docs/database/README.md) đã cập nhật theo DBML 37 bảng ngày 21/10/2026; đây là thiết kế được cung cấp, chưa phải schema đã chạy migration.

Đã có khung module và các thành phần dùng chung. Security hiện dùng HTTP Basic; chưa triển khai JWT, API nghiệp vụ và
migration database.

Hiện chưa có entity nghiệp vụ nên ứng dụng vẫn chạy khi chưa có migration; Flyway chỉ cảnh báo
`No migrations found`. Khi thêm entity, cần migration tạo bảng tương ứng vì đang dùng `ddl-auto: validate`.
