package com.pmp.predictivemaintenance.prediction;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * A single SHAP feature contribution record belonging to a Prediction.
 *
 * One Prediction can have many PredictionExplanation records —
 * one per feature used by the ML model.
 *
 * direction: "POSITIVE" means the feature pushes the prediction towards failure;
 *            "NEGATIVE" means it pushes away from failure.
 */
@Entity
@Table(
        name = "prediction_explanations",
        indexes = {
                @Index(name = "idx_pred_exp_prediction_id", columnList = "prediction_id")
        }
)
public class PredictionExplanation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pred_exp_prediction"))
    private Prediction prediction;

    @Column(name = "feature_name", nullable = false, length = 255)
    private String featureName;

    @Column(name = "contribution", nullable = false)
    private Double contribution;

    /**
     * INCREASES_RISK: this feature pushes the prediction towards higher failure probability.
     * DECREASES_RISK: this feature pushes the prediction towards lower failure probability.
     * See architecture.md §4.2, prediction_explanations data dictionary.
     */
    @Column(name = "direction", nullable = false, length = 16)
    private String direction;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected PredictionExplanation() {
        // Required by JPA
    }

    public PredictionExplanation(Prediction prediction, String featureName,
                                  Double contribution, String direction) {
        this.prediction = prediction;
        this.featureName = featureName;
        this.contribution = contribution;
        this.direction = direction;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public Prediction getPrediction() { return prediction; }
    public String getFeatureName() { return featureName; }
    public Double getContribution() { return contribution; }
    public String getDirection() { return direction; }

    // -------------------------------------------------------------------------
    // equals / hashCode — identity based on database id only
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PredictionExplanation other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PredictionExplanation{id=" + id + ", featureName='" + featureName
                + "', contribution=" + contribution + ", direction='" + direction + "'}";
    }
}
