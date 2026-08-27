const API = '/api';
const state = {
  token: localStorage.getItem('ukss_token') || '',
  user: JSON.parse(localStorage.getItem('ukss_user') || 'null'),
  expenses: [],
  attendance: [],
  allocations: [],
  users: [],
  expenseFilter: 'All'
};

const loginView = document.getElementById('login-view');
const dashboardView = document.getElementById('dashboard-view');
const loginError = document.getElementById('login-error');
const loginSubmit = document.getElementById('login-submit');

async function api(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const res = await fetch(`${API}${path}`, { ...options, headers });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || `Request failed (${res.status})`);
  return data;
}

function money(n) {
  const value = Number(n) || 0;
  return `Rs. ${value.toLocaleString('en-PK', {
    minimumFractionDigits: value % 1 === 0 ? 0 : 2,
    maximumFractionDigits: 2
  })}`;
}

function when(ts) {
  return new Date(Number(ts)).toLocaleString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
}

function initials(name) {
  return String(name || '?')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() || '')
    .join('') || '?';
}

function showDashboard() {
  loginView.hidden = true;
  dashboardView.hidden = false;
  document.getElementById('user-name').textContent = state.user.fullName;
  document.getElementById('user-role').textContent = state.user.role;
  document.getElementById('user-avatar').textContent = initials(state.user.fullName);
  const isAdmin = state.user.role === 'Admin';
  document.getElementById('budget-form').hidden = !isAdmin;
  const usersNav = document.querySelector('.nav-item--admin');
  usersNav.hidden = !isAdmin;
  document.querySelector('.bottom-nav').classList.toggle('has-admin', isAdmin);
  document.querySelectorAll('.admin-only').forEach((el) => {
    el.hidden = !isAdmin;
  });
  const monthInput = document.getElementById('duty-month');
  if (monthInput && !monthInput.value) {
    monthInput.value = new Date().toISOString().slice(0, 7);
  }
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

function updateSummary() {
  const pending = state.expenses
    .filter((e) => e.status === 'PENDING')
    .reduce((s, e) => s + Number(e.amount || 0), 0);
  const approved = state.expenses
    .filter((e) => e.status === 'APPROVED')
    .reduce((s, e) => s + Number(e.amount || 0), 0);
  const budget = state.allocations.reduce((s, a) => s + Number(a.amount || 0), 0);
  document.getElementById('sum-pending').textContent = money(pending);
  document.getElementById('sum-approved').textContent = money(approved);
  document.getElementById('sum-budget').textContent = money(budget);
}

async function loadAll() {
  const reqs = [
    api('/expenses'),
    api('/attendance'),
    api('/allocations')
  ];
  if (state.user.role === 'Admin') reqs.push(api('/users'));

  const [expenses, attendance, allocations, users] = await Promise.all(reqs);
  state.expenses = expenses;
  state.attendance = attendance;
  state.allocations = allocations;
  state.users = users || [];
  updateSummary();
  renderExpenses();
  renderAttendance();
  renderBudget();
  if (state.user.role === 'Admin') renderUsers();
}

function renderUsers() {
  const root = document.getElementById('users-list');
  if (!root) return;
  if (!state.users.length) {
    root.innerHTML = '<div class="empty">No users yet.</div>';
    return;
  }
  root.innerHTML = state.users.map((u) => `
    <article class="feed-item">
      <div class="feed-top">
        <div>
          <h3>${escapeHtml(u.fullName)}</h3>
          <span class="status ${u.isBlocked ? 'BLOCKED' : (u.role === 'Admin' ? 'APPROVED' : 'PENDING')}">${u.isBlocked ? 'BLOCKED' : escapeHtml(u.role)}</span>
        </div>
      </div>
      <p class="feed-meta">@${escapeHtml(u.username)} · WhatsApp: ${escapeHtml(u.phone || '—')}</p>
      <div class="feed-actions feed-actions--wrap">
        <button type="button" class="btn btn-secondary btn-sm" data-reset-user="${u.id}">Reset password</button>
        <button type="button" class="btn btn-ghost btn-sm" data-block-user="${u.id}" data-blocked="${u.isBlocked ? '1' : '0'}" ${u.id === state.user.id ? 'disabled' : ''}>
          ${u.isBlocked ? 'Unblock' : 'Block'}
        </button>
        <button type="button" class="btn btn-ghost btn-sm" data-delete-user="${u.id}" ${u.id === state.user.id ? 'disabled' : ''}>Delete</button>
      </div>
    </article>
  `).join('');
}

async function downloadExport(path, fallbackName) {
  const res = await fetch(`${API}${path}`, {
    headers: { Authorization: `Bearer ${state.token}` }
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || `Export failed (${res.status})`);
  }
  const blob = await res.blob();
  const disposition = res.headers.get('Content-Disposition') || '';
  const match = disposition.match(/filename="([^"]+)"/);
  const filename = match ? match[1] : fallbackName;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

function renderExpenses() {
  const root = document.getElementById('expenses-list');
  const items = state.expenses.filter((e) =>
    state.expenseFilter === 'All' ? true : e.status === state.expenseFilter
  );
  if (!items.length) {
    root.innerHTML = '<div class="empty">No expenses in this view yet.</div>';
    return;
  }
  root.innerHTML = items.map((e) => `
    <article class="feed-item">
      <div class="feed-top">
        <div>
          <h3>${escapeHtml(e.category)}</h3>
          <span class="status ${escapeHtml(e.status)}">${escapeHtml(e.status)}</span>
        </div>
        <div class="feed-amount">${money(e.amount)}</div>
      </div>
      <p class="feed-desc">${escapeHtml(e.description)}</p>
      <p class="feed-meta">${escapeHtml(e.staffName)} · ${when(e.timestamp)}</p>
      ${state.user.role === 'Admin' && e.status === 'PENDING' ? `
        <div class="feed-actions">
          <button type="button" class="btn btn-approve" data-approve="${e.id}">Approve</button>
          <button type="button" class="btn btn-reject" data-reject="${e.id}">Reject</button>
        </div>` : ''}
    </article>
  `).join('');
}

function renderAttendance() {
  const root = document.getElementById('attendance-list');
  if (!state.attendance.length) {
    root.innerHTML = '<div class="empty">No duty records yet. Use Duty In / Duty Out above.</div>';
    return;
  }
  root.innerHTML = state.attendance.map((a) => `
    <article class="feed-item">
      <div class="feed-top">
        <div>
          <h3>${escapeHtml(a.staffName)}</h3>
          <span class="status ${escapeHtml(a.type)}">DUTY ${escapeHtml(a.type)}</span>
        </div>
        <div class="feed-meta">${when(a.timestamp)}</div>
      </div>
      <p class="feed-meta">${escapeHtml(a.locationAddress || 'Location captured')}</p>
    </article>
  `).join('');
}

function renderBudget() {
  const root = document.getElementById('budget-list');
  if (!state.allocations.length) {
    root.innerHTML = '<div class="empty">No budget allocations yet.</div>';
    return;
  }
  root.innerHTML = state.allocations.map((b) => `
    <article class="feed-item">
      <div class="feed-top">
        <h3>${escapeHtml(b.description)}</h3>
        <div class="feed-amount">${money(b.amount)}</div>
      </div>
      <p class="feed-meta">${when(b.timestamp)}</p>
    </article>
  `).join('');
}

function setTab(tab) {
  document.querySelectorAll('.nav-item').forEach((btn) => {
    btn.classList.toggle('is-active', btn.dataset.tab === tab);
  });
  document.querySelectorAll('.tab-panel').forEach((panel) => {
    const active = panel.id === `tab-${tab}`;
    panel.hidden = !active;
    panel.classList.toggle('is-active', active);
  });
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function getLocation() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('Location is not available on this device'));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({
        latitude: pos.coords.latitude,
        longitude: pos.coords.longitude,
        locationAddress: `${pos.coords.latitude.toFixed(5)}, ${pos.coords.longitude.toFixed(5)}`
      }),
      () => reject(new Error('Please allow location access to mark duty')),
      { enableHighAccuracy: true, timeout: 12000 }
    );
  });
}

