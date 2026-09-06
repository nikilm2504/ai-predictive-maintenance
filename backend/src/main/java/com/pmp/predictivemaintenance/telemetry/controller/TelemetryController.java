package com.pmp.predictivemaintenance.telemetry.controller;

import com.pmp.predictivemaintenance.telemetry.dto.TelemetryIngestRequest;
import com.pmp.predictivemaintenance.telemetry.dto.TelemetryResponse;
import com.pmp.predictivemaintenance.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/machines/{machineId}/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TelemetryResponse ingestTelemetry(
            @PathVariable UUID machineId,
            @Valid @RequestBody TelemetryIngestRequest request) {
        return telemetryService.ingestTelemetry(machineId, request);
    }

    @GetMapping("/latest")
    public TelemetryResponse getLatestTelemetry(@PathVariable UUID machineId) {
        return telemetryService.getLatestTelemetry(machineId);
    }

    @GetMapping
    public Page<TelemetryResponse> getTelemetryHistory(
            @PathVariable UUID machineId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Pageable pageable) {
        return telemetryService.getTelemetryHistory(machineId, from, to, pageable);
    }
}
