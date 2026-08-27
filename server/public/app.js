const API = '/api';
const state = {
  token: localStorage.getItem('ukss_token') || '',
  user: JSON.parse(localStorage.getItem('ukss_user') || 'null'),
  expenses: [],
  attendance: [],
  allocations: [],
  users: [],
  categories: [],
  expenseFilter: 'All',
  staffFilter: '',
  monthFilter: ''
};

const loginView = document.getElementById('login-view');
const dashboardView = document.getElementById('dashboard-view');
const loginError = document.getElementById('login-error');
const loginSubmit = document.getElementById('login-submit');
const editUserModal = document.getElementById('edit-user-modal');

const CATEGORY_ICONS = {
  'food & meals': '🍽',
  travel: '✈',
  rent: '🏠',
  electricity: '⚡',
  equipment: '🔧',
  marketing: '📣',
  repairs: '🛠',
  miscellaneous: '📦'
};

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

function budgetForPerson(name) {
  const inflows = state.allocations
    .filter((a) => a.staffName === name)
    .reduce((s, a) => s + Number(a.amount || 0), 0);
  const debits = state.expenses
    .filter((e) => e.staffName === name && e.status === 'APPROVED')
    .reduce((s, e) => s + Number(e.amount || 0), 0);
  return { inflows, debits, available: inflows - debits };
}

function allEmployeeNames() {
  const fromUsers = state.users.filter((u) => u.role === 'Staff').map((u) => u.fullName);
  const fromBudget = state.allocations.map((a) => a.staffName);
  const fromExpenses = state.expenses.map((e) => e.staffName);
  return [...new Set([...fromUsers, ...fromBudget, ...fromExpenses])].filter(Boolean).sort();
}

function populateBudgetStaffSelect() {
  const select = document.getElementById('budget-staff');
  if (!select || state.user.role !== 'Admin') return;
  const names = allEmployeeNames();
  select.innerHTML = '<option value="" disabled selected>Select employee</option>' +
    names.map((n) => `<option value="${escapeHtml(n)}">${escapeHtml(n)}</option>`).join('');
}

function categoryIcon(name) {
  const key = String(name || '').toLowerCase();
  for (const [k, icon] of Object.entries(CATEGORY_ICONS)) {
    if (key.includes(k)) return icon;
  }
  return '💼';
}

function expenseInMonth(expense, monthStr) {
  if (!monthStr) return true;
  const d = new Date(Number(expense.timestamp));
  const ym = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  return ym === monthStr;
}

function filteredExpenses() {
  return state.expenses.filter((e) => {
    if (state.expenseFilter !== 'All' && e.status !== state.expenseFilter) return false;
    if (state.staffFilter && e.staffName !== state.staffFilter) return false;
    if (!expenseInMonth(e, state.monthFilter)) return false;
    return true;
  });
}

