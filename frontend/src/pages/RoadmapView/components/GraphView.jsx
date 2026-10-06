import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import StatusPill from '../../../components/StatusPill';
import Button from '../../../components/Button';
import styles from './GraphView.module.css';

const GraphView = ({ graphData, modules = [], onRefresh }) => {
  const navigate = useNavigate();

  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [selectedNode, setSelectedNode] = useState(null);

  const nodes = graphData?.nodes || modules.map((m, idx) => ({
    id: m.id,
    label: m.title,
    status: m.statusBadge || (m.isCompleted ? 'COMPLETED' : m.isLocked ? 'LOCKED' : 'UNLOCKED'),
    difficulty: m.difficulty || 'BEGINNER',
    sequenceOrder: idx,
  }));

  const nodePositions = nodes.map((node, i) => {
    const x = 120 + (i % 2 === 0 ? 0 : 80);
    const y = 80 + i * 110;
    return { ...node, x, y };
  });

  const handleMouseDown = (e) => {
    if (e.target.tagName === 'svg' || e.target.tagName === 'g') {
      setIsDragging(true);
      setDragStart({ x: e.clientX - pan.x, y: e.clientY - pan.y });
    }
  };

  const handleMouseMove = (e) => {
    if (isDragging) {
      setPan({ x: e.clientX - dragStart.x, y: e.clientY - dragStart.y });
    }
  };

  const handleMouseUp = () => setIsDragging(false);

  const handleFitToScreen = () => {
    setZoom(1);
    setPan({ x: 0, y: 0 });
  };

  const handleNodeClick = (node) => {
    const matchedModule = modules.find((m) => m.id === node.id) || {
      id: node.id,
      title: node.label,
      description: 'Module detailed study topics and exercises.',
      statusBadge: node.status,
      difficulty: node.difficulty,
      skills: ['Concepts', 'Practice Drills'],
      prerequisites: node.sequenceOrder > 0 ? [`Module ${node.sequenceOrder}`] : [],
      bestScore: node.status === 'COMPLETED' ? 95 : null,
      attempts: node.status === 'COMPLETED' ? 1 : 0,
      weakConcepts: ['Edge cases'],
      sourceChips: ['Official Documentation', 'Interactive Sandbox'],
    };

    setSelectedNode(matchedModule);
  };

  return (
    <div className={styles.graphContainer}>
      {/* Top Floating Controls */}
      <div className={styles.controlsBar}>
        <button className={styles.controlBtn} onClick={() => setZoom((z) => Math.min(z + 0.2, 2))} title="Zoom In">
          ➕
        </button>
        <button className={styles.controlBtn} onClick={() => setZoom((z) => Math.max(z - 0.2, 0.5))} title="Zoom Out">
          ➖
        </button>
        <Button variant="outlined" size="sm" onClick={handleFitToScreen}>
          🎯 Fit to screen
        </Button>
      </div>

      {/* Interactive SVG Canvas */}
      <div
        className={styles.canvasArea}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
      >
        <svg
          className={styles.svgCanvas}
          width="100%"
          height="520px"
          style={{ cursor: isDragging ? 'grabbing' : 'grab' }}
        >
          <defs>
            <marker id="arrow" viewBox="0 0 10 10" refX="28" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
              <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--color-neutral-400)" />
            </marker>
          </defs>

          <g transform={`translate(${pan.x}, ${pan.y}) scale(${zoom})`}>
            {/* Edge Connecting Lines */}
            {nodePositions.slice(0, -1).map((curr, idx) => {
              const next = nodePositions[idx + 1];
              return (
                <line
                  key={`edge-${curr.id}-${next.id}`}
                  x1={curr.x}
                  y1={curr.y}
                  x2={next.x}
                  y2={next.y}
                  stroke="var(--color-neutral-300)"
                  strokeWidth="3"
                  strokeDasharray={next.status === 'LOCKED' ? '6 4' : 'none'}
                  markerEnd="url(#arrow)"
                />
              );
            })}

            {/* Nodes */}
            {nodePositions.map((node) => {
              const isCompleted = node.status === 'COMPLETED';
              const isLocked = node.status === 'LOCKED';

              let fillColor = 'var(--color-tertiary)';
              if (isCompleted) fillColor = 'var(--color-primary)';
              if (isLocked) fillColor = 'var(--color-neutral-300)';

              return (
                <g
                  key={node.id}
                  transform={`translate(${node.x}, ${node.y})`}
                  className={styles.nodeGroup}
                  onClick={() => handleNodeClick(node)}
                >
                  <circle
                    r="24"
                    fill={fillColor}
                    stroke="var(--color-surface)"
                    strokeWidth="3"
                    className={styles.nodeCircle}
                  />
                  <text
                    textAnchor="middle"
                    dy="5"
                    fill="#ffffff"
                    fontWeight="700"
                    fontSize="13px"
                    pointerEvents="none"
                  >
                    {isCompleted ? '✓' : isLocked ? '🔒' : node.sequenceOrder + 1}
                  </text>
                  <text
                    x="36"
                    y="5"
                    fill="var(--color-text)"
                    fontWeight="600"
                    fontSize="14px"
                    className={styles.nodeLabel}
                  >
                    {node.label}
                  </text>
                </g>
              );
            })}
          </g>
        </svg>
      </div>

      {/* Module Detail Slide-in Side Drawer */}
      {selectedNode && (
        <div className={styles.drawerOverlay} onClick={() => setSelectedNode(null)}>
          <aside className={styles.drawerContent} onClick={(e) => e.stopPropagation()}>
            <div className={styles.drawerHeader}>
              <div>
                <span className={styles.drawerSubtitle}>MODULE DETAIL</span>
                <h3 className={styles.drawerTitle}>{selectedNode.title}</h3>
              </div>
              <button className={styles.closeBtn} onClick={() => setSelectedNode(null)}>
                ✕
              </button>
            </div>

            <div className={styles.drawerBody}>
              <div className={styles.statusRow}>
                <StatusPill status={selectedNode.statusBadge || 'UNLOCKED'} />
                <span className={styles.diffBadge}>{selectedNode.difficulty || 'BEGINNER'}</span>
              </div>

              <div className={styles.sectionGroup}>
                <h4 className={styles.sectionLabel}>Description</h4>
                <p className={styles.sectionText}>{selectedNode.description}</p>
              </div>

              {selectedNode.skills && selectedNode.skills.length > 0 && (
                <div className={styles.sectionGroup}>
                  <h4 className={styles.sectionLabel}>Skills Targeted</h4>
                  <div className={styles.chipRow}>
                    {selectedNode.skills.map((s, idx) => (
                      <span key={idx} className={styles.skillTag}>
                        {s}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {selectedNode.prerequisites && selectedNode.prerequisites.length > 0 && (
                <div className={styles.sectionGroup}>
                  <h4 className={styles.sectionLabel}>Prerequisites</h4>
                  <p className={styles.sectionText}>{selectedNode.prerequisites.join(', ')}</p>
                </div>
              )}

              <div className={styles.statsGrid}>
                <div className={styles.statBox}>
                  <span className={styles.statBoxLabel}>Best Score</span>
                  <span className={styles.statBoxValue}>
                    {selectedNode.bestScore != null ? `${selectedNode.bestScore}%` : 'N/A'}
                  </span>
                </div>
                <div className={styles.statBox}>
                  <span className={styles.statBoxLabel}>Attempts</span>
                  <span className={styles.statBoxValue}>{selectedNode.attempts || 0}</span>
                </div>
              </div>

              {selectedNode.weakConcepts && selectedNode.weakConcepts.length > 0 && (
                <div className={styles.sectionGroup}>
                  <h4 className={styles.sectionLabel}>Weak Concepts</h4>
                  <div className={styles.chipRow}>
                    {selectedNode.weakConcepts.map((w, idx) => (
                      <span key={idx} className={styles.weakChip}>
                        ⚠️ {w}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {selectedNode.sourceChips && selectedNode.sourceChips.length > 0 && (
                <div className={styles.sectionGroup}>
                  <h4 className={styles.sectionLabel}>Sources & Materials</h4>
                  <div className={styles.chipRow}>
                    {selectedNode.sourceChips.map((sc, idx) => (
                      <span key={idx} className={styles.sourceChip}>
                        🔗 {sc}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className={styles.drawerFooter}>
              {selectedNode.isLocked || selectedNode.statusBadge === 'LOCKED' ? (
                <Button variant="outlined" disabled style={{ width: '100%' }}>
                  🔒 Module Locked
                </Button>
              ) : selectedNode.isCompleted || selectedNode.statusBadge === 'COMPLETED' ? (
                <Button
                  variant="outlined"
                  style={{ width: '100%' }}
                  onClick={() => navigate(`/modules/${selectedNode.id}`)}
                >
                  Review Module
                </Button>
              ) : (
                <Button
                  variant="primary"
                  style={{ width: '100%' }}
                  onClick={() => navigate(`/modules/${selectedNode.id}`)}
                >
                  Start Learning Module →
                </Button>
              )}
            </div>
          </aside>
        </div>
      )}
    </div>
  );
};

export default GraphView;
