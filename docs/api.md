# API Documentation

This document describes the REST APIs exposed by the Predictive Maintenance platform.

## Base Path
All APIs are relative to: `/api/v1`

---

## Machine APIs

### Create a Machine
- **Method**: `POST`
- **Endpoint**: `/machines`
- **Request Body**:
  ```json
  {
    "machineCode": "M001",
    "name": "Industrial Motor 001",
    "machineType": "MOTOR",
    "location": "Factory Floor A"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "machineCode": "M001",
    "name": "Industrial Motor 001",
    "machineType": "MOTOR",
    "location": "Factory Floor A",
    "status": "ACTIVE",
    "createdAt": "2023-10-01T10:00:00Z",
    "updatedAt": "2023-10-01T10:00:00Z"
  }
  ```
- **Error Codes**:
  - `400 Bad Request`: Validation failure.
  - `409 Conflict`: Machine code already exists.

### Get a Machine by ID
- **Method**: `GET`
- **Endpoint**: `/machines/{id}`
- **Response**: `200 OK` (returns Machine Response object)
- **Error Codes**:
  - `404 Not Found`: Machine does not exist.

### Get all Machines
- **Method**: `GET`
- **Endpoint**: `/machines`
- **Response**: `200 OK` (returns an array of Machine Response objects)

### Update a Machine
- **Method**: `PUT`
- **Endpoint**: `/machines/{id}`
- **Request Body**:
  ```json
  {
    "name": "Industrial Motor 001 - Updated",
    "machineType": "MOTOR",
    "location": "Factory Floor B",
    "status": "INACTIVE"
  }
  ```
- **Response**: `200 OK` (returns updated Machine Response object)
- **Error Codes**:
  - `400 Bad Request`: Validation failure.
  - `404 Not Found`: Machine does not exist.

### Decommission a Machine
- **Method**: `DELETE`
- **Endpoint**: `/machines/{id}`
- **Description**: Logically deletes (decommissions) the machine by changing its status to `DECOMMISSIONED`. It preserves historical data like telemetry, alerts, and records.
- **Response**: `204 No Content`
- **Error Codes**:
  - `404 Not Found`: Machine does not exist.

---

## Sensor APIs

### Create a Sensor for a Machine
- **Method**: `POST`
- **Endpoint**: `/machines/{machineId}/sensors`
- **Request Body**:
  ```json
  {
    "sensorCode": "VIB-M001-01",
    "sensorType": "VIBRATION",
    "unit": "g"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "id": "223e4567-e89b-12d3-a456-426614174001",
    "machineId": "123e4567-e89b-12d3-a456-426614174000",
    "sensorCode": "VIB-M001-01",
    "sensorType": "VIBRATION",
    "unit": "g",
    "status": "ACTIVE",
    "installedAt": "2023-10-01T10:05:00Z"
  }
  ```
- **Error Codes**:
  - `400 Bad Request`: Validation failure.
  - `404 Not Found`: Machine does not exist.
  - `409 Conflict`: Sensor code already exists.

### Get a Sensor by ID
- **Method**: `GET`
- **Endpoint**: `/sensors/{id}`
- **Response**: `200 OK` (returns Sensor Response object)
- **Error Codes**:
  - `404 Not Found`: Sensor does not exist.

### Get Sensors for a Machine
- **Method**: `GET`
- **Endpoint**: `/machines/{machineId}/sensors`
- **Response**: `200 OK` (returns an array of Sensor Response objects)
- **Error Codes**:
  - `404 Not Found`: Machine does not exist.

### Update a Sensor
- **Method**: `PUT`
- **Endpoint**: `/sensors/{id}`
- **Request Body**:
  ```json
  {
    "unit": "m/s2",
    "status": "FAULTY"
  }
  ```
- **Response**: `200 OK` (returns updated Sensor Response object)
- **Error Codes**:
  - `400 Bad Request`: Validation failure.
  - `404 Not Found`: Sensor does not exist.

### Delete a Sensor
- **Method**: `DELETE`
- **Endpoint**: `/sensors/{id}`
- **Description**: Logically deletes a sensor by changing its status to `DECOMMISSIONED`. It preserves historical telemetry associated with this sensor.
- **Response**: `204 No Content`
- **Error Codes**:
  - `404 Not Found`: Sensor does not exist.
---

## Telemetry APIs

### Ingest Telemetry
- **Method**: POST
- **Endpoint**: /machines/{machineId}/telemetry
- **Description**: Records a new telemetry reading for a machine.
- **Request Body**:
  `json
  {
    "timestamp": "2026-09-06T12:30:00Z",
    "vibration": 2.35,
    "temperature": 48.7,
    "current": 5.2,
    "rpm": 1450.0
  }
  `
- **Response**: 201 Created
  `json
  {
    "id": "323e4567-e89b-12d3-a456-426614174002",
    "machineId": "123e4567-e89b-12d3-a456-426614174000",
    "timestamp": "2026-09-06T12:30:00Z",
    "vibration": 2.35,
    "temperature": 48.7,
    "current": 5.2,
    "rpm": 1450.0
  }
  `
- **Error Codes**:
  - 400 Bad Request: Validation failure (e.g., NaN/Infinity values, missing fields).
  - 404 Not Found: Machine does not exist.

### Get Latest Telemetry
- **Method**: GET
- **Endpoint**: /machines/{machineId}/telemetry/latest
- **Description**: Retrieves the most recent telemetry record for a given machine.
- **Response**: 200 OK (returns Telemetry Response object)
- **Error Codes**:
  - 404 Not Found: Machine does not exist, or no telemetry available for the machine.

### Get Telemetry History
- **Method**: GET
- **Endpoint**: /machines/{machineId}/telemetry
- **Description**: Retrieves a paginated list of telemetry records for a machine, sorted by timestamp descending.
- **Query Parameters**:
  - page (optional): Page number (default 0).
  - size (optional): Page size (default 20).
  - rom (optional): Start time (inclusive, ISO-8601 Instant).
  - 	o (optional): End time (inclusive, ISO-8601 Instant).
- **Response**: 200 OK
  `json
  {
    "content": [
      {
        "id": "...",
        "machineId": "...",
        "timestamp": "2026-09-06T12:30:00Z",
        "vibration": 2.35,
        "temperature": 48.7,
        "current": 5.2,
        "rpm": 1450.0
      }
    ],
    "page": {
      "size": 20,
      "number": 0,
      "totalElements": 1,
      "totalPages": 1
    }
  }
  `
- **Error Codes**:
  - 400 Bad Request: Invalid time range (e.g., rom is after 	o).
  - 404 Not Found: Machine does not exist.
