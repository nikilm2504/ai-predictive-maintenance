package com.pmp.predictivemaintenance.alert;

import com.pmp.predictivemaintenance.machine.Machine;
import com.pmp.predictivemaintenance.prediction.Prediction;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an alert generated from an ML prediction that breaches a risk threshold.
 *
 * An Alert belongs to one Machine.
 * An Alert optionally references the Prediction that triggered it.
 */
@Entity
@Table(
        name = "alerts",
        indexes = {
                @Index(name = "idx_alerts_machine_id", columnList = "machine_id"),
                @Index(name = "idx_alerts_status", columnList = "status")
        }
)
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false, foreignKey = @ForeignKey(name = "fk_alerts_machine"))
    private Machine machine;

    /**
     * The ML prediction that triggered this alert.
     * Nullable: manual or system-generated alerts may not have an associated prediction.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prediction_id", foreignKey = @ForeignKey(name = "fk_alerts_prediction"))
    private Prediction prediction;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.status == null) this.status = AlertStatus.OPEN;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected Alert() {
        // Required by JPA
    }

    public Alert(Machine machine, Prediction prediction, AlertSeverity severity, String title, String description) {
        this.machine = machine;
        this.prediction = prediction;
        this.severity = severity;
        this.title = title;
        this.description = description;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public Machine getMachine() { return machine; }
    public Prediction getPrediction() { return prediction; }
    public AlertSeverity getSeverity() { return severity; }
    public AlertStatus getStatus() { return status; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    // -------------------------------------------------------------------------
    // Setters (only for mutable lifecycle fields)
    // -------------------------------------------------------------------------

    public void setStatus(AlertStatus status) { this.status = status; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setSeverity(AlertSeverity severity) { this.severity = severity; }

    // -------------------------------------------------------------------------
    // equals / hashCode — identity based on database id only
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alert other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Alert{id=" + id + ", severity=" + severity + ", status=" + status
                + ", title='" + title + "'}";
    }
}
