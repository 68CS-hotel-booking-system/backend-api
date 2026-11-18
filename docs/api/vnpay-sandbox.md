# VNPAY sandbox: API và cách kiểm thử

Luồng đã triển khai: tạo booking kèm giữ phòng → tạo/lấy lại URL VNPAY → VNPAY gọi IPN → lưu kết quả và xác nhận booking/tồn phòng trong cùng transaction. Trang return chỉ đọc trạng thái từ API, không cập nhật thanh toán từ query string.

### Hiển thị QR ngay trên website

Frontend yêu cầu `POST /api/v1/payments/bookings/{bookingId}/vnpay?qr=true` với phiên đăng nhập của chủ booking. Backend gọi VNPAY Merchant hosted QR bằng `vnp_Command=genqr`, ký HMAC-SHA512 và lưu `qrContent` trong cùng giao dịch thanh toán. Gọi lại trước hạn sẽ dùng lại mã đã lưu; không tạo payment mới chỉ để đổi cách hiển thị.

Frontend vẽ dữ liệu `qrContent` thành QR bằng component Ant Design có sẵn và đọc trạng thái giao dịch mỗi 5 giây tới khi kết thúc hoặc hết hạn. Chỉ IPN hợp lệ mới ghi nhận thanh toán/xác nhận booking. Khi payment thành công, thất bại hoặc hết hạn, API không trả lại QR để tiếp tục thanh toán.

Không truyền `qr=true` vẫn lấy URL thanh toán theo luồng cũ. Nút **Mở cổng thanh toán VNPAY** cho phép chuyển sang trang VNPAY khi muốn dùng phương thức khác. Với sandbox, có thể dùng nút này để tiếp tục test thẻ NCB theo hướng dẫn bên dưới; để quét QR sandbox cần công cụ/ứng dụng thử nghiệm do VNPAY cung cấp.

Merchant phải được VNPAY hỗ trợ Merchant hosted QR. Lỗi mạng, phản hồi không hợp lệ hoặc merchant không sinh được QR trả HTTP 502 `PAYMENT_QR_UNAVAILABLE`; không giả lập QR/thành công. Khi triển khai bản này cần chạy migration thêm `qr_content` qua Flyway trước khi frontend dùng tính năng.

## 1. Cấu hình merchant

Đăng ký merchant sandbox tại <https://sandbox.vnpayment.vn/devreg/> nếu chưa có mã kết nối. Điền `.env` trong thư mục backend; không đưa secret vào frontend hoặc Git:

```dotenv
VNPAY_ENABLED=true
VNPAY_TMN_CODE=<ma-merchant-sandbox>
VNPAY_HASH_SECRET=<secret-cua-merchant>
VNPAY_PAYMENT_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:5173/payment/return
```

Return URL trên dùng khi trình duyệt thử nghiệm chạy trên cùng máy với frontend. Nếu frontend có tên miền HTTPS, đổi thành `https://<frontend>/payment/return`. Giữ cùng hostname với lúc đăng nhập để cookie phiên được gửi đúng.

`VNPAY_ENABLED=false` mặc định: ứng dụng vẫn khởi động, API tạo thanh toán trả HTTP 503. Khi bật nhưng thiếu/sai cấu hình URL, ứng dụng báo lỗi lúc khởi động. `.env` có sẵn không bị ghi đè.

## 2. Chạy dịch vụ local

Yêu cầu JDK 21, Maven, Node 24 và PostgreSQL. Nếu dùng Docker Compose của repo, PostgreSQL được publish ở cổng **15432**, SMTP Mailpit ở **11025**:

```dotenv
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:15432/hotel_booking
MAIL_HOST=localhost
MAIL_PORT=11025
```

Giữ username/password khớp cấu hình PostgreSQL hiện tại. Nếu dùng DB cài riêng, dùng cổng thực tế của DB đó.

Tại backend:

```powershell
docker compose up -d --wait
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Nếu `spring-boot:run` báo `Could not find or load main class ...HotelBookingBackendApplication`
trong khi class đã được biên dịch, dùng JAR chạy trực tiếp từ thư mục `backend-api`:

```powershell
.\mvnw.cmd package "-DskipTests"
java -jar .\target\hotel-booking-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Chờ bước `package` báo `BUILD SUCCESS` rồi chạy lệnh `java`. Sau mỗi lần sửa code backend,
cần đóng gói lại JAR. `-DskipTests` chỉ bỏ chạy test trong bước đóng gói local.

Flyway tự tạo bảng `payment_transactions` bằng migration `V20261115120000000`. Profile `dev` bật Swagger và dữ liệu demo. Kiểm tra:

- Health: <http://localhost:8080/actuator/health>
- Swagger: <http://localhost:8080/swagger-ui/index.html>
- Mailpit: <http://localhost:8025>

Tại frontend:

```powershell
npm.cmd ci
# Chỉ copy khi chưa có .env.local:
if (!(Test-Path .env.local)) { Copy-Item .env.example .env.local }
npm.cmd run dev
```

