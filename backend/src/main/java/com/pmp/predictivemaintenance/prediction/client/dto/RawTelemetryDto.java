package com.pmp.predictivemaintenance.prediction.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RawTelemetryDto(
    String timestamp,
    Double vibration,
    Double temperature,
    Double current,
    @JsonProperty("water_flow")
    Double waterFlow
) {}
