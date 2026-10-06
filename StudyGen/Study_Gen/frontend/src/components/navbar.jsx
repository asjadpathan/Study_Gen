import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Button from './Button';
import logoImg from '../assets/images/logo.png';
import styles from './Navbar.module.css';

const Navbar = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const navigate = useNavigate();

  const handleNavClick = (id) => {
    setMobileMenuOpen(false);
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <header className={styles.navbarHeader}>
      <div className={styles.container}>
        {/* Logo */}
        <Link to="/" className={styles.logo} aria-label="StudyGen Home">
          <img src={logoImg} alt="StudyGen" className={styles.navbarLogo} />
        </Link>

        {/* Desktop Navigation Links */}
        <nav className={styles.navLinks}>
          <button
            type="button"
            className={styles.navLink}
            onClick={() => handleNavClick('features')}
          >
            Features
          </button>
          <button
            type="button"
            className={styles.navLink}
            onClick={() => handleNavClick('how-it-works')}
          >
            How it works
          </button>
        </nav>

        {/* Action Buttons */}
        <div className={styles.authButtons}>
          <Button
            variant="outlined"
            size="sm"
            onClick={() => navigate('/auth?tab=login')}
          >
            Log in
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={() => navigate('/auth?tab=signup')}
          >
            Sign up
          </Button>
        </div>

        {/* Mobile Hamburger Toggle */}
        <button
          className={styles.hamburger}
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          aria-label="Toggle navigation menu"
        >
          <span className={`${styles.bar} ${mobileMenuOpen ? styles.open : ''}`}></span>
          <span className={`${styles.bar} ${mobileMenuOpen ? styles.open : ''}`}></span>
          <span className={`${styles.bar} ${mobileMenuOpen ? styles.open : ''}`}></span>
        </button>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className={styles.mobileDrawer}>
          <button
            type="button"
            className={styles.mobileNavLink}
            onClick={() => handleNavClick('features')}
          >
            Features
          </button>
          <button
            type="button"
            className={styles.mobileNavLink}
            onClick={() => handleNavClick('how-it-works')}
          >
            How it works
          </button>
          <div className={styles.mobileAuthButtons}>
            <Button
              variant="outlined"
              size="md"
              onClick={() => {
                setMobileMenuOpen(false);
                navigate('/auth?tab=login');
              }}
            >
              Log in
            </Button>
            <Button
              variant="primary"
              size="md"
              onClick={() => {
                setMobileMenuOpen(false);
                navigate('/auth?tab=signup');
              }}
            >
              Sign up
            </Button>
          </div>
        </div>
      )}
    </header>
  );
};

export default Navbar;
