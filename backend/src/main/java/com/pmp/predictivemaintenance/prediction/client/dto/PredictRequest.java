package com.pmp.predictivemaintenance.prediction.client.dto;

import java.util.Map;

public record PredictRequest(
    String machine_id,
    Map<String, Double> features
) {}
