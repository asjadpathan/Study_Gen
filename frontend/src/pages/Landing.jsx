import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../components/Button';
import Card from '../components/Card';
import styles from './Landing.module.css';

const Landing = () => {
  const navigate = useNavigate();

  // Redirect to /dashboard if valid JWT is present
  useEffect(() => {
    const token = localStorage.getItem('token');
    const userStr = localStorage.getItem('studygen_user');
    let hasValidToken = !!token;

    if (!hasValidToken && userStr) {
      try {
        const parsed = JSON.parse(userStr);
        if (parsed?.token) hasValidToken = true;
      } catch {
        // ignore JSON parse error
      }
    }

    if (hasValidToken) {
      navigate('/dashboard', { replace: true });
    }
  }, [navigate]);

  const handleScrollToHowItWorks = () => {
    const element = document.getElementById('how-it-works');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const steps = [
    {
      number: 1,
      title: 'Define Goal & Diagnostic',
      description: 'Input your target role or topic and complete an initial diagnostic assessment.',
    },
    {
      number: 2,
      title: 'AI Roadmap Generation',
      description: 'Receive a personalized, module-by-module learning path tailored to your baseline.',
    },
    {
      number: 3,
      title: 'Interactive Study & Mind Maps',
      description: 'Dive into structured concepts, visual mind maps, and instant AI study assistant help.',
    },
    {
      number: 4,
      title: 'Adaptive Quizzes & Remediation',
      description: 'Take module assessments with automatic AI error notes for concepts you miss.',
    },
    {
      number: 5,
      title: 'Career Matching & Mastery',
      description: 'Track your study streak, earn badges, and match your skills directly to job postings.',
    },
  ];

  const features = [
    {
      icon: '🗺️',
      title: 'AI Learning Roadmaps',
      description: 'Hyper-personalized study roadmaps built dynamically based on your availability and goals.',
    },
    {
      icon: '🎯',
      title: 'Diagnostic Skill Assessment',
      description: 'Evaluate your exact starting baseline to skip topics you already know and focus on gaps.',
    },
    {
      icon: '📈',
      title: 'Adaptive Concept Progression',
      description: 'Sequential concept unlocking that ensures foundational mastery before advancing.',
    },
    {
      icon: '🤖',
      title: 'Real-time Study Assistant',
      description: 'Ask questions anytime and get instant, clear AI explanations tailored to your context.',
    },
    {
      icon: '📄',
      title: 'Document & PDF Analyzer',
      description: 'Upload lecture slides or PDFs to generate summaries, key terms, and practice questions.',
    },
    {
      icon: '⏱️',
      title: 'Smart Pomodoro & Heatmap',
      description: 'Log your focused study sessions and visualize your daily consistency with streak heatmaps.',
    },
    {
      icon: '🧠',
      title: 'Interactive Mind Maps',
      description: 'Explore visual node graphs connecting concepts, subtopics, and prerequisite dependencies.',
    },
    {
      icon: '🏋️',
      title: 'Targeted Practice & Drills',
      description: 'Reinforce memory with auto-generated flashcards, practice drills, and instant feedback.',
    },
    {
      icon: '🩹',
      title: 'AI Remediation & Error Notes',
      description: 'Receive targeted explanations for wrong quiz answers to fix misunderstandings instantly.',
    },
    {
      icon: '💼',
      title: 'Career Matching & Skill Insights',
      description: 'Compare your roadmap progress with real-world job posting requirements and skill demand.',
    },
  ];

  return (
    <div className={styles.landingPage}>
      {/* Hero Section */}
      <section className={styles.heroSection}>
        <div className={styles.heroContainer}>
          <div className={styles.heroBadge}>
            <span className={styles.badgePulse}></span>
            <span>Next-Gen AI Learning Platform</span>
          </div>
          <h1 className={styles.heroTitle}>
            Master Any Subject with <span className={styles.gradientText}>AI-Powered</span> Study Paths
          </h1>
          <p className={styles.heroSubtitle}>
            StudyGen crafts personalized roadmaps, diagnostic assessments, and interactive AI tutoring so you learn faster, stay consistent, and achieve real mastery.
          </p>

          <div className={styles.heroActions}>
            <Button
              variant="primary"
              size="lg"
              onClick={() => navigate('/auth?tab=signup')}
            >
              Get started <span>→</span>
            </Button>
            <Button
              variant="ghost"
              size="lg"
              onClick={handleScrollToHowItWorks}
            >
              See how it works
            </Button>
          </div>

          {/* Quick Metrics / Social Proof */}
          <div className={styles.heroMetrics}>
            <div className={styles.metricItem}>
              <span className={styles.metricValue}>100%</span>
              <span className={styles.metricLabel}>Personalized Paths</span>
            </div>
            <div className={styles.metricDivider}></div>
            <div className={styles.metricItem}>
              <span className={styles.metricValue}>10+</span>
              <span className={styles.metricLabel}>AI Learning Tools</span>
            </div>
            <div className={styles.metricDivider}></div>
            <div className={styles.metricItem}>
              <span className={styles.metricValue}>2x</span>
              <span className={styles.metricLabel}>Retention Speed</span>
            </div>
          </div>
        </div>
      </section>

      {/* "How It Works" Section */}
      <section id="how-it-works" className={styles.howItWorksSection}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>How It Works</h2>
          <p className={styles.sectionSubtitle}>
            Five structured steps designed to take you from initial goal setting to complete subject mastery.
          </p>
        </div>

        <div className={styles.stepsContainer}>
          {steps.map((step) => (
            <div key={step.number} className={styles.stepCard}>
              <div className={styles.stepBadge}>{step.number}</div>
              <h3 className={styles.stepTitle}>{step.title}</h3>
              <p className={styles.stepDescription}>{step.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Features Grid Section */}
      <section id="features" className={styles.featuresSection}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>Built for Serious Learners</h2>
          <p className={styles.sectionSubtitle}>
            Everything you need to plan, study, practice, and track your progress in one integrated platform.
          </p>
        </div>

        <div className={styles.featuresGrid}>
          {features.map((feature, idx) => (
            <Card key={idx} hoverable className={styles.featureCard}>
              <div className={styles.iconCircle}>{feature.icon}</div>
              <h3 className={styles.featureTitle}>{feature.title}</h3>
              <p className={styles.featureDescription}>{feature.description}</p>
            </Card>
          ))}
        </div>
      </section>

      {/* Final CTA Band Section */}
      <section className={styles.ctaBandSection}>
        <div className={styles.ctaBandContainer}>
          <h2 className={styles.ctaBandTitle}>Ready to Accelerate Your Learning?</h2>
          <p className={styles.ctaBandSubtitle}>
            Join StudyGen today and get your first AI-generated learning roadmap in under 60 seconds.
          </p>
          <div className={styles.ctaBandAction}>
            <Button
              variant="inverted"
              size="lg"
              onClick={() => navigate('/auth?tab=signup')}
            >
              Get started for free <span>→</span>
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Landing;
