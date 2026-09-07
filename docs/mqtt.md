# MQTT Integration & Simulator

This document describes the MQTT infrastructure used for ingesting telemetry from IoT devices (and the telemetry simulator).

## 1. MQTT Broker
- **Broker**: Eclipse Mosquitto (v2.0+)
- **Deployment**: Docker Compose (`docker-compose.yml`)
- **Port**: `1883` (TCP)
- **Authentication**: Anonymous (for local development only)

## 2. Topic Structure
Telemetry messages must be published to a specific topic pattern:
`machines/{machineCode}/telemetry`

Example: `machines/M001/telemetry`

This decouples the message payload from the routing logic and avoids exposing internal backend UUIDs.

## 3. Message Format
Payloads must be formatted as JSON. The backend expects the following fields:

```json
{
  "timestamp": "2026-09-06T12:30:00Z",
  "vibration": 2.35,
  "temperature": 48.7,
  "current": 5.2,
  "rpm": 1450
}
```

- **timestamp**: ISO-8601 formatted UTC time.
- **Sensor values**: Numeric (finite) values representing physical measurements. 
- *Note: `machineId` and `machineCode` are explicitly excluded from the payload.*

## 4. QoS and Retention
- **Quality of Service (QoS)**: `1` (At least once delivery). Telemetry is important and should not be silently dropped if the connection briefly drops. Note that this allows duplicate delivery; complex deduplication logic is out of scope for the current milestone.
- **Retention**: `False`. Telemetry represents point-in-time events. Retained messages are not appropriate for time-series readings.

## 5. Backend Consumer
The Spring Boot backend uses `spring-integration-mqtt`. 
It subscribes to `machines/+/telemetry`.

The message flow is:
`MQTT Listener -> Extract machineCode -> Parse JSON -> Verify Machine -> TelemetryService -> PostgreSQL`

*Note: Malformed messages or messages for unknown machines are gracefully logged and ignored. The listener will not crash.*

## 6. Simulator
A Python-based IoT simulator is provided in `iot/simulator`.

### Setup
```bash
cd iot/simulator
pip install -r requirements.txt
```

### Running
```bash
python simulator.py
```

### Modes
- `NORMAL`: Emits stable readings simulating an optimal machine.
- `DEGRADING`: Simulates an impending fault by gradually increasing temperature, current, and vibration over time. Useful for ML validation in later milestones.

### Configuration
Environment variables available:
- `MQTT_HOST` (default: localhost)
- `MQTT_PORT` (default: 1883)
- `MACHINE_CODE` (default: M001)
- `PUBLISH_INTERVAL` (default: 2.0)
- `MODE` (default: NORMAL)

## Known Limitations
1. **Duplicate Delivery**: Using QoS 1 may yield duplicate telemetry records if a PUBACK is lost. Idempotency is not yet fully enforced.
2. **Security**: Mosquitto allows anonymous connections. Future production deployments will require TLS and authenticated connections.
