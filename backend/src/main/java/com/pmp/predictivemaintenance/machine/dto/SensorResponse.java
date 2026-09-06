package com.pmp.predictivemaintenance.machine.dto;

import com.pmp.predictivemaintenance.machine.SensorStatus;
import com.pmp.predictivemaintenance.machine.SensorType;
import java.time.Instant;
import java.util.UUID;

public record SensorResponse(
        UUID id,
        UUID machineId,
        String sensorCode,
        SensorType sensorType,
        String unit,
        SensorStatus status,
        Instant installedAt
) {}
