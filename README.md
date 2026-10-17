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

## Trạng thái

Đã có khung module và các thành phần dùng chung. Security hiện dùng HTTP Basic; chưa triển khai JWT, API nghiệp vụ và
migration database.

Hiện chưa có entity nghiệp vụ nên ứng dụng vẫn chạy khi chưa có migration; Flyway chỉ cảnh báo
`No migrations found`. Khi thêm entity, cần migration tạo bảng tương ứng vì đang dùng `ddl-auto: validate`.
