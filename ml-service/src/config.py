# Data processing configurations
WINDOW_SIZE = 10  # Number of samples per telemetry window
VIBRATION_SAMPLING_FREQ = 100.0  # Hz - Assumed physical sampling frequency for FFT features

# Note: The VIBRATION_SAMPLING_FREQ is a configuration parameter.
# The MQTT publishing interval (e.g., 2s) is often much slower than the internal 
# hardware vibration sampling rate. FFT assumes this configured rate.
# Health Score & Adaptive Threshold Configurations
HEALTH_ML_WEIGHT = 0.5
HEALTH_SENSOR_WEIGHT = 0.5
ADAPTIVE_THRESHOLD_K = 3.0  # Z-score at which sensor risk is considered 100%
Z_SCORE_EPSILON = 1e-6      # Prevents division by zero for completely static signals

SENSOR_WEIGHTS = {
    "vibration": 0.40,
    "temperature": 0.30,
    "current": 0.15,
    "rpm": 0.15
}

# Features selected to represent each sensor for the health baseline calculation
PRIMARY_BASELINE_FEATURES = {
    "vibration": "vibration_rms",
    "temperature": "temperature_mean",
    "current": "current_mean",
    "rpm": "rpm_mean"
}

# Explainability Configurations
TOP_SHAP_FEATURES = 5
