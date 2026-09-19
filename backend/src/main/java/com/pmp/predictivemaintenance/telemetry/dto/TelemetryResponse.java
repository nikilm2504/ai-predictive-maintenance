package com.pmp.predictivemaintenance.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record TelemetryResponse(
        UUID id,
        UUID machineId,
        Instant timestamp,
        Double vibration,
        Double temperature,
        Double current,
        @JsonProperty("water_flow")
        @JsonAlias({"waterFlow", "water_flow"})
        Double waterFlow
) {}
