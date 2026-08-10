import { useAuth } from '../../context/AuthContext'
import './Dashboard.css'
import StatsOverview from './components/StatsOverview'
import StreakHeatmap from './components/StreakHeatmap'
import ActiveRoadmaps from './components/ActiveRoadmaps'
import QuickUploadWidget from './components/QuickUploadWidget'

const Dashboard = () => {
  const { user } = useAuth()

  return (
    <main className="dashboard">
      <div className="dashboard__container">
        <div className="dashboard__header">
          <h1 className="dashboard__title">Dashboard</h1>
          <p className="dashboard__subtitle">Welcome back, {user?.fullName}! Here&apos;s your learning progress.</p>
        </div>
        <StatsOverview />
        <div className="dashboard__grid">
          <ActiveRoadmaps />
          <div className="dashboard__side">
            <StreakHeatmap />
            <QuickUploadWidget />
          </div>
        </div>
      </div>
    </main>
  )
}

export default Dashboard
