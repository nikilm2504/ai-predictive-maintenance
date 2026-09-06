package com.pmp.predictivemaintenance.machine.service;

import com.pmp.predictivemaintenance.common.exception.DuplicateResourceException;
import com.pmp.predictivemaintenance.common.exception.ResourceNotFoundException;
import com.pmp.predictivemaintenance.machine.Machine;
import com.pmp.predictivemaintenance.machine.Sensor;
import com.pmp.predictivemaintenance.machine.SensorStatus;
import com.pmp.predictivemaintenance.machine.dto.CreateSensorRequest;
import com.pmp.predictivemaintenance.machine.dto.SensorResponse;
import com.pmp.predictivemaintenance.machine.dto.UpdateSensorRequest;
import com.pmp.predictivemaintenance.machine.repository.SensorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SensorService {

    private final SensorRepository sensorRepository;
    private final MachineService machineService;

    public SensorService(SensorRepository sensorRepository, MachineService machineService) {
        this.sensorRepository = sensorRepository;
        this.machineService = machineService;
    }

    @Transactional
    public SensorResponse createSensor(UUID machineId, CreateSensorRequest request) {
        if (sensorRepository.existsBySensorCode(request.sensorCode())) {
            throw new DuplicateResourceException("Sensor code already exists: " + request.sensorCode());
        }

        Machine machine = machineService.getMachineEntity(machineId);

        Sensor sensor = new Sensor(
                machine,
                request.sensorCode(),
                request.sensorType(),
                request.unit()
        );

        sensor = sensorRepository.save(sensor);
        return mapToResponse(sensor);
    }

    @Transactional(readOnly = true)
    public SensorResponse getSensorById(UUID id) {
        Sensor sensor = getSensorEntity(id);
        return mapToResponse(sensor);
    }

    @Transactional(readOnly = true)
    public List<SensorResponse> getSensorsByMachineId(UUID machineId) {
        // Validate machine exists first
        machineService.getMachineEntity(machineId);
        
        return sensorRepository.findByMachineId(machineId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SensorResponse updateSensor(UUID id, UpdateSensorRequest request) {
        Sensor sensor = getSensorEntity(id);

        sensor.setUnit(request.unit());
        sensor.setStatus(request.status());

        sensor = sensorRepository.save(sensor);
        return mapToResponse(sensor);
    }

    @Transactional
    public void deleteSensor(UUID id) {
        Sensor sensor = getSensorEntity(id);
        // Logical deletion: keep historical data but mark as DECOMMISSIONED.
        sensor.setStatus(SensorStatus.DECOMMISSIONED);
        sensorRepository.save(sensor);
    }

    private Sensor getSensorEntity(UUID id) {
        return sensorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor not found with id: " + id));
    }

    private SensorResponse mapToResponse(Sensor sensor) {
        return new SensorResponse(
                sensor.getId(),
                sensor.getMachine().getId(),
                sensor.getSensorCode(),
                sensor.getSensorType(),
                sensor.getUnit(),
                sensor.getStatus(),
                sensor.getInstalledAt()
        );
    }
}
