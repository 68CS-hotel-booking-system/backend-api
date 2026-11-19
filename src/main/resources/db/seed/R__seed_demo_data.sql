-- =====================================================================================
-- Dữ liệu demo CHỈ cho profile dev.
--
-- Chỉ application-dev.yaml thêm classpath:db/seed vào spring.flyway.locations; mặc định
-- (application.yaml), test, staging và prod chỉ chạy classpath:db/migration nên không có file này.
--
-- Đây là repeatable migration của Flyway: chạy sau mọi migration versioned và chạy lại khi nội dung
-- file đổi. Mọi INSERT dùng UUID cố định + ON CONFLICT DO NOTHING nên chạy nhiều lần không nhân đôi.
-- Hệ quả: sửa một dòng đã seed KHÔNG tự cập nhật DB dev cũ; muốn làm mới thì reset DB dev
-- (docker compose down -v) rồi chạy lại ứng dụng.
-- Ai đổi schema làm file này lỗi thì sửa file này cùng thay đổi schema.
--
-- Không dùng CURRENT_DATE/now(): mọi ngày giờ là mốc UTC cố định. Giá/tồn phủ 2026-11-01 → 2027-01-31.
-- Tài khoản owner seed KHÔNG đăng nhập được: password_hash '!seed-no-login' không phải bcrypt.
-- Không seed media (không có object R2 thật).
--
-- Các ca để thử tìm kiếm và duyệt (HT-03, HT-05, IV-13, PR-02):
--   ACTIVE đủ tồn và giá mọi đêm ......... Hà Nội Phố Cổ, Sài Gòn Riverside, Đà Nẵng Mỹ Khê, Huế Hoàng Thành
--   ACTIVE có loại phòng 1 khách .......... Hà Nội Hồ Tây (Phòng đơn, max_occupancy 1)
--   ACTIVE thiếu tồn đêm 2026-12-24 ....... Sài Gòn Bến Thành (room_blocks chặn hết phòng đêm đó)
--   ACTIVE thiếu giá đêm 2026-12-31 ....... Đà Nẵng Sơn Trà (base_price 0, không có room_rates đêm đó)
--   SUSPENDED (không hiện công khai) ...... Huế Sông Hương
--   DRAFT đủ HT-03 (approve → 200) ........ Hà Nội Long Biên
--   DRAFT thiếu liên hệ (approve → 422) ... Sài Gòn Thảo Điền
-- =====================================================================================

-- 1. Owner seed (role HOTEL_OWNER đã có từ migration V20261104100000000).
INSERT INTO users (id, email, password_hash, full_name, phone, status, email_verified_at,
                   is_deleted, created_at, updated_at)
VALUES
    ('de0000a0-0000-4000-8000-000000000001', 'owner.demo1@example.com', '!seed-no-login',
     'Chủ khách sạn demo 1', '+84900000001', 'active', '2026-11-01T00:00:00Z'::timestamptz, false,
     '2026-11-01T00:00:00Z'::timestamptz, '2026-11-01T00:00:00Z'::timestamptz),
    ('de0000a0-0000-4000-8000-000000000002', 'owner.demo2@example.com', '!seed-no-login',
     'Chủ khách sạn demo 2', '+84900000002', 'active', '2026-11-01T00:00:00Z'::timestamptz, false,
     '2026-11-01T00:00:00Z'::timestamptz, '2026-11-01T00:00:00Z'::timestamptz)
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles (id, user_id, role_id, assigned_at)
SELECT seed.id, seed.user_id, r.id, '2026-11-01T00:00:00Z'::timestamptz
FROM (VALUES
          ('de0000a1-0000-4000-8000-000000000001'::uuid, 'de0000a0-0000-4000-8000-000000000001'::uuid),
          ('de0000a1-0000-4000-8000-000000000002'::uuid, 'de0000a0-0000-4000-8000-000000000002'::uuid)
     ) AS seed(id, user_id)
JOIN roles r ON r.role_key = 'HOTEL_OWNER'
ON CONFLICT DO NOTHING;

-- 2. Khách sạn. Thành phố lấy theo slug đã seed ở V20261104154000000.
INSERT INTO hotels (id, owner_user_id, city_id, name, slug, description, address, contact_phone,
                    contact_email, latitude, longitude, star_rating, check_in_time, check_out_time,
                    status, created_at, updated_at, version)
