import './GenerateRoadmap.css'
import GoalInputForm from './components/GoalInputForm'
import JobDescriptionInput from './components/JobDescriptionInput'
import PreferencesSelector from './components/PreferencesSelector'

const GenerateRoadmap = () => {
  return (
    <main className="">
      <div className="__container">
        <h1>Generate Roadmap</h1>
        <GoalInputForm />
        <JobDescriptionInput />
        <PreferencesSelector />
      </div>
    </main>
  )
}

export default GenerateRoadmap

