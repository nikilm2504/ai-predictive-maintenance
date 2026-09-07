package com.pmp.predictivemaintenance.alert;

import com.pmp.predictivemaintenance.common.exception.ResourceNotFoundException;
import com.pmp.predictivemaintenance.machine.Machine;
import com.pmp.predictivemaintenance.machine.repository.MachineRepository;
import com.pmp.predictivemaintenance.maintenance.MaintenanceDecisionEngine;
import com.pmp.predictivemaintenance.maintenance.MaintenanceRecommendation;
import com.pmp.predictivemaintenance.prediction.Prediction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final MachineRepository machineRepository;
    private final MaintenanceDecisionEngine decisionEngine;

    public AlertService(AlertRepository alertRepository,
                        MachineRepository machineRepository,
                        MaintenanceDecisionEngine decisionEngine) {
        this.alertRepository = alertRepository;
        this.machineRepository = machineRepository;
        this.decisionEngine = decisionEngine;
    }

    /**
     * Processes machine risk information and generates/updates an alert deterministically.
     */
    @Transactional
    public Alert processRiskAssessment(UUID machineId, Prediction prediction, String riskLevel, Double healthScore, Double failureProbability, List<String> topContributors) {
        if ("HEALTHY".equalsIgnoreCase(riskLevel)) {
            return null; // No alert generated for healthy machines
        }

        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with id: " + machineId));

        AlertSeverity targetSeverity = AlertSeverity.valueOf(riskLevel.toUpperCase());

        MaintenanceRecommendation recommendation = decisionEngine.evaluate(riskLevel, healthScore, failureProbability, topContributors);

        String title = "Machine Risk Level: " + targetSeverity.name();
        String description = "Health Score: " + healthScore + ", Failure Probability: " + failureProbability;
        if (recommendation != null) {
            description += "\nRecommended Action: " + recommendation.recommendedAction() + "\nReason: " + recommendation.reason();
        }

        // Deduplication Logic
        // Find existing unresolved alert for the machine
        Optional<Alert> existingAlertOpt = alertRepository.findFirstByMachineIdAndStatusInOrderByCreatedAtDesc(
                machineId, List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
        );

        if (existingAlertOpt.isPresent()) {
            Alert existingAlert = existingAlertOpt.get();
            // If the risk hasn't increased, do not create a duplicate
            if (existingAlert.getSeverity().ordinal() >= targetSeverity.ordinal()) {
                return existingAlert; // Active alert already covers this severity
            }

            // If the risk has increased (e.g., MEDIUM -> HIGH), upgrade the severity
            existingAlert.setSeverity(targetSeverity);
            // We could update the description, but let's keep it simple or append
            return alertRepository.save(existingAlert);
        }

        // Create new alert
        Alert alert = new Alert(machine, prediction, targetSeverity, title, description);
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert acknowledgeAlert(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + alertId));

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new IllegalStateException("Cannot acknowledge a resolved alert.");
        }

        if (alert.getStatus() == AlertStatus.OPEN) {
            alert.setStatus(AlertStatus.ACKNOWLEDGED);
            alert.setAcknowledgedAt(Instant.now());
        }
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert resolveAlert(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + alertId));

        if (alert.getStatus() != AlertStatus.RESOLVED) {
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setResolvedAt(Instant.now());
        }
        return alertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public Alert getAlert(UUID alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + alertId));
    }

    @Transactional(readOnly = true)
    public List<Alert> getAlertsForMachine(UUID machineId) {
        if (!machineRepository.existsById(machineId)) {
            throw new ResourceNotFoundException("Machine not found with id: " + machineId);
        }
        return alertRepository.findByMachineIdOrderByCreatedAtDesc(machineId);
    }

    @Transactional(readOnly = true)
    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }
}