SELECT h.id, h.owner_id, c.id, h.name, h.slug, h.description, h.address, h.phone, h.email,
       h.lat, h.lng, h.stars, '14:00'::time, '12:00'::time, h.status::hotel_status__t,
       '2026-11-01T00:00:00Z'::timestamptz, '2026-11-01T00:00:00Z'::timestamptz, 0
FROM (VALUES
    ('de0000b0-0000-4000-8000-000000000001'::uuid, 'de0000a0-0000-4000-8000-000000000001'::uuid,
     'ha-noi', 'Demo Hà Nội Phố Cổ', 'demo-ha-noi-pho-co', 'Khách sạn demo gần Hồ Hoàn Kiếm.',
     '12 Hàng Bạc, Hoàn Kiếm', '+84240000001', 'phoco@demo.example.com',
     21.034000, 105.852000, 4::smallint, 'ACTIVE'),
    ('de0000b0-0000-4000-8000-000000000002'::uuid, 'de0000a0-0000-4000-8000-000000000001'::uuid,
     'ha-noi', 'Demo Hà Nội Hồ Tây', 'demo-ha-noi-ho-tay', 'Khách sạn demo có phòng đơn một khách.',
     '45 Quảng An, Tây Hồ', '+84240000002', NULL,
     21.065000, 105.823000, 3::smallint, 'ACTIVE'),
    ('de0000b0-0000-4000-8000-000000000003'::uuid, 'de0000a0-0000-4000-8000-000000000001'::uuid,
     'ha-noi', 'Demo Hà Nội Long Biên', 'demo-ha-noi-long-bien', 'DRAFT đủ điều kiện HT-03.',
     '8 Nguyễn Văn Cừ, Long Biên', NULL, 'longbien@demo.example.com',
     21.043000, 105.873000, 3::smallint, 'DRAFT'),
    ('de0000b0-0000-4000-8000-000000000004'::uuid, 'de0000a0-0000-4000-8000-000000000002'::uuid,
     'ho-chi-minh', 'Demo Sài Gòn Riverside', 'demo-sai-gon-riverside', 'Khách sạn demo ven sông.',
     '20 Tôn Đức Thắng, Quận 1', '+84280000004', 'riverside@demo.example.com',
     10.776000, 106.706000, 5::smallint, 'ACTIVE'),
    ('de0000b0-0000-4000-8000-000000000005'::uuid, 'de0000a0-0000-4000-8000-000000000002'::uuid,
     'ho-chi-minh', 'Demo Sài Gòn Bến Thành', 'demo-sai-gon-ben-thanh',
     'Hết phòng đêm 2026-12-24 (đã chặn bảo trì).',
     '5 Lê Lợi, Quận 1', '+84280000005', NULL,
     10.772000, 106.698000, 4::smallint, 'ACTIVE'),
    ('de0000b0-0000-4000-8000-000000000006'::uuid, 'de0000a0-0000-4000-8000-000000000002'::uuid,
     'ho-chi-minh', 'Demo Sài Gòn Thảo Điền', 'demo-sai-gon-thao-dien',
     'DRAFT thiếu liên hệ để thử approve trả 422.',
     '30 Xuân Thủy, Thủ Đức', NULL, NULL,
     10.803000, 106.735000, 4::smallint, 'DRAFT'),
    ('de0000b0-0000-4000-8000-000000000007'::uuid, 'de0000a0-0000-4000-8000-000000000001'::uuid,
     'da-nang', 'Demo Đà Nẵng Mỹ Khê', 'demo-da-nang-my-khe', 'Khách sạn demo sát biển Mỹ Khê.',
     '88 Võ Nguyên Giáp, Sơn Trà', '+84236000007', 'mykhe@demo.example.com',
     16.061000, 108.247000, 4::smallint, 'ACTIVE'),
    ('de0000b0-0000-4000-8000-000000000008'::uuid, 'de0000a0-0000-4000-8000-000000000001'::uuid,
     'da-nang', 'Demo Đà Nẵng Sơn Trà', 'demo-da-nang-son-tra',
     'Chưa có giá đêm 2026-12-31 (không có giá cơ bản).',
     '15 Hoàng Sa, Sơn Trà', '+84236000008', NULL,
     16.101000, 108.278000, 3::smallint, 'ACTIVE'),
    ('de0000b0-0000-4000-8000-000000000009'::uuid, 'de0000a0-0000-4000-8000-000000000002'::uuid,
     'hue', 'Demo Huế Hoàng Thành', 'demo-hue-hoang-thanh', 'Khách sạn demo gần Đại Nội.',
     '3 Đinh Tiên Hoàng, Huế', '+84234000009', 'hoangthanh@demo.example.com',
     16.469000, 107.578000, 3::smallint, 'ACTIVE'),
    ('de0000b0-0000-4000-8000-000000000010'::uuid, 'de0000a0-0000-4000-8000-000000000002'::uuid,
     'hue', 'Demo Huế Sông Hương', 'demo-hue-song-huong', 'Đang tạm ngừng bán (SUSPENDED).',
     '10 Lê Lợi, Huế', '+84234000010', NULL,
     16.463000, 107.590000, 4::smallint, 'SUSPENDED')
) AS h(id, owner_id, city_slug, name, slug, description, address, phone, email, lat, lng, stars,
       status)
