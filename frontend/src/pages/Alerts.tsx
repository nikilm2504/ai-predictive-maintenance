import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getAllAlerts, acknowledgeAlert, resolveAlert, getMachines } from '../services/api'
import { Alert, Machine } from '../types'
import { AlertTriangle, CheckCircle, Info } from 'lucide-react'

export function Alerts() {
    const { data: alerts, isLoading, isError } = useQuery({ queryKey: ['allAlerts'], queryFn: getAllAlerts })
    const { data: machines } = useQuery({ queryKey: ['machines'], queryFn: getMachines })

    if (isLoading) return <div className="text-muted-foreground animate-pulse p-6">Loading alerts...</div>
    if (isError) return <div className="text-destructive p-6">Failed to load alerts.</div>

    // Sort alerts by created time descending
    const sortedAlerts = alerts ? [...alerts].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()) : []

    return (
        <div className="space-y-6">
            <h2 className="text-2xl font-bold tracking-tight">System Alerts</h2>
            <div className="grid gap-4">
                {sortedAlerts.map(alert => (
                    <AlertCard key={alert.id} alert={alert} machine={machines?.find(m => m.id === alert.machineId)} />
                ))}
                {sortedAlerts.length === 0 && (
                    <div className="p-8 text-center text-muted-foreground border rounded-lg bg-card">No alerts found.</div>
                )}
            </div>
        </div>
    )
}

function AlertCard({ alert, machine }: { alert: Alert, machine?: Machine }) {
    const queryClient = useQueryClient()

    const ackMutation = useMutation({
        mutationFn: acknowledgeAlert,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['allAlerts'] })
            queryClient.invalidateQueries({ queryKey: ['activeAlerts'] })
            queryClient.invalidateQueries({ queryKey: ['alerts'] })
        }
    })

    const resolveMutation = useMutation({
        mutationFn: resolveAlert,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['allAlerts'] })
            queryClient.invalidateQueries({ queryKey: ['activeAlerts'] })
            queryClient.invalidateQueries({ queryKey: ['alerts'] })
        }
    })

    const isCritical = alert.severity === 'CRITICAL'
    const isResolved = alert.status === 'RESOLVED'
    const isAcknowledged = alert.status === 'ACKNOWLEDGED'

    return (
        <div className={`p-5 rounded-lg border ${isResolved ? 'bg-secondary/20 border-border/50 opacity-75' : 'bg-card shadow-sm'} transition-colors`}>
            <div className="flex items-start justify-between">
                <div className="flex gap-4">
                    <div className="mt-1">
                        {isResolved ? (
                            <CheckCircle className="h-6 w-6 text-green-500" />
                        ) : isCritical ? (
                            <AlertTriangle className="h-6 w-6 text-destructive animate-pulse" />
                        ) : (
                            <Info className="h-6 w-6 text-yellow-500" />
                        )}
                    </div>
                    <div>
                        <div className="flex items-center gap-3 mb-1">
                            <h3 className="font-semibold text-lg">{alert.title}</h3>
                            <span className={`px-2 py-0.5 rounded text-xs font-bold ${
                                isCritical ? 'bg-destructive/20 text-destructive' : 'bg-yellow-500/20 text-yellow-500'
                            }`}>
                                {alert.severity}
                            </span>
                            <span className="px-2 py-0.5 rounded bg-secondary text-secondary-foreground text-xs font-semibold">
                                {alert.status}
                            </span>
                        </div>
                        <p className="text-muted-foreground text-sm max-w-3xl leading-relaxed whitespace-pre-wrap">{alert.description}</p>
                        
                        <div className="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-xs text-muted-foreground">
                            {machine && (
                                <div><span className="font-semibold">Machine:</span> {machine.machineName} ({machine.machineCode})</div>
                            )}
                            <div><span className="font-semibold">Created:</span> {new Date(alert.createdAt).toLocaleString()}</div>
                            {alert.acknowledgedAt && <div><span className="font-semibold">Acknowledged:</span> {new Date(alert.acknowledgedAt).toLocaleString()}</div>}
                            {alert.resolvedAt && <div><span className="font-semibold">Resolved:</span> {new Date(alert.resolvedAt).toLocaleString()}</div>}
                        </div>
                    </div>
                </div>

                {!isResolved && (
                    <div className="flex flex-col gap-2 shrink-0">
                        {!isAcknowledged && (
                            <button 
                                onClick={() => ackMutation.mutate(alert.id)}
                                disabled={ackMutation.isPending}
                                className="px-4 py-2 bg-secondary text-secondary-foreground text-sm font-medium rounded-md hover:bg-secondary/80 disabled:opacity-50 transition-colors"
                            >
                                Acknowledge
                            </button>
                        )}
                        <button 
                            onClick={() => resolveMutation.mutate(alert.id)}
                            disabled={resolveMutation.isPending}
                            className="px-4 py-2 bg-green-600/20 text-green-500 text-sm font-medium rounded-md hover:bg-green-600/30 disabled:opacity-50 transition-colors"
                        >
                            Mark Resolved
                        </button>
                    </div>
                )}
            </div>
        </div>
    )
}
