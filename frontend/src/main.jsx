import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import 'bootstrap/dist/css/bootstrap.min.css'; // বুটস্ট্র্যাপ স্টাইল
import './index.css' // বেসিক স্টাইল
import './App.css'   // <--- এই লাইনটি আপনার মিসিং ছিল! এটিই মূল ডিজাইন আনবে।

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)