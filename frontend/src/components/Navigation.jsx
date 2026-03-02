import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { FaShieldAlt } from 'react-icons/fa';
import { Container, Navbar, Nav } from 'react-bootstrap';

function Navigation() {
  const location = useLocation();

  return (
    <Navbar expand="lg" variant="dark" fixed="top" style={{ background: 'rgba(2, 6, 12, 0.95)', borderBottom: '1px solid rgba(0, 243, 255, 0.1)' }}>
      <Container>
        <Navbar.Brand as={Link} to="/" style={{ fontFamily: 'Orbitron', letterSpacing: '2px' }}>
          PRO<span style={{ color: '#00f3ff' }}>HORY</span>
        </Navbar.Brand>
        <Navbar.Toggle aria-controls="basic-navbar-nav" />
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="ms-auto">
            <Nav.Link as={Link} to="/" className={location.pathname === '/' ? 'text-info fw-bold' : ''}>HOME</Nav.Link>
            <Nav.Link as={Link} to="/login">LOGIN</Nav.Link>
            <Nav.Link as={Link} to="/register">REGISTER</Nav.Link>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}

export default Navigation;