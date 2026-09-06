package com.pmp.predictivemaintenance.machine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmp.predictivemaintenance.machine.Machine;
import com.pmp.predictivemaintenance.machine.MachineStatus;
import com.pmp.predictivemaintenance.machine.dto.CreateMachineRequest;
import com.pmp.predictivemaintenance.machine.dto.UpdateMachineRequest;
import com.pmp.predictivemaintenance.machine.repository.MachineRepository;
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
class MachineControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MachineRepository machineRepository;

    @Test
    void shouldCreateMachine() throws Exception {
        CreateMachineRequest request = new CreateMachineRequest("TEST-M001", "Motor 1", "MOTOR", "Floor 1");

        mockMvc.perform(post("/api/v1/machines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.machineCode", is("TEST-M001")))
                .andExpect(jsonPath("$.name", is("Motor 1")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        CreateMachineRequest request = new CreateMachineRequest("", "Motor 1", "MOTOR", "Floor 1");

        mockMvc.perform(post("/api/v1/machines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void shouldReturnConflictOnDuplicateMachineCode() throws Exception {
        Machine machine = new Machine("TEST-M002", "Motor 2", "MOTOR", "Floor 1");
        machineRepository.save(machine);

        CreateMachineRequest request = new CreateMachineRequest("TEST-M002", "Motor Dup", "MOTOR", "Floor 1");

        mockMvc.perform(post("/api/v1/machines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Duplicate Resource")));
    }

    @Test
    void shouldGetMachineById() throws Exception {
        Machine machine = new Machine("TEST-M003", "Motor 3", "MOTOR", "Floor 1");
        machine = machineRepository.save(machine);

        mockMvc.perform(get("/api/v1/machines/{id}", machine.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.machineCode", is("TEST-M003")))
                .andExpect(jsonPath("$.name", is("Motor 3")));
    }

    @Test
    void shouldReturnNotFoundForInvalidId() throws Exception {
        mockMvc.perform(get("/api/v1/machines/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    void shouldGetAllMachines() throws Exception {
        machineRepository.save(new Machine("TEST-M004", "Motor 4", "MOTOR", "Floor 1"));
        machineRepository.save(new Machine("TEST-M005", "Motor 5", "MOTOR", "Floor 1"));

        mockMvc.perform(get("/api/v1/machines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void shouldUpdateMachine() throws Exception {
        Machine machine = new Machine("TEST-M006", "Motor 6", "MOTOR", "Floor 1");
        machine = machineRepository.save(machine);

        UpdateMachineRequest request = new UpdateMachineRequest("Motor 6 Updated", "MOTOR", "Floor 2", MachineStatus.INACTIVE);

        mockMvc.perform(put("/api/v1/machines/{id}", machine.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Motor 6 Updated")))
                .andExpect(jsonPath("$.location", is("Floor 2")))
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }

    @Test
    void shouldDecommissionMachine() throws Exception {
        Machine machine = new Machine("TEST-M007", "Motor 7", "MOTOR", "Floor 1");
        machine = machineRepository.save(machine);

        mockMvc.perform(delete("/api/v1/machines/{id}", machine.getId()))
                .andExpect(status().isNoContent());

        Machine updatedMachine = machineRepository.findById(machine.getId()).orElseThrow();
        assertEquals(MachineStatus.DECOMMISSIONED, updatedMachine.getStatus());
    }
}
