import React, { useState, useEffect } from 'react';
import { useParams, useSearchParams, useNavigate, Link } from 'react-router-dom';
import axiosInstance from '../../services/axiosInstance';
import { useToast } from '../../context/ToastContext';
import Card from '../../components/Card';
import Button from '../../components/Button';
import StatusPill from '../../components/StatusPill';
import ProgressBar from '../../components/ProgressBar';
import Skeleton from '../../components/Skeleton';
import ErrorCard from '../../components/ErrorCard';
import ConfirmDialog from '../../components/ConfirmDialog';
import ModuleRow from './components/ModuleRow';
import GraphView from './components/GraphView';
import RecreateRoadmapDialog from './components/RecreateRoadmapDialog';
import styles from './RoadmapView.module.css';

const RoadmapView = () => {
  const params = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { addToast } = useToast();

  const roadmapId = params.id || searchParams.get('id') || '1';

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [roadmap, setRoadmap] = useState(null);

  // Tabs state: 'list' | 'graph'
  const [activeTab, setActiveTab] = useState('list');

  // Graph data & lazy loading
  const [graphData, setGraphData] = useState(null);
  const [loadingGraph, setLoadingGraph] = useState(false);

  // Inline editing title
  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [titleInput, setTitleInput] = useState('');

  // Dropdown menu state
  const [menuOpen, setMenuOpen] = useState(false);

  // Dialog & Confirm states
  const [isRecreateOpen, setIsRecreateOpen] = useState(false);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  // Fetch full roadmap detail on mount or roadmapId change
  const fetchRoadmapDetail = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axiosInstance.get(`/roadmaps/${roadmapId}`);
      setRoadmap(res.data);
      setTitleInput(res.data?.title || '');
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to load roadmap details.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRoadmapDetail();
  }, [roadmapId]);

  // Lazy fetch graph data when Graph tab is first selected
  useEffect(() => {
    if (activeTab === 'graph' && !graphData && roadmapId) {
      const fetchGraph = async () => {
        setLoadingGraph(true);
        try {
          const res = await axiosInstance.get(`/roadmaps/${roadmapId}/graph`);
          setGraphData(res.data);
        } catch {
          addToast('Could not load interactive graph view.', 'error');
        } finally {
          setLoadingGraph(false);
        }
      };
      fetchGraph();
    }
  }, [activeTab, graphData, roadmapId]);

  // Inline Title Save
  const handleSaveTitle = async () => {
    setIsEditingTitle(false);
    if (!titleInput.trim() || titleInput === roadmap?.title) return;

    try {
      await axiosInstance.patch(`/roadmaps/${roadmapId}`, { title: titleInput.trim() });
      setRoadmap((prev) => ({ ...prev, title: titleInput.trim() }));
      addToast('Roadmap title updated!', 'success');
    } catch {
      setTitleInput(roadmap?.title || '');
      addToast('Failed to update title', 'error');
    }
  };

  // Toggle Archive / Re-activate
  const handleToggleActive = async () => {
    setMenuOpen(false);
    const newIsActive = !roadmap.isActive;
    try {
      await axiosInstance.patch(`/roadmaps/${roadmapId}`, { isActive: newIsActive });
      setRoadmap((prev) => ({
        ...prev,
        isActive: newIsActive,
        status: newIsActive ? 'ACTIVE' : 'ARCHIVED',
      }));
      addToast(`Roadmap ${newIsActive ? 're-activated' : 'archived'}!`, 'success');
    } catch {
      addToast('Failed to update roadmap status', 'error');
    }
  };

  // Delete Roadmap Handler
  const handleDeleteRoadmap = async () => {
    setIsDeleting(true);
    try {
      await axiosInstance.delete(`/roadmaps/${roadmapId}`);
      addToast('Roadmap deleted permanently.', 'success');
      setIsDeleteOpen(false);
      navigate('/roadmaps');
    } catch {
      addToast('Failed to delete roadmap', 'error');
    } finally {
      setIsDeleting(false);
    }
  };

  // Finalize Draft (Skip Diagnostic)
  const handleFinalizeDraft = async () => {
    try {
      const res = await axiosInstance.post(`/roadmaps/${roadmapId}/finalize?skip=true`);
      setRoadmap(res.data);
      addToast('Draft finalized! All baseline modules unlocked.', 'success');
    } catch {
      addToast('Failed to finalize draft', 'error');
    }
  };

  if (loading) {
    return (
      <div className={styles.container}>
        <Skeleton height="140px" borderRadius="1.5rem" className={styles.skeletonHeader} />
        <Skeleton height="300px" borderRadius="1.5rem" style={{ marginTop: '1.5rem' }} />
      </div>
    );
  }

  if (error || !roadmap) {
    return (
      <div className={styles.container}>
        <ErrorCard
          title="Roadmap Not Found"
          message={error || 'The requested roadmap could not be found or you do not have permission to access it.'}
          onRetry={fetchRoadmapDetail}
        />
      </div>
    );
  }

  const isArchived = !roadmap.isActive || roadmap.status === 'ARCHIVED';
  const isDraft = roadmap.isDraft || roadmap.status === 'DRAFT';
  const isCompleted = roadmap.completedModulesCount === roadmap.totalModulesCount && roadmap.totalModulesCount > 0;

  return (
    <div className={`${styles.container} ${isArchived ? styles.archivedView : ''}`}>
      {/* "What Changed" Recreation Summary Banner */}
      {roadmap.whatChanged && (
        <div className={styles.whatChangedBanner}>
          <div className={styles.whatChangedContent}>
            <h4>✨ Updated AI Learning Path</h4>
            <p>
              This roadmap was recreated based on your updated preferences.{' '}
              {roadmap.whatChanged.parentRoadmapId && (
                <Link to={`/roadmaps/${roadmap.whatChanged.parentRoadmapId}`} className={styles.parentLink}>
                  View Original Roadmap →
                </Link>
              )}
            </p>
            <div className={styles.changedChips}>
              {roadmap.whatChanged.keptModules?.map((m, idx) => (
                <span key={idx} className={styles.keptChip}>
                  ✓ Kept: {m}
                </span>
              ))}
              {roadmap.whatChanged.removedModules?.map((m, idx) => (
                <span key={idx} className={styles.removedChip}>
                  ✕ Refactored: {m}
                </span>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* DRAFT Banner */}
      {isDraft && !isArchived && (
        <div className={styles.draftBanner}>
          <div className={styles.draftTextGroup}>
            <span className={styles.draftBadge}>DRAFT ROADMAP</span>
            <h3>Your roadmap configuration is currently a draft</h3>
            <p>
              Take our quick 5-minute diagnostic test to personalize module difficulty, or skip to unlock the baseline modules.
            </p>
          </div>
          <div className={styles.draftActions}>
            <Button
              variant="primary"
              size="sm"
              onClick={() => navigate(`/diagnostic-test?roadmapId=${roadmap.id}`)}
            >
              Take diagnostic test
            </Button>
            <Button variant="outlined" size="sm" onClick={handleFinalizeDraft}>
              Skip and start
            </Button>
          </div>
        </div>
      )}

      {/* ARCHIVED Banner */}
      {isArchived && (
        <div className={styles.archivedBanner}>
          <span>📦 This roadmap is currently archived and in read-only mode.</span>
          <Button variant="outlined" size="sm" onClick={handleToggleActive}>
            Re-activate Roadmap
          </Button>
        </div>
      )}

      {/* Roadmap Header Card */}
      <Card className={styles.roadmapHeader}>
        <div className={styles.headerTopRow}>
          <div className={styles.titleArea}>
            {isEditingTitle ? (
              <input
                type="text"
                value={titleInput}
                onChange={(e) => setTitleInput(e.target.value)}
                onBlur={handleSaveTitle}
                onKeyDown={(e) => e.key === 'Enter' && handleSaveTitle()}
                className={styles.titleInlineInput}
                autoFocus
              />
            ) : (
              <h1
                className={styles.title}
                onClick={() => !isArchived && setIsEditingTitle(true)}
                title="Click to rename"
              >
                {roadmap.title}
                {!isArchived && <span className={styles.editPencil}> ✏️</span>}
              </h1>
            )}

            <div className={styles.subtitleRow}>
              <span className={styles.targetRole}>🎯 {roadmap.targetRole || 'General Objective'}</span>
              <StatusPill status={roadmap.status || 'ACTIVE'} />
              {roadmap.learningStyle && (
                <span className={styles.styleTag}>⚡ {roadmap.learningStyle}</span>
              )}
            </div>
          </div>

          {/* Overflow Menu Dropdown */}
          <div className={styles.menuWrapper}>
            <button
              className={styles.menuTriggerBtn}
              onClick={() => setMenuOpen(!menuOpen)}
              title="Roadmap options"
            >
              •••
            </button>

            {menuOpen && (
              <div className={styles.dropdownMenu} onMouseLeave={() => setMenuOpen(false)}>
                <button
                  className={styles.dropdownItem}
                  onClick={() => {
                    setMenuOpen(false);
                    setIsEditingTitle(true);
                  }}
                >
                  ✏️ Rename Title
                </button>
                <button className={styles.dropdownItem} onClick={handleToggleActive}>
                  {roadmap.isActive ? '📦 Archive Roadmap' : '⚡ Re-activate Roadmap'}
                </button>
                <button
                  className={styles.dropdownItem}
                  onClick={() => {
                    setMenuOpen(false);
                    setIsRecreateOpen(true);
                  }}
                >
                  🔄 Recreate Roadmap
                </button>
                <div className={styles.dropdownDivider} />
                <button
                  className={`${styles.dropdownItem} ${styles.dangerItem}`}
                  onClick={() => {
                    setMenuOpen(false);
                    setIsDeleteOpen(true);
                  }}
                >
                  🗑️ Delete Roadmap
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Progress & Time Stats Bar */}
        <div className={styles.headerStatsRow}>
          <div className={styles.progressCol}>
            <div className={styles.progressTextRow}>
              <span>
                <strong>{roadmap.completedModulesCount || 0}</strong> of{' '}
                <strong>{roadmap.totalModulesCount || 0}</strong> modules completed
              </span>
              <span className={styles.progressPercent}>{roadmap.progressPercent || 0}%</span>
            </div>
            <ProgressBar value={roadmap.progressPercent || 0} />
          </div>

          <div className={styles.timeStatsCol}>
            <div className={styles.timeItem}>
              <span className={styles.timeLabel}>Remaining Time</span>
              <span className={styles.timeValue}>≈ {roadmap.remainingHours || 0} hrs left</span>
            </div>
            <div className={styles.timeItem}>
              <span className={styles.timeLabel}>Pacing Schedule</span>
              <span className={styles.timeValue}>
                {roadmap.hoursPerWeek || 10} hrs/wk · {roadmap.targetWeeks || 8} wks
              </span>
            </div>
          </div>
        </div>
      </Card>

      {/* ROADMAP COMPLETE BANNER */}
      {isCompleted && (
        <div className={styles.completeBanner}>
          <div className={styles.completeContent}>
            <h3>🎉 Congratulations! You completed this roadmap!</h3>
            <p>Ready to level up your career or explore additional learning resources?</p>
          </div>
          <div className={styles.completeActions}>
            <Link to="/career">
              <Button variant="primary" size="sm">Explore Career Guidance →</Button>
            </Link>
            <Link to="/resources">
              <Button variant="outlined" size="sm">Browse Recommended Resources</Button>
            </Link>
          </div>
        </div>
      )}

      {/* View Tabs: List / Graph */}
      <div className={styles.tabsRow}>
        <div className={styles.buttonGroup}>
          <button
            className={`${styles.tabBtn} ${activeTab === 'list' ? styles.tabActive : ''}`}
            onClick={() => setActiveTab('list')}
          >
            📋 List View
          </button>
          <button
            className={`${styles.tabBtn} ${activeTab === 'graph' ? styles.tabActive : ''}`}
            onClick={() => setActiveTab('graph')}
          >
            🕸️ Visual Graph
          </button>
        </div>
      </div>

      {/* Main Tab Content */}
      <div className={styles.tabContentArea}>
        {activeTab === 'list' ? (
          <div className={styles.modulesListCol}>
            {roadmap.modules && roadmap.modules.length > 0 ? (
              roadmap.modules.map((module) => (
                <ModuleRow
                  key={module.id}
                  module={module}
                  disabled={isArchived}
                />
              ))
            ) : (
              <Card className={styles.emptyCard}>
                <p>No learning modules generated for this roadmap yet.</p>
              </Card>
            )}
          </div>
        ) : (
          <div>
            {loadingGraph ? (
              <Skeleton height="400px" borderRadius="1.5rem" />
            ) : (
              <GraphView
                graphData={graphData}
                modules={roadmap.modules}
                onRefresh={fetchRoadmapDetail}
              />
            )}
          </div>
        )}
      </div>

      {/* Recreate Roadmap Modal */}
      <RecreateRoadmapDialog
        isOpen={isRecreateOpen}
        roadmap={roadmap}
        onClose={() => setIsRecreateOpen(false)}
      />

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        isOpen={isDeleteOpen}
        title="Delete Roadmap"
        message={`This permanently deletes "${roadmap.title}" and its ${roadmap.totalModulesCount || 0} modules. This action cannot be undone.`}
        confirmText="Delete Roadmap"
        confirmVariant="danger"
        isSubmitting={isDeleting}
        onConfirm={handleDeleteRoadmap}
        onCancel={() => setIsDeleteOpen(false)}
      />
    </div>
  );
};

export default RoadmapView;
