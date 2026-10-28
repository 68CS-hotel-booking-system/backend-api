CREATE TABLE refresh_tokens
(
  id         UUID PRIMARY KEY,
  user_id    UUID        NOT NULL REFERENCES users (id),
  family_id  UUID        NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked    BOOLEAN     NOT NULL DEFAULT FALSE,

  CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id
  ON refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_family_id
  ON refresh_tokens (family_id);
