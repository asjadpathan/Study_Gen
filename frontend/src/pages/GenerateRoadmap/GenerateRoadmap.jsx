import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import axiosInstance from '../../services/axiosInstance';
import { useToast } from '../../context/ToastContext';
import Button from '../../components/Button';
import Card from '../../components/Card';
import StatusPill from '../../components/StatusPill';
import StepIndicator from '../../components/StepIndicator';
import AIStageLoader from '../../components/AIStageLoader';
import RateLimitBanner from '../../components/RateLimitBanner';
import ErrorCard from '../../components/ErrorCard';
import styles from './GenerateRoadmap.module.css';

const stepsMeta = [
  { title: 'Goal & Role' },
  { title: 'Skill Baseline' },
  { title: 'Time Commitment' },
  { title: 'Learning Style' },
  { title: 'Context & Files' },
  { title: 'Review & Build' },
];

const learningStyles = [
  {
    id: 'PRACTICAL',
    title: 'Practical & Project-Based',
    description: 'Hands-on coding drills, real-world mini-projects, and immediate code execution.',
    icon: '🛠️',
  },
  {
    id: 'THEORY_FIRST',
    title: 'Theory & Foundations First',
    description: 'Deep architectural principles, algorithm analysis, and core computer science theory.',
    icon: '📖',
  },
  {
    id: 'VISUAL_FIRST',
    title: 'Visual & Conceptual',
    description: 'Interactive node graphs, visual diagrams, and mental model maps.',
    icon: '🧠',
  },
  {
    id: 'CONCISE',
    title: 'Concise & Fast-Paced',
    description: 'Bite-sized summaries, key takeaways, and accelerated progression.',
    icon: '⚡',
  },
];

