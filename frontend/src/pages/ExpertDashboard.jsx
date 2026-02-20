import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import {
  FaGlobe, FaBug, FaUserSecret, FaSignOutAlt, FaExclamationTriangle,
  FaCheckCircle, FaSearch, FaCog, FaFileUpload, FaMicrochip, FaComments
} from 'react-icons/fa';

import '../styles/Dashboard.css';
import ExpertChat from './ExpertChat'; // 🔥 চ্যাট ইমপোর্ট করা হলো

function ExpertDashboard() {

  // --- STATE MANAGEMENT ---
  const [activeTab, setActiveTab] = useState('threat-map');
  const [reports, setReports] = useState([]);
  const [stats, setStats] = useState({ total: 0, critical: 0, pending: 0 });
  const [loading, setLoading] = useState(true);

  // Malware Scanner State
  const [selectedFile, setSelectedFile] = useState(null);
  const [scanResult, setScanResult] = useState(null);
  const [scanning, setScanning] = useState(false);

  const navigate = useNavigate();
  const username = localStorage.getItem('username');
  const token = localStorage.getItem('token');

  // WebSocket Ref
  const stompClientRef = useRef(null);

  // =====================================================
  // ✅ 1. REAL-TIME WEBSOCKET (NO DUPLICATE)
  // =====================================================
  useEffect(() => {
    if (!token) return;

    if (stompClientRef.current) return; // StrictMode double prevent

    const socket = new SockJS("https://cyber-aware.onrender.com/ws");
    const client = Stomp.over(socket);
    client.debug = null;

    client.connect(
      { Authorization: `Bearer ${token}` },
      () => {
        console.log("🚀 WebSocket Connected!");
        stompClientRef.current = client;

        client.subscribe("/topic/alerts", (notification) => {
          const data = JSON.parse(notification.body);

          // Toast popup once
          toast.error(
            <div style={{ fontFamily: "Orbitron", fontSize: "12px" }}>
              <strong style={{ color: "#ff003c" }}>
                🚨 {data.title}
              </strong>
              <p className="m-0 text-white">{data.message}</p>
            </div>,
            {
              position: "top-right",
              theme: "dark",
              autoClose: 4000,
              toastId: data.timestamp
            }
          );

          // Instant report add (no duplicate)
          if (data.report) {
            setReports((prev) => {
              if (prev.some(r => r.id === data.report.id)) return prev;
              return [data.report, ...prev];
            });

            // Stats update
            setStats((prevStats) => ({
              total: prevStats.total + 1,
              critical: ["SCAM", "DANGER"].includes(data.report.aiResult)
                ? prevStats.critical + 1
                : prevStats.critical,
              pending: ["SPAM", "SUSPICIOUS"].includes(data.report.aiResult)
                ? prevStats.pending + 1
                : prevStats.pending
            }));
          }
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

  // =====================================================
  // ✅ 2. FETCH REPORTS INITIALLY
  // =====================================================
  useEffect(() => {
    if (!token) {
      navigate("/login");
      return;
    }

    if (activeTab === "threat-map") {
      fetchGlobalReports();
    }
  }, [token, activeTab]);

  const fetchGlobalReports = async () => {
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };

      const res = await axios.get("https://cyber-aware.onrender.com/api/reports", config);
      const data = res.data.reverse();

      setReports(data);

      setStats({
        total: data.length,
        critical: data.filter(r => ["SCAM", "DANGER"].includes(r.aiResult)).length,
        pending: data.filter(r => ["SPAM", "SUSPICIOUS"].includes(r.aiResult)).length
      });

      setLoading(false);

    } catch (error) {
      if (error.response?.status === 403) navigate("/login");
    }
  };

  // =====================================================
  // ✅ 3. UPDATE STATUS SAFE/DANGER
  // =====================================================
  const updateStatus = async (id, newStatus) => {
    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };

      await axios.put(
        `https://cyber-aware.onrender.com/api/report/${id}?status=${newStatus}`,
        {},
        config
      );

      toast.success(`Report #${id} marked as ${newStatus}`);

      // SAFE হলে remove
      if (newStatus === "SAFE") {
        setReports(prev => prev.filter(r => r.id !== id));
      }
      else {
        setReports(prev =>
          prev.map(r =>
            r.id === id ? { ...r, aiResult: newStatus } : r
          )
        );
      }

    } catch (error) {
      toast.error("Update failed.");
    }
  };

  // =====================================================
  // ✅ 4. MALWARE SCAN LOGIC (UNCHANGED)
  // =====================================================
  const handleFileScan = async () => {
    if (!selectedFile) {
      alert("Please select a file first!");
      return;
    }

    setScanning(true);
    setScanResult(null);

    const formData = new FormData();
    formData.append("file", selectedFile);

    try {
      const config = { headers: { Authorization: `Bearer ${token}` } };

      const res = await axios.post(
        "https://cyber-aware.onrender.com/api/malware/scan",
        formData,
        config
      );

      setTimeout(() => {
        setScanResult(res.data);
        setScanning(false);
      }, 1500);

    } catch (error) {
      setScanning(false);
      alert("Scan Failed!");
    }
  };

  const handleLogout = () => {
    localStorage.clear();
    navigate("/login");
  };

  const getCardStyle = (label) => {
    const l = label ? label.toUpperCase() : "";

    if (["DANGER", "SCAM"].includes(l))
      return { borderLeft: "4px solid #ff003c", iconColor: "text-danger", icon: <FaExclamationTriangle /> };

    if (["SPAM", "SUSPICIOUS", "PROMO"].includes(l))
      return { borderLeft: "4px solid #ffca00", iconColor: "text-warning", icon: <FaSearch /> };

    return { borderLeft: "4px solid #00ff9d", iconColor: "text-success", icon: <FaCheckCircle /> };
  };

  // =====================================================
  // ✅ FULL UI (Threat Map + Malware Analysis + Chat)
  // =====================================================
  return (
    <div className="dashboard-layout" style={{ background: "#050000" }}>
      <ToastContainer pauseOnFocusLoss={false} />

      {/* SIDEBAR */}
      <aside className="sidebar">
        <div className="brand-section">
          <h3 style={{ fontFamily: "Orbitron" }}>
            CYBER<span style={{ color: "#ff003c" }}>OPS</span>
          </h3>
          <small className="text-secondary">EXPERT COMMAND</small>
        </div>

        <ul className="nav-links">
          <li
            className={`nav-item ${activeTab === "threat-map" ? "active" : ""}`}
            onClick={() => setActiveTab("threat-map")}
          >
            <FaGlobe /> THREAT MAP
          </li>

          <li
            className={`nav-item ${activeTab === "malware" ? "active" : ""}`}
            onClick={() => setActiveTab("malware")}
          >
            <FaBug /> MALWARE ANALYSIS
          </li>

          {/* 🔥 চ্যাট ট্যাব যোগ করা হলো */}
          <li
            className={`nav-item ${activeTab === "chat" ? "active" : ""}`}
            onClick={() => setActiveTab("chat")}
          >
            <FaComments /> LIVE CHAT
          </li>

          <li className="nav-item" onClick={() => navigate("/settings")}>
            <FaCog /> SETTINGS
          </li>
        </ul>

        <button onClick={handleLogout} className="btn btn-outline-danger w-100">
          <FaSignOutAlt /> TERMINATE
        </button>
      </aside>

      {/* MAIN CONTENT */}
      <main className="main-content">

        {/* HEADER */}
        <header className="dashboard-header">
          <h2 className="text-white" style={{ fontFamily: "Orbitron" }}>
            {activeTab === "threat-map" ? "THREAT INTELLIGENCE" : activeTab === "malware" ? "MALWARE LAB" : "SECURE COMMUNICATION"}
          </h2>
        </header>

        {/* THREAT MAP TAB */}
        {activeTab === "threat-map" && (
          <>
            {/* STATS */}
            <div className="row g-4 mb-4">
              <div className="col-md-4">
                <div className="cyber-card">
                  <h6>TOTAL SIGNALS</h6>
                  <div className="stat-number text-white">{stats.total}</div>
                </div>
              </div>

              <div className="col-md-4">
                <div className="cyber-card" style={{ borderColor: "#ff003c" }}>
                  <h6>CRITICAL THREATS</h6>
                  <div className="stat-number text-danger">{stats.critical}</div>
                </div>
              </div>

              <div className="col-md-4">
                <div className="cyber-card" style={{ borderColor: "#ff9100" }}>
                  <h6>SUSPICIOUS</h6>
                  <div className="stat-number text-warning">{stats.pending}</div>
                </div>
              </div>
            </div>

            {/* REPORT LIST */}
            <h5 className="text-white mb-3">INCOMING SIGNALS</h5>

            <div className="row g-3">
              <AnimatePresence>
                {reports.map((report) => {
                  const style = getCardStyle(report.aiResult);

                  return (
                    <motion.div key={report.id} className="col-12">
                      <div
                        className="cyber-card d-flex justify-content-between align-items-center p-3"
                        style={{ borderLeft: style.borderLeft }}
                      >
                        <div>
                          <p className="text-light">"{report.message}"</p>
                          <small className={style.iconColor}>
                            AI VERDICT: {report.aiResult}
                          </small>
                        </div>

                        <div className="d-flex gap-2">
                          <button
                            onClick={() => updateStatus(report.id, "SAFE")}
                            className="btn btn-outline-success btn-sm"
                          >
                            SAFE
                          </button>

                          <button
                            onClick={() => updateStatus(report.id, "DANGER")}
                            className="btn btn-outline-danger btn-sm"
                          >
                            DANGER
                          </button>
                        </div>
                      </div>
                    </motion.div>
                  );
                })}
              </AnimatePresence>
            </div>
          </>
        )}

        {/* MALWARE TAB */}
        {activeTab === "malware" && (
          <motion.div className="row justify-content-center">
            <div className="col-lg-8">
              <div className="cyber-card p-5 text-center">
                <FaMicrochip className="fs-1 text-danger mb-4" />
                <h3 style={{ fontFamily: "Orbitron" }}>
                  FILE INTEGRITY SCANNER
                </h3>

                <div className="mb-4">
                  <input
                    type="file"
                    id="fileInput"
                    className="d-none"
                    onChange={(e) => setSelectedFile(e.target.files[0])}
                  />

                  <label
                    htmlFor="fileInput"
                    className="btn btn-outline-secondary w-100 py-4"
                    style={{ borderStyle: "dashed", cursor: "pointer" }}
                  >
                    {selectedFile ? (
                      <span className="text-info">{selectedFile.name}</span>
                    ) : (
                      <>
                        <FaFileUpload className="me-2" /> UPLOAD PAYLOAD
                      </>
                    )}
                  </label>
                </div>

                <button
                  onClick={handleFileScan}
                  className="btn btn-danger w-100 fw-bold py-3"
                  disabled={scanning || !selectedFile}
                >
                  {scanning ? "SCANNING..." : "INITIATE DEEP SCAN"}
                </button>

                {scanResult && (
                  <div className="mt-4 text-start border border-secondary p-4 rounded bg-dark">
                    <h5
                      className={
                        scanResult.status === "DANGER"
                          ? "text-danger"
                          : "text-success"
                      }
                    >
                      RESULT: {scanResult.status}
                    </h5>
                    <p className="small text-secondary m-0">
                      Hash: {scanResult.hash}
                    </p>
                    <p className="small text-secondary m-0">
                      Threat: {scanResult.threatType}
                    </p>
                    <p className="small text-secondary m-0">
                      Confidence: {scanResult.confidence}
                    </p>
                  </div>
                )}
              </div>
            </div>
          </motion.div>
        )}

        {/* 🔥 VIEW: LIVE CHAT */}
        {activeTab === 'chat' && (
          <motion.div initial={{opacity: 0, y: 20}} animate={{opacity: 1, y: 0}} className="p-3">
            <ExpertChat />
          </motion.div>
        )}

      </main>
    </div>
  );
}

export default ExpertDashboard;