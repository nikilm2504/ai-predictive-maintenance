-- =============================================================================
-- V1__create_initial_schema.sql
-- Initial schema for AI/ML IoT-Based Predictive Maintenance Platform
-- Managed by Flyway. DO NOT modify Hibernate ddl-auto to create/update.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- USERS
-- Stores operators, maintenance engineers, and admins.
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL,  -- ADMIN | OPERATOR | MAINTENANCE_ENGINEER
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- -----------------------------------------------------------------------------
-- MACHINES
-- Industrial machines being monitored.
-- -----------------------------------------------------------------------------
CREATE TABLE machines (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    machine_code VARCHAR(100) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    machine_type VARCHAR(100) NOT NULL,
    location     VARCHAR(255),
    status       VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | INACTIVE | MAINTENANCE | DECOMMISSIONED
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_machines PRIMARY KEY (id),
    CONSTRAINT uq_machines_machine_code UNIQUE (machine_code)
);

-- -----------------------------------------------------------------------------
-- SENSORS
-- Physical or simulated sensors attached to machines.
-- -----------------------------------------------------------------------------
CREATE TABLE sensors (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    machine_id   UUID         NOT NULL,
    sensor_code  VARCHAR(100) NOT NULL,
    sensor_type  VARCHAR(50)  NOT NULL,  -- VIBRATION | TEMPERATURE | CURRENT | RPM
    unit         VARCHAR(50)  NOT NULL,
    status       VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    installed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_sensors PRIMARY KEY (id),
    CONSTRAINT uq_sensors_sensor_code UNIQUE (sensor_code),
    CONSTRAINT fk_sensors_machine FOREIGN KEY (machine_id) REFERENCES machines (id)
);

CREATE INDEX idx_sensors_machine_id ON sensors (machine_id);

-- -----------------------------------------------------------------------------
-- TELEMETRY
-- High-volume raw sensor readings. No bidirectional JPA mapping from Machine.
-- See ADR-003: Machine entity must NOT contain @OneToMany List<Telemetry>.
-- -----------------------------------------------------------------------------
CREATE TABLE telemetry (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    machine_id  UUID         NOT NULL,
    ts          TIMESTAMPTZ  NOT NULL,  -- named 'ts' to avoid PostgreSQL reserved word 'timestamp'
    vibration   DOUBLE PRECISION,
    temperature DOUBLE PRECISION,
    current     DOUBLE PRECISION,
    rpm         DOUBLE PRECISION,

    CONSTRAINT pk_telemetry PRIMARY KEY (id),
    CONSTRAINT fk_telemetry_machine FOREIGN KEY (machine_id) REFERENCES machines (id)
);

-- Composite index for the two primary query patterns per ADR-003:
--   1. SELECT * FROM telemetry WHERE machine_id = ? ORDER BY ts DESC LIMIT n
--   2. SELECT * FROM telemetry WHERE machine_id = ? AND ts BETWEEN ? AND ?
CREATE INDEX idx_telemetry_machine_ts ON telemetry (machine_id, ts DESC);

-- -----------------------------------------------------------------------------
-- PREDICTIONS
-- ML model inference results for a machine at a given point in time.
-- -----------------------------------------------------------------------------
CREATE TABLE predictions (
    id                  UUID             NOT NULL DEFAULT gen_random_uuid(),
    machine_id          UUID             NOT NULL,
    ts                  TIMESTAMPTZ      NOT NULL,
    fault_type          VARCHAR(100),
    failure_probability DOUBLE PRECISION NOT NULL,
    confidence          DOUBLE PRECISION NOT NULL,
    health_score        DOUBLE PRECISION NOT NULL,
    model_version       VARCHAR(50)      NOT NULL,

    CONSTRAINT pk_predictions PRIMARY KEY (id),
    CONSTRAINT fk_predictions_machine FOREIGN KEY (machine_id) REFERENCES machines (id)
);

CREATE INDEX idx_predictions_machine_ts ON predictions (machine_id, ts DESC);

