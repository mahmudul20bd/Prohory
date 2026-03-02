import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { AreaChart, Area, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ComposedChart } from 'recharts';
import { 
  FaUsers, FaUserShield, FaTrashAlt, FaSignOutAlt, FaServer, 
  FaSearch, FaTachometerAlt, FaBug, FaShieldAlt, FaHistory, FaClock, 
  FaCheckCircle, FaStar, FaDownload, FaExclamationCircle, FaList, FaFilter, FaChevronDown
} from 'react-icons/fa';
import '../styles/Dashboard.css';

const API_BASE_URL = "https://cyber-aware.onrender.com";

// Helper function to safely parse Spring Boot Dates
const parseDate = (timestamp) => {
    if (!timestamp) return new Date();
    if (Array.isArray(timestamp)) {
        const [y, m, d, h, min, s] = timestamp;
        return new Date(Date.UTC(y, m - 1, d, h || 0, min || 0, s || 0));
    }
    let ts = timestamp;
    if (typeof ts === 'string' && !ts.endsWith('Z') && !ts.includes('+')) ts += 'Z';
    return new Date(ts);
};

function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [expertTab, setExpertTab] = useState('pending'); 
  
  const [users, setUsers] = useState([]);
  const [logs, setLogs] = useState([]);
  const [graphData, setGraphData] = useState([]);
  const [reportStats, setReportStats] = useState({ totalScans: 0, blockedScams: 0, safeLinks: 0 });
  const [loading, setLoading] = useState(true);
  
  const [selectedImage, setSelectedImage] = useState(null);

  // Custom Animated Confirmation Modal State
  const [confirmModal, setConfirmModal] = useState({ isOpen: false, action: null, id: null, title: '', message: '' });

  // Log Filter States
  const [userSearchTerm, setUserSearchTerm] = useState('');
  const [logSearchTerm, setLogSearchTerm] = useState('');
  const [logTimeframe, setLogTimeframe] = useState('all'); 
  const [logDateFilter, setLogDateFilter] = useState(''); 
  const [showLogFilterDropdown, setShowLogFilterDropdown] = useState(false); // 🔥 NEW: Toggle State for Custom Dropdown

  // Ref for closing dropdown on outside click
  const dropdownRef = useRef(null);

  const navigate = useNavigate();
  const currentUser = localStorage.getItem('username');
  const token = localStorage.getItem('token');

  useEffect(() => {
    if (!token) { navigate('/login'); return; }
    fetchData();
  }, [token, activeTab]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowLogFilterDropdown(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [dropdownRef]);

  const fetchData = async () => {
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      
      if (activeTab === 'dashboard') {
        const statsRes = await axios.get(`${API_BASE_URL}/api/reports/dashboard-stats`, config);
        setReportStats(statsRes.data);
        const graphRes = await axios.get(`${API_BASE_URL}/api/reports/stats`, config);
        setGraphData(graphRes.data);
      }

      if (activeTab === 'users' || activeTab === 'experts' || activeTab === 'dashboard') {
        const userRes = await axios.get(`${API_BASE_URL}/api/users`, config);
        setUsers(userRes.data);
      } 
      
      if (activeTab === 'logs') {
        const logRes = await axios.get(`${API_BASE_URL}/api/logs`, config);
        setLogs(logRes.data);
      }

      setLoading(false);
    } catch (error) { 
      if(error.response?.status === 403) navigate('/login'); 
    }
  };

  const executeAction = async () => {
    const { action, id } = confirmModal;
    const config = { headers: { Authorization: `Bearer ${token}` } };

    try {
      if (action === 'delete') {
        await axios.delete(`${API_BASE_URL}/api/users/${id}`, config);
        setUsers(prev => prev.filter(u => u.id !== id));
      } else if (action === 'approve') {
        await axios.put(`${API_BASE_URL}/api/users/${id}/approve`, {}, config);
        setUsers(prev => prev.map(u => u.id === id ? { ...u, isApproved: true, approved: true } : u));
      }
    } catch (error) {
      alert("Action failed. Please check network connection.");
    }
    setConfirmModal({ isOpen: false, action: null, id: null, title: '', message: '' });
  };

  const handleLogout = () => { localStorage.clear(); navigate('/login'); };

  const displayUsers = users
    .filter(u => u.role && u.role.toLowerCase() !== 'expert')
    .filter(u => !userSearchTerm || u.username.toLowerCase().includes(userSearchTerm.toLowerCase()));

  const pendingExperts = users
    .filter(u => u.role && u.role.toLowerCase() === 'expert' && u.isApproved !== true && u.approved !== true)
    .filter(u => !userSearchTerm || u.username.toLowerCase().includes(userSearchTerm.toLowerCase()));

  const approvedExperts = users
    .filter(u => u.role && u.role.toLowerCase() === 'expert' && (u.isApproved === true || u.approved === true))
    .filter(u => !userSearchTerm || u.username.toLowerCase().includes(userSearchTerm.toLowerCase()));

  // 🔥 FIX: Robust & Accurate Date Logic for Logs Filtering
  const filteredLogs = logs.filter(l => {
    let matchSearch = true;
    if (logSearchTerm) {
      const searchLower = logSearchTerm.toLowerCase();
      matchSearch = (l.action?.toLowerCase().includes(searchLower)) || 
                    (l.performedBy?.toLowerCase().includes(searchLower)) ||
                    (l.details?.toLowerCase().includes(searchLower));
    }
    
    let matchDate = true;
    const logDate = parseDate(l.timestamp);
    const today = new Date();
    today.setHours(0, 0, 0, 0); // Start of today for accurate comparison

    if (logTimeframe === 'today') {
        const logDateMidnight = new Date(logDate);
        logDateMidnight.setHours(0, 0, 0, 0);
        matchDate = logDateMidnight.getTime() === today.getTime();
    } else if (logTimeframe === 'week') {
        const lastWeek = new Date(today);
        lastWeek.setDate(today.getDate() - 7);
        matchDate = logDate >= lastWeek;
    } else if (logTimeframe === 'specific' && logDateFilter) {
        // Format log date to YYYY-MM-DD for accurate string matching
        const localYYYYMMDD = `${logDate.getFullYear()}-${String(logDate.getMonth() + 1).padStart(2, '0')}-${String(logDate.getDate()).padStart(2, '0')}`;
        matchDate = localYYYYMMDD === logDateFilter;
    }

    return matchSearch && matchDate;
  });

  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      return (
        <div style={{ background: 'rgba(10, 15, 25, 0.95)', border: '1px solid #00E5FF', padding: '10px 15px', borderRadius: '8px', color: '#fff', boxShadow: '0 0 15px rgba(0, 229, 255, 0.2)' }}>
          <p style={{ margin: 0, fontWeight: 'bold', color: '#00E5FF', fontSize: '12px' }}>{`Date: ${label}`}</p>
          <p style={{ margin: 0, fontSize: '14px', marginTop: '5px' }}>{`Total Scans: ${payload[0].value}`}</p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="dashboard-layout" style={{background: '#050810', color: '#e0e0e0', fontFamily: "'Inter', sans-serif"}}>
      
      {/* CUSTOM CONFIRMATION MODAL */}
      <AnimatePresence>
        {confirmModal.isOpen && (
          <motion.div initial={{opacity: 0}} animate={{opacity: 1}} exit={{opacity: 0}}
            style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.8)', backdropFilter: 'blur(5px)', zIndex: 9999, display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
            <motion.div initial={{scale: 0.9, y: 20}} animate={{scale: 1, y: 0}} 
              style={{ background: '#0a0f1c', border: `1px solid ${confirmModal.action === 'delete' ? '#ff003c' : '#00E676'}`, padding: '30px', borderRadius: '12px', textAlign: 'center', maxWidth: '400px' }}>
              <FaExclamationCircle size={40} color={confirmModal.action === 'delete' ? '#ff003c' : '#00E676'} className="mb-3" />
              <h4 style={{ color: '#fff', fontWeight: 'bold', fontFamily: 'Orbitron' }}>{confirmModal.title}</h4>
              <p style={{ color: '#aaa', fontSize: '14px', marginBottom: '25px' }}>{confirmModal.message}</p>
              <div className="d-flex gap-3 justify-content-center">
                <button className="btn btn-outline-secondary px-4" onClick={() => setConfirmModal({isOpen: false})}>Cancel</button>
                <button className={`btn px-4 fw-bold ${confirmModal.action === 'delete' ? 'btn-danger' : 'btn-success'}`} onClick={executeAction}>
                  Confirm
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* SIDEBAR */}
      <aside className="sidebar" style={{borderRight: '1px solid rgba(0, 229, 255, 0.1)', background: 'rgba(5, 8, 15, 0.95)'}}>
        <div className="brand-section" style={{ padding: '25px 20px', borderBottom: '1px solid rgba(255,255,255,0.05)', textAlign: 'center' }}>
          <h3 style={{ fontFamily: "Orbitron", fontWeight: "bold", letterSpacing: "2px", color: "#fff", margin: 0, fontSize: "2.2rem" }}>
            PRO<span style={{ color: "#00E5FF" }}>HORY</span></h3>
          <small style={{ color: '#00E5FF', letterSpacing: '1px', fontSize: '10px', fontWeight: 'bold' }}>ADMINISTRATION</small>
        </div>
        
        <ul className="nav-links" style={{ marginTop: '20px', listStyle: "none", padding: '0 10px' }}>
          <li className="nav-item" onClick={() => setActiveTab('dashboard')} style={{ padding: '12px 20px', marginBottom: '8px', borderRadius: '8px', cursor: 'pointer', display: 'flex', alignItems: 'center', transition: '0.3s', ...(activeTab === 'dashboard' ? {color: '#00E5FF', background: 'rgba(0, 229, 255, 0.1)'} : { color: '#8892b0' }) }}>
            <FaTachometerAlt /> <span style={{ marginLeft: '12px', fontSize: '13px', fontWeight: '600' }}>Dashboard</span>
          </li>
          <li className="nav-item" onClick={() => {setActiveTab('users'); setUserSearchTerm('');}} style={{ padding: '12px 20px', marginBottom: '8px', borderRadius: '8px', cursor: 'pointer', display: 'flex', alignItems: 'center', transition: '0.3s', ...(activeTab === 'users' ? {color: '#9d00ff', background: 'rgba(157, 0, 255, 0.1)'} : { color: '#8892b0' }) }}>
            <FaUsers /> <span style={{ marginLeft: '12px', fontSize: '13px', fontWeight: '600' }}>Manage Users</span>
          </li>
          <li className="nav-item" onClick={() => {setActiveTab('experts'); setUserSearchTerm('');}} style={{ padding: '12px 20px', marginBottom: '8px', borderRadius: '8px', cursor: 'pointer', display: 'flex', alignItems: 'center', transition: '0.3s', ...(activeTab === 'experts' ? {color: '#00E676', background: 'rgba(0, 230, 118, 0.1)'} : { color: '#8892b0' }) }}>
            <FaUserShield /> <span style={{ marginLeft: '12px', fontSize: '13px', fontWeight: '600' }}>Manage Experts</span>
          </li>
          <li className="nav-item" onClick={() => setActiveTab('logs')} style={{ padding: '12px 20px', marginBottom: '8px', borderRadius: '8px', cursor: 'pointer', display: 'flex', alignItems: 'center', transition: '0.3s', ...(activeTab === 'logs' ? {color: '#FFEA00', background: 'rgba(255, 234, 0, 0.1)'} : { color: '#8892b0' }) }}>
            <FaServer /> <span style={{ marginLeft: '12px', fontSize: '13px', fontWeight: '600' }}>System Logs</span>
          </li>
        </ul>

        <div className="user-profile-mini mt-auto" style={{ borderTop: '1px solid rgba(255,255,255,0.05)', padding: '20px' }}>
          <div className="d-flex align-items-center gap-3 mb-3">
            <div style={{background: 'rgba(255,234,0,0.1)', padding: '10px', borderRadius: '50%'}}>
              <FaUserShield style={{ color: "#FFEA00", fontSize: '1.5rem' }} />
            </div>
            <div>
              <h6 style={{ color: "#fff", margin: 0, fontWeight: 'bold', fontSize: '14px' }}>{currentUser}</h6>
              <small style={{ color: "#FFEA00", fontSize: '10px', letterSpacing: '1px' }}>ROOT ADMIN</small>
            </div>
          </div>
          <button onClick={handleLogout} className="btn btn-outline-danger w-100 btn-sm fw-bold" style={{ borderRadius: '6px', fontSize: '12px', padding: '10px', letterSpacing: '1px' }}>
            <FaSignOutAlt className="me-2"/> LOGOUT
          </button>
        </div>
      </aside>

      <main className="main-content custom-scrollbar" style={{ padding: '30px', flex: 1, overflowY: 'auto' }}>
        
        <header className="dashboard-header d-flex justify-content-between align-items-center mb-4 pb-3" style={{borderBottom: '1px solid rgba(255,255,255,0.05)'}}>
          <div className="d-flex align-items-center gap-3">
            <h2 style={{fontFamily: 'Orbitron', color: '#fff', fontSize: '20px', textTransform: 'uppercase', letterSpacing: '1px', fontWeight: 'bold', margin: 0}}>
              {activeTab === 'dashboard' ? 'Overview' : activeTab === 'users' ? 'User Database' : activeTab === 'experts' ? 'Expert Directory' : 'Server Logs'}
            </h2>
            <div className="badge d-flex align-items-center gap-2 px-3 py-2" style={{background: 'rgba(0, 230, 118, 0.1)', color: '#00E676', border: '1px solid rgba(0, 230, 118, 0.3)'}}>
                <div className="spinner-grow spinner-grow-sm" role="status" style={{width: '10px', height: '10px'}}></div>
                SYSTEM ONLINE
            </div>
          </div>

          <div className="header-actions d-flex gap-3">
            {(activeTab === 'users' || activeTab === 'experts' || activeTab === 'logs') && (
              <div className="input-group" style={{maxWidth: '300px'}}>
                <span className="input-group-text border-0" style={{ background: 'rgba(255,255,255,0.05)', color: '#8892b0' }}><FaSearch /></span>
                <input 
                  type="text" className="form-control border-0 text-white shadow-none" 
                  style={{background: 'rgba(255,255,255,0.05)', fontSize: '13px'}}
                  placeholder="Search database..." value={activeTab === 'logs' ? logSearchTerm : userSearchTerm}
                  onChange={(e) => activeTab === 'logs' ? setLogSearchTerm(e.target.value) : setUserSearchTerm(e.target.value)}
                />
              </div>
            )}
          </div>
        </header>

        <div className="content-body">
          
          {/* TAB: DASHBOARD */}
          {activeTab === 'dashboard' && (
            <>
              <div className="row g-4 mb-4">
                <div className="col-md-4">
                  <div className="cyber-card p-4" style={{borderColor: 'rgba(157, 0, 255, 0.3)', background: 'rgba(157, 0, 255, 0.03)', borderRadius: '12px'}}>
                    <h6 style={{ color: '#9d00ff', fontWeight: 'bold', margin: 0, fontSize: '12px', letterSpacing: '1px' }}><FaSearch className="me-2"/> TOTAL SCANS DONE</h6>
                    <div className="stat-number mt-3" style={{ color: '#ffffff', fontSize: '3rem', fontWeight: 'bold', fontFamily: 'Orbitron' }}>{reportStats.totalScans}</div>
                  </div>
                </div>
                <div className="col-md-4">
                  <div className="cyber-card p-4" style={{borderColor: 'rgba(255, 0, 60, 0.3)', background: 'rgba(255, 0, 60, 0.03)', borderRadius: '12px'}}>
                    <h6 style={{ color: '#ff003c', fontWeight: 'bold', margin: 0, fontSize: '12px', letterSpacing: '1px' }}><FaBug className="me-2"/> THREATS BLOCKED</h6>
                    <div className="stat-number mt-3" style={{ color: '#ff003c', fontSize: '3rem', fontWeight: 'bold', fontFamily: 'Orbitron' }}>{reportStats.blockedScams}</div>
                  </div>
                </div>
                <div className="col-md-4">
                  <div className="cyber-card p-4" style={{borderColor: 'rgba(255, 234, 0, 0.3)', background: 'rgba(255, 234, 0, 0.03)', borderRadius: '12px'}}>
                    <h6 style={{ color: '#FFEA00', fontWeight: 'bold', margin: 0, fontSize: '12px', letterSpacing: '1px' }}><FaShieldAlt className="me-2"/> SAFE CONTENT</h6>
                    <div className="stat-number mt-3" style={{ color: '#ffffff', fontSize: '3rem', fontWeight: 'bold', fontFamily: 'Orbitron' }}>{reportStats.safeLinks}</div>
                  </div>
                </div>
              </div>

              <div className="row g-4 mb-4">
                <div className="col-md-4">
                  <div className="cyber-card p-4 d-flex align-items-center justify-content-between" style={{borderColor: 'rgba(0, 229, 255, 0.2)', background: 'rgba(0, 229, 255, 0.02)', borderRadius: '12px'}}>
                    <div>
                        <h6 style={{ color: '#00f3ff', fontWeight: 'bold', margin: 0, fontSize: '11px', letterSpacing: '1px' }}>REGISTERED USERS</h6>
                        <div className="stat-number mt-2" style={{ color: '#ffffff', fontSize: '2rem', fontWeight: 'bold' }}>{users.filter(u => u.role && u.role.toLowerCase() !== 'expert').length}</div>
                    </div>
                    <FaUsers size={40} color="rgba(0, 243, 255, 0.2)"/>
                  </div>
                </div>
                <div className="col-md-4">
                  <div className="cyber-card p-4 d-flex align-items-center justify-content-between" style={{borderColor: 'rgba(0, 230, 118, 0.2)', background: 'rgba(0, 230, 118, 0.02)', borderRadius: '12px'}}>
                    <div>
                        <h6 style={{ color: '#00E676', fontWeight: 'bold', margin: 0, fontSize: '11px', letterSpacing: '1px' }}>APPROVED EXPERTS</h6>
                        <div className="stat-number mt-2" style={{ color: '#ffffff', fontSize: '2rem', fontWeight: 'bold' }}>{approvedExperts.length}</div>
                    </div>
                    <FaUserShield size={40} color="rgba(0, 230, 118, 0.2)"/>
                  </div>
                </div>
                <div className="col-md-4">
                  <div className="cyber-card p-4 d-flex align-items-center justify-content-between" style={{borderColor: 'rgba(255, 234, 0, 0.2)', background: 'rgba(255, 234, 0, 0.02)', borderRadius: '12px'}}>
                    <div>
                        <h6 style={{ color: '#FFEA00', fontWeight: 'bold', margin: 0, fontSize: '11px', letterSpacing: '1px' }}>PENDING EXPERTS</h6>
                        <div className="stat-number mt-2" style={{ color: '#FFEA00', fontSize: '2rem', fontWeight: 'bold' }}>{pendingExperts.length}</div>
                    </div>
                    <FaClock size={40} color="rgba(255, 234, 0, 0.2)"/>
                  </div>
                </div>
              </div>

              {/* Advanced Graph */}
              <div className="cyber-card p-4 mt-2" style={{height: '450px', background: 'rgba(10, 15, 25, 0.6)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: '12px'}}>
                <h6 style={{ color: '#00E5FF', fontWeight: 'bold', marginBottom: '30px', fontSize: '14px', letterSpacing: '1px' }}><FaHistory className="me-2"/> TRAFFIC HISTORY</h6>
                <ResponsiveContainer width="100%" height="90%">
                  <ComposedChart data={graphData}>
                    <defs>
                      <linearGradient id="colorTraffic" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#00E5FF" stopOpacity={0.5}/>
                        <stop offset="95%" stopColor="#00E5FF" stopOpacity={0}/>
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                    <XAxis dataKey="date" stroke="#64748b" fontSize={11} tickLine={false} axisLine={false} />
                    <YAxis stroke="#64748b" fontSize={11} tickLine={false} axisLine={false} />
                    <Tooltip content={<CustomTooltip />} cursor={{fill: 'rgba(255,255,255,0.02)'}} />
                    <Area type="monotone" dataKey="count" stroke="none" fillOpacity={1} fill="url(#colorTraffic)" />
                    <Line type="monotone" dataKey="count" stroke="#00E5FF" strokeWidth={3} dot={{r: 4, fill: '#00E5FF', strokeWidth: 0}} activeDot={{r: 6, fill: '#fff'}} />
                  </ComposedChart>
                </ResponsiveContainer>
              </div>
            </>
          )}

          {/* TAB: USERS */}
          {activeTab === 'users' && (
             <div className="cyber-card p-4" style={{ background: 'rgba(10, 15, 25, 0.6)', border: '1px solid rgba(157, 0, 255, 0.2)', borderRadius: '12px' }}>
               <h6 style={{ color: '#9d00ff', fontWeight: 'bold', marginBottom: '20px', fontSize: '14px', letterSpacing: '1px' }}><FaUsers className="me-2"/> REGISTERED USERS</h6>
               <div className="table-responsive">
                 <table style={{ width: '100%', borderCollapse: 'collapse', color: '#fff' }}>
                   <thead>
                     <tr style={{ color: '#9d00ff', borderBottom: '1px solid rgba(157,0,255,0.2)', fontSize: '12px', textAlign: 'left' }}>
                       <th className="pb-3 px-2">ID</th><th className="pb-3">NAME</th><th className="pb-3">EMAIL</th><th className="pb-3">ROLE</th><th className="pb-3 text-end">ACTION</th>
                     </tr>
                   </thead>
                   <tbody>
                     {displayUsers.map(u => (
                       <tr key={u.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.03)', transition: '0.2s' }} className="hover-row">
                         <td style={{ color: '#64748b', padding: '15px 10px', fontSize: '13px' }}>#{u.id}</td>
                         <td style={{ color: '#e2e8f0', fontWeight: 'bold', fontSize: '14px' }}>{u.username}</td>
                         <td style={{ color: '#94a3b8', fontSize: '13px' }}>{u.email || 'N/A'}</td>
                         <td><span className="badge px-3 py-2" style={{ background: 'rgba(157, 0, 255, 0.1)', color: '#9d00ff', border: '1px solid rgba(157, 0, 255, 0.3)' }}>{u.role.toUpperCase()}</span></td>
                         <td className="text-end">
                           <button onClick={() => setConfirmModal({isOpen: true, action: 'delete', id: u.id, title: 'Remove User', message: `Remove ${u.username} from system?`})} className="btn btn-sm btn-outline-danger shadow-none fw-bold" style={{fontSize: '11px'}}><FaTrashAlt className="me-1"/> REMOVE</button>
                         </td>
                       </tr>
                     ))}
                   </tbody>
                 </table>
                 {displayUsers.length === 0 && <p className="text-center text-secondary py-4">No users found.</p>}
               </div>
             </div>
          )}

          {/* TAB: EXPERTS */}
          {activeTab === 'experts' && (
            <div className="cyber-card p-4" style={{ background: 'rgba(10, 15, 25, 0.6)', border: '1px solid rgba(0, 230, 118, 0.2)', borderRadius: '12px' }}>
              
              <div className="d-flex justify-content-between align-items-center mb-4">
                <h6 style={{ color: '#00E676', fontWeight: 'bold', margin: 0, fontSize: '14px', letterSpacing: '1px' }}><FaUserShield className="me-2"/> EXPERT MANAGEMENT</h6>
                <div className="btn-group border border-secondary rounded" role="group">
                  <button type="button" className={`btn btn-sm ${expertTab === 'pending' ? 'btn-warning text-dark fw-bold' : 'text-light'}`} onClick={() => setExpertTab('pending')}>
                    Pending ({pendingExperts.length})
                  </button>
                  <button type="button" className={`btn btn-sm ${expertTab === 'approved' ? 'btn-success text-dark fw-bold' : 'text-light'}`} onClick={() => setExpertTab('approved')}>
                    Approved ({approvedExperts.length})
                  </button>
                </div>
              </div>

              <div className="table-responsive">
                <table style={{ width: '100%', borderCollapse: 'collapse', color: '#fff' }}>
                  <thead>
                    <tr style={{ color: '#00E676', borderBottom: '1px solid rgba(0,230,118,0.2)', fontSize: '12px', textAlign: 'left' }}>
                      <th className="pb-3 px-2">ID</th><th className="pb-3">NAME</th><th className="pb-3">EMAIL</th>
                      {expertTab === 'approved' && <th className="pb-3">RATING</th>}
                      <th className="pb-3">DOCUMENTS</th><th className="pb-3 text-end">ACTION</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(expertTab === 'pending' ? pendingExperts : approvedExperts).map(u => {
                      const avgRating = u.averageRating ? u.averageRating.toFixed(1) : "0.0";
                      return (
                        <tr key={u.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.03)' }} className="hover-row">
                          <td style={{ color: '#64748b', padding: '15px 10px', fontSize: '13px' }}>#{u.id}</td>
                          <td style={{ color: '#e2e8f0', fontWeight: 'bold', fontSize: '14px' }}>{u.username}</td>
                          <td style={{ color: '#94a3b8', fontSize: '13px' }}>{u.email || 'N/A'}</td>
                          
                          {expertTab === 'approved' && (
                            <td>
                              <div className="d-flex align-items-center gap-1" style={{color: '#ffca00', fontSize: '13px', fontWeight: 'bold'}}>
                                <FaStar/> {avgRating} <span style={{color: '#64748b', fontSize: '10px'}}>({u.ratingCount || 0})</span>
                              </div>
                            </td>
                          )}

                          <td>
                            {u.profileImage ? (
                              <button onClick={() => setSelectedImage(u.profileImage)} className="btn btn-sm btn-outline-info" style={{ fontSize: '11px', fontWeight: "bold" }}>VIEW CV/NID</button>
                            ) : <span style={{ color: '#64748b', fontSize: '11px' }}>NO FILE</span>}
                          </td>
                          <td className="text-end">
                            <div className="d-flex gap-2 justify-content-end">
                              {expertTab === 'pending' && (
                                <button onClick={() => setConfirmModal({isOpen: true, action: 'approve', id: u.id, title: 'Approve Expert', message: `Grant ${u.username} access to the system?`})} className="btn btn-sm btn-success fw-bold" style={{ fontSize: '11px', background: '#00E676', color: '#000', border: 'none' }}>APPROVE</button>
                              )}
                              <button onClick={() => setConfirmModal({isOpen: true, action: 'delete', id: u.id, title: 'Remove Account', message: `Remove ${u.username} from system?`})} className="btn btn-sm btn-outline-danger fw-bold" style={{ fontSize: '11px' }}>{expertTab === 'pending' ? 'REJECT' : 'REMOVE'}</button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
                {(expertTab === 'pending' ? pendingExperts : approvedExperts).length === 0 && <p className="text-center text-secondary py-4">No records found.</p>}
              </div>
            </div>
          )}

          {/* 🔥 FIX: ADVANCED SYSTEM LOGS FILTER UI */}
          {activeTab === 'logs' && (
            <div className="cyber-card p-4" style={{ background: 'rgba(10, 15, 25, 0.6)', border: '1px solid rgba(255, 234, 0, 0.2)', borderRadius: '12px' }}>
              <div className="d-flex justify-content-between align-items-center mb-4">
                <h6 style={{ color: '#FFEA00', fontWeight: 'bold', margin: 0, fontSize: '14px', letterSpacing: '1px' }}><FaList className="me-2"/> SYSTEM EVENT LOGS</h6>
                
                {/* 🔥 Custom Cyber Dropdown Filter */}
                <div className="d-flex gap-2 align-items-center" ref={dropdownRef}>
                  <div className="position-relative">
                    <button 
                      className="btn shadow-none fw-bold d-flex align-items-center justify-content-between gap-2 transition-all" 
                      style={{ 
                        background: showLogFilterDropdown ? 'rgba(0, 229, 255, 0.2)' : 'rgba(0, 229, 255, 0.05)', 
                        border: '1px solid rgba(0, 229, 255, 0.4)', 
                        color: '#00E5FF', 
                        fontSize: '12px', 
                        padding: '10px 15px',
                        borderRadius: '6px',
                        minWidth: '180px'
                      }}
                      onClick={() => setShowLogFilterDropdown(!showLogFilterDropdown)}
                    >
                      <span>
                        <FaFilter className="me-2" /> 
                        {logTimeframe === 'all' ? 'ALL TIMEFRAME' : logTimeframe === 'today' ? 'TODAY' : logTimeframe === 'week' ? 'THIS WEEK' : 'SPECIFIC DATE'}
                      </span>
                      <motion.div animate={{ rotate: showLogFilterDropdown ? 180 : 0 }}><FaChevronDown /></motion.div>
                    </button>

                    {/* Animated Dropdown Menu */}
                    <AnimatePresence>
                      {showLogFilterDropdown && (
                        <motion.div 
                          initial={{ opacity: 0, y: 10, scale: 0.95 }} 
                          animate={{ opacity: 1, y: 0, scale: 1 }} 
                          exit={{ opacity: 0, y: 10, scale: 0.95 }}
                          style={{ 
                            position: 'absolute', top: '110%', right: 0, background: '#0a0f1c', 
                            border: '1px solid rgba(0, 229, 255, 0.3)', borderRadius: '8px', zIndex: 100, 
                            minWidth: '180px', overflow: 'hidden', boxShadow: '0 10px 30px rgba(0,0,0,0.8)' 
                          }}
                        >
                          {['all', 'today', 'week', 'specific'].map(tf => (
                            <div 
                              key={tf}
                              onClick={() => { setLogTimeframe(tf); setShowLogFilterDropdown(false); }}
                              style={{ 
                                padding: '12px 15px', cursor: 'pointer', fontSize: '12px', fontWeight: 'bold',
                                color: logTimeframe === tf ? '#000' : '#8892b0', 
                                background: logTimeframe === tf ? '#00E5FF' : 'transparent', 
                                borderBottom: '1px solid rgba(255,255,255,0.02)' 
                              }}
                              className="dropdown-hover-item"
                            >
                              {tf === 'all' && '⏳ ALL TIMEFRAME'}
                              {tf === 'today' && '📅 TODAY'}
                              {tf === 'week' && '📅 THIS WEEK'}
                              {tf === 'specific' && '🎯 SPECIFIC DATE'}
                            </div>
                          ))}
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                  
                  {/* Date Picker Input */}
                  <AnimatePresence>
                    {logTimeframe === 'specific' && (
                      <motion.input 
                        initial={{ opacity: 0, width: 0 }} animate={{ opacity: 1, width: 'auto' }} exit={{ opacity: 0, width: 0 }}
                        type="date" 
                        className="form-control shadow-none text-white fw-bold" 
                        style={{ background: 'rgba(0, 229, 255, 0.05)', border: '1px solid rgba(0, 229, 255, 0.4)', fontSize: '12px', colorScheme: 'dark', padding: '10px' }} 
                        value={logDateFilter} 
                        onChange={(e) => setLogDateFilter(e.target.value)}
                      />
                    )}
                  </AnimatePresence>
                </div>
              </div>

              <div style={{maxHeight: '600px', overflowY: 'auto'}} className="custom-scrollbar pr-2 mt-4">
                  <table style={{ width: '100%', borderCollapse: 'collapse', color: '#fff' }}>
                    <thead>
                      <tr style={{ color: '#FFEA00', borderBottom: '1px solid rgba(255,234,0,0.2)', position: 'sticky', top: 0, background: '#0a0f19', zIndex: 1, fontSize: '12px', textAlign: 'left' }}>
                        <th className="pb-3 px-2" style={{width: '20%'}}>TIME</th>
                        <th className="pb-3" style={{width: '20%'}}>ACTION</th>
                        <th className="pb-3" style={{width: '20%'}}>USER/AGENT</th>
                        <th className="pb-3" style={{width: '40%'}}>DETAILS</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredLogs.map(l => (
                      <tr key={l.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.03)' }} className="hover-row">
                        <td style={{ color: '#94a3b8', padding: '15px 10px', fontSize: '12px', verticalAlign: 'top' }}>{parseDate(l.timestamp).toLocaleString()}</td>
                        <td style={{ color: '#FFEA00', fontWeight: 'bold', fontSize: '13px', verticalAlign: 'top', paddingTop: '15px' }}>{l.action}</td>
                        <td style={{ color: '#e2e8f0', fontWeight: "bold", fontSize: '13px', verticalAlign: 'top', paddingTop: '15px' }}>{l.performedBy}</td>
                        <td style={{ color: '#e2e8f0', fontSize: '13.5px', verticalAlign: 'top', paddingTop: '15px', paddingBottom: '15px', wordBreak: 'break-word', lineHeight: '1.6' }}>
                           {l.details}
                        </td>
                      </tr>
                    ))}
                    </tbody>
                  </table>
                  {filteredLogs.length === 0 && (
                    <div className="text-center py-5">
                      <FaSearch size={40} color="rgba(255,255,255,0.1)" className="mb-3" />
                      <p className="text-secondary m-0">No matching events found for the selected filter.</p>
                    </div>
                  )}
              </div>
            </div>
          )}
        </div>
      </main>

      {/* ENHANCED DOCUMENT VIEW MODAL (WITH DOWNLOAD) */}
      <AnimatePresence>
        {selectedImage && (
          <motion.div 
            initial={{opacity: 0}} animate={{opacity: 1}} exit={{opacity: 0}}
            style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(3,5,10,0.95)', backdropFilter: 'blur(10px)', zIndex: 9999, display: 'flex', justifyContent: 'center', alignItems: 'center', flexDirection: 'column' }}
          >
            <motion.img 
              initial={{scale: 0.9}} animate={{scale: 1}}
              src={selectedImage} style={{ maxWidth: '80%', maxHeight: '75%', border: '1px solid #00E5FF', boxShadow: '0 0 30px rgba(0,229,255,0.3)', borderRadius: '12px', objectFit: 'contain' }} alt="Expert Document" 
            />
            <div className="d-flex gap-3 mt-4">
                <a href={selectedImage} download="Expert_Credential" target="_blank" rel="noreferrer" className="btn btn-info px-4 py-2 fw-bold d-flex align-items-center gap-2" style={{ borderRadius: '8px', color: '#000' }}>
                    <FaDownload /> DOWNLOAD FILE
                </a>
                <button className="btn btn-outline-danger px-4 py-2 fw-bold" style={{ borderRadius: '8px', letterSpacing: '1px' }} onClick={() => setSelectedImage(null)}>
                    CLOSE
                </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
      
      <style>{`
        .hover-row:hover {
          background-color: rgba(255, 255, 255, 0.02) !important;
        }
        .dropdown-hover-item:hover {
          background-color: rgba(0, 229, 255, 0.1) !important;
          color: #00E5FF !important;
        }
      `}</style>
    </div>
  );
}

export default AdminDashboard;