package com.pmp.predictivemaintenance.prediction.dto;

import java.time.Instant;
import java.util.UUID;

public record PredictionResponse(
        UUID id,
        UUID machineId,
        Instant ts,
        String faultType,
        Double failureProbability,
        Double confidence,
        Double healthScore,
        String riskLevel,
        String modelVersion
) {}
