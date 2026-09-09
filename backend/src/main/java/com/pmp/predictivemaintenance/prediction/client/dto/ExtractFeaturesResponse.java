package com.pmp.predictivemaintenance.prediction.client.dto;

import java.util.Map;

public record ExtractFeaturesResponse(
    Map<String, Double> features
) {}
