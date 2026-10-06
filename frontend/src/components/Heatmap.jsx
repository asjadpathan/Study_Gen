import React, { useState } from 'react';
import styles from './Heatmap.module.css';

const DAYS_OF_WEEK = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

const Heatmap = ({
  data = [],
  days = 60,
  currentStreak,
  longestStreak,
  totalActiveDays,
  todayMinutes,
  embedded = false,
  showTitle = true,
}) => {
  const [hoveredCell, setHoveredCell] = useState(null);
  const [tooltipPos, setTooltipPos] = useState({ x: 0, y: 0 });

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  // Build lookup map for fast date access
  const activityMap = new Map();
  if (Array.isArray(data)) {
    data.forEach((item) => {
      const dateStr = item.loggedDate || item.date;
      if (dateStr) {
        activityMap.set(dateStr, {
          activities: item.activityCount ?? item.activities ?? (item.minutesLogged ? 1 : 0),
          minutes: item.minutesLogged ?? item.minutes ?? 0,
          intensity: item.intensityLevel,
        });
      }
    });
  }

  // Calculate calendar grid ending on today
  const dayOfWeek = today.getDay(); // 0 = Sun, 1 = Mon ... 6 = Sat
  const numWeeks = Math.max(8, Math.ceil((days + dayOfWeek) / 7));
  const totalDays = numWeeks * 7;

  const startDate = new Date(today);
  startDate.setDate(today.getDate() - ((numWeeks - 1) * 7 + dayOfWeek));

  const columns = [];
  let computedActiveDays = 0;
  let computedTotalMinutes = 0;
  let computedTodayMins = 0;

  for (let w = 0; w < numWeeks; w++) {
    const weekCells = [];
    let weekMonth = null;

    for (let d = 0; d < 7; d++) {
      const cellDate = new Date(startDate);
      cellDate.setDate(startDate.getDate() + (w * 7 + d));
      const dateStr = cellDate.toISOString().split('T')[0];
      const isFuture = cellDate > today;
      const isToday = cellDate.getTime() === today.getTime();

      if (cellDate.getDate() === 1 || (w === 0 && d === 0)) {
        weekMonth = cellDate.toLocaleString('default', { month: 'short' });
      }

      if (isFuture) {
        weekCells.push({
          date: dateStr,
          isFuture: true,
          level: 0,
          activities: 0,
          minutes: 0,
        });
        continue;
      }

      const entry = activityMap.get(dateStr) || { activities: 0, minutes: 0, intensity: undefined };

      let level = entry.intensity;
      if (level === undefined || level === null) {
        if (entry.minutes >= 60) level = 4;
        else if (entry.minutes >= 35) level = 3;
        else if (entry.minutes >= 15) level = 2;
        else if (entry.minutes > 0 || entry.activities > 0) level = 1;
        else level = 0;
      }

      if (entry.minutes > 0 || entry.activities > 0) {
        computedActiveDays++;
        computedTotalMinutes += entry.minutes;
      }

      if (isToday) {
        computedTodayMins = entry.minutes;
      }

      weekCells.push({
        date: dateStr,
        dateObj: cellDate,
        isFuture: false,
        isToday,
        level: Math.min(4, Math.max(0, level)),
        activities: entry.activities,
        minutes: entry.minutes,
      });
    }

    columns.push({
      weekIndex: w,
      monthLabel: weekMonth,
      cells: weekCells,
    });
  }

  // Display metrics
  const displayStreak = currentStreak !== undefined ? currentStreak : 0;
  const displayActiveDays = totalActiveDays !== undefined ? totalActiveDays : computedActiveDays;
  const displayTodayMins = todayMinutes !== undefined ? todayMinutes : computedTodayMins;
  const activeToday = displayTodayMins > 0;
  const displayHours = (computedTotalMinutes / 60).toFixed(1);

  const handleMouseEnter = (cell, e) => {
    if (cell.isFuture) return;
    const rect = e.currentTarget.getBoundingClientRect();
    setTooltipPos({
      x: rect.left + rect.width / 2,
      y: rect.top - 8,
    });
    setHoveredCell(cell);
  };

  const handleMouseLeave = () => {
    setHoveredCell(null);
  };

  return (
    <div className={`${styles.heatmapCard} ${embedded ? styles.embeddedCard : ''}`}>
      {/* Top Header with Title and Status Badges */}
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          {showTitle && (
            <div className={styles.titleRow}>
              <span className={styles.icon}>🔥</span>
              <h3 className={styles.title}>Consistency & Study Activity</h3>
            </div>
          )}

          <div className={styles.statusPills}>
            <div
              className={`${styles.statusPill} ${
                activeToday ? styles.statusActive : styles.statusInactive
              }`}
            >
              <span className={styles.pulseDot}></span>
              <span>{activeToday ? `Active Today (${displayTodayMins}m logged)` : 'No study logged today'}</span>
            </div>

            <div className={styles.metricPill} title="Current uninterrupted study streak">
              <span>⚡ {displayStreak}d streak</span>
            </div>

            <div className={styles.metricPill} title="Total active days recorded">
              <span>📅 {displayActiveDays} active days</span>
            </div>

            {displayHours > 0 && (
              <div className={styles.metricPill} title="Total study hours in this period">
                <span>⏱️ {displayHours}h focus</span>
              </div>
            )}
          </div>
        </div>

        {/* Legend */}
        <div className={styles.legend}>
          <span className={styles.legendLabel}>Less</span>
          <span className={`${styles.square} ${styles.level0}`} />
          <span className={`${styles.square} ${styles.level1}`} />
          <span className={`${styles.square} ${styles.level2}`} />
          <span className={`${styles.square} ${styles.level3}`} />
          <span className={`${styles.square} ${styles.level4}`} />
          <span className={styles.legendLabel}>More</span>
        </div>
      </div>

      {/* Week-Aligned Calendar Grid Container */}
      <div className={styles.scrollWrapper}>
        <div className={styles.calendarContainer}>
          {/* Weekday Labels (Mon, Wed, Fri) */}
          <div className={styles.weekdayLabels}>
            <span className={styles.weekdayLabel} style={{ gridRow: 2 }}>Mon</span>
            <span className={styles.weekdayLabel} style={{ gridRow: 4 }}>Wed</span>
            <span className={styles.weekdayLabel} style={{ gridRow: 6 }}>Fri</span>
          </div>

          {/* Columns of weeks */}
          <div className={styles.columnsWrapper}>
            {/* Month Labels Bar */}
            <div className={styles.monthHeaderRow}>
              {columns.map((col, idx) => (
                <div key={idx} className={styles.monthHeaderCell}>
                  {col.monthLabel && <span className={styles.monthLabel}>{col.monthLabel}</span>}
                </div>
              ))}
            </div>

            {/* Weeks Grid */}
            <div className={styles.weeksGrid}>
              {columns.map((col, colIdx) => (
                <div key={colIdx} className={styles.weekCol}>
                  {col.cells.map((cell, rowIdx) => (
                    <div
                      key={rowIdx}
                      className={`${styles.square} ${
                        cell.isFuture
                          ? styles.squareHidden
                          : `${styles[`level${cell.level}`]} ${cell.isToday ? styles.squareToday : ''}`
                      }`}
                      onMouseEnter={(e) => handleMouseEnter(cell, e)}
                      onMouseLeave={handleMouseLeave}
                    />
                  ))}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Floating Tooltip */}
      {hoveredCell && (
        <div
          className={styles.tooltip}
          style={{
            left: `${tooltipPos.x}px`,
            top: `${tooltipPos.y}px`,
          }}
        >
          <div className={styles.tooltipDate}>
            {hoveredCell.dateObj
              ? hoveredCell.dateObj.toLocaleDateString(undefined, {
                  weekday: 'short',
                  month: 'short',
                  day: 'numeric',
                  year: 'numeric',
                })
              : hoveredCell.date}
          </div>
          <div className={styles.tooltipBody}>
            {hoveredCell.minutes > 0 ? (
              <span>
                <strong>{hoveredCell.minutes} minutes</strong> logged ({hoveredCell.activities || 1} session
                {(hoveredCell.activities || 1) > 1 ? 's' : ''})
              </span>
            ) : (
              <span style={{ color: '#94a3b8' }}>No study logged</span>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default Heatmap;
