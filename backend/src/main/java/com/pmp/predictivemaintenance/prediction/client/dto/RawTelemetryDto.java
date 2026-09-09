package com.pmp.predictivemaintenance.prediction.client.dto;

public record RawTelemetryDto(
    String timestamp,
    Double vibration,
    Double temperature,
    Double current,
    Double rpm
) {}
