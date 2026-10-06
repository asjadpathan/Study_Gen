import React, { useState, useEffect, useRef } from 'react';
import styles from './AmbientAudioPlayer.module.css';

const SOUND_TRACKS = [
  {
    id: 'rain',
    name: 'Rainy Cafe',
    icon: '🌧️',
    description: 'Gentle ambient rain against a window',
    url: 'https://cdn.pixabay.com/download/audio/2022/05/16/audio_db6591201e.mp3?filename=soft-rain-ambient-111154.mp3',
  },
  {
    id: 'lofi',
    name: 'Lo-Fi Chill Beats',
    icon: '🎧',
    description: 'Slow rhythmic beats for deep flow state',
    url: 'https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=lofi-study-112191.mp3',
  },
  {
    id: 'forest',
    name: 'Forest Nature & Birds',
    icon: '🌲',
    description: 'Breeze through tall trees and distant birds',
    url: 'https://cdn.pixabay.com/download/audio/2021/09/06/audio_8245781a8b.mp3?filename=forest-birds-chirping-nature-sound-8438.mp3',
  },
  {
    id: 'ocean',
    name: 'Calm Ocean Waves',
    icon: '🌊',
    description: 'Slow rolling tidal waves',
    url: 'https://cdn.pixabay.com/download/audio/2022/04/27/audio_3070498b95.mp3?filename=ocean-waves-ambient-110034.mp3',
  },
  {
    id: 'whitenoise',
    name: 'White Noise',
    icon: '💨',
    description: 'Constant frequency for masking distractions',
    isSynthesized: true, // Synthesized via Web Audio API so it plays offline without network!
  },
];

