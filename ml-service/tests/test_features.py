import pytest
import numpy as np
import pandas as pd
from datetime import datetime, timedelta

from src.features import statistical as stat
from src.features import fft as fft_mod
from src.features import extractor
from src.data.windowing import prepare_telemetry_data, segment_into_windows
from src.pipeline.feature_pipeline import process_telemetry

# ---------------------------------------------------------
# Test Statistical Features
# ---------------------------------------------------------
def test_statistical_features():
    signal = np.array([1.0, 2.0, 3.0, 4.0, 5.0])
    
    assert stat.calc_mean(signal) == 3.0
    assert stat.calc_min(signal) == 1.0
    assert stat.calc_max(signal) == 5.0
    assert stat.calc_median(signal) == 3.0
    assert stat.calc_range(signal) == 4.0
    assert np.isclose(stat.calc_rms(signal), 3.3166, atol=1e-4)
    assert stat.calc_peak(signal) == 5.0
    assert stat.calc_peak_to_peak(signal) == 4.0
    
    # Crest factor = peak / rms = 5.0 / 3.3166 = 1.5075
    assert np.isclose(stat.calc_crest_factor(signal), 1.5075, atol=1e-4)
    
def test_trend_slope():
    # Line steadily increasing
    signal_up = np.array([10, 20, 30, 40, 50])
    # x = [0, 1, 2, 3, 4], slope should be 10.0
    assert np.isclose(stat.calc_slope(signal_up), 10.0)

# ---------------------------------------------------------
# Test FFT Features
# ---------------------------------------------------------
def test_fft_features_sine_wave():
    """
    Test FFT extraction using a known sine wave.
    A sine wave with known frequency should produce a dominant frequency
    close to the expected frequency, subject to FFT resolution.
    """
    fs = 100.0  # 100 Hz sampling frequency
    f0 = 15.0   # 15 Hz signal
    
    # Create 1 second of data (100 samples)
    t = np.arange(100) / fs
    # Add a DC component of 5.0
    signal = 5.0 + 2.0 * np.sin(2 * np.pi * f0 * t)
    
    dom_freq, dom_amp = fft_mod.extract_fft_features(signal, fs=fs)
    
    # The dominant frequency should exactly match 15.0 Hz due to integer periods
    assert np.isclose(dom_freq, 15.0)
    # The amplitude (after normalization in extract_fft_features) should be half of peak amplitude (2.0/2 = 1.0)
    assert np.isclose(dom_amp, 1.0)

def test_fft_empty_signal():
    dom_freq, dom_amp = fft_mod.extract_fft_features(np.array([]), fs=100.0)
    assert dom_freq == 0.0
    assert dom_amp == 0.0

# ---------------------------------------------------------
# Test Data Windowing
# ---------------------------------------------------------
def test_windowing():
    df = pd.DataFrame({
        'timestamp': [datetime.now() - timedelta(seconds=i) for i in range(10, 0, -1)],
        'vibration': [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    })
    
    cleaned_df = prepare_telemetry_data(df)
    # Ensure chronological order
    assert cleaned_df['vibration'].tolist() == [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    
    windows = segment_into_windows(cleaned_df, window_size=4)
    assert len(windows) == 2  # 10 records, size 4 -> 2 full windows, 2 remainder dropped
    assert windows[0]['vibration'].tolist() == [1, 2, 3, 4]
    assert windows[1]['vibration'].tolist() == [5, 6, 7, 8]

def test_missing_values_handled():
    df = pd.DataFrame({
        'timestamp': [1, 2, 3, 4],
        'vibration': [1.0, np.nan, np.nan, 4.0]
    })
    cleaned_df = prepare_telemetry_data(df)
    # Forward fill should propagate 1.0 to index 1 and 2
    assert cleaned_df['vibration'].tolist() == [1.0, 1.0, 1.0, 4.0]

# ---------------------------------------------------------
# Test Complete Pipeline
# ---------------------------------------------------------
def test_feature_pipeline():
    # Create 25 rows of data. With WINDOW_SIZE=10, this should yield 2 windows.
    # We will override WINDOW_SIZE via monkeypatch for this test to be self-contained.
    pass

def test_pipeline_output(monkeypatch):
    import src.config
    monkeypatch.setattr(src.config, "WINDOW_SIZE", 10)
    
    # 20 samples
    data = []
    base_time = datetime(2026, 9, 6, 12, 0, 0)
    for i in range(20):
        data.append({
            'timestamp': base_time + timedelta(seconds=i),
            'vibration': 1.0 + (i % 2), # Alternates 1.0, 2.0
            'temperature': 40.0 + i,    # Trending up
            'current': 5.0,
            'rpm': 1400.0
        })
        
    df = pd.DataFrame(data)
    feature_dataset = process_telemetry(df)
    
    assert len(feature_dataset) == 2
    assert 'window_id' in feature_dataset.columns
    assert feature_dataset.iloc[0]['window_id'] == 'W000'
    assert feature_dataset.iloc[1]['window_id'] == 'W001'
    
    # Verify some features
    assert np.isclose(feature_dataset.iloc[0]['temperature_trend'], 1.0)  # steadily increasing by 1
    assert feature_dataset.iloc[0]['current_mean'] == 5.0
    assert feature_dataset.iloc[0]['vibration_mean'] == 1.5
