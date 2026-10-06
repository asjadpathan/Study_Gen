import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axiosInstance from '../services/axiosInstance';
import { useToast } from '../context/ToastContext';
import AIStageLoader from '../components/AIStageLoader';
import RateLimitBanner from '../components/RateLimitBanner';
import styles from './Career.module.css';

export default function Career() {
  const navigate = useNavigate();
  const { addToast } = useToast();

  // Active Tab: 'new' | 'history'
  const [activeTab, setActiveTab] = useState('new');

  // Input Form State
  const [inputType, setInputType] = useState('text'); // 'text' | 'url'
  const [jobTitle, setJobTitle] = useState('');
  const [company, setCompany] = useState('');
  const [jobDescription, setJobDescription] = useState('');
  const [url, setUrl] = useState('');

  // Status & Results
  const [loading, setLoading] = useState(false);
  const [rateLimited, setRateLimited] = useState(false);
  const [retryAfter, setRetryAfter] = useState(null);
  const [urlError, setUrlError] = useState(null);
  const [generalError, setGeneralError] = useState(null);
  const [currentResult, setCurrentResult] = useState(null);

  // History State
  const [historyList, setHistoryList] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [historyError, setHistoryError] = useState(null);

  // Fetch match history on mount or when switching to History tab
  const fetchHistory = async () => {
    try {
      setLoadingHistory(true);
      setHistoryError(null);
      const res = await axiosInstance.get('/career/job-matches');
      setHistoryList(res.data || []);
    } catch (err) {
      console.error('Failed to load career matches history:', err);
      setHistoryError('Could not load past job matches. Please try again.');
    } finally {
      setLoadingHistory(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'history') {
      fetchHistory();
    }
  }, [activeTab]);

  // Handle Analyzing Job Match
  const handleAnalyze = async (e) => {
    e?.preventDefault();
    setUrlError(null);
    setGeneralError(null);
    setRateLimited(false);

    if (inputType === 'text' && (!jobDescription || jobDescription.trim().length < 20)) {
      setGeneralError('Please enter at least 20 characters of the job description.');
      return;
    }

    if (inputType === 'url') {
      if (!url || !url.trim().startsWith('http')) {
        setUrlError('Please enter a valid job posting URL starting with http:// or https://');
        return;
      }
    }

    setLoading(true);

    try {
      const payload = {
        jobTitle: jobTitle.trim() || undefined,
        company: company.trim() || undefined,
        jobDescription: inputType === 'text' ? jobDescription.trim() : undefined,
        url: inputType === 'url' ? url.trim() : undefined,
      };

      const res = await axiosInstance.post('/career/job-match', payload);
      setCurrentResult(res.data);
      addToast('Job match analysis completed!', 'success');
      // If analyzing was successful, clear inputs
      if (inputType === 'url') setUrl('');
    } catch (err) {
      console.error('Job match analysis error:', err);
      if (err.response?.status === 429) {
        setRateLimited(true);
        const retryHeader = err.response.headers?.['retry-after'];
        if (retryHeader) setRetryAfter(parseInt(retryHeader, 10));
      } else if (err.response?.status === 400) {
        const errorMsg = err.response.data?.error || err.response.data?.message || err.message;
        if (inputType === 'url' || (errorMsg && errorMsg.toLowerCase().includes('url'))) {
          setUrlError(errorMsg);
        } else {
          setGeneralError(errorMsg);
        }
      } else {
        setGeneralError('Analysis could not be completed. Please verify your input and try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  // Open a saved job match from history
  const handleSelectHistoryItem = async (matchId) => {
    try {
      setLoading(true);
      const res = await axiosInstance.get(`/career/job-matches/${matchId}`);
      setCurrentResult(res.data);
      setActiveTab('new'); // View in results layout
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (err) {
      console.error('Failed to load match detail:', err);
      addToast('Could not load the saved match details.', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Build roadmap for this job navigation
  const handleBuildRoadmap = () => {
    if (!currentResult) return;
    navigate('/generate-roadmap', {
      state: {
        jobDescription: currentResult.jobDescription || '',
        targetRole: currentResult.jobTitle || 'Software Engineer',
        targetTopic: currentResult.jobTitle || 'Software Engineering',
        initialStep: 5,
      },
    });
  };

  // Donut Gauge calculations
  const matchPercent = currentResult?.matchPercentage || 0;
  const radius = 54;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (matchPercent / 100) * circumference;

  // Donut stroke color
  const getGaugeColor = (pct) => {
    if (pct >= 80) return 'var(--color-success, #10b981)';
    if (pct >= 50) return 'var(--color-secondary, #f59e0b)';
    return 'var(--color-danger, #ef4444)';
  };

  return (
    <div className={styles.container}>
      {/* Page Header */}
      <header className={styles.pageHeader}>
        <div className={styles.headerLeft}>
          <div className={styles.badgeRow}>
            <span className={styles.categoryBadge}>💼 Career Intelligence</span>
          </div>
          <h1 className={styles.title}>Role Match & Skill Gap Analysis</h1>
          <p className={styles.subtitle}>
            Cross-reference your completed roadmap modules against any live job description or URL to
            measure your qualifications and close skill gaps.
          </p>
        </div>

        {/* Tab Navigation */}
        <div className={styles.tabButtons}>
          <button
            className={`${styles.tabBtn} ${activeTab === 'new' ? styles.tabBtnActive : ''}`}
            onClick={() => setActiveTab('new')}
          >
            <span>✨ New Match</span>
          </button>
          <button
            className={`${styles.tabBtn} ${activeTab === 'history' ? styles.tabBtnActive : ''}`}
            onClick={() => setActiveTab('history')}
          >
            <span>📜 Match History</span>
          </button>
        </div>
      </header>

      {/* =========================================================
          TAB 1: NEW MATCH / CURRENT RESULT
         ========================================================= */}
      {activeTab === 'new' && (
        <div className={styles.tabContent}>
          {/* Rate Limit Banner if 429 */}
          {rateLimited && (
            <div className={styles.bannerRow}>
              <RateLimitBanner retryAfterSeconds={retryAfter} />
            </div>
          )}

          {/* AI Loader state while analyzing */}
          {loading && (
            <div className={styles.loaderWrapper}>
              <AIStageLoader
                title="Evaluating Job Match & Technical Gaps"
                messages={[
                  'Scanning job description & technical requirements...',
                  'Detecting seniority level & key engineering responsibilities...',
                  'Cross-referencing your completed roadmap modules & verified concepts...',
                  'Calculating competency match percentage & identifying missing skills...',
                  'Synthesizing gap-closing learning paths and recommendations...',
                ]}
              />
            </div>
          )}

          {/* New Match Input Card (Visible when not loading) */}
          {!loading && (
            <div className={styles.inputCard}>
              <div className={styles.inputCardHeader}>
                <div className={styles.modeToggle}>
                  <button
                    type="button"
                    className={`${styles.modeBtn} ${inputType === 'text' ? styles.modeBtnActive : ''}`}
                    onClick={() => setInputType('text')}
                  >
                    📝 Paste Job Description
                  </button>
                  <button
                    type="button"
                    className={`${styles.modeBtn} ${inputType === 'url' ? styles.modeBtnActive : ''}`}
                    onClick={() => setInputType('url')}
                  >
                    🔗 Import from Job URL
                  </button>
                </div>
                {currentResult && (
                  <button
                    type="button"
                    className={styles.resetBtn}
                    onClick={() => {
                      setCurrentResult(null);
                      setJobDescription('');
                      setUrl('');
                    }}
                  >
                    + Analyze Another Role
                  </button>
                )}
              </div>

              <form onSubmit={handleAnalyze} className={styles.formBody}>
                {/* Optional Title & Company row */}
                <div className={styles.formRow}>
                  <div className={styles.fieldGroup}>
                    <label htmlFor="jobTitleInput" className={styles.label}>
                      Target Role Title <span className={styles.optionalText}>(Optional)</span>
                    </label>
                    <input
                      id="jobTitleInput"
                      type="text"
                      placeholder="e.g. Senior Backend Engineer"
                      value={jobTitle}
                      onChange={(e) => setJobTitle(e.target.value)}
                      className={styles.input}
                    />
                  </div>
                  <div className={styles.fieldGroup}>
                    <label htmlFor="companyInput" className={styles.label}>
                      Company Name <span className={styles.optionalText}>(Optional)</span>
                    </label>
                    <input
                      id="companyInput"
                      type="text"
                      placeholder="e.g. Stripe, Netflix, Google"
                      value={company}
                      onChange={(e) => setCompany(e.target.value)}
                      className={styles.input}
                    />
                  </div>
                </div>

                {/* Textarea Input Mode */}
                {inputType === 'text' && (
                  <div className={styles.fieldGroup}>
                    <div className={styles.labelRow}>
                      <label htmlFor="descTextarea" className={styles.label}>
                        Job Description Requirements & Qualifications
                      </label>
                      <span
                        className={`${styles.charCount} ${
                          jobDescription.length >= 11500 ? styles.charCountWarn : ''
                        }`}
                      >
                        {jobDescription.length} / 12,000 characters
                      </span>
                    </div>
                    <textarea
                      id="descTextarea"
                      rows={6}
                      maxLength={12000}
                      placeholder="Paste the full job posting text including responsibilities, core tech stack, and qualifications..."
                      value={jobDescription}
                      onChange={(e) => setJobDescription(e.target.value)}
                      className={styles.textarea}
                    />
                  </div>
                )}

                {/* URL Input Mode */}
                {inputType === 'url' && (
                  <div className={styles.fieldGroup}>
                    <label htmlFor="urlField" className={styles.label}>
                      Job Posting URL
                    </label>
                    <input
                      id="urlField"
                      type="url"
                      placeholder="https://boards.greenhouse.io/company/jobs/..."
                      value={url}
                      onChange={(e) => setUrl(e.target.value)}
                      className={styles.input}
                    />
                    <span className={styles.urlHint}>
                      Supports public job board URLs (Greenhouse, Lever, LinkedIn public, company career pages).
                    </span>
                  </div>
                )}

                {/* Unreachable / Blocked URL Alert */}
                {urlError && (
                  <div className={styles.errorBanner}>
                    <span className={styles.errorIcon}>⚠️</span>
                    <div>
                      <strong>URL Error:</strong> {urlError}
                    </div>
                  </div>
                )}

                {/* General Error Alert */}
                {generalError && (
                  <div className={styles.errorBanner}>
                    <span className={styles.errorIcon}>⚠️</span>
                    <div>{generalError}</div>
                  </div>
                )}

                <div className={styles.formFooter}>
                  <button type="submit" className={styles.submitBtn} disabled={loading}>
                    <span className={styles.sparkle}>✨</span> Analyze Job Match
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* =========================================================
              RESULTS VIEW
             ========================================================= */}
          {currentResult && !loading && (
            <div className={styles.resultsContainer}>
              {/* No Completed Modules Callout */}
              {currentResult.userCompletedModulesCount === 0 && (
                <div className={styles.warningCallout}>
                  <div className={styles.warningLeft}>
                    <span className={styles.warningIcon}>💡</span>
                    <div>
                      <h4 className={styles.warningTitle}>No Completed Modules Yet</h4>
                      <p className={styles.warningText}>
                        You haven't completed any roadmap modules or passed tests yet. As you master modules,
                        your verified skills will turn green and increase your match percentage!
                      </p>
                    </div>
                  </div>
                  <Link to="/roadmap-workspace" className={styles.warningBtn}>
                    Go to Roadmaps →
                  </Link>
                </div>
              )}

              {/* Top Overview: Role Header & Match Gauge */}
              <div className={styles.overviewGrid}>
                {/* Role Header Card */}
                <div className={styles.roleCard}>
                  <div className={styles.roleHeaderTop}>
                    <span className={styles.seniorityTag}>
                      {currentResult.seniority || 'Mid-Level'}
                    </span>
                    {currentResult.company && (
                      <span className={styles.companyChip}>🏢 {currentResult.company}</span>
                    )}
                  </div>

                  <h2 className={styles.roleTitle}>{currentResult.jobTitle}</h2>
                  <p className={styles.adviceCallout}>
                    <strong>AI Verdict:</strong> {currentResult.actionableAdvice}
                  </p>

                  <div className={styles.responsibilitiesSection}>
                    <h4 className={styles.respHeading}>Key Responsibilities</h4>
                    <ul className={styles.respList}>
                      {(currentResult.responsibilities || []).slice(0, 6).map((resp, idx) => (
                        <li key={idx} className={styles.respItem}>
                          <span className={styles.respBullet}>•</span>
                          <span>{resp}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>

                {/* Match Gauge Card */}
                <div className={styles.gaugeCard}>
                  <div className={styles.gaugeHeader}>
                    <h4 className={styles.gaugeTitle}>Match Readiness</h4>
                    <span
                      className={styles.statusBadge}
                      style={{
                        background:
                          matchPercent >= 80 ? '#ecfdf5' : matchPercent >= 50 ? '#fffbeb' : '#fef2f2',
                        color: getGaugeColor(matchPercent),
                        borderColor: getGaugeColor(matchPercent),
                      }}
                    >
                      {matchPercent >= 80
                        ? '✅ Ready to Apply'
                        : matchPercent >= 50
                        ? '⚡ Strong Fit'
                        : '🎯 Skill Gap'}
                    </span>
                  </div>

                  {/* SVG Donut Gauge */}
                  <div className={styles.donutWrapper}>
                    <svg className={styles.donutSvg} width="160" height="160" viewBox="0 0 140 140">
                      {/* Background circle track */}
                      <circle
                        cx="70"
                        cy="70"
                        r={radius}
                        fill="transparent"
                        stroke="#f1f5f9"
                        strokeWidth="14"
                      />
                      {/* Foreground animated value circle */}
                      <circle
                        cx="70"
                        cy="70"
                        r={radius}
                        fill="transparent"
                        stroke={getGaugeColor(matchPercent)}
                        strokeWidth="14"
                        strokeDasharray={circumference}
                        strokeDashoffset={strokeDashoffset}
                        strokeLinecap="round"
                        transform="rotate(-90 70 70)"
                        className={styles.donutProgress}
                      />
                    </svg>
                    <div className={styles.donutLabel}>
                      <span className={styles.donutNumber}>{matchPercent}%</span>
                      <span className={styles.donutSub}>Matched</span>
                    </div>
                  </div>

                  <p className={styles.gaugeSentence}>
                    You match <strong>{matchPercent}%</strong> of the core technical requirements for this role.
                  </p>

                  <div className={styles.gaugeStats}>
                    <div className={styles.gaugeStatItem}>
                      <span className={styles.statNumber} style={{ color: 'var(--color-success, #10b981)' }}>
                        {(currentResult.matchedSkills || []).length}
                      </span>
                      <span className={styles.statLabel}>Skills Covered</span>
                    </div>
                    <div className={styles.gaugeStatItem}>
                      <span className={styles.statNumber} style={{ color: 'var(--color-danger, #ef4444)' }}>
                        {(currentResult.missingSkills || []).length}
                      </span>
                      <span className={styles.statLabel}>Skills Missing</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Skills Table Section */}
              <div className={styles.tableCard}>
                <div className={styles.tableHeader}>
                  <div>
                    <h3 className={styles.tableTitle}>Skill-by-Skill Competency Breakdown</h3>
                    <p className={styles.tableSub}>
                      Cross-referenced with your verified module test history and roadmaps.
                    </p>
                  </div>
                </div>

                <div className={styles.tableWrapper}>
                  <table className={styles.skillsTable}>
                    <thead>
                      <tr>
                        <th>Skill / Tool</th>
                        <th>Category</th>
                        <th>Importance</th>
                        <th>Status</th>
                        <th>Matched Module & Gap Plan</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(currentResult.skillsAnalysis || []).map((item, idx) => {
                        const isCovered = item.status === 'Covered';
                        const isPartly = item.status === 'Partly covered';
                        const isMissing = item.status === 'Missing';

                        return (
                          <tr key={idx} className={styles.tableRow}>
                            <td className={styles.skillCell}>
                              <span className={styles.skillName}>{item.skill}</span>
                            </td>
                            <td>
                              <span className={styles.categoryPill}>{item.category}</span>
                            </td>
                            <td>
                              <span
                                className={`${styles.importancePill} ${
                                  item.importance === 'Required'
                                    ? styles.importanceRequired
                                    : styles.importanceNice
                                }`}
                              >
                                {item.importance}
                              </span>
                            </td>
                            <td>
                              <span
                                className={`${styles.statusPill} ${
                                  isCovered
                                    ? styles.statusCovered
                                    : isPartly
                                    ? styles.statusPartly
                                    : styles.statusMissing
                                }`}
                              >
                                {isCovered ? '● Covered' : isPartly ? '◐ Partly covered' : '○ Missing'}
                              </span>
                            </td>
                            <td className={styles.gapCell}>
                              {item.matchedModuleId ? (
                                <Link
                                  to={`/modules/${item.matchedModuleId}`}
                                  className={styles.moduleLink}
                                >
                                  📖 {item.matchedModuleTitle || 'Open Module'} →
                                </Link>
                              ) : isMissing ? (
                                <span className={styles.gapNote}>
                                  💡 <strong>Gap plan:</strong> {item.gapAdvice}
                                </span>
                              ) : (
                                <span className={styles.neutralNote}>{item.gapAdvice}</span>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Suggested Modules Row */}
              {currentResult.suggestedModules && currentResult.suggestedModules.length > 0 && (
                <div className={styles.suggestionsCard}>
                  <div className={styles.suggestionsHeader}>
                    <div>
                      <h3 className={styles.suggestionsTitle}>Suggested Modules to Close Gaps</h3>
                      <p className={styles.suggestionsSub}>
                        Complete these modules to turn missing skills green and achieve a 100% role match.
                      </p>
                    </div>
                  </div>

                  <div className={styles.modulesGrid}>
                    {currentResult.suggestedModules.map((mod, idx) => (
                      <div key={idx} className={styles.moduleCard}>
                        <div className={styles.modTop}>
                          <span className={styles.modSkillTag}>🎯 {mod.associatedSkill}</span>
                          <span className={styles.modHours}>~{mod.estimatedHours || 4} hrs</span>
                        </div>
                        <h4 className={styles.modTitle}>{mod.title}</h4>
                        <p className={styles.modDesc}>{mod.description}</p>
                        <div className={styles.modFooter}>
                          {mod.moduleId ? (
                            <Link to={`/modules/${mod.moduleId}`} className={styles.modActionBtn}>
                              Resume Module →
                            </Link>
                          ) : (
                            <button
                              type="button"
                              onClick={handleBuildRoadmap}
                              className={styles.modActionBtnOutline}
                            >
                              Add to Roadmap +
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Action Banner: Build Roadmap for this job */}
              <div className={styles.ctaBanner}>
                <div className={styles.ctaLeft}>
                  <span className={styles.ctaIcon}>🚀</span>
                  <div>
                    <h3 className={styles.ctaTitle}>Accelerate Your Preparation</h3>
                    <p className={styles.ctaDesc}>
                      Generate a bespoke, step-by-step roadmap tailored exactly to this job description and your
                      current competency level.
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={handleBuildRoadmap}
                  className={styles.buildRoadmapBtn}
                >
                  Build a Roadmap for this Job →
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* =========================================================
          TAB 2: MATCH HISTORY
         ========================================================= */}
      {activeTab === 'history' && (
        <div className={styles.historyTabContent}>
          {loadingHistory ? (
            <div className={styles.historyLoading}>
              <div className={styles.spinner}></div>
              <span>Loading saved match history...</span>
            </div>
          ) : historyError ? (
            <div className={styles.historyErrorCard}>
              <p>{historyError}</p>
              <button onClick={fetchHistory} className={styles.retryBtn}>
                Retry
              </button>
            </div>
          ) : historyList.length === 0 ? (
            <div className={styles.emptyHistoryCard}>
              <span className={styles.emptyIcon}>📂</span>
              <h3 className={styles.emptyTitle}>No Match History Yet</h3>
              <p className={styles.emptyDesc}>
                You haven't analyzed any job postings yet. Paste a job description or URL in the "New Match"
                tab to evaluate your readiness.
              </p>
              <button onClick={() => setActiveTab('new')} className={styles.emptyCta}>
                + Analyze Your First Job
              </button>
            </div>
          ) : (
            <div className={styles.historyList}>
              <div className={styles.historyHeaderRow}>
                <span>Found {historyList.length} analyzed positions</span>
              </div>
              <div className={styles.historyGrid}>
                {historyList.map((item) => (
                  <div
                    key={item.id}
                    className={styles.historyCard}
                    onClick={() => handleSelectHistoryItem(item.id)}
                  >
                    <div className={styles.historyCardTop}>
                      <span className={styles.historySeniority}>{item.seniority || 'Mid-Level'}</span>
                      <span
                        className={styles.historyPercentBadge}
                        style={{
                          color: getGaugeColor(item.matchPercentage),
                          borderColor: getGaugeColor(item.matchPercentage),
                        }}
                      >
                        {item.matchPercentage}% Match
                      </span>
                    </div>

                    <h4 className={styles.historyJobTitle}>{item.jobTitle}</h4>
                    <p className={styles.historyCompany}>🏢 {item.company}</p>

                    <div className={styles.historyCardBottom}>
                      <span className={styles.historyDate}>📅 {item.createdAt}</span>
                      <span className={styles.viewResultLink}>View Report →</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
