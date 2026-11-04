INSERT INTO amenities (id, code, name, category)
VALUES
    ('59fe1090-6e67-4d93-b140-232c5e885c27', 'FREE_PARKING', 'Bãi đỗ xe miễn phí', 'HOTEL'),
    ('c290e20c-7bd7-440b-9b7d-38b0f9c359c3', 'SWIMMING_POOL', 'Hồ bơi', 'HOTEL'),
    ('c6bdf52b-8c50-4e74-a65e-f1ce128683c7', 'RESTAURANT', 'Nhà hàng', 'HOTEL'),
    ('b1d4b48f-ab41-4730-b345-0927e655f00c', 'FITNESS_CENTER', 'Phòng gym', 'HOTEL'),
    ('33d3639b-2f88-4b75-82d3-1f9260643a26', 'RECEPTION_24H', 'Lễ tân 24/7', 'HOTEL'),
    ('aa1438f9-ec5f-4c1d-9130-d4a0be05d86f', 'AIRPORT_SHUTTLE', 'Đưa đón sân bay', 'HOTEL'),
    ('4e10ab03-f280-4717-a8ef-0155f186c41a', 'FREE_WIFI', 'Wi-Fi miễn phí', 'ROOM'),
    ('f46f1604-0e9b-45fc-9f27-d2cd3e48a626', 'AIR_CONDITIONING', 'Điều hòa', 'ROOM'),
    ('4ba9504a-fe4f-4a89-a791-0f25d0f41935', 'TELEVISION', 'TV màn hình phẳng', 'ROOM'),
    ('49956e39-e905-480f-be80-307fd367dcf8', 'MINIBAR', 'Minibar', 'ROOM'),
    ('5a6484cd-f8c1-4662-b014-a0a42162198f', 'PRIVATE_BATHROOM', 'Phòng tắm riêng', 'ROOM'),
    ('76fac790-5753-4608-b2cd-a3fc4faaa281', 'BALCONY', 'Ban công', 'ROOM')
ON CONFLICT (code) DO NOTHING;
