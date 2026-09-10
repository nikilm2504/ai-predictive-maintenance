package com.pmp.predictivemaintenance.prediction.client.dto;

import java.util.List;
import java.util.Map;

public record PredictResponse(
    Double failure_probability,
    Double confidence,
    String prediction,
    String model_version,
    Double health_score,
    String risk_level,
    Map<String, Double> baseline_deviations,
    List<ExplanationDto> explanations
) {}
