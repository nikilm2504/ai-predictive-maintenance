package com.pmp.predictivemaintenance.machine.dto;

import com.pmp.predictivemaintenance.machine.MachineStatus;
import java.time.Instant;
import java.util.UUID;

public record MachineResponse(
        UUID id,
        String machineCode,
        String name,
        String machineType,
        String location,
        MachineStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
