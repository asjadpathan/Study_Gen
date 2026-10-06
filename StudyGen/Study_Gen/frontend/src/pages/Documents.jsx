import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import Button from '../components/Button';
import StatusPill from '../components/StatusPill';
import ProgressBar from '../components/ProgressBar';
import ConfirmDialog from '../components/ConfirmDialog';
import { useToast } from '../context/ToastContext';
import axiosInstance from '../services/axiosInstance';
import styles from './Documents.module.css';

const ACCEPTED_EXTENSIONS = ['pdf', 'docx', 'txt', 'md'];
const MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024; // 25 MB

const formatBytes = (bytes) => {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
};

const getFileTypeIcon = (filename = '', fileType = '') => {
  const lower = (filename + ' ' + fileType).toLowerCase();
  if (lower.includes('pdf')) return '📕';
  if (lower.includes('docx') || lower.includes('word')) return '📘';
  if (lower.includes('md') || lower.includes('markdown')) return '📝';
  if (lower.includes('html') || lower.includes('url') || lower.includes('http')) return '🌐';
  return '📄';
};

const Documents = () => {
  const navigate = useNavigate();
  const { addToast } = useToast();

  const [documents, setDocuments] = useState([]);
  const [roadmaps, setRoadmaps] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedRoadmapId, setSelectedRoadmapId] = useState('');

  // Drag & drop state
  const [isDragging, setIsDragging] = useState(false);
  const [uploadProgressMap, setUploadProgressMap] = useState({}); // { [fileName]: number }
  const [uploadError, setUploadError] = useState(null);

  // URL input state
  const [urlInput, setUrlInput] = useState('');
  const [isSubmittingUrl, setIsSubmittingUrl] = useState(false);
  const [urlError, setUrlError] = useState(null);

  // Delete dialog state
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fileInputRef = useRef(null);

  // Polling registry for exponential backoff: { [docId]: { interval: number, timeoutId: any } }
  const pollRegistryRef = useRef({});

  const fetchDocumentsAndRoadmaps = async () => {
    setLoading(true);
    try {
      const [docsRes, roadmapsRes] = await Promise.allSettled([
        axiosInstance.get('/documents'),
        axiosInstance.get('/roadmaps')
      ]);

      if (docsRes.status === 'fulfilled' && docsRes.value?.data) {
        setDocuments(docsRes.value.data);
      }
      if (roadmapsRes.status === 'fulfilled' && roadmapsRes.value?.data) {
        setRoadmaps(roadmapsRes.value.data);
      }
    } catch (err) {
      console.error('Failed to load documents data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDocumentsAndRoadmaps();

    return () => {
      // Clear any pending timeouts on unmount
      Object.values(pollRegistryRef.current).forEach((entry) => {
        if (entry.timeoutId) clearTimeout(entry.timeoutId);
      });
    };
  }, []);

  // Capped Exponential Backoff Poller for Processing Documents
  const startPollingDocument = useCallback((docId, currentInterval = 2000) => {
    const maxInterval = 16000;

    // Clear previous timeout if exists
    if (pollRegistryRef.current[docId]?.timeoutId) {
      clearTimeout(pollRegistryRef.current[docId].timeoutId);
    }

    const timeoutId = setTimeout(async () => {
      try {
        const res = await axiosInstance.get(`/documents/${docId}`);
        const updatedDoc = res.data;

        if (updatedDoc) {
          setDocuments((prev) =>
            prev.map((d) => (d.id === docId ? { ...d, ...updatedDoc } : d))
          );

          if (updatedDoc.status === 'Processing') {
            // Double interval up to max
            const nextInterval = Math.min(currentInterval * 2, maxInterval);
            startPollingDocument(docId, nextInterval);
          } else {
            // Finished processing (Ready or Failed)
            delete pollRegistryRef.current[docId];
          }
        }
      } catch (err) {
        console.warn(`Polling error for doc ${docId}:`, err);
        const nextInterval = Math.min(currentInterval * 2, maxInterval);
        startPollingDocument(docId, nextInterval);
      }
    }, currentInterval);

    pollRegistryRef.current[docId] = { interval: currentInterval, timeoutId };
  }, []);

  // Trigger polling for any document currently with status "Processing"
  useEffect(() => {
    documents.forEach((doc) => {
      if (doc.status === 'Processing' && !pollRegistryRef.current[doc.id]) {
        startPollingDocument(doc.id, 2000);
      }
    });
  }, [documents, startPollingDocument]);

  // Upload file handler
  const handleUploadFiles = async (files) => {
    if (!files || files.length === 0) return;
    setUploadError(null);

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const ext = file.name.split('.').pop().toLowerCase();

      // Validate file type
      if (!ACCEPTED_EXTENSIONS.includes(ext)) {
        setUploadError(`"${file.name}" has an unsupported format. Please upload PDF, DOCX, TXT, or MD.`);
        continue;
      }

      // Validate file size
      if (file.size > MAX_FILE_SIZE_BYTES) {
        setUploadError(`"${file.name}" exceeds the 25MB limit (${formatBytes(file.size)}).`);
        continue;
      }

      const formData = new FormData();
      formData.append('file', file);
      if (selectedRoadmapId) {
        formData.append('roadmapId', selectedRoadmapId);
      }

      setUploadProgressMap((prev) => ({ ...prev, [file.name]: 5 }));

      try {
        const res = await axiosInstance.post('/documents', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
          onUploadProgress: (progressEvent) => {
            const percent = progressEvent.total
              ? Math.round((progressEvent.loaded * 100) / progressEvent.total)
              : 50;
            setUploadProgressMap((prev) => ({ ...prev, [file.name]: percent }));
          }
        });

        const newDoc = res.data;
        setDocuments((prev) => [newDoc, ...prev]);
        addToast?.(`"${file.name}" uploaded successfully!`, 'success');

        // If newly created doc is processing, initiate polling
        if (newDoc.status === 'Processing') {
          startPollingDocument(newDoc.id, 2000);
        }
      } catch (err) {
        console.error('File upload failed:', err);
        const errMsg = err.response?.data?.message || err.message || 'Upload failed.';
        setUploadError(`Failed to upload "${file.name}": ${errMsg}`);
      } finally {
        setTimeout(() => {
          setUploadProgressMap((prev) => {
            const copy = { ...prev };
            delete copy[file.name];
            return copy;
          });
        }, 1200);
      }
    }
  };

  // Drag event listeners
  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    if (e.dataTransfer?.files?.length > 0) {
      handleUploadFiles(Array.from(e.dataTransfer.files));
    }
  };

  // URL import handler
  const handleUrlSubmit = async (e) => {
    e.preventDefault();
    if (!urlInput.trim() || isSubmittingUrl) return;

    setUrlError(null);
    setIsSubmittingUrl(true);
    try {
      const res = await axiosInstance.post('/documents/url', {
        url: urlInput.trim(),
        roadmapId: selectedRoadmapId ? Number(selectedRoadmapId) : null
      });

      const newDoc = res.data;
      setDocuments((prev) => [newDoc, ...prev]);
      setUrlInput('');
      addToast?.('Web document imported successfully!', 'success');

      if (newDoc.status === 'Processing') {
        startPollingDocument(newDoc.id, 2000);
      }
    } catch (err) {
      console.error('URL import failed:', err);
      const msg = err.response?.data?.message || 'Could not fetch content from this URL. Please verify the link.';
      setUrlError(msg);
    } finally {
      setIsSubmittingUrl(false);
    }
  };

  // Delete document handler
  const confirmDelete = async () => {
    if (!deleteTarget || isDeleting) return;
    setIsDeleting(true);
    try {
      await axiosInstance.delete(`/documents/${deleteTarget.id}`);
      setDocuments((prev) => prev.filter((d) => d.id !== deleteTarget.id));
      addToast?.(`"${deleteTarget.filename}" deleted.`, 'success');
      setDeleteTarget(null);
    } catch (err) {
      console.error('Failed to delete document:', err);
      addToast?.('Failed to delete document. Please try again.', 'error');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className={styles.documentsContainer}>
      {/* Page Header */}
      <div className={styles.pageHeader}>
        <div className={styles.headerTitles}>
          <div className={styles.tagStrip}>
            <span className={styles.libraryTag}>Knowledge Base</span>
          </div>
          <h1 className={styles.pageTitle}>Documents & Library</h1>
          <p className={styles.pageSubtitle}>
            Upload study materials, lecture slides, or web resources to generate summaries,
            synthesis notes, and contextual study room citations.
          </p>
        </div>

        {/* Optional Roadmap Attachment Selector */}
        <div className={styles.roadmapSelectWrapper}>
          <label htmlFor="roadmap-select" className={styles.selectLabel}>
            Attach to Roadmap (Optional):
          </label>
          <select
            id="roadmap-select"
            className={styles.roadmapSelect}
            value={selectedRoadmapId}
            onChange={(e) => setSelectedRoadmapId(e.target.value)}
          >
            <option value="">General Library (No Roadmap)</option>
            {roadmaps.map((r) => (
              <option key={r.id} value={r.id}>
                {r.title}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Upload Section: Drag & Drop Dropzone + URL Input */}
      <div className={styles.uploadSectionGrid}>
        {/* HTML5 Drag & Drop Dropzone */}
        <div
          className={`${styles.dropzone} ${isDragging ? styles.dropzoneActive : ''}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
        >
          <input
            type="file"
            ref={fileInputRef}
            style={{ display: 'none' }}
            multiple
            accept=".pdf,.docx,.txt,.md"
            onChange={(e) => {
              if (e.target.files) {
                handleUploadFiles(Array.from(e.target.files));
                e.target.value = '';
              }
            }}
          />

          <div className={styles.dropzoneIcon}>📁</div>
          <h3 className={styles.dropzoneTitle}>
            Drag & drop study files here, or <span className={styles.browseText}>browse</span>
          </h3>
          <p className={styles.dropzoneLimits}>
            Accepted formats: <strong>PDF, DOCX, TXT, MD</strong> · Max size: <strong>25MB</strong>
          </p>

          {/* Inline Upload Error */}
          {uploadError && (
            <div className={styles.inlineErrorBox} onClick={(e) => e.stopPropagation()}>
              <span>⚠️</span>
              <span>{uploadError}</span>
            </div>
          )}

          {/* Per-File Progress Bars */}
          {Object.entries(uploadProgressMap).map(([filename, progress]) => (
            <div
              key={filename}
              className={styles.fileProgressRow}
              onClick={(e) => e.stopPropagation()}
            >
              <div className={styles.progressFileInfo}>
                <span className={styles.progressFileName}>{filename}</span>
                <span className={styles.progressPercent}>{progress}%</span>
              </div>
              <ProgressBar progress={progress} height={6} />
            </div>
          ))}
        </div>

        {/* URL Importer Card */}
        <Card className={styles.urlCard}>
          <div className={styles.urlHeader}>
            <span className={styles.urlIcon}>🌐</span>
            <h3 className={styles.urlTitle}>Import from Web URL</h3>
          </div>
          <p className={styles.urlDesc}>
            Fetch articles, technical documentation, or online tutorials directly into your study deck.
          </p>

          <form onSubmit={handleUrlSubmit} className={styles.urlForm}>
            <div className={styles.urlInputGroup}>
              <input
                type="url"
                required
                placeholder="https://example.com/guide..."
                className={styles.urlInput}
                value={urlInput}
                onChange={(e) => setUrlInput(e.target.value)}
              />
              <Button
                variant="primary"
                type="submit"
                disabled={!urlInput.trim() || isSubmittingUrl}
              >
                {isSubmittingUrl ? 'Importing...' : 'Import'}
              </Button>
            </div>

            {/* Inline URL-fetch failure message */}
            {urlError && (
              <div className={styles.inlineUrlError}>
                <span>⚠️</span>
                <span>{urlError}</span>
              </div>
            )}
          </form>
        </Card>
      </div>

      {/* Documents Table */}
      <Card className={styles.tableCard}>
        <div className={styles.tableHeaderBar}>
          <h2 className={styles.tableHeading}>Uploaded Documents ({documents.length})</h2>
        </div>

        {loading ? (
          <div className={styles.tableLoading}>
            <div className={styles.spinner} />
            <p>Loading documents...</p>
          </div>
        ) : documents.length === 0 ? (
          <div className={styles.emptyTableState}>
            <span className={styles.emptyTableIcon}>📚</span>
            <h3>No documents in your library yet</h3>
            <p>Upload lecture notes, whitepapers, or enter a URL above to start synthesizing knowledge.</p>
          </div>
        ) : (
          <div className={styles.tableResponsive}>
            <table className={styles.docsTable}>
              <thead>
                <tr>
                  <th>Document</th>
                  <th>Size</th>
                  <th>Status</th>
                  <th>Uploaded Date</th>
                  <th>Linked Roadmap</th>
                  <th className={styles.actionColHeader}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {documents.map((doc) => {
                  const icon = getFileTypeIcon(doc.filename, doc.fileType);
                  return (
                    <tr key={doc.id} className={styles.tableRow}>
                      <td className={styles.docNameCell}>
                        <span className={styles.tableDocIcon}>{icon}</span>
                        <div className={styles.docNameWrapper}>
                          <span
                            className={styles.docNameLink}
                            onClick={() => navigate(`/documents/${doc.id}`)}
                            title={doc.filename}
                          >
                            {doc.filename}
                          </span>
                        </div>
                      </td>

                      <td className={styles.sizeCell}>
                        {formatBytes(doc.fileSize)}
                      </td>

                      <td>
                        <StatusPill status={doc.status || 'Ready'} />
                      </td>

                      <td className={styles.dateCell}>
                        {new Date(doc.uploadedAt).toLocaleDateString(undefined, {
                          year: 'numeric',
                          month: 'short',
                          day: 'numeric'
                        })}
                      </td>

                      <td className={styles.roadmapCell}>
                        {doc.roadmapTitle ? (
                          <span className={styles.roadmapBadge}>{doc.roadmapTitle}</span>
                        ) : (
                          <span className={styles.unlinkedText}>—</span>
                        )}
                      </td>

                      <td className={styles.actionCell}>
                        <div className={styles.actionBtnGroup}>
                          <button
                            type="button"
                            className={styles.openBtn}
                            onClick={() => navigate(`/documents/${doc.id}`)}
                          >
                            Open
                          </button>
                          <button
                            type="button"
                            className={styles.deleteBtn}
                            onClick={() => setDeleteTarget(doc)}
                            title="Delete document"
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        isOpen={Boolean(deleteTarget)}
        title="Delete Document"
        message={`This will permanently delete "${deleteTarget?.filename}" and its AI summaries. This action cannot be undone.`}
        confirmLabel={isDeleting ? 'Deleting...' : 'Delete Document'}
        cancelLabel="Cancel"
        isDanger={true}
        onConfirm={confirmDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
};

export default Documents;
