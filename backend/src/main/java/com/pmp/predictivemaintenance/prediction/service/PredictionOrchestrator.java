package com.pmp.predictivemaintenance.prediction.service;

import com.pmp.predictivemaintenance.alert.AlertService;
import com.pmp.predictivemaintenance.machine.Machine;
import com.pmp.predictivemaintenance.machine.repository.MachineRepository;
import com.pmp.predictivemaintenance.prediction.Prediction;
import com.pmp.predictivemaintenance.prediction.PredictionExplanation;
import com.pmp.predictivemaintenance.prediction.client.MlPredictionClient;
import com.pmp.predictivemaintenance.prediction.client.dto.*;
import com.pmp.predictivemaintenance.prediction.repository.PredictionExplanationRepository;
import com.pmp.predictivemaintenance.prediction.repository.PredictionRepository;
import com.pmp.predictivemaintenance.telemetry.Telemetry;
import com.pmp.predictivemaintenance.telemetry.repository.TelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PredictionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PredictionOrchestrator.class);
    
    // We need 10 records for a window based on Python configuration
    private static final int WINDOW_SIZE = 10;

    private final TelemetryRepository telemetryRepository;
    private final MachineRepository machineRepository;
    private final MlPredictionClient mlClient;
    private final PredictionRepository predictionRepository;
    private final PredictionExplanationRepository explanationRepository;
    private final AlertService alertService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public PredictionOrchestrator(TelemetryRepository telemetryRepository,
                                  MachineRepository machineRepository,
                                  MlPredictionClient mlClient,
                                  PredictionRepository predictionRepository,
                                  PredictionExplanationRepository explanationRepository,
                                  AlertService alertService,
                                  org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.telemetryRepository = telemetryRepository;
        this.machineRepository = machineRepository;
        this.mlClient = mlClient;
        this.predictionRepository = predictionRepository;
        this.explanationRepository = explanationRepository;
        this.alertService = alertService;
        this.eventPublisher = eventPublisher;
    }

    public void evaluateMachineHealth(UUID machineId) {
        try {
            // 1. Check Machine existence
            Optional<Machine> machineOpt = machineRepository.findById(machineId);
            if (machineOpt.isEmpty()) {
                log.warn("Cannot evaluate health: Machine not found (ID: {})", machineId);
                return;
            }
            Machine machine = machineOpt.get();

            // 2. Fetch latest telemetry records to form a window
            List<Telemetry> window = telemetryRepository.findByMachineIdOrderByTsDesc(machineId, PageRequest.of(0, WINDOW_SIZE)).getContent();
            
            if (window.size() < WINDOW_SIZE) {
                log.debug("Not enough telemetry records to form a window for Machine {}. Found {}, need {}", machine.getMachineCode(), window.size(), WINDOW_SIZE);
                return;
            }

            // Reverse them so they are chronological as expected by time-series processing
            List<RawTelemetryDto> rawWindow = window.stream()
                    .map(t -> new RawTelemetryDto(
                            t.getTs().toString(),
                            t.getVibration(),
                            t.getTemperature(),
                            t.getCurrent(),
                            t.getRpm()
                    ))
                    .sorted(Comparator.comparing(RawTelemetryDto::timestamp))
                    .collect(Collectors.toList());

            // 3. Call Python to extract features
            ExtractFeaturesResponse featureResponse = mlClient.extractFeatures(new ExtractFeaturesRequest(machine.getMachineCode(), rawWindow));
            
            if (featureResponse == null || featureResponse.features() == null || featureResponse.features().isEmpty()) {
                log.warn("Received empty feature extraction response for Machine {}", machine.getMachineCode());
                return;
            }

            // 4. Call Python to Predict
            PredictResponse predictResponse = mlClient.predict(new PredictRequest(machine.getMachineCode(), featureResponse.features()));
            
            if (predictResponse == null) {
                log.warn("Received empty prediction response for Machine {}", machine.getMachineCode());
                return;
            }

            // 5. Persist Prediction
            Prediction prediction = new Prediction(
                    machine,
                    Instant.now(),
                    predictResponse.prediction(),
                    predictResponse.failure_probability(),
                    0.0, // Confidence is not provided by current API schema directly, storing 0 or could store probability again
                    predictResponse.health_score(),
                    predictResponse.model_version()
            );
            prediction = predictionRepository.save(prediction);

            // 6. Persist Explanations
            if (predictResponse.explanations() != null) {
                for (ExplanationDto exp : predictResponse.explanations()) {
                    PredictionExplanation pe = new PredictionExplanation(
                            prediction,
                            exp.feature_name(),
                            exp.shap_value(),
                            exp.direction()
                    );
                    explanationRepository.save(pe);
                }
            }

            // 7. Trigger AlertService
            List<String> topContributors = List.of();
            if (predictResponse.explanations() != null) {
                topContributors = predictResponse.explanations().stream()
                        .map(ExplanationDto::feature_name)
                        .collect(Collectors.toList());
            }

            log.info("Evaluated Machine {}: Risk Level = {}, Health Score = {}", machine.getMachineCode(), predictResponse.risk_level(), predictResponse.health_score());

            com.pmp.predictivemaintenance.alert.Alert alertResult = alertService.processRiskAssessment(
                    machineId,
                    prediction,
                    predictResponse.risk_level(),
                    predictResponse.health_score(),
                    predictResponse.failure_probability(),
                    topContributors
            );

            // 8. Publish Dashboard SSE Event
            Telemetry latestTelemetry = window.get(0); // Window is ordered by TsDesc, so 0 is latest
            
            com.pmp.predictivemaintenance.dashboard.dto.DashboardUpdateDto.TelemetryDto tDto = new com.pmp.predictivemaintenance.dashboard.dto.DashboardUpdateDto.TelemetryDto(
                    latestTelemetry.getVibration(),
                    latestTelemetry.getTemperature(),
                    latestTelemetry.getCurrent(),
                    latestTelemetry.getRpm()
            );

            com.pmp.predictivemaintenance.dashboard.dto.DashboardUpdateDto.PredictionDto pDto = new com.pmp.predictivemaintenance.dashboard.dto.DashboardUpdateDto.PredictionDto(
                    prediction.getFailureProbability(),
                    prediction.getHealthScore(),
                    predictResponse.risk_level(),
                    prediction.getModelVersion()
            );

            com.pmp.predictivemaintenance.dashboard.dto.DashboardUpdateDto.AlertDto aDto = null;
            if (alertResult != null) {
                aDto = new com.pmp.predictivemaintenance.dashboard.dto.DashboardUpdateDto.AlertDto(
                        alertResult.getSeverity().name(),
                        alertResult.getStatus().name(),
                        alertResult.getTitle(),
                        alertResult.getDescription()
                );
            }

            List<com.pmp.predictivemaintenance.dashboard.dto.DashboardUpdateDto.ShapDto> sDtoList = List.of();
            if (predictResponse.explanations() != null) {
                sDtoList = predictResponse.explanations().stream()
                        .map(e -> new com.pmp.predictivemaintenance.dashboard.dto.DashboardUpdateDto.ShapDto(
                                e.feature_name(), e.shap_value(), e.direction()))
                        .collect(Collectors.toList());
            }

            com.pmp.predictivemaintenance.dashboard.dto.DashboardUpdateDto updateDto = new com.pmp.predictivemaintenance.dashboard.dto.DashboardUpdateDto(
                    machineId,
                    machine.getMachineCode(),
                    latestTelemetry.getTs().toString(),
                    tDto,
                    pDto,
                    aDto,
                    sDtoList
            );

            eventPublisher.publishEvent(new com.pmp.predictivemaintenance.dashboard.dto.MachineUpdateEvent(this, updateDto));

        } catch (Exception e) {
            log.error("Failed to execute ML pipeline for Machine {}: {}", machineId, e.getMessage(), e);
            // We log and return to avoid crashing the ingestion pipeline
        }
    }
}