JOIN cities c ON c.slug = h.city_slug
ON CONFLICT (id) DO NOTHING;

-- 3. Loại phòng; giá VND kiểu BIGINT (không float). rooms = số phòng vật lý sẽ tạo ở bước 4.
CREATE TEMPORARY TABLE seed_demo_room_types (
    id UUID PRIMARY KEY,
    hotel_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    max_adults SMALLINT NOT NULL,
    max_children SMALLINT NOT NULL,
    max_occupancy SMALLINT NOT NULL,
    base_price BIGINT NOT NULL,
    rooms INTEGER NOT NULL,
    type_no INTEGER NOT NULL
) ON COMMIT DROP;

INSERT INTO seed_demo_room_types VALUES
    -- Hà Nội Phố Cổ
    ('de0000c0-0000-4000-8000-000000000101', 'de0000b0-0000-4000-8000-000000000001', 'Standard', 2, 0, 2, 900000, 4, 1),
    ('de0000c0-0000-4000-8000-000000000102', 'de0000b0-0000-4000-8000-000000000001', 'Deluxe', 2, 1, 3, 1400000, 3, 2),
    ('de0000c0-0000-4000-8000-000000000103', 'de0000b0-0000-4000-8000-000000000001', 'Family Suite', 4, 2, 5, 2600000, 2, 3),
    -- Hà Nội Hồ Tây: có phòng đơn sức chứa 1
    ('de0000c0-0000-4000-8000-000000000201', 'de0000b0-0000-4000-8000-000000000002', 'Phòng đơn', 1, 0, 1, 550000, 3, 1),
    ('de0000c0-0000-4000-8000-000000000202', 'de0000b0-0000-4000-8000-000000000002', 'Superior', 2, 0, 2, 850000, 3, 2),
    -- Hà Nội Long Biên (DRAFT đủ HT-03)
    ('de0000c0-0000-4000-8000-000000000301', 'de0000b0-0000-4000-8000-000000000003', 'Standard', 2, 0, 2, 700000, 2, 1),
    ('de0000c0-0000-4000-8000-000000000302', 'de0000b0-0000-4000-8000-000000000003', 'Deluxe', 2, 1, 3, 1100000, 2, 2),
    -- Sài Gòn Riverside
    ('de0000c0-0000-4000-8000-000000000401', 'de0000b0-0000-4000-8000-000000000004', 'Deluxe City View', 2, 0, 2, 2200000, 5, 1),
    ('de0000c0-0000-4000-8000-000000000402', 'de0000b0-0000-4000-8000-000000000004', 'Deluxe River View', 2, 1, 3, 2800000, 4, 2),
    ('de0000c0-0000-4000-8000-000000000403', 'de0000b0-0000-4000-8000-000000000004', 'Executive Suite', 3, 1, 4, 4500000, 2, 3),
    ('de0000c0-0000-4000-8000-000000000404', 'de0000b0-0000-4000-8000-000000000004', 'Presidential Suite', 4, 2, 6, 9000000, 1, 4),
    -- Sài Gòn Bến Thành (thiếu tồn đêm 2026-12-24)
    ('de0000c0-0000-4000-8000-000000000501', 'de0000b0-0000-4000-8000-000000000005', 'Standard', 2, 0, 2, 1200000, 3, 1),
    ('de0000c0-0000-4000-8000-000000000502', 'de0000b0-0000-4000-8000-000000000005', 'Deluxe', 2, 1, 3, 1700000, 2, 2),
    -- Sài Gòn Thảo Điền (DRAFT thiếu liên hệ; loại phòng và phòng đều hợp lệ)
    ('de0000c0-0000-4000-8000-000000000601', 'de0000b0-0000-4000-8000-000000000006', 'Studio', 2, 0, 2, 1300000, 2, 1),
    ('de0000c0-0000-4000-8000-000000000602', 'de0000b0-0000-4000-8000-000000000006', 'Apartment', 4, 1, 5, 2500000, 2, 2),
    -- Đà Nẵng Mỹ Khê
    ('de0000c0-0000-4000-8000-000000000701', 'de0000b0-0000-4000-8000-000000000007', 'Superior', 2, 0, 2, 1000000, 4, 1),
    ('de0000c0-0000-4000-8000-000000000702', 'de0000b0-0000-4000-8000-000000000007', 'Deluxe Ocean View', 2, 1, 3, 1600000, 3, 2),
    ('de0000c0-0000-4000-8000-000000000703', 'de0000b0-0000-4000-8000-000000000007', 'Family', 4, 2, 6, 2400000, 2, 3),
    -- Đà Nẵng Sơn Trà (thiếu giá đêm 2026-12-31: base_price 0)
    ('de0000c0-0000-4000-8000-000000000801', 'de0000b0-0000-4000-8000-000000000008', 'Standard', 2, 0, 2, 0, 3, 1),
    ('de0000c0-0000-4000-8000-000000000802', 'de0000b0-0000-4000-8000-000000000008', 'Bungalow', 3, 1, 4, 0, 2, 2),
    -- Huế Hoàng Thành
    ('de0000c0-0000-4000-8000-000000000901', 'de0000b0-0000-4000-8000-000000000009', 'Standard', 2, 0, 2, 650000, 3, 1),
    ('de0000c0-0000-4000-8000-000000000902', 'de0000b0-0000-4000-8000-000000000009', 'Deluxe Garden', 2, 1, 3, 950000, 2, 2),
    -- Huế Sông Hương (SUSPENDED)
    ('de0000c0-0000-4000-8000-000000001001', 'de0000b0-0000-4000-8000-000000000010', 'Superior', 2, 0, 2, 800000, 2, 1),
    ('de0000c0-0000-4000-8000-000000001002', 'de0000b0-0000-4000-8000-000000000010', 'River Suite', 3, 1, 4, 1500000, 2, 2);

