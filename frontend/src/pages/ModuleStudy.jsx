import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axiosInstance from '../services/axiosInstance';
import { useToast } from '../context/ToastContext';
import styles from './ModuleStudy.module.css';

const renderMarkdownToHtml = (markdown) => {
  if (!markdown) return '';

  const lines = markdown
    .replace(/\r\n/g, '\n')
    .split('\n')
    .map((line) => line.trimEnd())
    .filter((line) => line.trim() !== '#');

  const htmlChunks = [];
  let inUl = false;
  let inOl = false;

  const closeLists = () => {
    if (inUl) {
      htmlChunks.push('</ul>');
      inUl = false;
    }
    if (inOl) {
      htmlChunks.push('</ol>');
      inOl = false;
    }
  };

  const formatInline = (text) => {
    return text
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/`([^`]+)`/g, '<code class="inlineCode">$1</code>')
      .replace(/\[x\]/gi, '☑ ')
      .replace(/\[ \]/g, '☐ ');
  };

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmed = line.trim();

    if (!trimmed) {
      closeLists();
      continue;
    }

    if (trimmed.startsWith('#### ')) {
      closeLists();
      htmlChunks.push(`<h4>${formatInline(trimmed.slice(5))}</h4>`);
    } else if (trimmed.startsWith('### ')) {
      closeLists();
      htmlChunks.push(`<h3>${formatInline(trimmed.slice(4))}</h3>`);
    } else if (trimmed.startsWith('## ')) {
      closeLists();
      htmlChunks.push(`<h2>${formatInline(trimmed.slice(3))}</h2>`);
    } else if (/^[-*]\s+/.test(trimmed)) {
      if (inOl) {
        htmlChunks.push('</ol>');
        inOl = false;
      }
      if (!inUl) {
        htmlChunks.push('<ul class="contentList">');
        inUl = true;
      }
      const itemContent = trimmed.replace(/^[-*]\s+/, '');
      htmlChunks.push(`<li>${formatInline(itemContent)}</li>`);
    } else if (/^\d+\.\s+/.test(trimmed)) {
      if (inUl) {
        htmlChunks.push('</ul>');
        inUl = false;
      }
      if (!inOl) {
        htmlChunks.push('<ol class="contentOrderedList">');
        inOl = true;
      }
      const itemContent = trimmed.replace(/^\d+\.\s+/, '');
      htmlChunks.push(`<li>${formatInline(itemContent)}</li>`);
    } else {
      closeLists();
      htmlChunks.push(`<p>${formatInline(trimmed)}</p>`);
    }
  }

  closeLists();
  return htmlChunks.join('\n');
};

const ModuleStudy = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useToast();

  const [loading, setLoading] = useState(true);
  const [moduleData, setModuleData] = useState(null);
  const [activePartIndex, setActivePartIndex] = useState(0);
  const [completedParts, setCompletedParts] = useState({});
  const [copied, setCopied] = useState(false);

  // Tab: 'lesson' (Guided Sub-Parts) vs 'guide' (Full Study Guide)
  const [activeTab, setActiveTab] = useState('lesson');
  const [guideData, setGuideData] = useState(null);
  const [guideLoading, setGuideLoading] = useState(false);

  useEffect(() => {
    const fetchModuleDetails = async () => {
      setLoading(true);
      try {
        const res = await axiosInstance.get(`/modules/${id}`);
        setModuleData(res.data);

        // Pre-populate completed parts if any
        if (res.data?.subParts) {
          const initialCompleted = {};
          res.data.subParts.forEach((sp, idx) => {
            if (sp.completed) initialCompleted[idx] = true;
          });
          setCompletedParts(initialCompleted);
        }
      } catch (err) {
        if (err.response?.status === 403) {
          navigate('/locked', {
            state: {
              title: 'Access Restricted: Module Locked',
              description: 'This technical module is locked. Complete the prerequisite modules and assessments first.',
            },
          });
          return;
        }
        addToast('Failed to load module teaching content.', 'error');
      } finally {
        setLoading(false);
      }
    };

    fetchModuleDetails();
  }, [id, navigate, addToast]);

  // Load Full Study Guide when the user switches to that tab
  const handleTabChange = async (tab) => {
    setActiveTab(tab);
    if (tab === 'guide' && !guideData && !guideLoading) {
      setGuideLoading(true);
      try {
        const res = await axiosInstance.get(`/study-guide/modules/${id}`);
        setGuideData(res.data);
      } catch (err) {
        addToast('Study guide currently unavailable for this module.', 'info');
      } finally {
        setGuideLoading(false);
      }
    }
  };

  const subParts = moduleData?.subParts || [];
  const currentPart = subParts[activePartIndex] || subParts[0];

  const handleTogglePartComplete = (index) => {
    setCompletedParts((prev) => ({
      ...prev,
      [index]: !prev[index],
    }));
  };

  const handleCopySnippet = (text) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    addToast('Code snippet copied to clipboard!', 'success');
    setTimeout(() => setCopied(false), 2000);
  };

  if (loading) {
    return (
      <div className={styles.container}>
        <div style={{ textAlign: 'center', padding: '80px 0' }}>
          <div style={{ fontSize: '2.5rem', marginBottom: '16px' }}>📚</div>
          <h3>Loading Module Teaching Materials...</h3>
          <p style={{ color: '#6b7280' }}>Preparing concepts, code examples, and structured lessons.</p>
        </div>
      </div>
    );
  }

  if (!moduleData) {
    return (
      <div className={styles.container}>
        <div style={{ textAlign: 'center', padding: '60px 0' }}>
          <h3>Module not found</h3>
          <button
            type="button"
            className={styles.prevModuleBtn}
            onClick={() => navigate('/roadmaps')}
            style={{ marginTop: '16px' }}
          >
            ← Back to Roadmaps
          </button>
        </div>
      </div>
    );
  }

  const completedCount = Object.values(completedParts).filter(Boolean).length;

  return (
    <div className={styles.container}>
      {/* Breadcrumb Navigation */}
      <nav className={styles.breadcrumb} aria-label="Breadcrumb">
        <button
          type="button"
          className={styles.breadcrumbLink}
          onClick={() =>
            moduleData.roadmapId
              ? navigate(`/roadmaps/${moduleData.roadmapId}`)
              : navigate('/roadmaps')
          }
        >
          ← Back to Roadmap: {moduleData.roadmapTitle || 'Roadmap'}
        </button>
      </nav>

      {/* Module Header Card */}
      <header className={styles.moduleHeader}>
        <div className={styles.headerLeft}>
          <div className={styles.metaRow}>
            <span className={styles.seqPill}>Module {moduleData.sequenceOrder + 1}</span>
            {moduleData.isCompleted ? (
              <span className={styles.statusCompleted}>
                ✓ Mastered ({Math.round(moduleData.masteryScore || 100)}%)
              </span>
            ) : (
              <span className={styles.statusInProgress}>⚡ Active Teaching Module</span>
            )}
          </div>
          <h1 className={styles.moduleTitle}>{moduleData.title}</h1>
          <p className={styles.moduleDesc}>{moduleData.description}</p>
        </div>
      </header>

      {/* View Switcher: Guided Sub-Parts vs Full Study Guide */}
      <div className={styles.tabGroup} role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'lesson'}
          className={`${styles.tabBtn} ${activeTab === 'lesson' ? styles.tabBtnActive : ''}`}
          onClick={() => handleTabChange('lesson')}
        >
          📖 Step-by-Step Lessons ({subParts.length} Parts)
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'guide'}
          className={`${styles.tabBtn} ${activeTab === 'guide' ? styles.tabBtnActive : ''}`}
          onClick={() => handleTabChange('guide')}
        >
          📑 Comprehensive Study Guide
        </button>
      </div>

      {activeTab === 'lesson' ? (
        /* Two-Column Layout: Sub-Parts Rail + Teaching Reader */
        <div className={styles.studyGrid}>
          {/* Sub-Parts Stepper Rail */}
          <aside className={styles.partsRail}>
            <div className={styles.railHeader}>
              <h3 className={styles.railTitle}>Teaching Sub-Parts</h3>
              <span className={styles.progressLabel}>
                {completedCount} / {subParts.length}
              </span>
            </div>

            <div className={styles.partsList}>
              {subParts.map((part, index) => {
                const isActive = index === activePartIndex;
                const isDone = !!completedParts[index];

                return (
                  <button
                    key={part.id || index}
                    type="button"
                    className={`${styles.partItemBtn} ${isActive ? styles.partItemActive : ''}`}
                    onClick={() => setActivePartIndex(index)}
                  >
                    <div
                      className={`${styles.partCheck} ${
                        isDone ? styles.partCheckDone : isActive ? styles.partCheckActive : ''
                      }`}
                    >
                      {isDone ? '✓' : index + 1}
                    </div>
                    <div className={styles.partMeta}>
                      <span className={styles.partLevelTag}>{part.level || 'LESSON'}</span>
                      <span className={styles.partTitleText}>{part.title}</span>
                    </div>
                  </button>
                );
              })}
            </div>
          </aside>

          {/* Teaching Content Reader */}
          <main className={styles.teachingCard}>
            {currentPart ? (
              <div>
                <div className={styles.partHeader}>
                  <h2 className={styles.partHeading}>{currentPart.title}</h2>
                  <span
                    className={`${styles.levelBadge} ${
                      currentPart.level === 'BASIC'
                        ? styles.levelBasic
                        : currentPart.level === 'ADVANCED'
                        ? styles.levelAdvanced
                        : styles.levelIntermediate
                    }`}
                  >
                    {currentPart.level || 'INTERMEDIATE'} LEVEL
                  </span>
                </div>

                {/* Formatted Content */}
                <div
                  className={styles.contentBody}
                  dangerouslySetInnerHTML={{
                    __html: renderMarkdownToHtml(currentPart.contentBody),
                  }}
                />

                {/* Practical Code Demonstration */}
                {currentPart.codeSnippet && (
                  <div className={styles.codeSection}>
                    <div className={styles.codeHeader}>
                      <span>PRACTICAL IMPLEMENTATION DEMO</span>
                      <button
                        type="button"
                        className={styles.copyCodeBtn}
                        onClick={() => handleCopySnippet(currentPart.codeSnippet)}
                      >
                        {copied ? '✓ Copied' : '📋 Copy Code'}
                      </button>
                    </div>
                    <pre className={styles.codeBlock}>
                      <code>{currentPart.codeSnippet}</code>
                    </pre>
                  </div>
                )}

                {/* Common Pitfalls & Defense Box */}
                {currentPart.pitfalls && (
                  <div className={styles.pitfallsBox}>
                    <span className={styles.pitfallIcon}>⚠️</span>
                    <p className={styles.pitfallText}>
                      <strong>Common Pitfalls & Edge Cases: </strong>
                      {currentPart.pitfalls}
                    </p>
                  </div>
                )}

                {/* Key Takeaways Callout Box */}
                {currentPart.keyTakeaways && (
                  <div className={styles.takeawaysBox}>
                    <span className={styles.takeawayIcon}>💡</span>
                    <p className={styles.takeawayText}>
                      <strong>Core Takeaway: </strong>
                      {currentPart.keyTakeaways}
                    </p>
                  </div>
                )}

                {/* Reader Footer Navigation */}
                <div className={styles.readerFooter}>
                  <button
                    type="button"
                    className={styles.breadcrumbLink}
                    disabled={activePartIndex === 0}
                    onClick={() => setActivePartIndex((prev) => Math.max(0, prev - 1))}
                    style={{ opacity: activePartIndex === 0 ? 0.3 : 1 }}
                  >
                    ← Previous Sub-Part
                  </button>

                  <button
                    type="button"
                    className={styles.completeBtn}
                    onClick={() => {
                      handleTogglePartComplete(activePartIndex);
                      if (activePartIndex < subParts.length - 1) {
                        setActivePartIndex((prev) => prev + 1);
                      } else {
                        addToast('All topic sub-parts completed! You can now take the module test below.', 'success');
                      }
                    }}
                  >
                    {activePartIndex === subParts.length - 1
                      ? (completedParts[activePartIndex] ? '✓ Topic Learning Complete' : 'Mark Topic Finished & Complete ➔')
                      : (completedParts[activePartIndex] ? '✓ Part Understood' : 'Mark as Finished & Next Topic →')}
                  </button>
                </div>
              </div>
            ) : (
              <p>No teaching content available for this sub-part.</p>
            )}
          </main>
        </div>
      ) : (
        /* Full Comprehensive Study Guide View */
        <div className={styles.guideContainer}>
          {guideLoading ? (
            <div style={{ textAlign: 'center', padding: '40px 0' }}>
              <p>Generating and loading study guide...</p>
            </div>
          ) : guideData ? (
            <div>
              {guideData.keyTakeaways && guideData.keyTakeaways.length > 0 && (
                <div className={styles.guideTakeaways}>
                  <h3 className={styles.guideTakeawaysTitle}>
                    <span>💡</span> Key High-Impact Takeaways
                  </h3>
                  {guideData.keyTakeaways.map((takeaway, i) => (
                    <div key={i} className={styles.guideTakeawayItem}>
                      • {takeaway}
                    </div>
                  ))}
                </div>
              )}
              <div
                className={styles.guideContent}
                dangerouslySetInnerHTML={{
                  __html: renderMarkdownToHtml(guideData.markdownContent),
                }}
              />
            </div>
          ) : (
            <p>No study guide available for this module yet.</p>
          )}
        </div>
      )}

      {/* Module-to-Module Progression Jump Bar (No Test Gate Required!) */}
      <nav className={styles.moduleJumpBar} aria-label="Module Progression">
        <div>
          {moduleData.previousModuleId ? (
            <button
              type="button"
              className={styles.prevModuleBtn}
              onClick={() => navigate(`/modules/${moduleData.previousModuleId}`)}
            >
              ← Previous: {moduleData.previousModuleTitle || 'Previous Module'}
            </button>
          ) : (
            <span style={{ fontSize: '0.875rem', color: '#6b7280' }}>
              🏁 First module of this curriculum
            </span>
          )}
        </div>

        <div>
          {moduleData.nextModuleId ? (
            moduleData.isCompleted ? (
              <button
                type="button"
                className={styles.nextModuleCTA}
                onClick={() => navigate(`/modules/${moduleData.nextModuleId}`)}
              >
                Continue to Next: {moduleData.nextModuleTitle || 'Next Module'} →
              </button>
            ) : (
              <button
                type="button"
                className={styles.nextModuleCTA}
                onClick={() => navigate(`/modules/${id}/test`)}
                title="Score 80% on this module's test to unlock the next module"
              >
                🔒 Next Module Locked · Pass Test (80%) to Unlock ➔
              </button>
            )
          ) : (
            <button
              type="button"
              className={styles.nextModuleCTA}
              onClick={() =>
                moduleData.roadmapId
                  ? navigate(`/roadmaps/${moduleData.roadmapId}`)
                  : navigate('/roadmaps')
              }
            >
              🏆 Curriculum Complete! View Roadmap →
            </button>
          )}
        </div>
      </nav>

      {/* Post-Learning Assessment Section: Teaching First, Assessment Second */}
      {completedCount >= subParts.length || completedParts[activePartIndex] ? (
        <section className={styles.completedLearningCard} aria-label="Completed Learning Assessment">
          <div className={styles.quizLeft}>
            <span className={styles.quizIcon}>🎓</span>
            <div>
              <h4 className={styles.quizTitle}>Topic Learning Complete! Ready for the Module Assessment?</h4>
              <p className={styles.quizDesc}>
                You have reviewed the key concepts, practical implementation code, and pitfalls. Verify your mastery with the 5-question module assessment!
              </p>
            </div>
          </div>

          <button
            type="button"
            className={styles.takeTestBtn}
            onClick={() => navigate(`/modules/${id}/test`)}
          >
            📝 Start Module Assessment (5 Questions) ➔
          </button>
        </section>
      ) : (
        <section className={styles.optionalQuizCard} aria-label="Module Assessment">
          <div className={styles.quizLeft}>
            <span className={styles.quizIcon}>📚</span>
            <div>
              <h4 className={styles.quizTitle}>Module Assessment (Available After Learning)</h4>
              <p className={styles.quizDesc}>
                Take your time to study the detailed topics and code examples above. Once you feel confident, take the assessment to test your understanding!
              </p>
            </div>
          </div>

          <button
            type="button"
            className={styles.quizBtn}
            onClick={() => navigate(`/modules/${id}/test`)}
          >
            Take Assessment Now ➔
          </button>
        </section>
      )}
    </div>
  );
};

export default ModuleStudy;
