import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { 
  FaUsers, FaUserShield, FaTrashAlt, FaSignOutAlt, FaServer, 
  FaUserEdit, FaCog, FaList, FaChartLine 
} from 'react-icons/fa';
import '../styles/Dashboard.css';

function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('users');
  const [users, setUsers] = useState([]);
  const [logs, setLogs] = useState([]);
  const [graphData, setGraphData] = useState([]);
  const [stats, setStats] = useState({ totalUsers: 0, experts: 0, admins: 0 });
  const [loading, setLoading] = useState(true);
  
  const navigate = useNavigate();
  const currentUser = localStorage.getItem('username');
  const token = localStorage.getItem('token');

  useEffect(() => {
    if (!token) { navigate('/login'); return; }
    fetchData();
  }, [token, activeTab]);

  const fetchData = async () => {
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      
      if (activeTab === 'users') {
        const userRes = await axios.get('https://cyber-aware.onrender.com/api/users', config);
        setUsers(userRes.data);
        setStats({
          totalUsers: userRes.data.length,
          experts: userRes.data.filter(u => u.role.toLowerCase() === 'expert').length,
          admins: userRes.data.filter(u => u.role.toLowerCase() === 'admin').length
        });
        
        // গ্রাফ ডাটা ফেচ
        const statsRes = await axios.get('https://cyber-aware.onrender.com/api/reports/stats', config);
        setGraphData(statsRes.data);
      } else {
        const logRes = await axios.get('https://cyber-aware.onrender.com/api/logs', config);
        setLogs(logRes.data);
      }
      setLoading(false);
    } catch (error) { 
      if(error.response?.status === 403) navigate('/login'); 
    }
  };

  const deleteUser = async (id) => {
    if(!window.confirm("Permanently delete this user?")) return;
    try {
      await axios.delete(`https://cyber-aware.onrender.com/api/users/${id}`, { headers: { Authorization: `Bearer ${token}` } });
      setUsers(users.filter(u => u.id !== id));
    } catch (error) { alert("Delete failed"); }
  };

  const handleLogout = () => { localStorage.clear(); navigate('/login'); };

  return (
    <div className="dashboard-layout" style={{background: '#0a0a0a'}}>
      <aside className="sidebar" style={{borderRightColor: '#9d00ff'}}>
        <div className="brand-section">
          <h3 style={{fontFamily: 'Orbitron', margin: 0}}>CYBER<span style={{color: '#9d00ff'}}>ROOT</span></h3>
          <small className="text-secondary">ADMINISTRATION</small>
        </div>
        <ul className="nav-links">
          <li className={`nav-item ${activeTab === 'users' ? 'active' : ''}`} onClick={() => setActiveTab('users')} 
              style={activeTab === 'users' ? {color: '#9d00ff', borderLeft: '3px solid #9d00ff', background: 'rgba(157, 0, 255, 0.1)'} : {}}>
            <FaUsers /> USER MANAGEMENT
          </li>
          <li className={`nav-item ${activeTab === 'logs' ? 'active' : ''}`} onClick={() => setActiveTab('logs')}
              style={activeTab === 'logs' ? {color: '#9d00ff', borderLeft: '3px solid #9d00ff', background: 'rgba(157, 0, 255, 0.1)'} : {}}>
            <FaServer /> SYSTEM LOGS
          </li>
          <li className="nav-item" onClick={() => navigate('/settings')}><FaCog /> SETTINGS</li>
        </ul>
        <div className="user-profile-mini">
          <div className="d-flex align-items-center gap-3 mb-3"><FaUserShield className="text-warning fs-1" /><div><h6 className="m-0 text-white">{currentUser}</h6><small className="text-warning">SUPER ADMIN</small></div></div>
          <button onClick={handleLogout} className="btn btn-outline-warning w-100 btn-sm rounded-0"><FaSignOutAlt /> LOGOUT</button>
        </div>
      </aside>

      <main className="main-content">
        <header className="dashboard-header" style={{borderBottomColor: '#9d00ff40'}}>
          <h2 className="m-0 text-white" style={{fontFamily: 'Orbitron'}}>{activeTab === 'users' ? 'SYSTEM OVERVIEW' : 'EVENT LOGS'}</h2>
        </header>

        <div className="content-body">
          {activeTab === 'users' && (
            <>
              {/* ১. স্ট্যাটাস কার্ড সেকশন (সবার উপরে) */}
              <div className="row g-4 mb-4">
                <div className="col-md-4"><div className="cyber-card"><h6>TOTAL USERS</h6><div className="stat-number text-white">{stats.totalUsers}</div></div></div>
                <div className="col-md-4"><div className="cyber-card" style={{borderColor: '#00f3ff'}}><h6>EXPERTS</h6><div className="stat-number text-info">{stats.experts}</div></div></div>
                <div className="col-md-4"><div className="cyber-card" style={{borderColor: '#ffca00'}}><h6>ADMINS</h6><div className="stat-number text-warning">{stats.admins}</div></div></div>
              </div>

              {/* ২. ইউজার টেবিল সেকশন (মাঝখানে) */}
              <div className="cyber-card mb-4">
                <table className="cyber-table">
                  <thead><tr><th>ID</th><th>USERNAME</th><th>ROLE</th><th>ACTIONS</th></tr></thead>
                  <tbody>{users.map(u => (
                    <tr key={u.id}><td>#{u.id}</td><td className="text-white">{u.username}</td><td><span className="badge bg-secondary">{u.role}</span></td>
                    <td><button onClick={() => deleteUser(u.id)} className="btn btn-sm btn-outline-danger"><FaTrashAlt /></button></td></tr>
                  ))}</tbody>
                </table>
              </div>

              {/* ৩. গ্রাফ সেকশন (সবার নিচে) */}
              <div className="cyber-card" style={{height: '350px', padding: '20px'}}>
                <h5 className="text-info mb-4"><FaChartLine className="me-2"/> THREAT ACTIVITY TREND</h5>
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={graphData}>
                    <defs><linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#9d00ff" stopOpacity={0.3}/><stop offset="95%" stopColor="#9d00ff" stopOpacity={0}/></linearGradient></defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#222" /><XAxis dataKey="date" stroke="#666" fontSize={12} /><YAxis stroke="#666" fontSize={12} /><Tooltip contentStyle={{backgroundColor: '#111', border: '1px solid #9d00ff'}} />
                    <Area type="monotone" dataKey="count" stroke="#9d00ff" fillOpacity={1} fill="url(#colorCount)" />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </>
          )}

          {activeTab === 'logs' && (
            <div className="cyber-card">
              <h5 className="text-info mb-4"><FaList className="me-2"/>SYSTEM EVENT STREAM</h5>
              <table className="cyber-table">
                <thead><tr><th>TIME</th><th>ACTION</th><th>AGENT</th><th>DETAILS</th></tr></thead>
                <tbody>{logs.map(l => (
                  <tr key={l.id}><td>{new Date(l.timestamp).toLocaleString()}</td><td className="text-warning">{l.action}</td><td>{l.performedBy}</td><td className="small text-secondary">{l.details}</td></tr>
                ))}</tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
export default AdminDashboard;