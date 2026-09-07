package com.pmp.predictivemaintenance.maintenance;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaintenanceDecisionEngine {

    /**
     * Generates a deterministic maintenance recommendation based on the ML risk assessment.
     *
     * @param riskLevel          The overall machine risk level (HEALTHY, LOW, MEDIUM, HIGH, CRITICAL)
     * @param healthScore        The 0-100 health score
     * @param failureProbability The ML failure probability
     * @param topContributors    The top SHAP feature names contributing to the risk
     * @return A structured MaintenanceRecommendation, or null if the machine is HEALTHY.
     */
    public MaintenanceRecommendation evaluate(String riskLevel, Double healthScore, Double failureProbability, List<String> topContributors) {
        if (riskLevel == null || "HEALTHY".equalsIgnoreCase(riskLevel)) {
            return null; // No maintenance needed
        }

        MaintenancePriority priority = switch (riskLevel.toUpperCase()) {
            case "LOW" -> MaintenancePriority.LOW;
            case "MEDIUM" -> MaintenancePriority.MEDIUM;
            case "HIGH" -> MaintenancePriority.HIGH;
            case "CRITICAL" -> MaintenancePriority.CRITICAL;
            default -> MaintenancePriority.MEDIUM;
        };

        if (priority == MaintenancePriority.CRITICAL) {
            return new MaintenanceRecommendation(
                    priority,
                    MaintenanceAction.IMMEDIATE_MACHINE_INSPECTION,
                    "Machine risk is CRITICAL. Immediate inspection required."
            );
        }

        // Determine action based on strongest evidence (top SHAP contributors)
        MaintenanceAction action = MaintenanceAction.GENERAL_MACHINE_INSPECTION;
        String reason = "General deterioration detected based on multi-sensor anomalies.";

        if (topContributors != null && !topContributors.isEmpty()) {
            String topFeature = topContributors.get(0).toLowerCase();

            if (topFeature.contains("vibration")) {
                action = MaintenanceAction.INSPECT_VIBRATION_SYSTEM;
                reason = "Elevated vibration is the primary contributor to the predicted failure risk.";
            } else if (topFeature.contains("temperature")) {
                action = MaintenanceAction.INSPECT_TEMPERATURE_SYSTEM;
                reason = "Elevated temperature is the primary contributor to the predicted failure risk.";
            } else if (topFeature.contains("current")) {
                action = MaintenanceAction.INSPECT_CURRENT_LOAD;
                reason = "Current load anomalies are the primary contributors to the predicted failure risk.";
            } else if (topFeature.contains("rpm")) {
                action = MaintenanceAction.INSPECT_RPM_SYSTEM;
                reason = "RPM fluctuations are the primary contributors to the predicted failure risk.";
            }
        }

        // For LOW risk, downgrade to SCHEDULE_PREVENTIVE_MAINTENANCE if it's a general inspection
        if (priority == MaintenancePriority.LOW && action == MaintenanceAction.GENERAL_MACHINE_INSPECTION) {
            action = MaintenanceAction.SCHEDULE_PREVENTIVE_MAINTENANCE;
            reason = "Minor deterioration detected. Schedule preventive maintenance.";
        }

        return new MaintenanceRecommendation(priority, action, reason);
    }
}
