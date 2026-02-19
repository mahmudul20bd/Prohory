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
import Settings from './pages/Settings'; // Settings Import

// Layout Component
const AppLayout = ({ children }) => {
  const location = useLocation();
  
  // 🔥 FIX: এখানে '/settings' যুক্ত করা হয়েছে যাতে পাবলিক ফুটার হাইড থাকে
  const hideNavRoutes = [
    '/login', 
    '/register', 
    '/dashboard', 
    '/expert-dashboard', 
    '/admin-dashboard',
    '/settings' // 👈 এই লাইনটি মিসিং ছিল
  ];

  // বর্তমান পাথ যদি উপরের লিস্টের কোনোটির সাথে মিলে যায়, তাহলে showNav = false হবে
  const showNav = !hideNavRoutes.some(route => location.pathname.startsWith(route));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {/* নেভিগেশন বার (শুধু পাবলিক পেজে) */}
      {showNav && <Navigation />}
      
      {/* মেইন কন্টেন্ট */}
      <div className={showNav ? "public-layout" : "dashboard-layout-wrapper"} style={{ flex: 1 }}>
        {children}
      </div>

      {/* ফুটার (শুধু পাবলিক পেজে) */}
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
          
          {/* Settings Route */}
          <Route path="/settings" element={<Settings />} />
        </Routes>
      </AppLayout>
    </Router>
  );
}

export default App;