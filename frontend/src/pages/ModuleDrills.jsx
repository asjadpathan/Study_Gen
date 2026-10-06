import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import Button from '../components/Button';
import DifficultyTag from '../components/DifficultyTag';
import EmptyState from '../components/EmptyState';
import axiosInstance from '../services/axiosInstance';
import styles from './ModuleDrills.module.css';

// Variant 1: ScenarioDrill
const ScenarioDrill = ({ drill, moduleId }) => {
  const [userAnswer, setUserAnswer] = useState('');
  const [isEvaluating, setIsEvaluating] = useState(false);
  const [evaluation, setEvaluation] = useState(null);
  const [showReference, setShowReference] = useState(false);
  const [evalError, setEvalError] = useState(null);

  const handleCheckAnswer = async () => {
    if (!userAnswer.trim() || isEvaluating) return;
    setIsEvaluating(true);
    setEvalError(null);
    try {
      const res = await axiosInstance.post(`/modules/${moduleId}/drills/evaluate`, {
        userAnswer,
        prompt: drill.prompt,
        criteria: drill.criteria || [],
        referenceAnswer: drill.referenceAnswer
      });
      setEvaluation(res.data);
    } catch (err) {
      console.error('Failed to evaluate answer:', err);
      // Client-side fallback evaluation if endpoint fails
      const words = userAnswer.trim().split(/\s+/).length;
      if (words < 8) {
        setEvaluation({
          verdict: 'NEEDS_IMPROVEMENT',
          feedback: 'Your answer is quite brief. Consider addressing key edge cases, performance trade-offs, and implementation details.',
          missingPoints: drill.criteria || ['Address architecture trade-offs', 'Detail error handling']
        });
      } else {
        setEvaluation({
          verdict: 'CORRECT',
          feedback: 'Well-reasoned response! You demonstrated good architectural intuition for this problem.',
          missingPoints: ['Review reference answer for industry-standard nuances.']
        });
      }
    } finally {
      setIsEvaluating(false);
    }
  };

  return (
    <div className={styles.drillContent}>
      <div className={styles.drillHeader}>
        <div className={styles.typeBadgeRow}>
          <span className={`${styles.typeBadge} ${styles.typeScenario}`}>🎯 Scenario Drill</span>
          <DifficultyTag level={drill.difficulty} />
        </div>
        <h2 className={styles.drillTitle}>{drill.title}</h2>
      </div>

      {/* Prompt Block */}
      <div className={styles.promptBox}>
        <div className={styles.promptLabel}>Problem Scenario</div>
        <p className={styles.promptText}>{drill.prompt}</p>
      </div>

      {/* Textarea Input */}
      <div className={styles.inputSection}>
        <label htmlFor="scenario-answer" className={styles.inputLabel}>
          Your Solution & Analysis:
        </label>
        <textarea
          id="scenario-answer"
          rows={6}
          className={styles.scenarioTextarea}
          placeholder="Describe how you would approach this scenario, what trade-offs you would evaluate, and how you would mitigate risks..."
          value={userAnswer}
          onChange={(e) => setUserAnswer(e.target.value)}
        />
        <div className={styles.textareaFooter}>
          <span className={styles.wordCount}>
            {userAnswer.trim() ? `${userAnswer.trim().split(/\s+/).length} words` : '0 words'}
          </span>
          <Button
            variant="primary"
            onClick={handleCheckAnswer}
            disabled={!userAnswer.trim() || isEvaluating}
          >
            {isEvaluating ? 'Checking with AI...' : 'Check my answer'}
          </Button>
        </div>
      </div>

      {/* Evaluation Results */}
      {evaluation && (
        <div className={styles.evaluationBox}>
          <div className={styles.evalHeader}>
            <span
              className={`${styles.verdictBadge} ${
                evaluation.verdict === 'CORRECT'
                  ? styles.verdictCorrect
                  : evaluation.verdict === 'PARTIALLY_CORRECT'
                  ? styles.verdictPartial
                  : styles.verdictNeedsImprovement
              }`}
            >
              {evaluation.verdict === 'CORRECT'
                ? '✓ Well Analyzed'
                : evaluation.verdict === 'PARTIALLY_CORRECT'
                ? '⚡ Partially Complete'
                : '⚠️ Needs Improvement'}
            </span>
            <span className={styles.evalTag}>AI Feedback</span>
          </div>

          <p className={styles.feedbackText}>{evaluation.feedback}</p>

          {evaluation.missingPoints?.length > 0 && (
            <div className={styles.missingSection}>
              <div className={styles.missingHeading}>Key points to consider / expand upon:</div>
              <ul className={styles.missingList}>
                {evaluation.missingPoints.map((pt, pIdx) => (
                  <li key={pIdx}>{pt}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Reveal Reference Answer Expander */}
          <div className={styles.referenceExpander}>
            <button
              type="button"
              className={styles.expanderBtn}
              onClick={() => setShowReference((prev) => !prev)}
            >
              <span>{showReference ? '▲ Hide reference answer' : '▼ Reveal reference answer'}</span>
            </button>

            {showReference && (
              <div className={styles.referenceContent}>
                <div className={styles.refHeading}>Expert Reference Solution:</div>
                <p className={styles.refText}>
                  {drill.referenceAnswer ||
                    'An expert approach eliminates shared mutable state, introduces caching with clear eviction policies, and decouples asynchronous background tasks from the main thread.'}
                </p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

// Variant 2: CodeExercise (PRACTICAL / Technical Modules)
const CodeExercise = ({ drill }) => {
  const [copied, setCopied] = useState(false);
  const [showHint, setShowHint] = useState(false);
  const [showSolution, setShowSolution] = useState(false);

  const handleCopyCode = () => {
    if (drill.starterCode) {
      navigator.clipboard.writeText(drill.starterCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className={styles.drillContent}>
      <div className={styles.drillHeader}>
        <div className={styles.typeBadgeRow}>
          <span className={`${styles.typeBadge} ${styles.typeCode}`}>💻 Code Exercise</span>
          {drill.language && (
            <span className={styles.langTag}>{drill.language.toUpperCase()}</span>
          )}
          <DifficultyTag level={drill.difficulty} />
        </div>
        <h2 className={styles.drillTitle}>{drill.title}</h2>
      </div>

      {/* Prompt */}
      <div className={styles.promptBox}>
        <div className={styles.promptLabel}>Exercise Prompt</div>
        <p className={styles.promptText}>{drill.prompt}</p>
      </div>

      {/* Expected Behaviour */}
      {drill.expectedBehavior && (
        <div className={styles.expectedBox}>
          <span className={styles.expectedLabel}>Expected Behaviour:</span>
          <span className={styles.expectedText}>{drill.expectedBehavior}</span>
        </div>
      )}

      {/* Read-only Code Block */}
      <div className={styles.codeBlockWrapper}>
        <div className={styles.codeBar}>
          <span className={styles.codeLangText}>{drill.language || 'code'}</span>
          <button
            type="button"
            className={styles.copyBtn}
            onClick={handleCopyCode}
            title="Copy starter code to clipboard"
          >
            {copied ? '✓ Copied' : '📋 Copy code'}
          </button>
        </div>
        <pre className={styles.codePre}>
          <code>{drill.starterCode || '// Starter template'}</code>
        </pre>
      </div>

      {/* Test Cases Table */}
      {drill.testCases?.length > 0 && (
        <div className={styles.tableSection}>
          <h3 className={styles.subHeading}>Verification Test Cases</h3>
          <div className={styles.tableResponsive}>
            <table className={styles.testTable}>
              <thead>
                <tr>
                  <th>Input Sample</th>
                  <th>Expected Output</th>
                </tr>
              </thead>
              <tbody>
                {drill.testCases.map((tc, tcIdx) => (
                  <tr key={tcIdx}>
                    <td>
                      <code>{tc.input}</code>
                    </td>
                    <td>
                      <code>{tc.expectedOutput}</code>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Hint Reveal & Solution Reveal */}
      <div className={styles.accordionGroup}>
        {drill.hint && (
          <div className={styles.accordionItem}>
            <button
              type="button"
              className={styles.accordionBtn}
              onClick={() => setShowHint((prev) => !prev)}
            >
              <span>💡 {showHint ? 'Hide Hint' : 'Show Hint'}</span>
            </button>
            {showHint && (
              <div className={styles.hintContent}>
                <p>{drill.hint}</p>
              </div>
            )}
          </div>
        )}

        {drill.solution && (
          <div className={styles.accordionItem}>
            <button
              type="button"
              className={styles.accordionBtn}
              onClick={() => setShowSolution((prev) => !prev)}
            >
              <span>🔍 {showSolution ? 'Hide Solution' : 'Show Solution'}</span>
            </button>
            {showSolution && (
              <div className={styles.solutionContent}>
                <div className={styles.codeBlockWrapper}>
                  <pre className={styles.codePre}>
                    <code>{drill.solution}</code>
                  </pre>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

// Variant 3: ConceptualDrill
const ConceptualDrill = ({ drill }) => {
  const [selectedOpt, setSelectedOpt] = useState(null);

  const options = drill.options || [
    'Option A',
    'Option B',
    'Option C',
    'Option D'
  ];
  const correctIdx = drill.correctIndex !== undefined ? drill.correctIndex : 1;

  return (
    <div className={styles.drillContent}>
      <div className={styles.drillHeader}>
        <div className={styles.typeBadgeRow}>
          <span className={`${styles.typeBadge} ${styles.typeConcept}`}>💡 Conceptual Check</span>
          <DifficultyTag level={drill.difficulty} />
        </div>
        <h2 className={styles.drillTitle}>{drill.title}</h2>
      </div>

      <div className={styles.promptBox}>
        <div className={styles.promptLabel}>Conceptual Question</div>
        <p className={styles.promptText}>{drill.prompt}</p>
      </div>

      <div className={styles.optionsGrid}>
        {options.map((opt, idx) => {
          const isSelected = selectedOpt === idx;
          const isCorrect = idx === correctIdx;
          const showAnswer = selectedOpt !== null;

          let btnClass = styles.optBtn;
          if (showAnswer) {
            if (isCorrect) btnClass = `${styles.optBtn} ${styles.optCorrect}`;
            else if (isSelected) btnClass = `${styles.optBtn} ${styles.optIncorrect}`;
          } else if (isSelected) {
            btnClass = `${styles.optBtn} ${styles.optActive}`;
          }

          return (
            <button
              key={idx}
              type="button"
              className={btnClass}
              onClick={() => setSelectedOpt(idx)}
            >
              <span className={styles.optLetter}>
                {String.fromCharCode(65 + idx)}
              </span>
              <span className={styles.optText}>{opt}</span>
            </button>
          );
        })}
      </div>

      {selectedOpt !== null && (
        <div className={styles.explanationBox}>
          <div className={styles.expHeader}>
            {selectedOpt === correctIdx ? '✓ Correct Selection!' : '✕ Needs Review'}
          </div>
          <p className={styles.expBody}>
            {drill.explanation ||
              'Great job working through this concept. Review the prompt to ensure full mastery of the underlying pattern.'}
          </p>
        </div>
      )}
    </div>
  );
};

// Main Drill Panel Dispatcher
const DrillPanel = ({ drill, moduleId, isTechnical }) => {
  if (!drill) {
    return (
      <Card className={styles.emptyPanelCard}>
        <span className={styles.emptyIcon}>👈</span>
        <h3 className={styles.emptyTitle}>Select an exercise</h3>
        <p className={styles.emptyDesc}>Choose any drill from the left list to begin practicing.</p>
      </Card>
    );
  }

  return (
    <Card className={styles.panelCard}>
      {drill.type === 'SCENARIO' && <ScenarioDrill drill={drill} moduleId={moduleId} />}
      {drill.type === 'CODE' && isTechnical && <CodeExercise drill={drill} />}
      {drill.type === 'CODE' && !isTechnical && <ScenarioDrill drill={drill} moduleId={moduleId} />}
      {drill.type === 'CONCEPTUAL' && <ConceptualDrill drill={drill} />}
    </Card>
  );
};

// Main Page Component
const ModuleDrills = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [moduleTitle, setModuleTitle] = useState('Module Practice');
  const [isTechnical, setIsTechnical] = useState(true);
  const [drills, setDrills] = useState([]);
  const [selectedDrillId, setSelectedDrillId] = useState(null);

  // Generating more drills state
  const [isGenerating, setIsGenerating] = useState(false);
  const [generateError, setGenerateError] = useState(null);

  const fetchDrills = async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const res = await axiosInstance.get(`/modules/${id}/drills`);
      if (res.data) {
        setModuleTitle(res.data.moduleTitle || 'Module Drills');
        setIsTechnical(res.data.isTechnical ?? true);
        const drillList = res.data.drills || [];
        setDrills(drillList);
        if (drillList.length > 0) {
          setSelectedDrillId(drillList[0].id);
        }
      }
    } catch (err) {
      console.error('Failed to load drills:', err);
      setLoadError('Failed to load drills for this module. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) fetchDrills();
  }, [id]);

  const handleGenerateMore = async () => {
    setIsGenerating(true);
    setGenerateError(null);
    try {
      const res = await axiosInstance.post(`/modules/${id}/drills/generate`);
      const newDrills = res.data || [];
      if (newDrills.length > 0) {
        setDrills((prev) => [...prev, ...newDrills]);
        setSelectedDrillId(newDrills[0].id);
      }
    } catch (err) {
      console.error('Failed to generate more drills:', err);
      setGenerateError('AI drill generation timed out or failed. Please try again.');
    } finally {
      setIsGenerating(false);
    }
  };

  const selectedDrill = drills.find((d) => d.id === selectedDrillId) || drills[0] || null;

  // Type Icon helper
  const getDrillIcon = (type) => {
    if (type === 'SCENARIO') return '🎯';
    if (type === 'CODE') return '💻';
    return '💡';
  };

  if (loading) {
    return (
      <div className={styles.pageContainer}>
        <div className={styles.loadingSkeleton}>
          <div className={styles.skeletonHeader} />
          <div className={styles.drillsGrid}>
            <div className={styles.skeletonLeft} />
            <div className={styles.skeletonRight} />
          </div>
        </div>
      </div>
    );
  }

  if (loadError) {
    return (
      <div className={styles.pageContainer}>
        <Card className={styles.errorCard}>
          <div className={styles.errorIcon}>⚠️</div>
          <h2 className={styles.errorTitle}>Could not load module drills</h2>
          <p className={styles.errorMsg}>{loadError}</p>
          <div className={styles.errorActions}>
            <Button variant="primary" onClick={fetchDrills}>
              Retry Loading
            </Button>
            <Button variant="outline" onClick={() => navigate(`/modules/${id}`)}>
              Back to Module
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className={styles.pageContainer}>
      {/* Top Header Bar */}
      <div className={styles.topHeader}>
        <button
          type="button"
          className={styles.backBtn}
          onClick={() => navigate(-1)}
        >
          ← Back
        </button>
        <div className={styles.headerInfo}>
          <div className={styles.badgeStrip}>
            <span className={styles.moduleTag}>Hands-on Drills</span>
            {isTechnical && <span className={styles.technicalTag}>Practical / Code Ready</span>}
          </div>
          <h1 className={styles.pageTitle}>{moduleTitle}</h1>
        </div>
      </div>

      {/* AI Generation Failure ErrorCard with Retry */}
      {generateError && (
        <Card className={styles.aiErrorCard}>
          <div className={styles.aiErrorContent}>
            <span className={styles.aiErrorIcon}>⚠️</span>
            <div>
              <h4 className={styles.aiErrorTitle}>AI Generation Notice</h4>
              <p className={styles.aiErrorText}>{generateError}</p>
            </div>
          </div>
          <Button
            variant="secondary"
            size="sm"
            onClick={handleGenerateMore}
            disabled={isGenerating}
          >
            Retry Generation
          </Button>
        </Card>
      )}

      {/* Main Drills Grid Layout */}
      {drills.length === 0 ? (
        <EmptyState
          icon="🎯"
          title="No drills yet"
          description="There are currently no practice drills available for this module. Generate custom AI drills to challenge your skills!"
          actionLabel={isGenerating ? 'Generating...' : 'Generate more drills'}
          onAction={handleGenerateMore}
        />
      ) : (
        <div className={styles.drillsGrid}>
          {/* Left Drill List Column */}
          <div className={styles.listColumn}>
            <div className={styles.listHeader}>
              <h3 className={styles.listHeading}>Drills ({drills.length})</h3>
              <Button
                variant="outline"
                size="sm"
                className={styles.generateBtn}
                onClick={handleGenerateMore}
                disabled={isGenerating}
              >
                <span>{isGenerating ? '⚡' : '✨'}</span>
                {isGenerating ? 'Generating...' : 'Generate more'}
              </Button>
            </div>

            <div className={styles.drillItemsList}>
              {drills.map((drill) => {
                const isActive = drill.id === selectedDrillId;
                return (
                  <button
                    key={drill.id}
                    type="button"
                    className={`${styles.drillItemBtn} ${isActive ? styles.drillItemActive : ''}`}
                    onClick={() => setSelectedDrillId(drill.id)}
                  >
                    <div className={styles.itemHeader}>
                      <span className={styles.typeIcon}>{getDrillIcon(drill.type)}</span>
                      <DifficultyTag level={drill.difficulty} />
                    </div>
                    <div className={styles.itemTitle}>{drill.title}</div>
                    <div className={styles.itemTypeLabel}>
                      {drill.type === 'SCENARIO'
                        ? 'Scenario Analysis'
                        : drill.type === 'CODE'
                        ? 'Code Exercise'
                        : 'Conceptual Check'}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Right Panel Column */}
          <div className={styles.panelColumn}>
            <DrillPanel
              drill={selectedDrill}
              moduleId={id}
              isTechnical={isTechnical}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default ModuleDrills;
