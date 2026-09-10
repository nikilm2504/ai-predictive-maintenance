package com.pmp.predictivemaintenance.prediction.service;

import com.pmp.predictivemaintenance.alert.AlertService;
import com.pmp.predictivemaintenance.machine.Machine;
import com.pmp.predictivemaintenance.machine.repository.MachineRepository;
import com.pmp.predictivemaintenance.prediction.client.MlPredictionClient;
import com.pmp.predictivemaintenance.prediction.client.dto.*;
import com.pmp.predictivemaintenance.prediction.repository.PredictionExplanationRepository;
import com.pmp.predictivemaintenance.prediction.repository.PredictionRepository;
import com.pmp.predictivemaintenance.telemetry.Telemetry;
import com.pmp.predictivemaintenance.telemetry.repository.TelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionOrchestratorTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private MlPredictionClient mlClient;

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private PredictionExplanationRepository explanationRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PredictionOrchestrator orchestrator;

    private Machine machine;

    @BeforeEach
    void setUp() {
        orchestrator = new PredictionOrchestrator(
                telemetryRepository,
                machineRepository,
                mlClient,
                predictionRepository,
                explanationRepository,
                alertService,
                eventPublisher
        );

        machine = new Machine(
                "M-001",
                "Test Machine",
                "MOTOR",
                "Factory Floor A"
        );
    }

    @Test
    void shouldNotCallMLIfWindowIsTooSmall() {
        UUID machineId = UUID.randomUUID();

        when(machineRepository.findById(machineId))
                .thenReturn(Optional.of(machine));

        when(telemetryRepository.findByMachineIdOrderByTsDesc(
                eq(machineId),
                any()
        )).thenReturn(
                new PageImpl<>(
                        List.of(
                                new Telemetry(
                                        machineId,
                                        Instant.now(),
                                        1.0,
                                        40.0,
                                        5.0,
                                        1400.0
                                )
                        )
                )
        );

        orchestrator.evaluateMachineHealth(machineId);

        verifyNoInteractions(mlClient);
    }

    @Test
    void shouldHandleMLServiceFailureGracefully() {
        UUID machineId = UUID.randomUUID();

        when(machineRepository.findById(machineId))
                .thenReturn(Optional.of(machine));

        List<Telemetry> window = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            window.add(
                    new Telemetry(
                            machineId,
                            Instant.now().minusSeconds(i),
                            1.0,
                            40.0,
                            5.0,
                            1400.0
                    )
            );
        }

        when(telemetryRepository.findByMachineIdOrderByTsDesc(
                eq(machineId),
                any()
        )).thenReturn(new PageImpl<>(window));

        when(mlClient.extractFeatures(any()))
                .thenThrow(new RuntimeException("Connection refused"));

        // Method should catch the exception and not propagate,
        // allowing ingestion to continue.
        orchestrator.evaluateMachineHealth(machineId);

        verify(mlClient).extractFeatures(any());
        verifyNoMoreInteractions(mlClient);
        verifyNoInteractions(predictionRepository);
    }

    @Test
    void shouldProcessEndToEndSuccessfully() {
        UUID machineId = UUID.randomUUID();

        when(machineRepository.findById(machineId))
                .thenReturn(Optional.of(machine));

        List<Telemetry> window = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            window.add(
                    new Telemetry(
                            machineId,
                            Instant.now().minusSeconds(i),
                            1.0,
                            40.0,
                            5.0,
                            1400.0
                    )
            );
        }

        when(telemetryRepository.findByMachineIdOrderByTsDesc(
                eq(machineId),
                any()
        )).thenReturn(new PageImpl<>(window));

        ExtractFeaturesResponse featureResponse =
                new ExtractFeaturesResponse(
                        Map.of("vibration_mean", 1.0)
                );

        when(mlClient.extractFeatures(any()))
                .thenReturn(featureResponse);

        PredictResponse predictResponse = new PredictResponse(
                0.85,   // failure_probability
                0.85,   // confidence
                "FAILURE_RISK",
                "v1",
                25.0,   // health_score
                "HIGH",
                Map.of(),
                List.of(
                        new ExplanationDto(
                                "vibration_mean",
                                0.5,
                                0.5,
                                "INCREASES_RISK"
                        )
                )
        );

        when(mlClient.predict(any()))
                .thenReturn(predictResponse);

        when(predictionRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        orchestrator.evaluateMachineHealth(machineId);

        verify(mlClient).extractFeatures(any());
        verify(mlClient).predict(any());
        verify(predictionRepository).save(any());
        verify(explanationRepository).save(any());

        verify(alertService).processRiskAssessment(
                eq(machineId),
                any(),
                eq("HIGH"),
                eq(25.0),
                eq(0.85),
                any()
        );
    }
}