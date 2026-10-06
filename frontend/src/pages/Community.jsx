import React, { useState, useEffect, useRef } from 'react';
import EmptyState from '../components/EmptyState';
import styles from './Community.module.css';

// Initial channels configuration
const CHANNELS_DATA = [
  {
    category: 'Community Hubs',
    channels: [
      { id: 'general', name: 'general', desc: 'General chatter, study milestones, and platform discussions', unread: 0 },
      { id: 'study-squads', name: 'study-squads', desc: 'Find study partners and Pomodoro accountability groups', unread: 2 },
      { id: 'showcase', name: 'showcase', desc: 'Share your completed roadmaps, drill streaks, and certificates', unread: 0 },
    ],
  },
  {
    category: 'Technical Channels',
    channels: [
      { id: 'frontend-dev', name: 'frontend-dev', desc: 'React, modern CSS, state management, and UI performance', unread: 3 },
      { id: 'backend-java', name: 'backend-java', desc: 'Spring Boot, Hibernate, microservices, and concurrency', unread: 0 },
      { id: 'system-design', name: 'system-design', desc: 'Scalability, distributed databases, caching, and event streaming', unread: 1 },
      { id: 'algorithms', name: 'algorithms', desc: 'Data structures, algorithm drills, and technical interview prep', unread: 0 },
    ],
  },
  {
    category: 'Assistance',
    channels: [
      { id: 'help-desk', name: 'help-desk', desc: 'Ask questions about StudyGen features and report feedback', unread: 0 },
    ],
  },
];

// Initial seeded posts
const INITIAL_POSTS = {
  general: [
    {
      id: 'p1',
      author: 'Sarah Chen',
      isOwn: false,
      role: 'Mentor',
      timestamp: '2 hours ago',
      content: 'Welcome to the new StudyGen Community! Feel free to introduce yourself, share what roadmaps you are currently tackling, and swap learning tips.',
      tag: 'General',
      likes: 12,
      liked: false,
      comments: [
        { id: 'c1', author: 'Alex Rivera', time: '1 hour ago', text: 'Excited to be here! Just completed the React Deep Dive roadmap.' },
        { id: 'c2', author: 'Priya Sharma', time: '45 mins ago', text: 'Same! The spaced repetition flashcard practice has been a game-changer.' },
      ],
    },
    {
      id: 'p2',
      author: 'Test User',
      isOwn: true,
      role: 'You',
      timestamp: '30 mins ago',
      content: 'Just reached a 7-day focus streak using the Study Room Pomodoro timer! Highly recommend setting 50/10 intervals for complex topics.',
      tag: 'Milestone',
      likes: 5,
      liked: true,
      comments: [],
    },
  ],
  'frontend-dev': [
    {
      id: 'p3',
      author: 'Marcus Vance',
      isOwn: false,
      role: 'Learner',
      timestamp: '3 hours ago',
      content: 'Anyone else loving CSS container queries? They make reusable widget cards so much cleaner without relying on viewport media queries.',
      tag: 'CSS',
      likes: 8,
      liked: false,
      comments: [
        { id: 'c3', author: 'Elena Rostova', time: '2 hours ago', text: 'Yes! Browser support is super solid across the board now too.' },
      ],
    },
  ],
  'backend-java': [
    {
      id: 'p4',
      author: 'Devin K.',
      isOwn: false,
      role: 'Learner',
      timestamp: '5 hours ago',
      content: 'Tip for Spring Security 6: Avoid relying on legacy WebSecurityConfigurerAdapter. Use SecurityFilterChain bean definitions for cleaner component isolation.',
      tag: 'Spring Boot',
      likes: 15,
      liked: false,
      comments: [],
    },
  ],
};

// Initial seeded chat messages
const INITIAL_CHAT = {
  general: [
    { id: 'm1', author: 'Sarah Chen', time: '10:32 AM', text: 'Good morning everyone! Happy studying today! ☕' },
    { id: 'm2', author: 'Alex Rivera', time: '10:35 AM', text: 'Starting a 2-hour session on distributed caching.' },
    { id: 'm3', author: 'Priya Sharma', time: '10:41 AM', text: 'Good luck Alex! Ping if you want to pair on Redis replication.' },
  ],
  'frontend-dev': [
    { id: 'm4', author: 'Marcus Vance', time: '11:15 AM', text: 'Anyone experimenting with React Server Components in production yet?' },
  ],
  'backend-java': [
    { id: 'm5', author: 'Devin K.', time: '09:40 AM', text: 'Spring Boot 3.3 virtual threads benchmarks are looking fantastic.' },
  ],
};

