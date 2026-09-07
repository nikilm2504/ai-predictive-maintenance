package com.pmp.predictivemaintenance.telemetry.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmp.predictivemaintenance.common.exception.ResourceNotFoundException;
import com.pmp.predictivemaintenance.telemetry.dto.TelemetryIngestRequest;
import com.pmp.predictivemaintenance.telemetry.service.TelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MqttTelemetryListener {

    private static final Logger log = LoggerFactory.getLogger(MqttTelemetryListener.class);
    
    // matches machines/{machineCode}/telemetry
    private static final Pattern TOPIC_PATTERN = Pattern.compile("^machines/([^/]+)/telemetry$");

    private final ObjectMapper objectMapper;
    private final TelemetryService telemetryService;

    public MqttTelemetryListener(ObjectMapper objectMapper, TelemetryService telemetryService) {
        this.objectMapper = objectMapper;
        this.telemetryService = telemetryService;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void processMessage(Message<String> message) {
        String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
        String payload = message.getPayload();

        if (topic == null) {
            log.warn("Received MQTT message without a topic header. Ignoring.");
            return;
        }

        Matcher matcher = TOPIC_PATTERN.matcher(topic);
        if (!matcher.matches()) {
            log.warn("Received MQTT message on unknown topic structure: {}. Ignoring.", topic);
            return;
        }

        String machineCode = matcher.group(1);

        try {
            MqttTelemetryMessage mqttMsg = objectMapper.readValue(payload, MqttTelemetryMessage.class);
            validatePayload(mqttMsg);

            TelemetryIngestRequest request = new TelemetryIngestRequest(
                    mqttMsg.timestamp(),
                    mqttMsg.vibration(),
                    mqttMsg.temperature(),
                    mqttMsg.current(),
                    mqttMsg.rpm()
            );

            telemetryService.ingestTelemetryByMachineCode(machineCode, request);
            log.debug("Successfully ingested MQTT telemetry for machine: {}", machineCode);

        } catch (ResourceNotFoundException e) {
            log.warn("Machine not found for code '{}'. Ignoring telemetry.", machineCode);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid telemetry data for machine '{}': {}. Ignoring.", machineCode, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process MQTT message on topic '{}'. Payload: {}. Error: {}", topic, payload, e.getMessage());
        }
    }

    private void validatePayload(MqttTelemetryMessage msg) {
        if (msg.timestamp() == null || msg.vibration() == null || msg.temperature() == null ||
            msg.current() == null || msg.rpm() == null) {
            throw new IllegalArgumentException("Missing required telemetry fields in JSON payload");
        }
    }
}
