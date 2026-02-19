import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FaUser, FaLock, FaShieldAlt } from 'react-icons/fa';
import { Spinner } from 'react-bootstrap';
import '../styles/Auth.css';

function Login() {
  const [username, setUsername] = useState(''); // Variable name updated for clarity
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      // API Call to Backend
      const response = await axios.post('http://localhost:9090/api/auth/login', {
        username: username, 
        password: password
      });

      if (response.data.success) {
        // 🔥 লজিক ফিক্স: রোল সেভ করার আগে trim এবং lowercase করা হচ্ছে
        const userRole = response.data.role ? response.data.role.toLowerCase().trim() : "user";
        
        // টোকেন এবং ইউজার ডাটা সেভ
        localStorage.setItem('token', response.data.token); 
        localStorage.setItem('username', response.data.username);
        localStorage.setItem('role', userRole); // এটি সেটিংস পেজের সাইডবার ফিক্স করবে

        // Role Based Navigation
        if (userRole === 'admin') navigate('/admin-dashboard');
        else if (userRole === 'expert') navigate('/expert-dashboard');
        else navigate('/dashboard');
      } else {
        alert("❌ Login Failed: " + response.data.message);
      }
      
    } catch (error) {
      console.error(error);
      const errMsg = error.response?.data?.message || "ACCESS DENIED: Invalid Credentials.";
      alert("❌ " + errMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="cyber-grid"></div>
      <div className="scanline-overlay"></div>

      <div className="particles-container">
        {[...Array(25)].map((_, i) => (
          <div key={i} className="particle" style={{ 
            left: `${Math.random() * 100}%`, 
            animationDelay: `${Math.random() * 5}s`, 
            animationDuration: `${4 + Math.random() * 6}s` 
          }}></div>
        ))}
      </div>

      <motion.div 
        initial={{ opacity: 0, scale: 0.8, y: 50 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        transition={{ duration: 0.6, type: "spring", stiffness: 120 }}
        className="auth-box"
      >
        <div className="auth-header">
          <motion.div 
            animate={{ rotateY: 360 }}
            transition={{ duration: 6, repeat: Infinity, ease: "linear" }}
            style={{ display: 'inline-block', marginBottom: '15px' }}
          >
            <FaShieldAlt style={{ fontSize: '4rem', color: '#00f3ff', filter: 'drop-shadow(0 0 20px #00f3ff)' }} />
          </motion.div>
          <h2 className="auth-title">SYSTEM LOGIN</h2>
          <p className="auth-subtitle">Secure Neural Gateway</p>
        </div>

        <form onSubmit={handleLogin}>
          <div className="input-wrapper">
            <FaUser className="input-icon" />
            <input 
              type="text" 
              className="input-field" 
              placeholder="Username" 
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>

          <div className="input-wrapper">
            <FaLock className="input-icon" />
            <input 
              type="password" 
              className="input-field" 
              placeholder="Security Passcode"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <div className="d-flex justify-content-between align-items-center mb-4" style={{ fontSize: '0.9rem', color: '#a0c4ff', fontWeight: '600' }}>
            <label style={{ cursor: 'pointer', display: 'flex', alignItems: 'center' }}>
              <input type="checkbox" style={{ marginRight: '8px', accentColor: '#00f3ff', width: '16px', height: '16px' }} /> Secure Session
            </label>
            <Link to="#" style={{ color: '#00f3ff', textDecoration: 'none', letterSpacing: '1px' }}>Lost Key?</Link>
          </div>

          <button type="submit" className="submit-btn" disabled={loading}>
            {loading ? <Spinner animation="border" size="sm" variant="dark" /> : 'AUTHENTICATE'}
          </button>
        </form>

        <div className="auth-footer">
          Unregistered Unit? <Link to="/register" className="link-text">Initialize</Link>
        </div>
      </motion.div>
    </div>
  );
}

export default Login;