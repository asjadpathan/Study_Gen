import './Footer.css'

const Footer = () => {
  return (
    <footer className="footer">
      <div className="footer__container">
        {/* Top: Brand + Link Columns */}
        <div className="footer__top">
          <div className="footer__brand">
            <span className="footer__logo">EduAI</span>
            <p className="footer__tagline">
              Empowering the next generation of learners through artificial intelligence and personalized growth maps.
            </p>
            <p className="footer__copy">&copy; 2024 EduAI Personalized Learning. All rights reserved.</p>
          </div>

          <div className="footer__columns">
            <div className="footer__column">
              <h4>Product</h4>
              <a href="#about">About</a>
              <a href="#careers">Careers</a>
              <a href="#blog">Blog</a>
            </div>
            <div className="footer__column">
              <h4>Resources</h4>
              <a href="#api">API</a>
              <a href="#guides">Guides</a>
              <a href="#help">Help Center</a>
            </div>
            <div className="footer__column">
              <h4>Legal</h4>
              <a href="#privacy">Privacy</a>
              <a href="#terms">Terms</a>
            </div>
            <div className="footer__column">
              <h4>Social</h4>
              <a href="#twitter">Twitter</a>
              <a href="#linkedin">LinkedIn</a>
              <a href="#discord">Discord</a>
            </div>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="footer__bottom">
          <div className="footer__bottom-bar" />
        </div>
      </div>
    </footer>
  )
}

export default Footer
