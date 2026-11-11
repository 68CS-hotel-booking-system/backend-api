# Xét duyệt hồ sơ khách sạn

Phạm vi: Owner xin công khai khách sạn **DRAFT** lần đầu. Trạng thái khách sạn vẫn là
`DRAFT / ACTIVE / SUSPENDED / LOCKED`; trạng thái xét duyệt nằm riêng trong hồ sơ
`PENDING / APPROVED / REJECTED`. Không thêm trạng thái chờ duyệt vào khách sạn.

Các API dùng Bearer token hiện có, `ApiResponse` khi thành công và `application/problem+json`
khi thất bại. Tài khoản phải đang hoạt động, đã xác minh email. Admin chỉ đọc và quyết định;
Admin có đồng thời role `HOTEL_OWNER` cũng không được gửi hoặc sửa hồ sơ thay Owner.
Mọi response hồ sơ có `Cache-Control: no-store`.

## 1. Owner gửi hồ sơ

`POST /api/v1/management/hotels/{hotelId}/review-requests`

Quyền: `HOTEL_OWNER`, chính là `hotels.owner_user_id`. Không cho Admin, Manager hoặc Owner khác gửi.

```json
{"expectedVersion": 3}
```

`expectedVersion` là `version` khách sạn lấy từ `GET /api/v1/management/hotels/{hotelId}`.
Không gửi lại nội dung khách sạn. Backend lấy dữ liệu đã lưu, kiểm `HotelPublishReadiness`:
tên, địa chỉ, điện thoại hoặc email liên hệ, một loại phòng đang bán có sức chứa hợp lệ
và ít nhất một phòng vật lý đang phục vụ của loại phòng đó.

Trả `201 Created`, header `Location: /api/v1/hotel-review-requests/{id}`. Dạng dữ liệu hồ sơ:

```json
{
  "message": "Gửi hồ sơ khách sạn thành công.",
  "data": {
    "id": "11111111-1111-4111-8111-111111111111",
    "hotelId": "22222222-2222-4222-8222-222222222222",
    "snapshot": {
      "hotel": {
        "id": "22222222-2222-4222-8222-222222222222",
        "ownerId": "33333333-3333-4333-8333-333333333333",
        "cityId": "822cafba-73f6-4ec0-bae7-71804535cae3",
        "name": "Khách sạn Biển Xanh",
        "slug": "khach-san-bien-xanh",
        "description": "Gần biển",
        "address": "12 Võ Nguyên Giáp",
        "contactPhone": "+84912345678",
        "contactEmail": null,
        "latitude": null,
        "longitude": null,
        "starRating": 4,
        "checkInTime": "14:00:00",
        "checkOutTime": "12:00:00",
        "status": "DRAFT",
        "createdAt": "2026-11-10T08:00:00Z",
        "updatedAt": "2026-11-11T08:00:00Z",
        "version": 3
      },
      "amenities": [],
      "customAmenities": [],
      "roomTypes": [
        {
          "roomType": {
            "id": "44444444-4444-4444-8444-444444444444",
            "hotelId": "22222222-2222-4222-8222-222222222222",
            "name": "Deluxe",
            "description": null,
            "maxAdults": 2,
            "maxChildren": 0,
            "maxOccupancy": 2,
            "basePrice": 0,
            "active": true,
            "createdAt": "2026-11-10T08:00:00Z",
            "updatedAt": "2026-11-10T08:00:00Z",
            "version": 0
          },
          "amenities": []
        }
      ],
      "rooms": [
        {
          "id": "55555555-5555-4555-8555-555555555555",
          "roomTypeId": "44444444-4444-4444-8444-444444444444",
          "roomNumber": "101",
          "floor": "1",
          "operationalFrom": "2026-01-01T00:00:00Z",
          "operationalUntil": null,
          "amenities": [],
          "customAmenities": []
        }
      ],
      "images": [
        {
          "mediaId": "66666666-6666-4666-8666-666666666666",
          "albumId": null,
          "sortOrder": 0,
          "cover": true,
          "altText": "Mặt tiền khách sạn"
        }
      ],
      "albums": []
    },
    "status": "PENDING",
    "submittedBy": "33333333-3333-4333-8333-333333333333",
    "submittedAt": "2026-11-11T09:00:00Z",
    "reviewedBy": null,
    "reviewedAt": null,
    "reason": null,
    "version": 0
  }
}
```

Các danh sách tiện ích dùng dữ liệu đang có: tiện ích danh mục có `id/code/name/category`;
tiện ích riêng giữ `id/hotelId/roomId/name/normalizedName/description/attached/createdAt/updatedAt/version`.
Album giữ `id/name/description/sortOrder`. Không tự thêm yêu cầu ảnh hoặc tiện ích bắt buộc.

## 2. Admin liệt kê hồ sơ

`GET /api/v1/admin/hotel-review-requests?status=PENDING&hotelId={uuid}&page=0&size=20`

