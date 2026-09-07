import pytest
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from src.explainability.shap_explainer import ModelExplainer

@pytest.fixture
def dummy_rf_model():
    """Trains a quick dummy RF model to test SHAP integration"""
    np.random.seed(42)
    # 50 samples, 3 features
    X = np.random.rand(50, 3)
    # Target (some 0s and 1s)
    y = np.random.randint(0, 2, 50)
    
    model = RandomForestClassifier(n_estimators=10, random_state=42)
    model.fit(X, y)
    return model

def test_shap_explainer_initialization(dummy_rf_model):
    feature_names = ["feat1", "feat2", "feat3"]
    explainer = ModelExplainer(dummy_rf_model, feature_names)
    
    assert explainer.failure_class_index != -1
    # Random forest classes are typically [0, 1]
    assert explainer.failure_class_index == list(dummy_rf_model.classes_).index(1)

def test_shap_explanation_logic(dummy_rf_model):
    feature_names = ["feat1", "feat2", "feat3"]
    explainer = ModelExplainer(dummy_rf_model, feature_names)
    
    # Create a single test sample
    X_test = pd.DataFrame([[0.5, 0.2, 0.8]], columns=feature_names)
    
    explanations = explainer.explain(X_test)
    
    # Ensure it returns explanations
    assert len(explanations) > 0
    # Must be capped by TOP_SHAP_FEATURES, but here we only have 3 features total
    assert len(explanations) <= 3
    
    # Check structure
    first_exp = explanations[0]
    assert "feature_name" in first_exp
    assert "shap_value" in first_exp
    assert "absolute_shap_value" in first_exp
    assert "direction" in first_exp
    
    # Check ranking (descending by absolute value)
    for i in range(len(explanations) - 1):
        assert explanations[i]["absolute_shap_value"] >= explanations[i+1]["absolute_shap_value"]
        
    # Check direction mapping
    for exp in explanations:
        if exp["shap_value"] > 0:
            assert exp["direction"] == "INCREASES_RISK"
        elif exp["shap_value"] < 0:
            assert exp["direction"] == "DECREASES_RISK"
        else:
            assert exp["direction"] == "NEUTRAL"
            
        assert exp["absolute_shap_value"] == abs(exp["shap_value"])
