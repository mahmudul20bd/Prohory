import React, { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import {
    FaPaperPlane, FaUserSecret, FaPaperclip,
    FaCircle, FaShieldAlt, FaCheckCircle, FaInbox, FaArchive, FaRobot, FaTimes, FaExclamationTriangle
} from 'react-icons/fa';
import { motion, AnimatePresence } from 'framer-motion';
import { toast } from 'react-toastify';
import axios from 'axios';
import '../styles/Chat.css';

// 🔥 Helper function to securely mask user's real username into a CASE ID
const maskUsername = (uname) => {
    if (!uname) return "CASE-UNKNOWN";
    if (uname.toUpperCase().startsWith("CASE-")) return uname.toUpperCase();

    let hash = 0;
    for (let i = 0; i < uname.length; i++) {
        hash = uname.charCodeAt(i) + ((hash << 5) - hash);
    }
    const hex = Math.abs(hash).toString(16).substring(0, 6).toUpperCase().padStart(6, '0');
    return `CASE-${hex}`;
};

const ExpertChat = () => {
    const [conversations, setConversations] = useState({});
    const [caseStatus, setCaseStatus] = useState({});
    const [activeContact, setActiveContact] = useState(null);
    const [inputValue, setInputValue] = useState("");
    const [stompClient, setStompClient] = useState(null);
    const [isUploading, setIsUploading] = useState(false);
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const [showEndModal, setShowEndModal] = useState(false);

    const fileInputRef = useRef(null);
    const messagesEndRef = useRef(null);
    const stompClientRef = useRef(null);

    const currentUser = localStorage.getItem('username') || 'Expert';
    const token = localStorage.getItem('auth_token') || localStorage.getItem('token');

    useEffect(() => {
        const savedStatus = localStorage.getItem('expert_case_status');
        if (savedStatus) setCaseStatus(JSON.parse(savedStatus));
    }, []);

    useEffect(() => {
        if (Object.keys(caseStatus).length > 0) {
            localStorage.setItem('expert_case_status', JSON.stringify(caseStatus));
        }
    }, [caseStatus]);

    useEffect(() => {
        const fetchHistory = async () => {
            try {
                const response = await fetch('https://cyber-aware.onrender.com/api/chat/all', {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                if (response.ok) {
                    const data = await response.json();
                    const grouped = {};
                    const statusObj = { ...caseStatus };
                    const involvedExperts = {}; // 🔥 Track which experts are involved in which chat

                    data.forEach(msg => {
                        if (msg.senderUsername === 'SYSTEM') return;

                        let contact = msg.senderUsername;
                        if (msg.receiverUsername === 'Support Pool' || msg.receiverUsername === 'All') {
                            contact = msg.senderUsername;
                        } else if (msg.senderUsername === currentUser) {
                            contact = msg.receiverUsername;
                        } else if (msg.receiverUsername === currentUser) {
                            contact = msg.senderUsername;
                        } else {
                            if (grouped[msg.receiverUsername]) contact = msg.receiverUsername;
                            else if (grouped[msg.senderUsername]) contact = msg.senderUsername;
                            else contact = msg.senderUsername;
                        }

                        if (!contact || contact === 'Support Pool' || contact === 'All') return;

                        if (!grouped[contact]) grouped[contact] = [];
                        grouped[contact].push(msg);

                        // Track involvement
                        if (!involvedExperts[contact]) involvedExperts[contact] = new Set();
                        if (msg.senderUsername !== contact && msg.senderUsername !== 'SYSTEM') {
                            involvedExperts[contact].add(msg.senderUsername);
                        }

                        if (!statusObj[contact]) statusObj[contact] = 'WAITING';

                        if (msg.content === '[SYS:CLOSED]') {
                            // 🔥 FIX: Resolved cases should ONLY be visible to the expert who resolved them
                            if (involvedExperts[contact].has(currentUser)) {
                                statusObj[contact] = 'RESOLVED';
                            } else {
                                statusObj[contact] = 'HIDDEN'; // Hide from other experts
                            }
                        } else if (msg.senderUsername === currentUser && statusObj[contact] !== 'RESOLVED' && statusObj[contact] !== 'HIDDEN') {
                            statusObj[contact] = 'MINE';
                        } else if (msg.senderUsername !== contact && msg.senderUsername !== currentUser && msg.senderUsername !== 'SYSTEM') {
                            if (statusObj[contact] !== 'RESOLVED' && statusObj[contact] !== 'HIDDEN') {
                                statusObj[contact] = 'TAKEN_BY_OTHER';
                            }
                        }
                    });

                    setConversations(grouped);
                    setCaseStatus(prev => ({ ...prev, ...statusObj }));
                }
            } catch (err) {
                console.error("Failed to load history", err);
            }
        };
        fetchHistory();
    }, [token, currentUser]);

    useEffect(() => {
        if (stompClientRef.current) return;

        const socket = new SockJS('https://cyber-aware.onrender.com/ws');
        const client = Stomp.over(socket);
        client.debug = null;

        client.connect(token ? { Authorization: `Bearer ${token}` } : {}, () => {
            stompClientRef.current = client;
            setStompClient(client);

            client.subscribe('/topic/messages', (sdkEvent) => {
                const msg = JSON.parse(sdkEvent.body);
                if (msg.senderUsername === 'SYSTEM') return;

                let contactUser = msg.senderUsername;
                let isMeInvolved = false;
                let isOtherExpert = false;

                if (msg.receiverUsername === 'Support Pool' || msg.receiverUsername === 'All') {
                    contactUser = msg.senderUsername;
                } else if (msg.senderUsername === currentUser) {
                    contactUser = msg.receiverUsername;
                    isMeInvolved = true;
                } else if (msg.receiverUsername === currentUser) {
                    contactUser = msg.senderUsername;
                    isMeInvolved = true;
                } else {
                    // Another expert communicating with a user
                    contactUser = msg.receiverUsername;
                    isOtherExpert = true;
                }

                if (!contactUser || contactUser === 'Support Pool' || contactUser === 'All') return;

                setCaseStatus(prev => {
                    const currentStatus = prev[contactUser];

                    if (msg.content === '[SYS:CLOSED]') {
                        // 🔥 FIX: Hide resolved cases from experts who were not involved
                        return { ...prev, [contactUser]: (currentStatus === 'MINE' || isMeInvolved) ? 'RESOLVED' : 'HIDDEN' };
                    }

                    if (isMeInvolved && currentStatus !== 'RESOLVED' && currentStatus !== 'HIDDEN') {
                        return { ...prev, [contactUser]: 'MINE' };
                    } else if (isOtherExpert && currentStatus !== 'RESOLVED' && currentStatus !== 'HIDDEN') {
                        return { ...prev, [contactUser]: 'TAKEN_BY_OTHER' };
                    } else if (!currentStatus) {
                        return { ...prev, [contactUser]: 'WAITING' };
                    }

                    return prev;
                });

                setConversations(prev => {
                    const updated = { ...prev };
                    if (!updated[contactUser]) updated[contactUser] = [];
                    if (!updated[contactUser].some(m => m.timestamp === msg.timestamp && m.content === msg.content)) {
                        updated[contactUser] = [...updated[contactUser], msg];
                    }
                    return updated;
                });
            });
        }, (error) => console.error("WebSocket Error:", error));

        return () => {
            if (stompClientRef.current) {
                stompClientRef.current.disconnect();
                stompClientRef.current = null;
            }
        };
    }, [currentUser, token]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [conversations, activeContact]);

    const sendMsg = (fileUrl = null, autoText = null) => {
        const textToSend = autoText || inputValue.trim();
        if (!textToSend && !fileUrl) return;

        if (stompClient && activeContact) {
            const msgObjToSend = {
                senderUsername: currentUser,
                receiverUsername: activeContact,
                content: textToSend || "Attached Evidence",
                fileUrl: fileUrl || null
            };
            stompClient.send("/app/chat.send", {}, JSON.stringify(msgObjToSend));
            if (!autoText) setInputValue("");
        }
    };

    const handleAcceptCase = () => {
        if (activeContact) {
            setCaseStatus(prev => ({ ...prev, [activeContact]: 'MINE' }));
            sendMsg(null, `Hello! I am ${currentUser}, a cybersecurity expert. I have accepted your case and will be assisting you today.`);
        }
    };

    const confirmEndSession = () => {
        sendMsg(null, "[SYS:CLOSED]");
        setCaseStatus(prev => ({ ...prev, [activeContact]: 'RESOLVED' }));
        setActiveContact(null);
        setShowEndModal(false);
    };

    const handleFileUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setIsUploading(true);
        const formData = new FormData();
        formData.append('file', file);
        const authHeader = { 'Authorization': token.startsWith('Bearer') ? token : `Bearer ${token}` };

        try {
            const scanResponse = await fetch('https://cyber-aware.onrender.com/api/malware/scan', {
                method: 'POST', headers: authHeader, body: formData
            });

            if (scanResponse.ok) {
                const scanData = await scanResponse.json();
                if (scanData.status === "DANGER") {
                    toast.error(`⚠️ Upload Blocked! Threat: ${scanData.threatType}`);
                    sendMsg(null, `🚨 [SYSTEM ALERT] The system blocked a malicious file attempt. (Threat: ${scanData.threatType})`);
                    setIsUploading(false); e.target.value = null; return;
                }
            }

            const response = await fetch('https://cyber-aware.onrender.com/api/chat/upload', {
                method: 'POST', headers: authHeader, body: formData
            });

            if (response.ok) {
                const data = await response.json();
                sendMsg(data.fileUrl);
            } else alert("Failed to upload file.");
        } catch (error) {
            alert("Error uploading file.");
        } finally {
            setIsUploading(false); e.target.value = null;
        }
    };

    const contactList = Object.keys(conversations);
    const waitingQueueList = contactList.filter(c => caseStatus[c] === 'WAITING');
    const activeSessionsList = contactList.filter(c => caseStatus[c] === 'MINE');
    const resolvedSessionsList = contactList.filter(c => caseStatus[c] === 'RESOLVED');
    const currentStatus = activeContact ? caseStatus[activeContact] : null;

    return (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="expert-chat-wrapper">

            <AnimatePresence>
                {showEndModal && (
                    <motion.div className="custom-modal-overlay" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                        <motion.div className="custom-modal-content" initial={{ scale: 0.9, y: 15 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.9, y: 15 }}>
                            <div className="modal-icon-wrapper">
                                <FaShieldAlt size={24} color="#ef4444" />
                            </div>
                            <h4>End This Session?</h4>
                            <p>Are you sure you want to end this secure session? The chat will be archived.</p>
                            {/* 🔥 FIX: CLEAR AND VISIBLE MODAL BUTTONS */}
                            <div className="modal-actions d-flex justify-content-center gap-3 mt-4">
                                <button
                                    className="btn btn-outline-secondary fw-bold px-4 py-2"
                                    style={{ borderRadius: '8px', color: '#cbd5e1', border: '1px solid #cbd5e1' }}
                                    onClick={() => setShowEndModal(false)}
                                >
                                    Cancel
                                </button>
                                <button
                                    className="btn fw-bold px-4 py-2 shadow"
                                    style={{ background: '#ef4444', color: '#ffffff', border: 'none', borderRadius: '8px' }}
                                    onClick={confirmEndSession}
                                >
                                    End Session
                                </button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            <button className="mobile-sidebar-toggle d-md-none" onClick={() => setIsSidebarOpen(!isSidebarOpen)}>
                {isSidebarOpen ? <FaTimes /> : <FaInbox />}
            </button>

            <div className={`chat-sidebar ${isSidebarOpen ? 'open' : ''}`}>
                <div className="sidebar-header">
                    <FaRobot style={{ color: '#00E5FF', fontSize: '18px' }} />
                    <div>
                        <h3 style={{ fontSize: '13px' }}>Communication</h3>
                        <span className="agent-badge text-muted" style={{ fontSize: '10px' }}>Agent: {currentUser}</span>
                    </div>
                </div>

                <div className="contact-list-container custom-scrollbar">
                    <div className="list-section-title">
                        Waiting Queue <span className="badge-count">{waitingQueueList.length}</span>
                    </div>
                    <AnimatePresence>
                        {waitingQueueList.map((contact) => (
                            <ContactCard key={contact} contact={contact} msgs={conversations[contact]} isActive={activeContact === contact} onClick={() => { setActiveContact(contact); setIsSidebarOpen(false); }} isWaiting={true} />
                        ))}
                        {waitingQueueList.length === 0 && <div className="empty-list-text text-center mt-3" style={{ fontSize: '12px', color: '#64748b', width: '100%' }}>No users waiting.</div>}
                    </AnimatePresence>

                    <div className="list-section-title mt-2">
                        My Active Cases <span className="badge-count active-bg">{activeSessionsList.length}</span>
                    </div>
                    <AnimatePresence>
                        {activeSessionsList.map((contact) => (
                            <ContactCard key={contact} contact={contact} msgs={conversations[contact]} isActive={activeContact === contact} onClick={() => { setActiveContact(contact); setIsSidebarOpen(false); }} />
                        ))}
                    </AnimatePresence>

                    <div className="list-section-title mt-2">
                        Resolved Cases <span className="badge-count resolved-bg">{resolvedSessionsList.length}</span>
                    </div>
                    <AnimatePresence>
                        {resolvedSessionsList.map((contact) => (
                            <ContactCard key={contact} contact={contact} msgs={conversations[contact]} isActive={activeContact === contact} onClick={() => { setActiveContact(contact); setIsSidebarOpen(false); }} isResolved={true} />
                        ))}
                    </AnimatePresence>
                </div>
            </div>

            <div className="chat-main-area">
                {activeContact ? (
                    <>
                        <div className="chat-header">
                            <div className="header-user-info">
                                {/* 🔥 MASKED USERNAME IN CHAT HEADER */}
                                <h2 style={{ fontSize: '15px', fontWeight: 'bold' }}>{maskUsername(activeContact)}</h2>
                                <p style={{ color: currentStatus === 'MINE' ? '#10B981' : currentStatus === 'RESOLVED' ? '#94a3b8' : '#F59E0B', fontSize: '11px' }}>
                                    {currentStatus === 'MINE' ? "● Secure Channel Established" : currentStatus === 'RESOLVED' ? "● Session Closed" : "● Awaiting Assignment"}
                                </p>
                            </div>
                            {currentStatus === 'MINE' && (
                                <button className="btn btn-outline-danger btn-sm rounded-pill fw-bold px-3 shadow-sm end-btn-hover" style={{ fontSize: '11px' }} onClick={() => setShowEndModal(true)}>
                                    END SESSION
                                </button>
                            )}
                        </div>

                        <div className="messages-container custom-scrollbar">
                            <AnimatePresence>
                                {(conversations[activeContact] || []).map((m, i) => {

                                    if (m.content && m.content.startsWith('🚨 [SYSTEM ALERT]')) {
                                        return (
                                            <motion.div layout initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} key={i} className="text-center my-2 w-100 d-flex justify-content-center">
                                                <div className="bg-danger text-white px-3 py-2 rounded-pill shadow-sm" style={{ fontFamily: 'Inter', fontSize: '12px', maxWidth: '80%' }}>
                                                    <FaExclamationTriangle className="me-2" /> {m.content}
                                                </div>
                                            </motion.div>
                                        );
                                    }

                                    if (m.content === '[SYS:CLOSED]') {
                                        return (
                                            <motion.div layout initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} key={i} className="text-center my-2">
                                                <span className="badge bg-secondary px-3 py-1" style={{ fontFamily: 'Inter', fontSize: '11px', letterSpacing: '1px' }}>
                                                    🔒 SESSION CLOSED BY YOU
                                                </span>
                                            </motion.div>
                                        );
                                    }

                                    const isMe = m.senderUsername === currentUser;
                                    return (
                                        <motion.div
                                            layout
                                            initial={{ opacity: 0, y: 10 }}
                                            animate={{ opacity: 1, y: 0 }}
                                            transition={{ type: "spring", stiffness: 400, damping: 25 }}
                                            key={i} className={`message-wrapper ${isMe ? 'msg-me' : 'msg-user'}`}
                                        >
                                            <div className="message-bubble">
                                                {m.fileUrl && <img src={m.fileUrl} alt="Attachment" className="chat-image" />}
                                                {m.content && m.content !== "Attached Evidence" && <span>{m.content}</span>}
                                            </div>
                                            <span className="msg-time">{m.timestamp || new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                                        </motion.div>
                                    );
                                })}
                            </AnimatePresence>
                            <div ref={messagesEndRef} />
                        </div>

                        <div className="chat-input-area">
                            {currentStatus === 'WAITING' ? (
                                <motion.div initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }} className="accept-case-massive-panel">
                                    <motion.button whileHover={{ scale: 1.03 }} whileTap={{ scale: 0.97 }} className="btn-accept-massive" onClick={handleAcceptCase}>
                                        <FaCheckCircle size={15} /> ACCEPT CASE TO REPLY
                                    </motion.button>
                                    <small className="text-secondary mt-2 d-block" style={{ fontFamily: 'Inter', fontSize: '12px' }}>
                                        Clicking this will assign the case exclusively to you.
                                    </small>
                                </motion.div>
                            ) : currentStatus === 'RESOLVED' ? (
                                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="resolved-badge">
                                    <FaArchive size={13} /> Case resolved. Read-only mode active.
                                </motion.div>
                            ) : currentStatus === 'TAKEN_BY_OTHER' ? (
                                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="resolved-badge text-warning">
                                    <FaUserSecret size={13} /> This case was claimed by another expert.
                                </motion.div>
                            ) : (
                                <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="input-box-wrapper w-100 shadow-sm">
                                    <input type="file" style={{ display: 'none' }} ref={fileInputRef} onChange={handleFileUpload} />
                                    <button className="icon-btn" onClick={() => fileInputRef.current.click()} disabled={isUploading}>
                                        {isUploading ? <div className="spinner-border spinner-border-sm text-info"></div> : <FaPaperclip size={15} />}
                                    </button>

                                    <input
                                        type="text" className="text-input"
                                        value={inputValue} onChange={(e) => setInputValue(e.target.value)}
                                        onKeyPress={(e) => e.key === 'Enter' && sendMsg()}
                                        placeholder="Type your secure message..."
                                    />

                                    <button className="icon-btn send-btn" onClick={() => sendMsg()} disabled={!inputValue.trim() && !isUploading}>
                                        <FaPaperPlane size={13} />
                                    </button>
                                </motion.div>
                            )}
                        </div>
                    </>
                ) : (
                    <div className="empty-state-container">
                        <motion.div animate={{ y: [0, -8, 0] }} transition={{ repeat: Infinity, duration: 3, ease: "easeInOut" }}>
                            <FaShieldAlt className="empty-state-icon" />
                        </motion.div>
                        <h4 className="empty-state-title">Terminal Standby</h4>
                        <p className="empty-state-desc" style={{ fontSize: '13px' }}>Select an anonymous case from the <b>Waiting Queue</b> to assign it to yourself.</p>
                    </div>
                )}
            </div>
        </motion.div>
    );
};

const ContactCard = ({ contact, msgs, isActive, onClick, isWaiting, isResolved }) => {
    const validMsgs = msgs.filter(m => m.content !== '[SYS:CLOSED]' && !m.content.startsWith('🚨 [SYSTEM ALERT]'));
    const lastMsg = validMsgs[validMsgs.length - 1];

    // 🔥 MASKED USERNAME IN SIDEBAR LIST
    const displayId = maskUsername(contact);
    const avatarLetter = displayId.split("-")[1]?.charAt(0) || 'C';

    return (
        <motion.div
            layout
            initial={{ opacity: 0, x: -15 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, height: 0, padding: 0, margin: 0 }}
            transition={{ type: "spring", stiffness: 400, damping: 25 }}
            className={`contact-card ${isActive ? 'active-card' : ''}`} onClick={onClick}
        >
            <div className="contact-avatar shadow-sm">
                {avatarLetter}
                {isWaiting && <FaCircle className="status-dot warning-dot" />}
                {isResolved && <FaArchive className="status-dot resolved-dot" />}
                {!isWaiting && !isResolved && <FaCircle className="status-dot success-dot" />}
            </div>
            <div className="contact-info">
                <h4>{displayId}</h4>
                <p className="last-msg">
                    {lastMsg?.fileUrl ? "📷 Image Attached" : lastMsg?.content}
                </p>
            </div>
        </motion.div>
    );
};

export default ExpertChat;