INSERT INTO room_types (id, hotel_id, name, description, max_adults, max_children, max_occupancy,
                        base_price, is_active, created_at, updated_at, version)
SELECT id, hotel_id, name, NULL, max_adults, max_children, max_occupancy, base_price, true,
       '2026-11-01T00:00:00Z'::timestamptz, '2026-11-01T00:00:00Z'::timestamptz, 0
FROM seed_demo_room_types
ON CONFLICT (id) DO NOTHING;

-- 4. Phòng vật lý đang phục vụ từ 2026-01-01. UUID cố định suy ra từ loại phòng + số thứ tự.
INSERT INTO rooms (id, hotel_id, room_type_id, room_number, floor, readiness, readiness_updated_at,
                   operational_from, created_at, updated_at, version)
SELECT md5('demo-room:' || rt.id || ':' || n)::uuid, rt.hotel_id, rt.id,
       (rt.type_no * 100 + n)::text, rt.type_no::text, 'READY'::room_readiness__t, '2026-11-01T00:00:00Z'::timestamptz,
       '2026-01-01T00:00:00Z'::timestamptz, '2026-11-01T00:00:00Z'::timestamptz, '2026-11-01T00:00:00Z'::timestamptz, 0
FROM seed_demo_room_types rt
CROSS JOIN LATERAL generate_series(1, rt.rooms) AS n
ON CONFLICT (id) DO NOTHING;

-- 5. Giá theo ngày 2026-11-01 → 2027-01-31: thứ Sáu/thứ Bảy +20% (vẫn là số nguyên VND).
-- Sơn Trà bỏ trống đêm 2026-12-31 và base_price = 0 nên đêm đó không có giá hợp lệ (PR-02).
INSERT INTO room_rates (room_type_id, rate_date, amount, currency, created_at, updated_at)
SELECT rt.id, d::date,
       CASE
           WHEN rt.base_price > 0 AND extract(isodow FROM d) IN (5, 6) THEN rt.base_price * 12 / 10
           WHEN rt.base_price > 0 THEN rt.base_price
           WHEN rt.type_no = 1 THEN 750000
           ELSE 1300000
       END,
       'VND', '2026-11-01T00:00:00Z'::timestamptz, '2026-11-01T00:00:00Z'::timestamptz
