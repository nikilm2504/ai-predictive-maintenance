package com.pmp.predictivemaintenance.prediction.repository;

import com.pmp.predictivemaintenance.prediction.PredictionExplanation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PredictionExplanationRepository extends JpaRepository<PredictionExplanation, UUID> {
    List<PredictionExplanation> findByPredictionId(UUID predictionId);
}
