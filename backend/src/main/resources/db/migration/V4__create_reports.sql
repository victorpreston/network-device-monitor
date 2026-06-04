CREATE TABLE reports (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id   UUID        NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL CHECK (status IN ('ONLINE', 'DEGRADED', 'OFFLINE')),
    message     TEXT,
    reported_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_reports_device_time ON reports(device_id, reported_at DESC);
