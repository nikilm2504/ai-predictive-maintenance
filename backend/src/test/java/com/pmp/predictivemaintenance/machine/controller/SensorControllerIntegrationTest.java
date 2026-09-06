package com.pmp.predictivemaintenance.machine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmp.predictivemaintenance.machine.Machine;
import com.pmp.predictivemaintenance.machine.Sensor;
import com.pmp.predictivemaintenance.machine.SensorStatus;
import com.pmp.predictivemaintenance.machine.SensorType;
import com.pmp.predictivemaintenance.machine.dto.CreateSensorRequest;
import com.pmp.predictivemaintenance.machine.dto.UpdateSensorRequest;
import com.pmp.predictivemaintenance.machine.repository.MachineRepository;
import com.pmp.predictivemaintenance.machine.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SensorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private SensorRepository sensorRepository;

    private Machine testMachine;

    @BeforeEach
    void setUp() {
        testMachine = new Machine("TEST-M-SETUP", "Test Machine", "MOTOR", "Lab");
        testMachine = machineRepository.save(testMachine);
    }

    @Test
    void shouldCreateSensor() throws Exception {
        CreateSensorRequest request = new CreateSensorRequest("TEST-VIB-01", SensorType.VIBRATION, "g");

        mockMvc.perform(post("/api/v1/machines/{machineId}/sensors", testMachine.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sensorCode", is("TEST-VIB-01")))
                .andExpect(jsonPath("$.sensorType", is("VIBRATION")))
                .andExpect(jsonPath("$.unit", is("g")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.machineId", is(testMachine.getId().toString())))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void shouldReturnNotFoundWhenCreatingSensorForInvalidMachine() throws Exception {
        CreateSensorRequest request = new CreateSensorRequest("TEST-VIB-02", SensorType.VIBRATION, "g");

        mockMvc.perform(post("/api/v1/machines/{machineId}/sensors", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictOnDuplicateSensorCode() throws Exception {
        Sensor sensor = new Sensor(testMachine, "TEST-VIB-03", SensorType.VIBRATION, "g");
        sensorRepository.save(sensor);

        CreateSensorRequest request = new CreateSensorRequest("TEST-VIB-03", SensorType.TEMPERATURE, "C");

        mockMvc.perform(post("/api/v1/machines/{machineId}/sensors", testMachine.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldGetSensorById() throws Exception {
        Sensor sensor = new Sensor(testMachine, "TEST-VIB-04", SensorType.VIBRATION, "g");
        sensor = sensorRepository.save(sensor);

        mockMvc.perform(get("/api/v1/sensors/{id}", sensor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorCode", is("TEST-VIB-04")));
    }

    @Test
    void shouldGetSensorsByMachineId() throws Exception {
        sensorRepository.save(new Sensor(testMachine, "TEST-VIB-05", SensorType.VIBRATION, "g"));
        sensorRepository.save(new Sensor(testMachine, "TEST-TEMP-01", SensorType.TEMPERATURE, "C"));

        mockMvc.perform(get("/api/v1/machines/{machineId}/sensors", testMachine.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldUpdateSensor() throws Exception {
        Sensor sensor = new Sensor(testMachine, "TEST-VIB-06", SensorType.VIBRATION, "g");
        sensor = sensorRepository.save(sensor);

        UpdateSensorRequest request = new UpdateSensorRequest("m/s2", SensorStatus.INACTIVE);

        mockMvc.perform(put("/api/v1/sensors/{id}", sensor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unit", is("m/s2")))
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }

    @Test
    void shouldDeleteSensorLogically() throws Exception {
        Sensor sensor = new Sensor(testMachine, "TEST-VIB-07", SensorType.VIBRATION, "g");
        sensor = sensorRepository.save(sensor);

        mockMvc.perform(delete("/api/v1/sensors/{id}", sensor.getId()))
                .andExpect(status().isNoContent());

        Sensor updatedSensor = sensorRepository.findById(sensor.getId()).orElseThrow();
        assertEquals(SensorStatus.DECOMMISSIONED, updatedSensor.getStatus());
    }
}
