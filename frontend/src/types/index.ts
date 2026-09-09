export interface Machine {
    id: string;
    machineCode: string;
    machineName: string;
    machineType: string;
    location: string;
    status: string;
    createdAt: string;
    updatedAt: string;
}

export interface Telemetry {
    id: string;
    machineId: string;
    timestamp: string;
    vibration: number;
    temperature: number;
    current: number;
    rpm: number;
}

export interface Prediction {
    id: string;
    machineId: string;
    ts: string;
    faultType: string | null;
    failureProbability: number;
    healthScore: number;
    riskLevel?: string;
    modelVersion: string;
}

export interface Alert {
    id: string;
    machineId: string;
    predictionId: string;
    severity: string;
    status: string;
    title: string;
    description: string;
    createdAt: string;
    acknowledgedAt: string | null;
    resolvedAt: string | null;
}

export interface ShapExplanation {
    id: string;
    predictionId: string;
    featureName: string;
    shapValue: number;
    direction: string;
}

// SSE Event Payload
export interface DashboardUpdateEvent {
    machineId: string;
    machineCode: string;
    timestamp: string;
    telemetry: {
        vibration: number;
        temperature: number;
        current: number;
        rpm: number;
    };
    prediction: {
        failureProbability: number;
        healthScore: number;
        riskLevel: string;
        modelVersion: string;
    };
    alert: {
        severity: string;
        status: string;
        title: string;
        description: string;
    } | null;
    shapExplanations: {
        featureName: string;
        contribution: number;
        direction: string;
    }[];
}
