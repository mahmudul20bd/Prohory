import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { motion, AnimatePresence } from 'framer-motion';
import { FaUser, FaLock, FaSave, FaImage, FaUpload, FaIdBadge } from 'react-icons/fa';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

const API_BASE_URL = "https://cyber-aware.onrender.com";

// 🔥 Custom Confetti Particle Component
const ConfettiPiece = ({ side }) => {
  const isLeft = side === 'left';
  const colors = ['#00E5FF', '#9d00ff', '#00E676', '#FFEA00', '#ff003c'];
  const color = colors[Math.floor(Math.random() * colors.length)];
  
  // Starting positions (Bottom Left or Bottom Right)
  const startX = isLeft ? -50 : window.innerWidth + 50;
  // End positions (Shooting towards the center top)
  const endX = startX + (isLeft ? 1 : -1) * (Math.random() * 600 + 200);
  const startY = window.innerHeight;
  const endY = window.innerHeight - (Math.random() * 600 + 400);

  return (
    <motion.div
      initial={{ x: startX, y: startY, opacity: 1, scale: Math.random() * 0.6 + 0.4, rotate: 0 }}
      animate={{ x: endX, y: endY, opacity: 0, rotate: 360 * (Math.random() * 5 + 2) }}
      transition={{ duration: Math.random() * 2 + 1.5, ease: "easeOut" }}
      style={{
        position: 'fixed', width: '12px', height: '24px', background: color, zIndex: 10000, borderRadius: '3px'
      }}
    />
  );
};

