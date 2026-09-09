import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Activity, Thermometer, Zap, Settings2 } from 'lucide-react'
import { getMachine, getLatestTelemetry, getLatestPrediction, getActiveAlerts, getExplanations, getTelemetryHistory } from '../services/api'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, ResponsiveContainer, BarChart, Bar, Cell } from 'recharts'

export function MachineDetail({ machineId, onBack }: { machineId: string, onBack: () => void }) {
    const { data: machine } = useQuery({ queryKey: ['machine', machineId], queryFn: () => getMachine(machineId) })
    const { data: telemetry } = useQuery({ queryKey: ['latestTelemetry', machineId], queryFn: () => getLatestTelemetry(machineId) })
    const { data: prediction } = useQuery({ queryKey: ['latestPrediction', machineId], queryFn: () => getLatestPrediction(machineId) })
    const { data: alerts } = useQuery({ queryKey: ['alerts', machineId], queryFn: getActiveAlerts })
    const { data: telemetryHistory } = useQuery({ queryKey: ['telemetryHistory', machineId], queryFn: () => getTelemetryHistory(machineId) })
    const { data: explanations } = useQuery({ 
        queryKey: ['latestExplanations', machineId], 
        queryFn: () => getExplanations(machineId, prediction?.id || ''),
        enabled: !!prediction?.id
    })

    const activeAlert = alerts?.find(a => a.machineId === machineId)
    const risk = prediction?.riskLevel ?? 'UNKNOWN'

    if (!machine) return <div className="animate-pulse">Loading machine details...</div>

    return (
        <div className="space-y-6">
            <button onClick={onBack} className="flex items-center text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
                <ArrowLeft className="h-4 w-4 mr-2" /> Back to Dashboard
            </button>

            <div className="flex items-start justify-between">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">{machine.machineName}</h2>
                    <p className="text-muted-foreground mt-1">{machine.machineCode} • {machine.location}</p>
                </div>
                {activeAlert && (
                    <div className="bg-destructive/10 border border-destructive/20 text-destructive px-4 py-3 rounded-lg max-w-md">
                        <h4 className="font-bold text-sm flex items-center gap-2">
                            <Activity className="h-4 w-4" /> 
                            {activeAlert.title}
                        </h4>
                        <p className="text-sm mt-1 opacity-90">{activeAlert.description}</p>
                    </div>
                )}
            </div>

            {/* Top Stats */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard title="Health Score" value={prediction?.healthScore?.toFixed(1) ?? '---'} suffix="/ 100" />
                <StatCard title="Failure Probability" value={`${((prediction?.failureProbability ?? 0) * 100).toFixed(1)}%`} />
                <StatCard title="Risk Level" value={risk} valueClassName={
                    risk === 'CRITICAL' ? 'text-destructive' : 
                    risk === 'HIGH' ? 'text-orange-500' :
                    risk === 'MEDIUM' ? 'text-yellow-500' :
                    risk === 'LOW' ? 'text-blue-500' : 'text-green-500'
                } />
                <StatCard title="Model Version" value={prediction?.modelVersion ?? '---'} className="text-sm" />
            </div>

            {/* Live Telemetry */}
            <h3 className="text-xl font-semibold mt-8 mb-4">Live Telemetry</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <SensorCard title="Vibration" value={telemetry?.vibration} unit="mm/s" icon={<Activity className="h-5 w-5 text-blue-500"/>} />
                <SensorCard title="Temperature" value={telemetry?.temperature} unit="°C" icon={<Thermometer className="h-5 w-5 text-orange-500"/>} />
                <SensorCard title="Current" value={telemetry?.current} unit="A" icon={<Zap className="h-5 w-5 text-yellow-500"/>} />
                <SensorCard title="RPM" value={telemetry?.rpm} unit="rpm" icon={<Settings2 className="h-5 w-5 text-slate-500"/>} />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-8">
                {/* Telemetry History Chart */}
                <div className="border rounded-xl bg-card p-6 shadow-sm">
                    <h3 className="font-semibold mb-4 text-lg">Vibration History</h3>
                    <div className="h-64">
                        <ResponsiveContainer width="100%" height="100%">
                            <LineChart data={telemetryHistory?.content?.slice().reverse() || []}>
                                <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                                <XAxis dataKey="timestamp" tickFormatter={(timestamp) => new Date(timestamp).toLocaleTimeString()} stroke="#888" fontSize={12} />
                                <YAxis stroke="#888" fontSize={12} />
                                <RechartsTooltip contentStyle={{backgroundColor: '#111', borderColor: '#333'}} />
                                <Line type="monotone" dataKey="vibration" stroke="#3b82f6" strokeWidth={2} dot={false} isAnimationActive={false} />
                            </LineChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* SHAP Explanations */}
                <div className="border rounded-xl bg-card p-6 shadow-sm">
                    <h3 className="font-semibold mb-4 text-lg">AI Risk Contributors (SHAP)</h3>
                    {explanations && explanations.length > 0 ? (
                        <div className="h-64">
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart layout="vertical" data={explanations} margin={{ left: 50 }}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="#333" horizontal={true} vertical={false} />
                                    <XAxis type="number" stroke="#888" fontSize={12} />
                                    <YAxis dataKey="featureName" type="category" stroke="#888" fontSize={12} />
                                    <RechartsTooltip contentStyle={{backgroundColor: '#111', borderColor: '#333'}} />
                                    <Bar dataKey="shapValue" isAnimationActive={false}>
                                        {explanations.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={entry.direction === 'INCREASES_RISK' ? '#ef4444' : '#22c55e'} />
                                        ))}
                                    </Bar>
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    ) : (
                        <div className="h-64 flex items-center justify-center text-muted-foreground border border-dashed rounded-lg">
                            No explanation data available
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

function StatCard({ title, value, suffix = "", valueClassName = "", className = "" }: { title: string, value: string | number, suffix?: string, valueClassName?: string, className?: string }) {
    return (
        <div className="border rounded-xl bg-card p-5 shadow-sm">
            <h4 className="text-sm font-medium text-muted-foreground mb-2">{title}</h4>
            <div className="flex items-baseline gap-1">
                <span className={`text-3xl font-bold ${valueClassName} ${className}`}>{value}</span>
                {suffix && <span className="text-sm font-medium text-muted-foreground">{suffix}</span>}
            </div>
        </div>
    )
}

function SensorCard({ title, value, unit, icon }: { title: string, value?: number, unit: string, icon: React.ReactNode }) {
    return (
        <div className="border rounded-xl bg-card p-5 shadow-sm flex items-center gap-4">
            <div className="p-3 bg-secondary rounded-lg">
                {icon}
            </div>
            <div>
                <h4 className="text-sm font-medium text-muted-foreground">{title}</h4>
                <div className="flex items-baseline gap-1 mt-1">
                    <span className="text-2xl font-bold font-mono">{value !== undefined ? value.toFixed(2) : '---'}</span>
                    <span className="text-sm font-medium text-muted-foreground">{unit}</span>
                </div>
            </div>
        </div>
    )
}
