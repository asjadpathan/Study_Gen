import './RoadmapWorkspace.css'
import ConceptExplanation from './components/ConceptExplanation'
import ConceptQuiz from './components/ConceptQuiz'
import FlashcardsDeck from './components/FlashcardsDeck'
import AIChatbotDrawer from './components/AIChatbotDrawer'

const RoadmapWorkspace = () => {
  return (
    <main className="">
      <div className="__container">
        <h1>Roadmap Workspace</h1>
        <ConceptExplanation />
        <ConceptQuiz />
        <FlashcardsDeck />
        <AIChatbotDrawer />
      </div>
    </main>
  )
}

export default RoadmapWorkspace

