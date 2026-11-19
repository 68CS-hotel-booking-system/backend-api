INSERT INTO cities (id, country_code, name, slug)
VALUES
    ('81560b64-8216-46c6-8f1b-f14214fc0c90', 'VN', 'Hà Nội', 'ha-noi'),
    ('0c0fcb3f-45a8-4f37-a33c-dc39b5d2c79d', 'VN', 'Thành phố Hồ Chí Minh', 'ho-chi-minh'),
    ('822cafba-73f6-4ec0-bae7-71804535cae3', 'VN', 'Đà Nẵng', 'da-nang'),
    ('286af3a8-b56c-4b79-8c9a-445d5bc4cbf5', 'VN', 'Hải Phòng', 'hai-phong'),
    ('4a6fd426-a8a4-49bf-99d0-015c6053d4e2', 'VN', 'Cần Thơ', 'can-tho'),
    ('d4266df8-21c1-4f42-849a-30bebfc561b9', 'VN', 'Huế', 'hue')
ON CONFLICT (slug) DO NOTHING;
