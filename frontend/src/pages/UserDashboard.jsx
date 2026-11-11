import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { MapContainer, TileLayer, GeoJSON, Popup, Marker } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

import {
  FaThLarge, FaSearch, FaShieldAlt, FaHistory, FaCog, FaSignOutAlt,
  FaUserCircle, FaNetworkWired, FaBolt, FaCheckCircle, FaBullhorn, FaExclamationTriangle, FaBan,
  FaComments, FaGlobe, FaFont, FaImage, FaMapMarkerAlt, FaFileUpload
} from 'react-icons/fa';

import '../styles/Dashboard.css';
import Settings from './Settings';
import UserChat from './UserChat';

const cartoKey = import.meta.env.VITE_CARTO_API_KEY;

// 🔥 Division Coordinates for mapping threats
const divisionCoordinates = {
  "Dhaka": [23.8103, 90.4125],
  "Chittagong": [22.3569, 91.7832],
  "Sylhet": [24.8949, 91.8687],
  "Khulna": [22.8456, 89.5403],
  "Rajshahi": [24.3636, 88.6241],
  "Barisal": [22.7010, 90.3535],
  "Rangpur": [25.7439, 89.2752],
  "Mymensingh": [24.7471, 90.4203]
};

// Helper: Calculate Time Ago
const timeAgo = (dateStr) => {
  let ts = dateStr;
  if (typeof ts === 'string' && !ts.endsWith('Z') && !ts.includes('+')) ts += 'Z';
  const seconds = Math.floor((new Date() - new Date(ts)) / 1000);
  let interval = seconds / 31536000;
  if (interval > 1) return Math.floor(interval) + " years ago";
  interval = seconds / 2592000;
  if (interval > 1) return Math.floor(interval) + " months ago";
  interval = seconds / 86400;
  if (interval > 1) return Math.floor(interval) + " days ago";
  interval = seconds / 3600;
  if (interval > 1) return Math.floor(interval) + " hours ago";
  interval = seconds / 60;
  if (interval > 1) return Math.floor(interval) + " minutes ago";
  return Math.floor(seconds) + " seconds ago";
};