const Community = () => {
  // Feature flag state: default is false as per specification
  const [isEnabled, setIsEnabled] = useState(() => {
    return localStorage.getItem('studygen_community_enabled') === 'true';
  });

  const [activeChannelId, setActiveChannelId] = useState('general');
  const [viewMode, setViewMode] = useState('threads'); // 'threads' | 'chat'
  
  // WebSocket connection status indicator: 'connected' | 'connecting' | 'reconnecting'
  const [wsStatus, setWsStatus] = useState('connected');
  const wsRef = useRef(null);

  // Content state
  const [postsByChannel, setPostsByChannel] = useState(INITIAL_POSTS);
  const [chatByChannel, setChatByChannel] = useState(INITIAL_CHAT);

  // Discussion Thread Composer state
  const [postDraft, setPostDraft] = useState('');
  const [postTag, setPostTag] = useState('Discussion');
  const [rateLimitSeconds, setRateLimitSeconds] = useState(0);
  const [expandedComments, setExpandedComments] = useState({});
  const [commentDrafts, setCommentDrafts] = useState({});

  // Chat Composer state
  const [chatDraft, setChatDraft] = useState('');

  // Handle Feature Flag toggle
  const toggleFeatureFlag = () => {
    const nextState = !isEnabled;
    setIsEnabled(nextState);
    localStorage.setItem('studygen_community_enabled', String(nextState));
  };

  // Simulated WebSocket connection lifecycle with reconnection indicator
  useEffect(() => {
    if (!isEnabled) return;

    let reconnectTimer;
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const socketUrl = `${protocol}//${window.location.host}/ws/community`;

    try {
      const socket = new WebSocket(socketUrl);
      wsRef.current = socket;

      socket.onopen = () => {
        setWsStatus('connected');
      };

      socket.onerror = () => {
        // Mock fallback: show reconnecting then connected for scaffold demonstration
        setWsStatus('reconnecting');
        reconnectTimer = setTimeout(() => {
          setWsStatus('connected');
        }, 3500);
      };

      socket.onclose = () => {
        setWsStatus('connecting');
      };
    } catch {
      // In development environments without real WS server endpoint, simulate connected
      setWsStatus('connected');
    }

    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
      if (reconnectTimer) clearTimeout(reconnectTimer);
    };
  }, [isEnabled]);

  // Rate-limit countdown timer effect
  useEffect(() => {
    if (rateLimitSeconds <= 0) return;
    const interval = setInterval(() => {
      setRateLimitSeconds((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(interval);
  }, [rateLimitSeconds]);

  // Find active channel info
  const allChannels = CHANNELS_DATA.flatMap((g) => g.channels);
  const currentChannel = allChannels.find((c) => c.id === activeChannelId) || allChannels[0];

  const currentPosts = postsByChannel[activeChannelId] || [];
  const currentChats = chatByChannel[activeChannelId] || [];

  // Submit Post in Discussion Threads
  const handlePostSubmit = (e) => {
    e.preventDefault();
    if (!postDraft.trim()) return;

    if (rateLimitSeconds > 0) {
      return;
    }

    if (postDraft.length > 500) {
      alert('Post exceeds 500 character limit.');
      return;
    }

    const newPost = {
      id: `p-${Date.now()}`,
      author: 'Test User',
      isOwn: true,
      role: 'You',
      timestamp: 'Just now',
      content: postDraft.trim(),
      tag: postTag,
      likes: 0,
      liked: false,
      comments: [],
    };

    setPostsByChannel((prev) => ({
      ...prev,
      [activeChannelId]: [newPost, ...(prev[activeChannelId] || [])],
    }));

    setPostDraft('');
    // Trigger 15-second rate-limit cooldown
    setRateLimitSeconds(15);
  };

  // Delete own post
  const handleDeletePost = (postId) => {
    if (window.confirm('Are you sure you want to delete your post?')) {
      setPostsByChannel((prev) => ({
        ...prev,
        [activeChannelId]: (prev[activeChannelId] || []).filter((p) => p.id !== postId),
      }));
    }
  };

  // Report post
  const handleReportPost = (postId) => {
    alert('Thank you for your report. Our community moderation team has been notified.');
  };

  // Toggle post like
  const handleToggleLike = (postId) => {
    setPostsByChannel((prev) => ({
      ...prev,
      [activeChannelId]: (prev[activeChannelId] || []).map((p) => {
        if (p.id === postId) {
          return {
            ...p,
            liked: !p.liked,
            likes: p.liked ? p.likes - 1 : p.likes + 1,
          };
        }
        return p;
      }),
    }));
  };

  // Toggle comments expander
  const toggleComments = (postId) => {
    setExpandedComments((prev) => ({
      ...prev,
      [postId]: !prev[postId],
    }));
  };

  // Submit comment
  const handleAddComment = (postId) => {
    const text = (commentDrafts[postId] || '').trim();
    if (!text) return;

    const newComment = {
      id: `c-${Date.now()}`,
      author: 'Test User',
      time: 'Just now',
      text,
    };

    setPostsByChannel((prev) => ({
      ...prev,
      [activeChannelId]: (prev[activeChannelId] || []).map((p) => {
        if (p.id === postId) {
          return {
            ...p,
            comments: [...(p.comments || []), newComment],
          };
        }
        return p;
      }),
    }));

    setCommentDrafts((prev) => ({ ...prev, [postId]: '' }));
  };

  // Submit live chat message
  const handleChatSubmit = (e) => {
    e.preventDefault();
    if (!chatDraft.trim()) return;

    const newMsg = {
      id: `m-${Date.now()}`,
      author: 'Test User',
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      text: chatDraft.trim(),
    };

    setChatByChannel((prev) => ({
      ...prev,
      [activeChannelId]: [...(prev[activeChannelId] || []), newMsg],
    }));

    setChatDraft('');
  };

  return (
    <div className={styles.container}>
      {/* Dev / Admin Feature Flag Switcher */}
      <div className={styles.flagBar}>
        <div className={styles.flagBarLeft}>
          <span className={styles.flagBadge}>Phase 11 Feature Flag</span>
          <span>
            Community Status: <strong>{isEnabled ? 'ENABLED (Full Scaffold Active)' : 'DISABLED (Default Coming Soon)'}</strong>
          </span>
        </div>
        <button
          className={styles.flagToggleBtn}
          onClick={toggleFeatureFlag}
          title="Toggle between default Coming Soon empty state and enabled scaffold"
        >
          {isEnabled ? '🔒 Disable (Show Coming Soon)' : '⚡ Enable Feature (Preview Scaffold)'}
        </button>
      </div>

      {/* Feature Flag Disabled: Default "Coming Soon" Empty State */}
      {!isEnabled ? (
        <div className={styles.comingSoonWrapper}>
          <EmptyState
            icon="👥"
            title="Community Zone is Coming Soon"
            description="We're currently building peer study groups, real-time technical channels, and collaborative discussion threads to help you learn faster together. Stay tuned for Phase 11!"
            actionLabel="Preview Phase 11 Scaffold"
            onAction={toggleFeatureFlag}
          />
        </div>
      ) : (
        /* Feature Flag Enabled: Full Community Workspace */
        <div className={styles.communityLayout}>
          {/* 1. ChannelSidebar */}
          <aside className={styles.channelSidebar}>
            <div className={styles.sidebarHeader}>
              <h2 className={styles.sidebarTitle}>
                <span>👥</span> StudyGen Community
              </h2>
              <p className={styles.sidebarSubtitle}>Collaborate & learn with peers</p>
            </div>

            <div className={styles.channelList}>
              {CHANNELS_DATA.map((group) => (
                <div key={group.category} className={styles.categoryGroup}>
                  <h3 className={styles.categoryTitle}>{group.category}</h3>
                  {group.channels.map((chan) => {
                    const isActive = chan.id === activeChannelId;
                    return (
                      <button
                        key={chan.id}
                        className={`${styles.channelBtn} ${isActive ? styles.channelBtnActive : ''}`}
                        onClick={() => setActiveChannelId(chan.id)}
                      >
                        <span className={styles.channelBtnLeft}>
                          <span className={styles.channelHash}>#</span>
                          <span>{chan.name}</span>
                        </span>
                        {chan.unread > 0 && (
                          <span className={styles.unreadBadge}>{chan.unread}</span>
                        )}
                      </button>
                    );
                  })}
                </div>
              ))}
            </div>
          </aside>

          {/* 2. Main Workspace */}
          <main className={styles.mainWorkspace}>
            {/* Header with Channel Meta & WS Status */}
            <header className={styles.channelHeader}>
              <div className={styles.channelHeaderLeft}>
                <h1 className={styles.channelHeaderTitle}>
                  <span className={styles.channelHash}>#</span>
                  <span>{currentChannel.name}</span>
                </h1>
                <p className={styles.channelHeaderDesc}>{currentChannel.desc}</p>
              </div>

              <div className={styles.channelHeaderRight}>
                {/* View Switcher: Live Chat vs Discussion Threads */}
                <div className={styles.viewSwitchGroup}>
                  <button
                    className={`${styles.viewSwitchBtn} ${viewMode === 'threads' ? styles.viewSwitchBtnActive : ''}`}
                    onClick={() => setViewMode('threads')}
                  >
                    📝 Discussions
                  </button>
                  <button
                    className={`${styles.viewSwitchBtn} ${viewMode === 'chat' ? styles.viewSwitchBtnActive : ''}`}
                    onClick={() => setViewMode('chat')}
                  >
                    💬 Live Chat
                  </button>
                </div>

                {/* WebSocket Connection Status Indicator */}
                <div
                  className={`${styles.connectionStatus} ${
                    wsStatus === 'connected'
                      ? styles.statusConnected
                      : wsStatus === 'reconnecting'
                      ? styles.statusReconnecting
                      : styles.statusConnecting
                  }`}
                  title={`WebSocket Status: ${wsStatus}`}
                >
                  <span className={styles.statusDot} />
                  <span>
                    {wsStatus === 'connected'
                      ? 'Connected'
                      : wsStatus === 'reconnecting'
                      ? 'Reconnecting...'
                      : 'Connecting...'}
                  </span>
                </div>
              </div>
            </header>

            {/* Content Body */}
            <div className={styles.contentBody}>
              {/* Mode A: Live Chat Window */}
              {viewMode === 'chat' ? (
                <div className={styles.chatWindow}>
                  {currentChats.length === 0 ? (
                    <div className={styles.emptyChannelState}>
                      <span className={styles.emptyChannelIcon}>💬</span>
                      <h4 className={styles.emptyChannelTitle}>No messages in #{currentChannel.name} yet</h4>
                      <p className={styles.emptyChannelDesc}>
                        Say hello and kick off the conversation with your fellow learners!
                      </p>
                    </div>
                  ) : (
                    <div className={styles.chatMessages}>
                      {currentChats.map((msg) => (
                        <div key={msg.id} className={styles.chatMessageItem}>
                          <div className={styles.messageAvatar}>
                            {msg.author.split(' ').map((n) => n[0]).join('').slice(0, 2)}
                          </div>
                          <div className={styles.messageContent}>
                            <div className={styles.messageHeader}>
                              <span className={styles.messageAuthor}>{msg.author}</span>
                              <span className={styles.messageTimestamp}>{msg.time}</span>
                            </div>
                            <div className={styles.messageText}>{msg.text}</div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  <form className={styles.chatComposer} onSubmit={handleChatSubmit}>
                    <input
                      type="text"
                      className={styles.chatInput}
                      placeholder={`Send a message to #${currentChannel.name}...`}
                      value={chatDraft}
                      onChange={(e) => setChatDraft(e.target.value)}
                    />
                    <button
                      type="submit"
                      className={styles.sendBtn}
                      disabled={!chatDraft.trim() || wsStatus === 'reconnecting'}
                    >
                      Send
                    </button>
                  </form>
                </div>
              ) : (
                /* Mode B: Discussion Threads */
                <div className={styles.threadWindow}>
                  {/* Post Composer Card */}
                  <div className={styles.postComposerCard}>
                    <h3 className={styles.composerTitle}>Start a discussion in #{currentChannel.name}</h3>
                    <textarea
                      className={styles.postTextarea}
                      placeholder="Ask a technical question, share a roadmap insight, or organize a study session..."
                      value={postDraft}
                      maxLength={500}
                      onChange={(e) => setPostDraft(e.target.value)}
                    />

                    {/* Rate Limit Alert */}
                    {rateLimitSeconds > 0 && (
                      <div className={styles.rateLimitBanner}>
                        <span>⏱️</span>
                        <span>
                          <strong>Rate limit active:</strong> Please wait {rateLimitSeconds}s before submitting another post.
                        </span>
                      </div>
                    )}

                    <div className={styles.composerFooter}>
                      <div className={styles.counterGroup}>
                        <span
                          className={`${styles.charCounter} ${
                            postDraft.length > 450
                              ? styles.counterWarning
                              : postDraft.length >= 500
                              ? styles.counterError
                              : ''
                          }`}
                        >
                          {postDraft.length}/500 characters
                        </span>
                      </div>
                      <button
                        type="button"
                        className={styles.postSubmitBtn}
                        onClick={handlePostSubmit}
                        disabled={!postDraft.trim() || rateLimitSeconds > 0}
                      >
                        Publish Post
                      </button>
                    </div>
                  </div>

                  {/* Posts Feed or Empty State */}
                  {currentPosts.length === 0 ? (
                    <div className={styles.emptyChannelState}>
                      <span className={styles.emptyChannelIcon}>📭</span>
                      <h4 className={styles.emptyChannelTitle}>No discussions in #{currentChannel.name} yet</h4>
                      <p className={styles.emptyChannelDesc}>
                        Be the first to share an idea, pose a question, or kick off a peer study topic!
                      </p>
                    </div>
                  ) : (
                    <div className={styles.postsList}>
                      {currentPosts.map((post) => (
                        <article key={post.id} className={styles.postCard}>
                          {/* Post Header */}
                          <div className={styles.postCardHeader}>
                            <div className={styles.postAuthorMeta}>
                              <div className={styles.authorAvatar}>
                                {post.author.split(' ').map((n) => n[0]).join('').slice(0, 2)}
                              </div>
                              <div>
                                <div className={styles.authorName}>
                                  <span>{post.author}</span>
                                  <span className={styles.authorBadge}>{post.role}</span>
                                </div>
                                <span className={styles.postTimestamp}>{post.timestamp}</span>
                              </div>
                            </div>

                            {/* Actions: Delete Own / Report Others */}
                            <div className={styles.postCardActions}>
                              {post.isOwn ? (
                                <button
                                  className={`${styles.actionBtn} ${styles.deleteBtn}`}
                                  onClick={() => handleDeletePost(post.id)}
                                  title="Delete your post"
                                >
                                  🗑️ Delete
                                </button>
                              ) : (
                                <button
                                  className={styles.actionBtn}
                                  onClick={() => handleReportPost(post.id)}
                                  title="Report post to moderators"
                                >
                                  🚩 Report
                                </button>
                              )}
                            </div>
                          </div>

                          {/* Post Body */}
                          <div className={styles.postBody}>{post.content}</div>

                          {/* Tags */}
                          {post.tag && (
                            <div className={styles.postTags}>
                              <span className={styles.topicTag}>#{post.tag}</span>
                            </div>
                          )}

                          {/* Footer Actions */}
                          <div className={styles.postFooter}>
                            <button
                              className={`${styles.reactionBtn} ${post.liked ? styles.reactionBtnActive : ''}`}
                              onClick={() => handleToggleLike(post.id)}
                            >
                              <span>{post.liked ? '❤️' : '🤍'}</span>
                              <span>{post.likes}</span>
                            </button>

                            <button
                              className={styles.reactionBtn}
                              onClick={() => toggleComments(post.id)}
                            >
                              <span>💬</span>
                              <span>{post.comments?.length || 0} Comments</span>
                            </button>
                          </div>

                          {/* Comments Drawer */}
                          {expandedComments[post.id] && (
                            <div className={styles.commentsSection}>
                              {post.comments && post.comments.length > 0 ? (
                                post.comments.map((comment) => (
                                  <div key={comment.id} className={styles.commentItem}>
                                    <div className={styles.commentAuthorMeta}>
                                      <span className={styles.commentAuthor}>{comment.author}</span>
                                      <span className={styles.commentTime}>{comment.time}</span>
                                    </div>
                                    <p className={styles.commentText}>{comment.text}</p>
                                  </div>
                                ))
                              ) : (
                                <p style={{ fontSize: '0.8125rem', color: '#9ca3af', margin: '4px 0' }}>
                                  No comments yet. Be the first to reply!
                                </p>
                              )}

                              {/* Comment Composer */}
                              <div className={styles.commentComposer}>
                                <input
                                  type="text"
                                  className={styles.commentInput}
                                  placeholder="Write a constructive reply..."
                                  value={commentDrafts[post.id] || ''}
                                  onChange={(e) =>
                                    setCommentDrafts({
                                      ...commentDrafts,
                                      [post.id]: e.target.value,
                                    })
                                  }
                                  onKeyDown={(e) => {
                                    if (e.key === 'Enter') {
                                      e.preventDefault();
                                      handleAddComment(post.id);
                                    }
                                  }}
                                />
                                <button
                                  type="button"
                                  className={styles.commentSubmitBtn}
                                  onClick={() => handleAddComment(post.id)}
                                >
                                  Reply
                                </button>
                              </div>
                            </div>
                          )}
                        </article>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          </main>
        </div>
      )}
    </div>
  );
};

export default Community;
