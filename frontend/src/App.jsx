import React from 'react';
import { BrowserRouter as Router, Routes, Route, useLocation } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';

// 🔥 Styles (এই লাইনগুলোর কারণেই লোগো বা ডিজাইন হাইড হয়ে যায়)
import './App.css';
import './index.css';

// Components (আপনার অরিজিনাল লোকেশন)
import Navigation from './components/Navigation'; 
import Footer from './components/Footer'; 

// Pages
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import UserDashboard from './pages/UserDashboard';
import ExpertDashboard from './pages/ExpertDashboard';
import AdminDashboard from './pages/AdminDashboard';
import Settings from './pages/Settings'; 
import ExpertChat from './pages/ExpertChat';

// Animated Route Wrapper
const AnimatedPage = ({ children }) => {
  return (
    <motion.div
      initial={{ opacity: 0, y: 15 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -15 }}
      transition={{ duration: 0.3, ease: "easeOut" }}
      style={{ width: '100%', height: '100%' }}
    >
      {children}
    </motion.div>
  );
};

// Layout Component
const AppLayout = ({ children }) => {
  const location = useLocation();
  
  const hideNavRoutes = [
    '/login', '/register', '/dashboard', 
    '/expert-dashboard', '/admin-dashboard', '/settings', '/chat'
  ];

  const showNav = !hideNavRoutes.some(route => location.pathname.startsWith(route));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', background: '#02040A' }}>
      {showNav && <Navigation />}
      
      <div className={showNav ? "public-layout" : "dashboard-layout-wrapper"} style={{ flex: 1, overflowX: 'hidden' }}>
        <AnimatePresence mode="wait">
          {React.cloneElement(children, { key: location.pathname })}
        </AnimatePresence>
      </div>

      {showNav && <Footer />}
    </div>
  );
};

function App() {
  return (
    <Router>
      <AppLayout>
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<AnimatedPage><Home /></AnimatedPage>} />
          <Route path="/login" element={<AnimatedPage><Login /></AnimatedPage>} />
          <Route path="/register" element={<AnimatedPage><Register /></AnimatedPage>} />
          
          {/* Private / Dashboard Routes */}
          <Route path="/dashboard" element={<AnimatedPage><UserDashboard /></AnimatedPage>} />
          <Route path="/expert-dashboard" element={<AnimatedPage><ExpertDashboard /></AnimatedPage>} />
          <Route path="/admin-dashboard" element={<AnimatedPage><AdminDashboard /></AnimatedPage>} /> 
          <Route path="/settings" element={<AnimatedPage><Settings /></AnimatedPage>} />
          
          {/* Chat Route */}
          <Route path="/chat" element={<AnimatedPage><ExpertChat /></AnimatedPage>} />
        </Routes>
      </AppLayout>
    </Router>
  );
}

export default App;