package com.pmp.predictivemaintenance.machine.dto;

import com.pmp.predictivemaintenance.machine.SensorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSensorRequest(
        @NotBlank(message = "Sensor code is required")
        String sensorCode,
        
        @NotNull(message = "Sensor type is required")
        SensorType sensorType,
        
        @NotBlank(message = "Unit is required")
        String unit
) {}
