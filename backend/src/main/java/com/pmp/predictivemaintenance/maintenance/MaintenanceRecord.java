package com.pmp.predictivemaintenance.maintenance;

import com.pmp.predictivemaintenance.common.User;
import com.pmp.predictivemaintenance.machine.Machine;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a completed maintenance activity log.
 *
 * A MaintenanceRecord:
 *   - belongs to one MaintenanceWorkOrder (required)
 *   - belongs to one Machine (required — denormalized for direct machine-based queries)
 *   - references the User who performed the work (required FK to users table)
 *   - records what was found and what action was taken
 *
 * Architecture note: performed_by is a UUID FK to users (architecture.md §4.2).
 * It is NOT a free-text name field.
 */
@Entity
@Table(
        name = "maintenance_records",
        indexes = {
                @Index(name = "idx_mr_work_order_id", columnList = "work_order_id"),
                @Index(name = "idx_mr_machine_id", columnList = "machine_id"),
                @Index(name = "idx_mr_performed_by", columnList = "performed_by")
        }
)
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mr_work_order"))
    private MaintenanceWorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mr_machine"))
    private Machine machine;

    /**
     * The engineer or technician who performed the maintenance.
     * Stored as a UUID foreign key to the users table (NOT a free-text name).
     * See architecture.md §4.2, maintenance_records data dictionary.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by", nullable = false, foreignKey = @ForeignKey(name = "fk_mr_performed_by"))
    private User performedBy;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "action_taken", columnDefinition = "TEXT")
    private String actionTaken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected MaintenanceRecord() {
        // Required by JPA
    }

    public MaintenanceRecord(MaintenanceWorkOrder workOrder, Machine machine,
                              User performedBy, String description, String actionTaken) {
        this.workOrder = workOrder;
        this.machine = machine;
        this.performedBy = performedBy;
        this.description = description;
        this.actionTaken = actionTaken;
    }

    // -------------------------------------------------------------------------
    // Getters (write-once records — no setters for core fields)
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public MaintenanceWorkOrder getWorkOrder() { return workOrder; }
    public Machine getMachine() { return machine; }
    public User getPerformedBy() { return performedBy; }
    public String getDescription() { return description; }
    public String getActionTaken() { return actionTaken; }
    public Instant getCreatedAt() { return createdAt; }

    // -------------------------------------------------------------------------
    // equals / hashCode — identity based on database id only
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaintenanceRecord other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "MaintenanceRecord{id=" + id + ", createdAt=" + createdAt + "}";
    }
}
