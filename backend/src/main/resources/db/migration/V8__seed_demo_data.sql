DO $$
DECLARE
  site_london     UUID := gen_random_uuid();
  site_manchester UUID := gen_random_uuid();
  site_edinburgh  UUID := gen_random_uuid();

  dt_router   UUID;
  dt_switch   UUID;
  dt_firewall UUID;
  dt_ap       UUID;
  dt_cpe      UUID;
  dt_ont      UUID;

  d1  UUID := gen_random_uuid();
  d2  UUID := gen_random_uuid();
  d3  UUID := gen_random_uuid();
  d4  UUID := gen_random_uuid();
  d5  UUID := gen_random_uuid();
  d6  UUID := gen_random_uuid();
  d7  UUID := gen_random_uuid();
  d8  UUID := gen_random_uuid();
  d9  UUID := gen_random_uuid();
  d10 UUID := gen_random_uuid();
BEGIN
  SELECT id INTO dt_router   FROM device_types WHERE name = 'Router';
  SELECT id INTO dt_switch   FROM device_types WHERE name = 'Switch';
  SELECT id INTO dt_firewall FROM device_types WHERE name = 'Firewall';
  SELECT id INTO dt_ap       FROM device_types WHERE name = 'Access Point';
  SELECT id INTO dt_cpe      FROM device_types WHERE name = 'CPE';
  SELECT id INTO dt_ont      FROM device_types WHERE name = 'ONT';

  INSERT INTO sites (id, name, address, latitude, longitude, created_at) VALUES
    (site_london,     'London HQ',        '1 Canada Square, London E14 5AB',      51.504590, -0.023560, NOW() - INTERVAL '60 days'),
    (site_manchester, 'Manchester NOC',   '1 Spinningfields, Manchester M3 3AP',   53.480095, -2.243057, NOW() - INTERVAL '45 days'),
    (site_edinburgh,  'Edinburgh Branch', '1 Festival Square, Edinburgh EH3 9SR',  55.945741, -3.192680, NOW() - INTERVAL '30 days');

  INSERT INTO devices (id, name, device_type_id, hostname, site_id, registered_at) VALUES
    (d1,  'core-router-01',  dt_router,   '10.0.1.1',   site_london,      NOW() - INTERVAL '55 days'),
    (d2,  'sw-dist-01',      dt_switch,   '10.0.1.2',   site_london,      NOW() - INTERVAL '54 days'),
    (d3,  'fw-perimeter-01', dt_firewall, '10.0.1.254', site_london,      NOW() - INTERVAL '53 days'),
    (d4,  'core-router-mnc', dt_router,   '10.1.1.1',   site_manchester,  NOW() - INTERVAL '40 days'),
    (d5,  'ap-floor2-mnc',   dt_ap,       '10.1.2.1',   site_manchester,  NOW() - INTERVAL '38 days'),
    (d6,  'sw-access-mnc',   dt_switch,   '10.1.3.1',   site_manchester,  NOW() - INTERVAL '35 days'),
    (d7,  'ont-wan-edn',     dt_ont,      '10.2.0.1',   site_edinburgh,   NOW() - INTERVAL '25 days'),
    (d8,  'cpe-edn-01',      dt_cpe,      '10.2.1.1',   site_edinburgh,   NOW() - INTERVAL '20 days'),
    (d9,  'fw-edn-01',       dt_firewall, '10.2.1.254', site_edinburgh,   NOW() - INTERVAL '18 days'),
    (d10, 'ap-lobby-edn',    dt_ap,       '10.2.2.1',   site_edinburgh,   NOW() - INTERVAL '15 days');

  -- Historical reports (for the timeline in the detail drawer)
  INSERT INTO reports (device_id, status, message, reported_at) VALUES
    (d1,  'ONLINE',   'All interfaces up. CPU 8%.',          NOW() - INTERVAL '45 minutes'),
    (d1,  'ONLINE',   'All interfaces up. CPU 11%.',         NOW() - INTERVAL '30 minutes'),
    (d1,  'ONLINE',   'All interfaces up. CPU 7%.',          NOW() - INTERVAL '8 minutes'),

    (d2,  'ONLINE',   'Uptime 54d. All ports nominal.',       NOW() - INTERVAL '20 minutes'),
    (d2,  'ONLINE',   'Uptime 54d. Ports nominal.',           NOW() - INTERVAL '5 minutes'),

    (d3,  'ONLINE',   'Policy check passed. 0 threats.',     NOW() - INTERVAL '9 minutes'),

    (d4,  'ONLINE',   'BGP full table. CPU 22%.',             NOW() - INTERVAL '3 hours'),
    (d4,  'DEGRADED', 'High CPU 91%. BGP reconverging.',     NOW() - INTERVAL '12 minutes'),

    (d5,  'ONLINE',   'All clients associated. RSSI -58.',   NOW() - INTERVAL '2 hours'),
    (d5,  'DEGRADED', 'Signal degraded. 2.4GHz band only.', NOW() - INTERVAL '7 minutes'),

    (d6,  'ONLINE',   'All 24 ports active.',                 NOW() - INTERVAL '6 hours'),
    (d6,  'OFFLINE',  'Uplink port 24 down. No connectivity.', NOW() - INTERVAL '90 minutes'),
    (d6,  'OFFLINE',  'Still unreachable. Escalated.',        NOW() - INTERVAL '4 minutes'),

    (d7,  'ONLINE',   'WAN synced at 1 Gbps.',                NOW() - INTERVAL '2 days'),
    (d7,  'OFFLINE',  'WAN signal lost. Physical alarm.',    NOW() - INTERVAL '3 minutes'),

    (d8,  'ONLINE',   'Customer premises normal.',            NOW() - INTERVAL '4 hours'),
    (d8,  'ONLINE',   'Normal. Latency 12ms.',                NOW() - INTERVAL '2 hours'),

    (d9,  'ONLINE',   'Ruleset synced. 0 blocks.',            NOW() - INTERVAL '8 hours'),
    (d9,  'DEGRADED', 'Config sync failed. Retrying.',       NOW() - INTERVAL '3 hours');
    -- d10 (ap-lobby-edn): no reports ever — will appear STALE

  -- Current status read model
  -- ONLINE: reported within last 15 min → will show as ONLINE at startup
  INSERT INTO current_status (device_id, status, message, reported_at) VALUES
    (d1,  'ONLINE',   'All interfaces up. CPU 7%.',          NOW() - INTERVAL '8 minutes'),
    (d2,  'ONLINE',   'Uptime 54d. Ports nominal.',           NOW() - INTERVAL '5 minutes'),
    (d3,  'ONLINE',   'Policy check passed. 0 threats.',     NOW() - INTERVAL '9 minutes'),
    (d4,  'DEGRADED', 'High CPU 91%. BGP reconverging.',     NOW() - INTERVAL '12 minutes'),
    (d5,  'DEGRADED', 'Signal degraded. 2.4GHz band only.', NOW() - INTERVAL '7 minutes'),
    (d6,  'OFFLINE',  'Still unreachable. Escalated.',        NOW() - INTERVAL '4 minutes'),
    (d7,  'OFFLINE',  'WAN signal lost. Physical alarm.',    NOW() - INTERVAL '3 minutes'),
    -- STALE: reported_at is hours old → stale threshold exceeded
    (d8,  'ONLINE',   'Normal. Latency 12ms.',                NOW() - INTERVAL '2 hours'),
    (d9,  'DEGRADED', 'Config sync failed. Retrying.',       NOW() - INTERVAL '3 hours');
    -- d10: no current_status row → STALE (never reported)
END $$;
