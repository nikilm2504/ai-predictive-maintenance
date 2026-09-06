package com.pmp.predictivemaintenance.telemetry.dto;

import java.time.Instant;
import java.util.UUID;

public record TelemetryResponse(
        UUID id,
        UUID machineId,
        Instant timestamp,
        Double vibration,
        Double temperature,
        Double current,
        Double rpm
) {}
