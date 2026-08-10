import './DiagnosticTest.css'
import QuizCard from './components/QuizCard'
import ScoreSummary from './components/ScoreSummary'
import QuestionProgress from './components/QuestionProgress'

const DiagnosticTest = () => {
  return (
    <main className="">
      <div className="__container">
        <h1>Diagnostic Test</h1>
        <QuizCard />
        <ScoreSummary />
        <QuestionProgress />
      </div>
    </main>
  )
}

export default DiagnosticTest

