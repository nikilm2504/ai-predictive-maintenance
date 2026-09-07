import shap
import numpy as np
import pandas as pd
from typing import List, Dict, Any
from src.config import TOP_SHAP_FEATURES

class ModelExplainer:
    def __init__(self, model, feature_names: List[str]):
        """
        Initializes the SHAP TreeExplainer.
        Extracts the internal index of the FAILURE_RISK class dynamically.
        """
        self.explainer = shap.TreeExplainer(model)
        self.feature_names = feature_names
        
        # Determine which class index corresponds to FAILURE_RISK (label 1)
        classes = list(model.classes_)
        if 1 in classes:
            self.failure_class_index = classes.index(1)
        else:
            # Fallback if label 1 isn't found
            self.failure_class_index = -1

    def explain(self, features_df: pd.DataFrame) -> List[Dict[str, Any]]:
        """
        Calculates SHAP values for a given feature vector DataFrame,
        and returns the top contributing features for the FAILURE_RISK prediction.
        """
        if self.failure_class_index == -1:
            return []
            
        shap_values = self.explainer.shap_values(features_df)
        
        # Normalize different SHAP version return types for TreeExplainer
        # For RandomForest, shap_values is typically a list of arrays (one per class).
        # We want the SHAP values corresponding to the failure risk class.
        
        if isinstance(shap_values, list):
            # Output format: list of (n_samples, n_features)
            target_shap = shap_values[self.failure_class_index][0]
        elif hasattr(shap_values, "values"):
            # Output format: Explanation object
            vals = shap_values.values
            if len(vals.shape) == 3:
                target_shap = vals[0, :, self.failure_class_index]
            else:
                target_shap = vals[0, :]
        elif isinstance(shap_values, np.ndarray):
            if len(shap_values.shape) == 3:
                target_shap = shap_values[0, :, self.failure_class_index]
            else:
                target_shap = shap_values[0]
        else:
            raise ValueError(f"Unrecognized SHAP values format: {type(shap_values)}")

        # Construct explanation objects
        explanations = []
        for feat_name, shap_val in zip(self.feature_names, target_shap):
            val = float(shap_val)
            abs_val = abs(val)
            
            if val > 0:
                direction = "INCREASES_RISK"
            elif val < 0:
                direction = "DECREASES_RISK"
            else:
                direction = "NEUTRAL"
                
            explanations.append({
                "feature_name": feat_name,
                "shap_value": val,
                "absolute_shap_value": abs_val,
                "direction": direction
            })
            
        # Sort by absolute SHAP value descending
        explanations.sort(key=lambda x: x["absolute_shap_value"], reverse=True)
        
        # Return top N
        return explanations[:TOP_SHAP_FEATURES]
