package com.pmp.predictivemaintenance.machine.controller;

import com.pmp.predictivemaintenance.machine.dto.CreateSensorRequest;
import com.pmp.predictivemaintenance.machine.dto.SensorResponse;
import com.pmp.predictivemaintenance.machine.dto.UpdateSensorRequest;
import com.pmp.predictivemaintenance.machine.service.SensorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @PostMapping("/machines/{machineId}/sensors")
    @ResponseStatus(HttpStatus.CREATED)
    public SensorResponse createSensor(
            @PathVariable UUID machineId,
            @Valid @RequestBody CreateSensorRequest request) {
        return sensorService.createSensor(machineId, request);
    }

    @GetMapping("/sensors/{id}")
    public SensorResponse getSensorById(@PathVariable UUID id) {
        return sensorService.getSensorById(id);
    }

    @GetMapping("/machines/{machineId}/sensors")
    public List<SensorResponse> getSensorsByMachineId(@PathVariable UUID machineId) {
        return sensorService.getSensorsByMachineId(machineId);
    }

    @PutMapping("/sensors/{id}")
    public SensorResponse updateSensor(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSensorRequest request) {
        return sensorService.updateSensor(id, request);
    }

    @DeleteMapping("/sensors/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSensor(@PathVariable UUID id) {
        sensorService.deleteSensor(id);
    }
}
