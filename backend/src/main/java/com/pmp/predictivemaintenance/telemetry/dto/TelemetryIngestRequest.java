package com.pmp.predictivemaintenance.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record TelemetryIngestRequest(
        @NotNull(message = "Timestamp is required")
        Instant timestamp,
        
        @NotNull(message = "Vibration reading is required")
        Double vibration,
        
        @NotNull(message = "Temperature reading is required")
        Double temperature,
        
        @NotNull(message = "Current reading is required")
        Double current,
        
        @NotNull(message = "Water flow reading is required")
        @JsonProperty("water_flow")
        @JsonAlias({"waterFlow", "water_flow"})
        Double waterFlow
) {}