async function markDuty(type) {
  const msg = document.getElementById('duty-msg');
  msg.hidden = true;
  try {
    const loc = await getLocation();
    await api('/attendance', {
      method: 'POST',
      body: JSON.stringify({
        type,
        staffName: state.user.fullName,
        ...loc
      })
    });
    msg.textContent = `Duty ${type} recorded. WhatsApp notification sent.`;
    msg.className = 'form-alert form-alert--ok';
    msg.hidden = false;
    await loadAll();
  } catch (err) {
    msg.textContent = err.message;
    msg.className = 'form-alert form-alert--error';
    msg.hidden = false;
  }
}

document.getElementById('toggle-password').addEventListener('click', () => {
  const input = document.getElementById('login-password');
  const btn = document.getElementById('toggle-password');
  const show = input.type === 'password';
  input.type = show ? 'text' : 'password';
  btn.textContent = show ? 'Hide' : 'Show';
});

document.getElementById('login-form').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  loginError.hidden = true;
  loginSubmit.disabled = true;
  loginSubmit.querySelector('.btn-label').textContent = 'Signing in…';
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
    loginError.textContent = err.message === 'Invalid credentials'
      ? 'Wrong username or password. Try admin / ChangeMe123! if you just migrated.'
      : err.message === 'Account is blocked. Contact admin.'
        ? 'Your account is blocked. Please contact admin.'
        : err.message;
    loginError.hidden = false;
  } finally {
    loginSubmit.disabled = false;
    loginSubmit.querySelector('.btn-label').textContent = 'Sign in';
  }
});

