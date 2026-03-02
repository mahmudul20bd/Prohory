import React from 'react';
import { Container, Row, Col } from 'react-bootstrap';
import { FaGithub, FaTwitter, FaLinkedin, FaShieldAlt, FaLock, FaUserSecret } from 'react-icons/fa';
import { Link } from 'react-router-dom';

function Footer() {
  return (
    <footer style={{ 
      background: '#02060c', 
      borderTop: '1px solid rgba(0, 243, 255, 0.1)',
      paddingTop: '50px',
      marginTop: 'auto',
      position: 'relative',
      overflow: 'hidden'
    }}>
      
      {/* Background Grid Effect */}
      <div style={{
        position: 'absolute',
        top: 0, left: 0, width: '100%', height: '100%',
        backgroundImage: 'linear-gradient(rgba(0, 243, 255, 0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(0, 243, 255, 0.03) 1px, transparent 1px)',
        backgroundSize: '30px 30px',
        zIndex: 0
      }}></div>

      <Container style={{ position: 'relative', zIndex: 1 }}>
        <Row className="gy-4">
          {/* Brand Section */}
          <Col md={4}>
            <h4 style={{ fontFamily: 'Orbitron', letterSpacing: '2px', color: '#fff' }}>
              PRO<span style={{ color: '#00f3ff' }}>HORY</span>
            </h4>
            <p className="text-secondary small mt-3">
              Advanced AI-powered threat detection system designed to secure digital communication channels against phishing and social engineering attacks.
            </p>
          </Col>

          {/* Quick Links */}
          <Col md={4} className="d-flex flex-column gap-2">
            <h6 className="text-white mb-3" style={{fontFamily: 'Orbitron'}}>NAVIGATION</h6>
            <Link to="/" className="text-secondary text-decoration-none hover-link">Home Node</Link>
            <Link to="/register" className="text-secondary text-decoration-none hover-link">Register Unit</Link>
            <Link to="/login" className="text-secondary text-decoration-none hover-link">Member Access</Link>
          </Col>

          {/* Contact / Social */}
          <Col md={4}>
            <h6 className="text-white mb-3" style={{fontFamily: 'Orbitron'}}>CONNECT</h6>
            <div className="d-flex gap-3 mb-3">
              <a href="#" className="text-info fs-5"><FaGithub /></a>
              <a href="#" className="text-info fs-5"><FaTwitter /></a>
              <a href="#" className="text-info fs-5"><FaLinkedin /></a>
            </div>
            <small className="text-secondary">System Version: v2.1.0 (Beta)</small>
          </Col>
        </Row>

        {/* Footer Bottom Line */}
        <div className="d-flex justify-content-between align-items-center mt-5 py-3 border-top border-secondary border-opacity-25">
          <small className="text-secondary">
            &copy; {new Date().getFullYear()} CyberAware Intelligence. All Systems Operational.
          </small>

          {/* 🔥 ADMIN LOGIN LINK (Hidden Corner) */}
          <Link to="/login" className="text-decoration-none d-flex align-items-center gap-2" 
                style={{ color: '#333', fontSize: '0.8rem', transition: '0.3s' }}
                onMouseOver={(e) => e.target.style.color = '#ff003c'}
                onMouseOut={(e) => e.target.style.color = '#333'}
          >
            <FaUserSecret /> <span style={{fontWeight: 'bold'}}>ADMIN PORTAL</span>
          </Link>
        </div>
      </Container>
    </footer>
  );
}

export default Footer;