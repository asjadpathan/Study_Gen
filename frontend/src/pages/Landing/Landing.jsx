import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../../components/Button';
import Card from '../../components/Card';
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
      <polyline points="10 9 9 9 8 9" />
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
  ShieldAlert: () => (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
      <line x1="12" y1="8" x2="12" y2="12" />
      <line x1="12" y1="16" x2="12.01" y2="16" />
    </svg>
  ),
  Briefcase: () => (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
      <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
    </svg>
  ),
  ArrowRight: () => (
    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="5" y1="12" x2="19" y2="12" />
      <polyline points="12 5 19 12 12 19" />
    </svg>
  )
};

const STEPS = [
  {
    step: '01',
    title: 'Diagnostic Baseline',
    description: 'Pinpoint exact proficiency gaps and baseline competency through a quick, adaptive assessment.'
  },
  {
    step: '02',
    title: 'Curriculum Generation',
    description: 'Receive an optimized sequence of modules dynamically mapped to target roles and skill expectations.'
  },
  {
    step: '03',
    title: 'Structured Mastery',
    description: 'Work through interactive concept maps, structured breakdowns, and contextual AI coaching.'
  },
  {
    step: '04',
    title: 'Remediation Loops',
    description: 'Target edge cases and misconceptions with automated error explanations on missed quiz items.'
  },
  {
    step: '05',
    title: 'Role Alignment',
    description: 'Validate completed modules against real-world job requirements and industry benchmarks.'
  }
];

const FEATURES = [
  {
    icon: <Icons.Map />,
    title: 'Dynamic Roadmaps',
    description: 'Study schedules mapped to your availability, target deadlines, and baseline speed.'
  },
  {
    icon: <Icons.Target />,
    title: 'Diagnostic Testing',
    description: 'Skip material you have already mastered and focus strictly on identified skill deficits.'
  },
  {
    icon: <Icons.TrendingUp />,
    title: 'Gated Progression',
    description: 'Enforce prerequisite understanding before unlocking higher-order concepts.'
  },
  {
    icon: <Icons.Bot />,
    title: 'Contextual AI Assistant',
    description: 'Ask questions directly against your active study materials with grounded, hallucination-resistant answers.'
  },
  {
    icon: <Icons.FileText />,
    title: 'Document Parser',
    description: 'Convert lecture slide PDFs, syllabi, and technical papers into actionable study guides.'
  },
  {
    icon: <Icons.Clock />,
    title: 'Session Analytics',
    description: 'Track active study focus, retention cycles, and weekly cadence without manual logging.'
  },
  {
    icon: <Icons.Network />,
    title: 'Visual Concept Graphs',
    description: 'Inspect relationships, dependencies, and taxonomy trees between interconnected modules.'
  },
  {
    icon: <Icons.Zap />,
    title: 'Active Recall Drills',
    description: 'Spaced repetition flashcards and problem sets tuned to your forgetting curve.'
  },
  {
    icon: <Icons.ShieldAlert />,
    title: 'Error-Driven Remediation',
    description: 'Instant analysis on incorrect quiz responses to rectify misunderstandings on the spot.'
  },
  {
    icon: <Icons.Briefcase />,
    title: 'Industry Skill Mapping',
    description: 'Map curriculum milestones directly to current engineering and analytical job specifications.'
  }
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

  if (!authChecked) {
    return null;
  }

  return (
    <div className={styles.landingPage}>
      {/* Hero Section */}
      <section className={styles.heroSection}>
        <div className={styles.heroContainer}>
          <div className={styles.heroBadge}>
            <span className={styles.badgePulse} />
            StudyGen Architecture v2.0
          </div>

          <h1 className={styles.heroTitle}>
            Engineering-grade learning paths, <span className={styles.gradientText}>personalized</span> by AI.
          </h1>

          <p className={styles.heroSubtitle}>
            StudyGen replaces unstructured self-study with verified diagnostic roadmaps, active recall systems, and targeted remediation.
          </p>

          <div className={styles.heroActions}>
            <Button
              variant="primary"
              size="lg"
              className={styles.ctaButton}
              onClick={() => navigate('/auth?tab=signup')}
            >
              Start Learning Free
              <Icons.ArrowRight />
            </Button>
            <Button
              variant="ghost"
              size="lg"
              onClick={scrollToHowItWorks}
            >
              Explore the framework
            </Button>
          </div>

          <div className={styles.heroMetrics}>
            <div className={styles.metricItem}>
              <span className={styles.metricValue}>100%</span>
              <span className={styles.metricLabel}>Adaptive Curriculum</span>
            </div>
            <div className={styles.metricDivider} />
            <div className={styles.metricItem}>
              <span className={styles.metricValue}>60s</span>
              <span className={styles.metricLabel}>Baseline Assessment</span>
            </div>
            <div className={styles.metricDivider} />
            <div className={styles.metricItem}>
              <span className={styles.metricValue}>2.4×</span>
              <span className={styles.metricLabel}>Recall Efficiency</span>
            </div>
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section id="how-it-works" className={styles.howItWorksSection}>
        <div className={styles.sectionHeader}>
          <span className={styles.sectionTag}>Methodology</span>
          <h2 className={styles.sectionTitle}>Systematic progress, zero guesswork</h2>
          <p className={styles.sectionSubtitle}>
            A continuous loop from initial diagnostic to proven proficiency.
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

      {/* Features Grid Section */}
      <section id="features" className={styles.featuresSection}>
        <div className={styles.sectionHeader}>
          <span className={styles.sectionTag}>Platform Capabilities</span>
          <h2 className={styles.sectionTitle}>Built for demanding subjects</h2>
          <p className={styles.sectionSubtitle}>
            Everything required to analyze, ingest, and retain complex technical knowledge.
          </p>
        </div>

        <div className={styles.featuresGrid}>
          {FEATURES.map((feature, idx) => (
            <Card key={idx} hoverable className={styles.featureCard}>
              <div className={styles.iconCircle}>{feature.icon}</div>
              <h3 className={styles.featureTitle}>{feature.title}</h3>
              <p className={styles.featureDescription}>{feature.description}</p>
            </Card>
          ))}
        </div>
      </section>

      {/* Final CTA Section */}
      <section className={styles.ctaBandSection}>
        <div className={styles.ctaBandContainer}>
          <h2 className={styles.ctaBandTitle}>Build your custom curriculum today</h2>
          <p className={styles.ctaBandSubtitle}>
            Take the 2-minute diagnostic and unlock your targeted study roadmap.
          </p>
          <div className={styles.ctaBandAction}>
            <Button
              variant="inverted"
              size="lg"
              className={styles.ctaButton}
              onClick={() => navigate('/auth?tab=signup')}
            >
              Get started now
              <Icons.ArrowRight />
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Landing;