document.getElementById('logout-btn').addEventListener('click', showLogin);
document.getElementById('account-btn').addEventListener('click', () => setTab('account'));

document.getElementById('password-form').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const msg = document.getElementById('password-msg');
  msg.hidden = true;
  const currentPassword = document.getElementById('pw-current').value;
  const newPassword = document.getElementById('pw-new').value;
  const confirm = document.getElementById('pw-confirm').value;
  if (newPassword !== confirm) {
    msg.textContent = 'New passwords do not match.';
    msg.className = 'form-alert form-alert--error';
    msg.hidden = false;
    return;
  }
  try {
    await api('/auth/change-password', {
      method: 'POST',
      body: JSON.stringify({ currentPassword, newPassword })
    });
    msg.textContent = 'Password updated successfully.';
    msg.className = 'form-alert form-alert--ok';
    msg.hidden = false;
    ev.target.reset();
  } catch (err) {
    msg.textContent = err.message;
    msg.className = 'form-alert form-alert--error';
    msg.hidden = false;
  }
});

document.getElementById('export-expenses-btn').addEventListener('click', async () => {
  try {
    await downloadExport('/export/expenses', 'ukss-expenses.xlsx');
  } catch (err) {
    alert(err.message);
  }
});

document.getElementById('export-duty-btn').addEventListener('click', async () => {
  const month = document.getElementById('duty-month').value;
  if (!month) {
    alert('Select a month first.');
    return;
  }
  try {
    await downloadExport(`/export/attendance?month=${encodeURIComponent(month)}`, `ukss-duty-${month}.xlsx`);
  } catch (err) {
    alert(err.message);
  }
});

document.querySelectorAll('.nav-item').forEach((btn) => {
  btn.addEventListener('click', () => setTab(btn.dataset.tab));
});

document.getElementById('expense-filters').addEventListener('click', (ev) => {
  const chip = ev.target.closest('[data-filter]');
  if (!chip) return;
  state.expenseFilter = chip.dataset.filter;
  document.querySelectorAll('#expense-filters .chip').forEach((c) => {
    c.classList.toggle('is-active', c === chip);
  });
  renderExpenses();
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
    msg.textContent = 'Expense submitted. WhatsApp notification sent.';
    msg.className = 'form-alert form-alert--ok';
    msg.hidden = false;
    ev.target.reset();
    await loadAll();
    setTab('expenses');
  } catch (err) {
    msg.textContent = err.message;
    msg.className = 'form-alert form-alert--error';
    msg.hidden = false;
  }
});

