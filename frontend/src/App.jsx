import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import LandingPage from './pages/LandingPage/LandingPage'
import Auth from './pages/Auth/Auth'
import Dashboard from './pages/Dashboard/Dashboard'
import GenerateRoadmap from './pages/GenerateRoadmap/GenerateRoadmap'
import DiagnosticTest from './pages/DiagnosticTest/DiagnosticTest'
import RoadmapView from './pages/RoadmapView/RoadmapView'
import RoadmapWorkspace from './pages/RoadmapWorkspace/RoadmapWorkspace'
import DocumentAnalyzer from './pages/DocumentAnalyzer/DocumentAnalyzer'
import StudyRoom from './pages/StudyRoom/StudyRoom'
import ResourceDiscovery from './pages/ResourceDiscovery/ResourceDiscovery'
import CommunityZone from './pages/CommunityZone/CommunityZone'
import UserProfile from './pages/UserProfile/UserProfile'
import './App.css'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Navbar />
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/auth" element={<Auth />} />
          <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
          <Route path="/generate-roadmap" element={<ProtectedRoute><GenerateRoadmap /></ProtectedRoute>} />
          <Route path="/diagnostic-test" element={<ProtectedRoute><DiagnosticTest /></ProtectedRoute>} />
          <Route path="/roadmap-view" element={<ProtectedRoute><RoadmapView /></ProtectedRoute>} />
          <Route path="/roadmap-workspace" element={<ProtectedRoute><RoadmapWorkspace /></ProtectedRoute>} />
          <Route path="/document-analyzer" element={<ProtectedRoute><DocumentAnalyzer /></ProtectedRoute>} />
          <Route path="/study-room" element={<ProtectedRoute><StudyRoom /></ProtectedRoute>} />
          <Route path="/resources" element={<ProtectedRoute><ResourceDiscovery /></ProtectedRoute>} />
          <Route path="/community" element={<ProtectedRoute><CommunityZone /></ProtectedRoute>} />
          <Route path="/profile" element={<ProtectedRoute><UserProfile /></ProtectedRoute>} />
        </Routes>
        <Footer />
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
