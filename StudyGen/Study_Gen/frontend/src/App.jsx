import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import PublicLayout from './components/PublicLayout'
import AppShell from './layouts/AppShell'
import Landing from './pages/Landing'
import Auth from './pages/Auth'
import Dashboard from './pages/Dashboard/Dashboard'
import GenerateRoadmap from './pages/GenerateRoadmap/GenerateRoadmap'
import DiagnosticTest from './pages/DiagnosticTest/DiagnosticTest'
import RoadmapView from './pages/RoadmapView/RoadmapView'
import RoadmapsList from './pages/RoadmapsList'
import DocumentAnalyzer from './pages/DocumentAnalyzer/DocumentAnalyzer'
import StudyRoom from './pages/StudyRoom'
import Resources from './pages/Resources'
import Community from './pages/Community'
import Profile from './pages/Profile'
import Locked from './pages/Locked'
import NotFound from './pages/NotFound'
import ModuleStudy from './pages/ModuleStudy'
import ModuleTest from './pages/ModuleTest'
import Remediation from './pages/Remediation'
import ModuleDrills from './pages/ModuleDrills'
import PracticeHub from './pages/PracticeHub'
import PracticeSession from './pages/PracticeSession'
import Documents from './pages/Documents'
import DocumentDetail from './pages/DocumentDetail'
import ChatPage from './pages/ChatPage'
import Career from './pages/Career'
import './App.css'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Routes with Shared Navbar & Footer Layout */}
          <Route element={<PublicLayout />}>
            <Route path="/" element={<Landing />} />
            <Route path="/auth" element={<Auth />} />
          </Route>

          {/* Protected Application Routes wrapped in AppShell & ProtectedRoute */}
          <Route
            element={
              <ProtectedRoute>
                <AppShell />
              </ProtectedRoute>
            }
          >
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/generate-roadmap" element={<GenerateRoadmap />} />
            <Route path="/diagnostic-test" element={<DiagnosticTest />} />
            <Route path="/roadmap-view" element={<RoadmapView />} />
            <Route path="/roadmaps/:id" element={<RoadmapView />} />
            <Route path="/roadmap-workspace" element={<RoadmapsList />} />
            <Route path="/roadmaps" element={<RoadmapsList />} />
            <Route path="/modules/:id/test" element={<ModuleTest />} />
            <Route path="/modules/:id/remediation" element={<Remediation />} />
            <Route path="/modules/:id/drills" element={<ModuleDrills />} />
            <Route path="/modules/:id" element={<ModuleStudy />} />
            <Route path="/practice" element={<PracticeHub />} />
            <Route path="/practice-hub" element={<PracticeHub />} />
            <Route path="/practice/session" element={<PracticeSession />} />
            <Route path="/documents" element={<Documents />} />
            <Route path="/documents/:id" element={<DocumentDetail />} />
            <Route path="/document-analyzer" element={<Documents />} />
            <Route path="/chat" element={<ChatPage />} />
            <Route path="/study-room" element={<StudyRoom />} />
            <Route path="/career" element={<Career />} />
            <Route path="/resources" element={<Resources />} />
            <Route path="/community" element={<Community />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/locked" element={<Locked />} />
            <Route path="*" element={<NotFound />} />
          </Route>

          {/* Fallback for unauthenticated routes */}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
