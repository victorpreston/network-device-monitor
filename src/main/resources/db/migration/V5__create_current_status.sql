CREATE TABLE current_status (
    device_id   UUID        PRIMARY KEY REFERENCES devices(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL CHECK (status IN ('ONLINE', 'DEGRADED', 'OFFLINE')),
    message     TEXT,
    reported_at TIMESTAMPTZ NOT NULL
);
