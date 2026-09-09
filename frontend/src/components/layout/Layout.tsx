import { ReactNode } from 'react'
import { NavLink } from 'react-router-dom'
import { Activity, Server, AlertTriangle, LayoutDashboard, Settings } from 'lucide-react'
import { useSSE } from '../../hooks/useSSE'

export function Layout({ children }: { children: ReactNode }) {
    const { status } = useSSE()

    const navLinkClass = ({ isActive }: { isActive: boolean }) =>
        `flex items-center gap-3 px-3 py-2 rounded-md font-medium transition-colors ${
            isActive ? 'bg-secondary text-secondary-foreground' : 'text-muted-foreground hover:bg-secondary/50 hover:text-foreground'
        }`

    return (
        <div className="flex h-screen w-full bg-background text-foreground overflow-hidden">
            {/* Sidebar */}
            <aside className="w-64 border-r bg-card flex flex-col">
                <div className="h-16 flex items-center px-6 border-b">
                    <Activity className="h-6 w-6 text-primary mr-2" />
                    <span className="font-bold text-lg tracking-tight">Predictive Maint</span>
                </div>
                <nav className="flex-1 py-4 flex flex-col gap-2 px-4">
                    <NavLink to="/dashboard" className={navLinkClass}>
                        <LayoutDashboard className="h-4 w-4" />
                        Dashboard
                    </NavLink>
                    <NavLink to="/machines" className={navLinkClass}>
                        <Server className="h-4 w-4" />
                        Machines
                    </NavLink>
                    <NavLink to="/alerts" className={navLinkClass}>
                        <AlertTriangle className="h-4 w-4" />
                        Alerts
                    </NavLink>
                    <NavLink to="/settings" className={navLinkClass + " mt-auto"}>
                        <Settings className="h-4 w-4" />
                        Settings
                    </NavLink>
                </nav>
                <div className="p-4 border-t border-border/50 text-sm flex items-center justify-between text-muted-foreground">
                    <span>System Status</span>
                    <div className="flex items-center gap-2">
                        <span className="text-xs uppercase font-bold">{status}</span>
                        <div className={`h-2 w-2 rounded-full ${status === 'connected' ? 'bg-green-500' : status === 'connecting' ? 'bg-yellow-500' : 'bg-red-500'}`}></div>
                    </div>
                </div>
            </aside>

            {/* Main Content */}
            <main className="flex-1 flex flex-col min-w-0 overflow-hidden">
                <header className="h-16 border-b bg-card/50 backdrop-blur flex items-center px-6 justify-between">
                    <h1 className="text-xl font-semibold">Live Monitoring</h1>
                    <div className="flex items-center gap-4">
                        <div className="flex items-center gap-2 text-sm text-muted-foreground">
                            <div className="h-2 w-2 rounded-full bg-green-500 animate-pulse"></div>
                            Receiving Live Telemetry
                        </div>
                    </div>
                </header>
                <div className="flex-1 overflow-auto p-6">
                    {children}
                </div>
            </main>
        </div>
    )
}
