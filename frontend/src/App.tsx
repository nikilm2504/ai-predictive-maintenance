import { BrowserRouter as Router, Routes, Route, Navigate, useNavigate, useParams } from 'react-router-dom'
import { Layout } from './components/layout/Layout'
import { Dashboard } from './pages/Dashboard'
import { MachineDetail } from './pages/MachineDetail'
import { MachinesList } from './pages/MachinesList'
import { Alerts } from './pages/Alerts'
import { Settings } from './pages/Settings'

function MachineDetailRoute() {
    const { machineId } = useParams()
    const navigate = useNavigate()
    if (!machineId) return <Navigate to="/dashboard" />
    
    return <MachineDetail machineId={machineId} onBack={() => navigate(-1)} />
}

function App() {
  return (
    <div className="dark">
      <Router>
        <Layout>
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/machines" element={<MachinesList />} />
            <Route path="/machines/:machineId" element={<MachineDetailRoute />} />
            <Route path="/alerts" element={<Alerts />} />
            <Route path="/settings" element={<Settings />} />
          </Routes>
        </Layout>
      </Router>
    </div>
  )
}

export default App
