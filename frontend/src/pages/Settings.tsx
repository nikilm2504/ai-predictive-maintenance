import { useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { Activity, Database, Server, Zap, Cpu, CheckCircle2, XCircle } from 'lucide-react'
import { useSSE } from '../hooks/useSSE'

const fetchSpringHealth = async () => {
    const { data } = await axios.get('http://localhost:8080/actuator/health')
    return data
}

const fetchMlHealth = async () => {
    const { data } = await axios.get('http://localhost:8000/health')
    return data
}

export function Settings() {
    const { status: sseStatus } = useSSE()
    const { data: springHealth, isError: isSpringError } = useQuery({ queryKey: ['springHealth'], queryFn: fetchSpringHealth })
    const { data: mlHealth, isError: isMlError } = useQuery({ queryKey: ['mlHealth'], queryFn: fetchMlHealth })

    return (
        <div className="space-y-6 max-w-4xl">
            <h2 className="text-2xl font-bold tracking-tight">System Status & Settings</h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                
                {/* Integration Status */}
                <div className="border rounded-xl bg-card shadow-sm overflow-hidden">
                    <div className="border-b bg-secondary/30 px-5 py-4 font-semibold text-lg flex items-center gap-2">
                        <Activity className="h-5 w-5 text-primary" />
                        Integration Health
                    </div>
                    <div className="p-0">
                        <StatusRow 
                            icon={<Server className="h-4 w-4" />}
                            name="Spring Boot Backend" 
                            status={isSpringError ? 'DOWN' : springHealth?.status ?? 'LOADING'} 
                        />
                        <StatusRow 
                            icon={<Database className="h-4 w-4" />}
                            name="PostgreSQL Database" 
                            status={isSpringError ? 'DOWN' : springHealth?.components?.db?.status ?? 'LOADING'} 
                        />
                        <StatusRow 
                            icon={<Cpu className="h-4 w-4" />}
                            name="Python ML Service" 
                            status={isMlError ? 'DOWN' : mlHealth?.status === 'ok' ? 'UP' : 'LOADING'} 
                            extra={mlHealth?.model_version ? `(Model: ${mlHealth.model_version})` : ''}
                        />
                        <StatusRow 
                            icon={<Zap className="h-4 w-4" />}
                            name="Real-Time SSE Connection" 
                            status={sseStatus === 'connected' ? 'UP' : sseStatus === 'connecting' ? 'LOADING' : 'DOWN'} 
                        />
                    </div>
                </div>

                {/* App Info */}
                <div className="border rounded-xl bg-card shadow-sm overflow-hidden">
                    <div className="border-b bg-secondary/30 px-5 py-4 font-semibold text-lg flex items-center gap-2">
                        <InfoIcon className="h-5 w-5 text-primary" />
                        Application Information
                    </div>
                    <div className="p-5 space-y-4 text-sm">
                        <div className="flex justify-between border-b border-border/50 pb-2">
                            <span className="text-muted-foreground">Application</span>
                            <span className="font-medium">Predictive Maintenance OS</span>
                        </div>
                        <div className="flex justify-between border-b border-border/50 pb-2">
                            <span className="text-muted-foreground">Frontend Version</span>
                            <span className="font-medium font-mono">v1.1.0-dashboard</span>
                        </div>
                        <div className="flex justify-between border-b border-border/50 pb-2">
                            <span className="text-muted-foreground">ML Model Version</span>
                            <span className="font-medium font-mono">{mlHealth?.model_version ?? 'Not available'}</span>
                        </div>
                        <div className="flex justify-between pb-2">
                            <span className="text-muted-foreground">API Target</span>
                            <span className="font-medium font-mono">http://localhost:8080/api/v1</span>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    )
}

function StatusRow({ name, status, icon, extra }: { name: string, status: string, icon: React.ReactNode, extra?: string }) {
    const isUp = status === 'UP'
    const isLoading = status === 'LOADING'

    return (
        <div className="flex items-center justify-between p-4 border-b last:border-0 border-border/50 hover:bg-muted/30 transition-colors">
            <div className="flex items-center gap-3">
                <div className="text-muted-foreground">{icon}</div>
                <div className="font-medium text-sm text-foreground">{name} {extra && <span className="text-xs text-muted-foreground font-normal ml-1">{extra}</span>}</div>
            </div>
            <div className="flex items-center gap-2">
                {isLoading ? (
                    <div className="h-4 w-4 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
                ) : isUp ? (
                    <CheckCircle2 className="h-5 w-5 text-green-500" />
                ) : (
                    <XCircle className="h-5 w-5 text-destructive" />
                )}
                <span className={`text-xs font-bold ${isUp ? 'text-green-500' : isLoading ? 'text-muted-foreground' : 'text-destructive'}`}>
                    {status}
                </span>
            </div>
        </div>
    )
}

function InfoIcon(props: any) {
    return (
        <svg
            {...props}
            xmlns="http://www.w3.org/2000/svg"
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <circle cx="12" cy="12" r="10" />
            <path d="M12 16v-4" />
            <path d="M12 8h.01" />
        </svg>
    )
}
