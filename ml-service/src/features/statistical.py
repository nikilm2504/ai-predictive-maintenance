import numpy as np
from scipy.stats import kurtosis, skew

def calc_mean(signal):
    return float(np.mean(signal))

def calc_std(signal):
    return float(np.std(signal))

def calc_min(signal):
    return float(np.min(signal))

def calc_max(signal):
    return float(np.max(signal))

def calc_median(signal):
    return float(np.median(signal))

def calc_range(signal):
    return float(np.max(signal) - np.min(signal))

def calc_rms(signal):
    return float(np.sqrt(np.mean(np.square(signal))))

def calc_peak(signal):
    return float(np.max(np.abs(signal)))

def calc_peak_to_peak(signal):
    return calc_range(signal)

def calc_variance(signal):
    return float(np.var(signal))

def calc_kurtosis(signal):
    return float(kurtosis(signal))

def calc_skewness(signal):
    return float(skew(signal))

def calc_crest_factor(signal):
    rms = calc_rms(signal)
    if rms == 0:
        return 0.0
    return float(calc_peak(signal) / rms)

def calc_slope(signal):
    """
    Calculates a simple linear trend (slope) over the window.
    Useful for identifying gradual increases, like temperature degradation.
    """
    if len(signal) < 2:
        return 0.0
    x = np.arange(len(signal))
    poly = np.polyfit(x, signal, 1)
    return float(poly[0])
