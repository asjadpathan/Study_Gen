import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axiosInstance from '../services/axiosInstance';
import { useToast } from '../context/ToastContext';
import Card from '../components/Card';
import Button from '../components/Button';
import AIStageLoader from '../components/AIStageLoader';
import EmptyState from '../components/EmptyState';
import QuestionCard from '../components/QuestionCard';
import ResultBanner from '../components/ResultBanner';
import QuestionReviewAccordion from '../components/QuestionReviewAccordion';
import ProgressBar from '../components/ProgressBar';
import styles from './Remediation.module.css';

// Local ConceptCard with local "Reveal answer" toggle
const ConceptCard = ({ concept, index }) => {
  const [selectedOption, setSelectedOption] = useState(null);
  const [isAnswerRevealed, setIsAnswerRevealed] = useState(false);

  const quickCheck = concept.quickCheck;

  return (
    <Card className={styles.conceptCard}>
      {/* Header */}
      <div className={styles.conceptHeader}>
        <span className={styles.conceptBadge}>CONCEPT #{index + 1}</span>
        <h2 className={styles.conceptTitle}>{concept.conceptTitle}</h2>
      </div>

      {/* Gap Diagnosis */}
      {concept.gapDiagnosis && (
        <div className={styles.diagnosisBox}>
          <span className={styles.diagnosisLabel}>🔍 GAP DIAGNOSIS</span>
          <p className={styles.diagnosisText}>{concept.gapDiagnosis}</p>
        </div>
      )}

      {/* Simplified Markdown Explanation */}
      <div className={styles.explanationSection}>
        <div className={styles.markdownRenderer}>
          {concept.simplifiedExplanation?.split('\n\n').map((paragraph, pIdx) => {
            if (paragraph.startsWith('### ')) {
              return (
                <h3 key={pIdx} className={styles.mdHeading}>
                  {paragraph.replace('### ', '')}
                </h3>
              );
            }
            if (paragraph.startsWith('- ') || paragraph.startsWith('1. ')) {
              const items = paragraph.split('\n');
              return (
                <ul key={pIdx} className={styles.mdList}>
                  {items.map((it, iIdx) => (
                    <li key={iIdx}>
                      {it.replace(/^[-*]\s+|\d+\.\s+/, '')}
                    </li>
                  ))}
                </ul>
              );
            }
            return (
              <p key={pIdx} className={styles.mdParagraph}>
                {paragraph}
              </p>
            );
          })}
        </div>
      </div>

      {/* Analogy Block */}
      {concept.analogy && (
        <div className={styles.analogyBox}>
          <div className={styles.analogyHeader}>
            <span className={styles.analogyIcon}>💡</span>
            <h4 className={styles.analogyTitle}>Mental Model & Analogy</h4>
          </div>
          <p className={styles.analogyText}>{concept.analogy}</p>
        </div>
      )}

      {/* Visual Breakdown Vertical Timeline */}
      {concept.visualSteps && concept.visualSteps.length > 0 && (
        <div className={styles.visualSection}>
          <h4 className={styles.visualTitle}>Execution Step-by-Step Flow</h4>
          <div className={styles.timeline}>
            {concept.visualSteps.map((step, sIdx) => (
              <div key={sIdx} className={styles.timelineItem}>
                <div className={styles.timelineMarker}>{sIdx + 1}</div>
                <div className={styles.timelineContent}>
                  <p>{step}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Quick Check Question with Local Reveal Toggle */}
      {quickCheck && (
        <div className={styles.quickCheckSection}>
          <div className={styles.quickCheckHeader}>
            <span className={styles.quickCheckLabel}>⚡ QUICK KNOWLEDGE CHECK</span>
            <button
              type="button"
              className={styles.revealToggleBtn}
              onClick={() => setIsAnswerRevealed((prev) => !prev)}
            >
              {isAnswerRevealed ? '🙈 Hide Explanation' : '👁️ Reveal Answer'}
            </button>
          </div>

          <p className={styles.quickCheckQuestion}>{quickCheck.questionText}</p>

          <div className={styles.quickCheckOptions}>
            {quickCheck.options?.map((opt, optIdx) => {
              const isSelected = selectedOption === optIdx;
              const isCorrect = quickCheck.correctAnswerIndex === optIdx;

              let optionClass = styles.qcOptionNormal;
              if (isAnswerRevealed) {
                if (isCorrect) optionClass = styles.qcOptionCorrect;
                else if (isSelected) optionClass = styles.qcOptionWrong;
              } else if (isSelected) {
                optionClass = styles.qcOptionSelected;
              }

              return (
                <button
                  key={optIdx}
                  type="button"
                  className={`${styles.qcOptionBtn} ${optionClass}`}
                  onClick={() => setSelectedOption(optIdx)}
                >
                  <span className={styles.qcLetter}>
                    {String.fromCharCode(65 + optIdx)}
                  </span>
                  <span className={styles.qcText}>{opt}</span>
                  {isAnswerRevealed && isCorrect && (
                    <span className={styles.correctPill}>✓ Correct</span>
                  )}
                </button>
              );
            })}
          </div>

          {isAnswerRevealed && quickCheck.explanation && (
            <div className={styles.revealedExplanation}>
              <strong>Why:</strong> {quickCheck.explanation}
            </div>
          )}
        </div>
      )}
    </Card>
  );
};

const Remediation = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useToast();

  // Mode: 'review' | 'quiz' | 'result'
  const [viewMode, setViewMode] = useState('review');

  // API State
  const [loading, setLoading] = useState(true);
  const [isLocked, setIsLocked] = useState(false);
  const [remediationData, setRemediationData] = useState(null);

  // Retest Quiz State
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [userAnswers, setUserAnswers] = useState({});
  const [submittingRetest, setSubmittingRetest] = useState(false);
  const [retestResult, setRetestResult] = useState(null);

  // Fetch remediation data on mount
  useEffect(() => {
    let isMounted = true;

    const fetchRemediation = async () => {
      setLoading(true);
      setIsLocked(false);
      try {
        const res = await axiosInstance.get(`/modules/${id}/remediation`);
        if (!isMounted) return;

        // Redirect to /modules/:id if the module wasn't actually failed
        if (res.data && !res.data.wasFailed) {
          addToast('Module was not failed. Redirecting to lesson overview.', 'info');
          navigate(`/modules/${id}`, { replace: true });
          return;
        }

        setRemediationData(res.data);
      } catch (err) {
        if (!isMounted) return;
        if (err.response && err.response.status === 403) {
          setIsLocked(true);
        } else {
          addToast(err.response?.data?.message || 'Failed to load remediation materials.', 'error');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    if (id) {
      fetchRemediation();
    }

    return () => {
      isMounted = false;
    };
  }, [id, navigate, addToast]);

  // Loading Screen with AIStageLoader
  if (loading) {
    return (
      <div className={styles.loaderPageContainer}>
        <AIStageLoader
          title="Analyzing Conceptual Gaps"
          messages={[
            'Reviewing missed questions and error patterns...',
            'Synthesizing personalized mental model analogies...',
            'Building step-by-step visual execution flow...',
            'Formulating targeted retest questions...',
          ]}
          intervalMs={2800}
        />
      </div>
    );
  }

  // 403 Locked State
  if (isLocked) {
    return (
      <div className={styles.container}>
        <EmptyState
          icon="🔒"
          title="Module is Locked"
          description="You do not have access to this module yet. Complete the prerequisites in your learning roadmap first."
          actionText="Back to Roadmap"
          onAction={() => navigate('/roadmap-view')}
        />
      </div>
    );
  }

  if (!remediationData) {
    return (
      <div className={styles.container}>
        <EmptyState
          icon="📚"
          title="No Remediation Session Needed"
          description="This module does not require remediation."
          actionText="Back to Lessons"
          onAction={() => navigate(`/modules/${id}`)}
        />
      </div>
    );
  }

  const weakConcepts = remediationData.weakConcepts || [];
  const retestQuestions = remediationData.retestQuestions || [];
  const totalRetestQuestions = retestQuestions.length;

  // Handler: Start Retest
  const handleStartRetest = () => {
    setUserAnswers({});
    setCurrentQuestionIndex(0);
    setViewMode('quiz');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Handler: Select Retest Answer
  const handleSelectRetestAnswer = (optIdx) => {
    setUserAnswers((prev) => ({
      ...prev,
      [currentQuestionIndex]: optIdx,
    }));
  };

  // Handler: Submit Retest
  const handleSubmitRetest = async () => {
    const answersArray = retestQuestions.map((_, idx) =>
      userAnswers[idx] !== undefined ? userAnswers[idx] : 0
    );

    setSubmittingRetest(true);
    try {
      const assessmentId = remediationData.remediationAssessmentId || 1;
      const res = await axiosInstance.post(`/assessments/${assessmentId}/submit`, {
        selectedOptions: answersArray,
      });

      setRetestResult(res.data);
      setViewMode('result');

      if (res.data.passed) {
        addToast('🏆 Remediation Retest Passed! Module progression unlocked.', 'success');
      } else {
        addToast('Retest completed. Review questions below.', 'info');
      }
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to submit retest.', 'error');
    } finally {
      setSubmittingRetest(false);
    }
  };

  // 1. CONCEPT REVIEW VIEW
  if (viewMode === 'review') {
    return (
      <div className={styles.container}>
        {/* Intro Strip */}
        <div className={styles.introStrip}>
          <div className={styles.introText}>
            <span className={styles.introBadge}>REMEDIATION LAB</span>
            <h1 className={styles.introTitle}>
              Let's fix the gaps: {weakConcepts.length} concepts to review
            </h1>
            <p className={styles.introDesc}>
              Targeted conceptual reconstruction for <strong>"{remediationData.moduleTitle}"</strong>.
              Review the analogies and execution steps below, then take the targeted retest to advance.
            </p>
          </div>
          <Button
            variant="primary"
            size="md"
            onClick={handleStartRetest}
            className={styles.introCtaBtn}
          >
            🎯 Take Targeted Retest →
          </Button>
        </div>

        {/* Concept Cards */}
        <div className={styles.conceptsList}>
          {weakConcepts.map((concept, idx) => (
            <ConceptCard key={idx} concept={concept} index={idx} />
          ))}
        </div>

        {/* Footer Action Bar */}
        <div className={styles.footerBar}>
          <div className={styles.footerLeft}>
            <span className={styles.readyText}>Ready to prove your understanding?</span>
            <span className={styles.retestInfo}>
              3 focused questions · 80% required to advance
            </span>
          </div>
          <div className={styles.footerActions}>
            <Button
              variant="outlined"
              onClick={() => navigate(`/modules/${id}`)}
            >
              Review Full Lesson
            </Button>
            <Button
              variant="primary"
              size="lg"
              onClick={handleStartRetest}
              className={styles.footerRetestBtn}
            >
              ✨ Take the Targeted Retest →
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // 2. RETEST QUIZ VIEW (Reusing QuestionCard)
  if (viewMode === 'quiz') {
    const currentQ = retestQuestions[currentQuestionIndex];
    const progressPercent = Math.round(((currentQuestionIndex + 1) / totalRetestQuestions) * 100);
    const allAnswered = Object.keys(userAnswers).length >= totalRetestQuestions;

    return (
      <div className={styles.container}>
        <div className={styles.quizTopBar}>
          <div className={styles.quizInfo}>
            <span className={styles.retestTag}>TARGETED RETEST</span>
            <h2 className={styles.quizTitle}>
              Remediation Quiz: {remediationData.moduleTitle}
            </h2>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setViewMode('review')}
          >
            ← Back to Concepts
          </Button>
        </div>

        <div className={styles.progressSection}>
          <div className={styles.progressRow}>
            <span>
              Question <strong>{currentQuestionIndex + 1}</strong> of <strong>{totalRetestQuestions}</strong>
            </span>
            <span>{progressPercent}% completed</span>
          </div>
          <ProgressBar value={progressPercent} />
        </div>

        <QuestionCard
          question={currentQ}
          currentIndex={currentQuestionIndex}
          totalQuestions={totalRetestQuestions}
          selectedAnswer={userAnswers[currentQuestionIndex]}
          onSelectAnswer={handleSelectRetestAnswer}
        />

        <div className={styles.quizFooter}>
          <Button
            variant="outlined"
            onClick={() => setCurrentQuestionIndex((prev) => Math.max(prev - 1, 0))}
            disabled={currentQuestionIndex === 0}
          >
            ← Previous
          </Button>

          {currentQuestionIndex < totalRetestQuestions - 1 ? (
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
              onClick={handleSubmitRetest}
              disabled={!allAnswered || submittingRetest}
              className={styles.submitRetestBtn}
            >
              {submittingRetest ? 'Evaluating...' : 'Submit Retest Results ✓'}
            </Button>
          )}
        </div>
      </div>
    );
  }

  // 3. RETEST RESULT VIEW (Reusing ResultBanner & QuestionReviewAccordion)
  if (viewMode === 'result' && retestResult) {
    const isPassed = retestResult.passed;

    return (
      <div className={styles.container}>
        {/* Confetti Animation on Pass */}
        {isPassed && (
          <div className={styles.confettiWrapper}>
            <div className={`${styles.confettiPiece} ${styles.c1}`} />
            <div className={`${styles.confettiPiece} ${styles.c2}`} />
            <div className={`${styles.confettiPiece} ${styles.c3}`} />
            <div className={`${styles.confettiPiece} ${styles.c4}`} />
            <div className={`${styles.confettiPiece} ${styles.c5}`} />
          </div>
        )}

        <ResultBanner
          score={retestResult.score}
          passingScore={retestResult.passingScore || 80}
        />

        {isPassed ? (
          <Card className={styles.passOutcomeCard}>
            <div className={styles.outcomeHeader}>
              <span className={styles.outcomeIcon}>🏆</span>
              <div>
                <h2>Concept Gaps Resolved!</h2>
                <p className={styles.outcomeDesc}>
                  You scored {retestResult.score}% on the targeted retest. Your progress has been updated!
                </p>
              </div>
            </div>

            <div className={styles.outcomeActions}>
              <Button
                variant="primary"
                onClick={() => navigate('/roadmap-view')}
              >
                Continue Roadmap Progression →
              </Button>
              <Button
                variant="outlined"
                onClick={() => setViewMode('review')}
              >
                Review Concepts Again
              </Button>
            </div>
          </Card>
        ) : (
          <Card className={styles.failOutcomeCard}>
            <div className={styles.outcomeHeader}>
              <span className={styles.outcomeIcon}>💡</span>
              <div>
                <h2>Score: {retestResult.score}%. (80% needed to pass)</h2>
                <p className={styles.outcomeDesc}>
                  You're getting closer! Re-examine the mental models and visual timelines to master these concepts.
                </p>
              </div>
            </div>

            <div className={styles.outcomeActions}>
              <Button
                variant="primary"
                onClick={() => setViewMode('review')}
              >
                ← Return to Conceptual Review
              </Button>
              <Button
                variant="outlined"
                onClick={() => navigate(`/modules/${id}`)}
              >
                Study Lesson Material
              </Button>
            </div>
          </Card>
        )}

        {/* Detailed Question Review */}
        <QuestionReviewAccordion questionsReview={retestResult.questionsReview} />
      </div>
    );
  }

  return null;
};

export default Remediation;
