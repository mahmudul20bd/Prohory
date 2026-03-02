import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { MapContainer, TileLayer, GeoJSON, CircleMarker, Popup, Marker } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

import {
  FaGlobe, FaBug, FaSignOutAlt, FaExclamationTriangle,
  FaCheckCircle, FaSearch, FaCog, FaFileUpload, FaMicrochip, FaComments, FaChartLine, FaLink, FaIdBadge, FaStar, FaUserShield
} from 'react-icons/fa';

import '../styles/Dashboard.css';
import ExpertChat from './ExpertChat';
import Settings from './Settings';

// Division Coordinates
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

function ExpertDashboard() {

  const [activeTab, setActiveTab] = useState(() => {
    return localStorage.getItem('expert_active_tab') || 'dashboard';
  });

  useEffect(() => {
    localStorage.setItem('expert_active_tab', activeTab);
  }, [activeTab]);

  const [reports, setReports] = useState([]);
  const [stats, setStats] = useState({ total: 0, critical: 0, pending: 0 });
  const [loading, setLoading] = useState(true);

  // Real-time Active Threats for Map Animation
  const [activeThreatsOnMap, setActiveThreatsOnMap] = useState([]);

  // Expert Profile State 
  const [expertProfile, setExpertProfile] = useState({
    username: localStorage.getItem('username'),
    specialization: 'Loading...',
    aboutMe: 'Loading expert details...',
    averageRating: 0.0,
    ratingCount: 0,
    profileImage: null,
    coverImage: null
  });

  const [selectedFile, setSelectedFile] = useState(null);
  const [scanInput, setScanInput] = useState('');
  const [scanResult, setScanResult] = useState(null);
  const [scanning, setScanning] = useState(false);

  const [actionModal, setActionModal] = useState({ isOpen: false, reportId: null, actionType: null });
  const [geoData, setGeoData] = useState(null);

  const [trafficData, setTrafficData] = useState([]);
  const [graphFilter, setGraphFilter] = useState('daily');

  const navigate = useNavigate();
  const username = localStorage.getItem('username');
  const token = localStorage.getItem('token');
  const stompClientRef = useRef(null);

  useEffect(() => {
    fetch('/bd_divisions.geojson')
      .then(res => res.json())
      .then(data => setGeoData(data))
      .catch(err => console.error("GeoJSON Load Error:", err));
  }, []);

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

        // 🔥 FIX: We use /topic/alerts because it sends the full Report object (including Region & ID)
        // 🔥 FIX: We use /topic/alerts and parse both manual reports and background telemetry
        client.subscribe("/topic/alerts", (notification) => {
          const data = JSON.parse(notification.body);
          console.log("📢 Alert Received in Expert Dashboard:", data);

          let parsedReport = null;

          // Condition 1: Manual Report (এতে 'report' অবজেক্ট থাকে)
          if (data.report) {
            parsedReport = data.report;
          }
          // Condition 2: Background Telemetry (এতে সরাসরি region, type, heatPoints থাকে)
          else if (data.region && data.type) {
            const typeUpper = data.type.toUpperCase();
            let aiResultStr = "SAFE";

            // DANGER/SCAM/PHISHING অথবা heatPoints >= 5 হলে
            if (typeUpper.includes("DANGER") || typeUpper.includes("SCAM") || typeUpper.includes("PHISHING") || data.heatPoints >= 5) {
              aiResultStr = "DANGER";
            }
            // SPAM অথবা heatPoints >= 3 হলে
            else if (typeUpper.includes("SPAM") || data.isSpam || data.heatPoints >= 3) {
              aiResultStr = "SPAM";
            }

            parsedReport = {
              id: Date.now() + Math.floor(Math.random() * 1000),
              aiResult: aiResultStr,
              region: data.region,
              timestamp: data.timestamp || new Date().toISOString()
            };
          }

          if (parsedReport && ["SCAM", "DANGER", "SPAM", "SUSPICIOUS"].includes(parsedReport.aiResult?.toUpperCase())) {
            const animId = Date.now() + Math.random();
            const threatWithAnim = { ...parsedReport, animId };

            // Add to Map Animation
            setActiveThreatsOnMap(prev => [...prev, threatWithAnim]);

            // 🔥 Remove after 8 seconds (৮ সেকেন্ড পর্যন্ত ম্যাপে দেখাবে)
            setTimeout(() => {
              setActiveThreatsOnMap(prev => prev.filter(t => t.animId !== animId));
            }, 8000);
          }

          fetchGlobalReports();
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
      navigate("/login");
      return;
    }
    fetchGlobalReports();
  }, [token]);

  useEffect(() => {
    if (activeTab === 'profile') {
      fetchExpertProfile();
    }
  }, [activeTab, token]);

  const fetchExpertProfile = async () => {
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      const res = await axios.get("https://cyber-aware.onrender.com/api/users/me", config);
      setExpertProfile(prev => ({
        ...prev,
        specialization: res.data.specialization || 'Cyber Security Analyst',
        aboutMe: res.data.aboutMe || 'I am a certified security expert working with Prohory to ensure a safe digital environment. Feel free to connect for any security-related assistance.',
        averageRating: res.data.averageRating || 0.0,
        ratingCount: res.data.ratingCount || 0,
        profileImage: res.data.profileImage,
        coverImage: res.data.coverImage
      }));
    } catch (error) {
      console.log("Profile fetch error");
    }
  };

  useEffect(() => {
    if (reports.length > 0) {
      generateRealGraphData(reports, graphFilter);
    } else {
      setTrafficData([{ time: 'No Data', traffic: 0, threats: 0 }]);
    }
  }, [reports, graphFilter]);

  const fetchGlobalReports = async () => {
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };

      const statsRes = await axios.get("https://cyber-aware.onrender.com/api/reports/dashboard-stats", config);
      setStats({
        total: statsRes.data.totalScans || 0,
        critical: statsRes.data.blockedScams || 0,
        pending: statsRes.data.totalScans - statsRes.data.blockedScams - (statsRes.data.safeLinks || 0)
      });

      const reportsRes = await axios.get("https://cyber-aware.onrender.com/api/reports", config);
      const fetchedReports = Array.isArray(reportsRes.data) ? reportsRes.data.reverse() : [];
      setReports(fetchedReports);

      setLoading(false);
    } catch (error) {
      if (error.response?.status === 403) navigate("/login");
    }
  };

  const generateRealGraphData = (dataList, filter) => {
    const groupedData = {};
    const now = new Date();

    dataList.forEach(report => {
      if (!report.timestamp) return;

      let dateObj;
      if (Array.isArray(report.timestamp)) {
        const [y, m, d, h, min, s] = report.timestamp;
        dateObj = new Date(Date.UTC(y, m - 1, d, h || 0, min || 0, s || 0));
      } else {
        let ts = report.timestamp;
        if (typeof ts === 'string' && !ts.endsWith('Z') && !ts.includes('+')) ts += 'Z';
        dateObj = new Date(ts);
      }

      let key = '';
      const diffTime = now.getTime() - dateObj.getTime();
      const diffHours = diffTime / (1000 * 60 * 60);
      const diffDays = diffTime / (1000 * 60 * 60 * 24);

      if (filter === 'daily') {
        if (diffHours >= 0 && diffHours <= 24) key = dateObj.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
      } else if (filter === 'weekly') {
        if (diffDays >= 0 && diffDays <= 7) key = dateObj.toLocaleDateString('en-US', { weekday: 'short' });
      } else {
        if (diffDays >= 0 && diffDays <= 30) key = dateObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
      }

      if (key) {
        if (!groupedData[key]) groupedData[key] = { time: key, traffic: 0, threats: 0 };
        groupedData[key].traffic += 1;
        if (["SCAM", "DANGER"].includes(report.aiResult?.toUpperCase())) {
          groupedData[key].threats += 1;
        }
      }
    });

    const chartArray = Object.values(groupedData);
    if (chartArray.length === 0) chartArray.push({ time: 'No Data', traffic: 0, threats: 0 });
    setTrafficData(chartArray);
  };

  const confirmAction = async () => {
    const { reportId, actionType } = actionModal;
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      await axios.put(`https://cyber-aware.onrender.com/api/report/${reportId}?status=${actionType}`, {}, config);

      setReports(prev => prev.map(r => r.id === reportId ? { ...r, aiResult: actionType, expertReviewed: true } : r));
      setActionModal({ isOpen: false, reportId: null, actionType: null });
      toast.success(`Signal marked as ${actionType}`);

      fetchGlobalReports();
    } catch (error) {
      toast.error("Failed to update status.");
    }
  };

  const sortedReports = [...reports].sort((a, b) => {
    if (a.aiResult === 'SAFE' && b.aiResult !== 'SAFE') return 1;
    if (b.aiResult === 'SAFE' && a.aiResult !== 'SAFE') return -1;
    return 0;
  });

  const handleFileScan = async () => {
    if (!selectedFile && !scanInput) return alert("Please select a file or enter a hash/URL!");
    setScanning(true); setScanResult(null);

    const formData = new FormData();
    if (selectedFile) formData.append("file", selectedFile);
    if (scanInput) formData.append("hash", scanInput);

    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };
      const res = await axios.post("https://cyber-aware.onrender.com/api/scan/image", formData, config);

      setTimeout(() => {
        setScanResult({
          status: res.data.result === 'SAFE' ? 'SAFE' : 'DANGER',
          hash: "Generated via AI",
          threatType: res.data.result,
          confidence: "98%"
        });
        setScanning(false);
      }, 1500);
    } catch (error) {
      setScanning(false); alert("Scan Failed! Server Error.");
    }
  };

  const handleLogout = () => { localStorage.clear(); navigate("/login"); };

  const getCardStyle = (label) => {
    const l = label ? label.toUpperCase() : "";
    if (["DANGER", "SCAM"].includes(l)) return { borderLeft: "4px solid #ff003c", iconColor: "text-danger", icon: <FaExclamationTriangle /> };
    if (["SPAM", "SUSPICIOUS", "PROMO"].includes(l)) return { borderLeft: "4px solid #F59E0B", iconColor: "text-warning", icon: <FaSearch /> };
    return { borderLeft: "4px solid #10B981", iconColor: "text-success", icon: <FaCheckCircle /> };
  };

  return (
    <div className="dashboard-layout" style={{ background: "#050810" }}>
      <ToastContainer pauseOnFocusLoss={false} />

      <AnimatePresence>
        {actionModal.isOpen && (
          <motion.div className="custom-modal-overlay" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <motion.div className="custom-modal-content" initial={{ scale: 0.9, y: 20 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.9, y: 20 }}>
              <div className="modal-icon-wrapper" style={{ background: actionModal.actionType === 'SAFE' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)' }}>
                {actionModal.actionType === 'SAFE' ? <FaCheckCircle size={30} color="#10b981" /> : <FaExclamationTriangle size={30} color="#ef4444" />}
              </div>
              <h4>Confirm Security Action</h4>
              <p>Are you sure you want to mark this signal as <b style={{ color: actionModal.actionType === 'SAFE' ? '#10b981' : '#ef4444' }}>{actionModal.actionType}</b>?</p>
              <div className="modal-actions">
                <button className="btn-cancel" onClick={() => setActionModal({ isOpen: false, reportId: null, actionType: null })}>Cancel</button>
                <button className="btn-confirm" style={{ background: actionModal.actionType === 'SAFE' ? '#10b981' : '#ef4444' }} onClick={confirmAction}>
                  Yes, Mark as {actionModal.actionType}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <aside className="sidebar" style={{ zIndex: 100 }}>
        <div className="brand-section">
          <h2 style={{ fontFamily: "Orbitron", fontWeight: "bold", letterSpacing: "2px", color: "#fff", margin: 0 }}>
            PRO<span style={{ color: "#00E5FF" }}>HORY</span>
          </h2>
          <small className="text-secondary" style={{ fontSize: '11px', letterSpacing: '1px' }}>EXPERT COMMAND</small>
        </div>

        <ul className="nav-links mt-4">
          <li className={`nav-item ${activeTab === "dashboard" ? "active" : ""}`} onClick={() => setActiveTab("dashboard")}><FaChartLine size={17} /> DASHBOARD</li>
          <li className={`nav-item ${activeTab === "threat-map" ? "active" : ""}`} onClick={() => setActiveTab("threat-map")}><FaGlobe size={17} /> THREAT MAP</li>
          <li className={`nav-item ${activeTab === "malware" ? "active" : ""}`} onClick={() => setActiveTab("malware")}><FaBug size={17} /> MALWARE LAB</li>
          <li className={`nav-item ${activeTab === "chat" ? "active" : ""}`} onClick={() => setActiveTab("chat")}><FaComments size={17} /> LIVE CHAT</li>

          <li className={`nav-item ${activeTab === "profile" ? "active" : ""}`} onClick={() => setActiveTab("profile")}><FaIdBadge size={17} /> PUBLIC PROFILE</li>

          <li className={`nav-item ${activeTab === "settings" ? "active" : ""}`} onClick={() => setActiveTab("settings")}><FaCog size={17} /> SETTINGS</li>
        </ul>

        <button onClick={handleLogout} className="btn btn-outline-danger w-100" style={{ fontSize: '13px', fontWeight: '600', padding: '12px' }}>
          <FaSignOutAlt className="me-2" /> TERMINATE
        </button>
      </aside>

      <main className="main-content" style={{ display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>

        <header className="dashboard-header" style={{ flexShrink: 0, padding: '15px 25px', minHeight: '65px' }}>
          <h4 className="text-white m-0" style={{ fontFamily: "Orbitron", fontSize: '17px', fontWeight: '600', letterSpacing: '1px' }}>
            {activeTab === "dashboard" ? "SYSTEM DASHBOARD" :
              activeTab === "threat-map" ? "GLOBAL THREAT MAP" :
                activeTab === "malware" ? "VIRUS & MALWARE SCANNER" :
                  activeTab === "chat" ? "SECURE COMMUNICATION" :
                    activeTab === "profile" ? "YOUR PUBLIC IDENTITY" : "SYSTEM SETTINGS"}
          </h4>
        </header>

        {/* 1. DASHBOARD TAB */}
        {activeTab === "dashboard" && (
          <div style={{ flex: 1, overflowY: 'auto', padding: '25px' }} className="custom-scrollbar">
            <h5 className="text-white mb-4" style={{ fontFamily: 'Inter', fontWeight: '600' }}>Database Real-Time Statistics</h5>
            <div className="row g-4 mb-4">
              <div className="col-md-4">
                <div className="cyber-card p-4">
                  <div className="d-flex justify-content-between align-items-center">
                    <h6 style={{ fontSize: '12px', color: '#94a3b8', margin: 0 }}>TOTAL SCANS (DB)</h6>
                    <FaGlobe color="#00E5FF" size={18} />
                  </div>
                  <div className="stat-number text-white mt-2" style={{ fontSize: '32px', fontWeight: 'bold' }}>{stats.total}</div>
                </div>
              </div>
              <div className="col-md-4">
                <div className="cyber-card p-4" style={{ borderColor: "#ff003c" }}>
                  <div className="d-flex justify-content-between align-items-center">
                    <h6 style={{ fontSize: '12px', color: '#94a3b8', margin: 0 }}>CRITICAL THREATS</h6>
                    <FaExclamationTriangle color="#ff003c" size={18} />
                  </div>
                  <div className="stat-number text-danger mt-2" style={{ fontSize: '32px', fontWeight: 'bold' }}>{stats.critical}</div>
                </div>
              </div>
              <div className="col-md-4">
                <div className="cyber-card p-4" style={{ borderColor: "#F59E0B" }}>
                  <div className="d-flex justify-content-between align-items-center">
                    <h6 style={{ fontSize: '12px', color: '#94a3b8', margin: 0 }}>SUSPICIOUS</h6>
                    <FaSearch color="#F59E0B" size={18} />
                  </div>
                  <div className="stat-number text-warning mt-2" style={{ fontSize: '32px', fontWeight: 'bold' }}>{stats.pending}</div>
                </div>
              </div>
            </div>

            <div className="d-flex justify-content-between align-items-center mb-3">
              <h5 className="text-white m-0" style={{ fontFamily: 'Inter', fontWeight: '600' }}>Network Traffic & Threat History</h5>
              <div className="btn-group border border-secondary rounded" role="group">
                <button type="button" className={`btn btn-sm ${graphFilter === 'daily' ? 'btn-primary' : 'btn-dark text-light'}`} onClick={() => setGraphFilter('daily')}>Day</button>
                <button type="button" className={`btn btn-sm ${graphFilter === 'weekly' ? 'btn-primary' : 'btn-dark text-light'}`} onClick={() => setGraphFilter('weekly')}>Week</button>
                <button type="button" className={`btn btn-sm ${graphFilter === 'monthly' ? 'btn-primary' : 'btn-dark text-light'}`} onClick={() => setGraphFilter('monthly')}>Month</button>
              </div>
            </div>

            <div className="cyber-card p-4" style={{ height: '350px' }}>
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={trafficData} margin={{ top: 10, right: 0, left: -20, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorTraffic" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#00E5FF" stopOpacity={0.4} />
                      <stop offset="95%" stopColor="#00E5FF" stopOpacity={0} />
                    </linearGradient>
                    <linearGradient id="colorThreat" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#ff003c" stopOpacity={0.5} />
                      <stop offset="95%" stopColor="#ff003c" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                  <XAxis dataKey="time" stroke="#64748b" tick={{ fontSize: 12 }} />
                  <YAxis stroke="#64748b" tick={{ fontSize: 12 }} />
                  <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px' }} itemStyle={{ color: '#fff' }} />
                  <Area type="monotone" dataKey="traffic" stroke="#00E5FF" strokeWidth={2} fillOpacity={1} fill="url(#colorTraffic)" name="Report Logs" />
                  <Area type="monotone" dataKey="threats" stroke="#ff003c" strokeWidth={2} fillOpacity={1} fill="url(#colorThreat)" name="Detected Threats" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}

        {/* 2. THREAT MAP TAB (Real-time Animated) */}
        {activeTab === "threat-map" && (
          <div style={{ flex: 1, overflowY: 'auto', padding: '25px' }} className="custom-scrollbar">

            <div className="map-wrapper mb-4 rounded overflow-hidden shadow" style={{ height: '450px', border: '1px solid rgba(255,255,255,0.1)', position: 'relative' }}>
              <MapContainer
                center={[23.6850, 90.3563]}
                zoom={7}
                style={{ height: '100%', width: '100%', background: '#02040a' }}
                zoomControl={true} scrollWheelZoom={false} dragging={true} doubleClickZoom={true} touchZoom={false}
              >
                <TileLayer url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png" attribution="&copy; OpenStreetMap" />
                {geoData && <GeoJSON data={geoData} style={{ color: '#00E5FF', weight: 1.5, fillOpacity: 0.05 }} />}

                {/* 🔥 Show ONLY Real-Time Active Threats */}
                {/* 🔥 Show ONLY Real-Time Active Threats */}
                {activeThreatsOnMap.map((r) => {
                  const regionKey = r.region ? r.region.charAt(0).toUpperCase() + r.region.slice(1).toLowerCase() : "Dhaka";
                  let coords = divisionCoordinates[regionKey] || divisionCoordinates["Dhaka"];

                  const lat = coords[0] + (Math.random() * 0.4 - 0.2);
                  const lng = coords[1] + (Math.random() * 0.4 - 0.2);

                  const aiResult = r.aiResult?.toUpperCase() || "UNKNOWN";
                  const isDanger = ["DANGER", "SCAM"].includes(aiResult);
                  const isSpam = ["SPAM", "SUSPICIOUS"].includes(aiResult);

                  // 🔥 কালার এবং রাডার ক্লাস সিলেক্ট করার লজিক
                  let radarClass = 'pulse-info';
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

            <h5 className="text-white mb-3" style={{ fontFamily: 'Inter', fontWeight: '600' }}>Incoming Threat Signals</h5>
            <div className="row g-3 pb-4">
              <AnimatePresence>
                {sortedReports.length === 0 && <div className="text-secondary text-center mt-4 w-100">No active signals found in Database.</div>}

                {sortedReports.map((report) => {
                  const style = getCardStyle(report.aiResult);
                  const isSafe = report.aiResult === 'SAFE';

                  return (
                    <motion.div key={report.id} className="col-12" layout initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, scale: 0.9 }}>
                      <div className="cyber-card d-flex justify-content-between align-items-center p-3" style={{ borderLeft: style.borderLeft, opacity: isSafe ? 0.5 : 1 }}>
                        <div>
                          <p className="text-light m-0" style={{ fontSize: '14px', lineHeight: '1.5' }}>"{report.message}"</p>
                          <small className={style.iconColor} style={{ fontSize: '11px', fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: '5px', marginTop: '6px' }}>
                            {style.icon} AI VERDICT: {report.aiResult}
                          </small>
                        </div>
                        <div className="d-flex gap-2">
                          {isSafe ? (
                            <div className="badge bg-success p-2 d-flex align-items-center" style={{ fontSize: '11px', borderRadius: '6px' }}>
                              <FaCheckCircle className="me-1" /> Marked as Safe
                            </div>
                          ) : (
                            <>
                              <button onClick={() => setActionModal({ isOpen: true, reportId: report.id, actionType: 'SAFE' })} className="btn btn-outline-success btn-sm px-3 fw-bold">SAFE</button>
                              <button onClick={() => setActionModal({ isOpen: true, reportId: report.id, actionType: 'DANGER' })} className="btn btn-outline-danger btn-sm px-3 fw-bold">DANGER</button>
                            </>
                          )}
                        </div>
                      </div>
                    </motion.div>
                  );
                })}
              </AnimatePresence>
            </div>
          </div>
        )}

        {/* 3. MALWARE LAB */}
        {activeTab === "malware" && (
          <div className="p-4 custom-scrollbar" style={{ flex: 1, overflowY: 'auto' }}>
            <div className="row justify-content-center">
              <div className="col-lg-8">
                <div className="cyber-card p-5 text-center shadow-lg" style={{ background: 'rgba(15,23,42,0.6)' }}>
                  <FaMicrochip className="fs-1 text-primary mb-3" />
                  <h3 className="text-white mb-2" style={{ fontFamily: "Orbitron", fontWeight: '600', fontSize: '22px' }}>Malware Analysis Engine</h3>
                  <p className="text-secondary mb-4" style={{ fontSize: '13.5px' }}>Upload a suspicious file or enter a hash/URL to scan for malware signatures.</p>

                  <div className="row g-3 mb-4 text-start">
                    <div className="col-12">
                      <label className="text-light mb-2" style={{ fontSize: '12px', fontWeight: '600' }}>SCAN URL OR HASH</label>
                      <div className="input-group">
                        <span className="input-group-text bg-dark border-secondary text-secondary"><FaLink /></span>
                        <input type="text" className="form-control bg-dark text-white border-secondary" placeholder="e.g. 44d88612fea8a8f36de82e1278abb02f" value={scanInput} onChange={(e) => setScanInput(e.target.value)} />
                      </div>
                    </div>
                    <div className="col-12 text-center my-2"><small className="text-secondary">OR</small></div>
                    <div className="col-12">
                      <input type="file" id="fileInput" className="d-none" onChange={(e) => setSelectedFile(e.target.files[0])} />
                      <label htmlFor="fileInput" className="w-100 p-4 rounded d-flex flex-column align-items-center justify-content-center transition-all" style={{ border: "2px dashed #334155", cursor: "pointer", background: selectedFile ? "rgba(59, 130, 246, 0.1)" : "rgba(0,0,0,0.2)" }}>
                        {selectedFile ? (
                          <><FaCheckCircle className="fs-3 text-success mb-2" /><h6 className="text-info m-0">{selectedFile.name}</h6></>
                        ) : (
                          <><FaFileUpload className="fs-3 text-secondary mb-2" /><h6 className="text-light m-0" style={{ fontSize: '14px' }}>Click to Browse File</h6></>
                        )}
                      </label>
                    </div>
                  </div>

                  <button onClick={handleFileScan} className="btn btn-primary w-100 fw-bold py-3" disabled={scanning || (!selectedFile && !scanInput)} style={{ fontSize: '14px', borderRadius: '8px', letterSpacing: '1px' }}>
                    {scanning ? "ANALYZING PAYLOAD..." : "INITIATE SCAN"}
                  </button>

                  {scanning && (
                    <div className="mt-4 text-start">
                      <div className="progress" style={{ height: '8px', background: '#1e293b', borderRadius: '10px' }}>
                        <div className="progress-bar progress-bar-striped progress-bar-animated bg-info" style={{ width: '100%' }}></div>
                      </div>
                      <small className="text-info mt-2 d-block text-center" style={{ fontFamily: 'monospace' }}>Extracting signatures & comparing with database...</small>
                    </div>
                  )}

                  {scanResult && !scanning && (
                    <motion.div initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }} className="mt-4 text-start border p-4 rounded" style={{ borderColor: scanResult.status === "DANGER" ? '#ff003c' : '#10B981', background: 'rgba(0,0,0,0.4)' }}>
                      <h5 className={scanResult.status === "DANGER" ? "text-danger mb-3 fw-bold" : "text-success mb-3 fw-bold"} style={{ fontSize: '16px' }}>
                        {scanResult.status === "DANGER" ? "⚠️ CRITICAL THREAT DETECTED" : "✅ NO THREATS FOUND"}
                      </h5>
                      <div className="text-light" style={{ fontSize: '13px', lineHeight: '2' }}>
                        <div className="d-flex justify-content-between border-bottom border-secondary pb-1 mb-1">
                          <span className="text-secondary">File Hash (MD5):</span> <span>{scanResult.hash}</span>
                        </div>
                        <div className="d-flex justify-content-between border-bottom border-secondary pb-1 mb-1">
                          <span className="text-secondary">Threat Signature:</span> <span>{scanResult.threatType}</span>
                        </div>
                        <div className="d-flex justify-content-between">
                          <span className="text-secondary">AI Confidence Level:</span> <span>{scanResult.confidence}</span>
                        </div>
                      </div>
                    </motion.div>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* 4. LIVE CHAT */}
        {activeTab === 'chat' && (
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden', padding: '0' }}>
            <ExpertChat />
          </div>
        )}

        {/* 5. EXPERT PUBLIC PROFILE VIEW */}
        {activeTab === 'profile' && (
          <div className="p-4 custom-scrollbar" style={{ flex: 1, overflowY: 'auto' }}>
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="row justify-content-center">
              <div className="col-lg-8">
                <div className="cyber-card p-0 overflow-hidden shadow-lg" style={{ border: '1px solid rgba(0, 229, 255, 0.3)', borderRadius: '15px' }}>

                  {/* Profile Banner */}
                  <div style={{
                    height: '180px',
                    background: expertProfile.coverImage ? `url(${expertProfile.coverImage}) center/cover` : 'linear-gradient(135deg, rgba(0,243,255,0.2) 0%, rgba(157,0,255,0.2) 100%)',
                    position: 'relative',
                    borderBottom: '1px solid rgba(255,255,255,0.1)'
                  }}>
                    <div style={{ position: 'absolute', bottom: '-50px', left: '40px' }}>
                      {expertProfile.profileImage ? (
                        <img src={expertProfile.profileImage} alt="Expert" style={{ width: '110px', height: '110px', borderRadius: '50%', border: '4px solid #050810', objectFit: 'cover' }} />
                      ) : (
                        <div style={{ width: '110px', height: '110px', borderRadius: '50%', border: '4px solid #050810', background: 'rgba(0, 243, 255, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          <FaUserShield size={50} color="#00f3ff" />
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Profile Info */}
                  <div style={{ padding: '70px 40px 40px 40px', background: 'rgba(10, 15, 25, 0.8)' }}>

                    <div className="d-flex justify-content-between align-items-start mb-3">
                      <div>
                        <h2 className="text-white m-0 fw-bold" style={{ fontFamily: 'Orbitron', letterSpacing: '1px' }}>{expertProfile.username}</h2>
                        <p style={{ color: '#00E5FF', fontWeight: 'bold', fontSize: '14px', letterSpacing: '1px', marginTop: '5px' }}>{expertProfile.specialization.toUpperCase()}</p>
                      </div>
                      <div className="text-end">
                        <span className="badge px-3 py-2 d-flex align-items-center gap-1" style={{ background: 'rgba(255, 202, 0, 0.1)', color: '#ffca00', border: '1px solid rgba(255, 202, 0, 0.3)', fontSize: '14px' }}>
                          <FaStar /> {expertProfile.averageRating.toFixed(1)}
                          <span style={{ color: '#aaa', fontSize: '11px', marginLeft: '5px' }}>({expertProfile.ratingCount} Reviews)</span>
                        </span>
                      </div>
                    </div>

                    <div className="d-flex gap-3 mb-4">
                      <span className="badge bg-dark border border-secondary text-light px-3 py-2"><FaCheckCircle className="me-2 text-success" /> Verified Agent</span>
                      <span className="badge bg-dark border border-secondary text-light px-3 py-2">Prohory Security Unit</span>
                    </div>

                    <hr style={{ borderColor: 'rgba(255,255,255,0.1)' }} />

                    <div className="mt-4">
                      <h6 className="text-white fw-bold mb-3" style={{ letterSpacing: '1px' }}>ABOUT EXPERT</h6>
                      <p style={{ color: '#94a3b8', fontSize: '14px', lineHeight: '1.8', whiteSpace: 'pre-wrap' }}>
                        {expertProfile.aboutMe}
                      </p>
                    </div>

                    <div className="mt-5 p-3 rounded" style={{ background: 'rgba(0, 243, 255, 0.05)', border: '1px dashed rgba(0, 243, 255, 0.3)' }}>
                      <small className="text-info d-block text-center" style={{ fontStyle: 'italic' }}>
                        "This is how users view your profile when they connect with you via Live Chat."
                      </small>
                    </div>

                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        )}

        {/* 6. SETTINGS */}
        {activeTab === 'settings' && (
          <div className="p-4 w-100 h-100 custom-scrollbar" style={{ overflowY: 'auto' }}>
            <Settings />
          </div>
        )}

      </main>
    </div>
  );
}

export default ExpertDashboard;