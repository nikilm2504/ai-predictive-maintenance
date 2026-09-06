package com.pmp.predictivemaintenance.machine;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a physical or simulated sensor attached to a machine.
 *
 * Relationship: Sensor belongs to one Machine (many-to-one).
 * Machine does NOT maintain a list of Sensors to avoid eager graph loading.
 * Sensors for a machine are queried via SensorRepository.
 */
@Entity
@Table(
        name = "sensors",
        uniqueConstraints = @UniqueConstraint(name = "uq_sensors_sensor_code", columnNames = "sensor_code")
)
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sensors_machine"))
    private Machine machine;

    @Column(name = "sensor_code", nullable = false, length = 100)
    private String sensorCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 50)
    private SensorType sensorType;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SensorStatus status;

    @Column(name = "installed_at", nullable = false, updatable = false)
    private Instant installedAt;

    @PrePersist
    protected void onCreate() {
        if (this.installedAt == null) this.installedAt = Instant.now();
        if (this.status == null) this.status = SensorStatus.ACTIVE;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected Sensor() {
        // Required by JPA
    }

    public Sensor(Machine machine, String sensorCode, SensorType sensorType, String unit) {
        this.machine = machine;
        this.sensorCode = sensorCode;
        this.sensorType = sensorType;
        this.unit = unit;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public Machine getMachine() { return machine; }
    public String getSensorCode() { return sensorCode; }
    public SensorType getSensorType() { return sensorType; }
    public String getUnit() { return unit; }
    public SensorStatus getStatus() { return status; }
    public Instant getInstalledAt() { return installedAt; }

    // -------------------------------------------------------------------------
    // Setters (only for mutable fields)
    // -------------------------------------------------------------------------

    public void setUnit(String unit) { this.unit = unit; }
    public void setStatus(SensorStatus status) { this.status = status; }

    // -------------------------------------------------------------------------
    // equals / hashCode — identity based on database id only
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sensor other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Sensor{id=" + id + ", sensorCode='" + sensorCode + "', sensorType=" + sensorType + "}";
    }
}
