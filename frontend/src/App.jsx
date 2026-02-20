import React from 'react';
import { BrowserRouter as Router, Routes, Route, useLocation } from 'react-router-dom';

// Components
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
import ExpertChat from './pages/ExpertChat'; // 🔥 এই লাইনটি মিসিং ছিল!

// Layout Component
const AppLayout = ({ children }) => {
  const location = useLocation();
  
  // এই পাথগুলোতে নেভিগেশন এবং ফুটার হাইড থাকবে
  const hideNavRoutes = [
    '/login', 
    '/register', 
    '/dashboard', 
    '/expert-dashboard', 
    '/admin-dashboard',
    '/settings',
    '/chat'
  ];

  const showNav = !hideNavRoutes.some(route => location.pathname.startsWith(route));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {showNav && <Navigation />}
      
      <div className={showNav ? "public-layout" : "dashboard-layout-wrapper"} style={{ flex: 1 }}>
        {children}
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
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          {/* Private / Dashboard Routes */}
          <Route path="/dashboard" element={<UserDashboard />} />
          <Route path="/expert-dashboard" element={<ExpertDashboard />} />
          <Route path="/admin-dashboard" element={<AdminDashboard />} /> 
          <Route path="/settings" element={<Settings />} />

          {/* 🔥 Chat Route */}
          <Route path="/chat" element={<ExpertChat />} />
        </Routes>
      </AppLayout>
    </Router>
  );
}

export default App;