-- -----------------------------------------------------------------------------
-- PREDICTION_EXPLANATIONS
-- SHAP-based feature contribution records for a prediction.
-- -----------------------------------------------------------------------------
CREATE TABLE prediction_explanations (
    id             UUID             NOT NULL DEFAULT gen_random_uuid(),
    prediction_id  UUID             NOT NULL,
    feature_name   VARCHAR(255)     NOT NULL,
    contribution   DOUBLE PRECISION NOT NULL,
    direction      VARCHAR(10)      NOT NULL,  -- POSITIVE | NEGATIVE

    CONSTRAINT pk_prediction_explanations PRIMARY KEY (id),
    CONSTRAINT fk_pred_exp_prediction FOREIGN KEY (prediction_id) REFERENCES predictions (id)
);

CREATE INDEX idx_pred_exp_prediction_id ON prediction_explanations (prediction_id);

-- -----------------------------------------------------------------------------
-- ALERTS
-- Alerts generated from ML predictions exceeding risk thresholds.
-- -----------------------------------------------------------------------------
CREATE TABLE alerts (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    machine_id      UUID        NOT NULL,
    prediction_id   UUID,                -- nullable: alert can exist without a prediction
    severity        VARCHAR(20) NOT NULL,  -- LOW | MEDIUM | HIGH | CRITICAL
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',  -- OPEN | ACKNOWLEDGED | RESOLVED
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged_at TIMESTAMPTZ,
    resolved_at     TIMESTAMPTZ,

    CONSTRAINT pk_alerts PRIMARY KEY (id),
    CONSTRAINT fk_alerts_machine FOREIGN KEY (machine_id) REFERENCES machines (id),
    CONSTRAINT fk_alerts_prediction FOREIGN KEY (prediction_id) REFERENCES predictions (id)
);

CREATE INDEX idx_alerts_machine_id ON alerts (machine_id);
CREATE INDEX idx_alerts_status ON alerts (status);

-- -----------------------------------------------------------------------------
-- MAINTENANCE_WORK_ORDERS
-- Maintenance tasks created in response to alerts.
-- -----------------------------------------------------------------------------
CREATE TABLE maintenance_work_orders (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    machine_id   UUID         NOT NULL,
    alert_id     UUID,                    -- nullable: work order may not originate from an alert
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    priority     VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',  -- LOW | MEDIUM | HIGH | CRITICAL
    status       VARCHAR(30)  NOT NULL DEFAULT 'OPEN',    -- OPEN | ASSIGNED | IN_PROGRESS | COMPLETED | CANCELLED
    assigned_to  UUID,                    -- nullable: may not yet be assigned
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,

    CONSTRAINT pk_maintenance_work_orders PRIMARY KEY (id),
    CONSTRAINT fk_mwo_machine FOREIGN KEY (machine_id) REFERENCES machines (id),
    CONSTRAINT fk_mwo_alert FOREIGN KEY (alert_id) REFERENCES alerts (id),
    CONSTRAINT fk_mwo_assigned_user FOREIGN KEY (assigned_to) REFERENCES users (id)
);

CREATE INDEX idx_mwo_machine_id ON maintenance_work_orders (machine_id);
CREATE INDEX idx_mwo_status ON maintenance_work_orders (status);

-- -----------------------------------------------------------------------------
-- MAINTENANCE_RECORDS
-- Completed maintenance activity logs linked to a work order and machine.
-- -----------------------------------------------------------------------------
CREATE TABLE maintenance_records (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    work_order_id  UUID         NOT NULL,
    machine_id     UUID         NOT NULL,
    performed_by   VARCHAR(255) NOT NULL,
    description    TEXT,
    action_taken   TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_maintenance_records PRIMARY KEY (id),
    CONSTRAINT fk_mr_work_order FOREIGN KEY (work_order_id) REFERENCES maintenance_work_orders (id),
    CONSTRAINT fk_mr_machine FOREIGN KEY (machine_id) REFERENCES machines (id)
);

CREATE INDEX idx_mr_work_order_id ON maintenance_records (work_order_id);
CREATE INDEX idx_mr_machine_id ON maintenance_records (machine_id);
