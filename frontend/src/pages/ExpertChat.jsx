import React, { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { FaPaperPlane, FaUserShield, FaPaperclip, FaLock, FaCircle, FaShieldAlt } from 'react-icons/fa';
import { motion, AnimatePresence } from 'framer-motion';
import '../styles/Chat.css';

const ExpertChat = () => {
    const [conversations, setConversations] = useState({});
    const [activeContact, setActiveContact] = useState(null);
    const [inputValue, setInputValue] = useState("");
    const [stompClient, setStompClient] = useState(null);
    const [isUploading, setIsUploading] = useState(false);

    const fileInputRef = useRef(null);
    const messagesEndRef = useRef(null);
    const stompClientRef = useRef(null);

    const currentUser = localStorage.getItem('username') || 'Expert';
    const role = (localStorage.getItem('role') || 'Expert').toLowerCase();
    const token = localStorage.getItem('auth_token') || localStorage.getItem('token'); 

    // 🔥 WEBSOCKET CONNECTION
    useEffect(() => {
        if (stompClientRef.current) return;

        const socket = new SockJS('https://cyber-aware.onrender.com/ws');
        const client = Stomp.over(socket);
        client.debug = null; // Hide unnecessary logs

        const headers = token ? { Authorization: `Bearer ${token}` } : {};

        client.connect(headers, () => {
            stompClientRef.current = client;
            setStompClient(client);
            
            client.subscribe('/topic/messages', (sdkEvent) => {
                const msg = JSON.parse(sdkEvent.body);
                
                // Determine the contact (Who is the expert talking to?)
                let contactName = msg.senderUsername === currentUser ? msg.receiverUsername : msg.senderUsername;
                
                // Avoid grouping system messages as users
                if (contactName === 'Support Pool' || !contactName) return;

                setConversations(prev => {
                    const updated = { ...prev };
                    if (!updated[contactName]) updated[contactName] = [];
                    
                    // Prevent duplicate messages
                    if (!updated[contactName].some(m => m.id === msg.id || (m.timestamp === msg.timestamp && m.content === msg.content))) {
                        updated[contactName] = [...updated[contactName], msg];
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

    // Auto Scroll
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [conversations, activeContact]);

    // 🔥 SEND MESSAGE
    const sendMsg = (fileUrl = null) => {
        if (stompClient && (inputValue.trim() !== "" || fileUrl) && activeContact) {
            const msgObj = {
                senderUsername: currentUser,
                receiverUsername: activeContact,
                content: inputValue.trim() || "Attached Evidence",
                fileUrl: fileUrl
            };
            
            stompClient.send("/app/chat.send", {}, JSON.stringify(msgObj));
            setInputValue("");
        }
    };

    // 🔥 HANDLE IMAGE UPLOAD VIA REST API
    const handleFileUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setIsUploading(true);
        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch('https://cyber-aware.onrender.com/api/chat/upload', {
                method: 'POST',
                headers: {
                    'Authorization': token.startsWith('Bearer') ? token : `Bearer ${token}`
                },
                body: formData
            });

            if (response.ok) {
                const data = await response.json();
                sendMsg(data.fileUrl); // Send the Cloudinary URL via WebSocket
            } else {
                alert("Failed to upload image.");
            }
        } catch (error) {
            console.error("Upload error:", error);
            alert("Error uploading file.");
        } finally {
            setIsUploading(false);
            e.target.value = null; // Reset input
        }
    };

    const activeMessages = activeContact ? (conversations[activeContact] || []) : [];
    const contactList = Object.keys(conversations);

    return (
        <div className="expert-chat-container">
            {/* 🛡️ LEFT SIDEBAR: Active Cases */}
            <div className="chat-sidebar">
                <div className="sidebar-header">
                    <FaShieldAlt className="header-icon" />
                    <h3>ACTIVE INCIDENTS</h3>
                </div>
                
                <div className="contact-list">
                    {contactList.length === 0 ? (
                        <div className="empty-contacts">No active cases right now.</div>
                    ) : (
                        contactList.map((contact, index) => {
                            const msgs = conversations[contact];
                            const lastMsg = msgs[msgs.length - 1];
                            const isActive = activeContact === contact;

                            return (
                                <div 
                                    key={index} 
                                    className={`contact-card ${isActive ? 'active-card' : ''}`}
                                    onClick={() => setActiveContact(contact)}
                                >
                                    <div className="contact-avatar">
                                        {contact.charAt(0).toUpperCase()}
                                        <FaCircle className="online-dot" />
                                    </div>
                                    <div className="contact-info">
                                        <h4>{contact}</h4>
                                        <p className="last-msg">
                                            {lastMsg.senderUsername === currentUser ? "You: " : ""}
                                            {lastMsg.fileUrl ? "📷 Image Attached" : lastMsg.content}
                                        </p>
                                    </div>
                                </div>
                            );
                        })
                    )}
                </div>
            </div>

            {/* 💻 RIGHT SIDE: Chat Interface */}
            <div className="chat-main-area">
                {activeContact ? (
                    <>
                        {/* Chat Header */}
                        <div className="chat-header">
                            <div className="header-user-info">
                                <div className="header-avatar"><FaUserShield /></div>
                                <div>
                                    <h2>{activeContact}</h2>
                                    <p><FaLock className="lock-icon"/> Secure Encrypted Channel</p>
                                </div>
                            </div>
                            <div className="case-id">CASE: #{activeContact.substring(0,4).toUpperCase()}{activeContact.length * 1024}</div>
                        </div>

                        {/* Messages Area */}
                        <div className="messages-container">
                            <AnimatePresence>
                                {activeMessages.map((m, i) => {
                                    const isMe = m.senderUsername === currentUser;
                                    return (
                                        <motion.div 
                                            initial={{ opacity: 0, y: 10 }}
                                            animate={{ opacity: 1, y: 0 }}
                                            key={i} 
                                            className={`message-wrapper ${isMe ? 'msg-me' : 'msg-user'}`}
                                        >
                                            <div className="message-bubble">
                                                {!isMe && <span className="sender-name">{m.senderUsername}</span>}
                                                
                                                {m.fileUrl && (
                                                    <img src={m.fileUrl} alt="Evidence" className="chat-image" />
                                                )}
                                                
                                                {m.content && m.content !== "Attached Evidence" && (
                                                    <span className="msg-content">{m.content}</span>
                                                )}
                                                
                                                <span className="msg-time">{m.timestamp || new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                                            </div>
                                        </motion.div>
                                    );
                                })}
                            </AnimatePresence>
                            <div ref={messagesEndRef} />
                        </div>

                        {/* Input Area */}
                        <div className="chat-input-area">
                            <input 
                                type="file" 
                                accept="image/*" 
                                style={{display: 'none'}} 
                                ref={fileInputRef} 
                                onChange={handleFileUpload}
                            />
                            
                            <button 
                                className="icon-btn attach-btn" 
                                onClick={() => fileInputRef.current.click()}
                                disabled={isUploading}
                            >
                                {isUploading ? <div className="spinner"></div> : <FaPaperclip />}
                            </button>

                            <input 
                                type="text" 
                                className="text-input"
                                value={inputValue} 
                                onChange={(e) => setInputValue(e.target.value)} 
                                onKeyPress={(e) => e.key === 'Enter' && sendMsg()}
                                placeholder="Type response to user..." 
                            />
                            
                            <button 
                                className="icon-btn send-btn" 
                                onClick={() => sendMsg()} 
                                disabled={!inputValue.trim() && !isUploading}
                            >
                                <FaPaperPlane />
                            </button>
                        </div>
                    </>
                ) : (
                    // Empty State (When no contact is selected)
                    <div className="empty-chat-state">
                        <div className="pulse-circle">
                            <FaShieldAlt className="empty-icon" />
                        </div>
                        <h3>CYBER COMMAND CENTER</h3>
                        <p>Select a case from the active incidents list to provide support.</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default ExpertChat;