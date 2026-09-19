import pandas as pd
from src.config import VIBRATION_SAMPLING_FREQ
import src.features.statistical as stat
import src.features.fft as fft_mod

def extract_features(window: pd.DataFrame) -> dict:
    """
    Extracts a machine-learning-ready feature vector from a telemetry window.
    
    :param window: A pandas DataFrame containing one window of telemetry.
                   Expected columns: timestamp, vibration, temperature, current, water_flow.
    :return: A dictionary containing the extracted features.
    """
    features = {}
    
    # ---------------------------------------------------------
    # Vibration Features
    # ---------------------------------------------------------
    if 'vibration' in window.columns:
        vib = window['vibration'].values
        features['vibration_mean'] = stat.calc_mean(vib)
        features['vibration_std'] = stat.calc_std(vib)
        features['vibration_rms'] = stat.calc_rms(vib)
        features['vibration_peak'] = stat.calc_peak(vib)
        features['vibration_peak_to_peak'] = stat.calc_peak_to_peak(vib)
        features['vibration_variance'] = stat.calc_variance(vib)
        features['vibration_kurtosis'] = stat.calc_kurtosis(vib)
        features['vibration_skewness'] = stat.calc_skewness(vib)
        features['vibration_crest_factor'] = stat.calc_crest_factor(vib)
        
        dom_freq, dom_amp = fft_mod.extract_fft_features(vib, fs=VIBRATION_SAMPLING_FREQ)
        features['vibration_dominant_frequency'] = dom_freq
        features['vibration_dominant_frequency_amplitude'] = dom_amp

    # ---------------------------------------------------------
    # Temperature Features
    # ---------------------------------------------------------
    if 'temperature' in window.columns:
        temp = window['temperature'].values
        features['temperature_mean'] = stat.calc_mean(temp)
        features['temperature_std'] = stat.calc_std(temp)
        features['temperature_min'] = stat.calc_min(temp)
        features['temperature_max'] = stat.calc_max(temp)
        features['temperature_range'] = stat.calc_range(temp)
        features['temperature_trend'] = stat.calc_slope(temp)

    # ---------------------------------------------------------
    # Current Features
    # ---------------------------------------------------------
    if 'current' in window.columns:
        curr = window['current'].values
        features['current_mean'] = stat.calc_mean(curr)
        features['current_std'] = stat.calc_std(curr)
        features['current_min'] = stat.calc_min(curr)
        features['current_max'] = stat.calc_max(curr)
        features['current_rms'] = stat.calc_rms(curr)
        features['current_range'] = stat.calc_range(curr)

    # ---------------------------------------------------------
    # Water Flow Features
    # ---------------------------------------------------------
    if 'water_flow' in window.columns:
        wf_vals = window['water_flow'].values
        features['water_flow_mean'] = stat.calc_mean(wf_vals)
        features['water_flow_std'] = stat.calc_std(wf_vals)
        features['water_flow_min'] = stat.calc_min(wf_vals)
        features['water_flow_max'] = stat.calc_max(wf_vals)
        features['water_flow_range'] = stat.calc_range(wf_vals)

    return features
