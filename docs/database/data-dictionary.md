# Từ điển dữ liệu — Hotel Booking

[Tổng quan](README.md) · [ERD và FK](erd.md) · [Quy tắc/enum](business-rules.md)

Theo DBML mới ngày 21/10/2026: **37 bảng, 300 cột, 62 index có tên**. Giữ đúng tên/kiểu/ràng buộc/default nguồn, kể cả chỗ cần sửa. Nội dung giải thích không tự tạo ràng buộc SQL. Các enum được liệt kê đầy đủ trong business-rules.md.

Nhiều cột PK cùng tạo một PK ghép. Không ghi UNIQUE riêng từng cột khi chỉ có UNIQUE bộ cột. Cột không có not null là nullable, trừ thành phần PK. `updated_at DEFAULT now()` không tự cập nhật khi UPDATE.

## Tra cứu

**Identity:** [users](#users), [roles](#roles), [permissions](#permissions), [user_roles](#user_roles), [role_permissions](#role_permissions), [refresh_tokens](#refresh_tokens), [password_reset_tokens](#password_reset_tokens), [email_verification_tokens](#email_verification_tokens).

**Hotel:** [hotels](#hotels), [room_types](#room_types), [rooms](#rooms), [hotel_members](#hotel_members), [cities](#cities), [amenities](#amenities), [hotel_amenities](#hotel_amenities), [room_type_amenities](#room_type_amenities), [hotel_status_history](#hotel_status_history).

**Inventory:** [room_inventory](#room_inventory), [room_blocks](#room_blocks), [inventory_reservations](#inventory_reservations).

**Pricing:** [room_rates](#room_rates).

**Booking:** [bookings](#bookings), [booking_items](#booking_items), [booking_night_prices](#booking_night_prices), [booking_status_history](#booking_status_history), [room_assignments](#room_assignments).

**Payment:** [payment_attempts](#payment_attempts), [payment_webhook_events](#payment_webhook_events), [refunds](#refunds).

**Media:** [media_objects](#media_objects), [hotel_images](#hotel_images), [room_type_images](#room_type_images), [user_images](#user_images).

**Review:** [reviews](#reviews).

**Promotion:** [promotions](#promotions), [promotion_redemptions](#promotion_redemptions).

**Notification:** [notifications](#notifications).

**Outbox:** [outbox_events](#outbox_events).

## Identity

### users

**Mục đích:** Tài khoản, hồ sơ, trạng thái và thời điểm xác minh email.

**Note nguyên văn nguồn:** CURRENT - Flyway V1

**Diễn giải / cần review:** Nguồn đang để email_verified_at là timestamp NOT NULL; tài khoản chưa xác minh cần NULL. Đề xuất dùng timestamptz nullable. avatar_url cũ cùng tồn tại với user_images; cần chọn một nguồn avatar hiện hành.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **email** | varchar(255) | not null; unique |  | Email tài khoản; chuẩn hóa và tính duy nhất theo quy tắc Identity. **Ghi chú nguồn:** CHECK đề xuất: email = lower(btrim(email)) |
| **password_hash** | varchar(255) | not null |  | Hash mật khẩu; không chứa mật khẩu thô. |
| **full_name** | varchar(100) | not null |  | Tên hiển thị trong hồ sơ hiện tại. |
| **phone** | varchar(20) | not null; unique |  | Số điện thoại tài khoản; cách chuẩn hóa cần thống nhất. |
| **avatar_url** | varchar(255) | null |  | URL ảnh đại diện theo thiết kế nguồn; chưa nối với media_objects. |
| **status** | user_status__t | not null; default: pending |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **email_verified_at** | timestamp | not null |  | Thời điểm xác minh email. Nguồn NOT NULL đang mâu thuẫn với trạng thái chưa xác minh; xem điểm cần sửa trong README. |
| **is_deleted** | boolean | not null; default: false |  | Cờ xóa mềm người dùng; không tự gỡ các ràng buộc UNIQUE. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **updated_at** | timestamptz | not null; default: now() |  | Thời điểm sửa; DEFAULT now() không tự cập nhật khi UPDATE. |

#### Index có tên trong nguồn

Không có index có tên khai báo riêng. PK/UNIQUE cột vẫn được giữ ở bảng cột trên.

### roles

**Mục đích:** Danh mục vai trò toàn hệ thống.

**Note nguyên văn nguồn:** CURRENT - Flyway V1

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | integer | pk; increment; not null |  | Định danh duy nhất của bản ghi. |
| **role_key** | varchar(50) | not null; unique |  | Mã vai trò ổn định để kiểm tra quyền. **Ghi chú nguồn:** Mã ổn định dùng để phân quyền: CUSTOMER, HOTEL_OWNER, ADMIN |
| **role_name** | varchar(100) | not null |  | Tên vai trò để hiển thị. **Ghi chú nguồn:** Tên hiển thị của vai trò |
| **description** | varchar(255) | null |  | Mô tả bổ sung, có thể chưa được cung cấp nếu nullable. |
| **is_active** | boolean | not null; default: true |  | Cờ cho phép sử dụng; không tương đương xóa bản ghi. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **updated_at** | timestamptz | not null; default: now() |  | Thời điểm sửa; DEFAULT now() không tự cập nhật khi UPDATE. |

#### Index có tên trong nguồn

Không có index có tên khai báo riêng. PK/UNIQUE cột vẫn được giữ ở bảng cột trên.

### permissions

**Mục đích:** Danh mục quyền chức năng.

**Note nguyên văn nguồn:** CURRENT - Flyway V1

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | integer | pk; increment; not null |  | Định danh duy nhất của bản ghi. |
| **permission_key** | varchar(100) | not null; unique |  | Mã quyền chức năng ổn định. |
| **permission_name** | varchar(150) | not null |  | Tên hiển thị của quyền. |
| **description** | varchar(255) | null |  | Mô tả bổ sung, có thể chưa được cung cấp nếu nullable. |
| **is_active** | boolean | not null; default: true |  | Cờ cho phép sử dụng; không tương đương xóa bản ghi. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **updated_at** | timestamptz | not null; default: now() |  | Thời điểm sửa; DEFAULT now() không tự cập nhật khi UPDATE. |

#### Index có tên trong nguồn

Không có index có tên khai báo riêng. PK/UNIQUE cột vẫn được giữ ở bảng cột trên.

### user_roles

**Mục đích:** Gán vai trò cho tài khoản.

**Note nguyên văn nguồn:** CURRENT - Flyway V1

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **user_id** | uuid | not null | fk_user_roles_user | Tài khoản liên quan. |
| **role_id** | integer | not null | fk_user_roles_role | Vai trò được gán, tham chiếu roles. |
| **assigned_at** | timestamptz | not null; default: now() |  | Thời điểm phân công. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `uq_user_roles_user_role` | `user_id, role_id` | Có |
| `idx_user_roles_role_id` | `role_id` | Không |

### role_permissions

**Mục đích:** Gán quyền cho vai trò.

**Note nguyên văn nguồn:** CURRENT - Flyway V1

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **role_id** | integer | pk; not null | fk_role_permissions_role | Vai trò được gán, tham chiếu roles. |
| **permission_id** | integer | pk; not null | fk_role_permissions_permission | Quyền chức năng, tham chiếu permissions. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_role_permissions_permission_id` | `permission_id` | Không |

### refresh_tokens

**Mục đích:** Token làm mới và nhóm phiên đăng nhập.

**Note nguyên văn nguồn:** CURRENT - Flyway V2

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null |  | Định danh duy nhất của bản ghi. |
| **user_id** | uuid | not null | fk_refresh_tokens_user | Tài khoản liên quan. |
| **family_id** | uuid | not null |  | Định danh nhóm refresh token; không phải FK đến một bảng family trong nguồn. |
| **token_hash** | varchar(64) | not null; unique |  | Mã đã băm; không lưu mã gốc gửi qua URL. |
| **expires_at** | timestamptz | not null |  | Hạn hiệu lực; cần logic kiểm tra, không tự đổi trạng thái khi đến hạn. |
| **revoked** | boolean | not null; default: false |  | Token đã bị thu hồi; được kiểm tra cùng thời hạn. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_refresh_tokens_user_id` | `user_id` | Không |
| `idx_refresh_tokens_family_id` | `family_id` | Không |

### password_reset_tokens

**Mục đích:** Mã đã băm dùng đặt lại mật khẩu.

**Note nguyên văn nguồn:** CURRENT - Flyway V5

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null |  | Định danh duy nhất của bản ghi. |
| **user_id** | uuid | not null | fk_password_reset_tokens_user | Tài khoản liên quan. |
| **token_hash** | varchar(64) | not null; unique |  | Mã đã băm; không lưu mã gốc gửi qua URL. |
| **expires_at** | timestamptz | not null |  | Hạn hiệu lực; cần logic kiểm tra, không tự đổi trạng thái khi đến hạn. |
| **used_at** | timestamptz | null |  | Thời điểm mã đã được sử dụng. |
| **revoked** | boolean | not null; default: false |  | Token đã bị thu hồi; được kiểm tra cùng thời hạn. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_password_reset_tokens_user_id` | `user_id` | Không |

### email_verification_tokens

**Mục đích:** Mã đã băm dùng xác minh email một lần.

**Note nguyên văn nguồn:** URL gửi qua email chứa mã gốc; database chỉ lưu mã đã băm. Chỉ dùng khi chưa hết hạn, chưa sử dụng và chưa thu hồi.

**Diễn giải / cần review:** Có riêng mã xác minh email, không dùng lẫn với password_reset_tokens. Chưa có CHECK thực thi trong DBML được gửi.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **user_id** | uuid | not null | fk_email_verification_tokens_user | Tài khoản liên quan. |
| **token_hash** | varchar(64) | not null; unique |  | Mã đã băm; không lưu mã gốc gửi qua URL. |
| **expires_at** | timestamptz | not null |  | Hạn hiệu lực; cần logic kiểm tra, không tự đổi trạng thái khi đến hạn. |
| **used_at** | timestamptz | null |  | Thời điểm mã đã được sử dụng. |
| **revoked_at** | timestamptz | null |  | Thời điểm thu hồi; NULL là chưa ghi nhận thu hồi. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_email_verification_tokens_user` | `user_id` | Không |
| `idx_email_verification_tokens_expiry` | `expires_at` | Không |

## Hotel

### hotels

**Mục đích:** Khách sạn, chủ sở hữu, vị trí, giờ nhận/trả và trạng thái công khai.

**Note nguyên văn nguồn:** CURRENT - Flyway V3. CHECK: star_rating 1..5; latitude -90..90; longitude -180..180

**Bổ sung Flyway V20261110120100000 và V20261110120200000 (ngoài DBML gốc):** enum `hotel_status__t` thêm SUSPENDED (quản lý tạm ngừng bán) và LOCKED (quản trị hệ thống khóa bán), dữ liệu DRAFT/ACTIVE giữ nguyên. Thêm `contact_phone`, `contact_email` (nullable, khách sạn cũ là NULL); HT-03 yêu cầu có ít nhất một liên hệ trước khi approve/resume. Liên hệ chỉ trả qua API quản lý, không trả qua API công khai. Mỗi lần đổi trạng thái bán ghi một dòng vào [hotel_status_history](#hotel_status_history).

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **owner_user_id** | uuid | not null | fk_hotels_owner | Chủ sở hữu chính của hotel; không suy ra mọi member đều là owner. **Ghi chú nguồn:** Chủ sở hữu chính; nhân viên được ủy quyền nằm trong hotel_members |
| **city_id** | uuid | not null | fk_hotels_city | Thành phố chứa khách sạn, tham chiếu cities. |
| **name** | varchar(255) | not null |  | Tên nghiệp vụ của đối tượng. |
| **slug** | varchar(255) | not null; unique |  | Chuỗi dùng trong URL; quy tắc sinh và xử lý trùng ở application. |
| **description** | text | null |  | Mô tả bổ sung, có thể chưa được cung cấp nếu nullable. |
| **address** | varchar(255) | not null |  | Địa chỉ khách sạn. |
| **contact_phone** | varchar(20) | null |  | Số điện thoại liên hệ, 8–15 chữ số, có thể có dấu + đầu. Bổ sung so với DBML gốc. |
| **contact_email** | varchar(255) | null |  | Email liên hệ của khách sạn. Bổ sung so với DBML gốc. |
| **latitude** | decimal(9,6) | null |  | Vĩ độ; NULL khi chưa có tọa độ. |
| **longitude** | decimal(9,6) | null |  | Kinh độ; NULL khi chưa có tọa độ. |
| **star_rating** | smallint | null |  | Số sao khách sạn, khác rating đánh giá của khách. **Ghi chú nguồn:** Từ 1 đến 5 sao |
| **check_in_time** | time | not null; default: '14:00:00' |  | Giờ nhận phòng theo giờ địa phương của khách sạn. |
| **check_out_time** | time | not null; default: '12:00:00' |  | Giờ trả phòng theo giờ địa phương của khách sạn. |
| **status** | hotel_status__t | not null; default: DRAFT |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. Đã triển khai DRAFT, ACTIVE, SUSPENDED, LOCKED; chỉ ACTIVE là đang bán. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **updated_at** | timestamptz | not null; default: now() |  | Thời điểm sửa; DEFAULT now() không tự cập nhật khi UPDATE. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_hotels_owner_user_id` | `owner_user_id` | Không |
| `idx_hotels_city_id` | `city_id` | Không |
| `idx_hotels_status` | `status` | Không |

### room_types

**Mục đích:** Loại phòng và sức chứa trong một khách sạn.

**Note nguyên văn nguồn:** CURRENT - Flyway V3. CHECK: max_adults >= 1; max_children >= 0

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **hotel_id** | uuid | not null | fk_room_types_hotel | Khách sạn liên quan; phạm vi cụ thể theo bảng. |
| **name** | varchar(100) | not null |  | Tên nghiệp vụ của đối tượng. **Ghi chú nguồn:** Ví dụ Deluxe King, Standard Twin |
| **description** | text | null |  | Mô tả bổ sung, có thể chưa được cung cấp nếu nullable. |
| **max_adults** | smallint | not null; default: 2 |  | Số người lớn tối đa của một phòng thuộc loại này. |
| **max_children** | smallint | not null; default: 0 |  | Số trẻ em tối đa của một phòng thuộc loại này. |
| **is_active** | boolean | not null; default: true |  | Cờ cho phép sử dụng; không tương đương xóa bản ghi. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_room_types_hotel_id` | `hotel_id` | Không |
| `uq_room_types_id_hotel` | `id, hotel_id` | Có |
| `idx_room_types_hotel_name` | `hotel_id, name` | Không |

### rooms

**Mục đích:** Phòng vật lý, loại phòng, thời gian phục vụ và tình trạng bàn giao.

**Note nguyên văn nguồn:** Phòng cụ thể như 301, 302. Readiness chỉ phản ánh khả năng bàn giao hiện tại, không thay thế lịch đặt hoặc lịch bảo trì.

**Diễn giải / cần review:** UNIQUE (hotel_id, room_number); FK ghép giữ đúng hotel của loại phòng. READY/DIRTY không thay thế lịch đặt và bảo trì.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **hotel_id** | uuid | not null | fk_rooms_hotel, fk_rooms_room_type_hotel | Khách sạn liên quan; phạm vi cụ thể theo bảng. |
| **room_type_id** | uuid | not null | fk_rooms_room_type_hotel | Loại phòng liên quan. |
| **room_number** | varchar(20) | not null |  | Số phòng; duy nhất trong cùng khách sạn. |
| **floor** | varchar(20) | null |  | Tên/mã tầng, có thể có ký tự như G hoặc B1. |
| **readiness** | room_readiness__t | not null; default: DIRTY |  | READY hoặc DIRTY; tình trạng bàn giao hiện tại. |
| **readiness_updated_at** | timestamptz | not null; default: now() |  | Thời điểm cập nhật tình trạng bàn giao. |
| **readiness_updated_by** | uuid | null | fk_rooms_readiness_updated_by | Nhân sự cập nhật tình trạng bàn giao. |
| **operational_from** | timestamptz | not null; default: now() |  | Thời điểm bắt đầu phục vụ của phòng vật lý. |
| **operational_until** | timestamptz | null |  | Thời điểm kết thúc phục vụ; NULL là chưa ấn định. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **updated_at** | timestamptz | not null; default: now() |  | Thời điểm sửa; DEFAULT now() không tự cập nhật khi UPDATE. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `uq_rooms_hotel_room_number` | `hotel_id, room_number` | Có |
| `uq_rooms_id_room_type` | `id, room_type_id` | Có |
| `idx_rooms_type_readiness` | `room_type_id, readiness` | Không |

### hotel_members

**Mục đích:** Nhân sự và vai trò theo từng khách sạn.

**Note nguyên văn nguồn:** PLANNED - quyền vận hành theo từng hotel; owner chính vẫn là hotels.owner_user_id. CHECK: status ACTIVE phải có joined_at

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **hotel_id** | uuid | pk; not null | fk_hotel_members_hotel | Khách sạn liên quan; phạm vi cụ thể theo bảng. |
| **user_id** | uuid | pk; not null | fk_hotel_members_user | Tài khoản liên quan. |
| **member_role** | hotel_member_role__t | not null |  | Vai trò vận hành tại một hotel; không phải role toàn hệ thống. |
| **status** | hotel_member_status__t | not null; default: INVITED |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **invited_by_user_id** | uuid | null | fk_hotel_members_invited_by | Người mời thành viên; NULL nếu không có thông tin người mời. |
| **joined_at** | timestamptz | null |  | Thời điểm thành viên tham gia/chấp nhận, bắt buộc khi ACTIVE theo ý định nguồn. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **updated_at** | timestamptz | not null; default: now() |  | Thời điểm sửa; DEFAULT now() không tự cập nhật khi UPDATE. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_hotel_members_user_id` | `user_id` | Không |
| `idx_hotel_members_status_hotel` | `status, hotel_id` | Không |

### cities

**Mục đích:** Danh mục thành phố.

**Note nguyên văn nguồn:** CURRENT - Flyway V3 and V4

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **country_code** | varchar(2) | not null |  | Mã quốc gia hai ký tự, ví dụ VN. **Ghi chú nguồn:** ISO 3166-1 alpha-2, ví dụ VN, US, JP |
| **name** | varchar(100) | not null |  | Tên nghiệp vụ của đối tượng. |
| **slug** | varchar(150) | not null; unique |  | Chuỗi dùng trong URL; quy tắc sinh và xử lý trùng ở application. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_cities_country_name` | `country_code, name` | Không |

### amenities

**Mục đích:** Danh mục tiện ích.

**Note nguyên văn nguồn:** CURRENT - Flyway V3

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **code** | varchar(50) | not null; unique |  | Mã nghiệp vụ để tra cứu; phạm vi duy nhất giữ theo nguồn. **Ghi chú nguồn:** Ví dụ FREE_WIFI, SWIMMING_POOL |
| **name** | varchar(100) | not null |  | Tên nghiệp vụ của đối tượng. |
| **category** | amenity_category__t | not null; default: ROOM |  | Phân loại tiện ích; xem danh mục enum và nội dung `QUYEN`. |

**Bổ sung Flyway V20261110120000000 (ngoài DBML gốc):** `uq_amenities_id_category` UNIQUE(id, category). `id` đã là PK nên dữ liệu cũ luôn thỏa; ràng buộc này chỉ để `hotel_amenities` và `room_type_amenities` dùng FK ghép kiểm tra đúng nhóm tiện ích.

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `uq_amenities_id_category` | `id`, `category` | Có |

### hotel_amenities

**Mục đích:** Liên kết tiện ích với khách sạn; chỉ nhận tiện ích category HOTEL.

**Note nguyên văn nguồn:** CURRENT - Flyway V3

**Đã triển khai:** Flyway V20261110120000000. CHECK `ck_hotel_amenities_category`: amenity_category = 'HOTEL'. FK ghép `fk_hotel_amenities_amenity` (amenity_id, amenity_category) → amenities(id, category), nên DB từ chối gắn tiện ích ROOM cho khách sạn. Thay danh sách qua API PUT /api/v1/management/hotels/{hotelId}/amenities.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **hotel_id** | uuid | pk; not null | fk_hotel_amenities_hotel | Khách sạn liên quan; xóa hotel thì xóa liên kết (ON DELETE CASCADE). |
| **amenity_id** | uuid | pk; not null | fk_hotel_amenities_amenity | Tiện ích liên quan, tham chiếu amenities; cột đầu của FK ghép. |
| **amenity_category** | amenity_category__t | not null; default: HOTEL; check = 'HOTEL' | fk_hotel_amenities_amenity | Cột cố định, ứng dụng không ghi; cột thứ hai của FK ghép tới amenities(id, category). Bổ sung so với DBML gốc. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `pk_hotel_amenities` | `hotel_id`, `amenity_id` | Có (PK) |
| `idx_hotel_amenities_amenity_id` | `amenity_id` | Không |

### room_type_amenities

**Mục đích:** Liên kết tiện ích với loại phòng; chỉ nhận tiện ích category ROOM.

**Note nguyên văn nguồn:** CURRENT - Flyway V3

**Đã triển khai:** Flyway V20261110120000000. CHECK `ck_room_type_amenities_category`: amenity_category = 'ROOM'. FK ghép `fk_room_type_amenities_amenity` (amenity_id, amenity_category) → amenities(id, category), nên DB từ chối gắn tiện ích HOTEL cho loại phòng. Thay danh sách qua API PUT /api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/amenities.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **room_type_id** | uuid | pk; not null | fk_room_type_amenities_room_type | Loại phòng liên quan; xóa room type thì xóa liên kết (ON DELETE CASCADE). |
| **amenity_id** | uuid | pk; not null | fk_room_type_amenities_amenity | Tiện ích liên quan, tham chiếu amenities; cột đầu của FK ghép. |
| **amenity_category** | amenity_category__t | not null; default: ROOM; check = 'ROOM' | fk_room_type_amenities_amenity | Cột cố định, ứng dụng không ghi; cột thứ hai của FK ghép tới amenities(id, category). Bổ sung so với DBML gốc. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `pk_room_type_amenities` | `room_type_id`, `amenity_id` | Có (PK) |
| `idx_room_type_amenities_amenity_id` | `amenity_id` | Không |

### hotel_status_history

**Mục đích:** Lịch sử đổi trạng thái bán của khách sạn (approve, suspend, resume, lock, unlock).

**Đã triển khai:** Flyway V20261110120200000, bổ sung so với DBML gốc. Chỉ thêm mới, không sửa/xóa; FK tới hotels không CASCADE để giữ lịch sử (HT-08). CHECK `ck_hotel_status_history_changed`: from_status <> to_status. Lý do bắt buộc khi lock/unlock (kiểm ở API và domain).

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **hotel_id** | uuid | not null | fk_hotel_status_history_hotel | Khách sạn đổi trạng thái. |
| **from_status** | hotel_status__t | not null |  | Trạng thái trước khi đổi. |
| **to_status** | hotel_status__t | not null |  | Trạng thái sau khi đổi; khác from_status. |
| **reason** | varchar(500) | null |  | Lý do; bắt buộc khi khóa/gỡ khóa, tùy chọn với suspend/resume; approve ghi NULL. |
| **changed_by_user_id** | uuid | not null | fk_hotel_status_history_actor | Người thực hiện, lấy từ principal. |
| **changed_at** | timestamptz | not null |  | Thời điểm đổi, lấy từ Clock của ứng dụng (UTC). |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_hotel_status_history_hotel_time` | `hotel_id`, `changed_at` | Không |

## Inventory

### room_inventory

**Mục đích:** Tổng và lượng đang chiếm theo loại phòng/ngày; cần đồng bộ với phòng vật lý.

**Note nguyên văn nguồn:** PLANNED - khóa hàng theo room_type_id và inventory_date khi giữ phòng. CHECK: total_inventory >= 0; reserved_inventory từ 0 đến total_inventory

**Diễn giải / cần review:** reserved_inventory phải khớp reservation HELD/CONFIRMED còn chiếm tồn. Không đếm thêm room_assignments như một lượt bán khác.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **room_type_id** | uuid | pk; not null | fk_room_inventory_room_type | Loại phòng liên quan. |
| **inventory_date** | date | pk; not null |  | Ngày lưu trú được quản lý tồn kho. |
| **total_inventory** | integer | not null |  | Tổng sức chứa theo ngày trong phương án `TON_KHO` đề xuất; nguồn chưa chốt cách tính block. |
| **reserved_inventory** | integer | not null; default: 0 |  | Counter phân bổ theo ngày; đề xuất gồm HELD và CONFIRMED chưa RELEASED. |

#### Index có tên trong nguồn

Không có index có tên khai báo riêng. PK/UNIQUE cột vẫn được giữ ở bảng cột trên.

### room_blocks

**Mục đích:** Khoảng ngày chặn một phòng vật lý; nguồn vẫn còn quantity và ghi chú cũ cần sửa.

**Note nguyên văn nguồn:** PLANNED - chặn một số lượng phòng theo room type; chưa quản lý phòng vật lý riêng lẻ. CHECK: start_date <= end_date; quantity > 0; RELEASED phải có released_at

**Diễn giải / cần review:** Đã có room_id và FK ghép đến rooms, nhưng quantity chưa mặc định/giới hạn bằng 1 và ghi chú còn nói chưa có phòng vật lý. Tài liệu giữ nguyên nguồn và ghi đây là điểm cần sửa.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **room_type_id** | uuid | not null | fk_room_blocks_room_type, fk_room_blocks_physical_room_type | Loại phòng liên quan. |
| **start_date** | date | not null |  | Ngày đầu của khoảng chặn; quy ước đầu/cuối cần chốt `NGAY`. |
| **room_id** | uuid | not null | fk_room_blocks_physical_room_type | Phòng vật lý cụ thể. |
| **end_date** | date | not null |  | Ngày cuối của khoảng chặn; đề xuất tính cả ngày cuối. |
| **quantity** | integer | not null |  | Số phòng/đơn vị phân bổ; phải dương theo quy tắc tương ứng. |
| **reason** | varchar(255) | not null |  | Lý do nghiệp vụ; bắt buộc hoặc nullable giữ theo nguồn. |
| **status** | room_block_status__t | not null; default: ACTIVE |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **created_by_user_id** | uuid | not null | fk_room_blocks_created_by | Người tạo đối tượng/thao tác, tham chiếu users. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **released_at** | timestamptz | null |  | Thời điểm giải phóng/thu hồi phần còn hiệu lực. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_room_blocks_room_dates` | `room_type_id, start_date, end_date` | Không |
| `idx_room_blocks_status_end_date` | `status, end_date` | Không |

### inventory_reservations

**Mục đích:** Ledger giữ/xác nhận/trả tồn theo hạng mục booking và đêm.

**Note nguyên văn nguồn:** PLANNED - ledger giữ tồn kho theo booking item và ngày lưu trú. CHECK: quantity > 0; HELD phải có expires_at; RELEASED phải có released_at

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **booking_item_id** | uuid | pk; not null | fk_inventory_reservations_booking_item_room_type | Hạng mục loại phòng trong đơn. |
| **room_type_id** | uuid | not null | fk_inventory_reservations_booking_item_room_type, fk_inventory_reservations_inventory_row | Loại phòng liên quan. |
| **stay_date** | date | pk; not null | fk_inventory_reservations_inventory_row | Ngày của một đêm đã đặt; phải nằm trong khoảng lưu trú theo quy tắc application. |
| **quantity** | smallint | not null |  | Số phòng/đơn vị phân bổ; phải dương theo quy tắc tương ứng. |
| **status** | inventory_reservation_status__t | not null; default: HELD |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **expires_at** | timestamptz | null |  | Hạn hiệu lực; cần logic kiểm tra, không tự đổi trạng thái khi đến hạn. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **released_at** | timestamptz | null |  | Thời điểm giải phóng/thu hồi phần còn hiệu lực. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_inventory_reservations_status_expires_at` | `status, expires_at` | Không |
| `idx_inventory_reservations_room_date` | `room_type_id, stay_date` | Không |

## Pricing

### room_rates

**Mục đích:** Giá một phòng theo loại phòng và ngày.

**Note nguyên văn nguồn:** PLANNED

**Diễn giải / cần review:** Chỉ có giá theo ngày; chưa có giá cơ bản dự phòng. Nguồn đề xuất amount >= 0, còn luồng thanh toán có amount > 0; cần chốt chính sách giá 0.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **room_type_id** | uuid | pk; not null | fk_room_rates_room_type | Loại phòng liên quan. |
| **rate_date** | date | pk; not null |  | Ngày áp dụng giá cho một đêm lưu trú. |
| **amount** | bigint | not null |  | Số tiền theo đơn vị nhỏ nhất của currency; với VND là đồng. **Ghi chú nguồn:** Đơn vị tiền tệ nhỏ nhất; VND lưu theo đồng. CHECK đề xuất: amount >= 0 |
| **currency** | char(3) | not null; default: 'VND' |  | Mã tiền tệ ba ký tự; cùng luồng booking/payment phải kiểm tra nhất quán. |

#### Index có tên trong nguồn

Không có index có tên khai báo riêng. PK/UNIQUE cột vẫn được giữ ở bảng cột trên.

## Booking

### bookings

**Mục đích:** Đơn đặt theo khách sạn/kỳ lưu trú, thông tin khách và tổng tiền trước/sau voucher.

**Note nguyên văn nguồn:** PLANNED. CHECK: check_in < check_out; adults >= 1; children >= 0; các amount không âm; discount <= subtotal; total = subtotal - discount; PENDING_PAYMENT phải có expires_at

**Diễn giải / cần review:** Có NO_SHOW trong enum. Chưa có khóa chống tạo trùng booking, snapshot giờ/chính sách hoặc thông tin đến muộn có cấu trúc.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **booking_code** | varchar(20) | not null; unique |  | Mã đặt phòng để hiển thị/tra cứu; không dùng làm bằng chứng quyền truy cập. **Ghi chú nguồn:** Ví dụ BK-2026-X9A2 |
| **customer_id** | uuid | not null | fk_bookings_customer | Tài khoản khách tạo/sở hữu booking hoặc review. |
| **hotel_id** | uuid | not null | fk_bookings_hotel | Khách sạn liên quan; phạm vi cụ thể theo bảng. |
| **guest_name_snapshot** | varchar(100) | not null |  | Tên khách được chụp lại lúc đặt, khác hồ sơ user hiện tại. |
| **guest_email_snapshot** | varchar(255) | not null |  | Email liên hệ được chụp lại lúc đặt. |
| **guest_phone_snapshot** | varchar(20) | not null |  | Số điện thoại liên hệ được chụp lại lúc đặt. |
| **check_in** | date | not null |  | Ngày bắt đầu lưu trú; đề xuất có chiếm tồn kho đêm đó. |
| **check_out** | date | not null |  | Ngày kết thúc; đề xuất không chiếm thêm tồn kho/giá đêm đó. |
| **adults** | smallint | not null; default: 1 |  | Số người lớn; ý nghĩa tổng booking/tổng item cần chốt `SO_KHACH`. |
| **children** | smallint | not null; default: 0 |  | Số trẻ em; ý nghĩa tổng booking/tổng item cần chốt `SO_KHACH`. |
| **status** | booking_status__t | not null; default: PENDING_PAYMENT |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **currency** | char(3) | not null; default: 'VND' |  | Mã tiền tệ ba ký tự; cùng luồng booking/payment phải kiểm tra nhất quán. |
| **subtotal_amount** | bigint | not null; default: 0 |  | Tổng tiền trước giảm giá, tính từ số lượng và các giá đêm snapshot. |
| **discount_amount** | bigint | not null; default: 0 |  | Số tiền giảm được ghi nhận; phạm vi/điều kiện theo bảng và quy tắc. |
| **total_amount** | bigint | not null; default: 0 |  | Số tiền sau giảm, bằng subtotal_amount trừ discount_amount theo nguồn. |
| **expires_at** | timestamptz | null |  | Hạn hiệu lực; cần logic kiểm tra, không tự đổi trạng thái khi đến hạn. **Ghi chú nguồn:** Hạn giữ tồn kho cho booking chờ thanh toán |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **updated_at** | timestamptz | not null; default: now() |  | Thời điểm sửa; DEFAULT now() không tự cập nhật khi UPDATE. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_bookings_customer_id` | `customer_id` | Không |
| `idx_bookings_hotel_id` | `hotel_id` | Không |
| `idx_bookings_status_expires_at` | `status, expires_at` | Không |
| `uq_bookings_id_hotel` | `id, hotel_id` | Có |

### booking_items

**Mục đích:** Hạng mục loại phòng, số lượng và số khách trong một đơn.

**Note nguyên văn nguồn:** PLANNED - hotel_id được lặp có chủ đích để DB bảo đảm room type và booking cùng hotel. CHECK: quantity > 0; adults >= 1; children >= 0

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **booking_id** | uuid | not null | fk_booking_items_booking_hotel | Đơn đặt liên quan. |
| **hotel_id** | uuid | not null | fk_booking_items_booking_hotel, fk_booking_items_room_type_hotel | Khách sạn liên quan; phạm vi cụ thể theo bảng. |
| **room_type_id** | uuid | not null | fk_booking_items_room_type_hotel | Loại phòng liên quan. |
| **room_type_name_snapshot** | varchar(100) | not null |  | Tên loại phòng chụp tại thời điểm đặt. |
| **quantity** | smallint | not null; default: 1 |  | Số phòng/đơn vị phân bổ; phải dương theo quy tắc tương ứng. |
| **adults** | smallint | not null; default: 1 |  | Số người lớn; ý nghĩa tổng booking/tổng item cần chốt `SO_KHACH`. |
| **children** | smallint | not null; default: 0 |  | Số trẻ em; ý nghĩa tổng booking/tổng item cần chốt `SO_KHACH`. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_booking_items_booking_id` | `booking_id` | Không |
| `idx_booking_items_room_type_id` | `room_type_id` | Không |
| `uq_booking_items_id_room_type` | `id, room_type_id` | Có |

### booking_night_prices

**Mục đích:** Giá từng đêm được lưu cố định cho hạng mục tại lúc đặt.

**Note nguyên văn nguồn:** PLANNED - snapshot giá từng đêm tại thời điểm đặt

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **booking_item_id** | uuid | pk; not null | fk_booking_night_prices_booking_item | Hạng mục loại phòng trong đơn. |
| **stay_date** | date | pk; not null |  | Ngày của một đêm đã đặt; phải nằm trong khoảng lưu trú theo quy tắc application. |
| **unit_price** | bigint | not null |  | Giá một phòng cho đêm đó; không nhân quantity sẵn. **Ghi chú nguồn:** Giá một phòng trong đêm đó. CHECK đề xuất: unit_price >= 0 |

#### Index có tên trong nguồn

Không có index có tên khai báo riêng. PK/UNIQUE cột vẫn được giữ ở bảng cột trên.

### booking_status_history

**Mục đích:** Lịch sử chuyển trạng thái đơn, người thực hiện và lý do.

**Note nguyên văn nguồn:** PLANNED - append-only audit trail

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **booking_id** | uuid | not null | fk_booking_status_history_booking | Đơn đặt liên quan. |
| **from_status** | booking_status__t | null |  | Trạng thái booking trước khi đổi; NULL ở dòng lịch sử đầu tiên. **Ghi chú nguồn:** Null ở bản ghi trạng thái đầu tiên |
| **to_status** | booking_status__t | not null |  | Trạng thái booking sau khi đổi. |
| **changed_by_user_id** | uuid | null | fk_booking_status_history_actor | Người đổi trạng thái; NULL dành cho tác động hệ thống. **Ghi chú nguồn:** Null nếu trạng thái do hệ thống thay đổi |
| **reason** | varchar(255) | null |  | Lý do nghiệp vụ; bắt buộc hoặc nullable giữ theo nguồn. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_booking_status_history_booking_time` | `booking_id, created_at` | Không |

### complaints

**Mục đích:** Khiếu nại của khách gắn với booking (BK-21).

**Đã triển khai (tuần 1):** Flyway V20261110140000000, bổ sung so với DBML gốc. Chỉ tiếp nhận: khách gửi (chỉ chủ booking, mọi trạng thái trừ PENDING_PAYMENT), xem danh sách/chi tiết của mình. Chưa có đính kèm bằng chứng (`complaint_attachments`), lịch sử xử lý và API quản trị: để tuần 2. Thêm loại khiếu nại bằng `ALTER TYPE complaint_category__t ADD VALUE`.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null |  | Định danh khiếu nại, sinh ở application. |
| **booking_id** | uuid | not null | fk_complaints_booking_hotel | Đơn đặt bị khiếu nại. |
| **customer_id** | uuid | not null | fk_complaints_customer | Chủ booking lúc gửi, lấy từ principal. |
| **hotel_id** | uuid | not null | fk_complaints_booking_hotel | Chép từ booking; FK ghép (booking_id, hotel_id) bảo đảm khớp booking. |
| **category** | complaint_category__t | not null |  | ROOM_CONDITION, SERVICE_QUALITY, STAFF_BEHAVIOR, BILLING, CANCELLATION_REFUND, OFF_PLATFORM_PAYMENT, OTHER. |
| **content** | text | not null; CHECK `ck_complaints_content` 1–2000 ký tự sau btrim |  | Nội dung; OTHER cần tối thiểu 20 ký tự (kiểm ở domain). |
| **status** | complaint_status__t | not null; default: OPEN |  | OPEN, IN_REVIEW, RESOLVED, REJECTED; tuần 1 chỉ tạo OPEN. |
| **created_at** | timestamptz | not null |  | Thời điểm gửi (Clock, cắt về micro giây). |
| **updated_at** | timestamptz | not null |  | Thời điểm sửa gần nhất. |
| **version** | bigint | not null; default: 0; CHECK >= 0 |  | Optimistic lock cho xử lý ở tuần 2. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `uq_complaints_open_per_booking` | `booking_id` WHERE status IN ('OPEN', 'IN_REVIEW') | Có (partial): mỗi booking tối đa một khiếu nại đang xử lý; chèn bằng `ON CONFLICT ... DO NOTHING` nên request đồng thời nhận 409 `COMPLAINT_ALREADY_OPEN` kèm `openComplaintId` |
| `idx_complaints_customer_created` | `customer_id, created_at DESC, id` | Không |

### room_assignments

**Mục đích:** Phân công phòng vật lý cho hạng mục booking và lịch sử nhận/trả/giải phóng.

**Note nguyên văn nguồn:** Khoảng phân công [starts_at, ends_at). Một booking item có quantity = 2 cần hai phân công phòng khác nhau. Đổi phân công phải giữ bản ghi cũ. Không trừ tồn hoặc thu tiền thêm khi gán phòng. Chống trùng lịch và giới hạn số phân công phải thực hiện thêm trong migration/backend.

**Diễn giải / cần review:** Hai FK ghép buộc phòng và booking item cùng room_type_id; room type toàn cục suy ra cùng hotel. Chưa có ràng buộc chống chồng lịch hoặc kiểm tra tổng số phân công.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **booking_item_id** | uuid | not null | fk_room_assignments_booking_item_type | Hạng mục loại phòng trong đơn. |
| **room_type_id** | uuid | not null | fk_room_assignments_booking_item_type, fk_room_assignments_room_type | Loại phòng liên quan. |
| **room_id** | uuid | not null | fk_room_assignments_room_type | Phòng vật lý cụ thể. |
| **room_number_snapshot** | varchar(20) | not null |  | Số phòng được lưu tại lúc phân công để giữ lịch sử. |
| **starts_at** | timestamptz | not null |  | Thời điểm bắt đầu hiệu lực. |
| **ends_at** | timestamptz | not null |  | Thời điểm kết thúc hiệu lực. |
| **status** | room_assignment_status__t | not null; default: ASSIGNED |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **assigned_by** | uuid | not null | fk_room_assignments_assigned_by | Nhân viên phân công phòng. |
| **assigned_at** | timestamptz | not null; default: now() |  | Thời điểm phân công. |
| **checked_in_at** | timestamptz | null |  | Thời điểm nhận phòng thực tế. |
| **checked_out_at** | timestamptz | null |  | Thời điểm trả phòng thực tế. |
| **released_at** | timestamptz | null |  | Thời điểm giải phóng/thu hồi phần còn hiệu lực. |
| **released_by** | uuid | null | fk_room_assignments_released_by | Nhân sự giải phóng phân công; cần định nghĩa khi hệ thống thực hiện. |
| **release_reason** | varchar(255) | null |  | Lý do kết thúc/đổi phân công. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_room_assignments_item_status` | `booking_item_id, status` | Không |
| `idx_room_assignments_room_period` | `room_id, starts_at, ends_at` | Không |

## Payment

### hotel_contracts

**Mục đích:** Hợp đồng hoa hồng giữa nền tảng và Owner của một khách sạn (PM-16).

**Đã triển khai:** Flyway V20261104190000000 (bảng) và V20261110130000000 (exclusion constraint, bổ sung so với DBML gốc). Điều khoản không sửa sau khi tạo; đổi điều khoản bằng hợp đồng mới. API chỉ chuyển trạng thái DRAFT → ACTIVE (activate) và DRAFT/ACTIVE → TERMINATED (terminate). Module khác đọc hợp đồng hiệu lực qua `ContractDirectory.findEffective(hotelId, at)`: ACTIVE và `starts_at <= at < ends_at`.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh hợp đồng. |
| **contract_code** | varchar(100) | not null; unique (`uq_hotel_contracts_code`) |  | Mã hợp đồng; API giới hạn 50 ký tự. |
| **hotel_id** | uuid | not null | references hotels(id) ON DELETE CASCADE | Khách sạn áp dụng. |
| **owner_user_id** | uuid | not null | references users(id) | Owner ký hợp đồng. |
| **commission_rate** | numeric(5,2) | not null; CHECK 0.00–100.00 |  | Tỷ lệ hoa hồng (%), kiểu chính xác. |
| **commission_basis** | commission_basis__t | not null |  | BEFORE_DISCOUNT, AFTER_DISCOUNT, AFTER_HOTEL_DISCOUNT. |
| **starts_at** | timestamptz | not null |  | Bắt đầu hiệu lực (tính cả thời điểm này). |
| **ends_at** | timestamptz | not null; CHECK ends_at > starts_at |  | Kết thúc hiệu lực (không tính thời điểm này): khoảng `[starts_at, ends_at)`. |
| **status** | hotel_contract_status__t | not null; default: DRAFT |  | DRAFT, ACTIVE, EXPIRED, TERMINATED; chưa có job tự chuyển EXPIRED. |
| **created_by_user_id** | uuid | not null | references users(id) | ADMIN tạo, lấy từ principal. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo. |
| **updated_at** | timestamptz | not null; default: now() |  | Thời điểm đổi trạng thái gần nhất. |

#### Ràng buộc và index

| Tên | Định nghĩa | Ghi chú |
| --- | --- | --- |
| `ex_hotel_contracts_active_period` | `EXCLUDE USING gist (hotel_id WITH =, tstzrange(starts_at, ends_at, '[)') WITH &&) WHERE (status = 'ACTIVE')` | Không cho hai hợp đồng ACTIVE cùng khách sạn chồng hiệu lực, kể cả hai giao dịch song song; hai khoảng nối tiếp không bị coi là chồng. Cần extension `btree_gist`. Vi phạm (SQLSTATE 23P01) được đổi thành 409 `ACTIVE_CONTRACT_PERIOD_OVERLAP`. |
| `idx_hotel_contracts_hotel` | `hotel_id, status` | Không UNIQUE. |

### payment_attempts

**Mục đích:** Các lần yêu cầu thanh toán và tham chiếu cổng.

**Note nguyên văn nguồn:** PLANNED. CHECK: attempt_number > 0; amount > 0; SUCCEEDED phải có succeeded_at

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **booking_id** | uuid | not null | fk_payment_attempts_booking | Đơn đặt liên quan. |
| **attempt_number** | smallint | not null; default: 1 |  | Số thứ tự thử thanh toán trong một booking, bắt đầu từ 1. |
| **method** | payment_method__t | not null |  | Phương thức thanh toán; chưa có danh sách giá trị hoặc mapping provider đầy đủ. |
| **status** | payment_status__t | not null; default: PENDING |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **amount** | bigint | not null |  | Số tiền theo đơn vị nhỏ nhất của currency; với VND là đồng. |
| **currency** | char(3) | not null; default: 'VND' |  | Mã tiền tệ ba ký tự; cùng luồng booking/payment phải kiểm tra nhất quán. |
| **idempotency_key** | varchar(255) | not null; unique |  | Khóa chống tạo/xử lý lặp; nội dung cùng key phải được so sánh tại application. |
| **gateway_transaction_ref** | varchar(255) | null |  | Tham chiếu giao dịch từ cổng; NULL khi chưa được cấp/ghi nhận. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **updated_at** | timestamptz | not null; default: now() |  | Thời điểm sửa; DEFAULT now() không tự cập nhật khi UPDATE. |
| **succeeded_at** | timestamptz | null |  | Thời điểm xác nhận thanh toán/hoàn tiền thành công. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `uq_payment_attempts_booking_attempt` | `booking_id, attempt_number` | Có |
| `uq_payment_attempts_gateway_ref` | `method, gateway_transaction_ref` | Có |
| `idx_payment_attempts_status_created` | `status, created_at` | Không |

### payment_webhook_events

**Mục đích:** Thông báo kết quả từ cổng và kết quả xử lý chống lặp.

**Note nguyên văn nguồn:** PLANNED - provider và provider_event_id chống xử lý webhook lặp. CHECK: PROCESSED phải có processed_at

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **payment_attempt_id** | uuid | null | fk_payment_webhook_events_attempt | Lần thanh toán liên quan; nullable riêng với webhook chưa ánh xạ. **Ghi chú nguồn:** Có thể null nếu chưa ánh xạ được payment attempt |
| **provider** | varchar(50) | not null |  | Tên nhà cung cấp webhook; định danh cần thống nhất với cấu hình thanh toán. |
| **provider_event_id** | varchar(255) | not null |  | Định danh sự kiện của provider; chống trùng cùng provider. |
| **event_type** | varchar(100) | not null |  | Loại sự kiện; contract payload và version cần được quy định. |
| **payload** | jsonb | not null |  | Dữ liệu JSON; hợp đồng, giới hạn và lọc dữ liệu nhạy cảm cần được thiết kế. |
| **processing_status** | webhook_processing_status__t | not null; default: PENDING |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **error_message** | text | null |  | Lỗi xử lý webhook, tránh ghi secret hoặc dữ liệu thanh toán nhạy cảm. |
| **received_at** | timestamptz | not null; default: now() |  | Thời điểm hệ thống tiếp nhận webhook. |
| **processed_at** | timestamptz | null |  | Thời điểm hoàn thành xử lý webhook; bắt buộc khi PROCESSED theo nguồn. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `uq_payment_webhook_provider_event` | `provider, provider_event_id` | Có |
| `idx_payment_webhook_status_received` | `processing_status, received_at` | Không |
| `idx_payment_webhook_attempt_id` | `payment_attempt_id` | Không |

### refunds

**Mục đích:** Yêu cầu hoàn tiền gắn với khoản thu và booking.

**Note nguyên văn nguồn:** PLANNED - hoàn tiền gắn với payment attempt đã thanh toán. CHECK: amount > 0; SUCCEEDED phải có succeeded_at

**Diễn giải / cần review:** Hai FK riêng chưa bảo đảm booking_id thuộc payment_attempt_id. Tổng hoàn nhiều dòng cần kiểm tra đồng thời, không chỉ một CHECK từng dòng.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **payment_attempt_id** | uuid | not null | fk_refunds_payment_attempt | Lần thanh toán liên quan; nullable riêng với webhook chưa ánh xạ. |
| **booking_id** | uuid | not null | fk_refunds_booking | Đơn đặt liên quan. |
| **amount** | bigint | not null |  | Số tiền theo đơn vị nhỏ nhất của currency; với VND là đồng. |
| **currency** | char(3) | not null; default: 'VND' |  | Mã tiền tệ ba ký tự; cùng luồng booking/payment phải kiểm tra nhất quán. |
| **status** | refund_status__t | not null; default: PENDING |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **idempotency_key** | varchar(255) | not null; unique |  | Khóa chống tạo/xử lý lặp; nội dung cùng key phải được so sánh tại application. |
| **provider_refund_ref** | varchar(255) | null |  | Tham chiếu hoàn tiền từ provider; phạm vi duy nhất chưa được nguồn ràng buộc. |
| **reason** | varchar(255) | not null |  | Lý do nghiệp vụ; bắt buộc hoặc nullable giữ theo nguồn. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **updated_at** | timestamptz | not null; default: now() |  | Thời điểm sửa; DEFAULT now() không tự cập nhật khi UPDATE. |
| **succeeded_at** | timestamptz | null |  | Thời điểm xác nhận thanh toán/hoàn tiền thành công. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_refunds_payment_attempt_id` | `payment_attempt_id` | Không |
| `idx_refunds_booking_id` | `booking_id` | Không |
| `idx_refunds_status_created` | `status, created_at` | Không |

## Media

### media_objects

**Mục đích:** Metadata tệp lưu ở object storage.

**Note nguyên văn nguồn:** PLANNED - metadata cho S3, R2 hoặc MinIO; không lưu presigned URL. CHECK: size_bytes > 0; status DELETED phải có deleted_at

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **storage_provider** | varchar(32) | not null |  | Nhà cung cấp lưu trữ, ví dụ S3, R2, MINIO theo nguồn. **Ghi chú nguồn:** S3, R2 hoặc MINIO |
| **bucket_name** | varchar(100) | not null |  | Tên bucket chứa object. |
| **object_key** | varchar(512) | not null |  | Khóa bền vững của object trong bucket; không phải presigned URL. |
| **original_filename** | varchar(255) | not null |  | Tên tệp ban đầu để hiển thị; không dùng trực tiếp làm đường dẫn tin cậy. |
| **content_type** | varchar(100) | not null |  | MIME type metadata; phải được xác minh theo quy trình upload. |
| **size_bytes** | bigint | not null |  | Kích thước object tính bằng byte, dương theo ý định nguồn. |
| **checksum_sha256** | varchar(64) | null |  | Dữ liệu checksum_sha256; ý nghĩa trong mục đích và quy tắc của bảng. |
| **status** | media_status__t | not null; default: PENDING |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **created_by_user_id** | uuid | not null | fk_media_objects_created_by | Người tạo đối tượng/thao tác, tham chiếu users. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **deleted_at** | timestamptz | null |  | Thời điểm object được xác nhận đã xóa theo quy trình Media đề xuất. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `uq_media_objects_storage_key` | `storage_provider, bucket_name, object_key` | Có |
| `idx_media_objects_created_by` | `created_by_user_id` | Không |
| `idx_media_objects_status` | `status` | Không |

### hotel_images

**Mục đích:** Ảnh và ảnh cover của khách sạn.

**Note nguyên văn nguồn:** PLANNED - mỗi hotel chỉ nên có một ảnh cover; tạo partial unique index trong migration

**Diễn giải / cần review:** Một cover/hotel mới là ghi chú; index display_order không thay thế partial UNIQUE cover.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **hotel_id** | uuid | pk; not null | fk_hotel_images_hotel | Khách sạn liên quan; phạm vi cụ thể theo bảng. |
| **media_object_id** | uuid | pk; not null | fk_hotel_images_media | Metadata tệp ảnh liên quan. |
| **display_order** | integer | not null; default: 0 |  | Thứ tự ảnh trong một hotel/room type; nguồn yêu cầu duy nhất trong phạm vi đó. **Ghi chú nguồn:** CHECK đề xuất: display_order >= 0 |
| **is_cover** | boolean | not null; default: false |  | Đánh dấu ảnh đại diện; tối đa một cover cần partial UNIQUE. |
| **alt_text** | varchar(255) | null |  | Mô tả ảnh cho hiển thị và khả năng tiếp cận. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `uq_hotel_images_display_order` | `hotel_id, display_order` | Có |
| `idx_hotel_images_media_object_id` | `media_object_id` | Không |

### room_type_images

**Mục đích:** Ảnh và ảnh cover của loại phòng.

**Note nguyên văn nguồn:** PLANNED - mỗi room type chỉ nên có một ảnh cover; tạo partial unique index trong migration

**Diễn giải / cần review:** Một cover/loại phòng mới là ghi chú; index display_order không thay thế partial UNIQUE cover.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **room_type_id** | uuid | pk; not null | fk_room_type_images_room_type | Loại phòng liên quan. |
| **media_object_id** | uuid | pk; not null | fk_room_type_images_media | Metadata tệp ảnh liên quan. |
| **display_order** | integer | not null; default: 0 |  | Thứ tự ảnh trong một hotel/room type; nguồn yêu cầu duy nhất trong phạm vi đó. **Ghi chú nguồn:** CHECK đề xuất: display_order >= 0 |
| **is_cover** | boolean | not null; default: false |  | Đánh dấu ảnh đại diện; tối đa một cover cần partial UNIQUE. |
| **alt_text** | varchar(255) | null |  | Mô tả ảnh cho hiển thị và khả năng tiếp cận. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `uq_room_type_images_display_order` | `room_type_id, display_order` | Có |
| `idx_room_type_images_media_object_id` | `media_object_id` | Không |

### user_images

**Mục đích:** Thư viện ảnh tài khoản và ảnh đại diện hiện hành.

**Note nguyên văn nguồn:** Không giới hạn số lượng ảnh. Mỗi media object ở thư viện này thuộc một tài khoản. Cần partial unique index trên user_id WHERE is_avatar = true trong migration để mỗi tài khoản có tối đa một avatar.

**Diễn giải / cần review:** media_object_id UNIQUE nên một ảnh thư viện thuộc tối đa một tài khoản. Một avatar/user mới được yêu cầu bằng Note, chưa có partial unique index trong danh sách nguồn.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **user_id** | uuid | pk; not null | fk_user_images_user | Tài khoản liên quan. |
| **media_object_id** | uuid | pk; not null; unique | fk_user_images_media | Metadata tệp ảnh liên quan. |
| **is_avatar** | boolean | not null; default: false |  | Có phải ảnh đại diện hiện hành của tài khoản không. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_user_images_user_avatar` | `user_id, is_avatar` | Không |

## Review

### reviews

**Mục đích:** Đánh giá của người đặt sau khi kết thúc lưu trú.

**Note nguyên văn nguồn:** PLANNED - một booking chỉ có một review và chỉ tạo sau CHECKED_OUT

**Diễn giải / cần review:** Có booking_id duy nhất và enum PUBLISHED/HIDDEN. Các FK riêng chưa buộc customer/hotel khớp booking; quyền và CHECKED_OUT phải kiểm tra thêm.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **booking_id** | uuid | not null; unique | fk_reviews_booking | Đơn đặt liên quan. |
| **customer_id** | uuid | not null | fk_reviews_customer | Tài khoản khách tạo/sở hữu booking hoặc review. |
| **hotel_id** | uuid | not null | fk_reviews_hotel | Khách sạn liên quan; phạm vi cụ thể theo bảng. |
| **rating** | smallint | not null |  | Điểm đánh giá của khách, từ 1 đến 5 theo CHECK được nguồn đề xuất. **Ghi chú nguồn:** CHECK đề xuất: rating BETWEEN 1 AND 5 |
| **title** | varchar(150) | null |  | Tiêu đề nội dung. |
| **content** | text | null |  | Nội dung đánh giá của khách. |
| **status** | review_status__t | not null; default: PUBLISHED |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **updated_at** | timestamptz | not null; default: now() |  | Thời điểm sửa; DEFAULT now() không tự cập nhật khi UPDATE. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_reviews_hotel_status_created` | `hotel_id, status, created_at` | Không |
| `idx_reviews_customer_id` | `customer_id` | Không |

## Promotion

### promotions

**Mục đích:** Voucher: mã, phạm vi, điều kiện áp dụng, cách giảm và mức giảm tối đa.

**Note nguyên văn nguồn:** PLANNED - hotel_id null nghĩa là khuyến mãi toàn hệ thống. CHECK: starts_at < ends_at; giới hạn dương; chỉ percentage_off hoặc fixed_amount được có giá trị theo discount_type

**Diễn giải / cần review:** hotel_id NULL là toàn hệ thống; có giá trị thì chỉ hotel đó. FK hiện ON DELETE SET NULL có thể biến mã riêng thành mã toàn hệ thống khi xóa hotel: cần ngăn bằng chính sách/ràng buộc phù hợp.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **hotel_id** | uuid | null | fk_promotions_hotel | NULL: voucher toàn hệ thống; có giá trị: voucher riêng của khách sạn này. |
| **code** | varchar(50) | not null; unique |  | Mã voucher duy nhất toàn bảng; chuẩn hóa mã là quy tắc backend, chưa có CHECK trong nguồn. |
| **name** | varchar(150) | not null |  | Tên nghiệp vụ của đối tượng. |
| **description** | text | null |  | Mô tả bổ sung, có thể chưa được cung cấp nếu nullable. |
| **discount_type** | promotion_discount_type__t | not null |  | PERCENTAGE hoặc FIXED_AMOUNT. |
| **percentage_off** | decimal(5,2) | null |  | Tỷ lệ giảm cho PERCENTAGE, ví dụ 10 là giảm 10%. |
| **fixed_amount** | bigint | null |  | Số tiền giảm cho FIXED_AMOUNT. |
| **max_discount_amount** | bigint | null |  | Trần tiền giảm khi được cấu hình; NULL là không có trần riêng, vẫn phải kiểm tra tổng đơn hợp lệ. |
| **min_booking_amount** | bigint | not null; default: 0 |  | Giá trọn gói trước giảm tối thiểu để áp voucher. |
| **usage_limit** | integer | null |  | Giới hạn tổng lượt theo chiến dịch; NULL là không đặt giới hạn tổng. |
| **per_user_limit** | integer | not null; default: 1 |  | Giới hạn lượt mỗi người dùng; mặc định nguồn là 1. |
| **starts_at** | timestamptz | not null |  | Thời điểm bắt đầu nhận áp mã; đề xuất xét theo lúc tạo đơn. |
| **ends_at** | timestamptz | not null |  | Thời điểm ngừng nhận áp mã; đề xuất khoảng starts_at <= lúc đặt < ends_at. |
| **status** | promotion_status__t | not null; default: DRAFT |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |
| **updated_at** | timestamptz | not null; default: now() |  | Thời điểm sửa; DEFAULT now() không tự cập nhật khi UPDATE. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_promotions_hotel_id` | `hotel_id` | Không |
| `idx_promotions_status_period` | `status, starts_at, ends_at` | Không |

### promotion_redemptions

**Mục đích:** Voucher đã áp cho đơn và số tiền thực tế được giảm.

**Note nguyên văn nguồn:** PLANNED - snapshot số tiền giảm; MVP giới hạn một promotion cho mỗi booking

**Diễn giải / cần review:** UNIQUE booking_id giới hạn một voucher/đơn. discount_amount lưu khoản giảm đã tính; chưa có snapshot riêng mã/điều kiện voucher.

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **promotion_id** | uuid | not null | fk_promotion_redemptions_promotion | Chương trình khuyến mãi được áp dụng. |
| **booking_id** | uuid | not null; unique | fk_promotion_redemptions_booking | Đơn đặt liên quan. |
| **user_id** | uuid | not null | fk_promotion_redemptions_user | Tài khoản liên quan. |
| **discount_amount** | bigint | not null |  | Số tiền thực tế được giảm tại lúc áp voucher cho booking; không tự tính lại từ chiến dịch đã sửa. **Ghi chú nguồn:** CHECK đề xuất: discount_amount > 0 |
| **status** | promotion_redemption_status__t | not null; default: APPLIED |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **redeemed_at** | timestamptz | not null; default: now() |  | Thời điểm ghi nhận áp mã, không tự có nghĩa booking đã thanh toán. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_promotion_redemptions_promotion_user` | `promotion_id, user_id` | Không |
| `idx_promotion_redemptions_user_id` | `user_id` | Không |

## Notification

### notifications

**Mục đích:** Thông báo trong ứng dụng theo người nhận.

**Note nguyên văn nguồn:** PLANNED - thông báo trong ứng dụng; delivery email hoặc push có thể tách bảng sau

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **recipient_user_id** | uuid | not null | fk_notifications_recipient | Người nhận thông báo, tham chiếu users. |
| **type** | varchar(100) | not null |  | Mã loại thông báo; chưa có danh mục contract trong nguồn. |
| **title** | varchar(200) | not null |  | Tiêu đề nội dung. |
| **body** | text | not null |  | Nội dung thông báo trong ứng dụng. |
| **payload** | jsonb | null |  | Dữ liệu JSON; hợp đồng, giới hạn và lọc dữ liệu nhạy cảm cần được thiết kế. |
| **dedupe_key** | varchar(255) | null; unique |  | Khóa chống trùng toàn bảng; đề xuất gồm cả recipient và event id. |
| **read_at** | timestamptz | null |  | Thời điểm đã đọc; NULL khi chưa đọc. |
| **created_at** | timestamptz | not null; default: now() |  | Thời điểm tạo bản ghi. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_notifications_recipient_read_created` | `recipient_user_id, read_at, created_at` | Không |

## Outbox

### outbox_events

**Mục đích:** Sự kiện lưu để xử lý/gửi bất đồng bộ.

**Note nguyên văn nguồn:** PLANNED - ghi cùng transaction nghiệp vụ rồi publisher gửi bất đồng bộ. CHECK: attempts >= 0; PUBLISHED phải có published_at

#### Cột

| Cột | Kiểu nguồn | Ràng buộc/default nguồn | Tên FK nguồn | Ý nghĩa và ghi chú |
| --- | --- | --- | --- | --- |
| **id** | uuid | pk; not null; default: gen_random_uuid() |  | Định danh duy nhất của bản ghi. |
| **aggregate_type** | varchar(100) | not null |  | Loại đối tượng nghiệp vụ phát sự kiện, ví dụ Booking. |
| **aggregate_id** | uuid | not null |  | ID đối tượng nghiệp vụ; không có FK đa hình đến các bảng trong nguồn. |
| **event_type** | varchar(150) | not null |  | Loại sự kiện; contract payload và version cần được quy định. |
| **payload** | jsonb | not null |  | Dữ liệu JSON; hợp đồng, giới hạn và lọc dữ liệu nhạy cảm cần được thiết kế. |
| **status** | outbox_event_status__t | not null; default: PENDING |  | Trạng thái theo enum nguồn; xem bảng đầy đủ tại business-rules.md. |
| **occurred_at** | timestamptz | not null; default: now() |  | Thời điểm sự kiện nghiệp vụ xảy ra. |
| **available_at** | timestamptz | not null; default: now() |  | Thời điểm sớm nhất publisher được thử phát. |
| **published_at** | timestamptz | null |  | Thời điểm ghi nhận phát thành công; không chứng minh mọi consumer đã xử lý. |
| **attempts** | integer | not null; default: 0 |  | Số lần thử phát sự kiện; không âm theo ý định nguồn. |
| **last_error** | text | null |  | Lỗi phát sự kiện gần nhất, cần lọc dữ liệu nhạy cảm. |

#### Index có tên trong nguồn

| Tên | Cột theo thứ tự | UNIQUE |
| --- | --- | --- |
| `idx_outbox_events_status_available` | `status, available_at` | Không |
| `idx_outbox_events_aggregate` | `aggregate_type, aggregate_id` | Không |
