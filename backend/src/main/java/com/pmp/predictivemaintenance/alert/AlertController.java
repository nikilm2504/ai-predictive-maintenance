package com.pmp.predictivemaintenance.alert;

import com.pmp.predictivemaintenance.alert.dto.AlertDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<AlertDto>> getAllAlerts() {
        List<AlertDto> alerts = alertService.getAllAlerts()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/alerts/{alertId}")
    public ResponseEntity<AlertDto> getAlert(@PathVariable UUID alertId) {
        Alert alert = alertService.getAlert(alertId);
        return ResponseEntity.ok(mapToDto(alert));
    }

    @GetMapping("/machines/{machineId}/alerts")
    public ResponseEntity<List<AlertDto>> getAlertsForMachine(@PathVariable UUID machineId) {
        List<AlertDto> alerts = alertService.getAlertsForMachine(machineId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<AlertDto> acknowledgeAlert(@PathVariable UUID alertId) {
        Alert alert = alertService.acknowledgeAlert(alertId);
        return ResponseEntity.ok(mapToDto(alert));
    }

    @PostMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<AlertDto> resolveAlert(@PathVariable UUID alertId) {
        Alert alert = alertService.resolveAlert(alertId);
        return ResponseEntity.ok(mapToDto(alert));
    }

    private AlertDto mapToDto(Alert alert) {
        UUID predictionId = alert.getPrediction() != null ? alert.getPrediction().getId() : null;
        return new AlertDto(
                alert.getId(),
                alert.getMachine().getId(),
                predictionId,
                alert.getSeverity(),
                alert.getStatus(),
                alert.getTitle(),
                alert.getDescription(),
                alert.getCreatedAt(),
                alert.getAcknowledgedAt(),
                alert.getResolvedAt()
        );
    }
}
