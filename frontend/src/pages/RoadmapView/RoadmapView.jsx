import './RoadmapView.css'
import D3GraphCanvas from './components/D3GraphCanvas'
import RoadmapHeader from './components/RoadmapHeader'
import ModulesList from './components/ModulesList'

const RoadmapView = () => {
  return (
    <main className="">
      <div className="__container">
        <h1>Roadmap View</h1>
        <D3GraphCanvas />
        <RoadmapHeader />
        <ModulesList />
      </div>
    </main>
  )
}

export default RoadmapView

