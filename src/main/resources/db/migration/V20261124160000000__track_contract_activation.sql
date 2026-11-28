-- Không suy diễn hợp đồng TERMINATED cũ là đã ký: có thể bị chấm dứt ngay từ DRAFT.
ALTER TABLE hotel_contracts ADD COLUMN has_been_activated boolean NOT NULL DEFAULT false;
UPDATE hotel_contracts SET has_been_activated = true WHERE status IN ('ACTIVE', 'EXPIRED');

CREATE FUNCTION track_hotel_contract_activation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        NEW.has_been_activated := NEW.has_been_activated OR OLD.has_been_activated
            OR OLD.status IN ('ACTIVE', 'EXPIRED');
    END IF;
    NEW.has_been_activated := NEW.has_been_activated OR NEW.status IN ('ACTIVE', 'EXPIRED');
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_hotel_contract_activation BEFORE INSERT OR UPDATE ON hotel_contracts
FOR EACH ROW EXECUTE FUNCTION track_hotel_contract_activation();
CREATE INDEX ix_hotel_contracts_activated_owner ON hotel_contracts(owner_user_id)
WHERE has_been_activated;
