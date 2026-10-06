import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axiosInstance from '../../services/axiosInstance';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import Button from '../../components/Button';
import Card from '../../components/Card';
import Skeleton from '../../components/Skeleton';
import ErrorCard from '../../components/ErrorCard';
import ProgressBar from '../../components/ProgressBar';
import StatusPill from '../../components/StatusPill';
import Heatmap from '../../components/Heatmap';
import styles from './Dashboard.module.css';

const Dashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { addToast } = useToast();

  // Section States: independent loading & error tracking
  const [dashLoading, setDashLoading] = useState(true);
  const [dashError, setDashError] = useState(null);
  const [dashData, setDashData] = useState(null);

  const [heatmapLoading, setHeatmapLoading] = useState(true);
  const [heatmapError, setHeatmapError] = useState(null);
  const [heatmapData, setHeatmapData] = useState([]);

  const [practiceLoading, setPracticeLoading] = useState(true);
  const [practiceError, setPracticeError] = useState(null);
  const [practiceDue, setPracticeDue] = useState(0);

  // Quick Upload State
  const [selectedRoadmapId, setSelectedRoadmapId] = useState('');
  const [isDragOver, setIsDragOver] = useState(false);
  const [uploading, setUploading] = useState(false);

  // Fetch all parallel sections on mount
  useEffect(() => {
    fetchDashboardData();
    fetchHeatmapData();
    fetchPracticeDueData();
  }, []);

  const fetchDashboardData = () => {
    setDashLoading(true);
    setDashError(null);
    axiosInstance
      .get('/users/me/dashboard')
      .then((res) => {
        setDashData(res.data);
      })
      .catch((err) => {
        setDashError('Failed to load dashboard statistics.');
      })
      .finally(() => setDashLoading(false));
  };

  const fetchHeatmapData = () => {
    setHeatmapLoading(true);
    setHeatmapError(null);
    axiosInstance
      .get('/analytics/streak-heatmap?days=180')
      .then((res) => {
        const heatmapList = res.data?.logs || res.data?.activities || res.data || [];
        setHeatmapData(Array.isArray(heatmapList) ? heatmapList : []);
      })
      .catch(() => {
        setHeatmapError('Failed to load activity heatmap.');
      })
      .finally(() => setHeatmapLoading(false));
  };

  const fetchPracticeDueData = () => {
    setPracticeLoading(true);
    setPracticeError(null);
    axiosInstance
      .get('/practice/due')
      .then((res) => {
        setPracticeDue(res.data?.dueCount !== undefined ? res.data.dueCount : 5);
      })
      .catch(() => {
        setPracticeError('Failed to load flashcards status.');
      })
      .finally(() => setPracticeLoading(false));
  };

  // Drag & Drop File Upload Handler
  const handleFileUpload = async (files) => {
    if (!files || files.length === 0) return;
    const file = files[0];

    const formData = new FormData();
    formData.append('file', file);
    if (selectedRoadmapId) {
      formData.append('roadmapId', selectedRoadmapId);
    }

    setUploading(true);
    try {
      await axiosInstance.post('/documents/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      addToast(`Successfully uploaded document: ${file.name}`, 'success');
      navigate('/document-analyzer');
    } catch {
      addToast('Document upload failed. Please try again.', 'error');
    } finally {
      setUploading(false);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = () => {
    setIsDragOver(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);
    if (e.dataTransfer.files) {
      handleFileUpload(e.dataTransfer.files);
    }
  };

  const userName = dashData?.fullName || user?.fullName || 'Learner';
  const roadmaps = dashData?.roadmaps || [];
  const isEmptyState = !dashLoading && !dashError && roadmaps.length === 0;

  return (
    <div className={styles.dashboardContainer}>
      {/* Top Greeting Header */}
      <div className={styles.greetingHeader}>
        <div>
          <h1 className={styles.greetingTitle}>Welcome back, {userName}! 👋</h1>
          <p className={styles.greetingSubtitle}>
            Here is an overview of your active study roadmaps, consistency streak, and practice drills.
          </p>
        </div>
        <Button
          variant="primary"
          size="md"
          onClick={() => {
            const activeRoadmap = roadmaps[0];
            if (activeRoadmap) {
              navigate('/roadmap-workspace');
            } else {
              navigate('/generate-roadmap');
            }
          }}
        >
          Continue Learning <span>→</span>
        </Button>
      </div>

      {/* Main Dashboard Grid */}
      <div className={styles.dashboardGrid}>
        {/* ===================================================
            SECTION 1: 4-Card Stat Row or Onboarding Empty State
            =================================================== */}
        <div className={styles.fullRow}>
          {dashLoading ? (
            <div className={styles.statGrid}>
              <Skeleton height="100px" borderRadius="var(--radius-xl)" />
              <Skeleton height="100px" borderRadius="var(--radius-xl)" />
              <Skeleton height="100px" borderRadius="var(--radius-xl)" />
              <Skeleton height="100px" borderRadius="var(--radius-xl)" />
            </div>
          ) : dashError ? (
            <ErrorCard message={dashError} onRetry={fetchDashboardData} />
          ) : isEmptyState ? (
            /* Empty State Onboarding Variant */
            <Card className={styles.emptyOnboardingCard}>
              <div className={styles.onboardingHeader}>
                <span className={styles.onboardingIcon}>🚀</span>
                <h2>Welcome to StudyGen! Let's Get Started</h2>
                <p>Follow these 3 quick steps to kickstart your personalized AI study path.</p>
              </div>

              <div className={styles.checklistGrid}>
                <div className={styles.checkItem}>
                  <div className={styles.checkBadge}>1</div>
                  <h4>Create your learning roadmap</h4>
                  <p>Input your target role or topic and let AI structure your modules.</p>
                </div>

                <div className={styles.checkItem}>
                  <div className={styles.checkBadge}>2</div>
                  <h4>Take diagnostic assessment</h4>
                  <p>Evaluate your starting baseline to focus only on new skills.</p>
                </div>

                <div className={styles.checkItem}>
                  <div className={styles.checkBadge}>3</div>
                  <h4>Start your first study session</h4>
                  <p>Engage with concepts, flashcards, and instant AI explanations.</p>
                </div>
              </div>

              <div className={styles.emptyActionWrapper}>
                <Button
                  variant="primary"
                  size="lg"
                  onClick={() => navigate('/generate-roadmap')}
                >
                  ✨ Create your first roadmap
                </Button>
              </div>
            </Card>
          ) : (
            /* 4-Card Stat Row */
            <div className={styles.statGrid}>
              <Card className={styles.statCard}>
                <div className={styles.statHeader}>
                  <span className={styles.statIcon}>🔥</span>
                  <span className={styles.statTitle}>Current Streak</span>
                </div>
                <div className={styles.statValue}>{dashData?.currentStreak || 0} days</div>
                <div className={styles.statSubtext}>Keep up your daily study momentum</div>
              </Card>

              <Card className={styles.statCard}>
                <div className={styles.statHeader}>
                  <span className={styles.statIcon}>⏱️</span>
                  <span className={styles.statTitle}>Study Time (This Week)</span>
                </div>
                <div className={styles.statValue}>{dashData?.weeklyStudyMinutes || 0} mins</div>
                <div className={styles.statSubtext}>Logged across focused sessions</div>
              </Card>

              <Card className={styles.statCard}>
                <div className={styles.statHeader}>
                  <span className={styles.statIcon}>🎯</span>
                  <span className={styles.statTitle}>Modules Completed</span>
                </div>
                <div className={styles.statValue}>{dashData?.completedModules || 0}</div>
                <div className={styles.statSubtext}>Mastered module milestones</div>
              </Card>

              <Card className={styles.statCard}>
                <div className={styles.statHeader}>
                  <span className={styles.statIcon}>🏆</span>
                  <span className={styles.statTitle}>Assessments Passed</span>
                </div>
                <div className={styles.statValue}>{dashData?.passedTests || 0}</div>
                <div className={styles.statSubtext}>Verified diagnostic & quiz tests</div>
              </Card>
            </div>
          )}
        </div>

        {/* ===================================================
            SECTION 2: Active Roadmaps Card Grid
            =================================================== */}
        {!isEmptyState && (
          <div className={styles.mainColumn}>
            <h2 className={styles.sectionHeaderTitle}>Active Study Roadmaps</h2>
            {dashLoading ? (
              <Skeleton height="180px" borderRadius="var(--radius-xl)" />
            ) : dashError ? (
              <ErrorCard message={dashError} onRetry={fetchDashboardData} />
            ) : (
              <div className={styles.roadmapCardsGrid}>
                {roadmaps.map((roadmap) => (
                  <Card key={roadmap.id} className={styles.roadmapCard}>
                    <div className={styles.roadmapCardHeader}>
                      <div>
                        <h3 className={styles.roadmapTitle}>{roadmap.title}</h3>
                        <p className={styles.roadmapRole}>Target Role: {roadmap.targetRole}</p>
                      </div>
                      <StatusPill status={roadmap.status} />
                    </div>

                    <div className={styles.progressContainer}>
                      <div className={styles.progressTextRow}>
                        <span>Module Progress</span>
                        <span>
                          {roadmap.completedModules}/{roadmap.totalModules} modules
                        </span>
                      </div>
                      <ProgressBar progress={roadmap.progressPercentage} />
                    </div>

                    <div className={styles.nextModuleBox}>
                      <span className={styles.nextModuleLabel}>Next Module:</span>
                      <span className={styles.nextModuleTitle}>{roadmap.nextModuleTitle}</span>
                    </div>

                    <Button
                      variant="outlined"
                      size="sm"
                      className={styles.continueRoadmapBtn}
                      onClick={() => navigate('/roadmap-workspace')}
                    >
                      Continue Roadmap
                    </Button>
                  </Card>
                ))}
              </div>
            )}
          </div>
        )}

        {/* ===================================================
            SECTION 3: Practice Due & Quick Actions Side Column
            =================================================== */}
        {!isEmptyState && (
          <div className={styles.sideColumn}>
            {/* Practice Due Card */}
            {practiceLoading ? (
              <Skeleton height="140px" borderRadius="var(--radius-xl)" />
            ) : practiceError ? (
              <ErrorCard message={practiceError} onRetry={fetchPracticeDueData} />
            ) : (
              <Card className={styles.practiceDueCard}>
                <div className={styles.practiceHeader}>
                  <div className={styles.practiceTitleInfo}>
                    <span className={styles.practiceIcon}>🎴</span>
                    <div>
                      <h3 className={styles.practiceTitle}>Practice Due</h3>
                      <p className={styles.practiceSub}>
                        {practiceDue > 0
                          ? `${practiceDue} flashcard reviews pending`
                          : 'You are all caught up for today!'}
                      </p>
                    </div>
                  </div>
                  <span className={styles.dueBadge}>{practiceDue}</span>
                </div>
                <Button
                  variant="primary"
                  size="sm"
                  className={styles.practiceBtn}
                  onClick={() => navigate('/practice')}
                >
                  Start Review
                </Button>
              </Card>
            )}

            {/* Quick Actions Card */}
            <Card className={styles.quickActionsCard}>
              <h3 className={styles.quickActionsTitle}>Quick Shortcuts</h3>
              <div className={styles.actionButtonsList}>
                <Button
                  variant="outlined"
                  size="sm"
                  className={styles.shortcutBtn}
                  onClick={() => navigate('/generate-roadmap')}
                >
                  <span>✨</span> Generate New Roadmap
                </Button>
                <Button
                  variant="outlined"
                  size="sm"
                  className={styles.shortcutBtn}
                  onClick={() => navigate('/study-room')}
                >
                  <span>🎧</span> Open Study Room
                </Button>
                <Button
                  variant="outlined"
                  size="sm"
                  className={styles.shortcutBtn}
                  onClick={() => navigate('/profile')}
                >
                  <span>💼</span> Career & Skill Match
                </Button>
              </div>
            </Card>
          </div>
        )}

        {/* ===================================================
            SECTION 4: Heatmap Consistency Grid
            =================================================== */}
        <div className={styles.fullRow}>
          {heatmapLoading ? (
            <Skeleton height="160px" borderRadius="var(--radius-xl)" />
          ) : heatmapError ? (
            <ErrorCard message={heatmapError} onRetry={fetchHeatmapData} />
          ) : (
            <Heatmap data={heatmapData} days={180} />
          )}
        </div>

        {/* ===================================================
            SECTION 5: Quick-Upload Drop Zone
            =================================================== */}
        <div className={styles.fullRow}>
          <Card className={styles.dropzoneCard}>
            <h3 className={styles.dropzoneTitle}>📄 Quick Document & PDF Analyzer</h3>
            <p className={styles.dropzoneSub}>
              Upload lecture notes, slides, or syllabus PDFs to automatically generate study guides.
            </p>

            {roadmaps.length > 0 && (
              <div className={styles.roadmapSelectWrapper}>
                <label htmlFor="roadmapSelect">Link to Roadmap (Optional):</label>
                <select
                  id="roadmapSelect"
                  value={selectedRoadmapId}
                  onChange={(e) => setSelectedRoadmapId(e.target.value)}
                  className={styles.roadmapSelect}
                >
                  <option value="">-- Select Roadmap --</option>
                  {roadmaps.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.title}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div
              className={`${styles.dropzone} ${isDragOver ? styles.dropzoneActive : ''}`}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
            >
              <input
                type="file"
                id="fileInput"
                accept=".pdf,.txt,.doc,.docx"
                onChange={(e) => handleFileUpload(e.target.files)}
                className={styles.fileInput}
              />
              <label htmlFor="fileInput" className={styles.dropzoneLabel}>
                <span className={styles.cloudIcon}>☁️</span>
                <span className={styles.dropText}>
                  {uploading ? 'Uploading and analyzing document...' : 'Drag & drop file here, or click to browse'}
                </span>
                <span className={styles.supportedText}>Supports PDF, DOCX, TXT (up to 10MB)</span>
              </label>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
