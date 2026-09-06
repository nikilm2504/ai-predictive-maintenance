# AI/ML IoT-Based Predictive Maintenance for Industrial Machines

A production-inspired, end-to-end predictive maintenance platform for industrial machinery combining multi-sensor IoT telemetry, classical Machine Learning with explainability (SHAP), and an AI-powered Maintenance Copilot using Spring AI with Retrieval-Augmented Generation (RAG) and controlled tool calling.

---

## 📌 Project Overview

In industrial environments, unplanned equipment downtime leads to severe operational and financial losses. This platform continuously collects, monitors, and analyzes multi-sensor telemetry to predict machine failures before they occur, assess machine health, pinpoint root causes with feature attributions, and assist maintenance engineers with AI-driven, human-approved work orders.

### Core Objectives
1. **Multi-Sensor Telemetry Collection**: High-frequency monitoring of Vibration, Temperature, Current, and RPM.
2. **Abnormal Behavior & Anomaly Detection**: Machine-specific baseline modeling with adaptive thresholds.
3. **Failure Prediction & Health Scoring**: Machine learning failure probability estimation alongside an intuitive Health Score (0–100).
4. **Explainable AI (XAI)**: Prediction transparency via SHAP (SHapley Additive exPlanations) values for every alert.
5. **Maintenance Decision Engine**: Automatic generation of severity-graded alerts and maintenance work orders.
6. **AI Maintenance Copilot (Spring AI)**: Natural language reasoning, RAG on machinery technical manuals/SOPs, and controlled tool calling without direct database access.
7. **Human-in-the-Loop Governance**: Mandatory engineer approval for sensitive maintenance decisions and work order dispatch.

---

## 🏗️ High-Level System Architecture

```
Industrial Machine / Telemetry Simulator
                  ↓
          Multi-Sensor Telemetry
        (Vibration, Temp, Current, RPM)
                  ↓
       ESP32 / Telemetry Simulator
                  ↓ (MQTT)
         Eclipse Mosquitto Broker
                  ↓ (MQTT Ingestion)
      Spring Boot Backend (Modular Monolith)
       ├── Ingestion & Persistence ───► PostgreSQL (Flyway Migrations)
       │                                     ▲
       ├── ML Client Service ─────────────┐  │ (Predictions & SHAP)
       │                                  ▼  │
       │                         Python ML Microservice
       │                         ├── Windowing & Feature Extraction
       │                         ├── FFT Spectral Analysis
       │                         ├── Random Forest / XGBoost Inference
       │                         └── SHAP Attribution Engine
       │
       ├── Maintenance Decision Engine
       │   └── Rule & ML-based Alert Dispatcher
       │
       └── Spring AI Maintenance Copilot
           ├── RAG Engine (Vector Store + Machinery SOPs / Manuals)
           ├── Controlled Tool Calling (Internal Service Facades)
           └── Human-in-the-Loop Approval Workflow
                  ↓
       Maintenance Work Order & Records
```

### Key Architectural Tenets
- **Modular Monolith Backend**: Built with Java 21 and Spring Boot. No premature microservice complexity. Modules (`machine`, `telemetry`, `prediction`, `alert`, `maintenance`, `ai`, `common`) are strictly isolated within package boundaries.
- **Dedicated Python ML Service**: Isolated service leveraging the Python scientific stack (`pandas`, `numpy`, `scipy`, `scikit-learn`, `shap`) for heavy numerical, FFT, and explainability computation.
- **Strict Role for Spring AI**: Spring AI is **not** the ML prediction engine. It handles contextual interpretation, manual lookup (RAG), and engineer dialogue through controlled Spring service tool calls.
- **Database Schema Integrity**: PostgreSQL schema managed strictly via Flyway migrations. Hibernate operates in `validate` mode.
- **Scalable Telemetry Mapping**: `Machine` does *not* hold a bidirectional `@OneToMany List<Telemetry>` to prevent memory bottlenecks. Telemetry is queried explicitly via repository methods.

---

## 🛠️ Technology Stack

| Layer / Component | Technology | Rationale |
| :--- | :--- | :--- |
| **Backend Core** | Java 21, Spring Boot 3.x | Long-Term Support (LTS), high performance, robust enterprise ecosystem |
| **Data & Persistence** | PostgreSQL, Spring Data JPA, Flyway | Relational integrity, ACID compliance, deterministic schema evolution |
| **IoT & Messaging** | MQTT (Eclipse Mosquitto), Spring Integration MQTT | Lightweight publish-subscribe protocol suited for high-frequency sensor telemetry |
| **Machine Learning** | Python 3.11, scikit-learn, SciPy, NumPy, pandas | Industry-standard scientific computing, FFT signal processing, tabular ML |
| **Model Explainability** | SHAP (SHapley Additive exPlanations) | Game-theoretic feature attribution for trustworthy industrial diagnostics |
| **GenAI & Assistance** | Spring AI (OpenAI / Local LLM / PgVector) | Native Spring integration for RAG, prompt orchestration, and controlled tool calling |
| **Validation & Error Handling**| Spring Validation (`jakarta.validation`), RFC 7807 | Centralized API exception handling and strict payload validation |
| **Observability** | Spring Boot Actuator | Production-ready health probes, metrics, and application monitoring |
| **Build & Tooling** | Maven, Docker, Docker Compose | Reproducible builds and standardized local development environment |

