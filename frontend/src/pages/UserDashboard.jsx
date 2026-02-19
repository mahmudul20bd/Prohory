import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  FaThLarge, FaSearch, FaShieldAlt, FaHistory, FaCog, FaSignOutAlt, 
  FaUserCircle, FaNetworkWired, FaBolt, FaCheckCircle, FaBullhorn, FaExclamationTriangle, FaBan
} from 'react-icons/fa';
import '../styles/Dashboard.css';

function UserDashboard() {
  const [activeTab, setActiveTab] = useState('overview');
  const [message, setMessage] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [history, setHistory] = useState([]);
  const [stats, setStats] = useState({ total: 0, threats: 0, safe: 0 });
  
  const navigate = useNavigate();
  const username = localStorage.getItem('username') || 'Agent';
  const role = localStorage.getItem('role') || 'User';
  const token = localStorage.getItem('token');

  // --- 1. CONFIGURATION FOR 4 CATEGORIES ---
  // এখানে ৪টি ক্যাটাগরির জন্য রঙ, টেক্সট এবং আইকন সেট করা হয়েছে
  const getResultStyle = (label) => {
    const normalizedLabel = label ? label.toUpperCase() : '';
    
    switch (normalizedLabel) {
      case 'SAFE':
        return {
          color: '#00ff9d', // Green
          bg: 'rgba(0, 255, 157, 0.1)',
          border: '#00ff9d',
          title: 'CLEAN MESSAGE',
          icon: <FaCheckCircle className="fs-1 mb-3" style={{color: '#00ff9d'}} />
        };
      case 'PROMO':
        return {
          color: '#00f3ff', // Blue
          bg: 'rgba(0, 243, 255, 0.1)',
          border: '#00f3ff',
          title: 'PROMOTIONAL',
          icon: <FaBullhorn className="fs-1 mb-3" style={{color: '#00f3ff'}} />
        };
      case 'SPAM':
        return {
          color: '#ffca00', // Yellow/Orange
          bg: 'rgba(255, 202, 0, 0.1)',
          border: '#ffca00',
          title: 'SPAM DETECTED',
          icon: <FaExclamationTriangle className="fs-1 mb-3" style={{color: '#ffca00'}} />
        };
      case 'DANGER':
      case 'SCAM':
        return {
          color: '#ff003c', // Red
          bg: 'rgba(255, 0, 60, 0.1)',
          border: '#ff003c',
          title: 'THREAT DETECTED',
          icon: <FaBan className="fs-1 mb-3" style={{color: '#ff003c'}} />
        };
      default:
        return {
          color: '#ffffff',
          bg: 'rgba(255, 255, 255, 0.1)',
          border: '#ffffff',
          title: 'UNKNOWN RESULT',
          icon: <FaShieldAlt className="fs-1 mb-3" />
        };
    }
  };

  // --- 2. FETCH DATA ---
  useEffect(() => {
    if (!token) {
      navigate('/login');
      return;
    }
    fetchUserData();
  }, [token, navigate]);

  const fetchUserData = async () => {
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      const res = await axios.get(`http://localhost:9090/api/reports/user?username=${username}`, config);
      const data = res.data;
      
      setHistory(data);
      
      // Stats Logic
      const threatCount = data.filter(item => 
        ['DANGER', 'SCAM'].includes(item.aiResult)
      ).length;

      setStats({
        total: data.length,
        threats: threatCount,
        safe: data.length - threatCount
      });

    } catch (error) {
      console.error("Dashboard Load Error", error);
      if (error.response && error.response.status === 403) {
        alert("Session Expired.");
        handleLogout();
      }
    }
  };

  // --- 3. SCANNING LOGIC ---
  const handleScan = async () => {
    if (!message) return;
    setLoading(true);
    setResult(null);

    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      
      const res = await axios.post('http://localhost:9090/api/report', {
        sender: username,
        message: message,
        reporter: username
      }, config);
      
      setResult(res.data);
      fetchUserData(); 
    } catch (error) {
      console.error(error);
      alert("Scan Failed. Check connection.");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  return (
    <div className="dashboard-layout">
      
      {/* SIDEBAR */}
      <aside className="sidebar">
        <div className="brand-section">
          <h3 style={{fontFamily: 'Orbitron', margin: 0}}>CYBER<span style={{color: '#00f3ff'}}>AWARE</span></h3>
          <small className="text-secondary" style={{letterSpacing: '2px'}}>INTELLIGENCE</small>
        </div>

        <ul className="nav-links">
          <li className={`nav-item ${activeTab === 'overview' ? 'active' : ''}`} onClick={() => setActiveTab('overview')}>
            <FaThLarge /> OVERVIEW
          </li>
          <li className={`nav-item ${activeTab === 'scan' ? 'active' : ''}`} onClick={() => setActiveTab('scan')}>
            <FaSearch /> AI SCANNER
          </li>
          <li className={`nav-item ${activeTab === 'history' ? 'active' : ''}`} onClick={() => setActiveTab('history')}>
            <FaHistory /> HISTORY LOGS
          </li>
          <li className="nav-item" onClick={() => navigate('/settings')}>
  <FaCog /> SETTINGS
</li>
        </ul>

        <div className="user-profile-mini">
          <div className="d-flex align-items-center gap-3 mb-3">
            <FaUserCircle className="fs-1 text-secondary" />
            <div>
              <h6 className="m-0 text-white">{username}</h6>
              <small className="text-info">{role.toUpperCase()} LEVEL 1</small>
            </div>
          </div>
          <button onClick={handleLogout} className="btn btn-outline-danger w-100 btn-sm rounded-0">
            <FaSignOutAlt /> LOGOUT SESSION
          </button>
        </div>
      </aside>

      {/* MAIN CONTENT */}
      <main className="main-content">
        <header className="dashboard-header">
          <div>
            <h2 className="m-0" style={{fontFamily: 'Orbitron'}}>
              {activeTab === 'overview' && 'SYSTEM OVERVIEW'}
              {activeTab === 'scan' && 'THREAT DETECTION'}
              {activeTab === 'history' && 'ACTIVITY LOGS'}
            </h2>
            <small className="text-secondary">Connected to Secure Node: 192.168.X.X</small>
          </div>
          <div className="d-flex gap-3">
            <div className="cyber-card p-2 px-4 d-flex align-items-center gap-2">
              <div className="spinner-grow spinner-grow-sm text-success" role="status"></div>
              <small>SYSTEM ONLINE</small>
            </div>
          </div>
        </header>

        <div className="content-body">
          
          {/* VIEW: OVERVIEW */}
          {activeTab === 'overview' && (
            <motion.div initial={{opacity:0}} animate={{opacity:1}} className="row g-4">
              <div className="col-md-4">
                <div className="cyber-card">
                  <div className="d-flex justify-content-between text-info">
                    <h6>TOTAL SCANS</h6>
                    <FaNetworkWired />
                  </div>
                  <div className="stat-number">{stats.total}</div>
                </div>
              </div>
              <div className="col-md-4">
                <div className="cyber-card" style={{borderColor: '#ff003c'}}>
                  <div className="d-flex justify-content-between text-danger">
                    <h6>THREATS FOUND</h6>
                    <FaBolt />
                  </div>
                  <div className="stat-number text-danger">{stats.threats}</div>
                </div>
              </div>
              <div className="col-md-4">
                <div className="cyber-card" style={{borderColor: '#00ff9d'}}>
                  <div className="d-flex justify-content-between text-success">
                    <h6>SAFE MESSAGES</h6>
                    <FaShieldAlt />
                  </div>
                  <div className="stat-number text-success">{stats.safe}</div>
                </div>
              </div>

              <div className="col-12 mt-4">
                <div className="cyber-card">
                  <h5 className="mb-3 text-white">RECENT SYSTEM ACTIVITY</h5>
                  <table className="cyber-table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>MESSAGE CONTENT</th>
                        <th>AI VERDICT</th>
                        <th>STATUS</th>
                      </tr>
                    </thead>
                    <tbody>
                      {history.slice(-5).reverse().map(log => {
                        const style = getResultStyle(log.aiResult);
                        return (
                          <tr key={log.id}>
                            <td className="text-secondary">#{log.id}</td>
                            <td className="text-truncate" style={{maxWidth: '300px'}}>{log.message}</td>
                            <td style={{color: style.color, fontWeight: 'bold'}}>
                              {log.aiResult}
                            </td>
                            <td><span className="badge bg-secondary">LOGGED</span></td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </motion.div>
          )}

          {/* VIEW: SCANNER - 4 Category Display */}
          {activeTab === 'scan' && (
            <motion.div initial={{opacity:0, y:20}} animate={{opacity:1, y:0}} className="row justify-content-center">
              <div className="col-lg-8">
                <div className="cyber-card p-5">
                  <div className="text-center mb-4">
                    <FaShieldAlt className="fs-1 text-info mb-3" />
                    <h4>AI NEURAL SCANNER</h4>
                    <p className="text-secondary">Paste text, email body, or links below to detect phishing.</p>
                  </div>
                  
                  <textarea 
                    className="scan-box" 
                    rows="6" 
                    placeholder="Enter suspicious content here..."
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                  ></textarea>
                  
                  <button 
                    onClick={handleScan} 
                    className="btn btn-info w-100 py-3 fw-bold rounded-0"
                    style={{boxShadow: '0 0 15px rgba(0,243,255,0.4)'}}
                    disabled={loading}
                  >
                    {loading ? 'ANALYZING PATTERNS...' : 'INITIATE SCAN'}
                  </button>

                  <AnimatePresence>
                    {result && (() => {
                      const style = getResultStyle(result.aiResult);
                      return (
                        <motion.div 
                          initial={{opacity:0, scale: 0.9}} 
                          animate={{opacity:1, scale: 1}} 
                          className="mt-4 p-4 text-center border rounded"
                          style={{
                            borderColor: style.border,
                            backgroundColor: style.bg
                          }}
                        >
                          {style.icon}
                          <h2 style={{color: style.color, fontFamily: 'Orbitron'}}>
                            {style.title}
                          </h2>
                          
                          <p className="m-0 text-white mt-2">
                            AI Verdict: <strong style={{color: style.color}}>{result.aiResult}</strong>
                          </p>
                        </motion.div>
                      );
                    })()}
                  </AnimatePresence>
                </div>
              </div>
            </motion.div>
          )}

          {/* VIEW: HISTORY */}
          {activeTab === 'history' && (
            <motion.div initial={{opacity:0}} animate={{opacity:1}}>
              <div className="cyber-card">
                <h5 className="mb-4">FULL SECURITY LOGS</h5>
                <div style={{maxHeight: '600px', overflowY: 'auto'}}>
                  <table className="cyber-table">
                    <thead>
                      <tr>
                        <th>SCAN ID</th>
                        <th>CONTENT PREVIEW</th>
                        <th>RESULT</th>
                      </tr>
                    </thead>
                    <tbody>
                      {history.slice().reverse().map(log => {
                        const style = getResultStyle(log.aiResult);
                        return (
                          <tr key={log.id}>
                            <td className="text-secondary">SCAN-{log.id}</td>
                            <td>{log.message}</td>
                            <td>
                              <span className="badge" style={{
                                backgroundColor: style.bg,
                                color: style.color,
                                border: `1px solid ${style.border}`
                              }}>
                                {log.aiResult}
                              </span>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </motion.div>
          )}

        </div>
      </main>
    </div>
  );
}

export default UserDashboard;