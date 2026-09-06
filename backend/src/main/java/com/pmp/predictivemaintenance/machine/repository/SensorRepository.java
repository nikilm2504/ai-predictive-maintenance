package com.pmp.predictivemaintenance.machine.repository;

import com.pmp.predictivemaintenance.machine.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, UUID> {
    boolean existsBySensorCode(String sensorCode);
    List<Sensor> findByMachineId(UUID machineId);
}
