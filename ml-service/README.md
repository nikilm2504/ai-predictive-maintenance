# Predictive Maintenance ML Service (Milestone 6 & 7)

This service is the foundational Python Machine Learning pipeline for the Predictive Maintenance project.

## Scope (Milestone 6: Feature Engineering)
Transforms raw telemetry windows into structured, machine-learning-ready feature vectors.

## Scope (Milestone 7: Model Training & Inference)
Trains a Random Forest classifier using the engineered features and exposes a FastAPI endpoint for predictions.

---

## Input Telemetry Format
The pipeline natively reads telemetry data using pandas DataFrames. It is decoupled from PostgreSQL to remain easily testable. 
Expected columns: `timestamp`, `vibration`, `temperature`, `current`, `rpm`.

## Windowing Concept
Time-series data is segmented into configurable windows (`WINDOW_SIZE` = 10).
Missing values within a window use a `forward fill` (`ffill`) strategy to avoid silent zero-filling which could skew models.

## Feature Extraction

### Statistical Features
Applied across multiple sensors.
- Mean, Median, Min, Max, Standard Deviation, Range.
- Root Mean Square (RMS) & Crest Factor (Vibration/Current).
- Peak & Peak-to-Peak (Vibration).
- Kurtosis & Skewness (Vibration).
- Trend Slope (Temperature - detects gradual degradation).

### FFT (Frequency-Domain) Concept
FFT *does not predict failure on its own*. It transforms a time-domain vibration signal into a frequency-domain representation.
- The DC component (0 Hz) is removed.
- Features extracted: **Dominant Frequency** and **Dominant Frequency Amplitude**.
- Sampling frequency (`VIBRATION_SAMPLING_FREQ` = 100 Hz) is decoupled from MQTT intervals.

---

## Milestone 7: Training & Inference Architecture

### Dataset & Label Strategy
Since no real industrial data was provided, we use a deterministic synthetic telemetry generator (`src/data/generate_dataset.py`) to create a labelled dataset (`0`: NORMAL, `1`: FAILURE_RISK).
Data splitting uses `GroupShuffleSplit` across `machine_id` to strictly prevent data leakage across train/test splits.

### Model Training
- **Algorithm**: Random Forest (`scikit-learn`), a robust baseline.
- **Class Imbalance**: Handled via `class_weight='balanced'`.
- **Evaluation**: Accuracy, Precision, Recall, F1, and ROC-AUC. High Recall is prioritized to minimize False Negatives.
- **Persistence**: Models are persisted to `models/predictive_maintenance_rf_v1.joblib` using `joblib`.

### Inference API
A lightweight FastAPI application exposes the model:
- `GET /health`: Model status and version check.
- `POST /predict`: Accepts engineered feature vectors, returns `failure_probability`, the `prediction` classification, and the `model_version`.

---

## Setup & Testing

1. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

2. **Run the test suite** (Covers Feature Engineering, Training, and API):
   ```bash
   python -m pytest tests
   ```

3. **Generate Dataset**:
   ```bash
   python -m src.data.generate_dataset
   ```

4. **Train Model**:
   ```bash
   python -m src.training.train
   ```

5. **Start Inference Server**:
   ```bash
   python -m uvicorn src.api.app:app --host 0.0.0.0 --port 8000
   ```

6. **Example API Request**:
   ```bash
   curl -X POST "http://localhost:8000/predict" -H "Content-Type: application/json" -d '{
       "features": {
           "vibration_mean": 1.45,
           "vibration_std": 0.41,
           "vibration_rms": 1.50,
           "vibration_peak": 2.0,
           "vibration_peak_to_peak": 1.0,
           "vibration_variance": 0.17,
           "vibration_kurtosis": -1.5,
           "vibration_skewness": 0.18,
           "vibration_crest_factor": 1.32,
           "vibration_dominant_frequency": 30.0,
           "vibration_dominant_frequency_amplitude": 0.25,
           "temperature_mean": 40.45,
           "temperature_std": 0.28,
           "temperature_min": 40.0,
           "temperature_max": 40.9,
           "temperature_range": 0.9,
           "temperature_trend": 0.1,
           "current_mean": 5.1,
           "current_std": 0.1,
           "current_min": 5.0,
           "current_max": 5.2,
           "current_rms": 5.1,
           "current_range": 0.2,
           "rpm_mean": 1420.0,
           "rpm_std": 14.1,
           "rpm_min": 1400.0,
           "rpm_max": 1440.0,
           "rpm_range": 40.0
       }
   }'
   ```

## Known Limitations
- Model is purely a baseline architecture proof-of-concept using deterministic synthetic data.
- API is not yet dockerized.
- Does not interact with PostgreSQL. Future milestones will integrate this API back into the Spring Boot backend.
