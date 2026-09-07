import os
import random
import pandas as pd
from datetime import datetime, timedelta
from src.pipeline.feature_pipeline import process_telemetry

def generate_machine_telemetry(machine_id: str, mode: str, num_records: int, start_time: datetime, random_seed: int):
    """
    Generates synthetic telemetry for a specific machine.
    mode: 'NORMAL' or 'DEGRADING'
    """
    random.seed(random_seed)
    data = []
    
    # Base values
    vib_base = 1.0
    temp_base = 40.0
    curr_base = 5.0
    rpm_base = 1400.0
    
    for i in range(num_records):
        if mode == 'DEGRADING':
            # Degrade over time
            vib_base += 0.05
            temp_base += 0.1
            curr_base += 0.02
        
        # Add noise
        vib = max(0.0, vib_base + random.uniform(-0.5, 0.5))
        temp = temp_base + random.uniform(-2.0, 2.0)
        curr = curr_base + random.uniform(-0.5, 0.5)
        rpm = rpm_base + random.uniform(-50.0, 50.0)
        
        data.append({
            'timestamp': start_time + timedelta(seconds=i),
            'vibration': vib,
            'temperature': temp,
            'current': curr,
            'rpm': rpm
        })
        
    df = pd.DataFrame(data)
    
    # Extract features using Milestone 6 pipeline
    features_df = process_telemetry(df)
    
    # Assign metadata and label
    if not features_df.empty:
        features_df['machine_id'] = machine_id
        features_df['label'] = 1 if mode == 'DEGRADING' else 0
        
    return features_df

def generate_dataset(output_path: str):
    """
    Generates a deterministic synthetic dataset of machine telemetry features.
    Simulates 8 NORMAL machines and 4 DEGRADING machines.
    """
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    machines = [
        ("M001", "NORMAL", 100, 42),
        ("M002", "NORMAL", 100, 43),
        ("M003", "NORMAL", 100, 44),
        ("M004", "NORMAL", 100, 45),
        ("M005", "NORMAL", 100, 46),
        ("M006", "NORMAL", 100, 47),
        ("M007", "NORMAL", 100, 48),
        ("M008", "NORMAL", 100, 49),
        ("M009", "DEGRADING", 100, 50),
        ("M010", "DEGRADING", 100, 51),
        ("M011", "DEGRADING", 100, 52),
        ("M012", "DEGRADING", 100, 53),
    ]
    
    start_time = datetime(2026, 9, 6, 12, 0, 0)
    all_features = []
    
    for m_id, mode, records, seed in machines:
        df = generate_machine_telemetry(m_id, mode, records, start_time, seed)
        all_features.append(df)
        
    final_df = pd.concat(all_features, ignore_index=True)
    final_df.to_csv(output_path, index=False)
    print(f"Dataset generated at {output_path} with {len(final_df)} samples.")
    print(f"Label distribution:\n{final_df['label'].value_counts()}")

if __name__ == "__main__":
    out_path = os.path.join(os.path.dirname(__file__), "..", "..", "data", "processed", "synthetic_training_data.csv")
    generate_dataset(out_path)
