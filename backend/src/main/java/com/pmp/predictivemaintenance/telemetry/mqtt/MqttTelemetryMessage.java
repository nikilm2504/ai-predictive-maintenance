package com.pmp.predictivemaintenance.telemetry.mqtt;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record MqttTelemetryMessage(
        Instant timestamp,
        Double vibration,
        Double temperature,
        Double current,
        @JsonProperty("water_flow")
        @JsonAlias({"waterFlow", "water_flow"})
        Double waterFlow
) {}
