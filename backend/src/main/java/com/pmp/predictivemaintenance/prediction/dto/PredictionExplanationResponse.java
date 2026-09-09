package com.pmp.predictivemaintenance.prediction.dto;

import java.util.UUID;

public record PredictionExplanationResponse(
        UUID id,
        UUID predictionId,
        String featureName,
        Double shapValue,
        String direction
) {}
