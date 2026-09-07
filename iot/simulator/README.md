# IoT Telemetry Simulator

A simple Python-based simulator representing an industrial machine emitting telemetry to an MQTT broker.

## Setup

1. Install Python 3.
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

## Usage

Run the simulator:
```bash
python simulator.py
```

### Configuration

You can override defaults using environment variables:

- `MQTT_HOST`: The MQTT broker address (default: `localhost`).
- `MQTT_PORT`: The MQTT broker port (default: `1883`).
- `MACHINE_CODE`: The machine identifier injected into the topic (default: `M001`).
- `PUBLISH_INTERVAL`: Delay in seconds between publishes (default: `2.0`).
- `MODE`: Operating mode, either `NORMAL` or `DEGRADING` (default: `NORMAL`).

### Example

To simulate a degrading machine emitting telemetry every second:

```bash
MACHINE_CODE="M001" MODE="DEGRADING" PUBLISH_INTERVAL="1.0" python simulator.py
```
