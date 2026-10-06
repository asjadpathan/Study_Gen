import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axiosInstance from '../services/axiosInstance';
import { useToast } from '../context/ToastContext';
import Button from '../components/Button';
import Card from '../components/Card';
import ProgressBar from '../components/ProgressBar';
import StatusPill from '../components/StatusPill';
import Skeleton from '../components/Skeleton';
import ErrorCard from '../components/ErrorCard';
import EmptyState from '../components/EmptyState';
import ConfirmDialog from '../components/ConfirmDialog';
import styles from './RoadmapsList.module.css';

const RoadmapsList = () => {
  const navigate = useNavigate();
  const { addToast } = useToast();

  const [roadmaps, setRoadmaps] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('All'); // 'All' | 'Active' | 'Draft' | 'Archived'

  // Card Overflow Menu Dropdown State (roadmapId or null)
  const [openMenuId, setOpenMenuId] = useState(null);

  // Inline Edit States
  const [editingId, setEditingId] = useState(null);
  const [editField, setEditField] = useState(''); // 'title' | 'targetRole'
  const [editValue, setEditValue] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);

  // Confirm Delete Dialog State
  const [deleteTarget, setDeleteTarget] = useState(null); // { id, title, totalModules }
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    fetchRoadmaps();
  }, []);

  // Close overflow menu on outside click
  useEffect(() => {
    const handleOutsideClick = () => setOpenMenuId(null);
    document.addEventListener('click', handleOutsideClick);
    return () => document.removeEventListener('click', handleOutsideClick);
  }, []);

  const fetchRoadmaps = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axiosInstance.get('/roadmaps');
      setRoadmaps(Array.isArray(response.data) ? response.data : []);
    } catch {
      setError('Failed to load your study roadmaps.');
    } finally {
      setLoading(false);
    }
  };

  // Filter roadmaps based on selected tab
  const filteredRoadmaps = roadmaps.filter((r) => {
    const status = (r.status || 'ACTIVE').toUpperCase();
    if (activeTab === 'All') return true;
    if (activeTab === 'Active') return status === 'ACTIVE';
    if (activeTab === 'Draft') return status === 'DRAFT';
    if (activeTab === 'Archived') return status === 'ARCHIVED';
    return true;
  });

  // Start Inline Editing
  const handleStartEdit = (roadmap, field) => {
    setOpenMenuId(null);
    setEditingId(roadmap.id);
    setEditField(field);
    setEditValue(field === 'title' ? roadmap.title : roadmap.targetRole || '');
  };

  // Submit Inline Edit (PATCH /api/roadmaps/{id})
  const handleSaveEdit = async (roadmapId) => {
    if (!editValue.trim() || savingEdit) return;
    setSavingEdit(true);

    const payload = editField === 'title' ? { title: editValue.trim() } : { targetRole: editValue.trim() };

    try {
      const res = await axiosInstance.patch(`/roadmaps/${roadmapId}`, payload);
      setRoadmaps((prev) =>
        prev.map((item) => (item.id === roadmapId ? { ...item, ...res.data } : item))
      );
      addToast(`Updated roadmap ${editField === 'title' ? 'title' : 'target role'} successfully`, 'success');
      setEditingId(null);
    } catch {
      addToast('Failed to update roadmap. Please try again.', 'error');
    } finally {
      setSavingEdit(false);
    }
  };

  // Toggle Archive / Reactivate
  const handleToggleArchive = async (roadmap) => {
    setOpenMenuId(null);
    const newActiveState = roadmap.status === 'ARCHIVED';

    try {
      const res = await axiosInstance.patch(`/roadmaps/${roadmap.id}`, { isActive: newActiveState });
      setRoadmaps((prev) =>
        prev.map((item) => (item.id === roadmap.id ? { ...item, ...res.data } : item))
      );
      addToast(
        newActiveState
          ? `Re-activated "${roadmap.title}"`
          : `Archived "${roadmap.title}"`,
        'info'
      );
    } catch {
      addToast('Failed to update roadmap status.', 'error');
    }
  };

  // Delete Roadmap Handler
  const handleConfirmDelete = async () => {
    if (!deleteTarget || deleting) return;
    setDeleting(true);

    try {
      await axiosInstance.delete(`/roadmaps/${deleteTarget.id}`);
      setRoadmaps((prev) => prev.filter((item) => item.id !== deleteTarget.id));
      addToast(`Permanently deleted roadmap "${deleteTarget.title}"`, 'success');
      setDeleteTarget(null);
    } catch {
      addToast('Failed to delete roadmap.', 'error');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className={styles.container}>
      {/* Header & Title */}
      <div className={styles.pageHeader}>
        <div>
          <h1 className={styles.title}>My Roadmaps Workspace</h1>
          <p className={styles.subtitle}>
            Manage your AI-generated learning roadmaps, track module completion, and customize target roles.
          </p>
        </div>
        <Button
          variant="primary"
          size="md"
          onClick={() => navigate('/generate-roadmap')}
        >
          ✨ Generate New Roadmap
        </Button>
      </div>

      {/* Button-Group Filter Tabs */}
      <div className={styles.tabsContainer}>
        {['All', 'Active', 'Draft', 'Archived'].map((tab) => {
          const count = roadmaps.filter((r) => {
            const st = (r.status || 'ACTIVE').toUpperCase();
            if (tab === 'All') return true;
            if (tab === 'Active') return st === 'ACTIVE';
            if (tab === 'Draft') return st === 'DRAFT';
            if (tab === 'Archived') return st === 'ARCHIVED';
            return true;
          }).length;

          return (
            <button
              key={tab}
              type="button"
              className={`${styles.tab} ${activeTab === tab ? styles.tabActive : ''}`}
              onClick={() => setActiveTab(tab)}
            >
              {tab} <span className={styles.tabBadge}>{count}</span>
            </button>
          );
        })}
      </div>

      {/* Main Content Area */}
      {loading ? (
        <div className={styles.grid}>
          <Skeleton height="240px" borderRadius="var(--radius-xl)" />
          <Skeleton height="240px" borderRadius="var(--radius-xl)" />
          <Skeleton height="240px" borderRadius="var(--radius-xl)" />
        </div>
      ) : error ? (
        <ErrorCard message={error} onRetry={fetchRoadmaps} />
      ) : filteredRoadmaps.length === 0 ? (
        <EmptyState
          icon="🗺️"
          title={`No ${activeTab !== 'All' ? activeTab.toLowerCase() : ''} roadmaps found`}
          description="Create your first AI-driven study roadmap to get started on your personalized learning path."
          actionLabel="Generate Roadmap Now"
          onAction={() => navigate('/generate-roadmap')}
        />
      ) : (
        <div className={styles.grid}>
          {filteredRoadmaps.map((roadmap) => {
            const isArchived = (roadmap.status || '').toUpperCase() === 'ARCHIVED';
            const isDraft = (roadmap.status || '').toUpperCase() === 'DRAFT';

            return (
              <Card
                key={roadmap.id}
                className={`${styles.roadmapCard} ${isArchived ? styles.archivedCard : ''}`}
              >
                {/* Top Card Bar */}
                <div className={styles.cardTopBar}>
                  <StatusPill status={roadmap.status} />

                  {/* Overflow Menu Button */}
                  <div className={styles.menuWrapper} onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className={styles.menuTrigger}
                      onClick={() =>
                        setOpenMenuId(openMenuId === roadmap.id ? null : roadmap.id)
                      }
                      aria-label="Roadmap options menu"
                    >
                      ⋮
                    </button>

                    {/* Absolutely Positioned Dropdown */}
                    {openMenuId === roadmap.id && (
                      <div className={styles.dropdownMenu}>
                        <button
                          type="button"
                          className={styles.dropdownItem}
                          onClick={() => handleStartEdit(roadmap, 'title')}
                        >
                          ✏️ Rename Title
                        </button>
                        <button
                          type="button"
                          className={styles.dropdownItem}
                          onClick={() => handleStartEdit(roadmap, 'targetRole')}
                        >
                          🎯 Change Target Role
                        </button>
                        <button
                          type="button"
                          className={styles.dropdownItem}
                          onClick={() => handleToggleArchive(roadmap)}
                        >
                          {isArchived ? '🔓 Re-activate' : '📦 Archive'}
                        </button>
                        <div className={styles.menuDivider} />
                        <button
                          type="button"
                          className={`${styles.dropdownItem} ${styles.deleteItem}`}
                          onClick={() => {
                            setOpenMenuId(null);
                            setDeleteTarget({
                              id: roadmap.id,
                              title: roadmap.title,
                              totalModules: roadmap.totalModules || 0,
                            });
                          }}
                        >
                          🗑️ Delete Roadmap
                        </button>
                      </div>
                    )}
                  </div>
                </div>

                {/* Inline Editing for Title */}
                {editingId === roadmap.id && editField === 'title' ? (
                  <div className={styles.inlineEditGroup}>
                    <input
                      type="text"
                      className={styles.inlineInput}
                      value={editValue}
                      onChange={(e) => setEditValue(e.target.value)}
                      autoFocus
                    />
                    <Button
                      variant="primary"
                      size="sm"
                      disabled={savingEdit}
                      onClick={() => handleSaveEdit(roadmap.id)}
                    >
                      Save
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setEditingId(null)}
                    >
                      Cancel
                    </Button>
                  </div>
                ) : (
                  <h3 className={styles.roadmapTitle}>{roadmap.title}</h3>
                )}

                {/* Inline Editing for Target Role */}
                {editingId === roadmap.id && editField === 'targetRole' ? (
                  <div className={styles.inlineEditGroup}>
                    <input
                      type="text"
                      className={styles.inlineInput}
                      value={editValue}
                      onChange={(e) => setEditValue(e.target.value)}
                      placeholder="e.g. Software Engineer"
                      autoFocus
                    />
                    <Button
                      variant="primary"
                      size="sm"
                      disabled={savingEdit}
                      onClick={() => handleSaveEdit(roadmap.id)}
                    >
                      Save
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setEditingId(null)}
                    >
                      Cancel
                    </Button>
                  </div>
                ) : (
                  <p className={styles.targetRoleText}>
                    Target Role: <strong>{roadmap.targetRole || 'General Learner'}</strong>
                  </p>
                )}

                {/* Module Progress Section */}
                <div className={styles.progressSection}>
                  <div className={styles.progressInfoRow}>
                    <span>Completion</span>
                    <span>
                      {roadmap.completedModules || 0}/{roadmap.totalModules || 0} modules
                    </span>
                  </div>
                  <ProgressBar progress={roadmap.progressPercent || 0} />
                </div>

                {/* Extra Buttons for Draft Cards */}
                {isDraft ? (
                  <div className={styles.draftActionsRow}>
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => navigate(`/diagnostic-test?roadmapId=${roadmap.id}`)}
                    >
                      🎯 Take Diagnostic
                    </Button>
                    <Button
                      variant="outlined"
                      size="sm"
                      onClick={() => navigate(`/roadmap-view?id=${roadmap.id}`)}
                    >
                      Skip & Start
                    </Button>
                  </div>
                ) : (
                  <Button
                    variant="outlined"
                    size="md"
                    className={styles.openRoadmapBtn}
                    onClick={() => navigate(`/roadmap-view?id=${roadmap.id}`)}
                  >
                    Open Roadmap Workspace <span>→</span>
                  </Button>
                )}
              </Card>
            );
          })}
        </div>
      )}

      {/* Confirm Delete Dialog */}
      <ConfirmDialog
        isOpen={!!deleteTarget}
        title={`Delete "${deleteTarget?.title}"?`}
        message={`This permanently deletes the roadmap and its ${
          deleteTarget?.totalModules || 0
        } modules. This action cannot be undone.`}
        confirmLabel={deleting ? 'Deleting...' : 'Delete Permanently'}
        isDanger={true}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
};

export default RoadmapsList;
