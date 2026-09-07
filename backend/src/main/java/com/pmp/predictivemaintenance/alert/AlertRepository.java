package com.pmp.predictivemaintenance.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    
    List<Alert> findByMachineIdOrderByCreatedAtDesc(UUID machineId);
    
    Optional<Alert> findFirstByMachineIdAndStatusInOrderByCreatedAtDesc(UUID machineId, List<AlertStatus> statuses);
}
