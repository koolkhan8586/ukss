const API = '/api';
const state = {
  token: localStorage.getItem('ukss_token') || '',
  user: JSON.parse(localStorage.getItem('ukss_user') || 'null')
};

const loginView = document.getElementById('login-view');
const dashboardView = document.getElementById('dashboard-view');
const loginError = document.getElementById('login-error');
const userLabel = document.getElementById('user-label');

async function api(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const res = await fetch(`${API}${path}`, { ...options, headers });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || `Request failed (${res.status})`);
  return data;
}

function showDashboard() {
  loginView.hidden = true;
  dashboardView.hidden = false;
  userLabel.textContent = `${state.user.fullName} · ${state.user.role}`;
  loadAll();
}

function showLogin() {
  state.token = '';
  state.user = null;
  localStorage.removeItem('ukss_token');
  localStorage.removeItem('ukss_user');
  loginView.hidden = false;
  dashboardView.hidden = true;
}

function money(n) {
  return new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'GBP' }).format(n || 0);
}

function when(ts) {
  return new Date(Number(ts)).toLocaleString();
}

async function loadAll() {
  const [expenses, attendance, allocations] = await Promise.all([
    api('/expenses'),
    api('/attendance'),
    api('/allocations')
  ]);
  renderExpenses(expenses);
  renderAttendance(attendance);
  renderBudget(allocations);
}

function renderExpenses(items) {
  const root = document.getElementById('expenses-list');
  if (!items.length) {
    root.innerHTML = '<p class="meta">No expenses yet.</p>';
    return;
  }
  root.innerHTML = items.map((e) => `
    <article class="item">
      <div class="row">
        <h3>${escapeHtml(e.category)} · ${money(e.amount)}</h3>
        <span class="badge ${e.status}">${e.status}</span>
      </div>
      <p>${escapeHtml(e.description)}</p>
      <p class="meta">${escapeHtml(e.staffName)} · ${when(e.timestamp)}</p>
      ${state.user.role === 'Admin' && e.status === 'PENDING' ? `
        <div class="actions">
          <button class="approve" data-approve="${e.id}">Approve</button>
          <button class="reject" data-reject="${e.id}">Reject</button>
        </div>` : ''}
    </article>
  `).join('');
}

function renderAttendance(items) {
  const root = document.getElementById('attendance-list');
  if (!items.length) {
    root.innerHTML = '<p class="meta">No attendance records.</p>';
    return;
  }
  root.innerHTML = items.map((a) => `
    <article class="item">
      <div class="row">
        <h3>${escapeHtml(a.staffName)} · Duty ${a.type}</h3>
        <span class="meta">${when(a.timestamp)}</span>
      </div>
      <p class="meta">${escapeHtml(a.locationAddress || 'No address')}</p>
    </article>
  `).join('');
}

function renderBudget(items) {
  const root = document.getElementById('budget-list');
  if (!items.length) {
    root.innerHTML = '<p class="meta">No budget allocations.</p>';
    return;
  }
  root.innerHTML = items.map((b) => `
    <article class="item">
      <div class="row">
        <h3>${money(b.amount)}</h3>
        <span class="meta">${when(b.timestamp)}</span>
      </div>
      <p>${escapeHtml(b.description)}</p>
    </article>
  `).join('');
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
}

document.getElementById('login-form').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  loginError.hidden = true;
  try {
    const payload = {
      username: document.getElementById('login-username').value.trim(),
      password: document.getElementById('login-password').value
    };
    const data = await api('/auth/login', { method: 'POST', body: JSON.stringify(payload) });
    state.token = data.token;
    state.user = data.user;
    localStorage.setItem('ukss_token', state.token);
    localStorage.setItem('ukss_user', JSON.stringify(state.user));
    showDashboard();
  } catch (err) {
    loginError.textContent = err.message;
    loginError.hidden = false;
  }
});

document.getElementById('logout-btn').addEventListener('click', showLogin);

document.querySelectorAll('.tabs button').forEach((btn) => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tabs button').forEach((b) => b.classList.remove('active'));
    btn.classList.add('active');
    document.querySelectorAll('.tab-panel').forEach((panel) => {
      panel.hidden = panel.id !== `tab-${btn.dataset.tab}`;
    });
  });
});

document.getElementById('expense-form').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const msg = document.getElementById('submit-msg');
  msg.hidden = true;
  try {
    await api('/expenses', {
      method: 'POST',
      body: JSON.stringify({
        amount: Number(document.getElementById('exp-amount').value),
        category: document.getElementById('exp-category').value,
        description: document.getElementById('exp-description').value.trim(),
        staffName: state.user.fullName
      })
    });
    msg.textContent = 'Expense submitted to server database.';
    msg.hidden = false;
    ev.target.reset();
    await loadAll();
  } catch (err) {
    msg.textContent = err.message;
    msg.className = 'error';
    msg.hidden = false;
  }
});

document.getElementById('expenses-list').addEventListener('click', async (ev) => {
  const approveId = ev.target.getAttribute('data-approve');
  const rejectId = ev.target.getAttribute('data-reject');
  const id = approveId || rejectId;
  if (!id) return;
  await api(`/expenses/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ status: approveId ? 'APPROVED' : 'REJECTED' })
  });
  await loadAll();
});

if (state.token && state.user) {
  api('/auth/me').then(showDashboard).catch(showLogin);
}