Chỉ `ADMIN`. `status` và `hotelId` là bộ lọc tùy chọn; `status` nhận đúng một trong ba giá trị viết hoa.
Phân trang bắt đầu từ 0, mặc định 20, tối đa 100 theo cấu hình chung.
Thứ tự cố định `submittedAt DESC, id DESC`; danh sách chỉ đọc projection tóm tắt, không nạp JSON snapshot.

```json
{
  "message": "Lấy danh sách hồ sơ thành công.",
  "data": {
    "content": [{
      "id": "11111111-1111-4111-8111-111111111111",
      "hotelId": "22222222-2222-4222-8222-222222222222",
      "hotelName": "Khách sạn Biển Xanh",
      "status": "PENDING",
      "submittedBy": "33333333-3333-4333-8333-333333333333",
      "submittedAt": "2026-11-11T09:00:00Z",
      "reviewedBy": null,
      "reviewedAt": null,
      "reason": null,
      "version": 0
    }],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

## 3. Đọc hồ sơ

Để Owner lấy lại trạng thái khi tải lại trang, frontend dùng thêm
`GET /api/v1/management/hotels/{hotelId}/review-requests?page=0&size=1`.
Owner chỉ xem được lịch sử của khách sạn mình sở hữu; Admin xem được mọi khách sạn.
Response là cùng PageResponse tóm tắt như API danh sách Admin, thứ tự mới nhất trước;
chưa gửi hồ sơ trả danh sách rỗng. Có thể dùng page/size để đọc các lần gửi trước.
`hotelName` trong tóm tắt lấy từ snapshot đã gửi, không lấy tên khách sạn hiện tại.

`GET /api/v1/hotel-review-requests/{requestId}`

Admin xem mọi hồ sơ. Owner chỉ xem hồ sơ của khách sạn mình sở hữu; Owner ngoài phạm vi nhận `404`
để không lộ hồ sơ. Manager và Receptionist không có quyền đọc hồ sơ xét duyệt.
Trả `200`, cùng cấu trúc `data` chi tiết ở API gửi hồ sơ, với trạng thái xét duyệt hiện tại.

`snapshot` là dữ liệu tại lúc gửi, không dựng lại từ dữ liệu hiện tại. Để xem ảnh, lấy URL mới qua
`GET /api/v1/media/{mediaId}/access` bằng token Admin hoặc Owner. Không lưu URL ký có hạn trong snapshot.
Ảnh đã gửi có tham chiếu FK riêng: Owner bỏ ảnh khỏi danh sách sau khi bị từ chối vẫn không làm
mất ảnh lịch sử. Database chặn xóa/soft-delete hoặc đổi vị trí lưu trữ ảnh đã được hồ sơ tham chiếu.
Media API cần cấu hình storage R2 như luồng ảnh hiện có.

## 4. Admin duyệt

`POST /api/v1/admin/hotel-review-requests/{requestId}/approve`

Chỉ `ADMIN`. `expectedVersion` là version **hồ sơ**, không phải version khách sạn.

```json
{"expectedVersion": 0}
```

Trả `200`, cùng `data` chi tiết, đổi các trường:

```json
{
  "status": "APPROVED",
  "reviewedBy": "77777777-7777-4777-8777-777777777777",
  "reviewedAt": "2026-11-11T10:00:00Z",
  "reason": null,
  "version": 1
}
```

Chỉ hồ sơ `PENDING` và khách sạn `DRAFT` được duyệt. Kiểm lại điều kiện mở bán tại thời điểm duyệt.
Khách sạn chuyển `ACTIVE`, thêm lịch sử `DRAFT → ACTIVE` theo luồng hiện có. Quyết định, trạng thái
khách sạn và lịch sử nằm trong cùng transaction; bất kỳ lỗi nào cũng rollback cả ba.
Snapshot giữ trạng thái `DRAFT` và version khách sạn lúc gửi, kể cả sau khi duyệt.

## 5. Admin từ chối

`POST /api/v1/admin/hotel-review-requests/{requestId}/reject`

Chỉ `ADMIN`.

```json
{"expectedVersion": 0, "reason": "Vui lòng bổ sung mô tả loại phòng."}
```

Trả `200`, cùng `data` chi tiết, đổi các trường:

```json
{
  "status": "REJECTED",
  "reviewedBy": "77777777-7777-4777-8777-777777777777",
  "reviewedAt": "2026-11-11T10:00:00Z",
  "reason": "Vui lòng bổ sung mô tả loại phòng.",
  "version": 1
}
```

Lý do bắt buộc, không toàn khoảng trắng; backend bỏ khoảng trắng đầu/cuối. Chỉ xử lý hồ sơ `PENDING`
của khách sạn `DRAFT`. Khách sạn giữ `DRAFT`; Owner được sửa và gửi lại bằng API 1. Mỗi lần gửi tạo
ID mới, version hồ sơ mới bắt đầu từ 0. Hồ sơ cũ, snapshot và quyết định cũ giữ nguyên.
Nếu Admin đã khóa khách sạn trong khi hồ sơ đang chờ, cần gỡ khóa về `DRAFT` trước khi quyết định.

## Mã lỗi

| HTTP | Code | Khi xảy ra |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Thiếu version, version âm, reason thiếu/trắng hoặc body sai |
| 400 | `BAD_REQUEST` | UUID/status/query sai kiểu, theo handler chung |
| 401 | `UNAUTHORIZED` | Chưa xác thực |
| 403 | `ACCESS_DENIED` | Sai role; Admin cố sửa nội dung hoặc Owner cố quyết định |
| 404 | `HOTEL_NOT_FOUND` | Khách sạn không có hoặc không thuộc Owner gửi |
| 404 | `REVIEW_NOT_FOUND` | Hồ sơ không có hoặc Owner đọc ngoài phạm vi |
| 409 | `REVIEW_PENDING` | Đã có hồ sơ đang chờ hoặc sửa nội dung trong khi đang chờ |
| 409 | `REVIEW_REQUIRED` | API approve cũ được gọi khi không có hồ sơ PENDING |
| 409 | `REVIEW_CONCURRENT_UPDATE` | expectedVersion khách sạn/hồ sơ đã cũ |
| 409 | `REVIEW_ALREADY_DECIDED` | Quyết định trên hồ sơ đã xử lý, dù version đúng |
| 409 | `CONCURRENT_UPDATE` | Xung đột optimistic locking do handler chung xử lý |
| 409 | `INVALID_STATUS_TRANSITION` | Khách sạn không ở DRAFT |
| 422 | `HOTEL_NOT_READY` | Thiếu điều kiện mở bán; `errors` liệt kê từng điều kiện |

Ví dụ lỗi:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Khách sạn đang có hồ sơ chờ xét duyệt; chưa được chỉnh sửa hoặc gửi lại.",
  "instance": "/api/v1/management/hotels/22222222-2222-4222-8222-222222222222",
  "code": "REVIEW_PENDING",
  "traceId": "6a895925f9174399920ff996db9093311",
  "errors": []
}
```

