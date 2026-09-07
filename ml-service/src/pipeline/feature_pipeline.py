import pandas as pd
from src.config import WINDOW_SIZE
from src.data.windowing import prepare_telemetry_data, segment_into_windows
from src.features.extractor import extract_features

def process_telemetry(df: pd.DataFrame) -> pd.DataFrame:
    """
    Main entry point for processing a continuous stream of telemetry into a feature dataset.
    
    Target Pipeline:
    Raw telemetry -> Data validation -> Time-based window -> Feature extraction -> Feature vector
    
    :param df: Pandas DataFrame containing raw telemetry data for a single machine.
    :return: Pandas DataFrame containing the extracted feature vectors, ready for ML.
    """
    if df.empty:
        return pd.DataFrame()

    # 1. Prepare and clean data
    cleaned_df = prepare_telemetry_data(df)
    
    # 2. Segment into windows
    windows = segment_into_windows(cleaned_df, WINDOW_SIZE)
    
    # 3. Extract features for each window
    feature_vectors = []
    for i, window in enumerate(windows):
        features = extract_features(window)
        # Add a synthetic window identifier for dataset traceability
        features['window_id'] = f"W{i:03d}"
        feature_vectors.append(features)
        
    # 4. Construct feature dataset
    if not feature_vectors:
        return pd.DataFrame()
        
    # Move window_id to the front
    dataset = pd.DataFrame(feature_vectors)
    cols = ['window_id'] + [c for c in dataset.columns if c != 'window_id']
    dataset = dataset[cols]
    
    return dataset
