import React from 'react';
import { Container, Row, Col } from 'react-bootstrap';
import { FaShieldAlt, FaTwitter, FaLinkedin, FaGithub, FaEnvelope, FaPhone, FaMapMarkerAlt } from 'react-icons/fa';
import { Link } from 'react-router-dom';

const Footer = () => {
  return (
    <footer style={{ background: '#020c1b', borderTop: '1px solid var(--glass-border)', paddingTop: '80px', paddingBottom: '30px' }}>
      <Container>
        <Row className="g-5">
          <Col lg={4}>
            <div className="d-flex align-items-center gap-2 mb-4">
                <FaShieldAlt size={32} color="var(--neon-cyan)" />
                <h3 className="font-tech m-0">CYBER<span className="text-cyan">AWARE</span></h3>
            </div>
            <p className="text-secondary">Advanced AI-driven security platform protecting users from digital threats, scams, and phishing attacks in real-time.</p>
            <div className="d-flex gap-3 mt-4">
                {[FaTwitter, FaLinkedin, FaGithub].map((Icon, idx) => (
                    <motion.a whileHover={{y: -5, color: 'var(--neon-cyan)'}} key={idx} href="#" className="text-secondary" style={{fontSize: '1.5rem', transition: '0.3s'}}>
                        <Icon />
                    </motion.a>
                ))}
            </div>
          </Col>
          
          <Col lg={2} md={6}>
            <h5 className="text-white font-tech mb-4">QUICK LINKS</h5>
            <ul className="list-unstyled d-flex flex-column gap-3">
                {['Home', 'About Us', 'Services', 'News & Blog', 'Contact'].map(item => (
                    <li key={item}><Link to="#" className="text-secondary text-decoration-none hover-cyan">{item}</Link></li>
                ))}
            </ul>
          </Col>
          
          <Col lg={3} md={6}>
             <h5 className="text-white font-tech mb-4">SERVICES</h5>
              <ul className="list-unstyled d-flex flex-column gap-3">
                  <li><span className="text-secondary">AI Scam Scanning</span></li>
                  <li><span className="text-secondary">Expert Consultation</span></li>
                  <li><span className="text-secondary">Live Threat Monitoring</span></li>
                  <li><span className="text-secondary">Android Security</span></li>
              </ul>
          </Col>

          <Col lg={3}>
            <h5 className="text-white font-tech mb-4">CONTACT INFO</h5>
            <ul className="list-unstyled d-flex flex-column gap-3 text-secondary">
                <li className="d-flex gap-3"><FaMapMarkerAlt color="var(--neon-cyan)" /> 123 Cyber Street, Tech City</li>
                <li className="d-flex gap-3"><FaPhone color="var(--neon-cyan)" /> +880 1234 567890</li>
                <li className="d-flex gap-3"><FaEnvelope color="var(--neon-cyan)" /> support@cyberaware.com</li>
            </ul>
          </Col>
        </Row>
        
        <Row className="mt-5 pt-4 border-top border-secondary" style={{borderColor: 'rgba(255,255,255,0.1) !important'}}>
            <Col className="text-center text-secondary">
                <small>&copy; {new Date().getFullYear()} CyberAware. All rights reserved. Built for the future.</small>
            </Col>
        </Row>
      </Container>
    </footer>
  );
};
// Helper for framer motion in footer just in case
import { motion } from 'framer-motion';
export default Footer;