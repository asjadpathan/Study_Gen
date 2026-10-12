import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import styles from './Heatmap.module.css';

// Format a date object to 'YYYY-MM-DD' using local timezone
const formatLocalDate = (d) => {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const Heatmap = ({
  data = [],
  days = 60,
  currentStreak,
  longestStreak: _longestStreak,
  totalActiveDays,
  todayMinutes,
  embedded = false,
  showTitle = true,
}) => {
  const [hoveredCell, setHoveredCell] = useState(null);
  const [tooltipPos, setTooltipPos] = useState({ x: 0, y: 0 });
  const [scrollPosition, setScrollPosition] = useState('end'); // 'start' | 'middle' | 'end' | 'all'
  const [viewMode, setViewMode] = useState('6m'); // '6m' | '12m'

  const scrollWrapperRef = useRef(null);

  const today = useMemo(() => {
    const t = new Date();
    t.setHours(0, 0, 0, 0);
    return t;
  }, []);

  // Build lookup map for fast date access
  const activityMap = useMemo(() => {
    const map = new Map();
    if (Array.isArray(data)) {
      data.forEach((item) => {
        const dateStr = item.loggedDate || item.date;
        if (dateStr) {
          const mins = item.minutesLogged ?? item.totalMinutes ?? item.minutes ?? 0;
          const acts = item.activityCount ?? item.count ?? item.activities ?? (mins > 0 ? 1 : 0);
          map.set(dateStr, {
            activities: acts,
            minutes: mins,
            intensity: item.intensityLevel,
          });
        }
      });
    }
    return map;
  }, [data]);

  // Generate month-divided calendar data
  // Each month starts on a new column/line with its own group of weeks
  const { monthsData, computedStats } = useMemo(() => {
    const numMonths = viewMode === '6m' ? 6 : 12;
    const currentYear = today.getFullYear();
    const currentMonthIdx = today.getMonth(); // 0 = Jan, 11 = Dec

    let activeDaysCount = 0;
    let totalMinutesCount = 0;
    let todayMinsCount = 0;

    const months = [];

    for (let i = numMonths - 1; i >= 0; i--) {
      let targetMonthIdx = currentMonthIdx - i;
      let targetYear = currentYear;
      while (targetMonthIdx < 0) {
        targetMonthIdx += 12;
        targetYear -= 1;
      }

      const firstOfMonth = new Date(targetYear, targetMonthIdx, 1);
      const monthName = firstOfMonth.toLocaleString('default', { month: 'short' });
      const fullMonthName = firstOfMonth.toLocaleString('default', { month: 'long', year: 'numeric' });
      const firstDayOfWeek = firstOfMonth.getDay(); // 0 = Sun, 1 = Mon ... 6 = Sat
      const daysInMonth = new Date(targetYear, targetMonthIdx + 1, 0).getDate();

      const weeks = [];
      let currentDay = 1;
      let weekIdx = 0;

      while (currentDay <= daysInMonth) {
        const weekCells = [];
        for (let d = 0; d < 7; d++) {
          // Pre-month padding in first week
          if (weekIdx === 0 && d < firstDayOfWeek) {
            weekCells.push({
              isPlaceholder: true,
              level: 0,
            });
            continue;
          }

          // Post-month padding in final week
          if (currentDay > daysInMonth) {
            weekCells.push({
              isPlaceholder: true,
              level: 0,
            });
            continue;
          }

          // Real date within this month
          const cellDate = new Date(targetYear, targetMonthIdx, currentDay);
          cellDate.setHours(0, 0, 0, 0);
          const dateStr = formatLocalDate(cellDate);
          const isFuture = cellDate.getTime() > today.getTime();
          const isToday = cellDate.getTime() === today.getTime();

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
            activeDaysCount++;
            totalMinutesCount += entry.minutes;
          }

          if (isToday) {
            todayMinsCount = entry.minutes;
          }

          weekCells.push({
            date: dateStr,
            dateObj: cellDate,
            dayNumber: currentDay,
            isPlaceholder: false,
            isFuture,
            isToday,
            level: Math.min(4, Math.max(0, level)),
            activities: entry.activities,
            minutes: entry.minutes,
          });

          currentDay++;
        }

        weeks.push({
          weekIndex: weekIdx,
          cells: weekCells,
        });
        weekIdx++;
      }

      months.push({
        key: `${targetYear}-${targetMonthIdx}`,
        year: targetYear,
        monthIndex: targetMonthIdx,
        name: monthName,
        fullName: fullMonthName,
        weeks,
      });
    }

    return {
      monthsData: months,
      computedStats: {
        activeDays: activeDaysCount,
        totalMinutes: totalMinutesCount,
        todayMins: todayMinsCount,
      },
    };
  }, [today, activityMap, viewMode]);

  // Display metrics
  const displayStreak = currentStreak !== undefined ? currentStreak : 0;
  const displayActiveDays = totalActiveDays !== undefined ? totalActiveDays : computedStats.activeDays;
  const displayTodayMins = todayMinutes !== undefined ? todayMinutes : computedStats.todayMins;
  const activeToday = displayTodayMins > 0;
  const displayHours = (computedStats.totalMinutes / 60).toFixed(1);

  // Scroll position updater
  const updateScrollState = useCallback(() => {
    if (!scrollWrapperRef.current) return;
    const { scrollLeft, scrollWidth, clientWidth } = scrollWrapperRef.current;
    const maxScroll = scrollWidth - clientWidth;
    if (maxScroll <= 15) {
      setScrollPosition('all');
    } else if (scrollLeft <= 25) {
      setScrollPosition('start');
    } else if (scrollLeft >= maxScroll - 25) {
      setScrollPosition('end');
    } else {
      setScrollPosition('middle');
    }
  }, []);

  // Auto-scroll to today on viewMode change and initial mount
  useEffect(() => {
    const scrollToToday = () => {
      if (scrollWrapperRef.current) {
        const el = scrollWrapperRef.current;
        el.scrollLeft = el.scrollWidth - el.clientWidth;
        updateScrollState();
      }
    };

    scrollToToday();
    const timer = setTimeout(scrollToToday, 60);
    return () => clearTimeout(timer);
  }, [monthsData.length, viewMode, updateScrollState]);

  const handleScroll = () => {
    if (hoveredCell) setHoveredCell(null);
    updateScrollState();
  };

  const handleScrollLeft = () => {
    if (scrollWrapperRef.current) {
      scrollWrapperRef.current.scrollBy({ left: -320, behavior: 'smooth' });
    }
  };

  const handleScrollToToday = () => {
    if (scrollWrapperRef.current) {
      const el = scrollWrapperRef.current;
      el.scrollTo({ left: el.scrollWidth - el.clientWidth, behavior: 'smooth' });
    }
  };

  const handleMouseEnter = (cell, e) => {
    if (cell.isPlaceholder || cell.isFuture) return;
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

  const isViewingToday = scrollPosition === 'end';
  const canScrollLeft = scrollPosition !== 'start' && scrollPosition !== 'all';

  return (
    <div className={`${styles.heatmapCard} ${embedded ? styles.embeddedCard : ''}`}>
      {/* Top Header with Title, Metrics, and Mode Toggles */}
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

            {parseFloat(displayHours) > 0 && (
              <div className={styles.metricPill} title="Total study hours in this period">
                <span>⏱️ {displayHours}h focus</span>
              </div>
            )}
          </div>
        </div>

        <div className={styles.headerRight}>
          {/* View Mode Toggle (6M vs Full Year) */}
          <div className={styles.viewToggleGroup} title="Toggle between 6-month view and full year">
            <button
              type="button"
              className={`${styles.viewToggleBtn} ${viewMode === '6m' ? styles.viewToggleActive : ''}`}
              onClick={() => setViewMode('6m')}
            >
              6 Months
            </button>
            <button
              type="button"
              className={`${styles.viewToggleBtn} ${viewMode === '12m' ? styles.viewToggleActive : ''}`}
              onClick={() => setViewMode('12m')}
            >
              Full Year
            </button>
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
      </div>

      {/* Calendar Viewport with Navigation Bar */}
      <div className={styles.calendarViewportContainer}>
        <div className={styles.navBar}>
          <div className={styles.rangeIndicator}>
            <span className={`${styles.rangeDot} ${isViewingToday ? styles.rangeDotLive : ''}`} />
            <span className={styles.rangeText}>
              {viewMode === '6m' ? 'Showing: 6 Months Activity by Month' : 'Showing: 12 Months Activity by Month'}
            </span>
          </div>

          <div className={styles.navControls}>
            <button
              type="button"
              className={`${styles.scrollBtn} ${!canScrollLeft ? styles.scrollBtnDisabled : ''}`}
              onClick={handleScrollLeft}
              disabled={!canScrollLeft}
              title="Scroll left to see earlier months"
            >
              ◀ Earlier
            </button>

            <button
              type="button"
              className={`${styles.scrollBtn} ${styles.todayBtn} ${isViewingToday ? styles.todayBtnCurrent : ''}`}
              onClick={handleScrollToToday}
              title="Scroll back to current month (today)"
            >
              {isViewingToday ? 'Today ✓' : 'Today ▶'}
            </button>
          </div>
        </div>

        {/* Month-Grouped Calendar Grid */}
        <div
          ref={scrollWrapperRef}
          className={`${styles.scrollWrapper} ${viewMode === '6m' ? styles.sixMonthView : ''}`}
          onScroll={handleScroll}
        >
          <div className={styles.calendarContainer}>
            {/* Weekday Labels (Mon, Wed, Fri) aligned to 7 rows */}
            <div className={styles.weekdayLabels}>
              <span className={styles.weekdayLabel} style={{ gridRow: 2 }}>Mon</span>
              <span className={styles.weekdayLabel} style={{ gridRow: 4 }}>Wed</span>
              <span className={styles.weekdayLabel} style={{ gridRow: 6 }}>Fri</span>
            </div>

            {/* Months Row: each month has its own group with a gap and label at bottom */}
            <div className={styles.monthsWrapper}>
              {monthsData.map((month) => (
                <div key={month.key} className={styles.monthGroup}>
                  {/* Weeks grid for this month */}
                  <div className={styles.monthWeeksGrid}>
                    {month.weeks.map((week, wIdx) => (
                      <div key={wIdx} className={styles.weekCol}>
                        {week.cells.map((cell, rIdx) => (
                          <div
                            key={rIdx}
                            className={`${styles.square} ${
                              cell.isPlaceholder
                                ? styles.squarePlaceholder
                                : cell.isFuture
                                ? styles.squareFuture
                                : `${styles[`level${cell.level}`]} ${cell.isToday ? styles.squareToday : ''}`
                            }`}
                            onMouseEnter={(e) => handleMouseEnter(cell, e)}
                            onMouseLeave={handleMouseLeave}
                          />
                        ))}
                      </div>
                    ))}
                  </div>

                  {/* Month Label below the columns */}
                  <div className={styles.monthFooter}>
                    <span className={styles.monthLabelText} title={month.fullName}>
                      {month.name}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Floating Interactive Tooltip */}
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
              <span style={{ color: '#94a3b8' }}>No study logged on this day</span>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default Heatmap;