const GenerateRoadmap = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { addToast } = useToast();

  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState({
    targetTopic: '',
    targetRole: '',
    selfLevel: 'INTERMEDIATE', // 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
    knownSkills: [],
    hoursPerWeek: 10,
    targetWeeks: 8,
    learningStyle: 'PRACTICAL',
    fileUploads: [], // Array of { id, name, status }
    sourceUrls: [],
    jobDescription: '',
  });

  // Prefill state from Career or external referral
  useEffect(() => {
    if (location.state) {
      if (location.state.jobDescription) {
        setFormData((prev) => ({
          ...prev,
          jobDescription: location.state.jobDescription,
          targetRole: location.state.targetRole || prev.targetRole,
          targetTopic: location.state.targetTopic || location.state.targetRole || prev.targetTopic,
        }));
        addToast('Job description imported for roadmap alignment!', 'info');
      }
      if (location.state.initialStep) {
        setCurrentStep(location.state.initialStep);
      }
    }
  }, [location.state]);

  // Step 2 Tag Input State
  const [skillInput, setSkillInput] = useState('');

  // Step 5 URL Input State
  const [urlInput, setUrlInput] = useState('');

  // Submission & Overlay States
  const [submitting, setSubmitting] = useState(false);
  const [takingLonger, setTakingLonger] = useState(false);
  const [errorCode, setErrorCode] = useState(null); // 422 | 429 | 502
  const [errorMessage, setErrorMessage] = useState('');

  // Handle block browser back navigation during generation
  useEffect(() => {
    if (submitting) {
      const handleBeforeUnload = (e) => {
        e.preventDefault();
        e.returnValue = '';
      };
      window.addEventListener('beforeunload', handleBeforeUnload);
      return () => window.removeEventListener('beforeunload', handleBeforeUnload);
    }
  }, [submitting]);

  // Validation logic per step
  const isStepValid = (step) => {
    switch (step) {
      case 1:
        return formData.targetTopic.trim().length > 0 && formData.targetTopic.length <= 200;
      case 2:
        return !!formData.selfLevel;
      case 3:
        return formData.hoursPerWeek >= 1 && formData.hoursPerWeek <= 80 && formData.targetWeeks >= 1;
      case 4:
        return !!formData.learningStyle;
      case 5:
        return formData.jobDescription.length <= 12000;
      case 6:
        return true;
      default:
        return false;
    }
  };

  // Tag Input Handlers for Step 2
  const handleAddSkill = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      const val = skillInput.trim();
      if (val && val.length <= 60 && formData.knownSkills.length < 30 && !formData.knownSkills.includes(val)) {
        setFormData((prev) => ({ ...prev, knownSkills: [...prev.knownSkills, val] }));
        setSkillInput('');
      }
    }
  };

  const handleRemoveSkill = (skillToRemove) => {
    setFormData((prev) => ({
      ...prev,
      knownSkills: prev.knownSkills.filter((s) => s !== skillToRemove),
    }));
  };

  // File Upload Handler for Step 5
  const handleFileUpload = async (files) => {
    if (!files || files.length === 0) return;
    const file = files[0];

    const fileItem = { id: Date.now(), name: file.name, status: 'UPLOADING' };
    setFormData((prev) => ({ ...prev, fileUploads: [...prev.fileUploads, fileItem] }));

    const data = new FormData();
    data.append('file', file);

    try {
      const res = await axiosInstance.post('/documents/upload', data, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      const docId = res.data?.id || fileItem.id;

      // Update status to READY
      setFormData((prev) => ({
        ...prev,
        fileUploads: prev.fileUploads.map((f) =>
          f.id === fileItem.id ? { ...f, id: docId, status: 'READY' } : f
        ),
      }));
      addToast(`Document "${file.name}" uploaded successfully`, 'success');
    } catch {
      setFormData((prev) => ({
        ...prev,
        fileUploads: prev.fileUploads.map((f) =>
          f.id === fileItem.id ? { ...f, status: 'FAILED' } : f
        ),
      }));
      addToast(`Failed to upload "${file.name}"`, 'error');
    }
  };

  // Add Source URL Handler for Step 5
  const handleAddUrl = async () => {
    if (!urlInput.trim()) return;
    const url = urlInput.trim();
    setUrlInput('');

    try {
      await axiosInstance.post('/documents/url', { url });
      setFormData((prev) => ({ ...prev, sourceUrls: [...prev.sourceUrls, url] }));
      addToast(`Added source URL: ${url}`, 'success');
    } catch {
      setFormData((prev) => ({ ...prev, sourceUrls: [...prev.sourceUrls, url] }));
    }
  };

  // Final Submit Handler: Transitions to the 5-question Diagnostic Test
  const handleSubmit = () => {
    if (!formData.targetTopic || formData.targetTopic.trim().length === 0) {
      addToast('Please enter a target topic in Step 1 before proceeding.', 'error');
      setCurrentStep(1);
      return;
    }

    addToast('Calibrating your learning path with a 5-question diagnostic test...', 'info');
    navigate('/diagnostic-test', {
      state: {
        formData: {
          targetTopic: formData.targetTopic.trim(),
          targetRole: formData.targetRole || 'Software Engineer',
          selfLevel: formData.selfLevel || 'INTERMEDIATE',
          hoursPerWeek: formData.hoursPerWeek || 10,
          learningStyle: formData.learningStyle || 'PRACTICAL',
          knownSkills: formData.knownSkills || [],
          targetWeeks: formData.targetWeeks || 8,
        },
      },
    });
  };

  return (
    <div className={styles.container}>
      {/* Header */}
      <div className={styles.pageHeader}>
        <h1 className={styles.title}>AI Roadmap Generator</h1>
        <p className={styles.subtitle}>
          Configure your goals, baseline skills, and schedule to build a custom AI learning path.
        </p>
      </div>

      {/* 429 Rate Limit Banner */}
      {errorCode === 429 && <RateLimitBanner message={errorMessage} />}

      {/* Error Card for 502, 500, 400, or network failures */}
      {(errorCode && errorCode !== 429 && errorCode !== 422) && (
        <ErrorCard
          title={`Generation Error (${errorCode})`}
          message={errorMessage}
          onRetry={handleSubmit}
        />
      )}

      {/* Main Wizard Layout */}
      <div className={styles.wizardLayout}>
        {/* Step Rail Indicator */}
        <aside className={styles.stepRail}>
          <StepIndicator
            steps={stepsMeta}
            currentStep={currentStep}
            onStepClick={(step) => {
              if (step < currentStep) setCurrentStep(step);
            }}
          />
        </aside>

        {/* Form Body Area */}
        <main className={styles.formArea}>
          <Card className={styles.formCard}>
            {/* Step 1: Goal & Role */}
            {currentStep === 1 && (
              <div className={styles.stepContent}>
                <h2 className={styles.stepHeaderTitle}>Step 1: What do you want to learn?</h2>
                <p className={styles.stepHeaderDesc}>
                  Enter the subject, technology, or domain you want to master.
                </p>

                {errorCode === 422 && (
                  <div className={styles.errorAlert}>⚠️ {errorMessage}</div>
                )}

                <div className={styles.fieldGroup}>
                  <label htmlFor="targetTopic" className={styles.label}>
                    Target Topic or Subject <span className={styles.required}>*</span>
                  </label>
                  <input
                    id="targetTopic"
                    type="text"
                    maxLength={200}
                    placeholder="e.g. Full-Stack Web Development, System Architecture, Quantum Physics"
                    value={formData.targetTopic}
                    onChange={(e) => setFormData({ ...formData, targetTopic: e.target.value })}
                    className={styles.input}
                  />
                  <span className={styles.charCount}>
                    {formData.targetTopic.length}/200
                  </span>
                </div>

                <div className={styles.fieldGroup}>
                  <label htmlFor="targetRole" className={styles.label}>
                    Target Role or Objective (Optional)
                  </label>
                  <input
                    id="targetRole"
                    type="text"
                    maxLength={100}
                    placeholder="e.g. Senior Frontend Engineer, Data Scientist, Solutions Architect"
                    value={formData.targetRole}
                    onChange={(e) => setFormData({ ...formData, targetRole: e.target.value })}
                    className={styles.input}
                  />
                  <span className={styles.charCount}>
                    {formData.targetRole.length}/100
                  </span>
                </div>
              </div>
            )}

            {/* Step 2: Skill Baseline & Known Skills */}
            {currentStep === 2 && (
              <div className={styles.stepContent}>
                <h2 className={styles.stepHeaderTitle}>Step 2: Self-Assessed Baseline</h2>
                <p className={styles.stepHeaderDesc}>
                  Rate your current expertise and list skills you already know so we can skip redundant topics.
                </p>

                <div className={styles.fieldGroup}>
                  <label className={styles.label}>Current Experience Level</label>
                  <div className={styles.segmentedControl}>
                    {['BEGINNER', 'INTERMEDIATE', 'ADVANCED'].map((lvl) => (
                      <button
                        key={lvl}
                        type="button"
                        className={`${styles.segmentBtn} ${
                          formData.selfLevel === lvl ? styles.segmentActive : ''
                        }`}
                        onClick={() => setFormData({ ...formData, selfLevel: lvl })}
                      >
                        {lvl}
                      </button>
                    ))}
                  </div>
                </div>

                <div className={styles.fieldGroup}>
                  <label htmlFor="skillInput" className={styles.label}>
                    Known Skills (Press Enter to add tag)
                  </label>
                  <input
                    id="skillInput"
                    type="text"
                    maxLength={60}
                    placeholder="Type a skill and press Enter (e.g. HTML, Git, Basic SQL)..."
                    value={skillInput}
                    onChange={(e) => setSkillInput(e.target.value)}
                    onKeyDown={handleAddSkill}
                    className={styles.input}
                  />

                  <div className={styles.chipsContainer}>
                    {formData.knownSkills.map((skill, idx) => (
                      <span key={idx} className={styles.chip}>
                        {skill}
                        <button
                          type="button"
                          className={styles.chipRemove}
                          onClick={() => handleRemoveSkill(skill)}
                        >
                          ×
                        </button>
                      </span>
                    ))}
                  </div>
                  <span className={styles.charCount}>
                    {formData.knownSkills.length}/30 skills added
                  </span>
                </div>
              </div>
            )}

            {/* Step 3: Time Commitment */}
            {currentStep === 3 && (
              <div className={styles.stepContent}>
                <h2 className={styles.stepHeaderTitle}>Step 3: Schedule & Weekly Commitment</h2>
                <p className={styles.stepHeaderDesc}>
                  Define how many hours per week you can dedicate to studying.
                </p>

                <div className={styles.fieldGroup}>
                  <div className={styles.sliderLabelRow}>
                    <label className={styles.label}>Hours Per Week</label>
                    <span className={styles.sliderValue}>{formData.hoursPerWeek} hrs/week</span>
                  </div>
                  <input
                    type="range"
                    min={1}
                    max={80}
                    value={formData.hoursPerWeek}
                    onChange={(e) => setFormData({ ...formData, hoursPerWeek: Number(e.target.value) })}
                    className={styles.rangeSlider}
                  />
                </div>

                <div className={styles.fieldGroup}>
                  <div className={styles.sliderLabelRow}>
                    <label className={styles.label}>Target Timeline (Weeks)</label>
                    <span className={styles.sliderValue}>{formData.targetWeeks} weeks</span>
                  </div>
                  <input
                    type="range"
                    min={1}
                    max={104}
                    value={formData.targetWeeks}
                    onChange={(e) => setFormData({ ...formData, targetWeeks: Number(e.target.value) })}
                    className={styles.rangeSlider}
                  />
                </div>

                <div className={styles.totalHoursCallout}>
                  <span>💡</span> Live Total Estimate: 
                  <strong> ≈ {formData.hoursPerWeek * formData.targetWeeks} hours in total</strong>
                </div>
              </div>
            )}

            {/* Step 4: Learning Style Cards */}
            {currentStep === 4 && (
              <div className={styles.stepContent}>
                <h2 className={styles.stepHeaderTitle}>Step 4: Preferred Learning Style</h2>
                <p className={styles.stepHeaderDesc}>
                  Select the pedagogical structure that best matches how you absorb information.
                </p>

                <div className={styles.styleGrid}>
                  {learningStyles.map((style) => {
                    const isSelected = formData.learningStyle === style.id;
                    return (
                      <div
                        key={style.id}
                        className={`${styles.styleCard} ${isSelected ? styles.styleCardSelected : ''}`}
                        onClick={() => setFormData({ ...formData, learningStyle: style.id })}
                      >
                        <span className={styles.styleIcon}>{style.icon}</span>
                        <h4 className={styles.styleTitle}>{style.title}</h4>
                        <p className={styles.styleDesc}>{style.description}</p>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Step 5: Files, URLs, & Job Description */}
            {currentStep === 5 && (
              <div className={styles.stepContent}>
                <h2 className={styles.stepHeaderTitle}>Step 5: Context, Files & Job Description</h2>
                <p className={styles.stepHeaderDesc}>
                  Upload lecture notes, attach reference URLs, or paste a target job description.
                </p>

                {/* File Upload Drop Area */}
                <div className={styles.fieldGroup}>
                  <label className={styles.label}>Upload Syllabus or Reference Files (Optional)</label>
                  <input
                    type="file"
                    id="wizardFileInput"
                    onChange={(e) => handleFileUpload(e.target.files)}
                    className={styles.hiddenFileInput}
                  />
                  <label htmlFor="wizardFileInput" className={styles.uploadBox}>
                    <span>☁️ Upload PDF/DOCX Document</span>
                  </label>

                  {formData.fileUploads.length > 0 && (
                    <div className={styles.fileList}>
                      {formData.fileUploads.map((f) => (
                        <div key={f.id} className={styles.fileItem}>
                          <span>📄 {f.name}</span>
                          <StatusPill status={f.status} />
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* URL Attachment */}
                <div className={styles.fieldGroup}>
                  <label htmlFor="urlInput" className={styles.label}>Source URL Reference (Optional)</label>
                  <div className={styles.urlInputRow}>
                    <input
                      id="urlInput"
                      type="url"
                      placeholder="https://example.com/syllabus"
                      value={urlInput}
                      onChange={(e) => setUrlInput(e.target.value)}
                      className={styles.input}
                    />
                    <Button variant="outlined" size="sm" onClick={handleAddUrl}>
                      Add URL
                    </Button>
                  </div>
                  {formData.sourceUrls.length > 0 && (
                    <div className={styles.urlList}>
                      {formData.sourceUrls.map((u, idx) => (
                        <div key={idx} className={styles.urlTag}>🔗 {u}</div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Job Description Textarea */}
                <div className={styles.fieldGroup}>
                  <label htmlFor="jobDesc" className={styles.label}>
                    Target Job Description (Optional)
                  </label>
                  <textarea
                    id="jobDesc"
                    rows={4}
                    maxLength={12000}
                    placeholder="Paste job posting requirements to align your roadmap with industry demand..."
                    value={formData.jobDescription}
                    onChange={(e) => setFormData({ ...formData, jobDescription: e.target.value })}
                    className={styles.textarea}
                  />
                  <span className={styles.charCount}>
                    {formData.jobDescription.length}/12000
                  </span>
                </div>
              </div>
            )}

            {/* Step 6: Review & Final Build */}
            {currentStep === 6 && (
              <div className={styles.stepContent}>
                <h2 className={styles.stepHeaderTitle}>Step 6: Review Your Configuration</h2>
                <p className={styles.stepHeaderDesc}>
                  Verify your settings before generating your AI roadmap.
                </p>

                <div className={styles.reviewCardsGrid}>
                  <Card className={styles.reviewCard}>
                    <div className={styles.reviewHeader}>
                      <h4>1. Goal & Role</h4>
                      <button className={styles.editLink} onClick={() => setCurrentStep(1)}>Edit</button>
                    </div>
                    <p><strong>Topic:</strong> {formData.targetTopic}</p>
                    <p><strong>Role:</strong> {formData.targetRole || 'Not specified'}</p>
                  </Card>

                  <Card className={styles.reviewCard}>
                    <div className={styles.reviewHeader}>
                      <h4>2. Skill Baseline</h4>
                      <button className={styles.editLink} onClick={() => setCurrentStep(2)}>Edit</button>
                    </div>
                    <p><strong>Level:</strong> {formData.selfLevel}</p>
                    <p><strong>Known Skills:</strong> {formData.knownSkills.join(', ') || 'None specified'}</p>
                  </Card>

                  <Card className={styles.reviewCard}>
                    <div className={styles.reviewHeader}>
                      <h4>3. Time Commitment</h4>
                      <button className={styles.editLink} onClick={() => setCurrentStep(3)}>Edit</button>
                    </div>
                    <p><strong>Schedule:</strong> {formData.hoursPerWeek} hrs/week over {formData.targetWeeks} weeks</p>
                    <p><strong>Total Estimate:</strong> ≈ {formData.hoursPerWeek * formData.targetWeeks} hours</p>
                  </Card>

                  <Card className={styles.reviewCard}>
                    <div className={styles.reviewHeader}>
                      <h4>4. Learning Style & Context</h4>
                      <button className={styles.editLink} onClick={() => setCurrentStep(4)}>Edit</button>
                    </div>
                    <p><strong>Pedagogy:</strong> {formData.learningStyle}</p>
                    <p><strong>Uploaded Files:</strong> {formData.fileUploads.length} attached</p>
                  </Card>
                </div>
              </div>
            )}

            {/* Navigation Action Buttons Footer */}
            <div className={styles.formFooterActions}>
              {currentStep > 1 && (
                <Button
                  variant="outlined"
                  size="md"
                  onClick={() => setCurrentStep((prev) => prev - 1)}
                >
                  ← Back
                </Button>
              )}

              {currentStep < 6 ? (
                <Button
                  variant="primary"
                  size="md"
                  disabled={!isStepValid(currentStep)}
                  onClick={() => setCurrentStep((prev) => prev + 1)}
                  className={styles.nextBtn}
                >
                  Next Step →
                </Button>
              ) : (
                <Button
                  variant="primary"
                  size="lg"
                  onClick={handleSubmit}
                  className={styles.generateBtn}
                >
                  ✨ Take 5-Question Diagnostic & Build Roadmap ➔
                </Button>
              )}
            </div>
          </Card>
        </main>

        {/* Desktop Live Summary Side Panel (≥1024px) */}
        <aside className={styles.summaryPanel}>
          <Card className={styles.summaryCard}>
            <h3 className={styles.summaryTitle}>Live Configuration Summary</h3>
            <div className={styles.summaryDivider} />

            <div className={styles.summaryItem}>
              <span className={styles.summaryLabel}>Topic</span>
              <span className={styles.summaryValue}>{formData.targetTopic || 'Not set'}</span>
            </div>

            <div className={styles.summaryItem}>
              <span className={styles.summaryLabel}>Baseline</span>
              <span className={styles.summaryValue}>{formData.selfLevel}</span>
            </div>

            <div className={styles.summaryItem}>
              <span className={styles.summaryLabel}>Weekly Hours</span>
              <span className={styles.summaryValue}>{formData.hoursPerWeek} hrs/wk</span>
            </div>

            <div className={styles.summaryItem}>
              <span className={styles.summaryLabel}>Total Hours</span>
              <span className={styles.summaryValue}>
                ≈ {formData.hoursPerWeek * formData.targetWeeks} hrs
              </span>
            </div>

            <div className={styles.summaryItem}>
              <span className={styles.summaryLabel}>Style</span>
              <span className={styles.summaryValue}>{formData.learningStyle}</span>
            </div>
          </Card>
        </aside>
      </div>

      {/* Full-Page Fixed Generation Overlay */}
      {submitting && (
        <div className={styles.overlayBackdrop}>
          <div className={styles.overlayContent}>
            <AIStageLoader
              title="Building Your AI Learning Roadmap"
              messages={[
                'Analyzing target topic & requirements...',
                'Synthesizing optimal learning modules...',
                'Generating concept dependency graphs...',
                'Calibrating diagnostic assessment questions...',
              ]}
              intervalMs={4000}
            />
            {takingLonger && (
              <p className={styles.takingLongerNote}>
                ⏳ Generation is taking longer than usual due to complex AI module synthesis... Please hold tight.
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default GenerateRoadmap;
