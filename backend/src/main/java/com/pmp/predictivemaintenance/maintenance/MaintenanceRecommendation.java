package com.pmp.predictivemaintenance.maintenance;

public record MaintenanceRecommendation(
        MaintenancePriority priority,
        MaintenanceAction recommendedAction,
        String reason
) {}
