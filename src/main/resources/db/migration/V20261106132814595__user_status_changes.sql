CREATE TABLE user_status_changes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    actor_id UUID NOT NULL REFERENCES users(id),
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    changed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_user_status_changes_previous_status
        CHECK (previous_status IN ('active', 'inactive', 'pending', 'banned')),
    CONSTRAINT ck_user_status_changes_new_status
        CHECK (new_status IN ('active', 'inactive', 'pending', 'banned')),
    CONSTRAINT ck_user_status_changes_different_status
        CHECK (previous_status <> new_status)
);

CREATE INDEX idx_user_status_changes_user_changed_at
    ON user_status_changes(user_id, changed_at DESC);
