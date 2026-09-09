import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { DashboardUpdateEvent } from '../types';

export const useSSE = () => {
    const [status, setStatus] = useState<'connecting' | 'connected' | 'disconnected'>('connecting');
    const queryClient = useQueryClient();

    useEffect(() => {
        const eventSource = new EventSource('http://localhost:8080/api/v1/dashboard/stream');

        eventSource.onopen = () => {
            setStatus('connected');
        };

        eventSource.onerror = () => {
            setStatus('disconnected');
            // Reconnect automatically happens with SSE, but let's just log it
            console.error('SSE connection lost. Reconnecting...');
        };

        eventSource.addEventListener('INIT', (event) => {
            console.log('SSE Init:', event.data);
            setStatus('connected');
        });

        eventSource.addEventListener('MACHINE_UPDATE', (event) => {
            const data: DashboardUpdateEvent = JSON.parse(event.data);
            console.log('SSE Update:', data);

            // Invalidate or update React Query cache seamlessly
            queryClient.setQueryData(['latestTelemetry', data.machineId], (old: any) => ({
                ...old,
                vibration: data.telemetry.vibration,
                temperature: data.telemetry.temperature,
                current: data.telemetry.current,
                rpm: data.telemetry.rpm,
                timestamp: data.timestamp
            }));

            queryClient.setQueryData(['latestPrediction', data.machineId], (old: any) => ({
                ...old,
                failureProbability: data.prediction.failureProbability,
                healthScore: data.prediction.healthScore,
                riskLevel: data.prediction.riskLevel,
                ts: data.timestamp
            }));

            queryClient.setQueryData(['latestExplanations', data.machineId], data.shapExplanations.map(s => ({
                featureName: s.featureName,
                shapValue: s.contribution,
                direction: s.direction
            })));

            if (data.alert) {
                // If there's an alert update, just invalidate the alerts to pull the fresh list
                queryClient.invalidateQueries({ queryKey: ['alerts', data.machineId] });
                queryClient.invalidateQueries({ queryKey: ['activeAlerts'] });
            }
            
            // Also update history by invalidating
            queryClient.invalidateQueries({ queryKey: ['telemetryHistory', data.machineId] });
            queryClient.invalidateQueries({ queryKey: ['predictionHistory', data.machineId] });
        });

        return () => {
            eventSource.close();
        };
    }, [queryClient]);

    return { status };
};