## API cũ và tương thích

- Giữ `PATCH /api/v1/management/hotels/{hotelId}/approve`, quyền Admin, không đổi body hoặc response
  `ManagementHotelResponse`. API này khóa hotel, tìm hồ sơ `PENDING`, rồi dùng đúng cùng bước quyết định
  của API 4; không có hồ sơ trả `409 REVIEW_REQUIRED`. Caller cũ cần chờ Owner gửi hồ sơ trước.
  Do hợp đồng cũ không có expectedVersion, API cũ quyết định hồ sơ đang PENDING tại lúc lấy khóa;
  giao diện Admin mới nên gọi API 4 với requestId và version vừa đọc để tránh quyết định nhầm lần gửi lại.
- Các thao tác sửa nội dung hotel, tạo/sửa loại phòng, tạo/sửa thông tin hoặc thời gian phục vụ phòng,
  thay tiện ích hotel/loại phòng/phòng, tạo/sửa/tháo tiện ích riêng, upload ảnh, thay danh sách ảnh và ghi album:
  Admin nhận `403`, Owner/Manager nhận `409 REVIEW_PENDING` khi có hồ sơ đang chờ. Hợp đồng request
  và response cũ giữ nguyên. Quyền đọc quản lý vẫn giữ nguyên.
- PATCH chỉ đổi readiness (`status` ready/cleaning/maintenance) của phòng là thao tác vận hành,
  tiếp tục được xử lý khi chờ duyệt. Readiness không nằm trong snapshot.
- Các quyền Admin khóa/gỡ khóa và vận hành suspend/resume giữ nguyên. Pricing, booking,
  gán phòng, check-in/check-out và nghiệp vụ tài chính không bị khóa bởi hồ sơ.

## Transaction và khóa

Transaction đặt tại `HotelReviewConfig` bằng `TransactionTemplate`; application/domain dùng Java thuần
và thời gian từ `Clock`. Gửi duyệt và mọi chỉnh sửa hồ sơ khóa ghi hàng hotel trước khi đọc/ghi bản ghi
con. Quyết định đọc riêng hotelId của hồ sơ, khóa hotel rồi mới đọc lại hồ sơ và kiểm version; không
giữ bản entity hồ sơ cũ trong persistence context khi chờ khóa. Thứ tự chung là hotel → bản ghi con
(room type/media nếu cần). Unique partial index PostgreSQL bảo vệ tối đa một `PENDING` mỗi hotel,
kể cả khi có writer không qua application. `@Version` và khóa parent bảo vệ quyết định đồng thời.

Migration mới: `V20261111150000000__create_hotel_review_requests.sql`. Không sửa migration cũ.
Test tích hợp: `HotelReviewIntegrationTests`, PostgreSQL 17 qua Testcontainers; kiểm cả constraint,
gửi/duyệt đồng thời, snapshot ảnh, quyền ghi/đọc và rollback sau khi hotel/lịch sử đã flush.
