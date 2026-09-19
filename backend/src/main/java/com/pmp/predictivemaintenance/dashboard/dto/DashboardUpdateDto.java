package com.pmp.predictivemaintenance.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
            @JsonProperty("water_flow")
            @JsonAlias({"waterFlow", "water_flow"})
            Double waterFlow
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
