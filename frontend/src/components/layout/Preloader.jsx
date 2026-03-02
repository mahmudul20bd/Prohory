import React from 'react';
import { motion } from 'framer-motion';
import { FaShieldAlt } from 'react-icons/fa';
import { Spinner } from 'react-bootstrap';

const Preloader = () => {
  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0, y: -1000, transition: { duration: 1.5, ease: [0.87, 0, 0.13, 1] } }}
      style={{
        position: 'fixed', top: 0, left: 0, width: '100%', height: '100vh',
        background: '#010a13', zIndex: 9999,
        display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center',
      }}
    >
      <div style={{ position: 'relative', marginBottom: '30px' }}>
        <motion.div
          animate={{ rotate: 360 }}
          transition={{ repeat: Infinity, duration: 3, ease: "linear" }}
          style={{
            width: '120px', height: '120px', borderRadius: '50%',
            border: '3px solid transparent', borderTopColor: 'var(--neon-cyan)', borderBottomColor: 'var(--neon-purple)',
            position: 'absolute', top: '-10px', left: '-10px'
          }}
        />
        <FaShieldAlt size={100} color="var(--neon-cyan)" style={{ filter: 'drop-shadow(0 0 15px var(--neon-cyan))' }} />
      </div>
      
      <h2 className="font-tech text-white spacing-2 mb-3">PRO<span className="text-cyan">HORY</span></h2>
      <div className="d-flex align-items-center gap-3">
          <Spinner animation="grow" variant="info" size="sm" />
          <span className="font-tech text-secondary" style={{ letterSpacing: '3px' }}>INITIALIZING SECURITY PROTOCOLS...</span>
      </div>
      
      {/* Scanning lines effect */}
      <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', background: 'repeating-linear-gradient(0deg, rgba(0,247,255,0.05) 0px, rgba(0,247,255,0.05) 1px, transparent 1px, transparent 5px)', pointerEvents: 'none' }}></div>
    </motion.div>
  );
};

export default Preloader;