export default function AmbientAudioPlayer({ isFocusMode = false }) {
  const [selectedTrackId, setSelectedTrackId] = useState('rain');
  const [isPlaying, setIsPlaying] = useState(false);
  const [volume, setVolume] = useState(0.5);
  const [isMuted, setIsMuted] = useState(false);

  const audioRef = useRef(null);
  const audioContextRef = useRef(null);
  const noiseNodeRef = useRef(null);
  const gainNodeRef = useRef(null);

  const currentTrack = SOUND_TRACKS.find((t) => t.id === selectedTrackId) || SOUND_TRACKS[0];

  // Stop synthetic noise if playing
  const stopSyntheticNoise = () => {
    if (noiseNodeRef.current) {
      try {
        noiseNodeRef.current.stop();
        noiseNodeRef.current.disconnect();
      } catch {}
      noiseNodeRef.current = null;
    }
  };

  // Start synthetic pink/white noise via Web Audio API
  const startSyntheticNoise = () => {
    stopSyntheticNoise();
    try {
      if (!audioContextRef.current) {
        audioContextRef.current = new (window.AudioContext || window.webkitAudioContext)();
      }
      const ctx = audioContextRef.current;
      if (ctx.state === 'suspended') {
        ctx.resume();
      }

      const bufferSize = ctx.sampleRate * 2;
      const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
      const data = buffer.getChannelData(0);

      // Pink noise filter algorithm
      let b0 = 0, b1 = 0, b2 = 0, b3 = 0, b4 = 0, b5 = 0, b6 = 0;
      for (let i = 0; i < bufferSize; i++) {
        const white = Math.random() * 2 - 1;
        b0 = 0.99886 * b0 + white * 0.0555179;
        b1 = 0.99332 * b1 + white * 0.0750759;
        b2 = 0.96900 * b2 + white * 0.1538520;
        b3 = 0.86650 * b3 + white * 0.3104856;
        b4 = 0.55000 * b4 + white * 0.5329522;
        b5 = -0.7616 * b5 - white * 0.0168980;
        data[i] = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.11;
        b6 = white * 0.115926;
      }

      const noise = ctx.createBufferSource();
      noise.buffer = buffer;
      noise.loop = true;

      const gain = ctx.createGain();
      gain.gain.value = isMuted ? 0 : volume * 0.3;

      noise.connect(gain);
      gain.connect(ctx.destination);

      noise.start();
      noiseNodeRef.current = noise;
      gainNodeRef.current = gain;
    } catch (err) {
      console.warn('Web Audio synthesis not supported or failed', err);
    }
  };

  // Synchronize playback
  useEffect(() => {
    if (currentTrack.isSynthesized) {
      if (audioRef.current) {
        audioRef.current.pause();
      }
      if (isPlaying) {
        startSyntheticNoise();
      } else {
        stopSyntheticNoise();
      }
    } else {
      stopSyntheticNoise();
      if (audioRef.current) {
        audioRef.current.volume = isMuted ? 0 : volume;
        if (isPlaying) {
          audioRef.current.play().catch((err) => {
            console.warn('Audio playback error (e.g. autoplay blocked):', err);
            setIsPlaying(false);
          });
        } else {
          audioRef.current.pause();
        }
      }
    }

    return () => {
      stopSyntheticNoise();
    };
  }, [isPlaying, selectedTrackId]);

  // Volume / Mute change effect
  useEffect(() => {
    const effectiveVol = isMuted ? 0 : volume;
    if (audioRef.current) {
      audioRef.current.volume = effectiveVol;
    }
    if (gainNodeRef.current) {
      gainNodeRef.current.gain.value = effectiveVol * 0.3;
    }
  }, [volume, isMuted]);

  const handleTrackSelect = (trackId) => {
    setSelectedTrackId(trackId);
  };

  const togglePlay = () => {
    setIsPlaying(!isPlaying);
  };

  const toggleMute = () => {
    setIsMuted(!isMuted);
  };

  return (
    <div className={`${styles.playerCard} ${isFocusMode ? styles.focusModePlayer : ''}`}>
      {/* Hidden audio element for MP3 streams */}
      <audio
        ref={audioRef}
        src={currentTrack.isSynthesized ? undefined : currentTrack.url}
        loop
        preload="none"
      />

      <div className={styles.playerHeader}>
        <div className={styles.headerLeft}>
          <span className={styles.headerIcon}>🎵</span>
          <div>
            <h4 className={styles.headerTitle}>Ambient Soundscape</h4>
            <span className={styles.trackSubtitle}>{currentTrack.name}</span>
          </div>
        </div>
        <button
          className={`${styles.playBtn} ${isPlaying ? styles.playBtnActive : ''}`}
          onClick={togglePlay}
          title={isPlaying ? 'Pause ambient sound' : 'Play ambient sound'}
        >
          {isPlaying ? (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
              <rect x="6" y="4" width="4" height="16" rx="1"></rect>
              <rect x="14" y="4" width="4" height="16" rx="1"></rect>
            </svg>
          ) : (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
              <polygon points="5 3 19 12 5 21 5 3"></polygon>
            </svg>
          )}
        </button>
      </div>

      {/* Track Picker Grid */}
      <div className={styles.trackPicker}>
        {SOUND_TRACKS.map((t) => {
          const isSelected = t.id === selectedTrackId;
          return (
            <button
              key={t.id}
              className={`${styles.trackChip} ${isSelected ? styles.trackChipSelected : ''}`}
              onClick={() => handleTrackSelect(t.id)}
            >
              <span className={styles.trackEmoji}>{t.icon}</span>
              <span className={styles.trackName}>{t.name}</span>
              {isSelected && isPlaying && <span className={styles.playingWave}>•••</span>}
            </button>
          );
        })}
      </div>

      {/* Volume & Mute Section */}
      <div className={styles.volumeSection}>
        <button
          className={styles.muteBtn}
          onClick={toggleMute}
          title={isMuted ? 'Unmute' : 'Mute'}
        >
          {isMuted || volume === 0 ? (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
              <line x1="23" y1="9" x2="17" y2="15"></line>
              <line x1="17" y1="9" x2="23" y2="15"></line>
            </svg>
          ) : (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
              <path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>
            </svg>
          )}
        </button>

        <input
          type="range"
          min="0"
          max="1"
          step="0.01"
          value={isMuted ? 0 : volume}
          onChange={(e) => {
            setVolume(parseFloat(e.target.value));
            if (isMuted) setIsMuted(false);
          }}
          className={styles.volumeSlider}
          aria-label="Audio volume"
        />

        <span className={styles.volumePercent}>
          {isMuted ? '0%' : `${Math.round(volume * 100)}%`}
        </span>
      </div>

      {/* Attribution notice */}
      <div className={styles.attributionText}>
        <span>Royalty-free ambient audio licensed under Creative Commons & Web Audio synthesis.</span>
      </div>
    </div>
  );
}
