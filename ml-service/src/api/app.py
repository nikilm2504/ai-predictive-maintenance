from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Extra
from typing import Dict, Any, List

from src.inference.predictor import PredictiveMaintenanceModel

app = FastAPI(title="Predictive Maintenance Inference API")

# Initialize model lazily to allow tests to run without failing instantly if model isn't built
model_instance = None

def get_model():
    global model_instance
    if model_instance is None:
        try:
            model_instance = PredictiveMaintenanceModel()
        except FileNotFoundError as e:
            # We don't crash the server startup, but endpoints will fail gracefully
            print(f"Warning: {e}")
    return model_instance

class PredictRequest(BaseModel):
    machine_id: str | None = None
    features: Dict[str, float]

    model_config = {"extra": "forbid"}

class ExplanationModel(BaseModel):
    feature_name: str
    shap_value: float
    absolute_shap_value: float
    direction: str

class PredictResponse(BaseModel):
    failure_probability: float
    prediction: str
    model_version: str
    health_score: float
    risk_level: str
    baseline_deviations: Dict[str, float]
    explanations: List[ExplanationModel]

@app.get("/health")
def health_check():
    m = get_model()
    if m is None:
        return {"status": "degraded", "message": "Model not loaded"}
    return {"status": "ok", "model_version": m.version}

@app.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest):
    m = get_model()
    if m is None:
        raise HTTPException(status_code=503, detail="Model is not available. Please run training first.")
        
    try:
        result = m.predict(request.features, machine_id=request.machine_id)
        return PredictResponse(**result)
    except ValueError as e:
        # e.g., missing features
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail="Internal inference error")
