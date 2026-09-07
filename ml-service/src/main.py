import pandas as pd
from datetime import datetime, timedelta
from src.pipeline.feature_pipeline import process_telemetry

def generate_dummy_telemetry(num_records=50):
    """Generates synthetic telemetry data for local testing."""
    data = []
    base_time = datetime.now()
    for i in range(num_records):
        data.append({
            'timestamp': base_time + timedelta(seconds=i),
            'vibration': 1.0 + (i % 3) * 0.5,
            'temperature': 40.0 + (i * 0.1),
            'current': 5.0 + (i % 2) * 0.2,
            'rpm': 1400.0 + (i % 5) * 10
        })
    return pd.DataFrame(data)

if __name__ == "__main__":
    print("Generating dummy telemetry data...")
    df = generate_dummy_telemetry(25)  # 25 samples
    
    print(f"Raw data rows: {len(df)}")
    
    print("Processing telemetry into feature vectors...")
    feature_dataset = process_telemetry(df)
    
    print("\nGenerated Feature Vectors:")
    print(feature_dataset.to_string())
    print(f"\nFeature extraction complete. Total windows: {len(feature_dataset)}")
