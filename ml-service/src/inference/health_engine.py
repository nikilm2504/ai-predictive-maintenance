from typing import Tuple
from src.config import (
    HEALTH_ML_WEIGHT,
    HEALTH_SENSOR_WEIGHT,
    ADAPTIVE_THRESHOLD_K,
    Z_SCORE_EPSILON,
    SENSOR_WEIGHTS,
    PRIMARY_BASELINE_FEATURES
)

def calculate_sensor_deviations(features: dict, baseline: dict) -> dict:
    """
    Calculates the normalized deviation (Z-score) for primary sensor features.
    """
    deviations = {}
    if not baseline:
        return deviations
        
    for sensor, feat_name in PRIMARY_BASELINE_FEATURES.items():
        if feat_name in features and feat_name in baseline:
            val = features[feat_name]
            mean = baseline[feat_name]["mean"]
            std = baseline[feat_name]["std"]
            
            # Prevent division by zero
            if std < Z_SCORE_EPSILON:
                std = Z_SCORE_EPSILON
                
            z_score = abs(val - mean) / std
            deviations[feat_name] = z_score
            
    return deviations

def calculate_health_score(features: dict, ml_probability: float, baseline: dict) -> Tuple[float, str, dict]:
    """
    Calculates the overall machine health score (0-100) and risk level.
    Fuses the ML failure probability with adaptive sensor deviations.
    """
    sensor_risk_total = 0.0
    deviations = calculate_sensor_deviations(features, baseline)
    
    if baseline and deviations:
        for sensor, weight in SENSOR_WEIGHTS.items():
            feat_name = PRIMARY_BASELINE_FEATURES[sensor]
            if feat_name in deviations:
                z_score = deviations[feat_name]
                # Sensor risk is capped at 1.0 (100% risk) when Z-score reaches ADAPTIVE_THRESHOLD_K
                risk = min(1.0, z_score / ADAPTIVE_THRESHOLD_K)
                sensor_risk_total += risk * weight
    else:
        # If no baseline is available, the sensor risk defaults to the ML probability
        # to prevent artificially inflating the health score.
        sensor_risk_total = ml_probability

    # Combined risk (e.g. 50% ML, 50% Sensors)
    overall_risk = (HEALTH_ML_WEIGHT * ml_probability) + (HEALTH_SENSOR_WEIGHT * sensor_risk_total)
    
    # Map risk to 0-100 health score
    health_score = max(0.0, min(100.0, 100.0 * (1.0 - overall_risk)))
    health_score = round(health_score, 2)
    
    # Risk Level mapping
    if health_score >= 80:
        risk_level = "HEALTHY"
    elif health_score >= 60:
        risk_level = "LOW"
    elif health_score >= 40:
        risk_level = "MEDIUM"
    elif health_score >= 20:
        risk_level = "HIGH"
    else:
        risk_level = "CRITICAL"
        
    return health_score, risk_level, deviations
