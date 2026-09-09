package com.pmp.predictivemaintenance.prediction.client.dto;

public record ExplanationDto(
    String feature_name,
    Double shap_value,
    Double absolute_shap_value,
    String direction
) {}
