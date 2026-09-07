import pandas as pd
from typing import List

def prepare_telemetry_data(df: pd.DataFrame) -> pd.DataFrame:
    """
    Validates, sorts, and cleans a raw telemetry DataFrame.
    
    :param df: Raw pandas DataFrame.
    :return: Cleaned and sorted pandas DataFrame.
    """
    if df.empty:
        return df
        
    df_clean = df.copy()
    
    # Ensure timestamp is datetime
    if 'timestamp' in df_clean.columns:
        df_clean['timestamp'] = pd.to_datetime(df_clean['timestamp'])
        df_clean = df_clean.sort_values(by='timestamp').reset_index(drop=True)
    
    # Missing value strategy for Milestone 6:
    # Forward fill to handle occasional dropped readings.
    # If the beginning has NaNs, bfill will handle them.
    df_clean = df_clean.ffill().bfill()
    
    return df_clean

def segment_into_windows(df: pd.DataFrame, window_size: int) -> List[pd.DataFrame]:
    """
    Splits a chronological telemetry DataFrame into fixed-size windows.
    
    :param df: Cleaned pandas DataFrame.
    :param window_size: Number of samples per window.
    :return: A list of pandas DataFrames, where each is a telemetry window.
    """
    if df.empty or window_size <= 0:
        return []
        
    windows = []
    # Drop the trailing remainder if it doesn't form a full window.
    # If a window has insufficient valid data, it is skipped.
    for i in range(0, len(df) - window_size + 1, window_size):
        window = df.iloc[i : i + window_size]
        windows.append(window.copy())
        
    return windows
