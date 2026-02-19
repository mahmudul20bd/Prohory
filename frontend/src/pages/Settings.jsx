import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { 
  FaThLarge, FaSearch, FaShieldAlt, FaHistory, FaCog, FaSignOutAlt, 
  FaUserCircle, FaLock, FaUserShield, FaEnvelope, FaSave,
  FaGlobe, FaBug, FaUsers, FaServer, FaUserSecret
} from 'react-icons/fa';
import '../styles/Dashboard.css';

function Settings() {
  const [activeTab, setActiveTab] = useState('profile');
  const [loading, setLoading] = useState(false);
  
  const navigate = useNavigate();
  const username = localStorage.getItem('username') || 'Agent';
  const rawRole = localStorage.getItem('role') || 'User';
  const role = rawRole.toLowerCase().trim(); // Role check
  const token = localStorage.getItem('token');

  const [email, setEmail] = useState('');
  const [passwords, setPasswords] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });

  const handleProfileUpdate = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      await axios.put(`https://cyber-aware.onrender.com/api/users/profile?username=${username}`, { email }, config);
      alert("✅ Profile Updated!");
    } catch (error) { alert("❌ Failed to update."); } 
    finally { setLoading(false); }
  };

  const handlePasswordChange = async (e) => {
    e.preventDefault();
    if (passwords.newPassword !== passwords.confirmPassword) { alert("⚠️ Passwords don't match!"); return; }
    setLoading(true);
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      await axios.put(`https://cyber-aware.onrender.com/api/users/change-password?username=${username}`, {
        oldPassword: passwords.oldPassword, newPassword: passwords.newPassword
      }, config);
      alert("✅ Password Changed! Please login again.");
      localStorage.clear();
      window.location.href = '/login';
    } catch (error) { alert("❌ Failed to change password."); } 
    finally { setLoading(false); }
  };

  const handleLogout = () => { localStorage.clear(); navigate('/login'); };

  // Theme logic based on role
  const themeColor = role === 'admin' ? '#9d00ff' : role === 'expert' ? '#ff003c' : '#00f3ff';

  return (
    <div className="dashboard-layout" style={{background: role === 'admin' ? '#0a0a0a' : role === 'expert' ? '#050000' : ''}}>
      <aside className="sidebar" style={{borderRightColor: role === 'admin' ? '#9d00ff' : role === 'expert' ? 'rgba(255,0,60,0.2)' : ''}}>
        <div className="brand-section">
          {role === 'admin' ? (
            <><h3 style={{fontFamily: 'Orbitron', margin: 0}}>CYBER<span style={{color: '#9d00ff'}}>ROOT</span></h3><small className="text-secondary">ADMINISTRATION</small></>
          ) : role === 'expert' ? (
            <><h3 style={{fontFamily: 'Orbitron', margin: 0}}>CYBER<span style={{color: '#ff003c'}}>OPS</span></h3><small className="text-secondary">EXPERT COMMAND</small></>
          ) : (
            <><h3 style={{fontFamily: 'Orbitron', margin: 0}}>CYBER<span style={{color: '#00f3ff'}}>AWARE</span></h3><small className="text-secondary">INTELLIGENCE</small></>
          )}
        </div>

        <ul className="nav-links">
          {role === 'admin' && (
            <>
              <li className="nav-item" onClick={() => navigate('/admin-dashboard')}><FaUsers /> USER MANAGEMENT</li>
              <li className="nav-item" onClick={() => navigate('/admin-dashboard')}><FaServer /> SYSTEM LOGS</li>
            </>
          )}
          {role === 'expert' && (
            <>
              <li className="nav-item" onClick={() => navigate('/expert-dashboard')}><FaGlobe /> THREAT MAP</li>
              <li className="nav-item" onClick={() => navigate('/expert-dashboard')}><FaBug /> MALWARE ANALYSIS</li>
            </>
          )}
          {role === 'user' && (
            <>
              <li className="nav-item" onClick={() => navigate('/dashboard')}><FaThLarge /> OVERVIEW</li>
              <li className="nav-item" onClick={() => navigate('/dashboard')}><FaSearch /> AI SCANNER</li>
              <li className="nav-item" onClick={() => navigate('/dashboard')}><FaHistory /> HISTORY LOGS</li>
            </>
          )}
          <li className="nav-item active" style={{color: themeColor, borderLeft: `3px solid ${themeColor}`, background: `${themeColor}15`}}>
            <FaCog /> SETTINGS
          </li>
        </ul>

        <div className="user-profile-mini">
          <div className="d-flex align-items-center gap-3 mb-3">
            {role === 'admin' ? <FaUserShield className="text-warning fs-4"/> : 
             role === 'expert' ? <FaUserSecret className="text-danger fs-4"/> : 
             <FaUserCircle className="fs-1 text-secondary"/>}
            <div>
              <h6 className="m-0 text-white">{username}</h6>
              <small style={{color: themeColor}}>{rawRole.toUpperCase()}</small>
            </div>
          </div>
          <button onClick={handleLogout} className={`btn btn-outline-${role === 'admin' ? 'warning' : 'danger'} w-100 btn-sm rounded-0`}>
            <FaSignOutAlt /> LOGOUT SESSION
          </button>
        </div>
      </aside>

      <main className="main-content">
        <header className="dashboard-header" style={{borderBottomColor: `${themeColor}40`}}>
          <div>
            <h2 className="m-0" style={{fontFamily: 'Orbitron'}}>SYSTEM SETTINGS</h2>
            <small className="text-secondary">Configuration & Security Protocols</small>
          </div>
          <div className="cyber-card p-2 px-4 d-flex align-items-center gap-2" style={{borderColor: themeColor}}>
            <div className={`spinner-grow spinner-grow-sm text-${role === 'expert' ? 'danger' : 'success'}`} role="status"></div>
            <small style={{color: themeColor}}>SYSTEM ONLINE</small>
          </div>
        </header>

        <div className="content-body">
          <div className="row justify-content-center">
            <div className="col-lg-8">
              <div className="d-flex gap-2 mb-4">
                <button className={`btn btn-sm flex-grow-1 fw-bold`} onClick={() => setActiveTab('profile')}
                  style={{ backgroundColor: activeTab === 'profile' ? themeColor : 'transparent', color: activeTab === 'profile' ? '#000' : '#fff', border: `1px solid ${themeColor}` }}>
                  <FaUserShield className="me-2"/> PROFILE INFO
                </button>
                <button className={`btn btn-sm flex-grow-1 fw-bold ${activeTab === 'security' ? 'btn-danger text-white' : 'btn-outline-secondary text-light'}`}
                  onClick={() => setActiveTab('security')}><FaLock className="me-2"/> SECURITY</button>
              </div>

              {activeTab === 'profile' && (
                <motion.div initial={{opacity:0, y:10}} animate={{opacity:1, y:0}} className="cyber-card p-4" style={{borderColor: themeColor}}>
                  <h6 className="mb-4 border-bottom border-secondary pb-2" style={{color: themeColor}}>IDENTITY MODULE</h6>
                  <form onSubmit={handleProfileUpdate}>
                    <div className="row g-3 mb-3">
                      <div className="col-md-6">
                        <label className="text-secondary small mb-1">Username</label>
                        <input type="text" className="form-control bg-dark text-white border-secondary" value={username} disabled />
                      </div>
                      <div className="col-md-6">
                        <label className="text-secondary small mb-1">Role Clearance</label>
                        <input type="text" className="form-control bg-dark border-secondary fw-bold" style={{color: themeColor}} value={rawRole.toUpperCase()} disabled />
                      </div>
                    </div>
                    <div className="mb-3">
                      <label className="text-secondary small mb-1">Update Email Address</label>
                      <div className="input-group">
                        <span className="input-group-text bg-dark border-secondary" style={{color: themeColor}}><FaEnvelope/></span>
                        <input type="email" className="form-control bg-dark text-white border-secondary" placeholder="Enter new email..." value={email} onChange={(e) => setEmail(e.target.value)} required />
                      </div>
                    </div>
                    <button type="submit" className="btn w-100 fw-bold mt-2" style={{backgroundColor: themeColor, color: '#000'}} disabled={loading}>
                      {loading ? 'UPDATING...' : <><FaSave className="me-2"/> SAVE CHANGES</>}
                    </button>
                  </form>
                </motion.div>
              )}

              {activeTab === 'security' && (
                <motion.div initial={{opacity:0, y:10}} animate={{opacity:1, y:0}} className="cyber-card p-4" style={{borderColor: '#ff003c'}}>
                  <h6 className="mb-4 text-danger border-bottom border-secondary pb-2">ENCRYPTION KEYS</h6>
                  <form onSubmit={handlePasswordChange}>
                    <div className="mb-3">
                      <label className="text-secondary small mb-1">Current Password</label>
                      <input type="password" className="form-control bg-dark text-white border-secondary" value={passwords.oldPassword} onChange={(e) => setPasswords({...passwords, oldPassword: e.target.value})} required />
                    </div>
                    <div className="mb-3">
                      <label className="text-secondary small mb-1">New Password</label>
                      <input type="password" className="form-control bg-dark text-white border-secondary" value={passwords.newPassword} onChange={(e) => setPasswords({...passwords, newPassword: e.target.value})} required />
                    </div>
                    <div className="mb-4">
                      <label className="text-secondary small mb-1">Confirm New Password</label>
                      <input type="password" className="form-control bg-dark text-white border-secondary" value={passwords.confirmPassword} onChange={(e) => setPasswords({...passwords, confirmPassword: e.target.value})} required />
                    </div>
                    <button type="submit" className="btn btn-danger w-100 fw-bold mt-2" disabled={loading}>
                      {loading ? 'ENCRYPTING...' : <><FaLock className="me-2"/> UPDATE PASSWORD</>}
                    </button>
                  </form>
                </motion.div>
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default Settings;