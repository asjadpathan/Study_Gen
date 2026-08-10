import './StudyRoom.css'
import PomodoroTimer from './components/PomodoroTimer'
import AmbientAudioPlayer from './components/AmbientAudioPlayer'
import ProductivityAnalytics from './components/ProductivityAnalytics'

const StudyRoom = () => {
  return (
    <main className="">
      <div className="__container">
        <h1>Study Room</h1>
        <PomodoroTimer />
        <AmbientAudioPlayer />
        <ProductivityAnalytics />
      </div>
    </main>
  )
}

export default StudyRoom

