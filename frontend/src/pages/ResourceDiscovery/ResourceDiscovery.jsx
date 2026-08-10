import './ResourceDiscovery.css'
import ResourceFeed from './components/ResourceFeed'
import SavedLibrary from './components/SavedLibrary'
import CategoryFilter from './components/CategoryFilter'

const ResourceDiscovery = () => {
  return (
    <main className="">
      <div className="__container">
        <h1>Resource Discovery</h1>
        <ResourceFeed />
        <SavedLibrary />
        <CategoryFilter />
      </div>
    </main>
  )
}

export default ResourceDiscovery

