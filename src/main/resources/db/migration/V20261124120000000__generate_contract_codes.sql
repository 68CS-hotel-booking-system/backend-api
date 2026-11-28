-- Giữ mã cũ và bắt đầu sau số lớn nhất đã dùng, kể cả mã nhập tay cùng định dạng.
CREATE SEQUENCE hotel_contract_code_seq;

SELECT setval(
    'hotel_contract_code_seq',
    COALESCE(MAX(substring(contract_code FROM '^HD-[0-9]{4}-([0-9]{1,18})$')::bigint), 0) + 1,
    false
) FROM hotel_contracts;
