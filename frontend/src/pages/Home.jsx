import React, { useState, useEffect, useRef } from 'react';
import { Container, Row, Col, Carousel, Button, Form, Card, ProgressBar, Spinner } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom'; 
import { motion, AnimatePresence } from 'framer-motion'; 
import axios from 'axios';
import { 
  FaShieldAlt, FaUserSecret, FaRobot, FaCheckCircle, FaUsers, 
  FaPaperPlane, FaBuilding, FaQuoteLeft, FaNetworkWired 
} from 'react-icons/fa';

// --- ADVANCED ANIMATION VARIANTS ---
const fadeInUp = {
  hidden: { opacity: 0, y: 60 },
  visible: { 
    opacity: 1, 
    y: 0, 
    transition: { duration: 0.8, ease: "easeOut" } 
  }
};

const fadeInLeft = {
  hidden: { opacity: 0, x: -50 },
  visible: { 
    opacity: 1, 
    x: 0, 
    transition: { duration: 0.8, ease: "easeOut" } 
  }
};

const scaleIn = {
  hidden: { opacity: 0, scale: 0.8 },
  visible: { 
    opacity: 1, 
    scale: 1, 
    transition: { duration: 0.5, ease: "backOut" } 
  }
};

// --- SERVICES DATA ---
const services = [
  { 
    icon: <FaRobot />, 
    title: 'AI Scam Detection', 
    desc: 'Our advanced neural networks analyze messages instantly to detect phishing patterns with 99.9% accuracy.' 
  },
  { 
    icon: <FaUserSecret />, 
    title: 'Expert Consultation', 
    desc: 'Get your suspicious cases reviewed by certified cyber security experts for deep verification.' 
  },
  { 
    icon: <FaNetworkWired />, 
    title: 'Real-Time Monitoring', 
    desc: '24/7 scanning of incoming threats to ensure your digital environment stays secure against attacks.' 
  }
];

// --- CUSTOM ANIMATED COUNTER COMPONENT ---
const AnimatedCounter = ({ target, suffix }) => {
  const [count, setCount] = useState(0);
  const nodeRef = useRef(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          let start = 0;
          const duration = 2000; // 2 seconds animation
          const increment = target / (duration / 16);
          
          const timer = setInterval(() => {
            start += increment;
            if (start >= target) {
              setCount(target);
              clearInterval(timer);
            } else {
              setCount(Math.ceil(start));
            }
          }, 16);
          observer.disconnect(); 
        }
      },
      { threshold: 0.5 } 
    );

    if (nodeRef.current) {
      observer.observe(nodeRef.current);
    }
    return () => observer.disconnect();
  }, [target]);

  return <span ref={nodeRef}>{count}{suffix}</span>;
};

