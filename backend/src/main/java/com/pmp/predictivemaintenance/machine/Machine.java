package com.pmp.predictivemaintenance.machine;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an industrial machine being monitored.
 *
 * IMPORTANT (ADR-003): This entity intentionally has NO @OneToMany List<Telemetry>.
 * Telemetry is queried explicitly via TelemetryRepository using machine_id.
 * Loading all telemetry records through a collection would risk JVM OOM errors.
 */
@Entity
@Table(
        name = "machines",
        uniqueConstraints = @UniqueConstraint(name = "uq_machines_machine_code", columnNames = "machine_code")
)
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "machine_code", nullable = false, length = 100)
    private String machineCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "machine_type", nullable = false, length = 100)
    private String machineType;

    @Column(name = "location", length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private MachineStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.status == null) this.status = MachineStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected Machine() {
        // Required by JPA
    }

    public Machine(String machineCode, String name, String machineType, String location) {
        this.machineCode = machineCode;
        this.name = name;
        this.machineType = machineType;
        this.location = location;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public String getMachineCode() { return machineCode; }
    public String getName() { return name; }
    public String getMachineType() { return machineType; }
    public String getLocation() { return location; }
    public MachineStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // -------------------------------------------------------------------------
    // Setters (only for mutable fields)
    // -------------------------------------------------------------------------

    public void setName(String name) { this.name = name; }
    public void setMachineType(String machineType) { this.machineType = machineType; }
    public void setLocation(String location) { this.location = location; }
    public void setStatus(MachineStatus status) { this.status = status; }

    // -------------------------------------------------------------------------
    // equals / hashCode — identity based on database id only
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Machine other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Machine{id=" + id + ", machineCode='" + machineCode + "', status=" + status + "}";
    }
}
