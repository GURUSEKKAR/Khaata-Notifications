// Thin API client. Attaches the JWT (if logged in) and surfaces 401s so the app
// can redirect to login.
//
// In dev, VITE_API_URL is unset and calls go to "/api" through the Vite proxy.
// In production (Vercel), set VITE_API_URL to the Render backend URL, e.g.
// https://khaata-backend.onrender.com — then calls go to "<that>/api".
const API_ROOT = import.meta.env.VITE_API_URL
  ? `${import.meta.env.VITE_API_URL.replace(/\/$/, '')}/api`
  : '/api';
const BASE = API_ROOT;
const TOKEN_KEY = 'khaata_token';

export const auth = {
  getToken: () => localStorage.getItem(TOKEN_KEY),
  setToken: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
  isLoggedIn: () => !!localStorage.getItem(TOKEN_KEY),
};

function authHeaders(extra = {}) {
  const token = auth.getToken();
  return token ? { ...extra, Authorization: `Bearer ${token}` } : extra;
}

async function handle(res) {
  if (res.status === 401 || res.status === 403) {
    auth.clear();
    // Let the app re-render to the login screen.
    window.dispatchEvent(new Event('khaata-unauthorized'));
    throw new Error('Session expired. Please log in again.');
  }
  if (!res.ok) {
    let msg = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body?.message) msg = body.message;
    } catch (_) { /* ignore */ }
    throw new Error(msg);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  login: (payload) =>
    fetch(`${BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }).then(handle),

  register: (payload) =>
    fetch(`${BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }).then(handle),

  getSummary: () => fetch(`${BASE}/dashboard/summary`, { headers: authHeaders() }).then(handle),
  getCustomers: () => fetch(`${BASE}/customers`, { headers: authHeaders() }).then(handle),

  createCustomer: (payload) =>
    fetch(`${BASE}/customers`, {
      method: 'POST',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify(payload),
    }).then(handle),

  updateCustomer: (id, payload) =>
    fetch(`${BASE}/customers/${id}`, {
      method: 'PUT',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify(payload),
    }).then(handle),

  deleteCustomer: (id, password) =>
    fetch(`${BASE}/customers/${id}`, {
      method: 'DELETE',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ password }),
    }).then(handle),

  addTransaction: (customerId, payload) =>
    fetch(`${BASE}/customers/${customerId}/transactions`, {
      method: 'POST',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify(payload),
    }).then(handle),

  getHistory: (customerId) =>
    fetch(`${BASE}/customers/${customerId}/history`, { headers: authHeaders() }).then(handle),

  runReminders: () =>
    fetch(`${BASE}/reminders/run`, { method: 'POST', headers: authHeaders() }).then(handle),

  remindCustomer: (id) =>
    fetch(`${BASE}/reminders/customer/${id}`, { method: 'POST', headers: authHeaders() }).then(handle),

  getReminderLog: () =>
    fetch(`${BASE}/reminders/log`, { headers: authHeaders() }).then(handle),
};

export const rupee = (n) =>
  `₹${Number(n ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export const formatDateTime = (iso) => {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
};
