package com.pmp.predictivemaintenance.machine.dto;

import com.pmp.predictivemaintenance.machine.SensorStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateSensorRequest(
        @NotBlank(message = "Unit is required")
        String unit,
        
        @NotNull(message = "Sensor status is required")
        SensorStatus status
) {}
