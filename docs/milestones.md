# Project Milestones & Execution Roadmap

**Project Title:** AI/ML IoT-Based Predictive Maintenance for Industrial Machines  
**Document Version:** 1.0.0  
**Current Milestone:** Milestone 0 (Architecture & Project Specification)

---

## 📌 Milestone Execution Principles

To ensure engineering rigor and prevent scope creep, development follows these strict rules:
1. **Single-Milestone Execution**: Only one milestone is implemented at a time. Work never advances to the next milestone without explicit verification and approval.
2. **No Premature Technology Injection**: Technologies outside the approved stack will never be added without prior justification and approval.
3. **No Dead or Placeholder Code**: Code for future milestones will not be stubbed out prematurely.
4. **Validation First**: Every milestone must define automated verification commands and clear acceptance criteria before completion.

---

## 🗺️ Milestone Breakdown

### Milestone 0: Architecture and Project Specification *(CURRENT)*
- **Objective**: Establish the technical blueprint, domain models, architectural decision records, and roadmap documentation.
- **Key Deliverables**:
  - `README.md`: High-level system overview, technology stack, and quick start guide.
  - `docs/architecture.md`: In-depth architectural blueprint, ERD, component boundaries, and data flow sequences.
  - `docs/milestones.md`: Detailed 17-milestone roadmap with inputs, outputs, and verification criteria.
  - `docs/decisions.md`: Architectural Decision Records (ADRs) justifying foundational choices.
- **Dependencies**: None.
- **Acceptance Criteria**: Architectural documentation fully drafted, reviewed, and aligned with all user constraints. No code generated yet.

---

### Milestone 1: Spring Boot Backend Foundation
- **Objective**: Initialize the Maven-based Spring Boot modular monolith backend using Java 21.
- **Key Deliverables**:
  - `backend/pom.xml` with dependencies: Spring Web, Spring Data JPA, Spring Validation, Spring Actuator, PostgreSQL driver, Flyway core, Lombok (optional/minimal), and test starters.
  - Base application package: `com.pmp.predictivemaintenance`.
  - Package structure: `machine`, `telemetry`, `prediction`, `alert`, `maintenance`, `ai`, `common`.
  - Global centralized exception handling (`@RestControllerAdvice`) supporting RFC 7807 problem details.
  - Base configuration in `application.yml` with environment variable interpolation.
  - Working Actuator `/actuator/health` endpoint.
- **Dependencies**: Milestone 0.
- **Acceptance Criteria**: Application compiles cleanly with `./mvnw clean compile` (or `mvn clean compile`) and starts up successfully with health endpoint responding `UP`.

---

### Milestone 2: PostgreSQL + Flyway + Domain Model
- **Objective**: Establish the relational database schema via Flyway migrations and implement the JPA domain entity models.
- **Key Deliverables**:
  - Initial Flyway migration script `V1__create_initial_schema.sql` defining: `users`, `machines`, `sensors`, `telemetry`, `predictions`, `prediction_explanations`, `alerts`, `maintenance_work_orders`, `maintenance_records`.
  - JPA entities mapping each table, adhering to constraints and foreign keys.
  - Explicit exclusion: `Machine` entity must **not** contain `@OneToMany List<Telemetry>`.
  - Hibernate configuration set to `hibernate.ddl-auto=validate`.
  - Spring Data JPA repositories for all entities.
  - Local database connectivity verified via Docker Compose or local PostgreSQL.
- **Dependencies**: Milestone 1.
- **Acceptance Criteria**: Flyway executes migration cleanly on boot; JPA entities pass validation against the physical database schema without Hibernate mismatch errors.

---

### Milestone 3: Machine and Sensor Management APIs
- **Objective**: Implement CRUD operations and management services for industrial machines and physical sensors.
- **Key Deliverables**:
  - DTOs for Machine and Sensor creation, updates, and responses.
  - Declarative validation using `jakarta.validation` on all request payloads.
  - `MachineService` and `SensorService` with business rules (e.g. duplicate machine code validation, sensor type constraints).
  - `MachineController` and `SensorController` exposing RESTful endpoints:
    - `POST /api/v1/machines`, `GET /api/v1/machines`, `GET /api/v1/machines/{id}`, `PATCH /api/v1/machines/{id}/status`
    - `POST /api/v1/machines/{machineId}/sensors`, `GET /api/v1/machines/{machineId}/sensors`
  - Integration tests verifying CRUD lifecycle and error responses.
- **Dependencies**: Milestone 2.
- **Acceptance Criteria**: All machine and sensor management APIs function with valid DTOs and return appropriate HTTP status codes (201, 200, 400, 404, 409).

---

### Milestone 4: Telemetry Ingestion
- **Objective**: Implement high-throughput HTTP telemetry ingestion, payload validation, and indexed time-series query capabilities.
- **Key Deliverables**:
  - `TelemetryIngestRequest` DTO with physical range validations (vibration, temperature, current, RPM).
  - Single and batch ingestion endpoints (`POST /api/v1/telemetry`, `POST /api/v1/telemetry/batch`).
  - Indexed queries: `findRecentByMachineId(UUID machineId, Pageable pageable)` and time-bounded queries.
  - Persistence optimization for time-series throughput.
  - Unit and integration tests for validation failures (e.g., negative RPM or extreme current values).
