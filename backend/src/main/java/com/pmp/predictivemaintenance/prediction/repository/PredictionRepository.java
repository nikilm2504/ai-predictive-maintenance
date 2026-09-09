package com.pmp.predictivemaintenance.prediction.repository;

import com.pmp.predictivemaintenance.prediction.Prediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, UUID> {
    Page<Prediction> findByMachineId(UUID machineId, Pageable pageable);
    List<Prediction> findTop1ByMachineIdOrderByTsDesc(UUID machineId);
}
