package com.pmp.predictivemaintenance.maintenance;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceDecisionEngineTest {

    private final MaintenanceDecisionEngine engine = new MaintenanceDecisionEngine();

    @Test
    void shouldReturnNullForHealthyMachine() {
        MaintenanceRecommendation rec = engine.evaluate("HEALTHY", 95.0, 0.05, List.of());
        assertThat(rec).isNull();
    }

    @Test
    void shouldRecommendLowPriorityForLowRisk() {
        MaintenanceRecommendation rec = engine.evaluate("LOW", 75.0, 0.2, List.of("vibration_rms"));
        assertThat(rec).isNotNull();
        assertThat(rec.priority()).isEqualTo(MaintenancePriority.LOW);
        assertThat(rec.recommendedAction()).isEqualTo(MaintenanceAction.INSPECT_VIBRATION_SYSTEM);
    }

    @Test
    void shouldRecommendMediumPriorityForMediumRisk() {
        MaintenanceRecommendation rec = engine.evaluate("MEDIUM", 55.0, 0.5, List.of("temperature_mean"));
        assertThat(rec.priority()).isEqualTo(MaintenancePriority.MEDIUM);
        assertThat(rec.recommendedAction()).isEqualTo(MaintenanceAction.INSPECT_TEMPERATURE_SYSTEM);
    }

    @Test
    void shouldRecommendHighPriorityForHighRisk() {
        MaintenanceRecommendation rec = engine.evaluate("HIGH", 25.0, 0.85, List.of("current_mean"));
        assertThat(rec.priority()).isEqualTo(MaintenancePriority.HIGH);
        assertThat(rec.recommendedAction()).isEqualTo(MaintenanceAction.INSPECT_CURRENT_LOAD);
    }

    @Test
    void shouldRecommendCriticalPriorityForCriticalRisk() {
        MaintenanceRecommendation rec = engine.evaluate("CRITICAL", 10.0, 0.95, List.of("vibration_rms"));
        assertThat(rec.priority()).isEqualTo(MaintenancePriority.CRITICAL);
        assertThat(rec.recommendedAction()).isEqualTo(MaintenanceAction.IMMEDIATE_MACHINE_INSPECTION);
    }

    @Test
    void shouldRecommendGeneralInspectionIfShapUnclear() {
        MaintenanceRecommendation rec = engine.evaluate("HIGH", 25.0, 0.85, List.of("unknown_feature"));
        assertThat(rec.priority()).isEqualTo(MaintenancePriority.HIGH);
        assertThat(rec.recommendedAction()).isEqualTo(MaintenanceAction.GENERAL_MACHINE_INSPECTION);
    }
}
