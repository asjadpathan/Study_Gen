import './FeaturesSection.css'

const FeaturesSection = () => {
  return (
    <section className="features" id="features">
      <div className="features__container">
        <div className="features__heading">
          <h2 className="features__title">Intelligent Features for Modern Minds</h2>
          <p className="features__subtitle">
            Our AI engine works around the clock to organize information into actionable learning
            experiences.
          </p>
        </div>

        <div className="features__grid">
          <article className="fcard fcard--wide">
            <div className="fcard__icon fcard__icon--blue">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M4 4v16M4 8h4a4 4 0 010 8H4M14 4v16M14 4h4a4 4 0 010 8h-4" stroke="#2563eb" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
            <h3 className="fcard__title">AI Roadmap Generation</h3>
            <p className="fcard__desc">
              Go from zero to expert with paths tailored to your specific goals, previous knowledge,
              and available time.
            </p>
            <div className="fcard__progress-wrap">
              <div className="fcard__progress-bar">
                <div className="fcard__progress-fill" style={{ width: '65%' }} />
              </div>
              <span className="fcard__progress-label">65% Progress</span>
            </div>
            <div className="fcard__dots">
              <span className="fcard__dot fcard__dot--filled" />
              <span className="fcard__dot fcard__dot--filled fcard__dot--light" />
              <span className="fcard__dot" />
            </div>
            <div className="fcard__milestone">
              <span>&quot;Next milestone: Mastering React Hooks&quot;</span>
            </div>
          </article>

          <article className="fcard fcard--tall">
            <div className="fcard__icon fcard__icon--blue">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zM23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" stroke="#2563eb" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
            <h3 className="fcard__title">Community Zone</h3>
            <p className="fcard__desc">
              Learn alongside others, share your roadmaps, and find study buddies in a global
              ecosystem.
            </p>
            <div className="fcard__avatars">
              <div className="fcard__avatar-stack">
                <img className="fcard__avatar" src="https://i.pravatar.cc/40?img=1" alt="User" />
                <img className="fcard__avatar" src="https://i.pravatar.cc/40?img=2" alt="User" />
                <img className="fcard__avatar" src="https://i.pravatar.cc/40?img=3" alt="User" />
                <span className="fcard__avatar fcard__avatar--count">+2k</span>
              </div>
              <span className="fcard__avatar-label">Join 400+ online now</span>
            </div>
          </article>

          <article className="fcard fcard--half">
            <div className="fcard__icon fcard__icon--blue">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" stroke="#2563eb" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
                <polyline points="14,2 14,8 20,8" stroke="#2563eb" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
                <line x1="16" y1="13" x2="8" y2="13" stroke="#2563eb" strokeWidth="2.2" strokeLinecap="round" />
                <line x1="16" y1="17" x2="8" y2="17" stroke="#2563eb" strokeWidth="2.2" strokeLinecap="round" />
              </svg>
            </div>
            <div>
              <h3 className="fcard__title">Document Summarization</h3>
              <p className="fcard__desc">
                Upload any PDF or document and get core insights and flashcards in seconds.
              </p>
            </div>
          </article>

          <article className="fcard fcard--half">
            <div className="fcard__icon fcard__icon--blue">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2" stroke="#2563eb" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
                <line x1="8" y1="21" x2="16" y2="21" stroke="#2563eb" strokeWidth="2.2" strokeLinecap="round" />
                <line x1="12" y1="17" x2="12" y2="21" stroke="#2563eb" strokeWidth="2.2" strokeLinecap="round" />
              </svg>
            </div>
            <div>
              <h3 className="fcard__title">Job Description Matching</h3>
              <p className="fcard__desc">
                See exactly how your skills stack up against your dream role and what&apos;s missing.
              </p>
            </div>
          </article>
        </div>
      </div>
    </section>
  )
}

export default FeaturesSection
