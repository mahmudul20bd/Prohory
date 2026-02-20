import React, { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { FaPaperPlane, FaUserShield, FaUserAstronaut, FaUsers } from 'react-icons/fa';
import { motion, AnimatePresence } from 'framer-motion';
import '../styles/Chat.css';

const ExpertChat = () => {
    // Multi-chat State
    const [conversations, setConversations] = useState({}); 
    const [activeContact, setActiveContact] = useState(null);
    const [inputValue, setInputValue] = useState("");
    const [stompClient, setStompClient] = useState(null);
    
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
    }, [conversations, activeContact]);

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

    const activeMessages = activeContact ? (conversations[activeContact] || []) : [];
    const contactList = Object.keys(conversations).filter(name => name !== currentUser && name !== 'Support Pool');

    // 🔥 FIX: Inline Component বাদ দিয়ে রেগুলার ফাংশন ব্যবহার করা হলো (যাতে Focus Loss না হয়)
    const renderChatArea = (isFullWidth) => (
        <div className={`modern-chat-wrapper ${isFullWidth ? '' : 'chat-main-area'}`}>
            <div className="modern-chat-header">
                <div className="chat-avatar">
                    {role === 'user' ? <FaUserShield /> : <FaUserAstronaut />}
                </div>
                <div className="chat-info">
                    <h4>{activeContact === 'Support Pool' ? 'Waiting for Expert...' : activeContact}</h4>
                    <p><span className="online-indicator"></span> {activeContact === 'Support Pool' ? 'Queued' : 'Online'} | Secure</p>
                </div>
            </div>
            
            <div className="modern-chat-body">
                {activeMessages.length === 0 && (
                    <p className="text-center text-secondary mt-5" style={{fontFamily: 'Orbitron'}}>
                        Start transmission...
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
                                </div>
                            </motion.div>
                        );
                    })}
                </AnimatePresence>
                <div ref={messagesEndRef} />
            </div>
            
            <div className="modern-chat-footer">
                <div className="modern-input-box">
                    <input 
                        type="text" 
                        value={inputValue} 
                        onChange={(e) => setInputValue(e.target.value)} 
                        onKeyPress={(e) => e.key === 'Enter' && sendMsg()}
                        placeholder="Type message..." 
                        autoFocus /* ইউজারের সুবিধার জন্য অটো ফোকাস অ্যাড করা হলো */
                    />
                </div>
                <button onClick={sendMsg} className="modern-send-btn" disabled={!inputValue.trim()}>
                    <FaPaperPlane style={{ marginLeft: '-2px' }} />
                </button>
            </div>
        </div>
    );

    // ইউজারের জন্য সিঙ্গেল স্ক্রিন
    if (role === 'user') {
        return renderChatArea(true);
    }

    // এক্সপার্টের জন্য স্প্লিট স্ক্রিন (WhatsApp Web Style)
    return (
        <div className="chat-dashboard-container">
            {/* Left Sidebar: Contact List */}
            <div className="chat-contacts-sidebar">
                <div className="contacts-header">
                    <FaUsers /> ACTIVE USERS
                </div>
                <div className="contacts-list">
                    {contactList.length === 0 ? (
                        <p className="text-center text-secondary mt-4" style={{fontSize: '0.85rem'}}>No users in queue.</p>
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
                                        <p>{lastMsg.senderUsername === currentUser ? "You: " : ""}{lastMsg.content}</p>
                                    </div>
                                </div>
                            );
                        })
                    )}
                </div>
            </div>

            {/* Right Side: Main Chat Box */}
            {activeContact ? (
                renderChatArea(false)
            ) : (
                <div className="empty-chat-state">
                    <FaUserAstronaut className="fs-1 mb-3 text-secondary" />
                    <h4>Select a user to start providing support</h4>
                    <p>End-to-End Encrypted Secure Channel</p>
                </div>
            )}
        </div>
    );
};

export default ExpertChat;