- **Dependencies**: Milestone 3.
- **Acceptance Criteria**: Telemetry points can be ingested in singles or batches, stored efficiently, and queried chronologically in reverse order.

---

### Milestone 5: MQTT + Telemetry Simulator
- **Objective**: Implement asynchronous MQTT ingestion in Spring Boot and build a multi-sensor synthetic simulator.
- **Key Deliverables**:
  - Docker Compose configuration for Eclipse Mosquitto MQTT broker.
  - Spring Integration MQTT inbound adapter subscribed to `industrial/telemetry/#`.
  - Message parsing service mapping incoming MQTT payloads to `TelemetryService`.
  - Python-based telemetry simulator (`iot/simulator/simulator.py`) capable of generating:
    - Normal operating baseline data.
    - Bearing fault profile (rising high-frequency vibration and temperature).
    - Motor overload profile (elevated current and reduced RPM).
- **Dependencies**: Milestone 4.
- **Acceptance Criteria**: Simulator publishes messages to Mosquitto; Spring Boot asynchronously consumes the stream, parses payloads, and persists telemetry in PostgreSQL.

---

### Milestone 6: Python ML Service Foundation
- **Objective**: Establish the decoupled Python ML service skeleton with containerization and API scaffolding.
- **Key Deliverables**:
  - Python environment setup (`ml-service/requirements.txt`) with `pandas`, `numpy`, `scipy`, `scikit-learn`, `shap`, `fastapi`, `uvicorn`.
  - FastAPI application structure (`ml-service/app/`).
  - Health check endpoint (`GET /health`) and payload validation schemas (`pydantic`).
  - `ml-service/Dockerfile` for reproducible execution.
- **Dependencies**: Milestone 5.
- **Acceptance Criteria**: Python ML service boots cleanly, passes health checks, and accepts JSON payloads.

---

### Milestone 7: Feature Extraction + ML Prediction
- **Objective**: Implement digital signal processing (statistical + FFT) and tabular ML model inference using a saved model trained offline.
- **Key Deliverables**:
  - Telemetry window sizing: Determined experimentally based on sensor sampling frequency, Nyquist criteria for FFT, and model accuracy/latency performance (not fixed to an arbitrary sample count).
  - Feature extraction engine:
    - Time-domain statistics: Mean, Standard Deviation, RMS, Peak-to-Peak, Crest Factor, Kurtosis, Skewness.
    - Frequency-domain FFT: Fast Fourier Transform over vibration window to extract peak frequency and spectral energy bands.
  - Model inference pipeline: The ML model (Random Forest / XGBoost) is trained separately/offline using historical normal/fault datasets. During runtime operation, the Python ML Service loads the saved model artifact to perform low-latency online inference for fault classification (`NORMAL`, `BEARING_FAULT`, `ROTOR_UNBALANCE`, `OVERLOAD`).
  - Inference endpoint: `POST /api/v1/predict` returning fault type, failure probability, and prediction confidence.
  - Integration with Spring Boot: `PredictionClient` calling the ML service upon telemetry window completion.
- **Dependencies**: Milestone 6.
- **Acceptance Criteria**: Given a window of telemetry samples, the ML service extracts FFT and statistical features and returns a classified fault type and failure probability using the saved model artifact.

---

### Milestone 8: Health Score + Adaptive Threshold
- **Objective**: Compute dynamic, machine-specific health scores (0–100) and adaptive anomaly thresholds derived from historical/normal operating telemetry.
- **Key Deliverables**:
  - Baseline calibration derived from historical/normal operating telemetry for each individual machine.
  - Adaptive thresholding evaluated and calibrated specifically per machine (the mathematical algorithm will be designed, tested, and determined in this milestone, not pre-implemented).
  - Multi-parameter Health Score formula combining vibration degradation, thermal rise, and electrical stress into a normalized index (100 = Brand New, 0 = Immediate Failure).
  - Storage of health score within the `Prediction` entity.
- **Dependencies**: Milestone 7.
- **Acceptance Criteria**: Baseline adapts to individual machine profiles based on historical normal telemetry; machines in nominal states score >90, while machines exhibiting degradation show proportionally lower scores.

---

### Milestone 9: SHAP Explainability
- **Objective**: Generate feature attribution explanations using TreeSHAP for every ML prediction.
- **Key Deliverables**:
  - TreeSHAP explainer integrated into Python ML inference pipeline.
  - Feature importance ranking and directionality (`INCREASES_RISK` vs `DECREASES_RISK`).
  - Storage of top contributing features in `prediction_explanations` table via Spring Boot.
  - API endpoint to retrieve SHAP explanations for any prediction (`GET /api/v1/predictions/{id}/explanation`).
- **Dependencies**: Milestone 8.
- **Acceptance Criteria**: Predictions include top feature attributions explaining *why* the failure probability was assigned (e.g., `vib_kurtosis (+0.35)`, `temp_rms (+0.22)`).

