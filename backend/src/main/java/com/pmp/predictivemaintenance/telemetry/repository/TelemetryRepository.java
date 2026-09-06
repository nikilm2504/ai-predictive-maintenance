package com.pmp.predictivemaintenance.telemetry.repository;

import com.pmp.predictivemaintenance.telemetry.Telemetry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, UUID> {
    
    Optional<Telemetry> findFirstByMachineIdOrderByTsDesc(UUID machineId);

    Page<Telemetry> findByMachineIdOrderByTsDesc(UUID machineId, Pageable pageable);

    Page<Telemetry> findByMachineIdAndTsBetweenOrderByTsDesc(UUID machineId, Instant from, Instant to, Pageable pageable);

    Page<Telemetry> findByMachineIdAndTsGreaterThanEqualOrderByTsDesc(UUID machineId, Instant from, Pageable pageable);

    Page<Telemetry> findByMachineIdAndTsLessThanEqualOrderByTsDesc(UUID machineId, Instant to, Pageable pageable);
}
