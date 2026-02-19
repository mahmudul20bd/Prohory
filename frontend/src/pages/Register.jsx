import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { FaUser, FaEnvelope, FaLock, FaUserSecret, FaFileUpload } from 'react-icons/fa';
import { Spinner } from 'react-bootstrap';
import '../styles/Auth.css';

function Register() {
  const [formData, setFormData] = useState({
    username: '', email: '', password: '', role: 'user', profileImage: ''
  });
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const selectRole = (role) => {
    setFormData({ ...formData, role: role });
  };

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setFormData({ ...formData, profileImage: reader.result });
      };
      reader.readAsDataURL(file);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      // ✅ FIXED: Path changed from '/api/users' to '/api/auth'
      // পোর্ট 9090 রেখেছেন তাই 9090 ই থাকবে
      await axios.post('https://cyber-aware.onrender.com/api/auth/register', formData);
      
      alert("✅ Registration Successful! Please Login.");
      navigate('/login');
    } catch (error) {
      console.error(error);
      alert("❌ Registration Failed. Username might be taken.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="cyber-grid-bg"></div>
      
      <div className="particles-container">
        {[...Array(20)].map((_, i) => (
          <div key={i} className="particle" style={{ left: `${Math.random() * 100}%`, animationDelay: `${Math.random() * 5}s` }}></div>
        ))}
      </div>

      <motion.div 
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.5 }}
        className="auth-box"
        style={{ maxWidth: '550px' }}
      >
        <div className="auth-header">
          <h2 className="auth-title">NEW REGISTRY</h2>
          <p className="auth-subtitle">Create Your Digital Signature</p>
        </div>

        <form onSubmit={handleRegister}>
          
          <div className="role-group">
            <div 
              className={`role-btn ${formData.role === 'user' ? 'active' : ''}`}
              onClick={() => selectRole('user')}
            >
              <FaUser style={{ marginRight: '8px' }} /> User Unit
            </div>
            <div 
              className={`role-btn ${formData.role === 'expert' ? 'active' : ''}`}
              onClick={() => selectRole('expert')}
            >
              <FaUserSecret style={{ marginRight: '8px' }} /> Expert
            </div>
          </div>

          <div className="input-wrapper">
            <FaUser className="input-icon" />
            <input 
              name="username" type="text" className="input-field" 
              placeholder="Codename / Full Name" 
              onChange={handleChange} required 
            />
          </div>

          <div className="input-wrapper">
            <FaEnvelope className="input-icon" />
            <input 
              name="email" type="email" className="input-field" 
              placeholder="Secure Email Link" 
              onChange={handleChange} required 
            />
          </div>

          <div className="input-wrapper">
            <FaLock className="input-icon" />
            <input 
              name="password" type="password" className="input-field" 
              placeholder="Set Passcode" 
              onChange={handleChange} required 
            />
          </div>

          <AnimatePresence>
            {formData.role === 'expert' && (
              <motion.div 
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                className="mb-3 p-3 border border-dashed rounded"
                style={{ borderColor: 'var(--neon-cyan)', background: 'rgba(0, 243, 255, 0.05)' }}
              >
                <label className="d-block text-white small mb-2" style={{ letterSpacing: '1px' }}>
                  <FaFileUpload className="me-2"/> UPLOAD CREDENTIALS
                </label>
                <input type="file" accept="image/*" onChange={handleImageChange} className="form-control bg-dark text-white border-secondary" />
              </motion.div>
            )}
          </AnimatePresence>

          <button type="submit" className="submit-btn" disabled={loading}>
            {loading ? <Spinner animation="border" size="sm" variant="dark" /> : 'CONFIRM IDENTITY'}
          </button>
        </form>

        <div className="auth-footer">
          Already Registered? 
          <Link to="/login" className="link-text">Login Terminal</Link>
        </div>
      </motion.div>
    </div>
  );
}

export default Register;