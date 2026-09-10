-- =============================================================================
-- V3__seed_demo_machine.sql
-- Creates the default demo machine and its sensors for a fresh installation.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- DEMO MACHINE
-- -----------------------------------------------------------------------------

INSERT INTO machines (
    machine_code,
    name,
    machine_type,
    location,
    status
)
VALUES (
    'M001',
    'Demo Motor',
    'MOTOR',
    'Demo Plant',
    'ACTIVE'
);

-- -----------------------------------------------------------------------------
-- DEMO SENSORS
-- -----------------------------------------------------------------------------

INSERT INTO sensors (
    machine_id,
    sensor_code,
    sensor_type,
    unit,
    status
)
SELECT
    id,
    'M001-VIB',
    'VIBRATION',
    'mm/s',
    'ACTIVE'
FROM machines
WHERE machine_code = 'M001';

INSERT INTO sensors (
    machine_id,
    sensor_code,
    sensor_type,
    unit,
    status
)
SELECT
    id,
    'M001-TEMP',
    'TEMPERATURE',
    '°C',
    'ACTIVE'
FROM machines
WHERE machine_code = 'M001';

INSERT INTO sensors (
    machine_id,
    sensor_code,
    sensor_type,
    unit,
    status
)
SELECT
    id,
    'M001-CUR',
    'CURRENT',
    'A',
    'ACTIVE'
FROM machines
WHERE machine_code = 'M001';

INSERT INTO sensors (
    machine_id,
    sensor_code,
    sensor_type,
    unit,
    status
)
SELECT
    id,
    'M001-RPM',
    'RPM',
    'RPM',
    'ACTIVE'
FROM machines
WHERE machine_code = 'M001';