FROM seed_demo_room_types rt
CROSS JOIN generate_series('2026-11-01'::date, '2027-01-31'::date, interval '1 day') AS d
WHERE NOT (rt.hotel_id = 'de0000b0-0000-4000-8000-000000000008' AND d::date = '2026-12-31')
ON CONFLICT (room_type_id, rate_date) DO NOTHING;

-- 6. Chặn hết phòng Bến Thành đêm 2026-12-24 (start_date = end_date: một đêm).
INSERT INTO room_blocks (id, hotel_id, room_type_id, room_id, start_date, end_date, reason, status,
                         created_by_user_id, created_at)
SELECT md5('demo-block:' || r.id)::uuid, r.hotel_id, r.room_type_id, r.id, '2026-12-24'::date,
       '2026-12-24'::date, 'Bảo trì demo', 'ACTIVE'::room_block_status__t, 'de0000a0-0000-4000-8000-000000000002'::uuid,
       '2026-11-01T00:00:00Z'::timestamptz
FROM rooms r
WHERE r.hotel_id = 'de0000b0-0000-4000-8000-000000000005'::uuid
ON CONFLICT (id) DO NOTHING;

-- 7. room_inventory (IV01): hiện chưa có code đọc bảng này (tồn tính từ rooms/room_blocks/
-- inventory_reservations), seed để dữ liệu nhất quán; đêm bị chặn có total_inventory 0.
INSERT INTO room_inventory (room_type_id, inventory_date, total_inventory, reserved_inventory,
                            created_at, updated_at)
SELECT rt.id, d::date,
       CASE
           WHEN rt.hotel_id = 'de0000b0-0000-4000-8000-000000000005' AND d::date = '2026-12-24'
               THEN 0
           ELSE rt.rooms
       END,
       0, '2026-11-01T00:00:00Z'::timestamptz, '2026-11-01T00:00:00Z'::timestamptz
FROM seed_demo_room_types rt
CROSS JOIN generate_series('2026-11-01'::date, '2027-01-31'::date, interval '1 day') AS d
ON CONFLICT (room_type_id, inventory_date) DO NOTHING;

-- 8. Tiện ích theo mã đã seed ở V20261104191000000.
INSERT INTO hotel_amenities (hotel_id, amenity_id, amenity_category)
SELECT h.hotel_id::uuid, a.id, 'HOTEL'::amenity_category__t
FROM (VALUES
    ('de0000b0-0000-4000-8000-000000000001', 'RESTAURANT'),
    ('de0000b0-0000-4000-8000-000000000001', 'FITNESS_CENTER'),
    ('de0000b0-0000-4000-8000-000000000002', 'FREE_PARKING'),
    ('de0000b0-0000-4000-8000-000000000003', 'FREE_PARKING'),
    ('de0000b0-0000-4000-8000-000000000004', 'SWIMMING_POOL'),
    ('de0000b0-0000-4000-8000-000000000004', 'RESTAURANT'),
    ('de0000b0-0000-4000-8000-000000000004', 'FITNESS_CENTER'),
    ('de0000b0-0000-4000-8000-000000000004', 'AIRPORT_SHUTTLE'),
    ('de0000b0-0000-4000-8000-000000000005', 'RESTAURANT'),
    ('de0000b0-0000-4000-8000-000000000006', 'SWIMMING_POOL'),
    ('de0000b0-0000-4000-8000-000000000007', 'SWIMMING_POOL'),
    ('de0000b0-0000-4000-8000-000000000007', 'FREE_PARKING'),
    ('de0000b0-0000-4000-8000-000000000008', 'FREE_PARKING'),
    ('de0000b0-0000-4000-8000-000000000009', 'RESTAURANT'),
    ('de0000b0-0000-4000-8000-000000000010', 'SWIMMING_POOL')
) AS h(hotel_id, code)
JOIN amenities a ON a.code = h.code AND a.category = 'HOTEL'
ON CONFLICT (hotel_id, amenity_id) DO NOTHING;

INSERT INTO room_type_amenities (room_type_id, amenity_id, amenity_category)
SELECT rt.id, a.id, 'ROOM'::amenity_category__t
FROM seed_demo_room_types rt
JOIN amenities a
  ON a.category = 'ROOM'
 AND (a.code IN ('FREE_WIFI', 'AIR_CONDITIONING', 'PRIVATE_BATHROOM')
      OR (rt.type_no >= 2 AND a.code IN ('TELEVISION', 'MINIBAR'))
      OR (rt.type_no >= 3 AND a.code = 'BALCONY'))
ON CONFLICT (room_type_id, amenity_id) DO NOTHING;
