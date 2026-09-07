import os
import joblib
import pandas as pd
from src.baseline.baseline_manager import load_baseline
from src.inference.health_engine import calculate_health_score
from src.explainability.shap_explainer import ModelExplainer

class PredictiveMaintenanceModel:
    def __init__(self, model_path: str = None):
        if model_path is None:
            # Default location
            base_dir = os.path.join(os.path.dirname(__file__), "..", "..")
            model_path = os.path.join(base_dir, "models", "predictive_maintenance_rf_v1.joblib")
            
        if not os.path.exists(model_path):
            raise FileNotFoundError(f"Model file not found at: {model_path}. Please run training first.")
            
        model_data = joblib.load(model_path)
        self.model = model_data["model"]
        self.features = model_data["features"]
        self.version = model_data.get("version", "unknown")
        
        # Initialize SHAP explainer
        self.explainer = ModelExplainer(self.model, self.features)
        
    def predict(self, feature_dict: dict, machine_id: str = None) -> dict:
        """
        Takes a dictionary of features and returns a prediction, health score, risk level, and explanations.
        """
        # Ensure ordering exactly matches training
        try:
            input_vector = [feature_dict[feat] for feat in self.features]
        except KeyError as e:
            raise ValueError(f"Missing required feature for prediction: {e}")
            
        df_input = pd.DataFrame([input_vector], columns=self.features)
        
        predicted_class_idx = int(self.model.predict(df_input)[0])
        failure_probability = float(self.model.predict_proba(df_input)[0][1])
        
        prediction_label = "FAILURE_RISK" if predicted_class_idx == 1 else "NORMAL"
        
        # Explain the prediction using SHAP
        explanations = self.explainer.explain(df_input)
        
        # Calculate Health Score based on machine baseline
        baseline = load_baseline(machine_id) if machine_id else None
        health_score, risk_level, deviations = calculate_health_score(
            features=feature_dict,
            ml_probability=failure_probability,
            baseline=baseline
        )
        
        return {
            "failure_probability": failure_probability,
            "prediction": prediction_label,
            "model_version": self.version,
            "health_score": health_score,
            "risk_level": risk_level,
            "baseline_deviations": deviations,
            "explanations": explanations
        }
