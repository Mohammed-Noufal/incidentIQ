import React, {useState, useEffect, Component} from 'react';
import {createRoot} from 'react-dom/client';
import {
  Activity,
  BrainCircuit,
  ShieldAlert,
  Server,
  Clock3,
  Send,
  CheckCircle2,
  AlertTriangle,
  Database,
  ArrowRight,
  RefreshCw,
  XCircle,
  Info,
  MessageSquare,
  Star,
  Search,
  ListFilter,
  FileQuestion
} from 'lucide-react';
import './style.css';
import './auth.css';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const TOKEN_KEY = 'incidentiq_access_token';

// Extract human-friendly error details from backend response
function extractErrorMessage(data, fallback = 'An unexpected error occurred') {
  if (!data) return fallback;
  if (typeof data === 'string') return data;
  if (Array.isArray(data.details) && data.details.length > 0) {
    const detailMsg = data.details.map(d => (d.field ? `${d.field}: ${d.message}` : d.message)).join('; ');
    return `${data.message || fallback} (${detailMsg})`;
  }
  return data.message || data.error || fallback;
}

// Global React Error Boundary
class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = {hasError: false, error: null};
  }

  static getDerivedStateFromError(error) {
    return {hasError: true, error};
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught an unhandled error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="auth-screen">
          <section className="auth-card" style={{maxWidth: 500}}>
            <div className="brandicon"><AlertTriangle/></div>
            <p className="eyebrow">SYSTEM ERROR BOUNDARY</p>
            <h1>Application Encountered an Error</h1>
            <p className="sub">An unexpected error occurred during rendering. Your incident data remains secure in the database.</p>
            <div className="error" style={{marginTop: 18}}>
              <p>{this.state.error?.message || 'Unknown render exception'}</p>
            </div>
            <button
              style={{marginTop: 20}}
              onClick={() => {
                this.setState({hasError: false, error: null});
                window.location.reload();
              }}
            >
              <RefreshCw size={16}/> Reload Application
            </button>
          </section>
        </div>
      );
    }
    return this.props.children;
  }
}

// Reusable Global Footer Component with keyboard accessibility and active state
function GlobalFooter({currentTab, onSelectTab}) {
  const handleNav = (tabKey) => (e) => {
    e.preventDefault();
    onSelectTab(tabKey);
  };

  const handleKey = (tabKey) => (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onSelectTab(tabKey);
    }
  };

  return (
    <footer>
      <span>IncidentIQ</span>
      <span>•</span>
      <a
        role="button"
        tabIndex={0}
        className={currentTab === 'agent' ? 'active' : ''}
        onClick={handleNav('agent')}
        onKeyDown={handleKey('agent')}
      >
        Home
      </a>
      <span>•</span>
      <a
        role="button"
        tabIndex={0}
        className={currentTab === 'incidents' ? 'active' : ''}
        onClick={handleNav('incidents')}
        onKeyDown={handleKey('incidents')}
      >
        Incidents
      </a>
      <span>•</span>
      <a
        role="button"
        tabIndex={0}
        className={currentTab === 'about' ? 'active' : ''}
        onClick={handleNav('about')}
        onKeyDown={handleKey('about')}
      >
        About
      </a>
      <span>•</span>
      <a
        role="button"
        tabIndex={0}
        className={currentTab === 'feedback' ? 'active' : ''}
        onClick={handleNav('feedback')}
        onKeyDown={handleKey('feedback')}
      >
        Feedback
      </a>
      <span>•</span>
      <span>Production-Grade Incident Response Agent • Spring Boot · MySQL · Hindsight · Groq</span>
    </footer>
  );
}

