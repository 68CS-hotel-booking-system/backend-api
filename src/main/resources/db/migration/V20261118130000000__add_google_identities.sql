CREATE TABLE google_identities (
    subject VARCHAR(255) PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users (id)
);
