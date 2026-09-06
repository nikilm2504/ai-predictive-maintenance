# Architectural Decision Records (ADRs)

**Project Title:** AI/ML IoT-Based Predictive Maintenance for Industrial Machines  
**Document Version:** 1.0.0  
**Status:** Approved (Milestone 0)

---

## Index of Architectural Decisions

- [ADR-001: Modular Monolith Architecture for Backend](#adr-001-modular-monolith-architecture-for-backend)
- [ADR-002: Decoupled Python Service for Feature Extraction and ML](#adr-002-decoupled-python-service-for-feature-extraction-and-ml)
- [ADR-003: JPA Mapping & High-Volume Telemetry Strategy](#adr-003-jpa-mapping--high-volume-telemetry-strategy)
- [ADR-004: IoT Ingestion Strategy: Simulator-First Phasing](#adr-004-iot-ingestion-strategy-simulator-first-phasing)
- [ADR-005: Classical ML with FFT & Statistical Features vs Deep Learning](#adr-005-classical-ml-with-fft--statistical-features-vs-deep-learning)
- [ADR-006: Separation of Concerns: Spring AI vs Classical ML Engine](#adr-006-separation-of-concerns-spring-ai-vs-classical-ml-engine)
- [ADR-007: Human-in-the-Loop Governance for Maintenance Operations](#adr-007-human-in-the-loop-governance-for-maintenance-operations)
- [ADR-008: Database Evolution via Flyway with Hibernate Schema Validation](#adr-008-database-evolution-via-flyway-with-hibernate-schema-validation)
- [ADR-009: Strict Initial Scope Boundaries and Technology Exclusions](#adr-009-strict-initial-scope-boundaries-and-technology-exclusions)

---

## ADR-001: Modular Monolith Architecture for Backend

### Context
A platform of this scope often faces pressure to be designed as microservices (e.g., separating Machine Catalog, Telemetry Ingestion, Alerts, and Maintenance into independent deployment units). However, microservices introduce distributed system failure modes: network latency, distributed transactions, complex orchestration, deployment overhead, and increased cognitive load.

### Decision
We choose a **Modular Monolith** pattern using Java 21 and Spring Boot 3.x. The backend codebase is organized into distinct, logically isolated domain modules (`machine`, `telemetry`, `prediction`, `alert`, `maintenance`, `ai`, `common`) within a single deployable artifact.

### Consequences
- **Positive**:
  - Simple deployment and debugging within a single JVM process.
  - In-process method calls eliminate network latency between core business domains.
  - ACID transactions are guaranteed by PostgreSQL without distributed transaction managers (e.g., 2PC or Saga).
  - Clear domain boundaries enforce separation of concerns, leaving open the option to extract modules into services in the future if scale necessitates it.
- **Negative**:
  - Requires developer discipline to avoid circular dependencies between packages.
  - Scaling is done at the monolith level rather than per-module.

---

## ADR-002: Decoupled Python ML Service for Feature Extraction and ML

### Context
The platform requires advanced digital signal processing (Fast Fourier Transforms) and machine learning routines (Random Forest, XGBoost, TreeSHAP). While Java has libraries like Weka or Deeplearning4j, the Python ecosystem (`pandas`, `numpy`, `scipy`, `scikit-learn`, `shap`) is the undisputed standard for signal processing and explainable AI.

### Decision
We decouple the ML computation into a dedicated **Python ML Service** communicating with the Spring Boot Modular Monolith over an HTTP REST interface (`POST /api/v1/predict`).
- **Offline Training vs. Online Inference**: Machine learning models are trained separately/offline on historical baseline and fault datasets. During normal operational runtime, the Python ML Service loads the saved model artifact to execute low-latency online inference.
- **Architectural Boundary**: The main Java backend remains a Modular Monolith, while the Python component is treated as an auxiliary service for scientific and explainability computation.

### Consequences
- **Positive**:
  - Native access to optimized C/Fortran libraries (`numpy`, `scipy.fft`, `scikit-learn`, `shap`).
  - Python developers and data scientists can iterate on models offline without touching the Java enterprise backend.
  - Predictor service can be containerized and scaled independently based on CPU/GPU compute demands.
- **Negative**:
  - Introduces an HTTP boundary between the backend and ML service.
  - Network serialization overhead (JSON) for windowed telemetry arrays (mitigated by batching windows).

---

## ADR-003: JPA Mapping & High-Volume Telemetry Strategy

### Context
Industrial machines generate continuous, high-frequency telemetry (vibration, temperature, current, RPM). In standard JPA domain modeling, developers frequently map one-to-many relationships bidirectionally:
```java
@OneToMany(mappedBy = "machine", cascade = CascadeType.ALL)
private List<Telemetry> telemetries;
```
If a machine generates tens of thousands of telemetry records per day, loading a `Machine` entity can accidentally fetch or hydrate immense object graphs into memory, triggering catastrophic JVM `OutOfMemoryError` (OOM) failures or `LazyInitializationException` issues.

### Decision
We explicitly **forbid** bidirectional collection mappings from `Machine` to `Telemetry`. 
- The `Machine` entity has **no** reference to `List<Telemetry>`.
- The `Telemetry` entity holds a unidirectional `machineId` (or unnavigated foreign key).
- Telemetry queries are performed exclusively via explicit repository methods with pagination or time bounds:
  `findRecentByMachineId(UUID machineId, Pageable pageable)`
  `findByMachineIdAndTimestampBetween(UUID machineId, Instant start, Instant end)`

### Consequences
- **Positive**:
  - Zero risk of accidental OOM crashes from loading large telemetry collections.
  - Queries are predictable, lean, and easily optimized with database indexes `(machine_id, timestamp DESC)`.
- **Negative**:
  - Cannot access telemetry via `machine.getTelemetries()`; developers must explicitly inject and query `TelemetryRepository`.

---

## ADR-004: IoT Ingestion Strategy: Simulator-First Phasing

### Context
Attempting to develop physical ESP32 firmware alongside backend ingestion and ML pipelines simultaneously creates multi-point debugging friction: hardware sensor noise, wiring faults, Wi-Fi drops, and firmware flashing delays make it difficult to determine whether a bug originates in hardware or backend logic.

### Decision
Adopt a **phased IoT approach**:
1. **Phase 1**: Build a comprehensive synthetic Telemetry Simulator in Python publishing over MQTT. The simulator generates mathematically sound baselines with configurable failure curves (bearing wear, thermal runaway, motor imbalance).
2. **Phase 2**: Introduce physical ESP32 hardware reading real sensors only after the backend ingestion, database schema, and ML pipeline are fully operational and verified.
3. **Firmware Simplicity**: ESP32 firmware will strictly read sensor pins and publish MQTT JSON payloads. No TinyML or Edge AI will be embedded on the microcontrollers.

### Consequences
- **Positive**:
  - Backend, ML, and alerting logic can be developed and validated deterministically under repeatable conditions.
  - Failure scenarios (which are dangerous and expensive to reproduce on physical machinery) can be safely simulated.
  - Clean separation of hardware debugging from software engineering.
- **Negative**:
  - Physical hardware verification is deferred until software plumbing is stabilized.

---

## ADR-005: Classical ML with FFT & Statistical Features vs Deep Learning

### Context
Predictive maintenance literature frequently mentions deep learning architectures like LSTMs or CNN-LSTMs. However, deep neural networks are black boxes, require vast amounts of labeled training data, demand GPU acceleration, are prone to overfitting on small datasets, and are notoriously difficult to explain to plant maintenance personnel.

### Decision
We select **Classical Machine Learning (Random Forest / XGBoost)** coupled with rigorous **Feature Engineering** and **Machine-Specific Baselines**:
- **Offline Training & Saved Models**: Models are trained separately/offline; the Python ML Service performs online inference using serialized saved models.
- **Experimental Window Sizing**: The telemetry window size is not an arbitrary static constant (e.g. 64 or 128 samples); it will be determined experimentally based on sensor sampling frequency, Nyquist criteria for vibration FFT, and model accuracy/latency performance.
- **Machine-Specific Adaptive Thresholds**: Anomaly thresholds are machine-specific and derived from historical/normal operating telemetry for each individual machine. Rather than assuming static thresholds or prematurely implementing an algorithm, baseline calibration and adaptive thresholding algorithms will be evaluated and determined in Milestone 8.
- **Time-Domain Statistics**: Root Mean Square (RMS), Peak-to-Peak, Crest Factor, Kurtosis, Skewness, Variance.
- **Frequency-Domain Signal Processing**: Fast Fourier Transform (FFT) over vibration windows to identify spectral energy peaks (e.g. 1X RPM imbalance, bearing pass frequencies).
- **Explainability**: TreeSHAP for exact, efficient Shapley value attribution.

### Consequences
- **Positive**:
  - High accuracy on tabular/feature-engineered industrial data with low computational overhead.
  - Fully explainable via TreeSHAP in real time.
  - Does not require expensive GPU hardware for inference or training.
  - High interpretability for maintenance engineers who understand vibration harmonics and statistical kurtosis.
- **Negative**:
  - Requires domain knowledge to design feature extraction algorithms rather than letting a neural network learn raw signal representations.

---

## ADR-006: Separation of Concerns: Spring AI vs Classical ML Engine

### Context
The emergence of Large Language Models (LLMs) creates confusion about where generative AI fits within industrial systems. LLMs are non-deterministic, prone to hallucination, and fundamentally unsuitable for calculating mathematical failure probabilities from raw sensor vectors. Conversely, classical ML classifiers cannot converse with engineers or parse PDF operating manuals.

### Decision
We establish a **strict division of responsibilities**:
- **Classical ML Engine**: Sole authority on numerical inference, feature extraction, FFT analysis, failure probability calculation, health score indexing, and SHAP attribution vectors.
- **Spring AI**: Acts as an **intelligent assistant (Copilot)**. It reads the computed predictions, SHAP values, and sensor history via **controlled tool calling**, performs RAG over machinery SOPs and repair manuals, and explains findings in natural language.
- **Security Constraint**: Spring AI **never** connects directly to the database. It interacts solely with controlled Spring service facade tools (`getMachineHealth`, `getLatestTelemetry`, `getPrediction`, etc.).

### Consequences
- **Positive**:
  - Complete mathematical safety and determinism for failure calculations.
  - Natural, contextual maintenance assistance leveraging unstructured engineering manuals.
  - Strict security boundary prevents prompt-injection attacks from modifying database state.
- **Negative**:
  - Requires maintaining two AI tiers (Classical ML for predictions + Generative AI for interaction).

---

## ADR-007: Human-in-the-Loop Governance for Maintenance Operations

### Context
Industrial machines operate under severe safety, operational, and financial constraints. An AI agent autonomously taking machinery offline or ordering costly component replacements without supervision poses operational and safety risks.

### Decision
The system enforces **mandatory Human-in-the-Loop (HITL) approval**:
- The AI Maintenance Copilot can formulate diagnoses, propose work orders, and recommend maintenance procedures.
- However, **no sensitive mutating action** (dispatching work orders, modifying machine operational state, or clearing critical safety alerts) can execute without an explicit approval request signed off by an authorized human engineer (`MAINTENANCE_ENGINEER` or `ADMIN`).

### Consequences
- **Positive**:
  - Prevents automated damage or unnecessary downtime from false positives or AI hallucinations.
  - Meets industrial compliance and safety standards.
  - Builds trust with maintenance personnel.
- **Negative**:
  - Introduces a human bottleneck in the workflow (which is desired for safety-critical plant operations).

---

## ADR-008: Database Evolution via Flyway with Hibernate Schema Validation

### Context
Using Hibernate's `ddl-auto=update` in production or multi-developer environments causes unversioned, unpredictable database schema mutations, data corruption, and inability to rollback changes.

### Decision
All database schema creation and alterations will be managed strictly through **Flyway versioned SQL migrations** (`src/main/resources/db/migration/V{version}__{description}.sql`).
- Hibernate is configured with `spring.jpa.hibernate.ddl-auto=validate`.
- Flyway migrations are executed automatically during application startup.

### Consequences
- **Positive**:
  - Exact, repeatable, auditable database schema evolution across all environments (dev, test, prod).
  - Hibernate will refuse to start if entity annotations do not match the physical database schema, catching mapping bugs immediately.
- **Negative**:
  - Developers must write explicit SQL migration scripts for every schema change rather than relying on Hibernate to generate tables automatically.

---

## ADR-009: Strict Initial Scope Boundaries and Technology Exclusions

### Context
Academic and showcase engineering projects frequently suffer from "resume-driven development", introducing premature distributed technologies (Kubernetes, Kafka, Redis, TinyML, Federated Learning, Digital Twins) that complicate development and obscure core domain functionality.

### Decision
The following technologies are **explicitly excluded** from initial milestones:
1. **Kubernetes (K8s) & Microservices**: Docker Compose and the Modular Monolith provide the right operational simplicity.
2. **Kafka & Redis**: The initial telemetry messaging architecture is strictly MQTT with Eclipse Mosquitto. Kafka and Redis are excluded to avoid distributed streaming overhead; Mosquitto and PostgreSQL provide reliable ingestion throughput and query performance for the project scale.
3. **TinyML / Edge AI**: ESP32 microcontrollers are dedicated solely to sensor acquisition and MQTT transport (Phase 2). Telemetry simulator is used exclusively in Phase 1.
4. **Federated Learning & Complex Digital Twins**: Out of scope for predictive maintenance core objectives.

### Consequences
- **Positive**:
  - Keeps the codebase lean, testable, and maintainable.
  - Maximizes velocity on core value: feature extraction, explainable ML, and Spring AI copilot assistance.
- **Negative**:
  - Distributed streaming or multi-cluster deployment is deferred until scale justifies it.