`VITE_API_BASE_URL=/api/v1`, `API_PROXY_TARGET=http://localhost:8080`. Mở <http://localhost:5173>.

## 3. Đăng ký IPN URL

Expose backend qua một địa chỉ HTTPS công khai hoặc gateway/tunnel có HTTPS. Đăng ký với merchant sandbox:

```text
https://<public-backend>/api/v1/payments/vnpay/ipn
```

Gateway phải chuyển nguyên query string tới backend. Không đặt đăng nhập, Basic Auth hoặc trang xác nhận truy cập của tunnel trước endpoint IPN. Endpoint này không yêu cầu JWT; nó kiểm chữ ký HMAC-SHA512 và TmnCode. Các API tạo/đọc giao dịch vẫn yêu cầu đăng nhập.

`localhost` không phải địa chỉ IPN mà máy chủ VNPAY có thể gọi được. Return URL là cấu hình riêng, không thay thế IPN URL. Backend không tự đăng ký IPN bằng biến môi trường.

Kiểm tra từ ngoài máy: GET endpoint IPN không có tham số phải trả HTTP 200 với `{"RspCode":"97","Message":"Invalid Signature"}`, thay vì trang HTML, redirect đăng nhập, 401 hoặc 404.

Nếu dùng ngrok free và PowerShell nhận trang cảnh báo `ERR_NGROK_6024`, thêm header của ngrok vào **lệnh kiểm tra**:

```powershell
Invoke-RestMethod -Uri "https://<ten-mien-ngrok>/api/v1/payments/vnpay/ipn" `
  -Headers @{ "ngrok-skip-browser-warning" = "1" }
```

Header này chỉ áp dụng cho yêu cầu thử của bạn. Khi thanh toán, vẫn cần kiểm tra IPN do VNPAY gửi trong ngrok Inspector (`http://127.0.0.1:4040`), đảm bảo phản hồi là JSON của backend. Bấm qua trang cảnh báo trong trình duyệt của bạn không xác nhận rằng máy chủ VNPAY cũng truy cập được.

## 4. Chạy giao dịch qua website

1. Đăng ký, xác minh email qua Mailpit và đăng nhập tài khoản khách.
2. Chọn khách sạn ACTIVE đang mở bán, loại phòng đang bán và phòng vật lý khả dụng. Chọn ngày tương lai, lấy báo giá rồi tạo booking thật. Tổng tiền phải từ 5.000 đến 9.999.999.999 VND; không dùng slug minh họa.
3. Booking mới ở `PENDING_PAYMENT` và đã giữ tồn phòng. Mở `/my-bookings`, chọn **Thanh toán qua VNPay** trước hạn `expiresAt`.
4. DevTools → Network: `POST /api/v1/payments/bookings/{bookingId}/vnpay` phải trả HTTP 200, `status=PENDING` và `paymentUrl` thuộc sandbox VNPAY. Số tiền do backend lấy từ booking, không nhận từ trình duyệt.
5. Trên VNPAY, chọn thẻ nội địa/ngân hàng **NCB** và nhập thẻ sandbox:

   | Trường | Giá trị |
   | --- | --- |
   | Số thẻ | `9704198526191432198` |
   | Chủ thẻ | `NGUYEN VAN A` |
   | Ngày phát hành | `07/15` |
   | OTP | `123456` |

6. Sau thanh toán, trình duyệt quay về `/payment/return?vnp_TxnRef=...`. Phiên đăng nhập được khôi phục bằng refresh cookie. Trang lấy giao dịch từ backend, kiểm tra lại mỗi 5 giây khi đang chờ, tối đa khoảng 20 lần; có nút kiểm tra thủ công.
7. Khi IPN hợp lệ đến trước lúc giữ phòng hết hạn, kết quả cần là payment `SUCCEEDED`, `bookingConfirmed=true`, booking `CONFIRMED`, tồn phòng `CONFIRMED` và một dòng lịch sử xác nhận booking.

Nếu VNPAY báo thành công nhưng website vẫn chờ: kiểm tra IPN URL trong merchant, log gateway và backend. Không sửa URL thành `vnp_ResponseCode=00` để giả kết quả và không thanh toán lại chỉ vì IPN chưa tới.

## API

| Method và path | Xác thực | Hành vi |
| --- | --- | --- |
| `POST /api/v1/payments/bookings/{bookingId}/vnpay` | JWT chủ booking | Tạo hoặc lấy lại giao dịch PENDING; không cần body |
| `GET /api/v1/payments/{paymentId}` | JWT chủ giao dịch | Đọc kết quả; tài khoản khác nhận 404 |
| `GET /api/v1/payments/vnpay/ipn` | Chữ ký VNPAY | Kiểm tra và cập nhật giao dịch; JSON trả trực tiếp, không bọc `data` |

Hai API của khách trả envelope `{message,data}` và `Cache-Control: no-store`. Ví dụ `data` khi mới tạo:

