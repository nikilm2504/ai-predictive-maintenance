import os
import json
import joblib
import pandas as pd
from sklearn.model_selection import GroupShuffleSplit
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, roc_auc_score, confusion_matrix

def train_and_evaluate(data_path: str, model_dir: str):
    print("Loading dataset...")
    df = pd.read_csv(data_path)
    
    # Exclude metadata from features
    drop_cols = ['window_id', 'machine_id', 'label']
    feature_cols = [c for c in df.columns if c not in drop_cols]
    
    X = df[feature_cols]
    y = df['label']
    groups = df['machine_id']
    
    print("Splitting data (Machine-aware to prevent data leakage)...")
    # GroupShuffleSplit ensures windows from the same machine don't leak across train/test
    gss = GroupShuffleSplit(n_splits=1, test_size=0.25, random_state=42)
    train_idx, test_idx = next(gss.split(X, y, groups))
    
    X_train, X_test = X.iloc[train_idx], X.iloc[test_idx]
    y_train, y_test = y.iloc[train_idx], y.iloc[test_idx]
    
    print(f"Training samples: {len(X_train)}, Testing samples: {len(X_test)}")
    
    print("Training Random Forest Classifier...")
    # Using class_weight='balanced' to handle class imbalance (80 Normal / 40 Degrading)
    clf = RandomForestClassifier(n_estimators=100, class_weight='balanced', random_state=42)
    clf.fit(X_train, y_train)
    
    print("Evaluating model...")
    y_pred = clf.predict(X_test)
    y_prob = clf.predict_proba(X_test)[:, 1] if hasattr(clf, "predict_proba") else None
    
    # Calculate metrics
    acc = accuracy_score(y_test, y_pred)
    prec = precision_score(y_test, y_pred, zero_division=0)
    rec = recall_score(y_test, y_pred, zero_division=0)
    f1 = f1_score(y_test, y_pred, zero_division=0)
    roc_auc = roc_auc_score(y_test, y_prob) if y_prob is not None else 0.0
    cm = confusion_matrix(y_test, y_pred).tolist()
    
    metrics = {
        "accuracy": float(acc),
        "precision": float(prec),
        "recall": float(rec),
        "f1_score": float(f1),
        "roc_auc": float(roc_auc),
        "confusion_matrix": cm
    }
    
    print("\n--- Evaluation Results ---")
    print(f"Accuracy : {acc:.4f}")
    print(f"Precision: {prec:.4f}")
    print(f"Recall   : {rec:.4f}  <-- Crucial for Predictive Maintenance (minimizing False Negatives)")
    print(f"F1 Score : {f1:.4f}")
    print(f"ROC-AUC  : {roc_auc:.4f}")
    print(f"Confusion Matrix:\n{cm}")
    
    # Save model
    os.makedirs(model_dir, exist_ok=True)
    model_path = os.path.join(model_dir, "predictive_maintenance_rf_v1.joblib")
    
    # Save the model and the feature ordering
    model_data = {
        "model": clf,
        "features": feature_cols,
        "version": "v1"
    }
    joblib.dump(model_data, model_path)
    print(f"\nModel and feature metadata persisted to: {model_path}")
    
    # Save metrics
    metrics_path = os.path.join(model_dir, "evaluation_metrics_v1.json")
    with open(metrics_path, "w") as f:
        json.dump(metrics, f, indent=2)
    print(f"Metrics saved to: {metrics_path}")

if __name__ == "__main__":
    base_dir = os.path.join(os.path.dirname(__file__), "..", "..")
    data_file = os.path.join(base_dir, "data", "processed", "synthetic_training_data.csv")
    models_dir = os.path.join(base_dir, "models")
    train_and_evaluate(data_file, models_dir)