function showDashboard() {
  loginView.hidden = true;
  dashboardView.hidden = false;
  document.getElementById('user-avatar').textContent = initials(state.user.fullName);
  document.getElementById('settings-user-name').textContent = state.user.fullName;
  const isAdmin = state.user.role === 'Admin';
  document.querySelectorAll('.admin-only').forEach((el) => {
    el.hidden = !isAdmin;
  });
  const budgetNav = document.querySelector('.nav-item--budget');
  if (budgetNav) budgetNav.hidden = false;
  document.querySelector('.bottom-nav').classList.toggle('has-budget', isAdmin);
  const monthInputs = [document.getElementById('duty-month'), document.getElementById('filter-month')];
  const currentMonth = new Date().toISOString().slice(0, 7);
  monthInputs.forEach((input) => {
    if (input && !input.value) input.value = currentMonth;
  });
  if (!state.monthFilter) state.monthFilter = currentMonth;
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

function renderDashboard() {
  const isAdmin = state.user.role === 'Admin';
  const scopeName = isAdmin ? null : state.user.fullName;

  let inflows;
  let debits;
  if (scopeName) {
    ({ inflows, debits } = budgetForPerson(scopeName));
    document.getElementById('dash-budget-label').textContent = 'Your Available Budget';
  } else {
    inflows = state.allocations.reduce((s, a) => s + Number(a.amount || 0), 0);
    debits = state.expenses
      .filter((e) => e.status === 'APPROVED')
      .reduce((s, e) => s + Number(e.amount || 0), 0);
    document.getElementById('dash-budget-label').textContent = 'Company Available Budget';
  }
  const available = inflows - debits;

  document.getElementById('dash-available').textContent = money(available);
  document.getElementById('dash-inflows').textContent = money(inflows);
  document.getElementById('dash-debits').textContent = money(debits);

  const groups = { PENDING: [], APPROVED: [], REJECTED: [] };
  state.expenses.forEach((e) => {
    if (groups[e.status]) groups[e.status].push(e);
  });

  ['PENDING', 'APPROVED', 'REJECTED'].forEach((status) => {
    const key = status.toLowerCase();
    const items = groups[status];
    const total = items.reduce((s, e) => s + Number(e.amount || 0), 0);
    document.getElementById(`dash-${key}-count`).textContent = String(items.length);
    document.getElementById(`dash-${key}-amt`).textContent = money(total);
  });

  document.getElementById('dash-expense-total').textContent = `Total ${state.expenses.length}`;

  const recent = state.expenses.slice(0, 8);
  const root = document.getElementById('dash-recent-list');
  if (!recent.length) {
    root.innerHTML = '<div class="empty">No expenses yet.</div>';
    return;
  }
  root.innerHTML = recent.map((e) => `
    <article class="recent-item">
      <div class="recent-item__icon">${categoryIcon(e.category)}</div>
      <div class="recent-item__body">
        <strong>${escapeHtml(e.description || e.category)}</strong>
        <span>${escapeHtml(e.staffName)} · ${escapeHtml(e.category)}</span>
      </div>
      <div class="recent-item__side">
        <strong>${money(e.amount)}</strong>
        <span class="status ${escapeHtml(e.status)}">${escapeHtml(e.status)}</span>
      </div>
    </article>
  `).join('');

  if (state.user.role === 'Admin') {
    const pendingCount = groups.PENDING.length;
    const badge = document.getElementById('notif-badge');
    badge.textContent = String(pendingCount);
    badge.hidden = pendingCount === 0;
  }
}

function renderStaffFilter() {
  const select = document.getElementById('filter-staff');
  if (!select || state.user.role !== 'Admin') return;
  const fromUsers = state.users.map((u) => u.fullName);
  const fromExpenses = state.expenses.map((e) => e.staffName);
  const names = [...new Set([...fromUsers, ...fromExpenses])].filter(Boolean).sort();
  const current = state.staffFilter;
  select.innerHTML = '<option value="">All employees</option>' +
    names.map((n) => `<option value="${escapeHtml(n)}"${n === current ? ' selected' : ''}>${escapeHtml(n)}</option>`).join('');
}

function renderCategories() {
  const select = document.getElementById('exp-category');
  const list = document.getElementById('categories-list');
  if (select) {
    if (!state.categories.length) {
      select.innerHTML = '<option value="" disabled selected>No categories — add in Settings</option>';
    } else {
      select.innerHTML = state.categories
        .map((c) => `<option value="${escapeHtml(c.name)}">${escapeHtml(c.name)}</option>`)
        .join('');
    }
  }
  if (list && state.user.role === 'Admin') {
    if (!state.categories.length) {
      list.innerHTML = '<p class="feed-meta">No categories yet.</p>';
      return;
    }
    list.innerHTML = state.categories.map((c) => `
      <span class="tag">
        ${escapeHtml(c.name)}
        <button type="button" data-delete-category="${c.id}" aria-label="Delete ${escapeHtml(c.name)}">×</button>
      </span>
    `).join('');
  }
}

async function loadAll() {
  const reqs = [
    api('/expenses'),
    api('/attendance'),
    api('/allocations'),
    api('/categories')
  ];
  if (state.user.role === 'Admin') {
    reqs.push(api('/users'));
    loadWahaStatus();
  }

  const [expenses, attendance, allocations, categories, users] = await Promise.all(reqs);
  state.expenses = expenses;
  state.attendance = attendance;
  state.allocations = allocations;
  state.categories = categories;
  state.users = users || [];

  renderDashboard();
  renderStaffFilter();
  renderCategories();
  populateBudgetStaffSelect();
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
        <button type="button" class="btn btn-secondary btn-sm" data-edit-user="${u.id}">Edit</button>
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

function adminExpenseActions(e) {
  if (state.user.role !== 'Admin') return '';
  if (e.status === 'PENDING') {
    return `
      <div class="feed-actions feed-actions--wrap">
        <button type="button" class="btn btn-approve btn-sm" data-approve="${e.id}">Approve</button>
        <button type="button" class="btn btn-reject btn-sm" data-reject="${e.id}">Reject</button>
        <button type="button" class="btn btn-ghost btn-sm" data-delete-expense="${e.id}">Delete</button>
      </div>`;
  }
  return `
    <div class="feed-actions feed-actions--wrap">
      <button type="button" class="btn btn-secondary btn-sm" data-revert="${e.id}">Revert to Pending</button>
      <button type="button" class="btn btn-ghost btn-sm" data-delete-expense="${e.id}">Delete</button>
    </div>`;
}

function renderExpenses() {
  const root = document.getElementById('expenses-list');
  const items = filteredExpenses();
  if (!items.length) {
    root.innerHTML = '<div class="empty">No expenses match these filters.</div>';
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
      ${adminExpenseActions(e)}
    </article>
  `).join('');
}

function renderBudget() {
  const listRoot = document.getElementById('budget-list');
  const summaryRoot = document.getElementById('budget-summary');
  if (!listRoot) return;

  if (state.user.role === 'Admin' && summaryRoot) {
    const names = allEmployeeNames();
    if (!names.length) {
      summaryRoot.innerHTML = '<p class="feed-meta">No employees yet. Create users in Settings.</p>';
    } else {
      summaryRoot.innerHTML = names.map((name) => {
        const { inflows, debits, available } = budgetForPerson(name);
        return `
          <article class="budget-emp-card">
            <strong>${escapeHtml(name)}</strong>
            <div class="budget-emp-card__row">
              <span>Allocated</span><em>${money(inflows)}</em>
            </div>
            <div class="budget-emp-card__row">
              <span>Spent</span><em>${money(debits)}</em>
            </div>
            <div class="budget-emp-card__row budget-emp-card__row--avail">
              <span>Remaining</span><em>${money(available)}</em>
            </div>
          </article>`;
      }).join('');
    }
  }

  if (!state.allocations.length) {
    listRoot.innerHTML = '<div class="empty">No budget allocations yet.</div>';
    return;
  }
  listRoot.innerHTML = state.allocations.map((b) => `
    <article class="feed-item">
      <div class="feed-top">
        <div>
          <h3>${escapeHtml(b.staffName || '—')}</h3>
          <p class="feed-meta">${escapeHtml(b.description)}</p>
        </div>
        <div class="feed-amount">${money(b.amount)}</div>
      </div>
      <p class="feed-meta">${when(b.timestamp)}</p>
      ${state.user.role === 'Admin' ? `
        <div class="feed-actions">
          <button type="button" class="btn btn-ghost btn-sm" data-delete-allocation="${b.id}">Delete</button>
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

function setTab(tab) {
  document.querySelectorAll('.nav-item').forEach((btn) => {
    btn.classList.toggle('is-active', btn.dataset.tab === tab);
  });
  document.querySelectorAll('.tab-panel').forEach((panel) => {
    const active = panel.id === `tab-${tab}`;
    panel.hidden = !active;
    panel.classList.toggle('is-active', active);
  });
  if (tab === 'settings' && state.user.role === 'Admin') {
    loadWahaStatus();
  }
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function loadWahaStatus() {
  const dot = document.getElementById('waha-dot');
  const label = document.getElementById('waha-status-label');
  const msg = document.getElementById('waha-status-msg');
  if (!dot || state.user.role !== 'Admin') return;
  dot.className = 'status-card__dot status-card__dot--checking';
  label.textContent = 'Checking…';
  msg.textContent = 'Connecting to WAHA…';
  try {
    const status = await api('/settings/waha');
    dot.className = `status-card__dot ${status.connected ? 'status-card__dot--ok' : 'status-card__dot--bad'}`;
    label.textContent = status.connected ? 'Connected' : 'Not connected';
    msg.textContent = status.message || status.status;
  } catch (err) {
    dot.className = 'status-card__dot status-card__dot--bad';
    label.textContent = 'Error';
    msg.textContent = err.message;
  }
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
      body: JSON.stringify({ type, staffName: state.user.fullName, ...loc })
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

function openEditUser(userId) {
  const user = state.users.find((u) => u.id === Number(userId));
  if (!user) return;
  document.getElementById('edit-user-id').value = user.id;
  document.getElementById('edit-user-name').value = user.fullName;
  document.getElementById('edit-user-phone').value = user.phone || '';
  document.getElementById('edit-user-msg').hidden = true;
  editUserModal.showModal();
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

document.getElementById('notif-btn').addEventListener('click', () => {
  state.expenseFilter = 'PENDING';
  document.querySelectorAll('#expense-filters .chip').forEach((c) => {
    c.classList.toggle('is-active', c.dataset.filter === 'PENDING');
  });
  setTab('reports');
  renderExpenses();
});

document.querySelectorAll('.approval-card[data-goto-filter]').forEach((card) => {
  card.addEventListener('click', () => {
    state.expenseFilter = card.dataset.gotoFilter;
    document.querySelectorAll('#expense-filters .chip').forEach((c) => {
      c.classList.toggle('is-active', c.dataset.filter === state.expenseFilter);
    });
    setTab('reports');
    renderExpenses();
  });
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

document.getElementById('filter-staff').addEventListener('change', (ev) => {
  state.staffFilter = ev.target.value;
  renderExpenses();
});

document.getElementById('filter-month').addEventListener('change', (ev) => {
  state.monthFilter = ev.target.value;
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
    setTab('overview');
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
        staffName: document.getElementById('budget-staff').value,
        amount: Number(document.getElementById('budget-amount').value),
        description: document.getElementById('budget-description').value.trim()
      })
    });
    msg.textContent = 'Budget assigned to employee.';
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

document.getElementById('budget-list').addEventListener('click', async (ev) => {
  const id = ev.target.getAttribute('data-delete-allocation');
  if (!id) return;
  if (!confirm('Delete this budget allocation?')) return;
  try {
    await api(`/allocations/${id}`, { method: 'DELETE' });
    await loadAll();
  } catch (err) {
    alert(err.message);
  }
});

document.getElementById('category-form').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const msg = document.getElementById('category-msg');
  msg.hidden = true;
  const name = document.getElementById('category-name').value.trim();
  if (!name) return;
  try {
    await api('/categories', { method: 'POST', body: JSON.stringify({ name }) });
    document.getElementById('category-name').value = '';
    msg.textContent = 'Category added.';
    msg.className = 'form-alert form-alert--ok';
    msg.hidden = false;
    state.categories = await api('/categories');
    renderCategories();
  } catch (err) {
    msg.textContent = err.message;
    msg.className = 'form-alert form-alert--error';
    msg.hidden = false;
  }
});

document.getElementById('categories-list').addEventListener('click', async (ev) => {
  const id = ev.target.getAttribute('data-delete-category');
  if (!id) return;
  if (!confirm('Delete this category? Existing expenses keep the old label.')) return;
  try {
    await api(`/categories/${id}`, { method: 'DELETE' });
    state.categories = await api('/categories');
    renderCategories();
  } catch (err) {
    alert(err.message);
  }
});

document.getElementById('waha-refresh-btn').addEventListener('click', loadWahaStatus);

document.getElementById('expenses-list').addEventListener('click', async (ev) => {
  const approveId = ev.target.getAttribute('data-approve');
  const rejectId = ev.target.getAttribute('data-reject');
  const revertId = ev.target.getAttribute('data-revert');
  const deleteId = ev.target.getAttribute('data-delete-expense');
  const id = approveId || rejectId || revertId;
  if (deleteId) {
    if (!confirm('Delete this expense permanently?')) return;
    try {
      await api(`/expenses/${deleteId}`, { method: 'DELETE' });
      await loadAll();
    } catch (err) {
      alert(err.message);
    }
    return;
  }
  if (!id) return;
  ev.target.disabled = true;
  try {
    let status = 'PENDING';
    if (approveId) status = 'APPROVED';
    else if (rejectId) status = 'REJECTED';
    await api(`/expenses/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ status })
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
    await api('/users', {
      method: 'POST',
      body: JSON.stringify({
        fullName: document.getElementById('user-fullname').value.trim(),
        username: document.getElementById('user-username').value.trim(),
        password: document.getElementById('user-password').value,
        phone: document.getElementById('user-phone').value.trim(),
        role: document.getElementById('user-role-select').value
      })
    });
    msg.textContent = 'User created. WhatsApp welcome queued via WAHA.';
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

document.getElementById('edit-user-cancel').addEventListener('click', () => editUserModal.close());

document.getElementById('edit-user-form').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const msg = document.getElementById('edit-user-msg');
  msg.hidden = true;
  const id = document.getElementById('edit-user-id').value;
  try {
    await api(`/users/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({
        fullName: document.getElementById('edit-user-name').value.trim(),
        phone: document.getElementById('edit-user-phone').value.trim()
      })
    });
    editUserModal.close();
    await loadAll();
  } catch (err) {
    msg.textContent = err.message;
    msg.className = 'form-alert form-alert--error';
    msg.hidden = false;
  }
});

document.getElementById('users-list').addEventListener('click', async (ev) => {
  const editId = ev.target.getAttribute('data-edit-user');
  const deleteId = ev.target.getAttribute('data-delete-user');
  const resetId = ev.target.getAttribute('data-reset-user');
  const blockId = ev.target.getAttribute('data-block-user');

  if (editId) {
    openEditUser(editId);
    return;
  }

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
