import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axiosInstance from '../services/axiosInstance';
import { useToast } from '../context/ToastContext';
import Card from '../components/Card';
import Button from '../components/Button';
import Skeleton from '../components/Skeleton';
import ErrorCard from '../components/ErrorCard';
import QuestionCard from '../components/QuestionCard';
import ResultBanner from '../components/ResultBanner';
import QuestionReviewAccordion from '../components/QuestionReviewAccordion';
import ProgressBar from '../components/ProgressBar';
import styles from './ModuleTest.module.css';

const ModuleTest = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useToast();

  // Screen states: 'prestart' | 'quiz' | 'result'
  const [screen, setScreen] = useState('prestart');

  // Loading & error states
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // Test metadata from POST /api/modules/{id}/test
  const [testData, setTestData] = useState(null);
  const [cooldownRemaining, setCooldownRemaining] = useState(0);

  // Quiz in-progress state
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [userAnswers, setUserAnswers] = useState({});

  // Result state after submission
  const [attemptResult, setAttemptResult] = useState(null);

  // Pre-start: Fetch or resume module test
  const fetchModuleTest = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axiosInstance.post(`/modules/${id}/test`);
      setTestData(res.data);
      setCooldownRemaining(res.data.cooldownSeconds || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to initialize module test.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) {
      fetchModuleTest();
    }
  }, [id]);

  // Live countdown timer for cooldown
  useEffect(() => {
    if (cooldownRemaining <= 0) return;

    const timer = setInterval(() => {
      setCooldownRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [cooldownRemaining]);

  const formatCooldown = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  // Start / Retake Test
  const handleStartTest = () => {
    if (cooldownRemaining > 0) return;
    setUserAnswers({});
    setCurrentQuestionIndex(0);
    setScreen('quiz');
  };

  // Question Answer Selection
  const handleSelectAnswer = (optionIndex) => {
    setUserAnswers((prev) => ({
      ...prev,
      [currentQuestionIndex]: optionIndex,
    }));
  };

  // Submit Quiz Attempt
  const handleSubmitTest = async () => {
    const questions = testData?.questions || [];
    const answersArray = questions.map((_, idx) =>
      userAnswers[idx] !== undefined ? userAnswers[idx] : 0
    );

    setSubmitting(true);
    try {
      const res = await axiosInstance.post(`/assessments/${testData.id}/submit`, {
        selectedOptions: answersArray,
      });

      setAttemptResult(res.data);
      setScreen('result');

      if (res.data.passed) {
        addToast('🏆 Module Passed! Next module unlocked.', 'success');
      } else {
        addToast('Assessment completed. Review feedback below.', 'info');
      }
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to submit test attempt.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  // Loading Skeleton
  if (loading) {
    return (
      <div className={styles.container}>
        <Skeleton height="160px" borderRadius="1.5rem" />
        <Skeleton height="320px" borderRadius="1.5rem" style={{ marginTop: '1.5rem' }} />
      </div>
    );
  }

  // Error Card
  if (error || !testData) {
    return (
      <div className={styles.container}>
        <ErrorCard
          title="Module Test Unavailable"
          message={error || 'Could not load the module assessment.'}
          onRetry={fetchModuleTest}
        />
      </div>
    );
  }

  const questions = testData.questions || [];
  const totalQuestions = questions.length;
  const currentQuestion = questions[currentQuestionIndex];
  const allAnswered = Object.keys(userAnswers).length >= totalQuestions;

  // 1. PRE-START SCREEN
  if (screen === 'prestart') {
    return (
      <div className={styles.container}>
        <Card className={styles.prestartCard}>
          <div className={styles.prestartBadgeRow}>
            <span className={styles.moduleTag}>MODULE GATEWAY TEST</span>
            {testData.alreadyPassed && (
              <span className={styles.passedChip}>✓ ALREADY PASSED</span>
            )}
          </div>

          <h1 className={styles.prestartTitle}>{testData.title || 'Mastery Assessment'}</h1>
          <p className={styles.prestartSubtitle}>
            Test your understanding of key concepts to unlock progression to the next module.
          </p>

          <div className={styles.statsGrid}>
            <div className={styles.statBox}>
              <span className={styles.statBoxLabel}>Questions</span>
              <span className={styles.statBoxValue}>{totalQuestions}</span>
            </div>
            <div className={styles.statBox}>
              <span className={styles.statBoxLabel}>Pass Mark</span>
              <span className={styles.statBoxValue}>80%</span>
            </div>
            <div className={styles.statBox}>
              <span className={styles.statBoxLabel}>Attempts Left</span>
              <span className={styles.statBoxValue}>{testData.attemptsLeft ?? 3} / 3</span>
            </div>
            <div className={styles.statBox}>
              <span className={styles.statBoxLabel}>Best Score</span>
              <span className={styles.statBoxValue}>
                {testData.bestScore != null ? `${testData.bestScore}%` : 'None'}
              </span>
            </div>
          </div>

          {/* Cooldown Active Banner */}
          {cooldownRemaining > 0 && (
            <div className={styles.cooldownBanner}>
              <span>⏳ Test is currently on cooldown after previous attempt.</span>
              <span className={styles.countdownPill}>{formatCooldown(cooldownRemaining)} remaining</span>
            </div>
          )}

          {/* Already Passed Notice */}
          {testData.alreadyPassed ? (
            <div className={styles.alreadyPassedBanner}>
              <div className={styles.alreadyPassedText}>
                <h3>🎉 You've already passed this module!</h3>
                <p>Your highest recorded score is <strong>{testData.bestScore}%</strong>.</p>
              </div>
              <div className={styles.alreadyPassedActions}>
                <Button
                  variant="primary"
                  onClick={() => navigate('/roadmap-view')}
                >
                  Continue Roadmap →
                </Button>
                <Button
                  variant="outlined"
                  onClick={handleStartTest}
                  disabled={cooldownRemaining > 0}
                >
                  Retake for Mastery
                </Button>
              </div>
            </div>
          ) : (
            <div className={styles.prestartActions}>
              <Button
                variant="primary"
                size="lg"
                onClick={handleStartTest}
                disabled={cooldownRemaining > 0 || testData.attemptsLeft <= 0}
                className={styles.startBtn}
              >
                {cooldownRemaining > 0
                  ? `Cooldown Active (${formatCooldown(cooldownRemaining)})`
                  : 'Start Module Test →'}
              </Button>
              <Button
                variant="ghost"
                onClick={() => navigate(-1)}
              >
                ← Back to Roadmap
              </Button>
            </div>
          )}
        </Card>
      </div>
    );
  }

  // 2. QUIZ SCREEN
  if (screen === 'quiz') {
    const progressPercent = Math.round(((currentQuestionIndex + 1) / totalQuestions) * 100);

    return (
      <div className={styles.container}>
        {/* Quiz Top Bar */}
        <div className={styles.quizTopBar}>
          <div className={styles.quizInfo}>
            <span className={styles.testLabel}>Module Test</span>
            <h2 className={styles.quizModuleTitle}>{testData.title}</h2>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setScreen('prestart')}
          >
            Exit Test
          </Button>
        </div>

        {/* Progress */}
        <div className={styles.progressSection}>
          <div className={styles.progressRow}>
            <span>
              Question <strong>{currentQuestionIndex + 1}</strong> of <strong>{totalQuestions}</strong>
            </span>
            <span>{progressPercent}% completed</span>
          </div>
          <ProgressBar value={progressPercent} />
        </div>

        {/* Question Card */}
        <QuestionCard
          question={currentQuestion}
          currentIndex={currentQuestionIndex}
          totalQuestions={totalQuestions}
          selectedAnswer={userAnswers[currentQuestionIndex]}
          onSelectAnswer={handleSelectAnswer}
        />

        {/* Navigation Footer */}
        <div className={styles.quizFooter}>
          <Button
            variant="outlined"
            onClick={() => setCurrentQuestionIndex((prev) => Math.max(prev - 1, 0))}
            disabled={currentQuestionIndex === 0}
          >
            ← Previous
          </Button>

          {currentQuestionIndex < totalQuestions - 1 ? (
            <Button
              variant="primary"
              onClick={() => setCurrentQuestionIndex((prev) => prev + 1)}
              disabled={userAnswers[currentQuestionIndex] === undefined}
            >
              Next Question →
            </Button>
          ) : (
            <Button
              variant="primary"
              onClick={handleSubmitTest}
              disabled={!allAnswered || submitting}
              className={styles.submitBtn}
            >
              {submitting ? 'Evaluating...' : 'Submit Test Results ✓'}
            </Button>
          )}
        </div>
      </div>
    );
  }

  // 3. RESULT SCREEN
  if (screen === 'result' && attemptResult) {
    const isPassed = attemptResult.passed;

    return (
      <div className={styles.container}>
        {/* Confetti Celebration for Pass */}
        {isPassed && (
          <div className={styles.confettiWrapper}>
            <div className={`${styles.confettiPiece} ${styles.c1}`} />
            <div className={`${styles.confettiPiece} ${styles.c2}`} />
            <div className={`${styles.confettiPiece} ${styles.c3}`} />
            <div className={`${styles.confettiPiece} ${styles.c4}`} />
            <div className={`${styles.confettiPiece} ${styles.c5}`} />
            <div className={`${styles.confettiPiece} ${styles.c6}`} />
          </div>
        )}

        {/* Score Banner with 80% Threshold Line */}
        <ResultBanner
          score={attemptResult.score}
          passingScore={attemptResult.passingScore || 80}
        />

        {/* Pass Variant Content */}
        {isPassed ? (
          <Card className={styles.passOutcomeCard}>
            <div className={styles.outcomeHeader}>
              <span className={styles.celebrationIcon}>🏆</span>
              <div>
                <h2>Module Mastered!</h2>
                <p className={styles.unlockedText}>
                  {attemptResult.nextModuleTitle
                    ? `Next up: "${attemptResult.nextModuleTitle}" unlocked!`
                    : 'All modules in this sequence are now unlocked!'}
                </p>
              </div>
            </div>

            <div className={styles.metaRow}>
              <div className={styles.masteryPill}>
                📊 Mastery Score: <strong>{attemptResult.score}%</strong>
              </div>
              <div className={styles.streakPill}>
                🔥 Streak updated +1 day!
              </div>
            </div>

            <div className={styles.outcomeActions}>
              {attemptResult.nextModuleId ? (
                <Button
                  variant="primary"
                  onClick={() => navigate(`/modules/${attemptResult.nextModuleId}`)}
                >
                  Start Next Module {attemptResult.nextModuleTitle ? `: ${attemptResult.nextModuleTitle}` : ''} →
                </Button>
              ) : (
                <Button
                  variant="primary"
                  onClick={() => navigate('/roadmap-view')}
                >
                  Return to Roadmap Workspace →
                </Button>
              )}
              <Button
                variant="outlined"
                onClick={() => navigate('/roadmap-view')}
              >
                View Roadmap Graph
              </Button>
            </div>
          </Card>
        ) : (
          /* Fail Variant Content */
          <Card className={styles.failOutcomeCard}>
            <div className={styles.outcomeHeader}>
              <span className={styles.failIcon}>🎯</span>
              <div>
                <h2>You scored {attemptResult.score}%. You need 80% to pass.</h2>
                <p className={styles.encouragingNote}>
                  Don't worry! Review the concepts below and try again. Practice makes perfect.
                </p>
              </div>
            </div>

            {/* Weak Concept Chips */}
            {attemptResult.weakConcepts && attemptResult.weakConcepts.length > 0 && (
              <div className={styles.weakConceptsSection}>
                <span className={styles.weakConceptsTitle}>Concepts to Strengthen:</span>
                <div className={styles.weakChipsList}>
                  {attemptResult.weakConcepts.map((concept, idx) => (
                    <span key={idx} className={styles.weakConceptChip}>
                      ⚠️ {concept}
                    </span>
                  ))}
                </div>
              </div>
            )}

            <div className={styles.attemptsNotice}>
              Attempts remaining today: <strong>{attemptResult.attemptsLeft ?? 2}</strong>
              {attemptResult.cooldownSeconds > 0 && (
                <span> · Cooldown active: {Math.ceil(attemptResult.cooldownSeconds / 60)} min</span>
              )}
            </div>

            <div className={styles.outcomeActions}>
              <Button
                variant="primary"
                onClick={() => navigate(`/modules/${id}/remediation`)}
              >
                🧠 Start Remediation Session
              </Button>
              <Button
                variant="outlined"
                onClick={() => navigate(`/modules/${id}`)}
              >
                📖 Review Lesson
              </Button>
              <Button
                variant="ghost"
                onClick={() => setScreen('prestart')}
              >
                ← Back to Overview
              </Button>
            </div>
          </Card>
        )}

        {/* Detailed Question Review Accordion */}
        <QuestionReviewAccordion questionsReview={attemptResult.questionsReview} />
      </div>
    );
  }

  return null;
};

export default ModuleTest;