```json
{
  "id": "11111111-1111-4111-8111-111111111111",
  "bookingId": "22222222-2222-4222-8222-222222222222",
  "amountVnd": 123456,
  "currency": "VND",
  "status": "PENDING",
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "expiresAt": "2026-11-15T10:15:00Z",
  "bookingConfirmed": false,
  "expired": false
}
```

`vnp_TxnRef` là UUID payment, không phải UUID booking. `vnp_Amount` bằng `amountVnd * 100`. Thời gian gửi VNPAY dùng GMT+7; dữ liệu lưu bằng `Instant`. `paymentUrl=null` khi giao dịch đã kết thúc hoặc URL hết hạn. `expired` phản ánh hạn URL, không phủ nhận một giao dịch đã thành công.

IPN chỉ xác nhận thanh toán thành công khi **cả** `vnp_ResponseCode=00` và `vnp_TransactionStatus=00`. Cần chữ ký hợp lệ, đúng merchant, tham chiếu tồn tại, số tiền khớp chính xác và ngày/mã giao dịch hợp lệ.

| RspCode IPN | Ý nghĩa |
| --- | --- |
| `00` | Đã lưu kết quả thành công, kể cả kết quả thanh toán thất bại |
| `01` | Không tìm thấy tham chiếu |
| `02` | Giao dịch đã xử lý; không ghi/xác nhận thêm |
| `04` | Sai số tiền |
| `97` | Sai chữ ký/merchant hoặc tham số VNPAY bị lặp |
| `99` | Dữ liệu kết quả không hợp lệ hoặc lỗi lưu; cần gọi lại |

## Kịch bản cần thử

- Thành công và tải lại trang: trạng thái vẫn đúng từ database.
- Hủy ở VNPAY: payment FAILED, booking còn chờ; được tạo tham chiếu mới trước hạn booking. Một thanh toán thất bại không gia hạn giữ phòng.
- Bấm thanh toán nhiều lần/gọi đồng thời: dùng lại cùng giao dịch PENDING, không sinh nhiều URL đang chờ cho cùng booking.
- IPN lặp/gọi đồng thời: lần xử lý đầu trả `00`, các lần sau trả `02`; một lịch sử xác nhận.
- Sai chữ ký, merchant hoặc số tiền; tham số lặp: không xác nhận booking/tồn phòng.
- Tài khoản khác hoặc chưa đăng nhập: không xem/tạo giao dịch của người khác.
- IPN thành công tới sau hạn giữ phòng: **vẫn lưu SUCCEEDED**, nhưng `bookingConfirmed=false`. Trang kết quả cảnh báo cần hỗ trợ/đối soát và không thanh toán lại. Không tự giữ lại phòng đã có thể bán cho người khác.
- Lỗi database trong lúc xác nhận: rollback cả payment, booking, tồn phòng; trả `99` để IPN có thể thử lại.

Các giao dịch cần đối soát có thể tìm bằng SQL dành cho người vận hành:

```sql
SELECT id, booking_id, amount_vnd, provider_transaction_no, paid_at, received_at
FROM payment_transactions
WHERE status = 'SUCCEEDED' AND NOT booking_confirmed
ORDER BY received_at DESC;
```

Không tự đổi booking sang CONFIRMED bằng SQL nếu chưa kiểm tra lại tồn phòng. API truy vấn VNPAY (`querydr`), hoàn tiền (`refund`), UI xử lý đối soát và scheduler hết hạn booking nằm ngoài phần triển khai này. HELD quá hạn không còn được tính vào tồn khả dụng; IPN tới muộn vẫn không được tự xác nhận tồn đã hết hạn.

## Kiểm thử tự động

```powershell
.\mvnw.cmd test "-Dtest=VnpayGatewayAdapterTests,VnpayPaymentServiceTests,VnpayPaymentIntegrationTests,ModularityTests"
```

Integration test dùng PostgreSQL 17 qua Testcontainers khi Docker đang chạy. Có thể chạy trên **database kiểm thử riêng** đã tạo sẵn:

```powershell
.\mvnw.cmd test "-Dtest=VnpayPaymentIntegrationTests" `
  "-Dtest.postgres.url=jdbc:postgresql://localhost:5432/hotel_vnpay_test" `
  "-Dtest.postgres.username=postgres" "-Dtest.postgres.password=<mat-khau-test>"
```

Test dùng secret giả và IPN ký giả lập, không liên lạc với VNPAY, không thay thế bước thanh toán thủ công bằng merchant sandbox. Không trỏ integration test vào DB thật: fixture sẽ dọn dữ liệu thử nghiệm của lớp test.

Tài liệu chính thức: [PAY 2.1.0](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html), [thông tin thẻ sandbox](https://sandbox.vnpayment.vn/apis/docs/gioi-thieu/), [Return và IPN](https://sandbox.vnpayment.vn/apis/docs/faqs/).
