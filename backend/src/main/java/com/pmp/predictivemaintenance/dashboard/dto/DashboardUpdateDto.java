package com.pmp.predictivemaintenance.dashboard.dto;

import java.util.List;
import java.util.UUID;

public record DashboardUpdateDto(
        UUID machineId,
        String machineCode,
        String timestamp,
        TelemetryDto telemetry,
        PredictionDto prediction,
        AlertDto alert,
        List<ShapDto> shapExplanations
) {
    public record TelemetryDto(
            Double vibration,
            Double temperature,
            Double current,
            Double rpm
    ) {}

    public record PredictionDto(
            Double failureProbability,
            Double healthScore,
            String riskLevel,
            String modelVersion
    ) {}

    public record AlertDto(
            String severity,
            String status,
            String title,
            String description
    ) {}

    public record ShapDto(
            String featureName,
            Double contribution,
            String direction
    ) {}
}
