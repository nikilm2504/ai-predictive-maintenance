import os
import json
import pandas as pd
import numpy as np

BASELINES_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "models", "baselines")

def calculate_baseline(df: pd.DataFrame) -> dict:
    """
    Calculates the historical mean and standard deviation for all numeric features
    in a DataFrame of NORMAL machine behavior.
    """
    baseline = {}
    
    # Filter only numeric columns, dropping labels and IDs
    numeric_df = df.select_dtypes(include=[np.number])
    exclude_cols = ['label']
    
    for col in numeric_df.columns:
        if col in exclude_cols:
            continue
            
        baseline[col] = {
            "mean": float(numeric_df[col].mean()),
            "std": float(numeric_df[col].std())
        }
        
    return baseline

def save_baseline(machine_id: str, baseline_data: dict):
    """
    Persists the machine's baseline to disk as JSON.
    """
    os.makedirs(BASELINES_DIR, exist_ok=True)
    file_path = os.path.join(BASELINES_DIR, f"machine_{machine_id}_baseline.json")
    with open(file_path, "w") as f:
        json.dump(baseline_data, f, indent=2)

def load_baseline(machine_id: str) -> dict:
    """
    Loads a machine's baseline from disk. Returns None if not found.
    """
    if not machine_id:
        return None
        
    file_path = os.path.join(BASELINES_DIR, f"machine_{machine_id}_baseline.json")
    if not os.path.exists(file_path):
        return None
        
    with open(file_path, "r") as f:
        return json.load(f)
