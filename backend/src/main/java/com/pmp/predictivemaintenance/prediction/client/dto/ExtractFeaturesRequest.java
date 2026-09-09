package com.pmp.predictivemaintenance.prediction.client.dto;

import java.util.List;

public record ExtractFeaturesRequest(
    String machine_id,
    List<RawTelemetryDto> telemetry_window
) {}
