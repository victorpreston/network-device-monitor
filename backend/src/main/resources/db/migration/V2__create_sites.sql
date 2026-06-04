CREATE TABLE sites (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL UNIQUE,
    address    TEXT,
    created_at TIMESTAMPTZ  NOT NULL
);
