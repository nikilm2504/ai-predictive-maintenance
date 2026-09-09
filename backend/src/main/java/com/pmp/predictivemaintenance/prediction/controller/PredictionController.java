package com.pmp.predictivemaintenance.prediction.controller;

import com.pmp.predictivemaintenance.prediction.Prediction;
import com.pmp.predictivemaintenance.prediction.PredictionExplanation;
import com.pmp.predictivemaintenance.prediction.dto.PredictionExplanationResponse;
import com.pmp.predictivemaintenance.prediction.dto.PredictionResponse;
import com.pmp.predictivemaintenance.prediction.repository.PredictionExplanationRepository;
import com.pmp.predictivemaintenance.prediction.repository.PredictionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/machines/{machineId}/predictions")
public class PredictionController {

    private final PredictionRepository predictionRepository;
    private final PredictionExplanationRepository explanationRepository;

    public PredictionController(PredictionRepository predictionRepository,
                                PredictionExplanationRepository explanationRepository) {
        this.predictionRepository = predictionRepository;
        this.explanationRepository = explanationRepository;
    }

    @GetMapping
    public ResponseEntity<Page<PredictionResponse>> getPredictionHistory(
            @PathVariable UUID machineId,
            @PageableDefault(sort = "ts", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Prediction> predictions = predictionRepository.findByMachineId(machineId, pageable);
        return ResponseEntity.ok(predictions.map(this::mapToResponse));
    }

    @GetMapping("/latest")
    public ResponseEntity<PredictionResponse> getLatestPrediction(@PathVariable UUID machineId) {
        List<Prediction> latest = predictionRepository.findTop1ByMachineIdOrderByTsDesc(machineId);
        if (latest.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToResponse(latest.get(0)));
    }

    @GetMapping("/{predictionId}/explanations")
    public ResponseEntity<List<PredictionExplanationResponse>> getExplanations(
            @PathVariable UUID machineId, 
            @PathVariable UUID predictionId) {
        List<PredictionExplanation> explanations = explanationRepository.findByPredictionId(predictionId);
        return ResponseEntity.ok(explanations.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    private PredictionResponse mapToResponse(Prediction p) {
        String risk = "HEALTHY";
        if (p.getHealthScore() < 20) risk = "CRITICAL";
        else if (p.getHealthScore() < 40) risk = "HIGH";
        else if (p.getHealthScore() < 60) risk = "MEDIUM";
        else if (p.getHealthScore() < 80) risk = "LOW";
        
        return new PredictionResponse(
                p.getId(),
                p.getMachine().getId(),
                p.getTs(),
                p.getFaultType(),
                p.getFailureProbability(),
                p.getConfidence(),
                p.getHealthScore(),
                risk,
                p.getModelVersion()
        );
    }

    private PredictionExplanationResponse mapToResponse(PredictionExplanation pe) {
        return new PredictionExplanationResponse(
                pe.getId(),
                pe.getPrediction().getId(),
                pe.getFeatureName(),
                pe.getContribution(), // map contribution to shapValue
                pe.getDirection()
        );
    }
}
