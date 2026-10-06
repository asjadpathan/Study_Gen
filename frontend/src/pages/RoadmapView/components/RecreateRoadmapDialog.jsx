import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axiosInstance from '../../../services/axiosInstance';
import { useToast } from '../../../context/ToastContext';
import Button from '../../../components/Button';
import styles from './RecreateRoadmapDialog.module.css';

const RecreateRoadmapDialog = ({ isOpen, roadmap, onClose }) => {
  const navigate = useNavigate();
  const { addToast } = useToast();

  const [reason, setReason] = useState('');
  const [targetTopic, setTargetTopic] = useState(roadmap?.title || '');
  const [targetRole, setTargetRole] = useState(roadmap?.targetRole || '');
  const [archiveOld, setArchiveOld] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!reason.trim()) {
      addToast('Please provide a reason for recreating the roadmap', 'error');
      return;
    }

    setSubmitting(true);
    try {
      const response = await axiosInstance.post(`/roadmaps/${roadmap.id}/recreate`, {
        reason,
        targetTopic: targetTopic.trim() || roadmap.title,
        targetRole: targetRole.trim() || roadmap.targetRole,
        archiveOld,
      });

      const newId = response.data?.newRoadmapId || response.data?.id;
      addToast('Roadmap recreated successfully with updated topics!', 'success');
      onClose();
      if (newId) {
        navigate(`/roadmaps/${newId}`);
      }
    } catch {
      addToast('Failed to recreate roadmap. Please try again.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.header}>
          <h3 className={styles.title}>🔄 Recreate Roadmap</h3>
          <button className={styles.closeBtn} onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          <p className={styles.subtitle}>
            Adjust your learning objectives, specify what needs changing, and generate an updated AI learning path.
          </p>

          <div className={styles.field}>
            <label className={styles.label}>
              Reason for Recreating <span className={styles.required}>*</span>
            </label>
            <textarea
              rows={3}
              maxLength={1000}
              placeholder="e.g. Topics are too basic, want more focus on system design and real-world microservices..."
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className={styles.textarea}
              required
            />
            <span className={styles.charCount}>{reason.length}/1000</span>
          </div>

          <div className={styles.field}>
            <label className={styles.label}>Updated Target Topic</label>
            <input
              type="text"
              value={targetTopic}
              onChange={(e) => setTargetTopic(e.target.value)}
              className={styles.input}
            />
          </div>

          <div className={styles.field}>
            <label className={styles.label}>Updated Target Role</label>
            <input
              type="text"
              value={targetRole}
              onChange={(e) => setTargetRole(e.target.value)}
              className={styles.input}
            />
          </div>

          <div className={styles.checkboxField}>
            <label className={styles.checkboxLabel}>
              <input
                type="checkbox"
                checked={archiveOld}
                onChange={(e) => setArchiveOld(e.target.checked)}
                className={styles.checkbox}
              />
              <span>Archive current roadmap (recommended)</span>
            </label>
          </div>

          <div className={styles.actions}>
            <Button type="button" variant="outlined" onClick={onClose} disabled={submitting}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" disabled={submitting}>
              {submitting ? 'Recreating...' : '✨ Recreate Roadmap'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RecreateRoadmapDialog;
