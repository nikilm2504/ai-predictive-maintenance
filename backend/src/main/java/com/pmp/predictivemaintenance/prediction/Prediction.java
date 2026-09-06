package com.pmp.predictivemaintenance.prediction;

import com.pmp.predictivemaintenance.machine.Machine;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single ML inference result for a machine at a point in time.
 *
 * A Prediction belongs to one Machine.
 * PredictionExplanation records (SHAP values) reference this entity.
 */
@Entity
@Table(
        name = "predictions",
        indexes = {
                @Index(name = "idx_predictions_machine_ts", columnList = "machine_id, ts DESC")
        }
)
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false, foreignKey = @ForeignKey(name = "fk_predictions_machine"))
    private Machine machine;

    @Column(name = "ts", nullable = false, updatable = false)
    private Instant ts;

    @Column(name = "fault_type", length = 100)
    private String faultType;

    @Column(name = "failure_probability", nullable = false)
    private Double failureProbability;

    @Column(name = "confidence", nullable = false)
    private Double confidence;

    @Column(name = "health_score", nullable = false)
    private Double healthScore;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected Prediction() {
        // Required by JPA
    }

    public Prediction(Machine machine, Instant ts, String faultType,
                      Double failureProbability, Double confidence,
                      Double healthScore, String modelVersion) {
        this.machine = machine;
        this.ts = ts;
        this.faultType = faultType;
        this.failureProbability = failureProbability;
        this.confidence = confidence;
        this.healthScore = healthScore;
        this.modelVersion = modelVersion;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public Machine getMachine() { return machine; }
    public Instant getTs() { return ts; }
    public String getFaultType() { return faultType; }
    public Double getFailureProbability() { return failureProbability; }
    public Double getConfidence() { return confidence; }
    public Double getHealthScore() { return healthScore; }
    public String getModelVersion() { return modelVersion; }

    // -------------------------------------------------------------------------
    // equals / hashCode — identity based on database id only
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Prediction other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Prediction{id=" + id + ", faultType='" + faultType
                + "', failureProbability=" + failureProbability
                + ", healthScore=" + healthScore + "}";
    }
}
