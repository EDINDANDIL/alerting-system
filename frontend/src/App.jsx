import React, { useState, useEffect, useRef } from 'react';

const parseJwt = (token) => {
  try {
    const base64Url = token.split('.')[1];
    let base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const pad = (4 - (base64.length % 4)) % 4;
    base64 += '='.repeat(pad);
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
};

export default function App() {
  const [user, setUser] = useState(() => {
    const token = localStorage.getItem('token');
    const email = localStorage.getItem('email');
    if (token && email) {
      const payload = parseJwt(token);
      if (payload && payload.exp * 1000 > Date.now()) {
        return { email, token, userId: payload.userId };
      }
    }
    return null;
  });

  const [isRegisterMode, setIsRegisterMode] = useState(false);
  const [authEmail, setAuthEmail] = useState('');
  const [authPassword, setAuthPassword] = useState('');
  const [authError, setAuthError] = useState('');
  const [authSuccess, setAuthSuccess] = useState('');

  const [filters, setFilters] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const sseRef = useRef(null);

  // Form State
  const [blackList, setBlackList] = useState('');
  const [timeWindow, setTimeWindow] = useState(300);
  const [direction, setDirection] = useState('UP');
  const [percent, setPercent] = useState(3);
  const [formError, setFormError] = useState('');
  const [formSuccess, setFormSuccess] = useState('');

  const fetchFilters = async (token) => {
    try {
      const res = await fetch('/api/filters', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.status === 200) {
        const data = await res.json();
        setFilters(data);
      } else if (res.status === 401) {
        handleLogout();
      }
    } catch (e) {
      console.error("Failed to fetch filters", e);
    }
  };

  const connectAlertStream = (userId) => {
    if (sseRef.current) sseRef.current.close();
    const sse = new EventSource(`/api/alerts/stream?userId=${userId}`);

    sse.onmessage = (event) => {
      try {
        const alert = JSON.parse(event.data);
        setAlerts((prev) => [alert, ...prev.slice(0, 49)]);
      } catch (e) {}
    };
    sseRef.current = sse;
  };

  useEffect(() => {
    if (user) {
      fetchFilters(user.token);
      connectAlertStream(user.userId);
    } else {
      if (sseRef.current) {
        sseRef.current.close();
        sseRef.current = null;
      }
    }
    return () => {
      if (sseRef.current) sseRef.current.close();
    };
  }, [user]);

  const handleAuthSubmit = async (e) => {
    e.preventDefault();
    setAuthError('');
    setAuthSuccess('');

    const endpoint = isRegisterMode ? '/api/auth/register' : '/api/auth/login';

    try {
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: authEmail, password: authPassword })
      });
      const data = await res.json();

      if (res.status === 200 || res.status === 201) {
        if (isRegisterMode) {
          setAuthSuccess('Account created. Please log in.');
          setIsRegisterMode(false);
          setAuthPassword('');
        } else {
          const token = data.token;
          const payload = parseJwt(token);
          if (payload) {
            localStorage.setItem('token', token);
            localStorage.setItem('email', authEmail);
            setUser({ email: authEmail, token, userId: payload.userId });
          } else setAuthError('Authentication error.');
        }
      } else {
        setAuthError(data.token || 'Authentication failed.');
      }
    } catch (err) {
      setAuthError('Connection failed.');
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('email');
    setUser(null);
    setFilters([]);
    setAlerts([]);
  };

  const handleCreateFilter = async (e) => {
    e.preventDefault();
    setFormError('');
    setFormSuccess('');

    const payload = {
      action: 'IMPULSE',
      exchange: ['binance', 'bybit'], 
      market: ['futures', 'spot'],
      blackList: blackList.split(',').map(s => s.trim()).filter(s => s.length > 0),
      timeWindow: parseInt(timeWindow),
      direction: direction,
      percent: parseInt(percent),
      volume24h: 0 
    };

    try {
      const res = await fetch('/api/filters/IMPULSE', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${user.token}`
        },
        body: JSON.stringify(payload)
      });
      if (res.status === 201) {
        setFormSuccess('Rule activated successfully');
        setBlackList('');
        fetchFilters(user.token);
      } else {
        const errData = await res.json();
        setFormError(errData.message || 'Failed to create rule.');
      }
    } catch (err) {
      setFormError('Connection failed.');
    }
  };

  const handleDeleteFilter = async (id) => {
    try {
      const res = await fetch(`/api/filters/IMPULSE/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${user.token}` }
      });
      if (res.status === 204) fetchFilters(user.token);
    } catch (e) {}
  };

  // SVG Icons to replace emojis
  const ArrowUp = () => <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M7 17L17 7M17 7H7M17 7V17"/></svg>;
  const ArrowDown = () => <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 7L7 17M7 17H17M7 17V7"/></svg>;
  const ArrowBoth = () => <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 9l-5-5-5 5M17 15l-5 5-5-5"/></svg>;
  const CloseIcon = () => <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>;
  const LogoMark = () => <div className="logo-mark"></div>;

  if (!user) {
    return (
      <div className="auth-wrapper fade-in">
        <div className="auth-container">
          <div className="brand-header">
            <LogoMark />
            <h2>Screener</h2>
          </div>
          
          <h3 className="auth-title">{isRegisterMode ? 'Create an account' : 'Sign in'}</h3>
          
          <div className="auth-messages">
            {authError && <div className="toast toast-error">{authError}</div>}
            {authSuccess && <div className="toast toast-success">{authSuccess}</div>}
          </div>

          <form onSubmit={handleAuthSubmit} className="auth-form">
            <div className="input-group">
              <label>Email</label>
              <input type="email" required value={authEmail} onChange={(e) => setAuthEmail(e.target.value)} />
            </div>
            <div className="input-group">
              <label>Password</label>
              <input type="password" required value={authPassword} onChange={(e) => setAuthPassword(e.target.value)} />
            </div>
            <button type="submit" className="btn btn-primary btn-block" style={{ marginTop: '1rem' }}>
              {isRegisterMode ? 'Sign up' : 'Continue'}
            </button>
          </form>

          <div className="auth-toggle">
            {isRegisterMode ? (
              <p>Have an account? <span onClick={() => setIsRegisterMode(false)}>Sign in</span></p>
            ) : (
              <p>No account? <span onClick={() => setIsRegisterMode(true)}>Sign up</span></p>
            )}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-wrapper fade-in">
      <nav className="top-nav">
        <div className="nav-brand">
          <LogoMark />
          <h2>Screener</h2>
        </div>
        <div className="nav-user">
          <span className="user-email">{user.email}</span>
          <button onClick={handleLogout} className="btn btn-ghost">Sign out</button>
        </div>
      </nav>

      <div className="dashboard-grid">
        <div className="grid-col fade-in-up" style={{ animationDelay: '0.1s' }}>
          
          <div className="surface-card">
            <div className="card-header">
              <h3 className="card-title">New Rule</h3>
              <p className="card-subtitle">Configure parameters to monitor market movements.</p>
            </div>

            <div className="form-messages">
              {formError && <div className="toast toast-error">{formError}</div>}
              {formSuccess && <div className="toast toast-success">{formSuccess}</div>}
            </div>

            <form onSubmit={handleCreateFilter} className="rule-form">
              <div className="input-row">
                <div className="input-group">
                  <label>Window (sec)</label>
                  <input type="number" required value={timeWindow} onChange={(e) => setTimeWindow(e.target.value)} />
                </div>
                <div className="input-group">
                  <label>Move (%)</label>
                  <input type="number" required value={percent} onChange={(e) => setPercent(e.target.value)} />
                </div>
              </div>

              <div className="input-group select-wrapper">
                <label>Direction</label>
                <select value={direction} onChange={(e) => setDirection(e.target.value)}>
                  <option value="UP">Up (Pumps)</option>
                  <option value="DOWN">Down (Dumps)</option>
                  <option value="BOTH">Both (Volatility)</option>
                </select>
              </div>

              <div className="input-group">
                <label>Blacklisted Symbols</label>
                <input type="text" placeholder="e.g. BTCUSDT, ETHUSDT" value={blackList} onChange={(e) => setBlackList(e.target.value)} />
              </div>

              <button type="submit" className="btn btn-primary btn-block mt-2">Activate</button>
            </form>
          </div>
          
          <div className="section-header mt-4">
            <h3 className="section-title">Active Rules</h3>
            <span className="badge-count">{filters.length}</span>
          </div>
          
          <div className="rules-list">
            {filters.length === 0 ? (
              <div className="empty-state">
                <p>No active rules configured.</p>
              </div>
            ) : (
              filters.map((filter) => {
                const data = filter.payload || filter;
                return (
                  <div key={filter.id} className="rule-item">
                    <div className="rule-info">
                      <div className="rule-main">
                        <span className="dir-icon">
                          {data.direction === 'UP' ? <ArrowUp /> : data.direction === 'DOWN' ? <ArrowDown /> : <ArrowBoth />}
                        </span>
                        <span className="rule-val">{data.percent}%</span>
                        <span className="rule-text">in {data.timeWindow}s</span>
                      </div>
                      {data.blackList && data.blackList.length > 0 && (
                        <div className="rule-sub">Excluding: {data.blackList.join(', ')}</div>
                      )}
                    </div>
                    <button onClick={() => handleDeleteFilter(filter.id)} className="btn-icon">
                      <CloseIcon />
                    </button>
                  </div>
                );
              })
            )}
          </div>
        </div>

        <div className="grid-col fade-in-up" style={{ animationDelay: '0.2s' }}>
          <div className="surface-card feed-card">
            <div className="card-header flex-between">
              <h3 className="card-title">Live Feed</h3>
              {alerts.length > 0 && (
                <button onClick={() => setAlerts([])} className="btn btn-ghost btn-sm">Clear</button>
              )}
            </div>

            <div className="feed-container">
              {alerts.length === 0 ? (
                <div className="empty-state">
                  <p>Awaiting market signals.</p>
                </div>
              ) : (
                <div className="alert-list">
                  {alerts.map((alert, idx) => (
                    <div key={idx} className="alert-item">
                      <div className="alert-left">
                        <div className="alert-symbol">{alert.symbol}</div>
                        <div className="alert-tags">
                          <span className="tag">{alert.exchange}</span>
                          <span className="tag tag-dim">{alert.market}</span>
                        </div>
                      </div>
                      <div className="alert-right">
                        <span className="tag tag-black">IMPULSE</span>
                        <div className="alert-time">
                          {new Date(alert.timestampNs / 1_000_000).toLocaleTimeString([], { hour12: false })}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}