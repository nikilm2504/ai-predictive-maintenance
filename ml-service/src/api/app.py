from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Dict, List

from src.inference.predictor import PredictiveMaintenanceModel
from fastapi.middleware.cors import CORSMiddleware


app = FastAPI(title="Predictive Maintenance Inference API")


# ---------------------------------------------------------
# CORS
# ---------------------------------------------------------

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://localhost:3000",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---------------------------------------------------------
# ML Model
# ---------------------------------------------------------

# Initialize model lazily so that tests can run even if
# the trained model is not available.
model_instance = None


def get_model():
    global model_instance

    if model_instance is None:
        try:
            model_instance = PredictiveMaintenanceModel()
        except FileNotFoundError as e:
            # Do not crash the server during startup.
            # Endpoints will return an appropriate error.
            print(f"Warning: {e}")

    return model_instance


# ---------------------------------------------------------
# Prediction Request / Response Models
# ---------------------------------------------------------

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
    confidence: float
    prediction: str
    model_version: str
    health_score: float
    risk_level: str
    baseline_deviations: Dict[str, float]
    explanations: List[ExplanationModel]

    model_config = {"extra": "forbid"}


# ---------------------------------------------------------
# Health Check
# ---------------------------------------------------------

@app.get("/health")
def health_check():
    m = get_model()

    if m is None:
        return {
            "status": "degraded",
            "message": "Model not loaded"
        }

    return {
        "status": "ok",
        "model_version": m.version
    }


# ---------------------------------------------------------
# Raw Telemetry Models
# ---------------------------------------------------------

class RawTelemetry(BaseModel):
    timestamp: str
    vibration: float
    temperature: float
    current: float
    rpm: float


class ExtractFeaturesRequest(BaseModel):
    machine_id: str | None = None
    telemetry_window: List[RawTelemetry]


class ExtractFeaturesResponse(BaseModel):
    features: Dict[str, float]


# ---------------------------------------------------------
# Feature Extraction API
# ---------------------------------------------------------

@app.post(
    "/features/extract",
    response_model=ExtractFeaturesResponse
)
def extract_features_api(request: ExtractFeaturesRequest):

    import pandas as pd
    from src.pipeline.feature_pipeline import process_telemetry

    # Convert telemetry window to DataFrame
    df = pd.DataFrame(
        [t.model_dump() for t in request.telemetry_window]
    )

    df["timestamp"] = pd.to_datetime(df["timestamp"])

    try:
        # Extract features
        features_df = process_telemetry(df)

        if features_df.empty:
            raise ValueError(
                "Could not extract features from the provided window."
            )

        # Get first feature vector
        feature_dict = features_df.iloc[0].to_dict()

        # Remove metadata fields if present
        for key in ["window_id", "machine_id", "label"]:
            feature_dict.pop(key, None)

        return ExtractFeaturesResponse(
            features=feature_dict
        )

    except Exception as e:
        raise HTTPException(
            status_code=400,
            detail=f"Feature extraction failed: {str(e)}"
        )


# ---------------------------------------------------------
# Prediction API
# ---------------------------------------------------------

@app.post(
    "/predict",
    response_model=PredictResponse
)
def predict(request: PredictRequest):

    m = get_model()

    if m is None:
        raise HTTPException(
            status_code=503,
            detail="Model is not available. Please run training first."
        )

    try:
        result = m.predict(
            request.features,
            machine_id=request.machine_id
        )

        return PredictResponse(**result)

    except ValueError as e:
        # Example: missing required features
        raise HTTPException(
            status_code=400,
            detail=str(e)
        )

    except Exception as e:
        print(f"Prediction error: {e}")

        raise HTTPException(
            status_code=500,
            detail="Internal inference error"
        )