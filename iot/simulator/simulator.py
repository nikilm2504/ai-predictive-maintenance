import os
import json
import time
import random
from datetime import datetime, timezone
import paho.mqtt.client as mqtt

# Configuration
MQTT_HOST = os.getenv("MQTT_HOST", "localhost")
MQTT_PORT = int(os.getenv("MQTT_PORT", 1883))
MACHINE_CODE = os.getenv("MACHINE_CODE", "M001")
PUBLISH_INTERVAL = float(os.getenv("PUBLISH_INTERVAL", "2.0"))
MODE = os.getenv("MODE", "NORMAL").upper()

# Base simulation state
state = {
    "vibration_base": 1.0,
    "temperature_base": 40.0,
    "current_base": 5.0,
    "rpm_base": 1400.0,
    "ticks": 0
}

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print(f"Connected to MQTT Broker at {MQTT_HOST}:{MQTT_PORT}")
    else:
        print(f"Failed to connect, return code {rc}")

def generate_telemetry():
    """Generates a telemetry payload based on the current mode."""
    state["ticks"] += 1
    
    if MODE == "DEGRADING":
        # Gradually increase baseline values over time
        state["vibration_base"] += 0.05
        state["temperature_base"] += 0.1
        state["current_base"] += 0.02

    vibration = state["vibration_base"] + random.uniform(-0.5, 0.5)
    temperature = state["temperature_base"] + random.uniform(-2.0, 2.0)
    current = state["current_base"] + random.uniform(-0.5, 0.5)
    rpm = state["rpm_base"] + random.uniform(-50.0, 50.0)

    # Ensure no negative values where physically impossible
    vibration = max(0.0, vibration)
    
    return {
        "timestamp": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "vibration": round(vibration, 3),
        "temperature": round(temperature, 2),
        "current": round(current, 2),
        "rpm": round(rpm, 1)
    }

def main():
    print(f"Starting IoT Simulator for Machine: {MACHINE_CODE}")
    print(f"Mode: {MODE}, Interval: {PUBLISH_INTERVAL}s")
    
    client = mqtt.Client(client_id=f"simulator-{MACHINE_CODE}-{random.randint(1000,9999)}")
    client.on_connect = on_connect

    try:
        client.connect(MQTT_HOST, MQTT_PORT, 60)
    except Exception as e:
        print(f"Error connecting to MQTT Broker: {e}")
        return

    client.loop_start()

    topic = f"machines/{MACHINE_CODE}/telemetry"

    try:
        while True:
            payload = generate_telemetry()
            payload_json = json.dumps(payload)
            
            # QoS 1, retain False
            result = client.publish(topic, payload_json, qos=1, retain=False)
            
            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                print(f"Published to {topic}: {payload_json}")
            else:
                print(f"Failed to publish message, return code: {result.rc}")
                
            time.sleep(PUBLISH_INTERVAL)
            
    except KeyboardInterrupt:
        print("Stopping simulator...")
    finally:
        client.loop_stop()
        client.disconnect()

if __name__ == "__main__":
    main()
