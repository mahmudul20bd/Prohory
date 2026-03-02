import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { FaUser, FaEnvelope, FaLock, FaUserSecret, FaFileUpload, FaIdCard, FaLaptopCode, FaCheckCircle, FaUpload, FaExclamationCircle } from 'react-icons/fa';
import { Spinner } from 'react-bootstrap';
import '../styles/Auth.css';

const API_BASE_URL = "https://cyber-aware.onrender.com";

function Register() {
  const [formData, setFormData] = useState({
    username: '', email: '', password: '', role: 'user', 
    specialization: '', profileImage: '', nidFile: '' // profileImage used for CV/NID link
  });
  
  const [loading, setLoading] = useState(false);
  const [showPendingScreen, setShowPendingScreen] = useState(false); 
  
  // File Upload States
  const [isUploadingCv, setIsUploadingCv] = useState(false);
  const [isUploadingNid, setIsUploadingNid] = useState(false);
  const [cvUploaded, setCvUploaded] = useState(false);
  const [nidUploaded, setNidUploaded] = useState(false);

  // 🔥 NEW: Beautiful Error Popup State
  const [errorPopup, setErrorPopup] = useState({ isOpen: false, message: '' });

  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const selectRole = (role) => {
    setFormData({ ...formData, role: role });
  };

  // 🔥 PRO LEVEL UPLOAD TO CLOUDINARY WITH ERROR HANDLING
  const handleFileUpload = async (e, fileType) => {
    const file = e.target.files[0];
    if (!file) return;

    if (fileType === 'cv') setIsUploadingCv(true);
    else setIsUploadingNid(true);

    const uploadData = new FormData();
    uploadData.append('file', file);

    try {
      // NOTE: No authorization header here because the user is registering (doesn't have a token yet)
      const res = await axios.post(`${API_BASE_URL}/api/chat/upload`, uploadData);
      
      if (res.data && res.data.fileUrl) {
        if (fileType === 'cv') {
           setFormData(prev => ({ ...prev, profileImage: res.data.fileUrl })); // CV link
           setCvUploaded(true);
        } else {
           setFormData(prev => ({ ...prev, nidFile: res.data.fileUrl })); // NID link
           setNidUploaded(true);
        }
      }
    } catch (error) {
      console.error("Upload Error:", error);
      // 🔥 Show Animated Error Popup instead of alert()
      setErrorPopup({ isOpen: true, message: "Secure Server Connection Refused or File too large." });
      setTimeout(() => setErrorPopup({ isOpen: false, message: '' }), 3500); // Auto close after 3.5s
    } finally {
      if (fileType === 'cv') setIsUploadingCv(false);
      else setIsUploadingNid(false);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    
    // Validation for Expert
    if (formData.role === 'expert') {
      if (!formData.specialization) return alert("Please select your expertise area.");
      if (!cvUploaded || !nidUploaded) {
          setErrorPopup({ isOpen: true, message: "Identity Verification Failed! Both CV and NID must be uploaded." });
          setTimeout(() => setErrorPopup({ isOpen: false, message: '' }), 3500);
          return;
      }
    }

    setLoading(true);
    try {
      await axios.post(`${API_BASE_URL}/api/auth/register`, formData);
      
      if (formData.role === 'expert') {
        setShowPendingScreen(true);
      } else {
        alert("Registration Successful! You can now log in.");
        navigate('/login');
      }
    } catch (error) {
      console.error(error);
      setErrorPopup({ isOpen: true, message: "Registration Failed. Username or Email might be already taken." });
      setTimeout(() => setErrorPopup({ isOpen: false, message: '' }), 3500);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      
      {/* 🔥 ANIMATED ERROR POPUP FOR FAILED UPLOADS & ERRORS */}
      <AnimatePresence>
        {errorPopup.isOpen && (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, zIndex: 9999, display: 'flex', justifyContent: 'center', alignItems: 'center', pointerEvents: 'none' }}>
            {/* Background Blur */}
            <motion.div initial={{opacity: 0}} animate={{opacity: 1}} exit={{opacity: 0}} style={{ position: 'absolute', width: '100%', height: '100%', background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(5px)' }} />

            {/* The Error Modal */}
            <motion.div 
              initial={{ opacity: 0, scale: 0.5, y: 50 }} 
              animate={{ opacity: 1, scale: 1, y: 0 }} 
              exit={{ opacity: 0, scale: 0.8, y: -20 }}
              transition={{ type: "spring", stiffness: 300, damping: 20 }}
              style={{ 
                background: 'rgba(10, 15, 25, 0.95)', border: '1px solid #ff003c', 
                padding: '40px 50px', borderRadius: '20px', boxShadow: '0 0 50px rgba(255, 0, 60, 0.4)', 
                display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '15px', zIndex: 10001
              }}
            >
              <FaExclamationCircle size={60} color="#ff003c" />
              <h3 style={{color: '#ff003c', margin: 0, fontWeight: 'bold', fontFamily: 'Orbitron', letterSpacing: '2px', marginTop: '10px'}}>
                ACCESS DENIED
              </h3>
              <p style={{color: '#e2e8f0', fontSize: '14px', margin: 0, letterSpacing: '1px', textAlign: 'center', maxWidth: '320px', lineHeight: '1.6'}}>
                {errorPopup.message}
              </p>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

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
        
        {/* EXPERT PENDING APPROVAL SCREEN */}
        {showPendingScreen ? (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="text-center p-4">
            <FaCheckCircle className="text-success mb-3" style={{ fontSize: '60px' }} />
            <h3 className="text-white fw-bold mb-3" style={{ fontFamily: 'Orbitron' }}>Application Submitted</h3>
            <div className="alert alert-info bg-transparent border-info text-info p-3 mb-4" style={{ fontSize: '14px' }}>
              Your documents have been securely sent to the Admin team. You will be able to log in once your account is verified and approved.
            </div>
            <Link to="/login" className="btn btn-info w-100 fw-bold py-2">Return to Login</Link>
          </motion.div>
        ) : (
          <>
            <div className="auth-header">
              <h2 className="auth-title">Create Account</h2>
              <p className="auth-subtitle">Join the Prohory Security Network</p>
            </div>

            <form onSubmit={handleRegister}>
              
              <div className="role-group">
                <div className={`role-btn ${formData.role === 'user' ? 'active' : ''}`} onClick={() => selectRole('user')}>
                  <FaUser style={{ marginRight: '8px' }} /> Standard User
                </div>
                <div className={`role-btn ${formData.role === 'expert' ? 'active' : ''}`} onClick={() => selectRole('expert')}>
                  <FaUserSecret style={{ marginRight: '8px' }} /> Security Expert
                </div>
              </div>

              <div className="input-wrapper">
                <FaUser className="input-icon" />
                <input name="username" type="text" className="input-field" placeholder="Full Name or Username" onChange={handleChange} required />
              </div>

              <div className="input-wrapper">
                <FaEnvelope className="input-icon" />
                <input name="email" type="email" className="input-field" placeholder="Email Address" onChange={handleChange} required />
              </div>

              <div className="input-wrapper">
                <FaLock className="input-icon" />
                <input name="password" type="password" className="input-field" placeholder="Create Password" onChange={handleChange} required />
              </div>

              {/* 🔥 EXPERT SPECIFIC FIELDS */}
              <AnimatePresence>
                {formData.role === 'expert' && (
                  <motion.div 
                    initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }}
                    className="mb-3 p-3 border border-dashed rounded"
                    style={{ borderColor: '#00E5FF', background: 'rgba(0, 243, 255, 0.05)' }}
                  >
                    
                    {/* EXPANDED Specialization Dropdown UI */}
                    <div className="input-wrapper mb-3" style={{ background: '#0a0f1c', border: '1px solid #334155' }}>
                      <FaLaptopCode className="input-icon" color="#00E5FF" />
                      <select name="specialization" className="input-field shadow-none" style={{ color: '#fff', cursor: 'pointer', appearance: 'none' }} onChange={handleChange} required>
                        <option value="" disabled selected>Select Your Expertise Area</option>
                        <option value="Malware Analyst" style={{background: '#0a0f1c', color: '#fff'}}>Malware Analyst</option>
                        <option value="Network Security Engineer" style={{background: '#0a0f1c', color: '#fff'}}>Network Security Engineer</option>
                        <option value="Threat Intelligence Expert" style={{background: '#0a0f1c', color: '#fff'}}>Threat Intelligence Expert</option>
                        <option value="Digital Forensics Investigator" style={{background: '#0a0f1c', color: '#fff'}}>Digital Forensics Investigator</option>
                        <option value="Penetration Tester (Ethical Hacker)" style={{background: '#0a0f1c', color: '#fff'}}>Penetration Tester (Ethical Hacker)</option>
                        <option value="Cloud Security Architect" style={{background: '#0a0f1c', color: '#fff'}}>Cloud Security Architect</option>
                        <option value="Cryptography Expert" style={{background: '#0a0f1c', color: '#fff'}}>Cryptography Expert</option>
                        <option value="Incident Response Specialist" style={{background: '#0a0f1c', color: '#fff'}}>Incident Response Specialist</option>
                        <option value="SOC Analyst" style={{background: '#0a0f1c', color: '#fff'}}>SOC Analyst</option>
                        <option value="Vulnerability Assessor" style={{background: '#0a0f1c', color: '#fff'}}>Vulnerability Assessor</option>
                        <option value="Cyber Crime Investigator" style={{background: '#0a0f1c', color: '#fff'}}>Cyber Crime Investigator</option>
                        <option value="OSINT Specialist" style={{background: '#0a0f1c', color: '#fff'}}>OSINT Specialist</option>
                        <option value="Web Application Security Specialist" style={{background: '#0a0f1c', color: '#fff'}}>Web Application Security Specialist</option>
                      </select>
                    </div>

                    <div className="row g-2 mt-2">
                      {/* CV Upload */}
                      <div className="col-6">
                        <label className="d-block text-white mb-2" style={{ fontSize: '11px', letterSpacing: '0.5px', color: '#94a3b8' }}>
                          Upload CV (PDF/IMG)
                        </label>
                        <input type="file" id="cvUpload" className="d-none" accept="image/*,application/pdf" onChange={(e) => handleFileUpload(e, 'cv')} />
                        <label htmlFor="cvUpload" className={`btn w-100 d-flex align-items-center justify-content-center gap-2 ${cvUploaded ? 'btn-success' : 'btn-outline-info'}`} style={{fontSize: '12px', padding: '10px', cursor: 'pointer', borderStyle: cvUploaded ? 'solid' : 'dashed'}}>
                           {isUploadingCv ? <Spinner size="sm" /> : cvUploaded ? <FaCheckCircle/> : <FaUpload/>}
                           {isUploadingCv ? 'Uploading...' : cvUploaded ? 'CV Verified' : 'Select File'}
                        </label>
                      </div>
                      
                      {/* NID Upload */}
                      <div className="col-6">
                        <label className="d-block text-white mb-2" style={{ fontSize: '11px', letterSpacing: '0.5px', color: '#94a3b8' }}>
                          Upload NID/Passport
                        </label>
                        <input type="file" id="nidUpload" className="d-none" accept="image/*" onChange={(e) => handleFileUpload(e, 'nid')} />
                        <label htmlFor="nidUpload" className={`btn w-100 d-flex align-items-center justify-content-center gap-2 ${nidUploaded ? 'btn-success' : 'btn-outline-info'}`} style={{fontSize: '12px', padding: '10px', cursor: 'pointer', borderStyle: nidUploaded ? 'solid' : 'dashed'}}>
                           {isUploadingNid ? <Spinner size="sm" /> : nidUploaded ? <FaCheckCircle/> : <FaIdCard/>}
                           {isUploadingNid ? 'Uploading...' : nidUploaded ? 'NID Verified' : 'Select Image'}
                        </label>
                      </div>
                    </div>
                    
                  </motion.div>
                )}
              </AnimatePresence>

              <button type="submit" className="submit-btn mt-2" disabled={loading || isUploadingCv || isUploadingNid}>
                {loading ? <Spinner animation="border" size="sm" variant="dark" /> : 'Create Account'}
              </button>
            </form>

            <div className="auth-footer">
              Already have an account? <Link to="/login" className="link-text">Login Here</Link>
            </div>
          </>
        )}
      </motion.div>
    </div>
  );
}

export default Register;