// --- PRELOADER COMPONENT (ADVANCED CURTAIN EFFECT) ---
const Preloader = () => {
  return (
    <motion.div
      exit={{ opacity: 0, transition: { delay: 1.2, duration: 0.1 } }}
      style={{
        position: 'fixed', top: 0, left: 0, width: '100%', height: '100vh',
        zIndex: 9999, display: 'flex', justifyContent: 'center', alignItems: 'center',
      }}
    >
      {/* --- Left Curtain --- */}
      <motion.div
        initial={{ x: 0 }}
        exit={{ x: "-100%", transition: { duration: 1, ease: [0.77, 0, 0.175, 1], delay: 0.3 } }}
        style={{
          position: 'absolute', top: 0, left: 0, width: '51%', height: '100%',
          background: '#010a13', zIndex: 1, boxShadow: 'inset -5px 0 15px rgba(0, 243, 255, 0.02)'
        }}
      >
        <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', background: 'repeating-linear-gradient(0deg, rgba(0,247,255,0.03) 0px, rgba(0,247,255,0.03) 1px, transparent 1px, transparent 5px)' }}></div>
      </motion.div>

      {/* --- Right Curtain --- */}
      <motion.div
        initial={{ x: 0 }}
        exit={{ x: "100%", transition: { duration: 1, ease: [0.77, 0, 0.175, 1], delay: 0.3 } }}
        style={{
          position: 'absolute', top: 0, right: 0, width: '51%', height: '100%',
          background: '#010a13', zIndex: 1, boxShadow: 'inset 5px 0 15px rgba(0, 243, 255, 0.02)'
        }}
      >
        <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', background: 'repeating-linear-gradient(0deg, rgba(0,247,255,0.03) 0px, rgba(0,247,255,0.03) 1px, transparent 1px, transparent 5px)' }}></div>
      </motion.div>

      {/* --- Center Content --- */}
      <motion.div
        initial={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 1.5, filter: "blur(10px)", transition: { duration: 0.4, ease: "easeIn" } }}
        style={{ zIndex: 2, display: 'flex', flexDirection: 'column', alignItems: 'center' }}
      >
        <div style={{ position: 'relative', marginBottom: '40px', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
          <motion.div
            animate={{ rotate: 360 }}
            transition={{ repeat: Infinity, duration: 4, ease: "linear" }}
            style={{ width: '140px', height: '140px', borderRadius: '50%', border: '2px dashed rgba(0, 243, 255, 0.5)', position: 'absolute' }}
          />
          <motion.div
            animate={{ rotate: -360 }}
            transition={{ repeat: Infinity, duration: 2, ease: "linear" }}
            style={{ width: '110px', height: '110px', borderRadius: '50%', border: '3px solid transparent', borderTopColor: '#00f3ff', borderBottomColor: '#ff003c', position: 'absolute' }}
          />
          <motion.div
            animate={{ scale: [1, 1.1, 1] }}
            transition={{ repeat: Infinity, duration: 1.5, ease: "easeInOut" }}
          >
            <FaShieldAlt size={60} color="#00f3ff" style={{ filter: 'drop-shadow(0 0 20px #00f3ff)' }} />
          </motion.div>
        </div>
        
        <h2 className="text-white mb-4" style={{fontFamily: 'Orbitron', letterSpacing: '3px', fontWeight: 'bold'}}>
          PRO<span style={{color: '#00f3ff', textShadow: '0 0 10px #00f3ff'}}>HORY</span>
        </h2>

        <div className="d-flex flex-column align-items-center">
            <span className="text-info fw-bold" style={{ letterSpacing: '4px', fontFamily: 'monospace', fontSize: '13px' }}>
              INITIALIZING...
            </span>
            <div style={{ width: '200px', height: '2px', background: 'rgba(255,255,255,0.1)', marginTop: '15px', position: 'relative', overflow: 'hidden' }}>
                <motion.div 
                  initial={{ x: '-100%' }}
                  animate={{ x: '100%' }}
                  transition={{ repeat: Infinity, duration: 1, ease: "linear" }}
                  style={{ width: '50%', height: '100%', background: '#00f3ff', boxShadow: '0 0 10px #00f3ff' }}
                />
            </div>
        </div>
      </motion.div>
    </motion.div>
  );
};

function Home() {
  const [news, setNews] = useState([]);
  const [isLoading, setIsLoading] = useState(true); 
  const navigate = useNavigate(); 

  const partners = [
    { name: "TechCorp Global", icon: <FaBuilding /> },
    { name: "SecureNet Ltd.", icon: <FaShieldAlt /> },
    { name: "CyberDefend Inc.", icon: <FaNetworkWired /> },
    { name: "DataGuard Solutions", icon: <FaUsers /> },
    { name: "Sentinel Systems", icon: <FaCheckCircle /> },
    { name: "Apex Security", icon: <FaUserSecret /> },
    { name: "Quantum Cyber", icon: <FaRobot /> }
  ];

  const defaultNews = [
    { id: 1, title: "Global Phishing Attack Stopped", content: "Prohory AI blocked 1M+ malicious messages yesterday.", image: "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?ixlib=rb-4.0.3&auto=format&fit=crop&w=2070&q=80" },
    { id: 2, title: "Android Malware Alert", content: "New SMS stealing malware detected in South Asia.", image: "https://images.unsplash.com/photo-1563986768609-322da13575f3?ixlib=rb-4.0.3&auto=format&fit=crop&w=2070&q=80" },
    { id: 3, title: "Dark Web Leak", content: "Check if your email was compromised in the latest breach.", image: "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?ixlib=rb-4.0.3&auto=format&fit=crop&w=2070&q=80" }
  ];

  useEffect(() => {
    document.body.style.overflow = 'auto';

    const timer = setTimeout(() => {
      setIsLoading(false);
    }, 2500);

    const fetchNews = async () => {
      try {
        const res = await axios.get('https://cyber-aware.onrender.com/api/news'); 
        setNews(res.data.length > 0 ? res.data.slice(0, 3) : defaultNews);
      } catch (error) { setNews(defaultNews); }
    };
    fetchNews();

    return () => {
        clearTimeout(timer);
        document.body.style.overflow = 'auto'; 
    };
  }, []);

  // 🔥 FIX: Directly navigating to /login without any condition
  const handleLiveScannerClick = () => {
    document.body.style.overflow = 'auto';
    setIsLoading(false);
    navigate('/login');
  };

  return (
    <div className="home-wrapper" style={{ overflowX: 'hidden', height: isLoading ? '100vh' : 'auto', overflowY: isLoading ? 'hidden' : 'auto' }}>
      
      <AnimatePresence>
        {isLoading && <Preloader />}
      </AnimatePresence>

      <div style={{ opacity: isLoading ? 0 : 1, pointerEvents: isLoading ? 'none' : 'auto', transition: 'opacity 0.5s ease-in' }}>

        {/* ==================== 1. HERO SLIDER ==================== */}
        <div className="hero-slider-area position-relative">
          
          <ul className="hero-particles">
              <li></li><li></li><li></li><li></li><li></li>
              <li></li><li></li><li></li><li></li><li></li>
          </ul>

          <Carousel fade interval={3500} controls={false} indicators={true} pause={false}>
            <Carousel.Item style={{ height: '100vh', minHeight: '600px' }}>
              <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', background: 'rgba(2, 6, 12, 0.7)', zIndex: 1 }}></div>
              <img className="d-block w-100 h-100" src="https://images.unsplash.com/photo-1550751827-4bd374c3f58b?q=80&w=2070" alt="Cyber Security" style={{ objectFit: 'cover' }} />
              
              <Carousel.Caption className="d-flex flex-column justify-content-center h-100 text-start pb-5" style={{ paddingTop: '80px', zIndex: 10 }}>
                <Container>
                  <motion.div initial="hidden" animate={!isLoading ? "visible" : "hidden"} variants={fadeInLeft}>
                    <h5 className="text-info fw-bold mb-2 font-tech spacing-2">● SYSTEM: ONLINE ●</h5>
                    
                    <h1 className="hero-title text-white mb-3">
                      SECURE YOUR <br /> <span style={{ color: '#00f0ff' }}>DIGITAL UNIVERSE</span>
                    </h1>
                    
                    <p className="hero-desc-visible w-75 mb-4 d-none d-md-block">
                      Next-Gen AI Protection against Phishing, Scams, and Identity Theft. Real-time scanning engine powered by Neural Networks.
                    </p>
                    
                    <div className="d-flex gap-3">
                      <Link to="/register"><button className="btn-cyber px-4">GET STARTED</button></Link>
                      <button className="btn-cyber" style={{ filter: 'hue-rotate(45deg)' }}>DOWNLOAD APP</button>
                    </div>
                  </motion.div>
                </Container>
              </Carousel.Caption>
            </Carousel.Item>

            <Carousel.Item style={{ height: '100vh', minHeight: '600px' }}>
              <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', background: 'rgba(2, 6, 12, 0.7)', zIndex: 1 }}></div>
              <img className="d-block w-100 h-100" src="https://images.unsplash.com/photo-1510511459019-5dda7724fd87?q=80&w=2070" alt="Tech" style={{ objectFit: 'cover' }} />
              <Carousel.Caption className="d-flex flex-column justify-content-center h-100 text-center pb-5" style={{ paddingTop: '80px', zIndex: 10 }}>
                <Container>
                  <motion.div initial="hidden" animate={!isLoading ? "visible" : "hidden"} variants={scaleIn}>
                    <h1 className="hero-title text-white mb-3">AI THREAT DETECTION</h1>
                    <p className="hero-desc-visible mb-4 d-inline-block">Instantly analyze messages and URLs with 99.9% accuracy.</p>
                    <br />
                    <Button 
                      variant="danger" 
                      size="lg" 
                      className="rounded-pill px-5 fw-bold mt-3" 
                      style={{ zIndex: 20, position: 'relative', boxShadow: '0 0 20px red' }}
                      onClick={handleLiveScannerClick}
                    >
                      TRY LIVE SCANNER
                    </Button>
                  </motion.div>
                </Container>
              </Carousel.Caption>
            </Carousel.Item>
          </Carousel>
        </div>

        {/* ==================== 2. ABOUT SECTION ==================== */}
        <motion.section 
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, amount: 0.3 }}
          variants={fadeInUp}
          className="py-5"
          style={{ background: 'linear-gradient(180deg, rgba(2,6,12,0) 0%, rgba(0, 240, 255, 0.05) 100%)' }}
        >
          <Container>
            <Row className="align-items-center mb-5">
              <Col lg={6}>
                <h6 className="text-info font-tech tracking-widest mb-3">● ABOUT COMPANY</h6>
                <h2 className="section-title text-white mb-4">
                  We Stop Scams Before They Start
                </h2>
                <p className="text-secondary lead mb-5">
                  Prohory combines advanced AI algorithms with human expertise to detect threats that others miss.
                </p>
                
                <div className="mb-5">
                  <div className="mb-4">
                    <div className="d-flex justify-content-between text-white mb-2 fw-bold"><span>AI Threat Detection Accuracy</span><span>98%</span></div>
                    <ProgressBar now={98} variant="info" style={{ height: '8px', borderRadius: '10px' }} />
                  </div>
                  <div>
                    <div className="d-flex justify-content-between text-white mb-2 fw-bold"><span>Community Verification Speed</span><span>95%</span></div>
                    <ProgressBar now={95} variant="info" style={{ height: '8px', borderRadius: '10px' }} />
                  </div>
                </div>

                <div className="d-flex gap-3">
                  <Link to="/register" className="btn-cyber">Discover More</Link>
                </div>
              </Col>
              
              <Col lg={6} className="text-center">
                <motion.img 
                  variants={scaleIn}
                  src="https://cdn-icons-png.flaticon.com/512/2083/2083256.png" 
                  className="img-fluid floating-img" 
                  style={{ maxWidth: '350px', filter: 'drop-shadow(0 0 30px #00f0ff)' }} 
                  alt="AI Security" 
                />
              </Col>
            </Row>
          </Container>
        </motion.section>

        {/* ==================== 3. SERVICES SECTION ==================== */}
        <section className="service-section" style={{ background: '#02060c' }}>
          <Container>
            <div className="d-flex justify-content-between align-items-end mb-5">
              <div>
                <h6 className="text-info font-tech tracking-widest mb-2">● CAPABILITIES ●</h6>
                <h2 className="section-title text-white">Our Protection Services</h2>
              </div>
              <Button variant="outline-info" className="rounded-pill px-4 fw-bold d-none d-md-block">See All Features &rarr;</Button>
            </div>
            <Row className="g-4">
              {services.map((service, idx) => (
                <Col md={4} key={idx}>
                  <motion.div 
                    initial={{ opacity: 0, y: 50 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    transition={{ delay: idx * 0.2 }}
                    whileHover={{ scale: 1.05 }}
                    viewport={{ once: true }}
                    className="cyber-card h-100"
                  >
                    <div className="mb-4 d-inline-block text-info" style={{ fontSize: '3rem' }}>{service.icon}</div>
                    <h3 className="h4 text-white mb-3 font-tech">{service.title}</h3>
                    <p className="text-secondary">{service.desc}</p>
                    <Link to="#" className="text-info text-decoration-none fw-bold mt-3 d-inline-block">Read More <span>&rarr;</span></Link>
                  </motion.div>
                </Col>
              ))}
            </Row>
          </Container>
        </section>

        {/* ==================== 4. LIVE STATS ==================== */}
        <section style={{ background: 'linear-gradient(90deg, #050b14, #0a1525)', padding: '80px 0' }}>
          <Container>
            <Row className="text-center g-4">
              {[
                { target: 1, suffix: "M+", label: "Scams Blocked", icon: <FaShieldAlt /> },
                { target: 50, suffix: "K+", label: "Active Users", icon: <FaUsers /> },
                { target: 24, suffix: "/7", label: "Live Support", icon: <FaUserSecret /> },
                { target: 99, suffix: "%", label: "Accuracy", icon: <FaCheckCircle /> },
              ].map((stat, idx) => (
                <Col md={3} xs={6} key={idx}>
                  <motion.div 
                    initial={{ opacity: 0, scale: 0.5 }}
                    whileInView={{ opacity: 1, scale: 1 }}
                    transition={{ duration: 0.5, delay: idx * 0.1 }}
                    viewport={{ once: true }}
                    className="p-4 border border-secondary rounded bg-dark bg-opacity-25 h-100 hover-scale" 
                    style={{ borderColor: 'rgba(0, 240, 255, 0.2)' }}
                  >
                    <div className="text-info mb-3 fs-1">{stat.icon}</div>
                    <h2 className="h2 fw-bold text-white">
                      <AnimatedCounter target={stat.target} suffix={stat.suffix} />
                    </h2>
                    <p className="text-secondary text-uppercase mb-0 small font-tech">{stat.label}</p>
                  </motion.div>
                </Col>
              ))}
            </Row>
          </Container>
        </section>

        {/* ==================== 5. LATEST NEWS ==================== */}
        <section className="py-5">
          <Container>
            <div className="text-center mb-5">
              <h6 className="text-info font-tech">INTELLIGENCE FEED</h6>
              <h2 className="section-title text-white">Latest Cyber Alerts</h2>
            </div>
            <Row className="g-4">
              {news.map((item, idx) => (
                <Col md={4} key={item.id}>
                  <motion.div
                    initial={{ opacity: 0, y: 50 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    transition={{ delay: idx * 0.2, duration: 0.6, ease: "easeOut" }}
                    viewport={{ once: true }}
                    className="h-100"
                  >
                    <Card className="h-100 bg-dark border-secondary text-white overflow-hidden shadow hover-scale" style={{ borderRadius: '15px' }}>
                      <div style={{ overflow: 'hidden' }}>
                        <motion.img 
                          whileHover={{ scale: 1.1 }}
                          transition={{ duration: 0.4 }}
                          src={item.image} 
                          alt={item.title}
                          style={{ height: '220px', width: '100%', objectFit: 'cover' }} 
                        />
                      </div>
                      <Card.Body className="d-flex flex-column p-4">
                        <small className="text-info mb-3 d-block fw-bold" style={{ letterSpacing: '1px', fontSize: '11px' }}>LATEST UPDATE</small>
                        <Card.Title className="fw-bold font-tech mb-3" style={{ fontSize: '18px', lineHeight: '1.4' }}>{item.title}</Card.Title>
                        <Card.Text className="text-secondary small mb-0 mt-auto" style={{ lineHeight: '1.6' }}>
                          {item.content.length > 100 ? item.content.substring(0, 100) + '...' : item.content}
                        </Card.Text>
                      </Card.Body>
                    </Card>
                  </motion.div>
                </Col>
              ))}
            </Row>
          </Container>
        </section>

        {/* ==================== 6. CUSTOMER FEEDBACK ==================== */}
        <section style={{ backgroundColor: 'rgba(11, 17, 30, 0.5)', padding: '80px 0' }}>
          <Container>
            <motion.div 
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="text-center mb-5"
            >
              <h2 className="section-title text-white">What Our Users Say</h2>
            </motion.div>
            
            <Row className="justify-content-center">
              <Col lg={8}>
                <Carousel indicators={true} controls={false} interval={3000} pause={false} className="text-center">
                  {[
                    { name: "Rahim Ahmed", role: "Business Owner", text: "Prohory saved my business from a massive phishing attack." },
                    { name: "Sarah Khan", role: "Freelancer", text: "I love the expert chat feature. Very helpful!" },
                    { name: "John Doe", role: "Developer", text: "The API integration is seamless and the detection rate is impressive." }
                  ].map((review, idx) => (
                    <Carousel.Item key={idx}>
                      <div className="p-5 border border-info rounded bg-dark bg-opacity-75 shadow">
                        <FaQuoteLeft size={40} className="text-info mb-4" />
                        <p className="lead text-light fst-italic">"{review.text}"</p>
                        <h5 className="text-info font-tech mt-4 mb-0">{review.name}</h5>
                        <small className="text-secondary">{review.role}</small>
                      </div>
                    </Carousel.Item>
                  ))}
                </Carousel>
              </Col>
            </Row>
          </Container>
        </section>

        {/* ==================== 7. CONTACT FORM ==================== */}
        <section className="position-relative py-5">
          <Container>
            <Row className="justify-content-center">
              <Col lg={8}>
                <motion.div 
                  initial={{ scale: 0.9, opacity: 0 }}
                  whileInView={{ scale: 1, opacity: 1 }}
                  viewport={{ once: true }}
                  className="p-5 rounded border border-info position-relative cyber-card"
                >
                  <div className="text-center mb-4">
                    <h2 className="section-title text-white">Report A Vulnerability</h2>
                  </div>
                  <Form>
                    <Row className="g-3">
                      <Col md={6}><input type="text" placeholder="Your Name" className="cyber-input" /></Col>
                      <Col md={6}><input type="email" placeholder="Email Address" className="cyber-input" /></Col>
                      <Col md={12}><textarea rows={4} placeholder="Describe the threat..." className="cyber-input"></textarea></Col>
                      <Col md={12} className="text-center mt-3">
                        <Button variant="info" className="w-100 fw-bold rounded-0 text-uppercase nav-login-btn">
                          <FaPaperPlane className="me-2" /> Send Report
                        </Button>
                      </Col>
                    </Row>
                  </Form>
                </motion.div>
              </Col>
            </Row>
          </Container>
        </section>

        {/* ==================== 8. INFINITE PARTNER LOOP ==================== */}
        <section style={{ background: 'rgba(0, 240, 255, 0.02)', padding: '40px 0' }}>
          <Container fluid>
            <div className="text-center mb-4">
              <h5 className="text-white font-tech opacity-75 text-uppercase" style={{ letterSpacing: '2px' }}>Trusted By Industry Leaders</h5>
            </div>
            <div className="slider-container">
              <div className="slider-track" style={{ display: 'flex', gap: '20px' }}>
                {[...partners, ...partners].map((item, i) => (
                  <div className="slider-item" key={i} style={{ minWidth: '200px' }}>
                     <div className="bg-white rounded px-4 py-2 d-flex align-items-center justify-content-center shadow-sm w-100 h-100 hover-scale">
                       <span className="text-dark me-2 fs-5">{item.icon}</span>
                       <span className="fw-bold text-dark font-tech">{item.name}</span>
                     </div>
                  </div>
                ))}
              </div>
            </div>
          </Container>
        </section>

      </div>
    </div>
  );
}

export default Home;