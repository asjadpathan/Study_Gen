import React from 'react';
import { Link } from 'react-router-dom';
import logoImg from '../assets/images/logo.png';
import styles from './Footer.module.css';

const Footer = () => {
  return (
    <footer className={styles.footer}>
      <div className={styles.container}>
        <div className={styles.topSection}>
          <div className={styles.brandInfo}>
            <Link to="/" className={styles.logo} aria-label="StudyGen Home">
              <div className={styles.footerLogoWrapper}>
                <img src={logoImg} alt="StudyGen" className={styles.footerLogo} />
              </div>
            </Link>
            <p className={styles.tagline}>
              Accelerating human mastery with AI-driven personalized learning paths, diagnostic assessment, and interactive study rooms.
            </p>
          </div>

          <div className={styles.linksGrid}>
            <div className={styles.linkColumn}>
              <h4 className={styles.columnTitle}>Product</h4>
              <a href="#features" className={styles.link}>Features</a>
              <a href="#how-it-works" className={styles.link}>How It Works</a>
              <Link to="/auth?tab=signup" className={styles.link}>Get Started</Link>
            </div>

            <div className={styles.linkColumn}>
              <h4 className={styles.columnTitle}>Platform</h4>
              <span className={styles.staticLink}>AI Roadmaps</span>
              <span className={styles.staticLink}>Diagnostic Testing</span>
              <span className={styles.staticLink}>Document Analyzer</span>
              <span className={styles.staticLink}>Study Chatbot</span>
            </div>

            <div className={styles.linkColumn}>
              <h4 className={styles.columnTitle}>Company</h4>
              <span className={styles.staticLink}>About Us</span>
              <span className={styles.staticLink}>Careers</span>
              <span className={styles.staticLink}>Privacy Policy</span>
              <span className={styles.staticLink}>Terms of Service</span>
            </div>
          </div>
        </div>

        <div className={styles.bottomSection}>
          <p className={styles.copyright}>
            © {new Date().getFullYear()} StudyGen. All rights reserved. Powered by Advanced AI & Spring Boot.
          </p>
          <div className={styles.socials}>
            <span className={styles.socialIcon}>🌐</span>
            <span className={styles.socialIcon}>💻</span>
            <span className={styles.socialIcon}>🚀</span>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