function App() {
  const [tab, setTab] = useState('agent'); // 'agent', 'incidents', 'quick', 'seed', 'about', 'feedback'
  const [services, setServices] = useState([]);
  const [accessToken, setAccessToken] = useState(() => sessionStorage.getItem(TOKEN_KEY));
  const [loginUsername, setLoginUsername] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [authMode, setAuthMode] = useState('login');
  const [authError, setAuthError] = useState('');
  const [authLoading, setAuthLoading] = useState(false);

  // Quick demo form
  const [quickForm, setQuickForm] = useState({
    serviceName: 'payments-worker',
    title: 'Elevated payment latency and intermittent 5xx errors',
    severity: 'HIGH',
    symptoms: 'p95 latency increased from 250ms to 4.8s. 18% of payment requests return HTTP 502. Started after deployment. Database connection pool usage is near 95%.'
  });
  const [quickResult, setQuickResult] = useState(null);
  const [quickLoading, setQuickLoading] = useState(false);
  const [quickError, setQuickError] = useState('');

  // Agent workflow state
  const [alertForm, setAlertForm] = useState({
    serviceName: 'payments-worker',
    severity: 'HIGH',
    title: 'Checkout latency above 8s',
    message: 'p95 checkout latency 8.4s for 6 minutes; error rate 12%',
    logExcerpt: '2026-11-05T02:03:11Z WARN HikariPool-1 - Connection is not available, request timed out after 30000ms'
  });
  const [activeIncident, setActiveIncident] = useState(null);
  const [briefing, setBriefing] = useState(null);
  const [agentLoading, setAgentLoading] = useState(false);
  const [agentError, setAgentError] = useState('');

  // Action state
  const [actionDesc, setActionDesc] = useState('Restarted cache-service');
  const [targetService, setTargetService] = useState('cache-service');
  const [outcomeVal, setOutcomeVal] = useState('WORSENED');
  const [observationVal, setObservationVal] = useState('Checkout latency doubled within 90 seconds; database CPU hit 100%.');
  const [explanationVal, setExplanationVal] = useState('The restart emptied the cache. Every request then queried the database directly, which was already out of free connections, so waiting requests piled up.');

  // Resolve state
  const [rootCauseVal, setRootCauseVal] = useState('A slow report query held database connections, exhausting the pool.');
  const [lessonVal, setLessonVal] = useState('Do not restart cache-service while the database is saturated; fix the connection holder first.');
  const [resolveResult, setResolveResult] = useState(null);

  // Seed state
  const [seedResult, setSeedResult] = useState(null);
  const [seedLoading, setSeedLoading] = useState(false);
  const [seedError, setSeedError] = useState('');

  // Incidents Explorer state
  const [incidentList, setIncidentList] = useState([]);
  const [incidentFilterStatus, setIncidentFilterStatus] = useState('ALL');
  const [incidentSearchQuery, setIncidentSearchQuery] = useState('');
  const [incidentsLoading, setIncidentsLoading] = useState(false);

  // Feedback state
  const [feedbackForm, setFeedbackForm] = useState({
    name: '',
    email: '',
    rating: 5,
    message: '',
    website: '' // honeypot
  });
  const [feedbackLoading, setFeedbackLoading] = useState(false);
  const [feedbackSuccess, setFeedbackSuccess] = useState(false);
  const [feedbackError, setFeedbackError] = useState('');

  async function apiFetch(path, options = {}) {
    const headers = new Headers(options.headers || {});
    const token = sessionStorage.getItem(TOKEN_KEY);
    if (token) headers.set('Authorization', `Bearer ${token}`);
    const response = await fetch(`${API_BASE}${path}`, {...options, headers});
    if (response.status === 401 && token) {
      logout();
    }
    return response;
  }

  useEffect(() => {
    if (!accessToken) {
      setServices([]);
      return;
    }
    apiFetch('/api/auth/me')
      .then(response => {
        if (!response.ok) throw new Error('Session expired');
        return apiFetch('/api/services');
      })
      .then(r => r.json())
      .then(data => {
        if (Array.isArray(data) && data.length > 0) setServices(data);
      })
      .catch(() => {});
  }, [accessToken]);

  // Load incidents when incidents tab is active
  useEffect(() => {
    if (!accessToken || (tab !== 'incidents' && tab !== 'agent')) return;
    loadIncidents();
  }, [accessToken, tab, incidentFilterStatus]);

  async function loadIncidents() {
    setIncidentsLoading(true);
    try {
      let queryUrl = '/api/incidents';
      if (incidentFilterStatus !== 'ALL') {
        queryUrl += `?status=${incidentFilterStatus}`;
      }
      const response = await apiFetch(queryUrl);
      if (response.ok) {
        const data = await response.json();
        setIncidentList(Array.isArray(data) ? data : []);
      }
    } catch (err) {
      console.warn('Could not load incident history:', err);
    } finally {
      setIncidentsLoading(false);
    }
  }

  async function selectIncidentByCode(code) {
    setAgentLoading(true);
    setAgentError('');
    setTab('agent');
    try {
      await refreshIncidentDetail(code);
      await requestBriefing(code);
    } catch (err) {
      setAgentError(err.message || 'Failed to load incident');
    } finally {
      setAgentLoading(false);
    }
  }

  async function submitAuth(e) {
    e.preventDefault();
    setAuthLoading(true);
    setAuthError('');
    try {
      const registering = authMode === 'register';
      const response = await fetch(`${API_BASE}/api/auth/${registering ? 'register' : 'login'}`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({username: loginUsername, password: loginPassword})
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(extractErrorMessage(data, registering ? 'Could not create account' : 'Invalid username or password'));
      }
      sessionStorage.setItem(TOKEN_KEY, data.accessToken);
      setLoginPassword('');
      setAccessToken(data.accessToken);
    } catch (err) {
      if (err.name === 'TypeError' || (err.message && err.message.toLowerCase().includes('fetch'))) {
        setAuthError(`Cannot connect to backend server at ${API_BASE}. Please ensure Spring Boot is running on port 8080.`);
      } else {
        setAuthError(err.message || 'Unable to sign in');
      }
    } finally {
      setAuthLoading(false);
    }
  }

  function logout() {
    sessionStorage.removeItem(TOKEN_KEY);
    setAccessToken(null);
    setActiveIncident(null);
    setBriefing(null);
    setQuickResult(null);
    setSeedResult(null);
    setSeedError('');
    setResolveResult(null);
    setAgentError('');
    setQuickError('');
    setAuthError('');
  }

  // Quick investigation handler
  async function runQuickInvestigate(e) {
    e.preventDefault();
    setQuickLoading(true);
    setQuickError('');
    setQuickResult(null);
    try {
      const r = await apiFetch('/api/demo/investigate', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(quickForm)
      });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(extractErrorMessage(data, 'Investigation failed'));
      setQuickResult(data);
    } catch (err) {
      setQuickError(err.message + ' — check that backend is reachable.');
    } finally {
      setQuickLoading(false);
    }
  }

  // Ingest alert and create incident
  async function submitAlert(e) {
    e.preventDefault();
    setAgentLoading(true);
    setAgentError('');
    setBriefing(null);
    setResolveResult(null);
    try {
      const r = await apiFetch('/api/alerts', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(alertForm)
      });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(extractErrorMessage(data, 'Alert creation failed'));
      
      const createdCode = data.incident?.code;
      if (createdCode) {
        // Hydrate full incident detail (including actions and alerts collections)
        await refreshIncidentDetail(createdCode);
        // Auto-trigger briefing
        await requestBriefing(createdCode);
        // Refresh explorer list
        loadIncidents();
      }
    } catch (err) {
      setAgentError(err.message);
    } finally {
      setAgentLoading(false);
    }
  }

  async function requestBriefing(code) {
    setAgentLoading(true);
    try {
      const r = await apiFetch(`/api/incidents/${code}/briefing`, {
        method: 'POST'
      });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(extractErrorMessage(data, 'Briefing failed'));
      setBriefing(data);
    } catch (err) {
      setAgentError(err.message);
    } finally {
      setAgentLoading(false);
    }
  }

  async function recordActionAttempt() {
    if (!activeIncident) return;
    if (outcomeVal === 'WORSENED' && (!explanationVal || !explanationVal.trim())) {
      setAgentError('Causal mechanism explanation is required when action outcome is WORSENED.');
      return;
    }
    setAgentLoading(true);
    setAgentError('');
    try {
      const r = await apiFetch(`/api/incidents/${activeIncident.code}/actions`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({actionDescription: actionDesc, targetService})
      });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(extractErrorMessage(data, 'Action recording failed'));
      // Now record outcome
      await recordOutcomeAttempt(data.sequenceNo);
    } catch (err) {
      setAgentError(err.message);
    } finally {
      setAgentLoading(false);
    }
  }

  async function recordOutcomeAttempt(seqNo) {
    const r = await apiFetch(`/api/incidents/${activeIncident.code}/actions/${seqNo}/outcome`, {
      method: 'PATCH',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({
        outcome: outcomeVal,
        observation: observationVal,
        explanation: outcomeVal === 'WORSENED' ? explanationVal : explanationVal || null
      })
    });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) throw new Error(extractErrorMessage(data, 'Outcome recording failed'));
    await refreshIncidentDetail(activeIncident.code);
  }

  async function refreshIncidentDetail(code) {
    const r = await apiFetch(`/api/incidents/${code}`);
    const data = await r.json().catch(() => ({}));
    if (r.ok) {
      setActiveIncident(data);
    } else {
      throw new Error(extractErrorMessage(data, `Failed to load details for ${code}`));
    }
  }

  async function resolveIncident() {
    if (!activeIncident) return;
    setAgentLoading(true);
    setAgentError('');
    try {
      const r = await apiFetch(`/api/incidents/${activeIncident.code}/resolve`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
          rootCause: rootCauseVal,
          lesson: lessonVal
        })
      });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(extractErrorMessage(data, 'Resolution failed'));
      setResolveResult(data);
      await refreshIncidentDetail(activeIncident.code);
      loadIncidents();
    } catch (err) {
      setAgentError(err.message);
    } finally {
      setAgentLoading(false);
    }
  }

  async function seedIncidents() {
    setSeedLoading(true);
    setSeedError('');
    setSeedResult(null);
    try {
      const r = await apiFetch('/api/admin/seed', {method: 'POST'});
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(extractErrorMessage(data, 'Database seeding failed'));
      setSeedResult(data);
      loadIncidents();
    } catch (err) {
      setSeedError(err.message);
    } finally {
      setSeedLoading(false);
    }
  }

  async function submitFeedback(e) {
    e.preventDefault();
    setFeedbackLoading(true);
    setFeedbackError('');
    try {
      const r = await fetch(`${API_BASE}/api/feedback`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(feedbackForm)
      });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(extractErrorMessage(data, 'Failed to submit feedback'));
      setFeedbackSuccess(true);
      setFeedbackForm({name: '', email: '', rating: 5, message: '', website: ''});
    } catch (err) {
      setFeedbackError(err.message);
    } finally {
      setFeedbackLoading(false);
    }
  }

  // Keyboard navigation helper for aside items
  const handleTabKey = (targetTab) => (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      setTab(targetTab);
    }
  };

  if (!accessToken) {
    return (
      <div className="auth-screen">
        <section className="auth-card">
          <div className="brandicon"><BrainCircuit/></div>
          <p className="eyebrow">INCIDENT OPERATIONS CENTER</p>
          <h1>{authMode === 'register' ? 'Create your account' : 'Sign in to IncidentIQ'}</h1>
          <p className="sub">{authMode === 'register' ? 'Create an account to start using IncidentIQ.' : 'Use your account credentials to continue.'}</p>
          {authError && <div className="error"><AlertTriangle/><p>{authError}</p></div>}
          <form onSubmit={submitAuth}>
            <label>Username
              <input
                autoComplete="username"
                minLength="3"
                maxLength="50"
                pattern="[a-zA-Z0-9_\-\.]+"
                value={loginUsername}
                onChange={e => setLoginUsername(e.target.value)}
                required
              />
            </label>
            <label>Password
              <input
                type="password"
                autoComplete={authMode === 'register' ? 'new-password' : 'current-password'}
                minLength={authMode === 'register' ? 12 : undefined}
                maxLength="72"
                value={loginPassword}
                onChange={e => setLoginPassword(e.target.value)}
                required
              />
            </label>
            <button disabled={authLoading}>
              {authLoading ? <><span className="spinner"/> {authMode === 'register' ? 'Creating account...' : 'Signing in...'}</> : authMode === 'register' ? 'Create account' : 'Sign in'}
            </button>
          </form>
          <p className="auth-switch">
            {authMode === 'register' ? 'Already have an account?' : 'New to IncidentIQ?'}
            {' '}
            <button type="button" className="auth-link" onClick={() => {setAuthMode(authMode === 'register' ? 'login' : 'register'); setAuthError('');}}>
              {authMode === 'register' ? 'Sign in' : 'Create account'}
            </button>
          </p>
        </section>
        <GlobalFooter currentTab={tab} onSelectTab={setTab}/>
      </div>
    );
  }

  // Filtered incidents for explorer view
  const filteredIncidents = incidentList.filter(inc => {
    if (!incidentSearchQuery) return true;
    const q = incidentSearchQuery.toLowerCase();
    return (
      (inc.code && inc.code.toLowerCase().includes(q)) ||
      (inc.title && inc.title.toLowerCase().includes(q)) ||
      (inc.serviceName && inc.serviceName.toLowerCase().includes(q)) ||
      (inc.severity && inc.severity.toLowerCase().includes(q))
    );
  });

  return (
    <div className="app">
      <aside>
        <div className="brand">
          <div className="brandicon"><BrainCircuit/></div>
          <div><b>IncidentIQ</b><small>AGENTIC MEMORY RESPONSE</small></div>
        </div>
        <nav aria-label="Main Navigation">
          <a
            role="button"
            tabIndex={0}
            className={tab === 'agent' ? 'active' : ''}
            onClick={() => setTab('agent')}
            onKeyDown={handleTabKey('agent')}
          >
            <ShieldAlert size={18}/> Incident Agent (C1-C6)
          </a>
          <a
            role="button"
            tabIndex={0}
            className={tab === 'incidents' ? 'active' : ''}
            onClick={() => setTab('incidents')}
            onKeyDown={handleTabKey('incidents')}
          >
            <ListFilter size={18}/> Incidents Explorer
          </a>
          <a
            role="button"
            tabIndex={0}
            className={tab === 'seed' ? 'active' : ''}
            onClick={() => setTab('seed')}
            onKeyDown={handleTabKey('seed')}
          >
            <Database size={18}/> Historical Memory
          </a>
          <a
            role="button"
            tabIndex={0}
            className={tab === 'quick' ? 'active' : ''}
            onClick={() => setTab('quick')}
            onKeyDown={handleTabKey('quick')}
          >
            <Activity size={18}/> Prototype Investigation
          </a>
          <a
            role="button"
            tabIndex={0}
            className={tab === 'about' ? 'active' : ''}
            onClick={() => setTab('about')}
            onKeyDown={handleTabKey('about')}
          >
            <Info size={18}/> About Platform
          </a>
          <a
            role="button"
            tabIndex={0}
            className={tab === 'feedback' ? 'active' : ''}
            onClick={() => setTab('feedback')}
            onKeyDown={handleTabKey('feedback')}
          >
            <MessageSquare size={18}/> Feedback & Support
          </a>
        </nav>
        <div className="sidefoot"><span className="dot"/> Active Monolith</div>
      </aside>

      <main>
        <header>
          <div>
            <p className="eyebrow">INCIDENT OPERATIONS CENTER / LIVE AGENT</p>
            <h1>Autonomous Incident Response Agent</h1>
            <p className="sub">Remembers what failed and verifies warnings against MySQL audit records.</p>
          </div>
          <div className="header-actions">
            <div className="status"><span className="dot"/> SYSTEM READY</div>
            <button className="logout" type="button" onClick={logout}>Sign out</button>
          </div>
        </header>

        <section className="stats">
          <div>
            <span>ACTIVE INCIDENT</span>
            <b>{activeIncident ? activeIncident.code : 'None'}</b>
            <small>{activeIncident ? activeIncident.status : 'Awaiting alert'}</small>
          </div>
          <div>
            <span>REASONING LLM</span>
            <b>Groq GPT-OSS 20B</b>
            <small>Structured output & deterministic validation</small>
          </div>
          <div>
            <span>MEMORY LAYER</span>
            <b>Hindsight Cloud</b>
            <small>Recall & retain narratives</small>
          </div>
        </section>

        {agentError && (
          <div className="error" style={{marginBottom: 16}}>
            <AlertTriangle/>
            <p>{agentError}</p>
          </div>
        )}

        {/* TAB: INCIDENT AGENT (MAIN WORKSPACE) */}
        {tab === 'agent' && (
          <div className="workspace">
            {/* Left panel: Trigger Alert & Actions */}
            <section className="panel">
              <div className="panelhead">
                <div>
                  <span className="step">01 / ALERT INGESTION</span>
                  <h2>{activeIncident ? `Incident ${activeIncident.code}` : 'Ingest New Alert'}</h2>
                </div>
                <Server/>
              </div>

              {!activeIncident ? (
                <form onSubmit={submitAlert}>
                  <label>Affected Service
                    <select
                      value={alertForm.serviceName}
                      onChange={e => setAlertForm({...alertForm, serviceName: e.target.value})}
                    >
                      {services.length > 0 ? (
                        services.map(s => <option key={s.name} value={s.name}>{s.name}</option>)
                      ) : (
                        <>
                          <option value="payments-worker">payments-worker</option>
                          <option value="orders-api">orders-api</option>
                          <option value="cache-service">cache-service</option>
                        </>
                      )}
                    </select>
                  </label>
                  <label>Severity
                    <select
                      value={alertForm.severity}
                      onChange={e => setAlertForm({...alertForm, severity: e.target.value})}
                    >
                      <option>CRITICAL</option>
                      <option>HIGH</option>
                      <option>MEDIUM</option>
                      <option>LOW</option>
                    </select>
                  </label>
                  <label>Alert Title
                    <input
                      value={alertForm.title}
                      onChange={e => setAlertForm({...alertForm, title: e.target.value})}
                      required
                    />
                  </label>
                  <label>Symptoms / Message
                    <textarea
                      rows="3"
                      value={alertForm.message}
                      onChange={e => setAlertForm({...alertForm, message: e.target.value})}
                      required
                    />
                  </label>
                  <label>Log Excerpt (Untrusted Data)
                    <textarea
                      rows="2"
                      value={alertForm.logExcerpt}
                      onChange={e => setAlertForm({...alertForm, logExcerpt: e.target.value})}
                    />
                  </label>
                  <button disabled={agentLoading}>
                    {agentLoading ? <><span className="spinner"/> Ingesting...</> : <><Send size={16}/> Trigger Alert & Briefing</>}
                  </button>
                </form>
              ) : (
                <div style={{display: 'grid', gap: 16}}>
                  <div className="memoryline">
                    <CheckCircle2 size={16}/> Active Incident: <b>{activeIncident.code}</b> ({activeIncident.status})
                  </div>

                  {/* Actions tried */}
                  <div>
                    <h3 style={{fontSize: 14, color: '#e5edf8', margin: '8px 0'}}>1. Record Action Attempt</h3>
                    <label>Action Description
                      <input value={actionDesc} onChange={e => setActionDesc(e.target.value)}/>
                    </label>
                    <label style={{marginTop: 8}}>Outcome
                      <select value={outcomeVal} onChange={e => setOutcomeVal(e.target.value)}>
                        <option value="WORSENED">WORSENED (Made situation worse)</option>
                        <option value="NO_EFFECT">NO_EFFECT (No change)</option>
                        <option value="RESOLVED">RESOLVED (Fixed the issue)</option>
                      </select>
                    </label>
                    {outcomeVal === 'WORSENED' && (
                      <label style={{marginTop: 8}}>Causal Mechanism (Required for WORSENED)
                        <textarea rows="2" value={explanationVal} onChange={e => setExplanationVal(e.target.value)} required/>
                      </label>
                    )}
                    <button style={{marginTop: 10}} onClick={recordActionAttempt} disabled={agentLoading || activeIncident.status === 'RESOLVED'}>
                      Record Action & Outcome
                    </button>
                  </div>

                  {/* Actions History */}
                  {activeIncident.actions && activeIncident.actions.length > 0 && (
                    <div style={{background: '#0b1426', padding: 12, borderRadius: 6}}>
                      <b style={{fontSize: 12, color: '#8ca0b9'}}>Recorded Actions:</b>
                      {activeIncident.actions.map(a => (
                        <div key={a.sequenceNo} style={{fontSize: 12, marginTop: 6, borderBottom: '1px solid #1c2b42', paddingBottom: 6}}>
                          Action {a.sequenceNo}: {a.actionDescription} ➔ <b style={{color: a.outcome === 'WORSENED' ? '#ff8b8b' : '#54d6a5'}}>{a.outcome || 'Pending'}</b>
                          {a.explanation && <div style={{fontSize: 11, color: '#8ca0b9'}}>Mechanism: {a.explanation}</div>}
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Resolve & Retain */}
                  {activeIncident.status !== 'RESOLVED' ? (
                    <div style={{borderTop: '1px solid #20304a', paddingTop: 12}}>
                      <h3 style={{fontSize: 14, color: '#e5edf8', margin: '8px 0'}}>2. Resolve & Retain to Memory</h3>
                      <label>Root Cause
                        <textarea rows="2" value={rootCauseVal} onChange={e => setRootCauseVal(e.target.value)}/>
                      </label>
                      <label style={{marginTop: 8}}>Lesson Learned
                        <textarea rows="2" value={lessonVal} onChange={e => setLessonVal(e.target.value)}/>
                      </label>
                      <button style={{marginTop: 10, background: '#54d6a5'}} onClick={resolveIncident} disabled={agentLoading}>
                        Resolve Incident & Sync Memory
                      </button>
                    </div>
                  ) : (
                    <div className="memoryline" style={{background: '#13392f'}}>
                      <CheckCircle2 size={16}/> Incident Resolved. Memory sync: <b>{activeIncident.memorySyncStatus}</b>
                    </div>
                  )}

                  <button style={{background: '#233854', color: '#e8edf7', marginTop: 8}} onClick={() => { setActiveIncident(null); setBriefing(null); }}>
                    Open Another Incident
                  </button>
                </div>
              )}
            </section>

            {/* Right panel: Two Distinct Panels (Constraint C6) */}
            <section className="panel">
              <div className="panelhead">
                <div>
                  <span className="step">02 / AGENT BRIEFING (C6 SEPARATION)</span>
                  <h2>Memory Recall & AI Reasoning</h2>
                </div>
                <BrainCircuit/>
              </div>

              {agentLoading && (
                <div className="empty">
                  <div className="spinner large"/>
                  <h3>Analyzing Incident with Hindsight & Groq</h3>
                  <p>Recalling similar outages and verifying recommendations against MySQL facts...</p>
                </div>
              )}

              {!agentLoading && !briefing && (
                <div className="empty">
                  <div><BrainCircuit size={34}/></div>
                  <h3>Awaiting Incident Alert</h3>
                  <p>Trigger an alert on the left to see historical memories recalled and the validated briefing.</p>
                </div>
              )}

              {briefing && (
                <div style={{display: 'grid', gap: 20}}>
                  {/* PANEL 1: RETRIEVED FROM HINDSIGHT */}
                  <div style={{background: '#0c172a', border: '1px solid #1e3352', borderRadius: 8, padding: 16}}>
                    <div style={{display: 'flex', alignItems: 'center', gap: 8, color: '#51d5bd', marginBottom: 12}}>
                      <Database size={16}/>
                      <b style={{fontSize: 13, textTransform: 'uppercase', letterSpacing: 1}}>Panel 1: Retrieved from Hindsight (Memory)</b>
                    </div>
                    <div style={{fontSize: 11, color: '#8291a9', marginBottom: 10}}>
                      Query: <i>"{briefing.retrieved?.query}"</i>
                    </div>

                    {briefing.retrieved?.items?.length > 0 ? (
                      briefing.retrieved.items.map(item => (
                        <div key={item.rank} style={{background: '#101d36', border: '1px solid #233b61', borderRadius: 6, padding: 10, marginBottom: 8}}>
                          <div style={{display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#5bd3bd', marginBottom: 4}}>
                            <span>Rank #{item.rank} — Precedent: <b>{item.incidentCode || 'Unknown'}</b></span>
                            {item.score && <span>Relevance Score: {item.score}</span>}
                          </div>
                          <pre style={{margin: 0, fontSize: 11, color: '#cbd6e6'}}>{item.text}</pre>
                        </div>
                      ))
                    ) : (
                      <div style={{color: '#8291a9', fontSize: 12}}>No previous memories returned from recall.</div>
                    )}
                  </div>

                  {/* PANEL 2: GENERATED BY GROQ */}
                  <div style={{background: '#0c172a', border: '1px solid #1e3352', borderRadius: 8, padding: 16}}>
                    <div style={{display: 'flex', alignItems: 'center', gap: 8, color: '#45c9b0', marginBottom: 12}}>
                      <BrainCircuit size={16}/>
                      <b style={{fontSize: 13, textTransform: 'uppercase', letterSpacing: 1}}>Panel 2: Generated by Groq (Reasoning & Guidance)</b>
                    </div>

                    {briefing.briefing ? (
                      <div>
                        {briefing.briefing.noRelevantPrecedent && (
                          <div style={{background: '#3d2b1f', border: '1px solid #754633', color: '#ffb582', padding: 8, borderRadius: 6, fontSize: 12, marginBottom: 12}}>
                            ⚠️ No relevant precedent found in long-term memory for this symptom pattern.
                          </div>
                        )}

                        <div style={{marginBottom: 12}}>
                          <b style={{fontSize: 12, color: '#a7b7ca'}}>Situation Summary:</b>
                          <p style={{fontSize: 13, color: '#e8edf7', margin: '4px 0'}}>{briefing.briefing.summary}</p>
                        </div>

                        {/* WARNINGS WITH AUTHORITATIVE MYSQL EVIDENCE (Constraint C4) */}
                        {briefing.briefing.warnings && briefing.briefing.warnings.length > 0 && (
                          <div style={{background: '#2b1414', border: '1px solid #662626', borderRadius: 6, padding: 12, marginBottom: 14}}>
                            <b style={{color: '#ff8b8b', fontSize: 12, display: 'flex', alignItems: 'center', gap: 6}}>
                              <AlertTriangle size={15}/> HISTORICAL WARNINGS (PROVEN FAILED ACTIONS)
                            </b>
                            {briefing.briefing.warnings.map((w, idx) => (
                              <div key={idx} style={{marginTop: 8, fontSize: 12, color: '#ffdede'}}>
                                <div><b>DO NOT:</b> {w.action}</div>
                                <div style={{fontSize: 11, color: '#e6a8a8'}}>Reason: {w.reason}</div>
                                {w.evidence && (
                                  <div style={{marginTop: 4, display: 'inline-block', background: '#4d1c1c', padding: '3px 8px', borderRadius: 4, fontSize: 10, color: '#ffc1c1'}}>
                                    Verified Evidence: <b>{w.citedAction?.incidentCode}</b> · Action {w.citedAction?.actionSequence} · Outcome <b>{w.evidence.outcome}</b> (MySQL Verified)
                                  </div>
                                )}
                              </div>
                            ))}
                          </div>
                        )}

                        {/* Likely causes */}
                        {briefing.briefing.likelyCauses && briefing.briefing.likelyCauses.length > 0 && (
                          <div style={{marginBottom: 12}}>
                            <b style={{fontSize: 12, color: '#a7b7ca'}}>Likely Causes:</b>
                            <ul style={{margin: '4px 0', paddingLeft: 18, fontSize: 12, color: '#cbd6e6'}}>
                              {briefing.briefing.likelyCauses.map((c, i) => (
                                <li key={i}>
                                  {c.text} {c.citedIncidents?.length > 0 && <span style={{color: '#51d5bd'}}>({c.citedIncidents.join(', ')})</span>}
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}

                        {/* Recommended actions */}
                        {briefing.briefing.recommendedActions && briefing.briefing.recommendedActions.length > 0 && (
                          <div style={{marginBottom: 12}}>
                            <b style={{fontSize: 12, color: '#a7b7ca'}}>Recommended Actions:</b>
                            <ul style={{margin: '4px 0', paddingLeft: 18, fontSize: 12, color: '#cbd6e6'}}>
                              {briefing.briefing.recommendedActions.map((a, i) => (
                                <li key={i}>
                                  {a.text} {a.citedIncidents?.length > 0 && <span style={{color: '#51d5bd'}}>({a.citedIncidents.join(', ')})</span>}
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div style={{color: '#ffb582', fontSize: 12}}>
                        Briefing in DEGRADED status: LLM response unavailable; historical memories displayed above.
                      </div>
                    )}
                  </div>
                </div>
              )}
            </section>
          </div>
        )}

        {/* TAB: INCIDENTS EXPLORER (SEARCH / FILTER / PAGINATION) */}
        {tab === 'incidents' && (
          <section className="panel" style={{maxWidth: 1000, margin: '0 auto'}}>
            <div className="panelhead">
              <div>
                <span className="step">RECORDS REPOSITORY</span>
                <h2>Incidents Explorer</h2>
              </div>
              <ListFilter/>
            </div>
            <p style={{fontSize: 13, color: '#8ca0b9'}}>
              Search and filter recorded production incidents. Click on any incident to load its full telemetry, action outcomes, and briefing into the Live Agent.
            </p>

            <div style={{display: 'flex', gap: 12, margin: '18px 0', flexWrap: 'wrap'}}>
              <div style={{flex: 1, minWidth: 240, position: 'relative'}}>
                <input
                  placeholder="Search by code, title, or service..."
                  value={incidentSearchQuery}
                  onChange={e => setIncidentSearchQuery(e.target.value)}
                />
              </div>
              <div style={{width: 160}}>
                <select
                  value={incidentFilterStatus}
                  onChange={e => setIncidentFilterStatus(e.target.value)}
                >
                  <option value="ALL">All Statuses</option>
                  <option value="OPEN">OPEN only</option>
                  <option value="RESOLVED">RESOLVED only</option>
                </select>
              </div>
              <button
                type="button"
                style={{padding: '10px 16px', background: '#20304a', color: '#e5edf8'}}
                onClick={loadIncidents}
                disabled={incidentsLoading}
              >
                <RefreshCw size={14}/> Refresh
              </button>
            </div>

            {incidentsLoading && (
              <div className="empty">
                <div className="spinner large"/>
                <h3>Loading Incidents...</h3>
              </div>
            )}

            {!incidentsLoading && filteredIncidents.length === 0 && (
              <div className="empty">
                <div><Search size={32}/></div>
                <h3>No Incidents Found</h3>
                <p>Try adjusting your search query or seed historical incidents from the Historical Memory tab.</p>
              </div>
            )}

            {!incidentsLoading && filteredIncidents.length > 0 && (
              <div style={{display: 'grid', gap: 10}}>
                {filteredIncidents.map(inc => (
                  <div
                    key={inc.code}
                    onClick={() => selectIncidentByCode(inc.code)}
                    style={{
                      background: '#0b1426',
                      border: '1px solid #1c2b42',
                      borderRadius: 8,
                      padding: 14,
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      cursor: 'pointer',
                      transition: 'border-color .15s'
                    }}
                    onMouseEnter={e => e.currentTarget.style.borderColor = '#51d5bd'}
                    onMouseLeave={e => e.currentTarget.style.borderColor = '#1c2b42'}
                  >
                    <div>
                      <div style={{display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4}}>
                        <b style={{color: '#51d5bd', font: '600 14px "Space Grotesk"'}}>{inc.code}</b>
                        <span style={{
                          fontSize: 10,
                          padding: '2px 6px',
                          borderRadius: 4,
                          background: inc.status === 'RESOLVED' ? '#13392f' : '#38241f',
                          color: inc.status === 'RESOLVED' ? '#54d6a5' : '#ffca8b'
                        }}>
                          {inc.status}
                        </span>
                        <span style={{fontSize: 11, color: '#8ca0b9'}}>• Service: <b>{inc.serviceName || 'Unknown'}</b></span>
                        <span style={{fontSize: 11, color: '#8ca0b9'}}>• Severity: <b>{inc.severity}</b></span>
                      </div>
                      <div style={{fontSize: 13, color: '#cbd6e6'}}>{inc.title}</div>
                    </div>
                    <div style={{display: 'flex', alignItems: 'center', gap: 6, color: '#51d5bd', fontSize: 12}}>
                      Open in Agent <ArrowRight size={14}/>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        )}

        {/* TAB: HISTORICAL MEMORY SEEDING */}
        {tab === 'seed' && (
          <section className="panel" style={{maxWidth: 800, margin: '0 auto'}}>
            <div className="panelhead">
              <div>
                <span className="step">HISTORICAL MEMORY BANK</span>
                <h2>Seed 15 Curated Outages</h2>
              </div>
              <Database/>
            </div>
            <p style={{fontSize: 13, color: '#8ca0b9'}}>
              Populate the system with 15 realistic historical outages spanning cache saturation, DNS outages, deadlocks, and GC storms.
              The held-out failure family (<code>connection-pool-exhaustion</code>) is verified absent by the Seed Guard so the live demo can learn in real-time.
            </p>

            {seedError && (
              <div className="error" style={{marginTop: 16}}>
                <AlertTriangle/>
                <p>Seeding error: {seedError}</p>
              </div>
            )}

            <button onClick={seedIncidents} disabled={seedLoading} style={{marginTop: 16}}>
              {seedLoading ? <><span className="spinner"/> Seeding Outages...</> : <><Database size={16}/> Seed Memory Bank (15 Incidents)</>}
            </button>

            {seedResult && (
              <div style={{marginTop: 20, background: '#0b1426', border: '1px solid #1e3352', padding: 16, borderRadius: 8}}>
                <b style={{color: '#54d6a5', display: 'flex', alignItems: 'center', gap: 6}}>
                  <CheckCircle2 size={16}/> Seeding Complete
                </b>
                <div style={{fontSize: 12, marginTop: 8}}>
                  <div>Incidents processed: <b>{seedResult.incidentsProcessed}</b></div>
                  <div>New incidents created: <b>{seedResult.incidentsCreated}</b></div>
                  <div>Actions created: <b>{seedResult.actionsCreated}</b></div>
                  <div>Memories synced to Hindsight: <b>{seedResult.memoryRetained}</b></div>
                </div>
              </div>
            )}
          </section>
        )}

        {/* TAB: PROTOTYPE QUICK INVESTIGATION */}
        {tab === 'quick' && (
          <div className="workspace">
            <section className="panel">
              <div className="panelhead">
                <div>
                  <span className="step">01 / PROTOTYPE DEMO</span>
                  <h2>Quick Incident Investigation</h2>
                </div>
                <Server/>
              </div>
              <form onSubmit={runQuickInvestigate}>
                <label>Service name<input value={quickForm.serviceName} onChange={e => setQuickForm({...quickForm, serviceName: e.target.value})} required/></label>
                <label>Incident title<input value={quickForm.title} onChange={e => setQuickForm({...quickForm, title: e.target.value})} required/></label>
                <label>Severity
                  <select value={quickForm.severity} onChange={e => setQuickForm({...quickForm, severity: e.target.value})}>
                    <option>CRITICAL</option>
                    <option>HIGH</option>
                    <option>MEDIUM</option>
                    <option>LOW</option>
                  </select>
                </label>
                <label>Symptoms and logs
                  <textarea rows="6" value={quickForm.symptoms} onChange={e => setQuickForm({...quickForm, symptoms: e.target.value})} required/>
                </label>
                {quickError && (
                  <div className="error">
                    <AlertTriangle/>
                    <p>{quickError}</p>
                  </div>
                )}
                <button disabled={quickLoading}>
                  {quickLoading ? <><span className="spinner"/> Investigating...</> : <><Send size={16}/> Run Quick Investigation</>}
                </button>
              </form>
            </section>

            <section className="panel result">
              <div className="panelhead">
                <div>
                  <span className="step">02 / PROTOTYPE OUTPUT</span>
                  <h2>Investigation Result</h2>
                </div>
                <BrainCircuit/>
              </div>
              {quickLoading && (
                <div className="empty">
                  <div className="spinner large"/>
                  <h3>Analyzing Context</h3>
                </div>
              )}
              {quickResult && (
                <>
                  <div className="memoryline">
                    <CheckCircle2 size={16}/>
                    {quickResult.memorySaved ? 'Saved to Hindsight memory' : 'Memory retain pending/mock'}
                  </div>
                  <article className="markdown">{quickResult.analysis}</article>
                  <details>
                    <summary>Retrieved context</summary>
                    <pre>{quickResult.memoryContext}</pre>
                  </details>
                </>
              )}
            </section>
          </div>
        )}

        {/* TAB: ABOUT PAGE (PHASE 5) */}
        {tab === 'about' && (
          <section className="panel" style={{maxWidth: 900, margin: '0 auto'}}>
            <div className="panelhead">
              <div>
                <span className="step">SYSTEM OVERVIEW</span>
                <h2>About IncidentIQ</h2>
              </div>
              <Info/>
            </div>

            <div style={{display: 'grid', gap: 20}}>
              <div>
                <h3 style={{font: '600 18px "Space Grotesk"', color: '#e5edf8', marginBottom: 8}}>Project Purpose</h3>
                <p style={{fontSize: 13, color: '#cbd6e6', lineHeight: 1.7}}>
                  IncidentIQ is an enterprise incident response agent designed to solve the critical flaw of AI hallucinations during high-pressure outages. Rather than trusting free-form LLM recommendations, IncidentIQ pairs historical memory recall with deterministic, authoritative validation against audited database logs.
                </p>
              </div>

              <div>
                <h3 style={{font: '600 18px "Space Grotesk"', color: '#e5edf8', marginBottom: 8}}>Key Architectural Capabilities</h3>
                <ul style={{fontSize: 13, color: '#cbd6e6', lineHeight: 1.8, paddingLeft: 20, margin: 0}}>
                  <li><b>Deterministic Warning Validation:</b> Every warning generated by AI must cite an exact historical incident action proven in MySQL audit logs to have worsened the outage. If proof is absent, the recommendation is automatically purged.</li>
                  <li><b>Strict Causal Precedent:</b> Mitigating actions with a "WORSENED" outcome require verified physical mechanisms to be recorded before an incident can be closed.</li>
                  <li><b>Dual-Panel Interface:</b> Strict separation between unadulterated retrieved memory facts and AI-generated syntheses.</li>
                  <li><b>Graceful Degradation:</b> Full autonomous operation continues even when external LLM or memory APIs experience rate limits or downtime.</li>
                </ul>
              </div>

              <div>
                <h3 style={{font: '600 18px "Space Grotesk"', color: '#e5edf8', marginBottom: 8}}>Technology Stack</h3>
                <div style={{display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12}}>
                  <div style={{background: '#0b1426', border: '1px solid #1c2b42', padding: 12, borderRadius: 6}}>
                    <b style={{color: '#51d5bd', fontSize: 12}}>Backend & Security</b>
                    <p style={{fontSize: 11, color: '#8ca0b9', margin: '4px 0 0'}}>Java 21 LTS, Spring Boot 3.5, Spring Security JWT, BCrypt, Flyway 11</p>
                  </div>
                  <div style={{background: '#0b1426', border: '1px solid #1c2b42', padding: 12, borderRadius: 6}}>
                    <b style={{color: '#51d5bd', fontSize: 12}}>Database & Storage</b>
                    <p style={{fontSize: 11, color: '#8ca0b9', margin: '4px 0 0'}}>MySQL 8.4 InnoDB / Dual-Mode H2 Fallback, HikariCP Connection Pool</p>
                  </div>
                  <div style={{background: '#0b1426', border: '1px solid #1c2b42', padding: 12, borderRadius: 6}}>
                    <b style={{color: '#51d5bd', fontSize: 12}}>Frontend Architecture</b>
                    <p style={{fontSize: 11, color: '#8ca0b9', margin: '4px 0 0'}}>React 19, Vite 6, Lucide Icons, Pure CSS Design Tokens</p>
                  </div>
                  <div style={{background: '#0b1426', border: '1px solid #1c2b42', padding: 12, borderRadius: 6}}>
                    <b style={{color: '#51d5bd', fontSize: 12}}>AI & Vector Memory</b>
                    <p style={{fontSize: 11, color: '#8ca0b9', margin: '4px 0 0'}}>Groq API (openai/gpt-oss-20b), Hindsight Vectorize Cloud</p>
                  </div>
                </div>
              </div>

              <div style={{borderTop: '1px solid #20304a', paddingTop: 16}}>
                <h3 style={{font: '600 18px "Space Grotesk"', color: '#e5edf8', marginBottom: 8}}>Engineering Team</h3>
                <p style={{fontSize: 13, color: '#cbd6e6', lineHeight: 1.6}}>
                  TODO: [Mohamed Mubassir S & IncidentIQ Platform Engineering Team]
                </p>
                <small style={{color: '#8ca0b9', fontSize: 11}}>
                  Built and verified for production deployment. Contact platform operations for incident protocol onboarding.
                </small>
              </div>
            </div>
          </section>
        )}

        {/* TAB: FEEDBACK PAGE (PHASE 5) */}
        {tab === 'feedback' && (
          <section className="panel" style={{maxWidth: 700, margin: '0 auto'}}>
            <div className="panelhead">
              <div>
                <span className="step">USER INSIGHTS</span>
                <h2>Feedback & Suggestions</h2>
              </div>
              <MessageSquare/>
            </div>
            <p style={{fontSize: 13, color: '#8ca0b9'}}>
              Share your operational feedback or report agent accuracy findings. Every submission is recorded in the audit database.
            </p>

            {feedbackSuccess ? (
              <div style={{background: '#0e2b29', border: '1px solid #266b5b', padding: 24, borderRadius: 8, textAlign: 'center', marginTop: 18}}>
                <CheckCircle2 size={36} color="#54d6a5" style={{margin: '0 auto 12px'}}/>
                <h3 style={{font: '600 18px "Space Grotesk"', color: '#e8edf7', margin: '0 0 6px'}}>Feedback Submitted</h3>
                <p style={{color: '#cbd6e6', fontSize: 13, maxWidth: 400, margin: '0 auto 16px'}}>
                  Thank you! Your feedback has been safely stored and will help improve agent reasoning and safety rules.
                </p>
                <button
                  type="button"
                  style={{margin: 'auto'}}
                  onClick={() => setFeedbackSuccess(false)}
                >
                  Submit Another Feedback
                </button>
              </div>
            ) : (
              <form onSubmit={submitFeedback} style={{marginTop: 18}}>
                {feedbackError && (
                  <div className="error">
                    <AlertTriangle/>
                    <p>{feedbackError}</p>
                  </div>
                )}

                <label>Your Name
                  <input
                    value={feedbackForm.name}
                    onChange={e => setFeedbackForm({...feedbackForm, name: e.target.value})}
                    placeholder="e.g. Alex Morgan"
                    maxLength={100}
                    required
                  />
                </label>

                <label>Work Email
                  <input
                    type="email"
                    value={feedbackForm.email}
                    onChange={e => setFeedbackForm({...feedbackForm, email: e.target.value})}
                    placeholder="name@company.com"
                    maxLength={254}
                    required
                  />
                </label>

                <label>Experience Rating (1 = Poor, 5 = Excellent)
                  <div className="rating-buttons">
                    {[1, 2, 3, 4, 5].map(star => (
                      <button
                        key={star}
                        type="button"
                        className={`rating-btn ${feedbackForm.rating === star ? 'active' : ''}`}
                        onClick={() => setFeedbackForm({...feedbackForm, rating: star})}
                      >
                        <Star size={14} fill={feedbackForm.rating >= star ? '#51d5bd' : 'none'}/> {star}
                      </button>
                    ))}
                  </div>
                </label>

                <label>Feedback Message
                  <textarea
                    rows="4"
                    value={feedbackForm.message}
                    onChange={e => setFeedbackForm({...feedbackForm, message: e.target.value})}
                    placeholder="Describe your experience with the agent, accuracy of briefings, or suggested improvements..."
                    maxLength={5000}
                    required
                  />
                </label>

                {/* Honeypot field (hidden from humans, catches bots) */}
                <div style={{display: 'none'}} aria-hidden="true">
                  <label>Website
                    <input
                      tabIndex={-1}
                      autoComplete="off"
                      value={feedbackForm.website}
                      onChange={e => setFeedbackForm({...feedbackForm, website: e.target.value})}
                    />
                  </label>
                </div>

                <button disabled={feedbackLoading}>
                  {feedbackLoading ? <><span className="spinner"/> Submitting...</> : <><Send size={16}/> Submit Feedback</>}
                </button>
              </form>
            )}
          </section>
        )}

        {/* 404 NOT FOUND VIEW */}
        {!['agent', 'incidents', 'seed', 'quick', 'about', 'feedback'].includes(tab) && (
          <section className="panel" style={{maxWidth: 600, margin: '40px auto', textAlign: 'center'}}>
            <div className="empty">
              <div><FileQuestion size={36}/></div>
              <h3>404 — Page Not Found</h3>
              <p>The requested view does not exist in Incident Operations Center.</p>
              <button style={{marginTop: 18}} onClick={() => setTab('agent')}>
                Return to Incident Agent
              </button>
            </div>
          </section>
        )}

        <GlobalFooter currentTab={tab} onSelectTab={setTab}/>
      </main>
    </div>
  );
}

createRoot(document.getElementById('root')).render(
  <ErrorBoundary>
    <App/>
  </ErrorBoundary>
);
