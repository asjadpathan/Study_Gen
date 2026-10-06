import HeroSection from './components/HeroSection'
import FeaturesSection from './components/FeaturesSection'
import StatsSection from './components/StatsSection'
import './LandingPage.css'

const LandingPage = () => {
  return (
    <main className="landing-page">
      <HeroSection />
      <StatsSection />
      <FeaturesSection />
    </main>
  )
}

export default LandingPage
