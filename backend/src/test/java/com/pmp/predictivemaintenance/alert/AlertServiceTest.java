package com.pmp.predictivemaintenance.alert;

import com.pmp.predictivemaintenance.common.exception.ResourceNotFoundException;
import com.pmp.predictivemaintenance.machine.Machine;
import com.pmp.predictivemaintenance.machine.repository.MachineRepository;
import com.pmp.predictivemaintenance.maintenance.MaintenanceDecisionEngine;
import com.pmp.predictivemaintenance.prediction.Prediction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private MachineRepository machineRepository;

    private AlertService alertService;
    private final MaintenanceDecisionEngine decisionEngine = new MaintenanceDecisionEngine();

    private Machine testMachine;
    private Prediction testPrediction;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository, machineRepository, decisionEngine);
        testMachine = new Machine("M-001", "Test Machine", "Type", "Loc");
        testPrediction = new Prediction(testMachine, Instant.now(), null, 0.85, 0.9, 25.0, "v1");
    }

    @Test
    void shouldNotCreateAlertForHealthyMachine() {
        Alert result = alertService.processRiskAssessment(UUID.randomUUID(), testPrediction, "HEALTHY", 95.0, 0.05, List.of());
        assertThat(result).isNull();
        verifyNoInteractions(machineRepository, alertRepository);
    }

    @Test
    void shouldCreateNewAlertWhenNoneExists() {
        when(machineRepository.findById(any())).thenReturn(Optional.of(testMachine));
        when(alertRepository.findFirstByMachineIdAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert alert = alertService.processRiskAssessment(UUID.randomUUID(), testPrediction, "HIGH", 25.0, 0.85, List.of("vibration_rms"));

        assertThat(alert).isNotNull();
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(alert.getDescription()).contains("INSPECT_VIBRATION_SYSTEM");

        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void shouldNotUpdateAlertIfExistingIsStrictlyHigherSeverity() {
        Alert existingAlert = new Alert(testMachine, testPrediction, AlertSeverity.HIGH, "Title", "Desc");
        existingAlert.setStatus(AlertStatus.OPEN);

        when(machineRepository.findById(any())).thenReturn(Optional.of(testMachine));
        when(alertRepository.findFirstByMachineIdAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(existingAlert));

        Alert result = alertService.processRiskAssessment(UUID.randomUUID(), testPrediction, "MEDIUM", 45.0, 0.6, List.of());

        assertThat(result).isEqualTo(existingAlert);
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void shouldUpdateAlertIfExistingIsEqualSeverity() {
        Alert existingAlert = new Alert(testMachine, testPrediction, AlertSeverity.HIGH, "Old Title", "Old Desc");
        existingAlert.setStatus(AlertStatus.OPEN);

        when(machineRepository.findById(any())).thenReturn(Optional.of(testMachine));
        when(alertRepository.findFirstByMachineIdAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(existingAlert));
        when(alertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Alert result = alertService.processRiskAssessment(UUID.randomUUID(), testPrediction, "HIGH", 25.0, 0.85, List.of("vibration_rms"));

        assertThat(result.getSeverity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(result.getDescription()).contains("Health Score: 25.0");
        verify(alertRepository).save(existingAlert);
    }

    @Test
    void shouldUpgradeAlertIfExistingIsLowerSeverity() {
        Alert existingAlert = new Alert(testMachine, testPrediction, AlertSeverity.LOW, "Machine Risk Level: LOW", "Health Score: 73.36");
        existingAlert.setStatus(AlertStatus.OPEN);

        when(machineRepository.findById(any())).thenReturn(Optional.of(testMachine));
        when(alertRepository.findFirstByMachineIdAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(existingAlert));
        when(alertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Alert result = alertService.processRiskAssessment(UUID.randomUUID(), testPrediction, "CRITICAL", 10.0, 0.95, List.of("vibration_rms"));

        assertThat(result.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(result.getTitle()).isEqualTo("Machine Risk Level: CRITICAL");
        assertThat(result.getDescription()).contains("Health Score: 10.0");
        verify(alertRepository).save(existingAlert);
    }

    @Test
    void shouldMapRiskLevelsCorrectly() {
        when(machineRepository.findById(any())).thenReturn(Optional.of(testMachine));
        when(alertRepository.findFirstByMachineIdAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // LOW
        Alert lowAlert = alertService.processRiskAssessment(UUID.randomUUID(), testPrediction, "LOW", 75.0, 0.1, List.of());
        assertThat(lowAlert.getSeverity()).isEqualTo(AlertSeverity.LOW);

        // MEDIUM
        Alert medAlert = alertService.processRiskAssessment(UUID.randomUUID(), testPrediction, "MEDIUM", 50.0, 0.4, List.of());
        assertThat(medAlert.getSeverity()).isEqualTo(AlertSeverity.MEDIUM);

        // HIGH
        Alert highAlert = alertService.processRiskAssessment(UUID.randomUUID(), testPrediction, "HIGH", 30.0, 0.7, List.of());
        assertThat(highAlert.getSeverity()).isEqualTo(AlertSeverity.HIGH);

        // CRITICAL
        Alert critAlert = alertService.processRiskAssessment(UUID.randomUUID(), testPrediction, "CRITICAL", 10.0, 0.95, List.of());
        assertThat(critAlert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void shouldAcknowledgeAlert() {
        Alert existingAlert = new Alert(testMachine, testPrediction, AlertSeverity.HIGH, "Title", "Desc");
        existingAlert.setStatus(AlertStatus.OPEN);

        when(alertRepository.findById(any())).thenReturn(Optional.of(existingAlert));
        when(alertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Alert result = alertService.acknowledgeAlert(UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(result.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void shouldResolveAlert() {
        Alert existingAlert = new Alert(testMachine, testPrediction, AlertSeverity.HIGH, "Title", "Desc");
        existingAlert.setStatus(AlertStatus.ACKNOWLEDGED);

        when(alertRepository.findById(any())).thenReturn(Optional.of(existingAlert));
        when(alertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Alert result = alertService.resolveAlert(UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(result.getResolvedAt()).isNotNull();
    }

    @Test
    void shouldThrowWhenAcknowledgingResolvedAlert() {
        Alert existingAlert = new Alert(testMachine, testPrediction, AlertSeverity.HIGH, "Title", "Desc");
        existingAlert.setStatus(AlertStatus.RESOLVED);

        when(alertRepository.findById(any())).thenReturn(Optional.of(existingAlert));

        assertThrows(IllegalStateException.class, () -> alertService.acknowledgeAlert(UUID.randomUUID()));
    }
}
