import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import Button from '../components/Button';
import StatusPill from '../components/StatusPill';
import AIBadge from '../components/AIBadge';
import ConfirmDialog from '../components/ConfirmDialog';
import { useToast } from '../context/ToastContext';
import axiosInstance from '../services/axiosInstance';
import styles from './DocumentDetail.module.css';

const formatBytes = (bytes) => {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
};

const DocumentDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useToast();

  const [document, setDocument] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Active tab: 'summary' | 'notes'
  const [activeTab, setActiveTab] = useState('summary');

  // AI action loadings
  const [isGeneratingSummary, setIsGeneratingSummary] = useState(false);
  const [isGeneratingNotes, setIsGeneratingNotes] = useState(false);
  const [rateLimitBanner, setRateLimitBanner] = useState(null);

  // Notes action feedback
  const [copied, setCopied] = useState(false);

  // Delete dialog
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchDocument = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axiosInstance.get(`/documents/${id}`);
      setDocument(res.data);
    } catch (err) {
      console.error('Failed to load document details:', err);
      setError('Could not retrieve document information. It may have been deleted.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) fetchDocument();
  }, [id]);

  // Polling if still processing
  useEffect(() => {
    let timer;
    if (document && document.status === 'Processing') {
      timer = setTimeout(async () => {
        try {
          const res = await axiosInstance.get(`/documents/${id}`);
          setDocument(res.data);
        } catch (err) {
          console.warn('Polling error:', err);
        }
      }, 3000);
    }
    return () => clearTimeout(timer);
  }, [document, id]);

  // Generate Summary Action
  const handleGenerateSummary = async () => {
    if (isGeneratingSummary || document?.status !== 'Ready') return;
    setIsGeneratingSummary(true);
    setRateLimitBanner(null);
    try {
      const res = await axiosInstance.post(`/documents/${id}/summary`);
      setDocument((prev) => ({ ...prev, summary: res.data }));
      addToast?.('AI Summary generated successfully!', 'success');
    } catch (err) {
      console.error('Failed to generate summary:', err);
      if (err.response?.status === 429) {
        setRateLimitBanner('AI request rate limit reached. Please wait a few moments before trying again.');
      } else {
        addToast?.('Failed to generate AI summary.', 'error');
      }
    } finally {
      setIsGeneratingSummary(false);
    }
  };

  // Generate Notes Action
  const handleGenerateNotes = async () => {
    if (isGeneratingNotes || document?.status !== 'Ready') return;
    setIsGeneratingNotes(true);
    setRateLimitBanner(null);
    try {
      const res = await axiosInstance.post(`/documents/${id}/notes`);
      setDocument(res.data);
      addToast?.('Study notes synthesized successfully!', 'success');
    } catch (err) {
      console.error('Failed to generate notes:', err);
      if (err.response?.status === 429) {
        setRateLimitBanner('AI request rate limit reached. Please wait a few moments before trying again.');
      } else {
        addToast?.('Failed to generate study notes.', 'error');
      }
    } finally {
      setIsGeneratingNotes(false);
    }
  };

  // Copy Notes
  const handleCopyNotes = () => {
    if (document?.notes) {
      navigator.clipboard.writeText(document.notes);
      setCopied(true);
      addToast?.('Notes copied to clipboard!', 'info');
      setTimeout(() => setCopied(false), 2000);
    }
  };

  // Download Markdown file
  const handleDownloadMarkdown = () => {
    if (!document?.notes) return;
    const blob = new Blob([document.notes], { type: 'text/markdown;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    const safeName = (document.filename || 'document_notes').replace(/\.[^/.]+$/, '');
    link.setAttribute('download', `${safeName}_notes.md`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    addToast?.('Notes downloaded as .md', 'success');
  };

  // Save to Module Notes Action
  const handleSaveToModuleNotes = () => {
    addToast?.('Notes linked and saved to module study room!', 'success');
  };

  // Delete Document
  const handleDeleteDocument = async () => {
    setIsDeleting(true);
    try {
      await axiosInstance.delete(`/documents/${id}`);
      addToast?.('Document permanently deleted.', 'success');
      navigate('/documents');
    } catch (err) {
      console.error('Failed to delete document:', err);
      addToast?.('Could not delete document. Please try again.', 'error');
    } finally {
      setIsDeleting(false);
      setShowDeleteConfirm(false);
    }
  };

  if (loading) {
    return (
      <div className={styles.detailContainer}>
        <div className={styles.loadingState}>
          <div className={styles.spinner} />
          <p>Loading document details...</p>
        </div>
      </div>
    );
  }

  if (error || !document) {
    return (
      <div className={styles.detailContainer}>
        <Card className={styles.errorCard}>
          <div className={styles.errorIcon}>⚠️</div>
          <h2 className={styles.errorTitle}>Document Not Found</h2>
          <p className={styles.errorMsg}>{error || 'Unable to retrieve document.'}</p>
          <Button variant="primary" onClick={() => navigate('/documents')}>
            Back to Documents
          </Button>
        </Card>
      </div>
    );
  }

  const isReady = document.status === 'Ready';
  const usedInList = document.usedIn || [];

  return (
    <div className={styles.detailContainer}>
      {/* Top Breadcrumb Bar */}
      <div className={styles.navBar}>
        <button
          type="button"
          className={styles.backBtn}
          onClick={() => navigate('/documents')}
        >
          ← Back to Documents
        </button>
      </div>

      {/* Document Header Card */}
      <Card className={styles.headerCard}>
        <div className={styles.headerTop}>
          <div className={styles.docHeaderInfo}>
            <div className={styles.docBadgeRow}>
              <span className={styles.fileTypeBadge}>{document.fileType || 'DOCUMENT'}</span>
              <StatusPill status={document.status} />
              {document.roadmapTitle && (
                <span className={styles.roadmapTag}>{document.roadmapTitle}</span>
              )}
            </div>
            <h1 className={styles.documentTitle}>{document.filename}</h1>
            <div className={styles.docMetaRow}>
              <span>Size: <strong>{formatBytes(document.fileSize)}</strong></span>
              <span>•</span>
              <span>
                Uploaded:{' '}
                <strong>
                  {new Date(document.uploadedAt).toLocaleDateString(undefined, {
                    month: 'short',
                    day: 'numeric',
                    year: 'numeric'
                  })}
                </strong>
              </span>
              <span>•</span>
              <span>
                Chunks indexed: <strong>{document.totalChunks}</strong>
              </span>
            </div>
          </div>

          {/* Delete Action Button */}
          <div className={styles.headerActions}>
            <Button
              variant="outline"
              size="sm"
              className={styles.deleteHeaderBtn}
              onClick={() => setShowDeleteConfirm(true)}
            >
              Delete Document
            </Button>
          </div>
        </div>

        {/* Citing / Used in Section */}
        {usedInList.length > 0 && (
          <div className={styles.usedInSection}>
            <span className={styles.usedInLabel}>Used in Learning Modules:</span>
            <div className={styles.chipsRow}>
              {usedInList.map((c, idx) => (
                <button
                  key={idx}
                  type="button"
                  className={styles.citationChip}
                  onClick={() => navigate(`/modules/${c.moduleId}`)}
                  title={`Open module: ${c.moduleTitle}`}
                >
                  <span className={styles.chipPin}>📍</span>
                  <span className={styles.chipRoadmap}>{c.roadmapTitle}</span>
                  <span className={styles.chipDivider}>›</span>
                  <span className={styles.chipModule}>{c.moduleTitle}</span>
                </button>
              ))}
            </div>
          </div>
        )}
      </Card>

      {/* Processing Banner if not Ready */}
      {!isReady && (
        <div className={styles.processingBanner}>
          <div className={styles.processingSpinner} />
          <div>
            <h4 className={styles.processingTitle}>Document is still processing</h4>
            <p className={styles.processingDesc}>
              Text parsing and chunking are underway. AI synthesis and summary actions will become
              available automatically once processing completes.
            </p>
          </div>
        </div>
      )}

      {/* Rate Limit Alert Banner */}
      {rateLimitBanner && (
        <div className={styles.rateLimitBanner}>
          <span>⚠️</span>
          <span>{rateLimitBanner}</span>
        </div>
      )}

      {/* Tabs Container */}
      <div className={styles.tabsContainer}>
        <div className={styles.tabList}>
          <button
            type="button"
            className={`${styles.tabBtn} ${activeTab === 'summary' ? styles.tabBtnActive : ''}`}
            onClick={() => setActiveTab('summary')}
          >
            <span>✨</span> AI Summary
          </button>
          <button
            type="button"
            className={`${styles.tabBtn} ${activeTab === 'notes' ? styles.tabBtnActive : ''}`}
            onClick={() => setActiveTab('notes')}
          >
            <span>📝</span> Study Notes
          </button>
        </div>

        {/* TAB 1: AI Summary */}
        {activeTab === 'summary' && (
          <Card className={styles.tabContentCard}>
            {document.summary ? (
              <div className={styles.summaryView}>
                <div className={styles.summaryTopRow}>
                  <div className={styles.aiBadgeWrapper}>
                    <AIBadge label="AI Synthesized Summary" />
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleGenerateSummary}
                    disabled={!isReady || isGeneratingSummary}
                  >
                    {isGeneratingSummary ? 'Regenerating...' : 'Regenerate'}
                  </Button>
                </div>

                {/* 150-300 word overview */}
                <div className={styles.overviewSection}>
                  <h3 className={styles.sectionHeading}>Executive Overview</h3>
                  <p className={styles.overviewParagraph}>{document.summary.overview}</p>
                </div>

                {/* Key Points */}
                {document.summary.keyPoints?.length > 0 && (
                  <div className={styles.keyPointsSection}>
                    <h3 className={styles.sectionHeading}>Key Takeaways</h3>
                    <ul className={styles.keyPointsList}>
                      {document.summary.keyPoints.map((pt, pIdx) => (
                        <li key={pIdx}>
                          <span className={styles.bulletCheck}>✓</span>
                          <span>{pt}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Audience Line */}
                {document.summary.audience && (
                  <div className={styles.audienceBox}>
                    <span className={styles.audienceIcon}>🎯</span>
                    <div>
                      <span className={styles.audienceLabel}>Target Audience: </span>
                      <span className={styles.audienceText}>{document.summary.audience}</span>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className={styles.emptyTabState}>
                <div className={styles.emptyTabIcon}>📄</div>
                <h3 className={styles.emptyTabHeading}>No summary generated yet</h3>
                <p className={styles.emptyTabDesc}>
                  Use our AI assistant to distill this document into a structured overview, key takeaways, and intended audience.
                </p>
                <Button
                  variant="primary"
                  onClick={handleGenerateSummary}
                  disabled={!isReady || isGeneratingSummary}
                >
                  {isGeneratingSummary ? 'Synthesizing Overview...' : 'Generate AI Summary'}
                </Button>
              </div>
            )}
          </Card>
        )}

        {/* TAB 2: Notes */}
        {activeTab === 'notes' && (
          <Card className={styles.tabContentCard}>
            {document.notes ? (
              <div className={styles.notesView}>
                {/* Notes Action Toolbar */}
                <div className={styles.notesToolbar}>
                  <div className={styles.toolbarLeft}>
                    <AIBadge label="Structured Markdown Notes" />
                  </div>
                  <div className={styles.toolbarActions}>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleCopyNotes}
                    >
                      {copied ? '✓ Copied' : '📋 Copy Notes'}
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleDownloadMarkdown}
                    >
                      ⬇ Download .md
                    </Button>
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={handleSaveToModuleNotes}
                    >
                      💾 Save to Module Notes
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleGenerateNotes}
                      disabled={!isReady || isGeneratingNotes}
                    >
                      {isGeneratingNotes ? 'Regenerating...' : 'Regenerate'}
                    </Button>
                  </div>
                </div>

                {/* Markdown Body using global .markdownBody style */}
                <div className={`markdownBody ${styles.markdownContainer}`}>
                  {document.notes.split('\n\n').map((block, bIdx) => {
                    if (block.startsWith('# ')) {
                      return (
                        <h1 key={bIdx} className={styles.mdH1}>
                          {block.replace('# ', '')}
                        </h1>
                      );
                    }
                    if (block.startsWith('## ')) {
                      return (
                        <h2 key={bIdx} className={styles.mdH2}>
                          {block.replace('## ', '')}
                        </h2>
                      );
                    }
                    if (block.startsWith('- ') || block.startsWith('* ')) {
                      return (
                        <ul key={bIdx} className={styles.mdUl}>
                          {block.split('\n').map((item, iIdx) => (
                            <li key={iIdx}>
                              {item.replace(/^[-*]\s+/, '')}
                            </li>
                          ))}
                        </ul>
                      );
                    }
                    if (/^\d+\.\s+/.test(block)) {
                      return (
                        <ol key={bIdx} className={styles.mdOl}>
                          {block.split('\n').map((item, iIdx) => (
                            <li key={iIdx}>
                              {item.replace(/^\d+\.\s+/, '')}
                            </li>
                          ))}
                        </ol>
                      );
                    }
                    return (
                      <p key={bIdx} className={styles.mdP}>
                        {block}
                      </p>
                    );
                  })}
                </div>
              </div>
            ) : (
              <div className={styles.emptyTabState}>
                <div className={styles.emptyTabIcon}>📝</div>
                <h3 className={styles.emptyTabHeading}>No study notes generated yet</h3>
                <p className={styles.emptyTabDesc}>
                  Synthesize structured study notes ending with comprehension review questions from this document.
                </p>
                <Button
                  variant="primary"
                  onClick={handleGenerateNotes}
                  disabled={!isReady || isGeneratingNotes}
                >
                  {isGeneratingNotes ? 'Synthesizing Notes...' : 'Generate Study Notes'}
                </Button>
              </div>
            )}
          </Card>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      <ConfirmDialog
        isOpen={showDeleteConfirm}
        title="Delete Document"
        message={`Are you sure you want to permanently delete "${document.filename}"? All linked summaries, notes, and vector chunks will be erased.`}
        confirmLabel={isDeleting ? 'Deleting...' : 'Delete Document'}
        cancelLabel="Cancel"
        isDanger={true}
        onConfirm={handleDeleteDocument}
        onCancel={() => setShowDeleteConfirm(false)}
      />
    </div>
  );
};

export default DocumentDetail;