document.getElementById('budget-form').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const msg = document.getElementById('budget-msg');
  msg.hidden = true;
  try {
    await api('/allocations', {
      method: 'POST',
      body: JSON.stringify({
        amount: Number(document.getElementById('budget-amount').value),
        description: document.getElementById('budget-description').value.trim()
      })
    });
    msg.textContent = 'Budget allocation added.';
    msg.className = 'form-alert form-alert--ok';
    msg.hidden = false;
    ev.target.reset();
    await loadAll();
  } catch (err) {
    msg.textContent = err.message;
    msg.className = 'form-alert form-alert--error';
    msg.hidden = false;
  }
});

document.getElementById('expenses-list').addEventListener('click', async (ev) => {
  const approveId = ev.target.getAttribute('data-approve');
  const rejectId = ev.target.getAttribute('data-reject');
  const id = approveId || rejectId;
  if (!id) return;
  ev.target.disabled = true;
  try {
    await api(`/expenses/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ status: approveId ? 'APPROVED' : 'REJECTED' })
    });
    await loadAll();
  } catch (err) {
    alert(err.message);
  }
});

document.getElementById('duty-in').addEventListener('click', () => markDuty('IN'));
document.getElementById('duty-out').addEventListener('click', () => markDuty('OUT'));

document.getElementById('user-form').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const msg = document.getElementById('user-msg');
  msg.hidden = true;
  try {
    const payload = {
      fullName: document.getElementById('user-fullname').value.trim(),
      username: document.getElementById('user-username').value.trim(),
      password: document.getElementById('user-password').value,
      phone: document.getElementById('user-phone').value.trim(),
      role: document.getElementById('user-role-select').value
    };
    await api('/users', { method: 'POST', body: JSON.stringify(payload) });
    msg.textContent = 'User created. WhatsApp welcome (username, password, URL) queued via WAHA.';
    msg.className = 'form-alert form-alert--ok';
    msg.hidden = false;
    ev.target.reset();
    await loadAll();
  } catch (err) {
    msg.textContent = err.message;
    msg.className = 'form-alert form-alert--error';
    msg.hidden = false;
  }
});

document.getElementById('users-list').addEventListener('click', async (ev) => {
  const deleteId = ev.target.getAttribute('data-delete-user');
  const resetId = ev.target.getAttribute('data-reset-user');
  const blockId = ev.target.getAttribute('data-block-user');

  if (resetId) {
    const password = prompt('Enter new password for this user (min 6 chars):');
    if (!password) return;
    if (password.length < 6) {
      alert('Password must be at least 6 characters.');
      return;
    }
    try {
      const result = await api(`/users/${resetId}/reset-password`, {
        method: 'POST',
        body: JSON.stringify({ password, notifyWhatsApp: true })
      });
      alert(result.whatsappQueued
        ? 'Password reset. WhatsApp sent with new credentials.'
        : 'Password reset successfully.');
    } catch (err) {
      alert(err.message);
    }
    return;
  }

  if (blockId) {
    const currentlyBlocked = ev.target.getAttribute('data-blocked') === '1';
    const action = currentlyBlocked ? 'unblock' : 'block';
    if (!confirm(`${action.charAt(0).toUpperCase() + action.slice(1)} this user?`)) return;
    try {
      await api(`/users/${blockId}/block`, {
        method: 'PATCH',
        body: JSON.stringify({ blocked: !currentlyBlocked })
      });
      await loadAll();
    } catch (err) {
      alert(err.message);
    }
    return;
  }

  if (deleteId) {
    if (!confirm('Delete this user?')) return;
    try {
      await api(`/users/${deleteId}`, { method: 'DELETE' });
      await loadAll();
    } catch (err) {
      alert(err.message);
    }
  }
});

if (state.token && state.user) {
  api('/auth/me').then(showDashboard).catch(showLogin);
}
