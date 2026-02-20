import React, { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { FaPaperPlane, FaUserShield, FaUserAstronaut, FaUsers, FaPaperclip, FaFolderOpen, FaTimes } from 'react-icons/fa';
import { motion, AnimatePresence } from 'framer-motion';
import '../styles/Chat.css';

const ExpertChat = () => {
    // Multi-chat State
    const [conversations, setConversations] = useState({}); 
    const [activeContact, setActiveContact] = useState(null);
    const [inputValue, setInputValue] = useState("");
    const [stompClient, setStompClient] = useState(null);
    
    // Evidence Locker State
    const [showLocker, setShowLocker] = useState(false);
    const fileInputRef = useRef(null);
    
    const currentUser = localStorage.getItem('username') || 'WebUser';
    const role = (localStorage.getItem('role') || 'User').toLowerCase(); 
    const token = localStorage.getItem('token'); 
    
    const messagesEndRef = useRef(null);
    const stompClientRef = useRef(null);

    // Initial setup for users
    useEffect(() => {
        if (role === 'user' && !activeContact) {
            setActiveContact("Support Pool");
        }
    }, [role, activeContact]);

    useEffect(() => {
        if (stompClientRef.current) return; 

        const socket = new SockJS('https://cyber-aware.onrender.com/ws');
        const client = Stomp.over(socket);
        client.debug = null; 

        const headers = token ? { Authorization: `Bearer ${token}` } : {};

        client.connect(headers, () => {
            stompClientRef.current = client;
            setStompClient(client);
            
            client.subscribe('/topic/messages', (sdkEvent) => {
                const msg = JSON.parse(sdkEvent.body);
                
                let contactName = null;
                if (role === 'user') {
                    if (msg.senderUsername !== currentUser) {
                        setActiveContact(msg.senderUsername);
                    }
                    contactName = activeContact || "Support Pool";
                } else {
                    contactName = msg.senderUsername === currentUser ? msg.receiverUsername : msg.senderUsername;
                }

                if (!contactName || (contactName === 'Support Pool' && role !== 'user')) {
                    contactName = msg.senderUsername;
                }

                if (msg.senderUsername === currentUser || msg.receiverUsername === currentUser || msg.receiverUsername === 'Support Pool') {
                    setConversations(prev => {
                        const updated = { ...prev };
                        if (!updated[contactName]) updated[contactName] = [];
                        
                        if (!updated[contactName].some(m => m.timestamp === msg.timestamp && m.content === msg.content)) {
                            updated[contactName] = [...updated[contactName], msg];
                        }
                        return updated;
                    });
                }
            });
        }, (error) => console.error("WebSocket Error:", error));

        return () => {
            if (stompClientRef.current) {
                stompClientRef.current.disconnect();
                stompClientRef.current = null;
            }
        };
    }, [currentUser, role, token, activeContact]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [conversations, activeContact, showLocker]);

    // 🔥 Send Regular Message
    const sendMsg = () => {
        if (stompClient && inputValue.trim() !== "" && activeContact) {
            const msgObj = {
                senderUsername: currentUser,
                receiverUsername: activeContact,
                content: inputValue,
            };
            stompClient.send("/app/chat.send", {}, JSON.stringify(msgObj));
            setInputValue("");
        }
    };

    // 🔥 Handle File / Evidence Upload
    const handleFileUpload = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        // Check size (limit to 2MB for websocket base64 prototype)
        if (file.size > 2 * 1024 * 1024) {
            alert("For secure transmission, evidence size must be under 2MB.");
            return;
        }

        const reader = new FileReader();
        reader.onloadend = () => {
            if (stompClient && activeContact) {
                const msgObj = {
                    senderUsername: currentUser,
                    receiverUsername: activeContact,
                    content: `Attached Encrypted Evidence: ${file.name}`,
                    fileUrl: reader.result // Base64 encoded image
                };
                stompClient.send("/app/chat.send", {}, JSON.stringify(msgObj));
            }
        };
        reader.readAsDataURL(file);
    };

    const activeMessages = activeContact ? (conversations[activeContact] || []) : [];
    const contactList = Object.keys(conversations).filter(name => name !== currentUser && name !== 'Support Pool');
    
    // 🔥 Get Evidence List (Messages with images)
    const evidenceList = activeMessages.filter(m => m.fileUrl);

    // 🔥 Generate Unique Case ID based on user name
    const getCaseId = () => {
        if (!activeContact || activeContact === 'Support Pool') return 'PENDING';
        const target = role === 'user' ? currentUser : activeContact;
        return `CYB-${target.substring(0, 3).toUpperCase()}${target.length * 1024}`;
    };

    const renderChatArea = (isFullWidth) => (
        <div className={`modern-chat-wrapper ${isFullWidth ? '' : 'chat-main-area'}`} style={{ position: 'relative' }}>
            
            {/* 🔥 Header with Case ID and Evidence Locker */}
            <div className="modern-chat-header">
                <div className="chat-avatar">
                    {role === 'user' ? <FaUserShield /> : <FaUserAstronaut />}
                </div>
                <div className="chat-info" style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                        <h4>{activeContact === 'Support Pool' ? 'Waiting for Expert...' : activeContact}</h4>
                        <span className="case-id-badge">CASE ID: {getCaseId()}</span>
                    </div>
                    <p><span className="online-indicator"></span> {activeContact === 'Support Pool' ? 'Queued' : 'Online'} | Secure</p>
                </div>
                
                {/* Evidence Vault Button */}
                {activeContact !== 'Support Pool' && (
                    <button className="vault-btn" onClick={() => setShowLocker(!showLocker)}>
                        <FaFolderOpen /> Evidence Vault ({evidenceList.length})
                    </button>
                )}
            </div>

            {/* 🔥 Evidence Locker Dropdown Panel */}
            <AnimatePresence>
                {showLocker && (
                    <motion.div 
                        initial={{ opacity: 0, y: -10 }} 
                        animate={{ opacity: 1, y: 0 }} 
                        exit={{ opacity: 0, y: -10 }} 
                        className="evidence-locker-panel"
                    >
                        <div className="locker-header">
                            <h5><FaFolderOpen /> SECURE VAULT</h5>
                            <button className="close-locker" onClick={() => setShowLocker(false)}><FaTimes /></button>
                        </div>
                        <div className="locker-body">
                            {evidenceList.length === 0 ? (
                                <p className="text-secondary text-center mt-3" style={{fontSize: '0.85rem'}}>No evidence submitted yet.</p>
                            ) : (
                                evidenceList.map((ev, idx) => (
                                    <div key={idx} className="evidence-item">
                                        <img src={ev.fileUrl} alt="Evidence" />
                                        <p>Uploaded by: {ev.senderUsername}</p>
                                    </div>
                                ))
                            )}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
            
            <div className="modern-chat-body">
                {activeMessages.length === 0 && (
                    <p className="text-center text-secondary mt-5" style={{fontFamily: 'Orbitron'}}>
                        Start transmission... Provide details of your cyber issue.
                    </p>
                )}
                <AnimatePresence>
                    {activeMessages.map((m, i) => {
                        const isMe = m.senderUsername === currentUser;
                        return (
                            <motion.div 
                                initial={{ opacity: 0, y: 15, scale: 0.95 }}
                                animate={{ opacity: 1, y: 0, scale: 1 }}
                                key={i} 
                                className={`chat-message-row ${isMe ? 'me' : 'other'}`}
                            >
                                <div className="modern-bubble">
                                    {!isMe && <span className="msg-sender">{m.senderUsername}</span>}
                                    <span className="msg-text">{m.content}</span>
                                    {/* 🔥 Render Image if exists */}
                                    {m.fileUrl && (
                                        <div style={{marginTop: '8px'}}>
                                            <img src={m.fileUrl} alt="attachment" className="chat-image-preview" />
                                        </div>
                                    )}
                                </div>
                            </motion.div>
                        );
                    })}
                </AnimatePresence>
                <div ref={messagesEndRef} />
            </div>
            
            <div className="modern-chat-footer">
                {/* 🔥 File Upload Input (Hidden) */}
                <input 
                    type="file" 
                    accept="image/*" 
                    style={{display: 'none'}} 
                    ref={fileInputRef} 
                    onChange={handleFileUpload}
                />
                
                {/* Attachment Button */}
                <button 
                    className="attach-btn" 
                    title="Attach Proof" 
                    onClick={() => fileInputRef.current.click()}
                    disabled={activeContact === 'Support Pool'}
                >
                    <FaPaperclip />
                </button>

                <div className="modern-input-box">
                    <input 
                        type="text" 
                        value={inputValue} 
                        onChange={(e) => setInputValue(e.target.value)} 
                        onKeyPress={(e) => e.key === 'Enter' && sendMsg()}
                        placeholder="Type secure message..." 
                        autoFocus
                    />
                </div>
                <button onClick={sendMsg} className="modern-send-btn" disabled={!inputValue.trim()}>
                    <FaPaperPlane style={{ marginLeft: '-2px' }} />
                </button>
            </div>
        </div>
    );

    if (role === 'user') {
        return renderChatArea(true);
    }

    return (
        <div className="chat-dashboard-container">
            <div className="chat-contacts-sidebar">
                <div className="contacts-header">
                    <FaUsers /> ACTIVE CASES
                </div>
                <div className="contacts-list">
                    {contactList.length === 0 ? (
                        <p className="text-center text-secondary mt-4" style={{fontSize: '0.85rem'}}>No active cases in queue.</p>
                    ) : (
                        contactList.map((contact, index) => {
                            const lastMsg = conversations[contact][conversations[contact].length - 1];
                            return (
                                <div 
                                    key={index} 
                                    className={`contact-item ${activeContact === contact ? 'active' : ''}`}
                                    onClick={() => setActiveContact(contact)}
                                >
                                    <div className="contact-avatar">
                                        {contact.charAt(0).toUpperCase()}
                                    </div>
                                    <div className="contact-info">
                                        <h6>{contact}</h6>
                                        <p>{lastMsg.senderUsername === currentUser ? "You: " : ""}{lastMsg.fileUrl ? "📷 Attached Evidence" : lastMsg.content}</p>
                                    </div>
                                </div>
                            );
                        })
                    )}
                </div>
            </div>

            {activeContact ? (
                renderChatArea(false)
            ) : (
                <div className="empty-chat-state">
                    <FaUserAstronaut className="fs-1 mb-3 text-secondary" />
                    <h4>Select a case to provide support</h4>
                    <p>End-to-End Encrypted Secure Channel</p>
                </div>
            )}
        </div>
    );
};

export default ExpertChat;