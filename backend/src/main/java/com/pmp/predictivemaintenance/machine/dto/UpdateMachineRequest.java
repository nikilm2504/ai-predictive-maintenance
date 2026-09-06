package com.pmp.predictivemaintenance.machine.dto;

import com.pmp.predictivemaintenance.machine.MachineStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateMachineRequest(
        @NotBlank(message = "Machine name is required")
        String name,
        
        @NotBlank(message = "Machine type is required")
        String machineType,
        
        String location,
        
        @NotNull(message = "Machine status is required")
        MachineStatus status
) {}
