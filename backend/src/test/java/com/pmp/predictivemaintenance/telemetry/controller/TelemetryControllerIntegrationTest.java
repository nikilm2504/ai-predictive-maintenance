package com.pmp.predictivemaintenance.telemetry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmp.predictivemaintenance.machine.Machine;
import com.pmp.predictivemaintenance.machine.repository.MachineRepository;
import com.pmp.predictivemaintenance.telemetry.dto.TelemetryIngestRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TelemetryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MachineRepository machineRepository;

    private Machine testMachine;

    @BeforeEach
    void setUp() {
        testMachine = new Machine("TEST-TEL-M01", "Telemetry Test Machine", "PUMP", "Basement");
        testMachine = machineRepository.save(testMachine);
    }

    @Test
    void shouldIngestTelemetry() throws Exception {
        TelemetryIngestRequest request = new TelemetryIngestRequest(
                Instant.parse("2026-09-06T12:30:00Z"),
                2.35,
                48.7,
                5.2,
                1450.0
        );

        mockMvc.perform(post("/api/v1/machines/{machineId}/telemetry", testMachine.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.machineId", is(testMachine.getId().toString())))
                .andExpect(jsonPath("$.timestamp", is("2026-09-06T12:30:00Z")))
                .andExpect(jsonPath("$.vibration", is(2.35)))
                .andExpect(jsonPath("$.temperature", is(48.7)))
                .andExpect(jsonPath("$.current", is(5.2)))
                .andExpect(jsonPath("$.rpm", is(1450.0)));
    }

    @Test
    void shouldReturnNotFoundWhenIngestingForNonexistentMachine() throws Exception {
        TelemetryIngestRequest request = new TelemetryIngestRequest(
                Instant.now(), 2.0, 45.0, 5.0, 1500.0);

        mockMvc.perform(post("/api/v1/machines/{machineId}/telemetry", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidNumericValues() throws Exception {
        TelemetryIngestRequest request = new TelemetryIngestRequest(
                Instant.now(), Double.NaN, 45.0, 5.0, 1500.0);

        mockMvc.perform(post("/api/v1/machines/{machineId}/telemetry", testMachine.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request")));
    }

    @Test
    void shouldGetLatestTelemetry() throws Exception {
        // Insert older
        mockMvc.perform(post("/api/v1/machines/{machineId}/telemetry", testMachine.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TelemetryIngestRequest(
                        Instant.parse("2026-09-06T10:00:00Z"), 1.0, 40.0, 4.0, 1000.0))));

        // Insert newer
        mockMvc.perform(post("/api/v1/machines/{machineId}/telemetry", testMachine.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TelemetryIngestRequest(
                        Instant.parse("2026-09-06T11:00:00Z"), 2.0, 45.0, 5.0, 1500.0))));

        mockMvc.perform(get("/api/v1/machines/{machineId}/telemetry/latest", testMachine.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp", is("2026-09-06T11:00:00Z")));
    }

    @Test
    void shouldReturnNotFoundWhenNoTelemetryAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/machines/{machineId}/telemetry/latest", testMachine.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("No telemetry available")));
    }

    @Test
    void shouldGetTelemetryHistoryWithPagination() throws Exception {
        // Insert 3 records
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/v1/machines/{machineId}/telemetry", testMachine.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new TelemetryIngestRequest(
                            Instant.parse(String.format("2026-09-06T1%d:00:00Z", i)), i * 1.0, 40.0, 4.0, 1000.0))));
        }

        mockMvc.perform(get("/api/v1/machines/{machineId}/telemetry?page=0&size=2", testMachine.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                // Should sort by timestamp DESC (13:00, then 12:00)
                .andExpect(jsonPath("$.content[0].timestamp", is("2026-09-06T13:00:00Z")))
                .andExpect(jsonPath("$.content[1].timestamp", is("2026-09-06T12:00:00Z")));
    }

    @Test
    void shouldFilterTelemetryByTimeRange() throws Exception {
        // Insert 3 records: 10:00, 11:00, 12:00
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/machines/{machineId}/telemetry", testMachine.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new TelemetryIngestRequest(
                            Instant.parse(String.format("2026-09-06T1%d:00:00Z", i)), 1.0, 40.0, 4.0, 1000.0))));
        }

        // Filter 10:30 to 11:30 (should get only 11:00)
        mockMvc.perform(get("/api/v1/machines/{machineId}/telemetry?from=2026-09-06T10:30:00Z&to=2026-09-06T11:30:00Z", testMachine.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].timestamp", is("2026-09-06T11:00:00Z")));
    }

    @Test
    void shouldRejectInvalidTimeRange() throws Exception {
        mockMvc.perform(get("/api/v1/machines/{machineId}/telemetry?from=2026-09-06T12:00:00Z&to=2026-09-06T10:00:00Z", testMachine.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("before or equal to")));
    }
}
