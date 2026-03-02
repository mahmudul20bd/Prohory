import React, { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { 
    FaPaperPlane, FaPaperclip, FaShieldAlt, FaLock, FaRobot, FaStar, 
    FaUserShield, FaCheckCircle, FaTimes, FaPowerOff, FaSyncAlt, FaHistory, FaArrowLeft
} from 'react-icons/fa';
import { motion, AnimatePresence } from 'framer-motion';
import { toast } from 'react-toastify';
import axios from 'axios';
import '../styles/Chat.css'; 

// 🔥 Helper function to parse Date from backend
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

const UserChat = () => {
    // 🔥 NEW: Chat Modes for History viewing
    const [viewMode, setViewMode] = useState('LIVE'); // 'LIVE', 'HISTORY_LIST', 'HISTORY_DETAIL'
    const [chatHistory, setChatHistory] = useState([]);
    const [selectedHistory, setSelectedHistory] = useState(null);

    const [messages, setMessages] = useState([]);
    const [inputValue, setInputValue] = useState("");
    const [stompClient, setStompClient] = useState(null);
    const [isUploading, setIsUploading] = useState(false);
    
    // States: CONNECTING, IN_QUEUE, EXPERT_JOINED, CLOSED
    const [currentState, setCurrentState] = useState('CONNECTING');
    const [expertName, setExpertName] = useState('Support Team');

    // Expert Profile States
    const [expertProfile, setExpertProfile] = useState(null);
    const [showProfileModal, setShowProfileModal] = useState(false);

    // Rating Modal States
    const [showRatingModal, setShowRatingModal] = useState(false);
    const [rating, setRating] = useState(0);
    const [hoverRating, setHoverRating] = useState(0);

    const fileInputRef = useRef(null);
    const messagesEndRef = useRef(null);
    const stompClientRef = useRef(null);

    const currentUser = localStorage.getItem('username') || 'User';
    const token = localStorage.getItem('token');

    // Format Time
    const getTime = () => {
        return new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    };

    // Auto Scroll to bottom
    useEffect(() => {
        if (viewMode === 'LIVE') messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages, viewMode]);

    // Fetch Expert Profile when an Expert Joins
    useEffect(() => {
        if (expertName && expertName !== 'Support Team') {
            const fetchExpertInfo = async () => {
                try {
                    const config = { headers: { Authorization: `Bearer ${token}` } };
                    const res = await axios.get('https://cyber-aware.onrender.com/api/users', config);
                    // Find the exact expert from the user list
                    const foundExpert = res.data.find(u => u.username === expertName);
                    if (foundExpert) {
                        setExpertProfile(foundExpert);
                    }
                } catch (error) {
                    console.error("Could not fetch expert profile:", error);
                }
            };
            fetchExpertInfo();
        }
    }, [expertName, token]);

    // Fetch Initial History & Connect WebSocket
    useEffect(() => {
        let isMounted = true;
        
        const fetchHistoryAndConnect = async () => {
            try {
                // 1. Fetch History
                const res = await fetch(`https://cyber-aware.onrender.com/api/chat/history/${currentUser}`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                
                if (res.ok) {
                    const data = await res.json();
                    
                    const pastSessions = [];
                    let activeMsgs = [];
                    let tempExp = 'Support Team';
                    let tempStart = null;

                    // 🔥 NEW: Splitting messages into separate sessions based on [SYS:CLOSED]
                    data.forEach(msg => {
                        if (!tempStart && msg.timestamp) tempStart = msg.timestamp;

                        if (msg.content === '[SYS:CLOSED]') {
                            if (activeMsgs.length > 0) {
                                pastSessions.push({
                                    id: Math.random(),
                                    expertName: tempExp,
                                    messages: [...activeMsgs],
                                    date: parseDate(tempStart).toLocaleString()
                                });
                            }
                            activeMsgs = [];
                            tempExp = 'Support Team';
                            tempStart = null;
                        } else if (msg.senderUsername !== 'SYSTEM') {
                            if (msg.senderUsername !== currentUser) tempExp = msg.senderUsername;
                            activeMsgs.push({
                                id: msg.id || Math.random(),
                                text: msg.content,
                                type: msg.senderUsername === currentUser ? 'USER' : 'EXPERT',
                                timestamp: parseDate(msg.timestamp).toLocaleTimeString('en-US', {hour:'2-digit', minute:'2-digit'}),
                                fileUrl: msg.fileUrl
                            });
                        }
                    });

                    if (isMounted) {
                        setChatHistory(pastSessions.reverse()); // Show newest history first
                        
                        // Check state of current session
                        const lastMsg = data[data.length - 1];
                        if (lastMsg && lastMsg.content === '[SYS:CLOSED]') {
                            setCurrentState('CLOSED');
                            setMessages([{ id: Math.random(), text: "🔒 The session was securely closed.", type: 'SYSTEM', timestamp: getTime() }]);
                        } else if (activeMsgs.length === 0) {
                            setCurrentState('IN_QUEUE');
                            setExpertName('Support Team');
                            setMessages([{ id: 'init', text: "Connected. You are in queue for a security expert.", type: 'SYSTEM', timestamp: getTime() }]);
                        } else {
                            setCurrentState('EXPERT_JOINED');
                            setExpertName(tempExp);
                            setMessages(activeMsgs);
                        }
                    }
                }
                
                // 2. Connect WebSocket
                if (!stompClientRef.current) {
                    const socket = new SockJS('https://cyber-aware.onrender.com/ws');
                    const client = Stomp.over(socket);
                    client.debug = null;

                    client.connect({ Authorization: `Bearer ${token}` }, () => {
                        stompClientRef.current = client;
                        setStompClient(client);

                        client.subscribe('/topic/messages', (sdkEvent) => {
                            const msg = JSON.parse(sdkEvent.body);
                            
                            if (msg.receiverUsername === currentUser || msg.receiverUsername === 'All') {
                                if (msg.senderUsername !== currentUser) {
                                    
                                    if (msg.content === '[SYS:CLOSED]') {
                                        setCurrentState('CLOSED');
                                        setMessages(prev => [...prev, { id: Math.random(), text: "🔒 The support agent has securely closed this session.", type: 'SYSTEM', timestamp: getTime() }]);
                                        setShowRatingModal(true); 
                                        return;
                                    }

                                    if (currentState !== 'EXPERT_JOINED' && currentState !== 'CLOSED') {
                                        setCurrentState('EXPERT_JOINED');
                                        setExpertName(msg.senderUsername);
                                        setMessages(prev => [...prev, { id: Math.random(), text: `${msg.senderUsername} has joined the secure channel.`, type: 'SYSTEM', timestamp: getTime() }]);
                                    }

                                    setMessages(prev => {
                                        const lastMsg = prev[prev.length - 1];
                                        if (lastMsg && lastMsg.text === msg.content) return prev;
                                        return [...prev, { id: Math.random(), text: msg.content, type: 'EXPERT', timestamp: getTime(), fileUrl: msg.fileUrl }];
                                    });
                                }
                            }
                        });
                    }, (err) => console.error("WS Error", err));
                }

            } catch (err) {
                console.error("Chat Load Error", err);
            }
        };

        fetchHistoryAndConnect();

        return () => {
            isMounted = false;
            if (stompClientRef.current) {
                stompClientRef.current.disconnect();
                stompClientRef.current = null;
            }
        };
    }, [currentUser, token]); // Removed currentState dependency

    const sendMsg = (fileUrl = null, autoText = null) => {
        const textToSend = autoText || inputValue.trim();
        if (!textToSend && !fileUrl) return;

        if (stompClient && stompClient.connected) {
            setMessages(prev => [...prev, { id: Math.random(), text: textToSend, type: 'USER', timestamp: getTime(), fileUrl: fileUrl }]);

            const receiver = currentState === 'EXPERT_JOINED' ? expertName : 'Support Pool';

            const msgObj = {
                senderUsername: currentUser,
                receiverUsername: receiver, 
                content: textToSend || "Attached Evidence",
                fileUrl: fileUrl || null
            };
            
            stompClient.send("/app/chat.send", {}, JSON.stringify(msgObj));
            if (!autoText) setInputValue("");
        } else {
            toast.error("Not connected to server.");
        }
    };

    // 🔥 NEW: User Manual End Session Logic
    const handleEndSession = () => {
        if (stompClient && stompClient.connected) {
            const receiver = currentState === 'EXPERT_JOINED' ? expertName : 'Support Pool';
            const msgObj = {
                senderUsername: currentUser,
                receiverUsername: receiver,
                content: '[SYS:CLOSED]',
                fileUrl: null
            };
            stompClient.send("/app/chat.send", {}, JSON.stringify(msgObj));

            // Move to history locally
            const closedSession = {
                id: Math.random(),
                expertName: expertName,
                messages: [...messages.filter(m => m.type !== 'SYSTEM')],
                date: new Date().toLocaleString()
            };
            if(closedSession.messages.length > 0) {
                setChatHistory(prev => [closedSession, ...prev]);
            }

            setMessages([{ id: Math.random(), text: "🔒 You ended the secure session.", type: 'SYSTEM', timestamp: getTime() }]);
            setCurrentState('CLOSED');
            
            if (currentState === 'EXPERT_JOINED') {
                setShowRatingModal(true);
            }
        }
    };

    // 🔥 NEW: Start a fresh session queue
    const startNewSession = () => {
        setCurrentState('IN_QUEUE');
        setExpertName('Support Team');
        setExpertProfile(null);
        setMessages([{ id: 'init', text: "Connected. You are in queue for a security expert...", type: 'SYSTEM', timestamp: getTime() }]);
    };

    const handleFileUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setIsUploading(true);
        const formData = new FormData();
        formData.append('file', file);
        const authHeader = { 'Authorization': `Bearer ${token}` };

        try {
            const scanRes = await fetch('https://cyber-aware.onrender.com/api/malware/scan', {
                method: 'POST', headers: authHeader, body: formData
            });
            
            if (scanRes.ok) {
                const scanData = await scanRes.json();
                if (scanData.status === "DANGER") {
                    toast.error(`Upload Blocked! Malware Detected: ${scanData.threatType}`);
                    setIsUploading(false); e.target.value = null; return;
                }
            }

            const uploadRes = await fetch('https://cyber-aware.onrender.com/api/chat/upload', {
                method: 'POST', headers: authHeader, body: formData
            });

            if (uploadRes.ok) {
                const data = await uploadRes.json();
                sendMsg(data.fileUrl, "Attached Evidence"); 
            } else {
                toast.error("Failed to upload image.");
            }
        } catch (error) {
            toast.error("Network error during upload.");
        } finally {
            setIsUploading(false); e.target.value = null; 
        }
    };

    const submitRating = async () => {
        if (rating === 0) return toast.error("Please select a rating.");
        try {
            await axios.post('https://cyber-aware.onrender.com/api/users/rate-expert', {
                expertUsername: expertName,
                userUsername: currentUser,
                rating: rating
            }, { headers: { Authorization: `Bearer ${token}` } });
            
            toast.success("Thank you for your feedback!");
            setShowRatingModal(false);
        } catch (error) {
            toast.success("Feedback submitted!"); 
            setShowRatingModal(false);
        }
    };

    // ==========================================
    // RENDERING DIFFERENT VIEWS (HISTORY VS LIVE)
    // ==========================================

    if (viewMode === 'HISTORY_LIST') {
        return (
            <div className="expert-chat-wrapper" style={{border: '1px solid rgba(0, 243, 255, 0.15)', display: 'flex', flexDirection: 'column', height: '100%', background: 'rgba(10, 15, 25, 0.9)'}}>
                <div className="p-4 custom-scrollbar" style={{flex: 1, overflowY: 'auto'}}>
                    <div className="d-flex justify-content-between align-items-center mb-4 border-bottom border-secondary pb-3">
                        <h4 className="text-info m-0" style={{fontFamily: 'Orbitron', fontWeight: 'bold'}}><FaHistory className="me-2"/> PAST SESSIONS</h4>
                        <button onClick={() => setViewMode('LIVE')} className="btn btn-sm btn-outline-info fw-bold"><FaArrowLeft className="me-2"/> BACK TO LIVE</button>
                    </div>
                    
                    {chatHistory.length === 0 ? (
                        <p className="text-secondary text-center mt-5">No past chat history found.</p>
                    ) : (
                        <div className="row g-3">
                            {chatHistory.map(session => (
                                <div key={session.id} className="col-12">
                                    <div 
                                        className="cyber-card p-3 d-flex justify-content-between align-items-center transition-all" 
                                        style={{cursor: 'pointer', background: 'rgba(0, 243, 255, 0.05)', borderLeft: '4px solid #00f3ff'}} 
                                        onClick={() => {setSelectedHistory(session); setViewMode('HISTORY_DETAIL');}}
                                    >
                                        <div>
                                            <h6 className="text-white m-0 fw-bold d-flex align-items-center"><FaUserShield className="me-2 text-info"/> Expert: {session.expertName}</h6>
                                            <small className="text-secondary d-block mt-1">{session.date}</small>
                                        </div>
                                        <span className="badge bg-dark border border-info text-info px-3 py-2">{session.messages.length} Msgs</span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        );
    }

    if (viewMode === 'HISTORY_DETAIL' && selectedHistory) {
        return (
            <div className="expert-chat-wrapper" style={{border: '1px solid rgba(0, 243, 255, 0.15)', display: 'flex', flexDirection: 'column', height: '100%', background: 'rgba(10, 15, 25, 0.9)'}}>
                <div className="chat-header d-flex justify-content-between align-items-center" style={{background: 'rgba(15, 23, 42, 0.9)', padding: '15px'}}>
                    <div>
                        <h5 className="text-white m-0 fw-bold d-flex align-items-center"><FaUserShield className="me-2 text-info"/> {selectedHistory.expertName}</h5>
                        <small className="text-secondary">{selectedHistory.date}</small>
                    </div>
                    <button onClick={() => setViewMode('HISTORY_LIST')} className="btn btn-sm btn-outline-danger fw-bold"><FaTimes className="me-1"/> CLOSE</button>
                </div>
                
                <div className="messages-container custom-scrollbar" style={{ padding: '20px', flex: 1, overflowY: 'auto' }}>
                    {selectedHistory.messages.map((m, i) => {
                        const isMe = m.type === 'USER';
                        return (
                            <motion.div key={i} className={`d-flex align-items-end mb-4 ${isMe ? 'justify-content-end' : 'justify-content-start'}`} style={{ gap: '10px' }}>
                                {!isMe && (
                                    <div className="shadow-sm" style={{width: '32px', height: '32px', borderRadius: '50%', background: 'rgba(0,243,255,0.1)', border: '1px solid rgba(0, 243, 255, 0.5)', display: 'flex', alignItems:'center', justifyContent:'center', flexShrink: 0, marginBottom: '15px'}}>
                                        <FaRobot size={14} color="#00f3ff"/>
                                    </div>
                                )}
                                <div style={{ maxWidth: '75%' }}>
                                    <div className="message-bubble shadow" style={isMe ? {background: 'linear-gradient(135deg, #00f3ff, #0072ff)', color: '#000', fontWeight: '600', padding: '12px 16px', borderRadius: '15px 15px 0 15px', fontSize: '14px'} : {background: 'rgba(15, 23, 42, 0.85)', border: '1px solid rgba(0, 243, 255, 0.2)', color: '#e2e8f0', padding: '12px 16px', borderRadius: '15px 15px 15px 0', fontSize: '14px'}}>
                                        {m.fileUrl && <img src={m.fileUrl} alt="Attachment" className="mb-2" style={{maxWidth: '100%', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.1)'}} />}
                                        {m.text && m.text !== "Attached Evidence" && <span>{m.text}</span>}
                                    </div>
                                    <div className={`msg-time mt-1 text-secondary ${isMe ? 'text-end' : 'text-start'}`} style={{fontSize: '10px'}}>{m.timestamp}</div>
                                </div>
                            </motion.div>
                        );
                    })}
                </div>
            </div>
        );
    }

    return (
        <div className="expert-chat-wrapper" style={{border: '1px solid rgba(0, 243, 255, 0.15)', display: 'flex', flexDirection: 'column', height: '100%'}}>
            
            {/* EXPERT FULL PROFILE MODAL */}
            <AnimatePresence>
                {showProfileModal && expertProfile && (
                    <motion.div className="custom-modal-overlay" initial={{opacity: 0}} animate={{opacity: 1}} exit={{opacity: 0}} style={{zIndex: 10000, position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.85)', backdropFilter: 'blur(5px)', display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '20px'}}>
                        <motion.div initial={{scale: 0.9, y: 20}} animate={{scale: 1, y: 0}} exit={{scale: 0.9, y: 20}} className="cyber-card p-0 overflow-hidden shadow-lg" style={{width: '100%', maxWidth: '450px', border: '1px solid rgba(0, 229, 255, 0.3)', borderRadius: '15px', background: '#0a0f1c'}}>
                            
                            <div style={{ height: '120px', background: expertProfile.coverImage ? `url(${expertProfile.coverImage}) center/cover` : 'linear-gradient(135deg, rgba(0,243,255,0.2) 0%, rgba(157,0,255,0.2) 100%)', position: 'relative', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
                                <button onClick={() => setShowProfileModal(false)} style={{position: 'absolute', top: '15px', right: '15px', background: 'rgba(0,0,0,0.5)', border: '1px solid rgba(0, 243, 255, 0.5)', color: '#00f3ff', borderRadius: '50%', width: '32px', height: '32px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
                                    <FaTimes />
                                </button>
                                
                                <div style={{ position: 'absolute', bottom: '-40px', left: '30px' }}>
                                    {expertProfile.profileImage ? (
                                        <img src={expertProfile.profileImage} alt="Expert" style={{ width: '85px', height: '85px', borderRadius: '50%', border: '4px solid #0a0f1c', objectFit: 'cover' }} />
                                    ) : (
                                        <div style={{ width: '85px', height: '85px', borderRadius: '50%', border: '4px solid #0a0f1c', background: 'rgba(0, 243, 255, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                            <FaUserShield size={40} color="#00f3ff" />
                                        </div>
                                    )}
                                </div>
                            </div>

                            <div style={{ padding: '55px 30px 30px 30px' }}>
                                <div className="d-flex justify-content-between align-items-start mb-3">
                                    <div>
                                        <h3 className="text-white m-0 fw-bold" style={{fontFamily: 'Orbitron', letterSpacing: '1px'}}>{expertProfile.username}</h3>
                                        <p style={{ color: '#00E5FF', fontWeight: 'bold', fontSize: '13px', letterSpacing: '1px', marginTop: '4px' }}>{expertProfile.specialization?.toUpperCase() || 'CYBER SECURITY EXPERT'}</p>
                                    </div>
                                    <div className="text-end">
                                        <span className="badge px-3 py-2 d-flex align-items-center gap-1" style={{background: 'rgba(255, 202, 0, 0.1)', color: '#ffca00', border: '1px solid rgba(255, 202, 0, 0.3)', fontSize: '14px'}}>
                                            <FaStar /> {(expertProfile.averageRating || 0).toFixed(1)} 
                                            <span style={{color: '#aaa', fontSize: '11px', marginLeft: '5px'}}>({expertProfile.ratingCount || 0})</span>
                                        </span>
                                    </div>
                                </div>

                                <div className="d-flex gap-2 mb-4">
                                    <span className="badge bg-dark border border-secondary text-light px-3 py-2" style={{fontSize: '11px'}}><FaCheckCircle className="me-2 text-success"/> Verified Prohory Agent</span>
                                </div>

                                <hr style={{ borderColor: 'rgba(255,255,255,0.1)' }} />

                                <div className="mt-3">
                                    <h6 className="text-white fw-bold mb-2" style={{letterSpacing: '1px', fontSize: '12px'}}>ABOUT EXPERT</h6>
                                    <p style={{ color: '#94a3b8', fontSize: '13px', lineHeight: '1.6', whiteSpace: 'pre-wrap', margin: 0 }}>
                                        {expertProfile.aboutMe || "This expert is a verified member of the Prohory Cyber Security unit, dedicated to protecting users from digital threats and scams."}
                                    </p>
                                </div>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* EXPERT RATING MODAL */}
            <AnimatePresence>
                {showRatingModal && (
                    <motion.div className="custom-modal-overlay" initial={{opacity: 0}} animate={{opacity: 1}} exit={{opacity: 0}}>
                        <motion.div className="custom-modal-content" initial={{scale: 0.9, y: 20}} animate={{scale: 1, y: 0}}>
                            <h4 style={{color: '#00f3ff', fontFamily: 'Orbitron'}}>Rate Support Session</h4>
                            <p className="text-secondary" style={{fontSize: '13px'}}>How was your experience with <b>{expertName}</b>?</p>
                            
                            <div className="d-flex justify-content-center gap-2 my-4">
                                {[1, 2, 3, 4, 5].map(star => (
                                    <FaStar 
                                        key={star} 
                                        size={32} 
                                        style={{cursor: 'pointer', transition: '0.2s', color: star <= (hoverRating || rating) ? '#ffca00' : '#334155'}}
                                        onMouseEnter={() => setHoverRating(star)}
                                        onMouseLeave={() => setHoverRating(0)}
                                        onClick={() => setRating(star)}
                                    />
                                ))}
                            </div>

                            <button onClick={submitRating} className="btn btn-info w-100 fw-bold mb-2">Submit Feedback</button>
                            <button onClick={() => setShowRatingModal(false)} className="btn btn-link text-secondary w-100" style={{fontSize: '12px', textDecoration: 'none'}}>Skip for now</button>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            <div className="chat-main-area w-100" style={{flex: 1, display: 'flex', flexDirection: 'column'}}>
                
                {/* 🔥 HEADER WITH HISTORY & END SESSION BUTTONS */}
                <div className="chat-header d-flex justify-content-between align-items-center" style={{background: 'rgba(15, 23, 42, 0.8)', padding: '15px'}}>
                    <div 
                        className="header-user-info d-flex align-items-center transition-all" 
                        style={{ cursor: expertProfile ? 'pointer' : 'default', padding: '5px', borderRadius: '8px' }}
                        onClick={() => expertProfile && setShowProfileModal(true)}
                        onMouseOver={(e) => expertProfile ? e.currentTarget.style.background = 'rgba(0, 243, 255, 0.05)' : null}
                        onMouseOut={(e) => e.currentTarget.style.background = 'transparent'}
                    >
                        <div className="contact-avatar me-3 shadow-sm" style={{width: '48px', height: '48px', borderRadius: '50%', overflow: 'hidden', border: '2px solid #00f3ff', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0, 243, 255, 0.1)'}}>
                            {expertProfile?.profileImage ? (
                                <img src={expertProfile.profileImage} alt="Expert" style={{width: '100%', height: '100%', objectFit: 'cover'}} />
                            ) : (
                                <FaRobot size={24} color="#00f3ff" />
                            )}
                        </div>
                        <div>
                            <h2 style={{fontSize: '16px', fontWeight: 'bold', color: '#fff', margin: 0, letterSpacing: '0.5px'}}>
                                {currentState === 'EXPERT_JOINED' ? expertName : 'Live Support Team'}
                            </h2>
                            
                            {expertProfile ? (
                                <p style={{color: '#00f3ff', fontSize: '11px', margin: 0, marginTop: '2px', fontWeight: 'bold', letterSpacing: '0.5px'}}>
                                    {expertProfile.specialization || 'Security Expert'} • <span style={{textDecoration: 'underline'}}>View Profile</span>
                                </p>
                            ) : (
                                <p style={{color: currentState === 'CLOSED' ? '#94a3b8' : '#00f3ff', fontSize: '11px', display: 'flex', alignItems: 'center', gap: '5px', margin: 0, marginTop: '3px'}}>
                                    <FaShieldAlt /> {currentState === 'CLOSED' ? 'Session Ended' : currentState === 'EXPERT_JOINED' ? 'Secure Channel Open' : 'Waiting for Expert...'}
                                </p>
                            )}
                        </div>
                    </div>

                    <div className="d-flex gap-2">
                        <button onClick={() => setViewMode('HISTORY_LIST')} className="btn btn-sm btn-outline-info fw-bold d-flex align-items-center gap-2" style={{fontSize: '11px', borderRadius: '6px', padding: '8px 12px'}}>
                            <FaHistory size={14}/> HISTORY
                        </button>
                        {currentState !== 'CLOSED' && currentState !== 'IN_QUEUE' && (
                            <button onClick={handleEndSession} className="btn btn-sm btn-outline-danger fw-bold d-flex align-items-center gap-1" style={{fontSize: '11px', borderRadius: '6px', padding: '8px 12px'}}>
                                <FaPowerOff size={14}/> END SESSION
                            </button>
                        )}
                    </div>
                </div>

                {/* Messages Body */}
                <div className="messages-container custom-scrollbar" style={{ padding: '20px', flex: 1, overflowY: 'auto' }}>
                    <AnimatePresence>
                        {messages.map((m, i) => {
                            if (m.type === 'SYSTEM') {
                                return (
                                    <motion.div layout initial={{opacity: 0, scale: 0.9}} animate={{opacity: 1, scale: 1}} key={i} className="text-center my-3">
                                        <span className="badge px-3 py-2 shadow-sm" style={{background: 'rgba(0, 243, 255, 0.1)', color: '#00f3ff', border: '1px solid rgba(0, 243, 255, 0.3)', fontFamily: 'Inter', fontSize: '11px', letterSpacing: '0.5px'}}>
                                            {m.text}
                                        </span>
                                    </motion.div>
                                );
                            }

                            const isMe = m.type === 'USER';

                            return (
                                <motion.div 
                                    layout
                                    initial={{ opacity: 0, y: 10 }} 
                                    animate={{ opacity: 1, y: 0 }}
                                    key={i} 
                                    className={`d-flex align-items-end mb-4 ${isMe ? 'justify-content-end' : 'justify-content-start'}`}
                                    style={{ gap: '10px' }}
                                >
                                    {!isMe && (
                                        <div className="shadow-sm" style={{width: '32px', height: '32px', borderRadius: '50%', overflow: 'hidden', border: '1px solid rgba(0, 243, 255, 0.5)', flexShrink: 0, marginBottom: '15px'}}>
                                            {expertProfile?.profileImage ? (
                                                <img src={expertProfile.profileImage} alt="Expert" style={{width: '100%', height: '100%', objectFit: 'cover'}} />
                                            ) : (
                                                <div style={{width: '100%', height: '100%', background: 'rgba(0,243,255,0.1)', display: 'flex', alignItems:'center', justifyContent:'center'}}>
                                                    <FaRobot size={14} color="#00f3ff"/>
                                                </div>
                                            )}
                                        </div>
                                    )}

                                    <div style={{ maxWidth: '75%' }}>
                                        <div 
                                            className="message-bubble shadow" 
                                            style={
                                                isMe 
                                                ? {background: 'linear-gradient(135deg, #00f3ff, #0072ff)', color: '#000', fontWeight: '600', padding: '12px 16px', borderRadius: '15px 15px 0 15px', fontSize: '14px', lineHeight: '1.5'} 
                                                : {background: 'rgba(15, 23, 42, 0.85)', border: '1px solid rgba(0, 243, 255, 0.2)', color: '#e2e8f0', padding: '12px 16px', borderRadius: '15px 15px 15px 0', fontSize: '14px', lineHeight: '1.5'}
                                            }
                                        >
                                            {m.fileUrl && <img src={m.fileUrl} alt="Attachment" className="mb-2" style={{maxWidth: '100%', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.1)'}} />}
                                            {m.text && m.text !== "Attached Evidence" && <span>{m.text}</span>}
                                        </div>
                                        <div className={`msg-time mt-1 text-secondary ${isMe ? 'text-end' : 'text-start'}`} style={{fontSize: '10px', letterSpacing: '0.5px'}}>{m.timestamp}</div>
                                    </div>
                                </motion.div>
                            );
                        })}
                    </AnimatePresence>
                    <div ref={messagesEndRef} />
                </div>

                {/* Input Area */}
                <div className="chat-input-area" style={{background: 'rgba(15, 23, 42, 0.9)'}}>
                    {currentState === 'CLOSED' ? (
                        <div className="resolved-badge text-center w-100 py-3 d-flex flex-column align-items-center justify-content-center" style={{color: '#94a3b8', fontSize: '14px'}}>
                            <span><FaLock className="me-2"/> Secure channel has been closed.</span>
                            <button onClick={startNewSession} className="btn btn-info fw-bold mt-3 d-flex align-items-center gap-2" style={{borderRadius: '8px', fontSize: '12px', color: '#000', padding: '10px 20px'}}>
                                <FaSyncAlt /> START NEW SESSION
                            </button>
                        </div>
                    ) : (
                        <div className="input-box-wrapper w-100 shadow-sm" style={{borderColor: 'rgba(0, 243, 255, 0.3)'}}>
                            <input type="file" style={{display: 'none'}} ref={fileInputRef} onChange={handleFileUpload} />
                            <button className="icon-btn" onClick={() => fileInputRef.current.click()} disabled={isUploading || currentState === 'CONNECTING'}>
                                {isUploading ? <div className="spinner-border spinner-border-sm text-info"></div> : <FaPaperclip size={16} color="#00f3ff"/>}
                            </button>

                            <input 
                                type="text" className="text-input"
                                value={inputValue} onChange={(e) => setInputValue(e.target.value)} 
                                onKeyPress={(e) => e.key === 'Enter' && sendMsg()}
                                placeholder="Type your message securely..." 
                                disabled={currentState === 'CONNECTING'}
                            />
                            
                            <button className="icon-btn send-btn shadow" onClick={() => sendMsg()} disabled={!inputValue.trim() || currentState === 'CONNECTING'} style={{background: '#00f3ff', color: '#000'}}>
                                <FaPaperPlane size={14}/>
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default UserChat;