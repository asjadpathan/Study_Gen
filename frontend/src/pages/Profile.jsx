import React, { useState, useEffect } from 'react';
import axiosInstance from '../services/axiosInstance';
import { useToast } from '../context/ToastContext';
import Heatmap from '../components/Heatmap';
import styles from './Profile.module.css';

const BADGES_CATALOG = [
  {
    name: 'First Step',
    icon: '🚀',
    description: 'Logged your first active study engagement.',
    howToEarn: 'Complete your first Pomodoro session, take a test, or start an AI chat.',
  },
  {
    name: 'Streak Starter',
    icon: '🔥',
    description: 'Maintained a 3-day continuous study streak.',
    howToEarn: 'Study at least 15 minutes each day for 3 consecutive days.',
  },
  {
    name: 'Dedication Champion',
    icon: '⚡',
    description: 'Maintained a 7-day continuous study streak.',
    howToEarn: 'Maintain an unbroken daily learning habit for a full week.',
  },
  {
    name: 'Unstoppable',
    icon: '👑',
    description: 'Maintained an epic 30-day continuous study streak.',
    howToEarn: 'Complete verified learning activities for 30 consecutive days.',
  },
  {
    name: 'Century Club',
    icon: '💯',
    description: 'Logged over 100 minutes of focused study.',
    howToEarn: 'Accumulate 100 total focused study minutes in the Study Room.',
  },
  {
    name: 'Focus Master',
    icon: '🧘',
    description: 'Logged over 500 minutes of focused study.',
    howToEarn: 'Reach 500 total focused study minutes across your roadmaps.',
  },
  {
    name: 'Test Ace',
    icon: '🎯',
    description: 'Scored 80%+ on a module test or diagnostic.',
    howToEarn: 'Pass any module assessment or remediation test with an 80%+ score.',
  },
  {
    name: 'Roadmap Finisher',
    icon: '🏆',
    description: 'Completed all modules in an engineering roadmap.',
    howToEarn: 'Master and complete every sequential module in an active roadmap.',
  },
];

