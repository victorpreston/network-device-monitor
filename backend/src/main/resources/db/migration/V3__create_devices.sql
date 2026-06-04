CREATE TABLE devices (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(255) NOT NULL,
    device_type_id UUID         NOT NULL REFERENCES device_types(id),
    hostname       VARCHAR(255) NOT NULL,
    site_id        UUID         NOT NULL REFERENCES sites(id),
    registered_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_devices_site ON devices(site_id);
CREATE INDEX idx_devices_type ON devices(device_type_id);
