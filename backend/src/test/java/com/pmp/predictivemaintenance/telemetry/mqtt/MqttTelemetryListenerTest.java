package com.pmp.predictivemaintenance.telemetry.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pmp.predictivemaintenance.common.exception.ResourceNotFoundException;
import com.pmp.predictivemaintenance.telemetry.dto.TelemetryIngestRequest;
import com.pmp.predictivemaintenance.telemetry.service.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MqttTelemetryListenerTest {

    private TelemetryService telemetryService;
    private MqttTelemetryListener listener;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        telemetryService = mock(TelemetryService.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        listener = new MqttTelemetryListener(objectMapper, telemetryService);
    }

    @Test
    void shouldProcessValidTelemetryMessage() {
        String payload = """
                {
                  "timestamp": "2026-09-06T12:30:00Z",
                  "vibration": 2.35,
                  "temperature": 48.7,
                  "current": 5.2,
                  "rpm": 1450.0
                }
                """;

        Message<String> message = createMessage("machines/M001/telemetry", payload);
        
        listener.processMessage(message);

        ArgumentCaptor<TelemetryIngestRequest> captor = ArgumentCaptor.forClass(TelemetryIngestRequest.class);
        verify(telemetryService).ingestTelemetryByMachineCode(eq("M001"), captor.capture());

        TelemetryIngestRequest request = captor.getValue();
        assertEquals(2.35, request.vibration());
        assertEquals("2026-09-06T12:30:00Z", request.timestamp().toString());
    }

    @Test
    void shouldIgnoreMessageWithoutTopic() {
        Message<String> message = new GenericMessage<>("{}");
        
        listener.processMessage(message);
        
        verify(telemetryService, never()).ingestTelemetryByMachineCode(any(), any());
    }

    @Test
    void shouldIgnoreMessageWithInvalidTopicStructure() {
        Message<String> message = createMessage("invalid/topic", "{}");
        
        listener.processMessage(message);
        
        verify(telemetryService, never()).ingestTelemetryByMachineCode(any(), any());
    }

    @Test
    void shouldNotCrashOnInvalidJson() {
        Message<String> message = createMessage("machines/M002/telemetry", "not-json");
        
        // This should log an error but not throw an exception out of the method
        assertDoesNotThrow(() -> listener.processMessage(message));
        
        verify(telemetryService, never()).ingestTelemetryByMachineCode(any(), any());
    }

    @Test
    void shouldNotCrashOnMissingFields() {
        String payload = """
                {
                  "vibration": 2.35
                }
                """;
        Message<String> message = createMessage("machines/M003/telemetry", payload);
        
        assertDoesNotThrow(() -> listener.processMessage(message));
        
        verify(telemetryService, never()).ingestTelemetryByMachineCode(any(), any());
    }

    @Test
    void shouldNotCrashWhenMachineNotFound() {
        String payload = """
                {
                  "timestamp": "2026-09-06T12:30:00Z",
                  "vibration": 2.35,
                  "temperature": 48.7,
                  "current": 5.2,
                  "rpm": 1450.0
                }
                """;
        Message<String> message = createMessage("machines/UNKNOWN/telemetry", payload);
        
        doThrow(new ResourceNotFoundException("Not found"))
                .when(telemetryService).ingestTelemetryByMachineCode(eq("UNKNOWN"), any());
        
        assertDoesNotThrow(() -> listener.processMessage(message));
        
        verify(telemetryService).ingestTelemetryByMachineCode(eq("UNKNOWN"), any());
    }

    private Message<String> createMessage(String topic, String payload) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("mqtt_receivedTopic", topic);
        return new GenericMessage<>(payload, headers);
    }
}
