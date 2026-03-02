import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { FaShieldAlt, FaFingerprint } from 'react-icons/fa';

const Preloader = () => {
  const [loadingText, setLoadingText] = useState("INITIALIZING CORE...");

  // 🔥 Cyberpunk Text Glitch Effect
  useEffect(() => {
    const texts = [
      "INITIALIZING CORE...",
      "BYPASSING FIREWALLS...",
      "ESTABLISHING SECURE LINK...",
      "SCANNING NEURAL NETWORKS...",
      "DECRYPTING PAYLOAD...",
      "SYSTEM READY."
    ];
    let i = 0;
    const interval = setInterval(() => {
      i = (i + 1) % texts.length;
      setLoadingText(texts[i]);
    }, 350); 
    return () => clearInterval(interval);
  }, []);

  return (
    <motion.div
      exit={{ opacity: 0, transition: { delay: 1.5, duration: 0.3 } }}
      style={{
        position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh',
        zIndex: 99999, display: 'flex', justifyContent: 'center', alignItems: 'center',
        background: '#010a13' // 🔥 FIX: পেছনের ব্যাকগ্রাউন্ড সলিড করে দেওয়া হয়েছে যেন হোম পেজ আগে না দেখা যায়
      }}
    >
      {/* --- বাম পাশের পর্দা (Left Curtain) --- */}
      <motion.div
        initial={{ x: 0 }}
        exit={{ x: "-100vw", transition: { duration: 1.2, ease: [0.77, 0, 0.175, 1], delay: 0.4 } }}
        style={{
          position: 'absolute', top: 0, left: 0, width: '50.5vw', height: '100%', // 50.5vw to avoid gap
          background: '#010a13', zIndex: 1
        }}
      >
        <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', background: 'repeating-linear-gradient(0deg, rgba(0,247,255,0.03) 0px, rgba(0,247,255,0.03) 1px, transparent 1px, transparent 5px)' }}></div>
      </motion.div>

      {/* --- ডান পাশের পর্দা (Right Curtain) --- */}
      <motion.div
        initial={{ x: 0 }}
        exit={{ x: "100vw", transition: { duration: 1.2, ease: [0.77, 0, 0.175, 1], delay: 0.4 } }}
        style={{
          position: 'absolute', top: 0, right: 0, width: '50.5vw', height: '100%', // 50.5vw to avoid gap
          background: '#010a13', zIndex: 1
        }}
      >
        <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', background: 'repeating-linear-gradient(0deg, rgba(0,247,255,0.03) 0px, rgba(0,247,255,0.03) 1px, transparent 1px, transparent 5px)' }}></div>
      </motion.div>

      {/* --- মাঝখানের লোডিং কন্টেন্ট (Center Content) --- */}
      <motion.div
        initial={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 2, filter: "blur(20px)", transition: { duration: 0.5, ease: "easeIn" } }}
        style={{ zIndex: 2, display: 'flex', flexDirection: 'column', alignItems: 'center' }}
      >
        {/* 🔥 NEW: Advanced Cyber HUD Animation */}
        <div style={{ position: 'relative', marginBottom: '50px', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
          
          {/* Main Shield */}
          <motion.div animate={{ opacity: [0.5, 1, 0.5] }} transition={{ repeat: Infinity, duration: 2 }}>
            <FaShieldAlt size={80} color="#00f3ff" style={{ filter: 'drop-shadow(0 0 25px #00f3ff)' }} />
          </motion.div>

          {/* Inner Cyber Fingerprint */}
          <motion.div style={{ position: 'absolute' }} animate={{ opacity: [0, 1, 0] }} transition={{ repeat: Infinity, duration: 1.5, delay: 0.5 }}>
            <FaFingerprint size={40} color="#000" />
          </motion.div>

          {/* Rotating Target Reticle 1 */}
          <motion.div
            animate={{ rotate: 360 }}
            transition={{ repeat: Infinity, duration: 6, ease: "linear" }}
            style={{ width: '160px', height: '160px', borderRadius: '50%', border: '1px solid rgba(0, 243, 255, 0.3)', borderTop: '3px solid #00f3ff', borderBottom: '3px solid #00f3ff', position: 'absolute' }}
          />

          {/* Rotating Target Reticle 2 */}
          <motion.div
            animate={{ rotate: -360 }}
            transition={{ repeat: Infinity, duration: 4, ease: "linear" }}
            style={{ width: '130px', height: '130px', borderRadius: '50%', border: '1px dashed rgba(255, 0, 60, 0.5)', position: 'absolute' }}
          />
        </div>
        
        <h2 className="text-white mb-2" style={{fontFamily: 'Orbitron', letterSpacing: '4px', fontWeight: '900', fontSize: '28px'}}>
          PRO<span style={{color: '#00f3ff', textShadow: '0 0 15px #00f3ff'}}>HORY</span>
        </h2>

        {/* 🔥 Terminal Decoding Effect */}
        <div className="d-flex flex-column align-items-center mt-3 p-3" style={{ background: 'rgba(0, 243, 255, 0.05)', border: '1px solid rgba(0, 243, 255, 0.2)', borderRadius: '4px', minWidth: '280px' }}>
            <span className="text-info fw-bold" style={{ letterSpacing: '2px', fontFamily: 'monospace', fontSize: '11px' }}>
              &gt;_ {loadingText}
            </span>
            <div style={{ width: '100%', height: '2px', background: 'rgba(255,255,255,0.1)', marginTop: '10px', position: 'relative', overflow: 'hidden' }}>
                <motion.div 
                  initial={{ x: '-100%' }}
                  animate={{ x: '100%' }}
                  transition={{ repeat: Infinity, duration: 1.5, ease: "easeInOut" }}
                  style={{ width: '30%', height: '100%', background: '#00f3ff', boxShadow: '0 0 10px #00f3ff' }}
                />
            </div>
        </div>
      </motion.div>
    </motion.div>
  );
};

export default Preloader;