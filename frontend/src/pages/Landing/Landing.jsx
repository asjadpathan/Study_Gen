import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './Landing.module.css';

// SVG Icon Definitions
const Icons = {
  Map: () => (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polygon points="3 6 9 3 15 6 21 3 21 18 15 21 9 18 3 21" />
      <line x1="9" y1="3" x2="9" y2="18" />
      <line x1="15" y1="6" x2="15" y2="21" />
    </svg>
  ),
  Target: () => (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <circle cx="12" cy="12" r="6" />
      <circle cx="12" cy="12" r="2" />
    </svg>
  ),
  TrendingUp: () => (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
      <polyline points="17 6 23 6 23 12" />
    </svg>
  ),
  Bot: () => (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="11" width="18" height="10" rx="2" />
      <circle cx="12" cy="5" r="2" />
      <path d="M12 7v4" />
      <line x1="8" y1="16" x2="8" y2="16.01" />
      <line x1="16" y1="16" x2="16" y2="16.01" />
    </svg>
  ),
  FileText: () => (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      <line x1="16" y1="13" x2="8" y2="13" />
      <line x1="16" y1="17" x2="8" y2="17" />
    </svg>
  ),
  Clock: () => (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <polyline points="12 6 12 12 16 14" />
    </svg>
  ),
  Network: () => (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="16" y="16" width="6" height="6" rx="1" />
      <rect x="2" y="16" width="6" height="6" rx="1" />
      <rect x="9" y="2" width="6" height="6" rx="1" />
      <path d="M5 16v-3a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v3" />
      <path d="M12 12V8" />
    </svg>
  ),
  Zap: () => (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
    </svg>
  ),
  ShieldCheck: () => (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
      <polyline points="9 12 11 14 15 10" />
    </svg>
  ),
  Briefcase: () => (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
      <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
    </svg>
  ),
  ArrowRight: () => (
    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <line x1="5" y1="12" x2="19" y2="12" />
      <polyline points="12 5 19 12 12 19" />
    </svg>
  ),
  Sparkles: () => (
    <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
      <path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17l-6.2 4.3 2.4-7.4L2 9.4h7.6z" />
    </svg>
  ),
};

const STEPS = [
  { step: '01', title: 'Diagnostic Baseline', description: 'Pinpoint exact proficiency gaps through a quick adaptive assessment.' },
  { step: '02', title: 'AI Curriculum Build', description: 'Receive an optimized module sequence mapped to your target role.' },
  { step: '03', title: 'Structured Mastery', description: 'Work through concept maps and contextual AI coaching sessions.' },
  { step: '04', title: 'Remediation Loops', description: 'Target misconceptions with automated error analysis on missed items.' },
  { step: '05', title: 'Role Alignment', description: 'Validate modules against real-world job requirements and benchmarks.' },
];

const FEATURES = [
  {
    icon: <Icons.Bot />,
    title: 'Contextual AI Assistant',
    description: 'Ask questions directly against your active study materials. Grounded answers, no hallucinations — every response is anchored to your actual content.',
  },
  {
    icon: <Icons.Map />,
    title: 'Dynamic Roadmaps',
    description: 'Study schedules mapped to your availability and target deadlines.',
  },
  {
    icon: <Icons.Target />,
    title: 'Diagnostic Testing',
    description: 'Skip material you\'ve already mastered and focus on identified gaps.',
  },
  {
    icon: <Icons.TrendingUp />,
    title: 'Gated Progression',
    description: 'Enforce prerequisite mastery before unlocking higher-order concepts.',
  },
  {
    icon: <Icons.FileText />,
    title: 'Document Parser',
    description: 'Convert PDFs, syllabi, and papers into actionable study guides.',
  },
  {
    icon: <Icons.Clock />,
    title: 'Session Analytics',
    description: 'Track focus, retention cycles, and weekly cadence automatically.',
  },
  {
    icon: <Icons.Network />,
    title: 'Visual Concept Graphs',
    description: 'Inspect relationships and taxonomy trees between modules.',
  },
  {
    icon: <Icons.Zap />,
    title: 'Active Recall Drills',
    description: 'Spaced repetition flashcards tuned to your forgetting curve.',
  },
  {
    icon: <Icons.ShieldCheck />,
    title: 'Error-Driven Remediation',
    description: 'Instant analysis on incorrect answers to fix misunderstandings.',
  },
  {
    icon: <Icons.Briefcase />,
    title: 'Industry Skill Mapping',
    description: 'Map milestones to real engineering and analytical job specs.',
  },
];

const hasToken = () => {
  if (localStorage.getItem('token')) return true;
  try {
    const user = JSON.parse(localStorage.getItem('studygen_user') || '{}');
    return Boolean(user?.token);
  } catch {
    return false;
  }
};

