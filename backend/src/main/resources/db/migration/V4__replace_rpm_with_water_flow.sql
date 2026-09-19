-- =============================================================================
-- Migration V4: Replace RPM with WATER_FLOW sensor
-- =============================================================================

-- 1. Rename column in telemetry table from rpm to water_flow
ALTER TABLE telemetry RENAME COLUMN rpm TO water_flow;

-- 2. Update existing sensor records in sensors table
UPDATE sensors
SET sensor_code = 'M001-WF',
    sensor_type = 'WATER_FLOW',
    unit = 'L/min'
WHERE sensor_code = 'M001-RPM';

UPDATE sensors
SET sensor_type = 'WATER_FLOW',
    unit = 'L/min'
WHERE sensor_type = 'RPM';