function UserDashboard() {

  const [activeTab, setActiveTab] = useState(() => {
    return localStorage.getItem('user_active_tab') || 'overview';
  });

  const [showDivisionModal, setShowDivisionModal] = useState(false);
  const [userDivision, setUserDivision] = useState(localStorage.getItem('user_division') || '');
  const [greeting, setGreeting] = useState('');
  const [weather, setWeather] = useState(null);

  useEffect(() => {
    localStorage.setItem('user_active_tab', activeTab);
    if (activeTab === 'threat-map' && !localStorage.getItem('user_division')) {
      setShowDivisionModal(true);
    }
  }, [activeTab]);

  const [scanMode, setScanMode] = useState('text');
  const [message, setMessage] = useState('');
  const [selectedImage, setSelectedImage] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const [history, setHistory] = useState([]);
  const [geoData, setGeoData] = useState(null);

  // 🔥 Map State for Real-Time Threat Animation
  const [activeThreatsOnMap, setActiveThreatsOnMap] = useState([]);
  const [globalThreatLogs, setGlobalThreatLogs] = useState([]);

  const [personalStats, setPersonalStats] = useState({ threats: 0, safe: 0, spam: 0, promo: 0 });
  const [globalStats, setGlobalStats] = useState({ total: 0, critical: 0, spam: 0 });
  const [graphData, setGraphData] = useState([]);

  const navigate = useNavigate();
  const username = localStorage.getItem('username') || 'User';
  const role = localStorage.getItem('role') || 'User';
  const token = localStorage.getItem('token');
  const stompClientRef = useRef(null);

  const COLORS = ['#00ff9d', '#ff003c', '#ffca00', '#00f3ff'];

  const getResultStyle = (label) => {
    const normalizedLabel = label ? label.toUpperCase() : '';
    switch (normalizedLabel) {
      case 'SAFE': return { color: '#00ff9d', bg: 'rgba(0, 255, 157, 0.1)', border: '#00ff9d', title: 'CLEAN MESSAGE', icon: <FaCheckCircle className="fs-1 mb-3" style={{ color: '#00ff9d' }} /> };
      case 'PROMO': return { color: '#00f3ff', bg: 'rgba(0, 243, 255, 0.1)', border: '#00f3ff', title: 'PROMOTIONAL', icon: <FaBullhorn className="fs-1 mb-3" style={{ color: '#00f3ff' }} /> };
      case 'SPAM': return { color: '#ffca00', bg: 'rgba(255, 202, 0, 0.1)', border: '#ffca00', title: 'SPAM DETECTED', icon: <FaExclamationTriangle className="fs-1 mb-3" style={{ color: '#ffca00' }} /> };
      case 'DANGER':
      case 'SCAM': return { color: '#ff003c', bg: 'rgba(255, 0, 60, 0.1)', border: '#ff003c', title: 'THREAT DETECTED', icon: <FaBan className="fs-1 mb-3" style={{ color: '#ff003c' }} /> };
      default: return { color: '#ffffff', bg: 'rgba(255, 255, 255, 0.1)', border: '#ffffff', title: 'UNKNOWN RESULT', icon: <FaShieldAlt className="fs-1 mb-3" /> };
    }
  };

  const getCardStyle = (label) => {
    const l = label ? label.toUpperCase() : "";
    if (["DANGER", "SCAM"].includes(l)) return { borderLeft: "4px solid #ff003c", bg: "rgba(255, 0, 60, 0.1)", color: "#ff003c" };
    if (["SPAM", "SUSPICIOUS", "PROMO"].includes(l)) return { borderLeft: "4px solid #ffca00", bg: "rgba(255, 202, 0, 0.1)", color: "#ffca00" };
    return { borderLeft: "4px solid #00ff9d", bg: "rgba(0, 255, 157, 0.1)", color: "#00ff9d" };
  };

  useEffect(() => {
    fetch('/bd_divisions.geojson')
      .then(res => res.json())
      .then(data => setGeoData(data))
      .catch(err => console.error("GeoJSON Load Error:", err));
  }, []);

  useEffect(() => {
    const hour = new Date().getHours();
    if (hour >= 5 && hour < 12) setGreeting('GOOD MORNING');
    else if (hour >= 12 && hour < 17) setGreeting('GOOD AFTERNOON');
    else setGreeting('GOOD EVENING');

    const fetchWeather = async () => {
      const div = userDivision || 'Dhaka';
      const coords = divisionCoordinates[div] || divisionCoordinates['Dhaka'];
      try {
        const res = await axios.get(`https://api.open-meteo.com/v1/forecast?latitude=${coords[0]}&longitude=${coords[1]}&current_weather=true`);
        setWeather({ temp: res.data.current_weather.temperature, city: div });
      } catch (err) {
        console.error("Weather Fetch Error:", err);
      }
    };
    fetchWeather();
  }, [userDivision]);

  useEffect(() => {
    if (!token) return;
    if (stompClientRef.current) return;

    const socket = new SockJS("https://cyber-aware.onrender.com/ws");
    const client = Stomp.over(socket);
    client.debug = null;

    client.connect(
      { Authorization: `Bearer ${token}` },
      () => {
        stompClientRef.current = client;

        client.subscribe("/topic/alerts", (notification) => {
          const data = JSON.parse(notification.body);
          console.log("📢 Real-time Alert Received:", data);

          let parsedReport = null;

          // Condition 1: যদি ম্যানুয়াল রিপোর্ট থেকে আসে (এতে 'report' অবজেক্ট থাকে)
          if (data.report) {
            parsedReport = data.report;
          }
          // Condition 2: যদি স্ক্যানার/টেলিমেট্রি থেকে আসে (এতে সরাসরি region, type থাকে)
          else if (data.region && data.type) {
            const typeUpper = data.type.toUpperCase();
            let aiResultStr = "SAFE";

            // 🔥 FIX: SPAM এবং DANGER আলাদা করা হলো
            // যদি টাইপে DANGER/SCAM/PHISHING থাকে অথবা পয়েন্ট ৫ বা তার বেশি হয়
            if (typeUpper.includes("DANGER") || typeUpper.includes("SCAM") || typeUpper.includes("PHISHING") || data.heatPoints >= 5) {
              aiResultStr = "DANGER";
            }
            // আর যদি টাইপে SPAM থাকে অথবা পয়েন্ট ৩ বা তার বেশি হয়
            else if (typeUpper.includes("SPAM") || data.isSpam || data.heatPoints >= 3) {
              aiResultStr = "SPAM";
            }

            // React state এর জন্য একটি ডামি report অবজেক্ট তৈরি করে নেওয়া হলো
            parsedReport = {
              id: Date.now() + Math.floor(Math.random() * 1000), // Unique ID
              aiResult: aiResultStr,
              region: data.region,
              timestamp: data.timestamp || new Date().toISOString()
            };
          }

          // যদি কোনো থ্রেট পাওয়া যায়, তবে ম্যাপে যোগ করুন
          // WebSocket সাবস্ক্রিপশনের ভেতরে থাকা এই অংশটি আপডেট করুন:
          if (parsedReport && ["SCAM", "DANGER", "SPAM", "SUSPICIOUS"].includes(parsedReport.aiResult?.toUpperCase())) {
            const animId = Date.now() + Math.random();
            const threatWithAnim = { ...parsedReport, animId };

            setActiveThreatsOnMap(prev => [...prev, threatWithAnim]);

            // 🔥 অ্যানিমেশন থাকার সময় ৪০০০ (৪ সেকেন্ড) থেকে বাড়িয়ে ৮০০০ (৮ সেকেন্ড) করা হলো
            setTimeout(() => {
              setActiveThreatsOnMap(prev => prev.filter(t => t.animId !== animId));
            }, 8000);

            setGlobalThreatLogs(prev => [parsedReport, ...prev].slice(0, 50));
          }
          fetchData();
        });
      },
      (error) => console.error("WebSocket Error:", error)
    );

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.disconnect();
        stompClientRef.current = null;
      }
    };
  }, [token]);

  useEffect(() => {
    if (!token) {
      navigate('/login');
      return;
    }
    fetchData();
  }, [token, navigate]);

  const fetchData = async () => {
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };

      const globalRes = await axios.get("https://cyber-aware.onrender.com/api/reports/dashboard-stats", config);
      setGlobalStats({
        total: globalRes.data.totalScans || 0,
        critical: globalRes.data.blockedScams || 0,
        spam: (globalRes.data.totalScans || 0) - (globalRes.data.blockedScams || 0) - (globalRes.data.safeLinks || 0)
      });

      if (globalThreatLogs.length === 0) {
        const allReportsRes = await axios.get("https://cyber-aware.onrender.com/api/reports", config);
        if (Array.isArray(allReportsRes.data)) {
          setGlobalThreatLogs(allReportsRes.data.filter(r => ["SCAM", "DANGER", "SPAM"].includes(r.aiResult?.toUpperCase())).reverse().slice(0, 50));
        }
      }

      const res = await axios.get(`https://cyber-aware.onrender.com/api/reports/user?username=${username}`, config);
      const data = res.data;
      setHistory(data);

      let safe = 0, threats = 0, spam = 0, promo = 0;
      const timelineData = {};

      data.forEach(item => {
        const type = item.aiResult?.toUpperCase();
        if (['DANGER', 'SCAM'].includes(type)) threats++;
        else if (['SAFE', 'CLEAN'].includes(type)) safe++;
        else if (type === 'SPAM') spam++;
        else if (type === 'PROMO') promo++;

        if (item.timestamp) {
          let ts = item.timestamp;
          if (typeof ts === 'string' && !ts.endsWith('Z') && !ts.includes('+')) ts += 'Z';
          const dateObj = new Date(ts);
          const dateKey = dateObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });

          if (!timelineData[dateKey]) timelineData[dateKey] = { time: dateKey, total: 0, threats: 0 };
          timelineData[dateKey].total++;
          if (['DANGER', 'SCAM'].includes(type)) timelineData[dateKey].threats++;
        }
      });

      setPersonalStats({ threats, safe, spam, promo });
      setGraphData(Object.values(timelineData));

    } catch (error) {
      if (error.response && error.response.status === 403) {
        handleLogout();
      }
    }
  };

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setSelectedImage(file);
      setImagePreview(URL.createObjectURL(file));
    }
  };

  const handleScan = async () => {
    if (scanMode === 'text' && !message.trim()) return toast.error("Enter some text!");
    if (scanMode === 'image' && !selectedImage) return toast.error("Select an image first!");

    setLoading(true);
    setResult(null);

    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      let res;
      const currentRegion = userDivision || 'UNKNOWN';

      if (scanMode === 'text') {
        res = await axios.post('https://cyber-aware.onrender.com/api/scan/text', {
          username: username, message: message, reporter: username, region: currentRegion
        }, config);
      } else {
        const formData = new FormData();
        formData.append("file", selectedImage);
        formData.append("username", username);
        formData.append("region", currentRegion);
        res = await axios.post('https://cyber-aware.onrender.com/api/scan/image', formData, config);
      }

      const finalResultStatus = res.data.result || res.data.aiResult || "UNKNOWN";
      setResult({ aiResult: finalResultStatus });

      if (finalResultStatus === 'SAFE') fetchData();

    } catch (error) {
      toast.error("Scan Failed. Server Error.");
    } finally {
      setLoading(false);
    }
  };

  const saveDivision = (division) => {
    localStorage.setItem('user_division', division);
    setUserDivision(division);
    setShowDivisionModal(false);
    toast.success(`Region set to ${division}. Database updated.`);
  };

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  const pieData = [
    { name: 'Safe', value: personalStats.safe },
    { name: 'Danger', value: personalStats.threats },
    { name: 'Spam', value: personalStats.spam },
    { name: 'Promo', value: personalStats.promo }
  ].filter(d => d.value > 0);

  return (
    <div className="dashboard-layout" style={{ background: "#050810" }}>
      <ToastContainer theme="dark" />

      {/* DIVISION SELECTION MODAL */}
      <AnimatePresence>
        {showDivisionModal && (
          <motion.div className="custom-modal-overlay" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <motion.div className="custom-modal-content" style={{ maxWidth: '500px' }} initial={{ scale: 0.9, y: 20 }} animate={{ scale: 1, y: 0 }}>
              <div className="modal-icon-wrapper" style={{ background: 'rgba(0, 243, 255, 0.1)' }}>
                <FaMapMarkerAlt size={30} color="#00f3ff" />
              </div>
              <h4>Select Your Division</h4>
              <p>Please select your current region so we can provide accurate local threat intelligence.</p>

              <div className="row g-2 mt-4">
                {Object.keys(divisionCoordinates).map(div => (
                  <div className="col-6" key={div}>
                    <button onClick={() => saveDivision(div)} className="btn btn-outline-info w-100 fw-bold" style={{ borderStyle: 'dashed' }}>
                      {div}
                    </button>
                  </div>
                ))}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <aside className="sidebar" style={{ zIndex: 100, borderRightColor: 'rgba(0,243,255,0.2)' }}>
        <div className="brand-section">
          <h2 style={{ fontFamily: "Orbitron", fontWeight: "bold", letterSpacing: "2px", color: "#fff", margin: 0 }}>
            PRO<span style={{ color: "#00E5FF" }}>HORY</span>
          </h2>
          <small className="text-secondary" style={{ fontSize: '11px', letterSpacing: '1px' }}>USER DASHBOARD</small>
        </div>

        <ul className="nav-links mt-4">
          <li className={`nav-item ${activeTab === 'overview' ? 'active' : ''}`} onClick={() => setActiveTab('overview')} style={activeTab === 'overview' ? { borderLeftColor: '#00f3ff', background: 'rgba(0,243,255,0.1)' } : {}}>
            <FaThLarge size={17} /> OVERVIEW
          </li>
          <li className={`nav-item ${activeTab === 'scan' ? 'active' : ''}`} onClick={() => setActiveTab('scan')} style={activeTab === 'scan' ? { borderLeftColor: '#00f3ff', background: 'rgba(0,243,255,0.1)' } : {}}>
            <FaSearch size={17} /> AI SCANNER
          </li>
          <li className={`nav-item ${activeTab === 'threat-map' ? 'active' : ''}`} onClick={() => setActiveTab('threat-map')} style={activeTab === 'threat-map' ? { borderLeftColor: '#00f3ff', background: 'rgba(0,243,255,0.1)' } : {}}>
            <FaGlobe size={17} /> LIVE THREAT MAP
          </li>
          <li className={`nav-item ${activeTab === 'history' ? 'active' : ''}`} onClick={() => setActiveTab('history')} style={activeTab === 'history' ? { borderLeftColor: '#00f3ff', background: 'rgba(0,243,255,0.1)' } : {}}>
            <FaHistory size={17} /> HISTORY LOGS
          </li>
          <li className={`nav-item ${activeTab === 'chat' ? 'active' : ''}`} onClick={() => setActiveTab('chat')} style={activeTab === 'chat' ? { borderLeftColor: '#00f3ff', background: 'rgba(0,243,255,0.1)' } : {}}>
            <FaComments size={17} /> LIVE SUPPORT
          </li>
          <li className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`} onClick={() => setActiveTab('settings')} style={activeTab === 'settings' ? { borderLeftColor: '#00f3ff', background: 'rgba(0,243,255,0.1)' } : {}}>
            <FaCog size={17} /> SETTINGS
          </li>
        </ul>

        <div className="user-profile-mini mb-3">
          <div className="d-flex align-items-center gap-3 mb-3">
            <FaUserCircle className="fs-1 text-secondary" />
            <div>
              <h6 className="m-0 text-white" style={{ fontSize: '13px' }}>{username}</h6>
              <small className="text-info" style={{ fontSize: '11px' }}>{role.toUpperCase()} ACCOUNT</small>
            </div>
          </div>
          <button onClick={handleLogout} className="btn btn-outline-danger w-100 btn-sm rounded" style={{ fontWeight: 'bold', letterSpacing: '0.5px' }}>
            <FaSignOutAlt className="me-2" /> LOGOUT
          </button>
        </div>
      </aside>

      <main className="main-content" style={{ display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>

        <header className="dashboard-header" style={{ flexShrink: 0, padding: '15px 25px', minHeight: '65px' }}>
          <div>
            <h4 className="m-0 text-white d-flex align-items-center" style={{ fontFamily: 'Orbitron', fontSize: '17px', fontWeight: 'bold' }}>
              {activeTab === 'overview' && (
                <>
                  {greeting}, {username.toUpperCase()}!
                  {weather && (
                    <span className="badge ms-3" style={{ background: 'rgba(0,243,255,0.1)', color: '#00f3ff', border: '1px solid rgba(0,243,255,0.3)', fontSize: '11px', fontFamily: 'Inter', letterSpacing: '1px' }}>
                      🌤️ {Math.round(weather.temp)}°C | {weather.city}
                    </span>
                  )}
                </>
              )}
              {activeTab === 'scan' && 'THREAT DETECTION ENGINE'}
              {activeTab === 'threat-map' && 'LIVE GLOBAL THREAT RADAR'}
              {activeTab === 'history' && 'ACTIVITY & AUDIT LOGS'}
              {activeTab === 'chat' && 'SECURE SUPPORT CHANNEL'}
              {activeTab === 'settings' && 'USER PREFERENCES'}
            </h4>
          </div>
          <div className="d-flex gap-3">
            <div className="cyber-card p-2 px-3 d-flex align-items-center gap-2" style={{ borderColor: '#00f3ff', background: 'rgba(0,243,255,0.05)' }}>
              <div className="spinner-grow spinner-grow-sm text-info" role="status" style={{ width: '12px', height: '12px' }}></div>
              <small className="text-info fw-bold" style={{ fontSize: '11px', letterSpacing: '1px' }}>ACTIVE</small>
            </div>
          </div>
        </header>

        {/* 1. VIEW: OVERVIEW */}
        {activeTab === 'overview' && (
          <div className="custom-scrollbar" style={{ flex: 1, overflowY: 'auto', padding: '25px' }}>
            <motion.div initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }}>

              <h5 className="text-white mb-3" style={{ fontFamily: 'Inter', fontWeight: '600' }}>Global Database Statistics</h5>
              <div className="row g-4 mb-4">
                <div className="col-md-4">
                  <div className="cyber-card p-4">
                    <div className="d-flex justify-content-between text-info">
                      <h6 style={{ fontSize: '12px', fontWeight: 'bold', margin: 0 }}>TOTAL SCANS WORLDWIDE</h6>
                      <FaGlobe size={18} />
                    </div>
                    <div className="stat-number text-white mt-2" style={{ fontSize: '32px' }}>{globalStats.total}</div>
                  </div>
                </div>
                <div className="col-md-4">
                  <div className="cyber-card p-4" style={{ borderColor: '#ff003c' }}>
                    <div className="d-flex justify-content-between text-danger">
                      <h6 style={{ fontSize: '12px', fontWeight: 'bold', margin: 0 }}>CRITICAL THREATS (DB)</h6>
                      <FaBolt size={18} />
                    </div>
                    <div className="stat-number text-danger mt-2" style={{ fontSize: '32px' }}>{globalStats.critical}</div>
                  </div>
                </div>
                <div className="col-md-4">
                  <div className="cyber-card p-4" style={{ borderColor: '#ffca00' }}>
                    <div className="d-flex justify-content-between text-warning">
                      <h6 style={{ fontSize: '12px', fontWeight: 'bold', margin: 0 }}>SPAM FILTERED (DB)</h6>
                      <FaExclamationTriangle size={18} />
                    </div>
                    <div className="stat-number text-warning mt-2" style={{ fontSize: '32px' }}>{globalStats.spam}</div>
                  </div>
                </div>
              </div>

              <h5 className="text-white mb-3 mt-5" style={{ fontFamily: 'Inter', fontWeight: '600' }}>Your Personal Analytics</h5>
              <div className="row g-4 mb-4">
                <div className="col-md-8">
                  <div className="cyber-card p-4 h-100">
                    <h6 className="text-secondary mb-4" style={{ fontSize: '13px', fontWeight: 'bold' }}>YOUR SCAN HISTORY TIMELINE</h6>
                    <div style={{ height: '250px' }}>
                      <ResponsiveContainer width="100%" height="100%">
                        <AreaChart data={graphData}>
                          <defs>
                            <linearGradient id="userTraffic" x1="0" y1="0" x2="0" y2="1">
                              <stop offset="5%" stopColor="#00f3ff" stopOpacity={0.4} />
                              <stop offset="95%" stopColor="#00f3ff" stopOpacity={0} />
                            </linearGradient>
                          </defs>
                          <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                          <XAxis dataKey="time" stroke="#64748b" tick={{ fontSize: 11 }} />
                          <YAxis stroke="#64748b" tick={{ fontSize: 11 }} />
                          <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px' }} itemStyle={{ color: '#fff', fontSize: '12px' }} />
                          <Area type="monotone" dataKey="total" stroke="#00f3ff" strokeWidth={2} fillOpacity={1} fill="url(#userTraffic)" name="Total Scans" />
                        </AreaChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                </div>
                <div className="col-md-4">
                  <div className="cyber-card p-4 h-100 d-flex flex-column align-items-center justify-content-center">
                    <h6 className="text-secondary mb-2 w-100" style={{ fontSize: '13px', fontWeight: 'bold' }}>YOUR THREAT DISTRIBUTION</h6>
                    <div style={{ height: '220px', width: '100%' }}>
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie data={pieData} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                            {pieData.map((entry, index) => (
                              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                            ))}
                          </Pie>
                          <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px', fontSize: '12px' }} itemStyle={{ color: '#fff' }} />
                        </PieChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                </div>
              </div>

            </motion.div>
          </div>
        )}

        {/* 2. VIEW: SCANNER WITH OCR */}
        {activeTab === 'scan' && (
          <div className="custom-scrollbar" style={{ flex: 1, overflowY: 'auto', padding: '25px' }}>
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="row justify-content-center mt-2">
              <div className="col-lg-8">

                <div className="d-flex mb-4 gap-2">
                  <button className={`btn w-50 fw-bold py-2 ${scanMode === 'text' ? 'btn-info text-dark' : 'btn-outline-secondary text-light'}`} onClick={() => { setScanMode('text'); setResult(null); }}>
                    <FaFont className="me-2" /> Text / Link Analysis
                  </button>
                  <button className={`btn w-50 fw-bold py-2 ${scanMode === 'image' ? 'btn-info text-dark' : 'btn-outline-secondary text-light'}`} onClick={() => { setScanMode('image'); setResult(null); }}>
                    <FaImage className="me-2" /> Image OCR Scan
                  </button>
                </div>

                <div className="cyber-card p-5 shadow-lg position-relative overflow-hidden" style={{ background: 'rgba(15,23,42,0.5)' }}>

                  {scanMode === 'text' ? (
                    <>
                      <div className="text-center mb-4">
                        <FaShieldAlt className="fs-1 text-info mb-3" />
                        <h4 className="text-white" style={{ fontFamily: 'Inter', fontWeight: 'bold' }}>TEXT & URL SCANNER</h4>
                        <p className="text-secondary" style={{ fontSize: '13px' }}>Paste SMS, email body, or links to detect phishing.</p>
                      </div>
                      <textarea className="scan-box w-100 p-3 mb-4 rounded text-white" rows="5" style={{ background: 'rgba(0,0,0,0.4)', border: '1px solid #334155', outline: 'none', fontSize: '14px', resize: 'none' }} placeholder="Enter suspicious content..." value={message} onChange={(e) => setMessage(e.target.value)}></textarea>
                    </>
                  ) : (
                    <>
                      <div className="text-center mb-4">
                        <FaImage className="fs-1 text-info mb-3" />
                        <h4 className="text-white" style={{ fontFamily: 'Inter', fontWeight: 'bold' }}>OCR IMAGE SCANNER</h4>
                        <p className="text-secondary" style={{ fontSize: '13px' }}>Upload screenshots of suspicious SMS, Emails or Websites.</p>
                      </div>
                      <div className="ocr-upload-box mb-4 position-relative">
                        <input type="file" id="ocrInput" className="d-none" accept="image/*" onChange={handleImageChange} />
                        <label htmlFor="ocrInput" className="w-100 rounded d-flex flex-column align-items-center justify-content-center transition-all overflow-hidden position-relative" style={{ border: "2px dashed #00f3ff", cursor: "pointer", background: "rgba(0, 243, 255, 0.05)", minHeight: '200px' }}>
                          {imagePreview ? (
                            <>
                              <img src={imagePreview} alt="Preview" style={{ width: '100%', height: '200px', objectFit: 'contain', opacity: loading ? 0.5 : 1 }} />
                              {loading && <div className="ocr-scan-line"></div>}
                            </>
                          ) : (
                            <>
                              <FaFileUpload className="fs-2 text-info mb-2" />
                              <h6 className="text-light m-0" style={{ fontSize: '14px' }}>Click or Drag Image Here</h6>
                            </>
                          )}
                        </label>
                      </div>
                    </>
                  )}

                  <button onClick={handleScan} className="btn btn-info w-100 py-3 fw-bold rounded" style={{ boxShadow: '0 5px 20px rgba(0,243,255,0.2)', fontSize: '14px', letterSpacing: '1px' }} disabled={loading}>
                    {loading ? 'ANALYZING NEURAL PATTERNS...' : 'INITIATE DEEP SCAN'}
                  </button>

                  <AnimatePresence>
                    {result && (() => {
                      const style = getResultStyle(result.aiResult);
                      return (
                        <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="mt-4 p-4 text-center rounded shadow" style={{ border: `1px solid ${style.border}`, backgroundColor: style.bg }}>
                          {style.icon}
                          <h4 style={{ color: style.color, fontFamily: 'Orbitron', fontWeight: 'bold', fontSize: '18px' }}>{style.title}</h4>
                          <p className="m-0 text-white mt-2" style={{ fontSize: '13px' }}>AI Verdict: <strong style={{ color: style.color }}>{result.aiResult}</strong></p>
                        </motion.div>
                      );
                    })()}
                  </AnimatePresence>
                </div>
              </div>
            </motion.div>
          </div>
        )}

        {/* 🔥 3. VIEW: THREAT MAP (FIXED FOR REAL-TIME RADAR ANIMATION) */}
        {activeTab === "threat-map" && (
          <div className="custom-scrollbar" style={{ flex: 1, overflowY: 'auto', padding: '25px' }}>
            <div className="map-wrapper mb-4 rounded overflow-hidden shadow" style={{ height: '450px', border: '1px solid rgba(255,255,255,0.1)', position: 'relative' }}>
              <MapContainer
                center={[23.6850, 90.3563]}
                zoom={7}
                style={{ height: '100%', width: '100%', background: '#02040a' }}
                zoomControl={true} scrollWheelZoom={false} dragging={true} doubleClickZoom={true} touchZoom={false}
              >
               <TileLayer
                url={`https://basemaps.cartocdn.com/rastertiles/voyager/15/16046/12355.png?key=${cartoKey}`}
                attribution="&copy; OpenStreetMap contributors &copy; CARTO"
                />
                {geoData && <GeoJSON data={geoData} style={{ color: '#00E5FF', weight: 1.5, fillOpacity: 0.05 }} />}

                {/* 🔥 REAL-TIME DYNAMIC MARKERS ONLY */}
                {activeThreatsOnMap.map((r) => {
                  const regionKey = r.region ? r.region.charAt(0).toUpperCase() + r.region.slice(1).toLowerCase() : "Dhaka";
                  let coords = divisionCoordinates[regionKey] || divisionCoordinates["Dhaka"];

                  const lat = coords[0] + (Math.random() * 0.4 - 0.2);
                  const lng = coords[1] + (Math.random() * 0.4 - 0.2);

                  const aiResult = r.aiResult?.toUpperCase() || "UNKNOWN";
                  const isDanger = ["DANGER", "SCAM"].includes(aiResult);
                  const isSpam = ["SPAM", "SUSPICIOUS"].includes(aiResult);

                  // 🔥 কালার এবং ক্লাস সিলেক্ট করার লজিক
                  let radarClass = 'pulse-info'; // ডিফল্ট (যদি অন্য কিছু হয়)
                  let colorHex = '#00f3ff';
                  if (isDanger) {
                    radarClass = 'pulse-danger';
                    colorHex = '#ff003c'; // Red
                  } else if (isSpam) {
                    radarClass = 'pulse-warning';
                    colorHex = '#ffca00'; // Yellow
                  }

                  // 🔥 নতুন সুন্দর রাডার অ্যানিমেশন আইকন
                  const customPulsingIcon = new L.divIcon({
                    className: `custom-radar-icon ${radarClass}`,
                    html: `
            <div class="radar-ping"></div>
            <div class="radar-core"></div>
        `,
                    iconSize: [40, 40],
                    iconAnchor: [20, 20]
                  });

                  return (
                    <Marker key={r.animId} position={[lat, lng]} icon={customPulsingIcon}>
                      <Popup className="cyber-popup">
                        <div style={{ textAlign: 'center' }}>
                          <strong style={{ color: colorHex }}>
                            {isDanger ? '🚨' : '⚠️'} {aiResult} DETECTED
                          </strong><br />
                          <small>Origin: {regionKey}</small>
                        </div>
                      </Popup>
                    </Marker>
                  );
                })}
              </MapContainer>
            </div>

            <h5 className="text-white mb-3" style={{ fontFamily: 'Inter', fontWeight: '600' }}>Global Threat Signals Radar</h5>
            <div className="row g-3 pb-4">
              <AnimatePresence>
                {globalThreatLogs.length === 0 && <div className="text-secondary text-center mt-4 w-100">Waiting for live signals...</div>}

                {globalThreatLogs.map((report) => {
                  const style = getCardStyle(report.aiResult);
                  const timeTxt = report.timestamp ? timeAgo(report.timestamp) : "Just now";
                  const regionName = report.region || "Unknown Region";

                  return (
                    <motion.div key={report.id} className="col-12" layout initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, scale: 0.9 }}>
                      <div className="cyber-card p-3 d-flex align-items-center justify-content-between" style={{ borderLeft: style.borderLeft, background: 'rgba(15,23,42,0.4)' }}>
                        <div>
                          <p className="text-light m-0" style={{ fontSize: '14px', fontWeight: '500' }}>
                            Detected <span style={{ color: style.color }}>{report.aiResult}</span> from {regionName}
                          </p>
                          <small className="text-secondary mt-1 d-block" style={{ fontSize: '11px', fontFamily: 'monospace' }}>
                            🕒 {timeTxt} | Signal ID: SCAN-{report.id}
                          </small>
                        </div>
                        <FaBolt size={20} color={style.color} style={{ opacity: 0.8 }} />
                      </div>
                    </motion.div>
                  );
                })}
              </AnimatePresence>
            </div>
          </div>
        )}

        {/* 4. VIEW: HISTORY */}
        {activeTab === 'history' && (
          <div className="custom-scrollbar" style={{ flex: 1, overflowY: 'auto', padding: '25px' }}>
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
              <div className="cyber-card p-4">
                <h5 className="mb-4 text-white" style={{ fontFamily: 'Inter', fontWeight: 'bold' }}>FULL SECURITY LOGS</h5>

                <table className="cyber-table w-100">
                  <thead>
                    <tr>
                      <th style={{ fontSize: '12px' }}>SCAN ID</th>
                      <th style={{ fontSize: '12px' }}>CONTENT PREVIEW</th>
                      <th style={{ fontSize: '12px' }}>RESULT</th>
                      <th style={{ fontSize: '12px' }}>DATE</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.length === 0 && <tr><td colSpan="4" className="text-center text-secondary py-4">No scan history found.</td></tr>}
                    {history.slice().reverse().map(log => {
                      const style = getResultStyle(log.aiResult);
                      let ts = log.timestamp;
                      if (typeof ts === 'string' && !ts.endsWith('Z') && !ts.includes('+')) ts += 'Z';
                      const dateObj = new Date(ts);
                      const formattedDate = isNaN(dateObj) ? "Recent" : dateObj.toLocaleDateString() + ' ' + dateObj.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

                      return (
                        <tr key={log.id}>
                          <td className="text-secondary" style={{ fontSize: '12px' }}>SCAN-{log.id}</td>
                          <td className="text-truncate text-light" style={{ maxWidth: '250px', fontSize: '13px' }}>{log.message}</td>
                          <td>
                            <span className="badge" style={{ backgroundColor: style.bg, color: style.color, border: `1px solid ${style.border}`, fontSize: '11px', padding: '5px 10px' }}>
                              {log.aiResult}
                            </span>
                          </td>
                          <td className="text-secondary" style={{ fontSize: '12px' }}>{formattedDate}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </motion.div>
          </div>
        )}

        {/* 5. VIEW: LIVE CHAT */}
        {activeTab === 'chat' && (
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden', padding: '0 10px 10px 10px' }}>
            <UserChat />
          </div>
        )}

        {/* 6. VIEW: SETTINGS */}
        {activeTab === 'settings' && (
          <div className="p-4 w-100 h-100 custom-scrollbar" style={{ overflowY: 'auto' }}>
            <Settings />
          </div>
        )}

      </main>
    </div>
  );
}

export default UserDashboard;