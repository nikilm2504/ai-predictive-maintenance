import { useQuery } from '@tanstack/react-query'
import { getMachines, getLatestPrediction, getActiveAlerts } from '../services/api'
import { Machine } from '../types'
import { AlertTriangle, CheckCircle2 } from 'lucide-react'

import { useNavigate } from 'react-router-dom'

export function Dashboard() {
    const { data: machines, isLoading } = useQuery({ queryKey: ['machines'], queryFn: getMachines })
    const navigate = useNavigate()

    if (isLoading) return <div className="text-muted-foreground animate-pulse">Loading machines...</div>

    return (
        <div className="space-y-6">
            <h2 className="text-2xl font-bold tracking-tight">Machine Overview</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {machines?.map(machine => (
                    <MachineCard key={machine.id} machine={machine} onClick={() => navigate(`/machines/${machine.id}`)} />
                ))}
            </div>
        </div>
    )
}

function MachineCard({ machine, onClick }: { machine: Machine, onClick: () => void }) {
    const { data: prediction } = useQuery({ 
        queryKey: ['latestPrediction', machine.id], 
        queryFn: () => getLatestPrediction(machine.id) 
    })
    const { data: alerts } = useQuery({
        queryKey: ['alerts', machine.id],
        queryFn: getActiveAlerts
    })

    const health = prediction?.healthScore ?? 100
    const risk = prediction?.riskLevel ?? 'UNKNOWN'
    
    // Check if there's any active alert for this machine
    const hasActiveAlert = alerts?.some(a => a.machineId === machine.id)

    return (
        <div 
            onClick={onClick}
            className="rounded-xl border bg-card text-card-foreground shadow-sm cursor-pointer hover:border-primary/50 transition-all p-6 space-y-4"
        >
            <div className="flex items-center justify-between">
                <div>
                    <h3 className="font-semibold text-lg">{machine.machineName}</h3>
                    <p className="text-sm text-muted-foreground">{machine.machineCode} • {machine.machineType}</p>
                </div>
                {hasActiveAlert ? (
                    <AlertTriangle className="h-6 w-6 text-destructive animate-pulse" />
                ) : (
                    <CheckCircle2 className="h-6 w-6 text-green-500" />
                )}
            </div>

            <div className="grid grid-cols-2 gap-4 pt-4 border-t border-border/50">
                <div>
                    <p className="text-sm font-medium text-muted-foreground mb-1">Health Score</p>
                    <div className="flex items-baseline gap-2">
                        <span className="text-2xl font-bold">{health.toFixed(1)}</span>
                        <span className="text-sm text-muted-foreground">/ 100</span>
                    </div>
                </div>
                <div>
                    <p className="text-sm font-medium text-muted-foreground mb-1">Risk Level</p>
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold
                        ${risk === 'CRITICAL' ? 'bg-destructive/20 text-destructive' : 
                          risk === 'HIGH' ? 'bg-orange-500/20 text-orange-500' :
                          risk === 'MEDIUM' ? 'bg-yellow-500/20 text-yellow-500' :
                          risk === 'LOW' ? 'bg-blue-500/20 text-blue-500' :
                          'bg-green-500/20 text-green-500'}`}>
                        {risk}
                    </span>
                </div>
            </div>
        </div>
    )
}
