import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { getMachines, getLatestPrediction } from '../services/api'
import { Machine } from '../types'

export function MachinesList() {
    const { data: machines, isLoading, isError } = useQuery({ queryKey: ['machines'], queryFn: getMachines })
    const navigate = useNavigate()

    if (isLoading) return <div className="text-muted-foreground animate-pulse p-6">Loading machines database...</div>
    if (isError) return <div className="text-destructive p-6">Failed to load machines.</div>

    return (
        <div className="space-y-6">
            <h2 className="text-2xl font-bold tracking-tight">Machine Fleet</h2>
            <div className="rounded-md border">
                <table className="w-full text-sm text-left">
                    <thead className="bg-secondary/50 text-muted-foreground border-b">
                        <tr>
                            <th className="px-4 py-3 font-medium">Machine</th>
                            <th className="px-4 py-3 font-medium">Location</th>
                            <th className="px-4 py-3 font-medium">Status</th>
                            <th className="px-4 py-3 font-medium">Health</th>
                            <th className="px-4 py-3 font-medium">Risk Level</th>
                            <th className="px-4 py-3 font-medium text-right">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {machines?.map(machine => (
                            <MachineRow key={machine.id} machine={machine} onClick={() => navigate(`/machines/${machine.id}`)} />
                        ))}
                    </tbody>
                </table>
                {(!machines || machines.length === 0) && (
                    <div className="p-8 text-center text-muted-foreground">No machines found in the database.</div>
                )}
            </div>
        </div>
    )
}

function MachineRow({ machine, onClick }: { machine: Machine, onClick: () => void }) {
    const { data: prediction } = useQuery({ 
        queryKey: ['latestPrediction', machine.id], 
        queryFn: () => getLatestPrediction(machine.id) 
    })
    
    const risk = prediction?.riskLevel ?? 'UNKNOWN'
    const health = prediction?.healthScore ?? 100

    return (
        <tr className="border-b last:border-0 hover:bg-muted/50 transition-colors cursor-pointer" onClick={onClick}>
            <td className="px-4 py-3">
                <div className="font-medium text-foreground">{machine.machineName}</div>
                <div className="text-xs text-muted-foreground">{machine.machineCode} • {machine.machineType}</div>
            </td>
            <td className="px-4 py-3 text-muted-foreground">{machine.location}</td>
            <td className="px-4 py-3">
                <span className={`inline-flex items-center gap-1.5 px-2 py-1 rounded-md text-xs font-medium bg-secondary text-secondary-foreground`}>
                    <div className={`h-1.5 w-1.5 rounded-full ${machine.status === 'ACTIVE' ? 'bg-green-500' : 'bg-yellow-500'}`} />
                    {machine.status}
                </span>
            </td>
            <td className="px-4 py-3 font-medium">
                {prediction ? health.toFixed(1) : '---'}
            </td>
            <td className="px-4 py-3">
                <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold
                    ${risk === 'CRITICAL' ? 'bg-destructive/20 text-destructive' : 
                      risk === 'HIGH' ? 'bg-orange-500/20 text-orange-500' :
                      risk === 'MEDIUM' ? 'bg-yellow-500/20 text-yellow-500' :
                      risk === 'LOW' ? 'bg-blue-500/20 text-blue-500' :
                      risk === 'HEALTHY' ? 'bg-green-500/20 text-green-500' :
                      'bg-secondary text-secondary-foreground'}`}>
                    {risk}
                </span>
            </td>
            <td className="px-4 py-3 text-right">
                <button className="text-sm font-medium text-primary hover:underline">View Details</button>
            </td>
        </tr>
    )
}