---

## 📂 Repository Structure

```
predictive-maintenance/
├── backend/                  # Spring Boot Modular Monolith
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/pmp/predictivemaintenance/
│       │   │   ├── machine/        # Machine & Sensor domain module
│       │   │   ├── telemetry/      # Telemetry ingestion & query module
│       │   │   ├── prediction/     # ML client, inference, & SHAP module
│       │   │   ├── alert/          # Alert generation & lifecycle module
│       │   │   ├── maintenance/    # Work orders & maintenance records module
│       │   │   ├── ai/             # Spring AI Copilot, RAG, & tool calling
│       │   │   └── common/         # Global config, exceptions, & utilities
│       │   └── resources/
│       │       ├── db/migration/   # Flyway SQL migration scripts
│       │       └── application.yml # Environment-driven configuration
│       └── test/                   # Unit and integration tests
├── ml-service/               # Python ML & Signal Processing Service
│   ├── app/
│   │   ├── api/                    # HTTP/FastAPI endpoints for inference
│   │   ├── features/               # Statistical & FFT feature extractors
│   │   ├── models/                 # Model loaders & inference pipelines
│   │   └── explainers/             # SHAP calculation engine
│   ├── requirements.txt
│   └── Dockerfile
├── iot/                      # Telemetry Simulator & ESP32 Firmware
│   ├── simulator/                  # Multi-sensor MQTT synthetic generator
│   └── esp32/                      # C++/Arduino firmware for physical sensors
├── docs/                     # Architectural documentation & specifications
│   ├── architecture.md             # In-depth architectural blueprint & ERD
│   ├── milestones.md               # 17-stage phased development roadmap
│   └── decisions.md                # Architecture Decision Records (ADRs)
└── docker/                   # Container orchestration
    └── docker-compose.yml          # Local environment (PostgreSQL, Mosquitto)
```

---

## 🗺️ Phased Development Roadmap

The project is executed in 17 distinct milestones (`Milestone 0` through `Milestone 16`):

- **Milestone 0**: Architecture and Project Specification *(Current)*
- **Milestone 1**: Spring Boot Backend Foundation
- **Milestone 2**: PostgreSQL + Flyway + Domain Model
- **Milestone 3**: Machine and Sensor Management APIs
- **Milestone 4**: Telemetry Ingestion
- **Milestone 5**: MQTT + Telemetry Simulator
- **Milestone 6**: Python ML Service Foundation
- **Milestone 7**: Feature Extraction + ML Prediction
- **Milestone 8**: Health Score + Adaptive Thresholds
- **Milestone 9**: SHAP Explainability Integration
- **Milestone 10**: Alerts + Maintenance Decision Engine
- **Milestone 11**: Spring AI Foundation
- **Milestone 12**: RAG with Maintenance Documents
- **Milestone 13**: Spring AI Tool Calling + Maintenance Copilot
- **Milestone 14**: Human Approval + Maintenance Work Orders
- **Milestone 15**: Real-Time Telemetry & Alerts Dashboard
- **Milestone 16**: System Integration, End-to-End Testing & Dockerization

For complete details on deliverables and acceptance criteria, refer to [`docs/milestones.md`](docs/milestones.md).

---

## 🚫 Out of Scope / Technologies Excluded Initially

To maintain architectural clarity, prevent unnecessary operational overhead, and focus on core engineering principles, the following technologies are **strictly excluded** during initial phases:
- No Microservices (Modular Monolith pattern is enforced)
- No Kubernetes (K8s) or service meshes
- No TinyML or Edge AI on ESP32 (ESP32 acts purely as an IoT sensor publisher)
- No Distributed streaming platforms (e.g., Kafka) or in-memory caches (e.g., Redis)
- No Complex Deep Learning / LSTMs (Tabular ML with Random Forest / XGBoost is used)
- No Federated Learning or complex Digital Twins

Detailed rationale is recorded in [`docs/decisions.md`](docs/decisions.md).

---

## 📖 Documentation Index

- **[System Architecture Blueprint](docs/architecture.md)**: Deep dive into modules, domain entities, ERD, and sequence diagrams.
- **[Development Milestones](docs/milestones.md)**: Full milestone breakdown with inputs, outputs, and verification strategies.
- **[Architecture Decision Records](docs/decisions.md)**: Justifications for architectural choices and technology constraints.
