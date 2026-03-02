import React, { useState, useEffect } from 'react';
import { Navbar, Nav, Container, Button } from 'react-bootstrap';
import { Link, useLocation } from 'react-router-dom';

// --- FIXED UNIFIED LOGO COMPONENT ---
const CyberLogo = () => (
  <svg width="45" height="45" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
    {/* Shield Outer */}
    <path d="M50 5L90 25V55C90 75 50 95 50 95C50 95 10 75 10 55V25L50 5Z" stroke="#00f0ff" strokeWidth="4" fill="rgba(0, 240, 255, 0.15)"/>
    {/* Inner Tech Details */}
    <path d="M50 25V45" stroke="#00f0ff" strokeWidth="3" strokeLinecap="round"/>
    <circle cx="50" cy="50" r="7" stroke="#00f0ff" strokeWidth="3" fill="#02060c"/>
    <path d="M50 58V75" stroke="#00f0ff" strokeWidth="3" strokeLinecap="round"/>
    <path d="M30 40L42 50L30 60" stroke="#00f0ff" strokeWidth="2" strokeLinecap="round"/>
    <path d="M70 40L58 50L70 60" stroke="#00f0ff" strokeWidth="2" strokeLinecap="round"/>
  </svg>
);

function Navigation() {
  const location = useLocation();
  const currentUser = localStorage.getItem('username');
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 50);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const isActive = (path) => location.pathname === path ? 'text-info fw-bold' : 'text-light';

  const handleLogout = () => {
    localStorage.clear();
    window.location.href = '/login';
  };

  return (
    <Navbar 
      expand="lg" 
      fixed="top" 
      variant="dark" 
      className="py-2"
      style={{ 
        background: scrolled ? 'rgba(2, 6, 12, 0.98)' : 'transparent', 
        backdropFilter: scrolled ? 'blur(20px)' : 'none',
        borderBottom: scrolled ? '1px solid rgba(0, 240, 255, 0.2)' : 'none',
        transition: 'all 0.4s ease',
        zIndex: 9999
      }}
    >
      <Container>
        <Navbar.Brand as={Link} to="/" className="d-flex align-items-center gap-3">
          {/* Logo Icon */}
          <div style={{ filter: 'drop-shadow(0 0 8px rgba(0, 240, 255, 0.5))' }}>
            <CyberLogo />
          </div>
          {/* Logo Text */}
          <div className="d-flex flex-column" style={{ lineHeight: '1' }}>
            <span style={{ fontFamily: "'Chakra Petch', sans-serif", fontSize: '1.6rem', fontWeight: '700', color: '#fff', letterSpacing: '1px' }}>
              PRO<span style={{ color: '#00f0ff' }}>HORY</span>
            </span>
            <span style={{ fontSize: '0.7rem', color: '#b0c4de', letterSpacing: '3px', textTransform: 'uppercase' }}>
              Secure Intelligence
            </span>
          </div>
        </Navbar.Brand>
        
        <Navbar.Toggle aria-controls="basic-navbar-nav" style={{ borderColor: 'rgba(0, 240, 255, 0.3)' }} />
        
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="mx-auto align-items-center gap-4">
            {['Home', 'Dashboard', 'Expert-Dashboard'].map((item) => (
              <Nav.Link 
                as={Link} 
                to={item === 'Home' ? '/' : `/${item.toLowerCase()}`} 
                key={item}
                className={isActive(item === 'Home' ? '/' : `/${item.toLowerCase()}`)}
                style={{ fontSize: '0.95rem', letterSpacing: '1px', fontFamily: "'Exo 2', sans-serif", textTransform: 'uppercase', fontWeight: '600' }}
              >
                {item.replace('-', ' ')}
              </Nav.Link>
            ))}
          </Nav>

          <div className="d-flex gap-3 align-items-center mt-3 mt-lg-0">
            {currentUser ? (
              <>
                <span className="text-secondary small font-tech border border-secondary px-3 py-1 rounded-pill">
                  OP: {currentUser}
                </span>
                <Button variant="outline-danger" size="sm" onClick={handleLogout} className="rounded-0 px-3 fw-bold">
                  LOGOUT
                </Button>
              </>
            ) : (
              <Link to="/login">
                <Button className="nav-login-btn px-4 rounded-0 text-uppercase">
                  LOGIN
                </Button>
              </Link>
            )}
          </div>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}

export default Navigation;