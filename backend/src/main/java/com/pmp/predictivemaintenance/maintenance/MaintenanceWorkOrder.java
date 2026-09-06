package com.pmp.predictivemaintenance.maintenance;

import com.pmp.predictivemaintenance.alert.Alert;
import com.pmp.predictivemaintenance.common.User;
import com.pmp.predictivemaintenance.machine.Machine;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a maintenance task raised for an industrial machine.
 *
 * A MaintenanceWorkOrder:
 *   - belongs to one Machine (required)
 *   - optionally references an Alert that triggered it
 *   - optionally references a User who is assigned to it
 *
 * Human-in-the-loop (ADR-007): sensitive state mutations (dispatch, completion)
 * must be approved by a MAINTENANCE_ENGINEER or ADMIN. Business logic for this
 * approval is implemented in a later milestone.
 */
@Entity
@Table(
        name = "maintenance_work_orders",
        indexes = {
                @Index(name = "idx_mwo_machine_id", columnList = "machine_id"),
                @Index(name = "idx_mwo_status", columnList = "status")
        }
)
public class MaintenanceWorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mwo_machine"))
    private Machine machine;

    /**
     * The alert that prompted this work order. Nullable.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", foreignKey = @ForeignKey(name = "fk_mwo_alert"))
    private Alert alert;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Priority stored as a plain string column to share the AlertSeverity vocabulary
     * (LOW, MEDIUM, HIGH, CRITICAL) without a dependency on the alert package enum.
     */
    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private WorkOrderStatus status;

    /**
     * The user (MAINTENANCE_ENGINEER or ADMIN) assigned to this work order. Nullable.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", foreignKey = @ForeignKey(name = "fk_mwo_assigned_user"))
    private User assignedTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.status == null) this.status = WorkOrderStatus.OPEN;
        if (this.priority == null) this.priority = "MEDIUM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected MaintenanceWorkOrder() {
        // Required by JPA
    }

    public MaintenanceWorkOrder(Machine machine, Alert alert, String title,
                                 String description, String priority) {
        this.machine = machine;
        this.alert = alert;
        this.title = title;
        this.description = description;
        this.priority = priority;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public Machine getMachine() { return machine; }
    public Alert getAlert() { return alert; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public WorkOrderStatus getStatus() { return status; }
    public User getAssignedTo() { return assignedTo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }

    // -------------------------------------------------------------------------
    // Setters (only for mutable lifecycle fields)
    // -------------------------------------------------------------------------

    public void setStatus(WorkOrderStatus status) { this.status = status; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setDescription(String description) { this.description = description; }

    // -------------------------------------------------------------------------
    // equals / hashCode — identity based on database id only
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaintenanceWorkOrder other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "MaintenanceWorkOrder{id=" + id + ", status=" + status
                + ", priority='" + priority + "', title='" + title + "'}";
    }
}