---

### Milestone 10: Alerts + Maintenance Decision Engine
- **Objective**: Build an automated decision engine to evaluate predictions against thresholds and manage the lifecycle of alerts.
- **Key Deliverables**:
  - `MaintenanceDecisionEngine` evaluating incoming predictions and telemetry anomalies.
  - Severity classification rules: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` based on failure probability and health score trajectory.
  - Alert deduplication and debounce logic to prevent alert storms.
  - Alert lifecycle management APIs (`POST /api/v1/alerts/{id}/acknowledge`, `POST /api/v1/alerts/{id}/resolve`).
- **Dependencies**: Milestone 9.
- **Acceptance Criteria**: Degraded telemetry automatically spawns severity-rated alerts linked to specific predictions without creating redundant duplicates.

---

### Milestone 11: Spring AI Foundation
- **Objective**: Integrate Spring AI into the Spring Boot backend with LLM provider configuration and structured output.
- **Key Deliverables**:
  - Add Spring AI starter dependency to `backend/pom.xml`.
  - Configure AI client (OpenAI API compatible or local LLM via Ollama) in `application.yml`.
  - Prompt template management and system prompt definition for the industrial maintenance persona.
  - Testing LLM connectivity and deterministic structured JSON responses.
- **Dependencies**: Milestone 10.
- **Acceptance Criteria**: Spring AI responds reliably to diagnostic prompts using structured formats without hallucinating arbitrary database operations.

---

### Milestone 12: RAG with Maintenance Documents
- **Objective**: Build a Retrieval-Augmented Generation (RAG) pipeline over machinery manuals, manufacturer specs, and standard operating procedures (SOPs).
- **Key Deliverables**:
  - Document ingestion and chunking pipeline for machinery technical manuals (PDF / Markdown).
  - Vector Store integration (e.g., PgVector via PostgreSQL extension).
  - Embedding generation and similarity search service.
  - Context augmentation injecting retrieved manual excerpts into the LLM prompt.
- **Dependencies**: Milestone 11.
- **Acceptance Criteria**: AI queries regarding specific machine fault codes retrieve relevant manufacturer SOP paragraphs and cite source document sections.

---

### Milestone 13: Spring AI Tool Calling + Maintenance Copilot
- **Objective**: Implement controlled AI tool calling allowing the Copilot to inspect machine state without direct database access.
- **Key Deliverables**:
  - Service facade tools registered with Spring AI:
    - `getMachineHealth(machineId)`
    - `getLatestTelemetry(machineId)`
    - `getPrediction(machineId)`
    - `getMachineHistory(machineId, days)`
    - `getActiveAlerts(machineId)`
    - `getMaintenanceHistory(machineId)`
  - Interactive Copilot API (`POST /api/v1/copilot/chat`).
  - Execution guardrails: Read-only query tools only.
- **Dependencies**: Milestone 12.
- **Acceptance Criteria**: Copilot dynamically selects and invokes backend tools to answer complex multi-step queries about machine health, predictions, and manual procedures.

---

### Milestone 14: Human Approval + Maintenance Work Orders
- **Objective**: Establish the human-in-the-loop approval workflow and full lifecycle management of Maintenance Work Orders.
- **Key Deliverables**:
  - AI Work Order recommendation engine (suggests title, description, priority, and parts needed).
  - Human approval endpoint (`POST /api/v1/work-orders/approve`).
  - Work Order state machine: `OPEN` -> `ASSIGNED` -> `IN_PROGRESS` -> `COMPLETED` / `CANCELLED`.
  - Maintenance record creation endpoint linking actions taken to closed work orders.
- **Dependencies**: Milestone 13.
- **Acceptance Criteria**: No work order can transition to active status or trigger maintenance actions without explicit human engineer sign-off.

---

### Milestone 15: Real-Time Dashboard
- **Objective**: Deliver real-time telemetry streaming and live alert updates via WebSockets or Server-Sent Events (SSE).
- **Key Deliverables**:
  - WebSocket / SSE configuration in Spring Boot.
  - Live broadcast channels for real-time telemetry points and newly triggered alerts.
  - Lightweight web dashboard showing live machine status gauges, health score charts, and active alert feeds.
- **Dependencies**: Milestone 14.
- **Acceptance Criteria**: Telemetry generated by the simulator appears in real-time on the dashboard without manual page refreshes.

---

### Milestone 16: Integration + Testing + Docker + Documentation
- **Objective**: Full end-to-end integration, automated test suites, complete Docker Compose environment, and production deployment documentation.
- **Key Deliverables**:
  - Unified `docker-compose.yml` orchestrating PostgreSQL, Mosquitto, Spring Boot backend, and Python ML service.
  - End-to-end integration test validating the complete pipeline: Telemetry -> MQTT -> Ingestion -> ML -> Prediction -> SHAP -> Alert -> Copilot -> Work Order.
  - Comprehensive operations manual and API documentation (OpenAPI/Swagger).
- **Dependencies**: Milestones 1 through 15.
- **Acceptance Criteria**: Single command `docker compose up` brings up the entire functional platform; end-to-end test suite passes cleanly.