function Settings() {
  const [profile, setProfile] = useState({
    username: '', role: '', clearanceLevel: '', email: '', 
    specialization: '', aboutMe: '', profileImage: '', coverImage: ''
  });

  const [passwordData, setPasswordData] = useState({ oldPassword: '', newPassword: '' });
  const [isUploadingPhoto, setIsUploadingPhoto] = useState(false);
  const [isUploadingCover, setIsUploadingCover] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  
  // 🔥 Center Animated Popup State
  const [showSuccessPopup, setShowSuccessPopup] = useState({ isOpen: false, message: '' });

  const token = localStorage.getItem('token');

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      const res = await axios.get(`${API_BASE_URL}/api/users/me`, config);
      
      setProfile(prev => ({
        ...prev,
        username: res.data.username || '',
        role: res.data.role || 'User',
        clearanceLevel: res.data.clearanceLevel || 'Level 1',
        email: res.data.email || '',
        specialization: res.data.specialization || 'Cyber Security Analyst',
        aboutMe: res.data.aboutMe || 'Security Expert ready to assist.',
        profileImage: res.data.profileImage || '',
        coverImage: res.data.coverImage || ''
      }));
    } catch (error) {
      console.error("Failed to fetch profile");
    }
  };

  const handleProfileUpdate = async (e) => {
    e.preventDefault();
    setIsSaving(true);
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      await axios.put(`${API_BASE_URL}/api/users/profile`, {
        email: profile.email,
        aboutMe: profile.aboutMe,
        profileImage: profile.profileImage,
        coverImage: profile.coverImage
      }, config);
      
      // 🔥 Trigger Center Popup & Confetti
      setShowSuccessPopup({ isOpen: true, message: "Public Identity Updated Successfully!" });
      setTimeout(() => setShowSuccessPopup({ isOpen: false, message: '' }), 3500); // 3.5 seconds to enjoy animation
      
    } catch (error) {
      toast.error("Failed to update profile.");
    } finally {
      setIsSaving(false);
    }
  };

  const handlePasswordUpdate = async (e) => {
    e.preventDefault();
    if (!passwordData.oldPassword || !passwordData.newPassword) return toast.error("Please fill both password fields.");
    
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      await axios.put(`${API_BASE_URL}/api/users/change-password`, passwordData, config);
      
      setShowSuccessPopup({ isOpen: true, message: "Security Passcode Changed Successfully!" });
      setTimeout(() => setShowSuccessPopup({ isOpen: false, message: '' }), 3500);
      
      setPasswordData({ oldPassword: '', newPassword: '' });
    } catch (error) {
      toast.error(error.response?.data || "Incorrect Old Password.");
    }
  };

  const handleImageUpload = async (e, type) => {
    const file = e.target.files[0];
    if (!file) return;

    if (type === 'profile') setIsUploadingPhoto(true);
    else setIsUploadingCover(true);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const config = { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data' } };
      const res = await axios.post(`${API_BASE_URL}/api/chat/upload`, formData, config);
      
      if (res.data && res.data.fileUrl) {
          if (type === 'profile') setProfile({ ...profile, profileImage: res.data.fileUrl });
          else setProfile({ ...profile, coverImage: res.data.fileUrl });
          toast.info("Image uploaded temporarily. Click Save to apply.", {icon: "🚀"});
      }
    } catch (error) {
      toast.error("Upload failed.");
    } finally {
      setIsUploadingPhoto(false);
      setIsUploadingCover(false);
    }
  };

  const isExpert = profile.role.toLowerCase() === 'expert';

  return (
    <div className="settings-container position-relative" style={{ minHeight: '100%' }}>
      <ToastContainer theme="dark" />

      {/* 🔥 CENTERED SUCCESS POPUP WITH CONFETTI & ANIMATED CHECKMARK */}
      <AnimatePresence>
        {showSuccessPopup.isOpen && (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, zIndex: 9999, display: 'flex', justifyContent: 'center', alignItems: 'center', pointerEvents: 'none' }}>
            
            {/* Background Blur */}
            <motion.div initial={{opacity: 0}} animate={{opacity: 1}} exit={{opacity: 0}} style={{ position: 'absolute', width: '100%', height: '100%', background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(5px)' }} />
            
            {/* Confetti Particles */}
            {[...Array(50)].map((_, i) => (
              <ConfettiPiece key={i} side={i % 2 === 0 ? 'left' : 'right'} />
            ))}

            {/* The Popup Modal */}
            <motion.div 
              initial={{ opacity: 0, scale: 0.5, y: 50 }} 
              animate={{ opacity: 1, scale: 1, y: 0 }} 
              exit={{ opacity: 0, scale: 0.8, y: -20 }}
              transition={{ type: "spring", stiffness: 300, damping: 20 }}
              style={{ 
                background: 'rgba(10, 15, 25, 0.95)', border: '1px solid #00E676', 
                padding: '40px 50px', borderRadius: '20px', boxShadow: '0 0 50px rgba(0, 230, 118, 0.4)', 
                display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '15px', zIndex: 10001
              }}
            >
              {/* Animated SVG Checkmark */}
              <motion.svg width="100" height="100" viewBox="0 0 50 50">
                <motion.circle cx="25" cy="25" r="22" fill="transparent" stroke="#00E676" strokeWidth="3"
                  initial={{ pathLength: 0 }} animate={{ pathLength: 1 }} transition={{ duration: 0.6, ease: "easeInOut" }} />
                <motion.path d="M15 25 l7 7 l15 -15" fill="transparent" stroke="#00E676" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round"
                  initial={{ pathLength: 0 }} animate={{ pathLength: 1 }} transition={{ delay: 0.5, duration: 0.4, ease: "easeOut" }} />
              </motion.svg>

              <h3 style={{color: '#00E676', margin: 0, fontWeight: 'bold', fontFamily: 'Orbitron', letterSpacing: '2px', marginTop: '10px'}}>
                SUCCESS
              </h3>
              <p style={{color: '#e2e8f0', fontSize: '15px', margin: 0, letterSpacing: '1px'}}>{showSuccessPopup.message}</p>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      <motion.div initial={{opacity: 0, y: 15}} animate={{opacity: 1, y: 0}}>
        
        {/* EXPERT ONLY: PUBLIC PROFILE SETTINGS */}
        {isExpert && (
          <div className="cyber-card p-0 overflow-hidden shadow-lg mb-4" style={{border: '1px solid rgba(0, 229, 255, 0.3)', borderRadius: '15px'}}>
            <div className="p-3 border-bottom border-secondary bg-dark text-white d-flex align-items-center gap-2">
                <FaIdBadge size={20} color="#00f3ff"/> <h5 className="m-0 fw-bold" style={{fontFamily: 'Orbitron'}}>PUBLIC IDENTITY CONFIG</h5>
            </div>

            {/* Cover Photo Upload */}
            <div className="position-relative" style={{ height: '180px', background: profile.coverImage ? `url(${profile.coverImage}) center/cover` : 'linear-gradient(135deg, rgba(0,243,255,0.2) 0%, rgba(157,0,255,0.2) 100%)' }}>
               <div className="position-absolute top-0 end-0 p-3">
                  <input type="file" id="coverUpload" className="d-none" accept="image/*" onChange={(e) => handleImageUpload(e, 'cover')} />
                  <label htmlFor="coverUpload" className="btn btn-sm btn-dark border-info text-info fw-bold" style={{background: 'rgba(0,0,0,0.6)'}}>
                      {isUploadingCover ? <span className="spinner-border spinner-border-sm me-2"></span> : <FaImage className="me-2"/>} Update Cover
                  </label>
               </div>

               {/* Profile Photo Upload */}
               <div style={{ position: 'absolute', bottom: '-50px', left: '40px' }}>
                  <div className="position-relative">
                    <img src={profile.profileImage || 'https://via.placeholder.com/150'} alt="Profile" style={{ width: '120px', height: '120px', borderRadius: '50%', border: '4px solid #050810', objectFit: 'cover' }} />
                    <input type="file" id="profileUpload" className="d-none" accept="image/*" onChange={(e) => handleImageUpload(e, 'profile')} />
                    <label htmlFor="profileUpload" className="position-absolute bottom-0 end-0 bg-info rounded-circle p-2 d-flex justify-content-center align-items-center" style={{cursor: 'pointer', border: '3px solid #050810'}}>
                        {isUploadingPhoto ? <span className="spinner-border spinner-border-sm text-dark" style={{width: '15px', height: '15px'}}></span> : <FaUpload size={14} color="#000"/>}
                    </label>
                  </div>
               </div>
            </div>

            <div style={{ padding: '70px 30px 30px 30px', background: 'rgba(10, 15, 25, 0.8)' }}>
               <form onSubmit={handleProfileUpdate}>
                  <div className="row g-4">
                     <div className="col-md-6">
                        <label className="text-secondary small fw-bold mb-2">DISPLAY NAME (System Locked)</label>
                        <input type="text" className="form-control bg-dark text-white border-secondary" value={profile.username} disabled />
                     </div>
                     <div className="col-md-6">
                        <label className="text-secondary small fw-bold mb-2">SPECIALIZATION (Verified by Admin)</label>
                        <input type="text" className="form-control bg-dark text-info border-secondary fw-bold" value={profile.specialization} disabled />
                     </div>
                     <div className="col-12">
                        <label className="text-info small fw-bold mb-2">ABOUT ME (Publicly Visible Biography)</label>
                        <textarea className="form-control text-white border-info" rows="5" style={{background: 'rgba(0, 243, 255, 0.05)'}} value={profile.aboutMe} onChange={(e) => setProfile({...profile, aboutMe: e.target.value})} placeholder="Write your professional biography..."></textarea>
                     </div>
                  </div>
                  <button type="submit" className="btn btn-info mt-4 fw-bold w-100 py-3" disabled={isSaving} style={{letterSpacing: '1px', fontSize: '15px'}}>
                      {isSaving ? <span className="spinner-border spinner-border-sm me-2"></span> : <FaSave className="me-2"/>} {isSaving ? "SAVING IDENTITY..." : "UPDATE PUBLIC IDENTITY"}
                  </button>
               </form>
            </div>
          </div>
        )}

        {/* GENERAL ACCOUNT SETTINGS */}
        <div className="row g-4">
          <div className="col-md-6">
            <div className="cyber-card p-4 h-100">
              <h5 className="text-white mb-4 d-flex align-items-center gap-2"><FaUser color="#9d00ff"/> Core Credentials</h5>
              <div className="mb-3">
                <label className="text-secondary small fw-bold">SYSTEM ROLE</label>
                <div className="p-3 rounded mt-1 border border-secondary" style={{background: 'rgba(255,255,255,0.05)', color: '#fff'}}>{profile.role.toUpperCase()}</div>
              </div>
              <div className="mb-3">
                <label className="text-secondary small fw-bold">CLEARANCE LEVEL</label>
                <div className="p-3 rounded mt-1 border border-secondary" style={{background: 'rgba(255,255,255,0.05)', color: '#fff'}}>{profile.clearanceLevel}</div>
              </div>
              <div className="mb-3">
                <label className="text-secondary small fw-bold">COMMUNICATION LINK (Email)</label>
                <input type="email" className="form-control bg-dark text-white border-secondary mt-1" value={profile.email} onChange={(e) => setProfile({...profile, email: e.target.value})} />
              </div>
              {!isExpert && (
                <button className="btn btn-outline-primary mt-3 w-100 fw-bold py-2" onClick={handleProfileUpdate}>
                   UPDATE EMAIL
                </button>
              )}
            </div>
          </div>

          <div className="col-md-6">
            <div className="cyber-card p-4 h-100 border-danger">
              <h5 className="text-danger mb-4 d-flex align-items-center gap-2"><FaLock /> Security & Passcode</h5>
              <form onSubmit={handlePasswordUpdate}>
                <div className="mb-3">
                  <label className="text-secondary small fw-bold">CURRENT PASSCODE</label>
                  <input type="password" className="form-control bg-dark text-white border-secondary mt-1" value={passwordData.oldPassword} onChange={(e) => setPasswordData({...passwordData, oldPassword: e.target.value})} required/>
                </div>
                <div className="mb-4">
                  <label className="text-secondary small fw-bold">NEW PASSCODE</label>
                  <input type="password" className="form-control bg-dark text-white border-secondary mt-1" value={passwordData.newPassword} onChange={(e) => setPasswordData({...passwordData, newPassword: e.target.value})} required/>
                </div>
                <button type="submit" className="btn btn-danger w-100 fw-bold py-2">
                  CHANGE SECURITY KEY
                </button>
              </form>
            </div>
          </div>
        </div>

      </motion.div>
    </div>
  );
}

export default Settings;