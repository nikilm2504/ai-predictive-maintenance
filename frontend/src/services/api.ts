import axios from 'axios';
import { Machine, Telemetry, Prediction, Alert, ShapExplanation } from '../types';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/v1',
});

export const getMachines = async (): Promise<Machine[]> => {
    const { data } = await api.get('/machines');
    return data;
};

export const getMachine = async (id: string): Promise<Machine> => {
    const { data } = await api.get(`/machines/${id}`);
    return data;
};

export const getLatestTelemetry = async (machineId: string): Promise<Telemetry> => {
    const { data } = await api.get(`/machines/${machineId}/telemetry/latest`);
    return data;
};

export const getTelemetryHistory = async (machineId: string): Promise<{content: Telemetry[]}> => {
    const { data } = await api.get(`/machines/${machineId}/telemetry?size=20&sort=ts,desc`);
    return data;
};

export const getLatestPrediction = async (machineId: string): Promise<Prediction> => {
    const { data } = await api.get(`/machines/${machineId}/predictions/latest`);
    return data;
};

export const getPredictionHistory = async (machineId: string): Promise<{content: Prediction[]}> => {
    const { data } = await api.get(`/machines/${machineId}/predictions?size=20&sort=ts,desc`);
    return data;
};

export const getAlertsForMachine = async (machineId: string): Promise<Alert[]> => {
    const { data } = await api.get(`/machines/${machineId}/alerts`);
    return data;
};

export const getActiveAlerts = async (): Promise<Alert[]> => {
    const { data } = await api.get('/alerts');
    return data.filter((a: Alert) => a.status === 'OPEN' || a.status === 'ACKNOWLEDGED');
};

export const getAllAlerts = async (): Promise<Alert[]> => {
    const { data } = await api.get('/alerts');
    return data;
};

export const acknowledgeAlert = async (alertId: string): Promise<Alert> => {
    const { data } = await api.post(`/alerts/${alertId}/acknowledge`);
    return data;
};

export const resolveAlert = async (alertId: string): Promise<Alert> => {
    const { data } = await api.post(`/alerts/${alertId}/resolve`);
    return data;
};

export const getExplanations = async (machineId: string, predictionId: string): Promise<ShapExplanation[]> => {
    const { data } = await api.get(`/machines/${machineId}/predictions/${predictionId}/explanations`);
    return data;
};
