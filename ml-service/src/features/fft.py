import numpy as np
from scipy.fft import fft, fftfreq

def extract_fft_features(signal, fs=100.0):
    """
    Extracts frequency-domain features from a vibration signal.
    
    FFT does not itself predict failure. It transforms the time-domain signal
    into a frequency-domain representation. These features (like dominant frequency)
    can later be used as inputs to a machine learning model.
    
    :param signal: numpy array or pandas Series of vibration values.
    :param fs: Sampling frequency in Hz.
    :return: Tuple of (dominant_frequency, dominant_amplitude).
    """
    if len(signal) == 0:
        return 0.0, 0.0
        
    # Remove DC component (0 Hz) by subtracting the mean
    signal_zero_mean = signal - np.mean(signal)
    n = len(signal_zero_mean)
    
    # Calculate FFT
    yf = fft(signal_zero_mean)
    xf = fftfreq(n, 1/fs)
    
    # Use only positive frequencies
    idx = np.where(xf > 0)
    xf_pos = xf[idx]
    
    # Normalize magnitude
    magnitudes = np.abs(yf[idx]) / n
    
    if len(magnitudes) == 0:
        return 0.0, 0.0
        
    peak_idx = np.argmax(magnitudes)
    dominant_frequency = float(xf_pos[peak_idx])
    dominant_amplitude = float(magnitudes[peak_idx])
    
    return dominant_frequency, dominant_amplitude
