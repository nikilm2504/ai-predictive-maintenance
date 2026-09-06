package com.pmp.predictivemaintenance.telemetry;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single telemetry reading from an industrial machine.
 *
 * IMPORTANT (ADR-003):
 * - This entity stores machine_id as a plain foreign key column.
 * - There is NO @ManyToOne Machine association on this entity.
 * - The Machine entity has NO @OneToMany List<Telemetry>.
 * - Telemetry is queried exclusively via TelemetryRepository
 *   with explicit pagination or time-range bounds.
 *
 * This design prevents accidental loading of millions of telemetry
 * records through JPA relationship traversal.
 */
@Entity
@Table(
        name = "telemetry",
        indexes = {
                // Composite index for machine+time queries per ADR-003
                @Index(name = "idx_telemetry_machine_ts", columnList = "machine_id, ts DESC")
        }
)
public class Telemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Machine identifier stored as a plain FK column.
     * No JPA @ManyToOne association is maintained (per ADR-003).
     */
    @Column(name = "machine_id", nullable = false, updatable = false)
    private UUID machineId;

    @Column(name = "ts", nullable = false, updatable = false)
    private Instant ts;

    @Column(name = "vibration", nullable = false)
    private Double vibration;

    @Column(name = "temperature", nullable = false)
    private Double temperature;

    @Column(name = "current", nullable = false)
    private Double current;

    @Column(name = "rpm", nullable = false)
    private Double rpm;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected Telemetry() {
        // Required by JPA
    }

    public Telemetry(UUID machineId, Instant ts, Double vibration, Double temperature,
                     Double current, Double rpm) {
        this.machineId = machineId;
        this.ts = ts;
        this.vibration = vibration;
        this.temperature = temperature;
        this.current = current;
        this.rpm = rpm;
    }

    // -------------------------------------------------------------------------
    // Getters (all immutable — telemetry records are write-once)
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public UUID getMachineId() { return machineId; }
    public Instant getTs() { return ts; }
    public Double getVibration() { return vibration; }
    public Double getTemperature() { return temperature; }
    public Double getCurrent() { return current; }
    public Double getRpm() { return rpm; }

    // -------------------------------------------------------------------------
    // equals / hashCode — identity based on database id only
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Telemetry other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Telemetry{id=" + id + ", machineId=" + machineId + ", ts=" + ts + "}";
    }
}