const Landing = () => {
  const navigate = useNavigate();
  const [authChecked, setAuthChecked] = useState(false);

  useEffect(() => {
    if (hasToken()) {
      navigate('/dashboard', { replace: true });
      return;
    }
    setAuthChecked(true);
  }, [navigate]);

  const scrollToHowItWorks = () => {
    document.getElementById('how-it-works')?.scrollIntoView({ behavior: 'smooth' });
  };

  if (!authChecked) return null;

  return (
    <div className={styles.landingPage}>

      {/* ── HERO ────────────────────────────────── */}
      <section className={styles.heroSection}>
        <div className={styles.heroDots} aria-hidden="true" />

        <div className={styles.heroContainer}>
          <div className={styles.heroBadge}>
            <span className={styles.badgePulse} />
            <Icons.Sparkles /> Now with Gemini AI · v2.0
          </div>

          <h1 className={styles.heroTitle}>
            Stop studying blindly.{' '}
            <span className={styles.gradientText}>Learn with precision.</span>
          </h1>

          <p className={styles.heroSubtitle}>
            StudyGen builds your personalized diagnostic roadmap, keeps you on track with spaced recall, 
            and tells you exactly where you're going wrong — before your exam does.
          </p>

          <div className={styles.heroActions}>
            <button
              id="hero-cta-signup"
              className={styles.ctaPrimary}
              onClick={() => navigate('/auth?tab=signup')}
            >
              Start for free <Icons.ArrowRight />
            </button>
            <button
              id="hero-cta-explore"
              className={styles.ctaSecondary}
              onClick={scrollToHowItWorks}
            >
              See how it works
            </button>
          </div>

          <div className={styles.heroMetrics}>
            <div className={styles.metricItem}>
              <span className={styles.metricValue}>100%</span>
              <span className={styles.metricLabel}>Adaptive Curriculum</span>
            </div>
            <div className={styles.metricItem}>
              <span className={styles.metricValue}>60s</span>
              <span className={styles.metricLabel}>Baseline Assessment</span>
            </div>
            <div className={styles.metricItem}>
              <span className={styles.metricValue}>2.4×</span>
              <span className={styles.metricLabel}>Recall Efficiency</span>
            </div>
          </div>
        </div>
      </section>

      {/* ── HOW IT WORKS ────────────────────────── */}
      <section id="how-it-works" className={styles.howItWorksSection}>
        <div className={styles.sectionHeader}>
          <span className={styles.sectionTag}>Methodology</span>
          <h2 className={styles.sectionTitle}>Systematic progress, zero guesswork</h2>
          <p className={styles.sectionSubtitle}>
            A tight feedback loop — from first diagnostic to verified mastery.
          </p>
        </div>

        <div className={styles.stepsRail}>
          {STEPS.map((step) => (
            <div key={step.step} className={styles.stepCard}>
              <div className={styles.stepHeader}>
                <span className={styles.stepNumber}>{step.step}</span>
              </div>
              <h3 className={styles.stepTitle}>{step.title}</h3>
              <p className={styles.stepDescription}>{step.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── FEATURES ────────────────────────────── */}
      <section id="features" className={styles.featuresSection}>
        <div className={styles.sectionHeader}>
          <span className={styles.sectionTag}>Platform Capabilities</span>
          <h2 className={styles.sectionTitle}>Built for demanding subjects</h2>
          <p className={styles.sectionSubtitle}>
            Every tool you need to analyze, absorb, and retain complex technical knowledge.
          </p>
        </div>

        <div className={styles.featuresGrid}>
          {FEATURES.map((feature, idx) => (
            <div key={idx} className={styles.featureCard}>
              <div className={styles.iconCircle}>{feature.icon}</div>
              <div className={styles.featureContent}>
                <h3 className={styles.featureTitle}>{feature.title}</h3>
                <p className={styles.featureDescription}>{feature.description}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ── CTA BAND ────────────────────────────── */}
      <section className={styles.ctaBandSection}>
        <div className={styles.ctaBandContainer}>
          <h2 className={styles.ctaBandTitle}>
            Your custom curriculum,{' '}
            <span className={styles.ctaBandTitleGradient}>built in minutes.</span>
          </h2>
          <p className={styles.ctaBandSubtitle}>
            Take the 2-minute diagnostic and unlock a targeted study roadmap — completely free.
          </p>
          <div className={styles.ctaBandAction}>
            <button
              id="cta-band-signup"
              className={styles.ctaBandPrimary}
              onClick={() => navigate('/auth?tab=signup')}
            >
              Get started now <Icons.ArrowRight />
            </button>
          </div>
        </div>
      </section>

    </div>
  );
};

export default Landing;