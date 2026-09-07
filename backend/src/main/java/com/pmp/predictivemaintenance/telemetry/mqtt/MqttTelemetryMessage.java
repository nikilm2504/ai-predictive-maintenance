package com.pmp.predictivemaintenance.telemetry.mqtt;

import java.time.Instant;

public record MqttTelemetryMessage(
        Instant timestamp,
        Double vibration,
        Double temperature,
        Double current,
        Double rpm
) {}
