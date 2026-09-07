# Data processing configurations
WINDOW_SIZE = 10  # Number of samples per telemetry window
VIBRATION_SAMPLING_FREQ = 100.0  # Hz - Assumed physical sampling frequency for FFT features

# Note: The VIBRATION_SAMPLING_FREQ is a configuration parameter.
# The MQTT publishing interval (e.g., 2s) is often much slower than the internal 
# hardware vibration sampling rate. FFT assumes this configured rate.
