package com.pmp.predictivemaintenance.telemetry.service;

import com.pmp.predictivemaintenance.common.exception.ResourceNotFoundException;
import com.pmp.predictivemaintenance.machine.repository.MachineRepository;
import com.pmp.predictivemaintenance.telemetry.Telemetry;
import com.pmp.predictivemaintenance.telemetry.dto.TelemetryIngestRequest;
import com.pmp.predictivemaintenance.telemetry.dto.TelemetryResponse;
import com.pmp.predictivemaintenance.telemetry.repository.TelemetryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.pmp.predictivemaintenance.prediction.service.PredictionOrchestrator;

@Service
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final MachineRepository machineRepository;
    private final PredictionOrchestrator predictionOrchestrator;

    public TelemetryService(TelemetryRepository telemetryRepository, MachineRepository machineRepository, PredictionOrchestrator predictionOrchestrator) {
        this.telemetryRepository = telemetryRepository;
        this.machineRepository = machineRepository;
        this.predictionOrchestrator = predictionOrchestrator;
    }

    @Transactional
    public TelemetryResponse ingestTelemetry(UUID machineId, TelemetryIngestRequest request) {
        validateMachineExists(machineId);
        validateNumericValues(request);

        Telemetry telemetry = new Telemetry(
                machineId,
                request.timestamp(),
                request.vibration(),
                request.temperature(),
                request.current(),
                request.rpm()
        );

        telemetry = telemetryRepository.save(telemetry);
        
        // Trigger prediction pipeline asynchronously, ensuring the current transaction is committed first
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(() -> predictionOrchestrator.evaluateMachineHealth(machineId));
                }
            }
        );
        
        return mapToResponse(telemetry);
    }

    @Transactional
    public TelemetryResponse ingestTelemetryByMachineCode(String machineCode, TelemetryIngestRequest request) {
        return machineRepository.findByMachineCode(machineCode)
                .map(machine -> ingestTelemetry(machine.getId(), request))
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with code: " + machineCode));
    }

    @Transactional(readOnly = true)
    public TelemetryResponse getLatestTelemetry(UUID machineId) {
        validateMachineExists(machineId);

        return telemetryRepository.findFirstByMachineIdOrderByTsDesc(machineId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No telemetry available for machine " + machineId));
    }

    @Transactional(readOnly = true)
    public Page<TelemetryResponse> getTelemetryHistory(UUID machineId, Instant from, Instant to, Pageable pageable) {
        validateMachineExists(machineId);
        validateTimeRange(from, to);

        Page<Telemetry> page;

        if (from != null && to != null) {
            page = telemetryRepository.findByMachineIdAndTsBetweenOrderByTsDesc(machineId, from, to, pageable);
        } else if (from != null) {
            page = telemetryRepository.findByMachineIdAndTsGreaterThanEqualOrderByTsDesc(machineId, from, pageable);
        } else if (to != null) {
            page = telemetryRepository.findByMachineIdAndTsLessThanEqualOrderByTsDesc(machineId, to, pageable);
        } else {
            page = telemetryRepository.findByMachineIdOrderByTsDesc(machineId, pageable);
        }

        return page.map(this::mapToResponse);
    }

    private void validateMachineExists(UUID machineId) {
        if (!machineRepository.existsById(machineId)) {
            throw new ResourceNotFoundException("Machine not found with id: " + machineId);
        }
    }

    private void validateTimeRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' timestamp must be before or equal to 'to' timestamp");
        }
    }

    private void validateNumericValues(TelemetryIngestRequest request) {
        if (!Double.isFinite(request.vibration()) ||
            !Double.isFinite(request.temperature()) ||
            !Double.isFinite(request.current()) ||
            !Double.isFinite(request.rpm())) {
            throw new IllegalArgumentException("Sensor values must be finite numeric values (no NaN or Infinity)");
        }
    }

    private TelemetryResponse mapToResponse(Telemetry telemetry) {
        return new TelemetryResponse(
                telemetry.getId(),
                telemetry.getMachineId(),
                telemetry.getTs(),
                telemetry.getVibration(),
                telemetry.getTemperature(),
                telemetry.getCurrent(),
                telemetry.getRpm()
        );
    }
}
