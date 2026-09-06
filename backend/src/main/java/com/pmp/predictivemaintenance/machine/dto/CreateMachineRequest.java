package com.pmp.predictivemaintenance.machine.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMachineRequest(
        @NotBlank(message = "Machine code is required")
        String machineCode,
        
        @NotBlank(message = "Machine name is required")
        String name,
        
        @NotBlank(message = "Machine type is required")
        String machineType,
        
        String location
) {}
