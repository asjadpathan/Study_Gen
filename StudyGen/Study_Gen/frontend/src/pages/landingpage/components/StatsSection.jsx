import './StatsSection.css'

const stats = [
  { value: '10k+', label: 'ROADMAPS GENERATED', accent: false },
  { value: '98%', label: 'COMPLETION RATE', accent: true },
  { value: '50k+', label: 'ACTIVE LEARNERS', accent: false },
]

const StatsSection = () => {
  return (
    <section className="stats">
      <div className="stats__container">
        <div className="stats__grid">
          {stats.map((item) => (
            <div key={item.label} className="stats__item">
              <span className={`stats__value ${item.accent ? 'stats__value--accent' : ''}`}>
                {item.value}
              </span>
              <span className="stats__label">{item.label}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

export default StatsSection
