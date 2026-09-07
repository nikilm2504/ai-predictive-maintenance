import pytest
from src.inference.health_engine import calculate_health_score, calculate_sensor_deviations
from src.config import Z_SCORE_EPSILON, ADAPTIVE_THRESHOLD_K

def test_sensor_deviations():
    features = {
        "vibration_rms": 2.0,
        "temperature_mean": 45.0,
        "current_mean": 6.0,
        "rpm_mean": 1450.0
    }
    
    baseline = {
        "vibration_rms": {"mean": 1.0, "std": 0.5},
        "temperature_mean": {"mean": 40.0, "std": 2.5},
        "current_mean": {"mean": 5.0, "std": 0.0}, # Zero std test
        "rpm_mean": {"mean": 1400.0, "std": 25.0}
    }
    
    devs = calculate_sensor_deviations(features, baseline)
    
    # vibration: (2.0 - 1.0) / 0.5 = 2.0
    assert devs["vibration_rms"] == 2.0
    # temperature: (45.0 - 40.0) / 2.5 = 2.0
    assert devs["temperature_mean"] == 2.0
    # current: (6.0 - 5.0) / Z_SCORE_EPSILON
    assert devs["current_mean"] == 1.0 / Z_SCORE_EPSILON
    # rpm: (1450 - 1400) / 25 = 2.0
    assert devs["rpm_mean"] == 2.0

def test_health_score_healthy():
    features = {
        "vibration_rms": 1.1,
        "temperature_mean": 40.5,
        "current_mean": 5.1,
        "rpm_mean": 1410.0
    }
    baseline = {
        "vibration_rms": {"mean": 1.0, "std": 0.5},
        "temperature_mean": {"mean": 40.0, "std": 2.5},
        "current_mean": {"mean": 5.0, "std": 0.5},
        "rpm_mean": {"mean": 1400.0, "std": 25.0}
    }
    
    # Low deviations: Z-scores around 0.2
    # ML probability is 0.1 (10% risk)
    ml_prob = 0.1
    
    score, risk_level, devs = calculate_health_score(features, ml_prob, baseline)
    
    # Expect high health score, >80 -> HEALTHY
    assert score > 80.0
    assert risk_level == "HEALTHY"

def test_health_score_critical():
    features = {
        "vibration_rms": 5.0,  # huge deviation
        "temperature_mean": 60.0, # huge deviation
        "current_mean": 10.0,
        "rpm_mean": 800.0
    }
    baseline = {
        "vibration_rms": {"mean": 1.0, "std": 0.5},
        "temperature_mean": {"mean": 40.0, "std": 2.5},
        "current_mean": {"mean": 5.0, "std": 0.5},
        "rpm_mean": {"mean": 1400.0, "std": 25.0}
    }
    
    # High Z-scores will cap sensor risk at 1.0
    # ML prob is 0.9 (90% risk)
    ml_prob = 0.9
    
    score, risk_level, devs = calculate_health_score(features, ml_prob, baseline)
    
    # Overall risk is 0.5*0.9 + 0.5*1.0 = 0.95 -> Score = 5.0 -> CRITICAL
    assert score == 5.0
    assert risk_level == "CRITICAL"

def test_health_score_no_baseline():
    # If no baseline is available, it should fallback smoothly
    score, risk_level, devs = calculate_health_score({"vibration_rms": 2.0}, 0.6, None)
    
    # Fallback logic says sensor_risk = ml_prob. 
    # overall_risk = 0.5*0.6 + 0.5*0.6 = 0.6 -> Score 40.0 -> MEDIUM
    assert score == 40.0
    assert risk_level == "MEDIUM"
    assert len(devs) == 0
