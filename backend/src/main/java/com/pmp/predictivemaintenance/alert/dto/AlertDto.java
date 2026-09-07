package com.pmp.predictivemaintenance.alert.dto;

import com.pmp.predictivemaintenance.alert.AlertSeverity;
import com.pmp.predictivemaintenance.alert.AlertStatus;

import java.time.Instant;
import java.util.UUID;

public record AlertDto(
        UUID id,
        UUID machineId,
        UUID predictionId,
        AlertSeverity severity,
        AlertStatus status,
        String title,
        String description,
        Instant createdAt,
        Instant acknowledgedAt,
        Instant resolvedAt
) {}
