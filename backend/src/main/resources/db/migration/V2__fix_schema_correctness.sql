-- =============================================================================
-- V2__fix_schema_correctness.sql
-- Fixes identified during Milestone 2 audit:
--
-- 1. maintenance_records.performed_by: Change from VARCHAR to UUID FK -> users(id)
--    Architecture spec (architecture.md) defines performed_by as UUID FK to users.
--    The V1 migration incorrectly used VARCHAR(255).
--
-- 2. telemetry sensor columns: Add NOT NULL constraints.
--    Architecture spec requires all four sensor readings to be NOT NULL.
--    The V1 migration incorrectly omitted NOT NULL on vibration, temperature,
--    current, and rpm. Safe to add now since the table is empty.
--
-- 3. prediction_explanations.direction: Widen column and correct vocabulary.
--    Architecture defines direction as INCREASES_RISK | DECREASES_RISK (14 chars).
--    V1 had VARCHAR(10) with POSITIVE | NEGATIVE (wrong, and too narrow).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- FIX 1: maintenance_records.performed_by VARCHAR -> UUID FK -> users(id)
-- The architecture (architecture.md) specifies performed_by as a FK to users,
-- not a free-text name. The entity now uses a @ManyToOne User reference.
-- Rename the old column to a temporary name, add the new UUID FK column.
-- -----------------------------------------------------------------------------
ALTER TABLE maintenance_records
    DROP COLUMN performed_by;

ALTER TABLE maintenance_records
    ADD COLUMN performed_by UUID NOT NULL;

ALTER TABLE maintenance_records
    ADD CONSTRAINT fk_mr_performed_by
        FOREIGN KEY (performed_by) REFERENCES users (id);

CREATE INDEX idx_mr_performed_by ON maintenance_records (performed_by);

-- -----------------------------------------------------------------------------
-- FIX 2: telemetry sensor readings must be NOT NULL
-- The architecture spec explicitly marks vibration, temperature, current, rpm
-- as NOT NULL. The V1 migration omitted these constraints. The table is empty
-- at this point in development so this alteration is safe.
-- -----------------------------------------------------------------------------
ALTER TABLE telemetry
    ALTER COLUMN vibration   SET NOT NULL,
    ALTER COLUMN temperature SET NOT NULL,
    ALTER COLUMN current     SET NOT NULL,
    ALTER COLUMN rpm         SET NOT NULL;

-- -----------------------------------------------------------------------------
-- FIX 3: prediction_explanations.direction — correct vocabulary and column width
-- Architecture defines: INCREASES_RISK (13 chars) | DECREASES_RISK (14 chars)
-- V1 had VARCHAR(10) with POSITIVE | NEGATIVE — wrong vocabulary and too narrow.
-- -----------------------------------------------------------------------------
ALTER TABLE prediction_explanations
    ALTER COLUMN direction TYPE VARCHAR(16);
