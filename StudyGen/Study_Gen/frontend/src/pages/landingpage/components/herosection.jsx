import './HeroSection.css'

const HeroSection = () => {
  return (
    <section className="hero">
      <div className="hero__container">
        <div className="hero__content">
          <p className="hero__eyebrow">Your AI-Powered</p>
          <h1 className="hero__title">
            <span className="hero__title-highlight">Personalized Learning</span>
            <br />
            Companion
          </h1>
          <p className="hero__description">
            Master any skill with custom roadmaps, intelligent summarization, and a
            community of lifelong learners. Experience the future of education today.
          </p>
          <div className="hero__actions">
            <a href="#get-started" className="hero__btn hero__btn--primary">
              Get Started
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path d="M3.333 8h9.334M8.667 4l4 4-4 4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </a>
            <a href="#signin" className="hero__btn hero__btn--outline">
              Sign In
            </a>
          </div>
        </div>

        <div className="hero__visual">
          <div className="hero__card">
            <div className="hero__card-badge">
              <span className="hero__card-badge-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </span>
              <div className="hero__card-badge-text">
                <span className="hero__card-badge-label">AI THINKING</span>
                <span className="hero__card-badge-sub">Curating your path...</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

export default HeroSection
