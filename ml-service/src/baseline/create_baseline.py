import os
import pandas as pd
from src.baseline.baseline_manager import calculate_baseline, save_baseline

def generate_all_baselines():
    """
    Reads the synthetic training dataset, filters for NORMAL data (label == 0),
    calculates machine-specific baselines, and persists them.
    """
    base_dir = os.path.join(os.path.dirname(__file__), "..", "..")
    data_file = os.path.join(base_dir, "data", "processed", "synthetic_training_data.csv")
    
    if not os.path.exists(data_file):
        print(f"Error: Training dataset not found at {data_file}")
        return
        
    df = pd.read_csv(data_file)
    
    if 'machine_id' not in df.columns or 'label' not in df.columns:
        print("Error: Dataset is missing 'machine_id' or 'label' columns.")
        return
        
    # Only use NORMAL data (label == 0) to establish the baseline!
    normal_df = df[df['label'] == 0]
    
    machines = normal_df['machine_id'].unique()
    print(f"Found {len(machines)} machines with NORMAL historical data.")
    
    for m_id in machines:
        m_df = normal_df[normal_df['machine_id'] == m_id]
        baseline = calculate_baseline(m_df)
        save_baseline(m_id, baseline)
        
        print(f"--- Baseline for {m_id} ---")
        print(f"  vibration_rms mean: {baseline.get('vibration_rms', {}).get('mean', 0):.4f}")
        print(f"  temperature_mean mean: {baseline.get('temperature_mean', {}).get('mean', 0):.4f}")
        print(f"  (Persisted to machine_{m_id}_baseline.json)\n")
        
    print("All machine baselines successfully initialized.")

if __name__ == "__main__":
    generate_all_baselines()