export default function Profile() {
  const { addToast } = useToast();

  const [profile, setProfile] = useState(null);
  const [heatmapData, setHeatmapData] = useState([]);
  const [heatmapStats, setHeatmapStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Edit Profile Modal State
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editName, setEditName] = useState('');
  const [savingProfile, setSavingProfile] = useState(false);

  // Fetch Profile & Heatmap on mount
  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);

      const [profileRes, heatmapRes] = await Promise.all([
        axiosInstance.get('/users/me/profile'),
        axiosInstance.get('/activity/heatmap'),
      ]);

      setProfile(profileRes.data);
      setEditName(profileRes.data.fullName || '');
      setHeatmapStats(heatmapRes.data);

      // Format heatmap logs for 365-day graph
      const rawHeatmap = heatmapRes.data?.heatmap || [];
      const formattedHeatmap = rawHeatmap.map((entry) => ({
        date: entry.date,
        minutesLogged: entry.totalMinutes || 0,
        activityCount: entry.count ?? entry.sessionCount ?? (entry.totalMinutes > 0 ? 1 : 0),
        intensityLevel: entry.intensityLevel,
      }));
      setHeatmapData(formattedHeatmap);
    } catch (err) {
      console.error('Failed to load profile data:', err);
      setError('Could not load user profile. Please check your connection.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Save Edit Profile Modal
  const handleSaveProfile = async (e) => {
    e.preventDefault();
    if (!editName || editName.trim().length < 2) {
      addToast('Full name must be at least 2 characters.', 'warning');
      return;
    }

    try {
      setSavingProfile(true);
      const res = await axiosInstance.patch('/users/me', { fullName: editName.trim() });
      setProfile(res.data);
      setIsEditModalOpen(false);
      addToast('Profile updated successfully!', 'success');
    } catch (err) {
      console.error('Failed to update profile:', err);
      addToast('Failed to update profile. Please try again.', 'error');
    } finally {
      setSavingProfile(false);
    }
  };

  // Helper for Initials
  const getInitials = (name) => {
    if (!name) return 'SG';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  };

  // SVG Radar Chart calculations
  const renderSkillMatrixRadar = () => {
    const radarData = profile?.skillsRadar || [
      { axis: 'Frontend', score: 20 },
      { axis: 'Backend', score: 20 },
      { axis: 'Database', score: 20 },
      { axis: 'Cloud & DevOps', score: 20 },
      { axis: 'Architecture', score: 20 },
      { axis: 'Algorithms', score: 20 },
    ];

    const viewBoxWidth = 360;
    const viewBoxHeight = 310;
    const centerX = viewBoxWidth / 2; // 180
    const centerY = viewBoxHeight / 2; // 155
    const radius = 80;
    const totalAxes = radarData.length;

    // Helper to calculate (x, y) for an axis at given percentage (0..1)
    const getCoordinates = (index, pct) => {
      const angle = (index * 2 * Math.PI) / totalAxes - Math.PI / 2;
      const r = radius * pct;
      return {
        x: centerX + r * Math.cos(angle),
        y: centerY + r * Math.sin(angle),
      };
    };

    // Concentric Web Grid Polygons (25%, 50%, 75%, 100%)
    const rings = [0.25, 0.5, 0.75, 1.0];

    // Data polygon points
    const dataPoints = radarData.map((d, i) => {
      const scorePct = Math.max(0.15, Math.min(1.0, (d.score || 0) / 100));
      return getCoordinates(i, scorePct);
    });
    const polygonPointsString = dataPoints.map((p) => `${p.x},${p.y}`).join(' ');

    return (
      <div className={styles.radarWrapper}>
        <svg
          viewBox={`0 0 ${viewBoxWidth} ${viewBoxHeight}`}
          className={styles.radarSvg}
        >
          {/* Concentric Grid Rings */}
          {rings.map((ring, idx) => {
            const ringPoints = Array.from({ length: totalAxes }, (_, i) => {
              const p = getCoordinates(i, ring);
              return `${p.x},${p.y}`;
            }).join(' ');

            return (
              <polygon
                key={idx}
                points={ringPoints}
                fill={idx === rings.length - 1 ? '#f8fafc' : 'transparent'}
                stroke="#e2e8f0"
                strokeWidth="1"
                strokeDasharray={idx < 3 ? '3 3' : 'none'}
              />
            );
          })}

          {/* Radial Axis Lines */}
          {Array.from({ length: totalAxes }, (_, i) => {
            const outer = getCoordinates(i, 1.0);
            return (
              <line
                key={i}
                x1={centerX}
                y1={centerY}
                x2={outer.x}
                y2={outer.y}
                stroke="#cbd5e1"
                strokeWidth="1"
              />
            );
          })}

          {/* Filled Data Polygon */}
          <polygon
            points={polygonPointsString}
            fill="rgba(59, 130, 246, 0.2)"
            stroke="var(--color-primary, #3b82f6)"
            strokeWidth="2.5"
            className={styles.radarPolygon}
          />

          {/* Vertex Circles and Values */}
          {dataPoints.map((p, i) => (
            <circle
              key={i}
              cx={p.x}
              cy={p.y}
              r="4.5"
              fill="#ffffff"
              stroke="var(--color-primary, #3b82f6)"
              strokeWidth="2.5"
              className={styles.radarVertex}
            >
              <title>{`${radarData[i].axis}: ${radarData[i].score}%`}</title>
            </circle>
          ))}

          {/* Axis Labels with dynamic anchoring to prevent clipping */}
          {radarData.map((d, i) => {
            let labelPos;
            let anchor = 'middle';
            let dy = 4;

            if (i === 0) {
              labelPos = getCoordinates(i, 1.28);
              anchor = 'middle';
              dy = -4;
            } else if (i === 1) {
              labelPos = getCoordinates(i, 1.22);
              anchor = 'start';
              dy = 3;
            } else if (i === 2) {
              labelPos = getCoordinates(i, 1.22);
              anchor = 'start';
              dy = 6;
            } else if (i === 3) {
              labelPos = getCoordinates(i, 1.28);
              anchor = 'middle';
              dy = 14;
            } else if (i === 4) {
              labelPos = getCoordinates(i, 1.22);
              anchor = 'end';
              dy = 6;
            } else {
              labelPos = getCoordinates(i, 1.22);
              anchor = 'end';
              dy = 3;
            }

            return (
              <text
                key={i}
                x={labelPos.x}
                y={labelPos.y + dy}
                textAnchor={anchor}
                className={styles.radarLabel}
              >
                {d.axis}
              </text>
            );
          })}
        </svg>

        {/* Domain Skill Progress Breakdown */}
        <div className={styles.skillListGrid}>
          {radarData.map((d, i) => (
            <div key={i} className={styles.skillItem}>
              <div className={styles.skillHeader}>
                <span className={styles.skillName}>{d.axis}</span>
                <span className={styles.skillScore}>{d.score}%</span>
              </div>
              <div className={styles.skillProgressBar}>
                <div
                  className={styles.skillProgressFill}
                  style={{ width: `${Math.min(100, Math.max(8, d.score))}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  };

  if (loading) {
    return (
      <div className={styles.loadingContainer}>
        <div className={styles.spinner}></div>
        <p>Loading your profile & activity records...</p>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className={styles.errorContainer}>
        <span className={styles.errorIcon}>⚠️</span>
        <h3>Could not load profile</h3>
        <p>{error}</p>
        <button onClick={loadData} className={styles.retryBtn}>
          Try Again
        </button>
      </div>
    );
  }

  // Check if new-user (all core metrics 0)
  const isNewUser =
    profile.currentStreak === 0 &&
    profile.completedModulesCount === 0 &&
    profile.totalStudyMinutes === 0;

  // Build earned badge mapping
  const earnedBadgeMap = new Map();
  (profile.badges || []).forEach((b) => {
    earnedBadgeMap.set(b.badgeName?.toLowerCase().trim(), b.earnedAt);
  });

  return (
    <div className={styles.container}>
      {/* Header Profile Card */}
      <header className={styles.profileHeader}>
        <div className={styles.headerLeft}>
          <div className={styles.avatar}>
            {getInitials(profile.fullName)}
          </div>
          <div className={styles.identity}>
            <div className={styles.nameRow}>
              <h1 className={styles.userName}>{profile.fullName || 'StudyGen Scholar'}</h1>
              <span className={styles.roleBadge}>🎓 Verified Learner</span>
            </div>
            <p className={styles.userEmail}>{profile.email}</p>
            <div className={styles.metaRow}>
              <span className={styles.metaItem}>
                📅 Member since <strong>{profile.memberSince || '2026'}</strong>
              </span>
              <span className={styles.metaDot}>•</span>
              <span className={styles.metaItem}>
                🔥 <strong>{profile.currentStreak}</strong> Day Streak
              </span>
            </div>
          </div>
        </div>

        <div className={styles.headerRight}>
          <button
            className={styles.editProfileBtn}
            onClick={() => setIsEditModalOpen(true)}
          >
            <span>✏️ Edit Profile</span>
          </button>
        </div>
      </header>

      {/* 7-Metric Stat Cards Row */}
      <section aria-label="Learning Statistics" className={styles.statsRow}>
        {/* Metric 1: Streak */}
        <div className={styles.statCard}>
          <div className={styles.statTop}>
            <span className={styles.statIcon} style={{ background: '#fff7ed', color: '#ea580c' }}>
              🔥
            </span>
            <span className={styles.statLabel}>Current Streak</span>
          </div>
          <span className={styles.statValue}>
            {profile.currentStreak} <small className={styles.statUnit}>days</small>
          </span>
          <span className={styles.statCaption}>
            {isNewUser && profile.currentStreak === 0
              ? 'Start your streak today!'
              : 'Continuous daily study'}
          </span>
        </div>

        {/* Metric 2: Study Hours */}
        <div className={styles.statCard}>
          <div className={styles.statTop}>
            <span className={styles.statIcon} style={{ background: '#eff6ff', color: '#2563eb' }}>
              ⏱️
            </span>
            <span className={styles.statLabel}>Focused Study</span>
          </div>
          <span className={styles.statValue}>
            {profile.totalStudyHours} <small className={styles.statUnit}>hrs</small>
          </span>
          <span className={styles.statCaption}>
            {isNewUser && profile.totalStudyHours === 0
              ? 'Ready for session #1'
              : `${profile.totalStudyMinutes} total minutes`}
          </span>
        </div>

        {/* Metric 3: Active Roadmaps */}
        <div className={styles.statCard}>
          <div className={styles.statTop}>
            <span className={styles.statIcon} style={{ background: '#f5f3ff', color: '#7c3aed' }}>
              🗺️
            </span>
            <span className={styles.statLabel}>Active Roadmaps</span>
          </div>
          <span className={styles.statValue}>
            {profile.totalRoadmaps} <small className={styles.statUnit}>paths</small>
          </span>
          <span className={styles.statCaption}>
            {isNewUser && profile.totalRoadmaps === 0
              ? 'Generate your first path'
              : 'Learning tracks enrolled'}
          </span>
        </div>

        {/* Metric 4: Modules Completed */}
        <div className={styles.statCard}>
          <div className={styles.statTop}>
            <span className={styles.statIcon} style={{ background: '#ecfdf5', color: '#059669' }}>
              📚
            </span>
            <span className={styles.statLabel}>Modules Mastered</span>
          </div>
          <span className={styles.statValue}>
            {profile.completedModulesCount} <small className={styles.statUnit}>modules</small>
          </span>
          <span className={styles.statCaption}>
            {isNewUser && profile.completedModulesCount === 0
              ? 'Complete a module to start'
              : 'Verified competency mastery'}
          </span>
        </div>

        {/* Metric 5: Passed Tests */}
        <div className={styles.statCard}>
          <div className={styles.statTop}>
            <span className={styles.statIcon} style={{ background: '#fdf2f8', color: '#db2777' }}>
              🎯
            </span>
            <span className={styles.statLabel}>Passed Tests</span>
          </div>
          <span className={styles.statValue}>
            {profile.passedTestsCount} <small className={styles.statUnit}>passed</small>
          </span>
          <span className={styles.statCaption}>
            {isNewUser && profile.passedTestsCount === 0
              ? 'Take tests to verify skills'
              : 'Diagnostic & module retests'}
          </span>
        </div>

        {/* Metric 6: Mastery Velocity */}
        <div className={styles.statCard}>
          <div className={styles.statTop}>
            <span className={styles.statIcon} style={{ background: '#f0fdfa', color: '#0d9488' }}>
              ⚡
            </span>
            <span className={styles.statLabel}>Mastery Velocity</span>
          </div>
          <span className={styles.statValue}>
            {profile.skillVelocityModulesPerWeek} <small className={styles.statUnit}>/wk</small>
          </span>
          <span className={styles.statCaption}>
            {isNewUser && profile.skillVelocityModulesPerWeek === 0
              ? 'Build weekly momentum'
              : 'Modules completed / week'}
          </span>
        </div>

        {/* Metric 7: Badges Earned */}
        <div className={styles.statCard}>
          <div className={styles.statTop}>
            <span className={styles.statIcon} style={{ background: '#fffbeb', color: '#d97706' }}>
              🏆
            </span>
            <span className={styles.statLabel}>Badges Earned</span>
          </div>
          <span className={styles.statValue}>
            {profile.totalBadgesEarned} <small className={styles.statUnit}>/ 8</small>
          </span>
          <span className={styles.statCaption}>
            {isNewUser && profile.totalBadgesEarned === 0
              ? '8 milestones to unlock'
              : 'Platform achievements'}
          </span>
        </div>
      </section>

      {/* Middle Two-Column Grid: 12-Month Heatmap & Skill Matrix Radar */}
      <div className={styles.middleGrid}>
        {/* Left Column: 12-Month Heatmap */}
        <div className={styles.heatmapSection}>
          <div className={styles.sectionHeader}>
            <div>
              <h3 className={styles.sectionTitle}>12-Month Consistency & Study Activity</h3>
              <p className={styles.sectionSubtitle}>
                Every Pomodoro sprint, test completion, and AI chat logged over the last 365 days.
              </p>
            </div>
          </div>

          <div className={styles.heatmapWrapper}>
            <Heatmap
              data={heatmapData}
              days={365}
              currentStreak={heatmapStats?.currentStreak ?? profile?.currentStreak ?? 0}
              longestStreak={heatmapStats?.longestStreak ?? 0}
              totalActiveDays={heatmapStats?.totalActiveDays ?? 0}
              todayMinutes={heatmapStats?.todayMinutes ?? 0}
              embedded={true}
              showTitle={false}
            />
          </div>

          {/* Activity Type Legend Strip */}
          <div className={styles.activityTypeLegend}>
            <span className={styles.legendHeader}>Logged Activity Types:</span>
            <div className={styles.legendItems}>
              <span className={styles.typePill}>⏱️ Pomodoro Focus</span>
              <span className={styles.typePill}>🎯 Assessments & Retests</span>
              <span className={styles.typePill}>💬 AI Mentorship Chats</span>
              <span className={styles.typePill}>🏋️ Practice Spaced Reviews</span>
              <span className={styles.typePill}>📄 Document Processing</span>
            </div>
          </div>
        </div>

        {/* Right Column: Skill Matrix Radar Chart */}
        <div className={styles.radarSection}>
          <div className={styles.sectionHeader}>
            <div>
              <h3 className={styles.sectionTitle}>Skill Matrix & Domain Competency</h3>
              <p className={styles.sectionSubtitle}>
                Evaluated from completed modules and test performance across 6 domains.
              </p>
            </div>
          </div>

          <div className={styles.radarCardBody}>
            {renderSkillMatrixRadar()}
            <div className={styles.radarLegend}>
              <span className={styles.radarLegendDot}></span>
              <span>Active Competency Polygon (0% to 100% mastery scale)</span>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Section: Achievements & Badges Grid */}
      <section aria-label="Achievements & Badges" className={styles.badgesSection}>
        <div className={styles.badgesHeader}>
          <div>
            <h3 className={styles.sectionTitle}>Achievements & Mastery Badges</h3>
            <p className={styles.sectionSubtitle}>
              Unlock milestones by maintaining study streaks, mastering modules, and clocking deep focus.
            </p>
          </div>
          <span className={styles.badgesProgressPill}>
            {profile.totalBadgesEarned} of {BADGES_CATALOG.length} Unlocked
          </span>
        </div>

        <div className={styles.badgesGrid}>
          {BADGES_CATALOG.map((b) => {
            const earnedDate = earnedBadgeMap.get(b.name.toLowerCase().trim());
            const isEarned = !!earnedDate;

            return (
              <div
                key={b.name}
                className={`${styles.badgeCard} ${
                  isEarned ? styles.badgeEarned : styles.badgeLocked
                }`}
              >
                <div className={styles.badgeCardHeader}>
                  <span className={styles.badgeIcon}>{b.icon}</span>
                  <span
                    className={`${styles.badgeStatusChip} ${
                      isEarned ? styles.statusChipEarned : styles.statusChipLocked
                    }`}
                  >
                    {isEarned ? 'UNLOCKED' : 'LOCKED 🔒'}
                  </span>
                </div>

                <h4 className={styles.badgeName}>{b.name}</h4>
                <p className={styles.badgeDescription}>{b.description}</p>

                <div className={styles.badgeFooter}>
                  {isEarned ? (
                    <span className={styles.earnedDateText}>
                      ⭐ Earned {earnedDate}
                    </span>
                  ) : (
                    <span className={styles.howToEarnText}>
                      <strong>How to earn:</strong> {b.howToEarn}
                    </span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* =========================================================
          EDIT PROFILE MODAL
         ========================================================= */}
      {isEditModalOpen && (
        <div className={styles.modalBackdrop} onClick={() => setIsEditModalOpen(false)}>
          <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <div className={styles.modalTitleRow}>
                <span className={styles.modalIcon}>✏️</span>
                <h3 className={styles.modalTitle}>Edit Profile</h3>
              </div>
              <button
                className={styles.modalCloseBtn}
                onClick={() => setIsEditModalOpen(false)}
                aria-label="Close modal"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveProfile} className={styles.modalForm}>
              {/* Full Name Input */}
              <div className={styles.fieldGroup}>
                <label htmlFor="editFullName" className={styles.label}>
                  Full Name
                </label>
                <input
                  id="editFullName"
                  type="text"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  className={styles.input}
                  placeholder="Enter your full name"
                  required
                  autoFocus
                />
              </div>

              {/* Email (Read-only / Greyed out) */}
              <div className={styles.fieldGroup}>
                <label htmlFor="editEmail" className={styles.label}>
                  Email Address <span className={styles.readOnlyTag}>(Read-only)</span>
                </label>
                <input
                  id="editEmail"
                  type="email"
                  value={profile.email}
                  disabled
                  className={`${styles.input} ${styles.inputDisabled}`}
                />
                <span className={styles.fieldNote}>
                  ℹ️ Email address is linked directly to your authentication account and cannot be modified.
                </span>
              </div>

              {/* Modal Actions */}
              <div className={styles.modalFooter}>
                <button
                  type="button"
                  className={styles.cancelBtn}
                  onClick={() => setIsEditModalOpen(false)}
                  disabled={savingProfile}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className={styles.saveBtn}
                  disabled={savingProfile}
                >
                  {savingProfile ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
