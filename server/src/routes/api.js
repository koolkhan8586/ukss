const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { query } = require('../db');
const {
  authRequired,
  adminRequired,
  mapUser,
  mapExpense,
  mapAllocation,
  mapAttendance
} = require('../helpers');
const {
  notifyWelcome,
  notifyExpenseSubmitted,
  notifyExpenseDecision,
  notifyDuty
} = require('../waha');

const router = express.Router();

async function findUserPhoneByName(name) {
  if (!name) return null;
  const rows = await query(
    `SELECT phone FROM users
     WHERE full_name = :name OR username = :name
     ORDER BY id DESC LIMIT 1`,
    { name }
  );
  return rows[0]?.phone || null;
}

function adminAlertPhone() {
  return process.env.ADMIN_WHATSAPP || null;
}

router.get('/health', async (_req, res) => {
  try {
    await query('SELECT 1');
    res.json({ ok: true, service: 'ukss-expense', host: 'exp.ukssolution.com' });
  } catch (err) {
    res.status(500).json({ ok: false, error: err.message });
  }
});

router.post('/auth/register', async (req, res) => {
  try {
    const { username, password, fullName, role = 'Staff' } = req.body || {};
    if (!username || !password || !fullName) {
      return res.status(400).json({ error: 'username, password, and fullName are required' });
    }
    const normalizedRole = role === 'Admin' ? 'Admin' : 'Staff';
    if (normalizedRole === 'Admin') {
      return res.status(403).json({ error: 'Admin accounts must be created by an existing admin' });
    }
    const existing = await query('SELECT id FROM users WHERE username = :username', { username });
    if (existing.length) {
      return res.status(409).json({ error: 'Username already exists' });
    }
    const passwordHash = await bcrypt.hash(password, 10);
    const timestamp = Date.now();
    const result = await query(
      `INSERT INTO users (username, password_hash, full_name, role, timestamp)
       VALUES (:username, :passwordHash, :fullName, :role, :timestamp)`,
      { username, passwordHash, fullName, role: normalizedRole, timestamp }
    );
    const user = mapUser({
      id: result.insertId,
      username,
      full_name: fullName,
      role: normalizedRole,
      timestamp
    });
    const token = jwt.sign(user, process.env.JWT_SECRET || 'dev-secret', { expiresIn: '30d' });
    return res.status(201).json({ token, user });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

router.post('/auth/login', async (req, res) => {
  try {
    const { username, password } = req.body || {};
    if (!username || !password) {
      return res.status(400).json({ error: 'username and password are required' });
    }
    const rows = await query('SELECT * FROM users WHERE username = :username LIMIT 1', { username });
    if (!rows.length) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }
    const row = rows[0];
    const ok = await bcrypt.compare(password, row.password_hash);
    if (!ok) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }
    const user = mapUser(row);
    const token = jwt.sign(user, process.env.JWT_SECRET || 'dev-secret', { expiresIn: '30d' });
    return res.json({ token, user });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

router.get('/auth/me', authRequired, async (req, res) => {
  try {
    const rows = await query('SELECT * FROM users WHERE id = :id LIMIT 1', { id: req.user.id });
    if (!rows.length) return res.status(404).json({ error: 'User not found' });
    return res.json({ user: mapUser(rows[0]) });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

router.get('/users', authRequired, adminRequired, async (_req, res) => {
  try {
    const rows = await query('SELECT * FROM users ORDER BY timestamp DESC');
    res.json(rows.map(mapUser));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.post('/users', authRequired, adminRequired, async (req, res) => {
  try {
    const { username, password, fullName, role = 'Staff', phone = null } = req.body || {};
    if (!username || !password || !fullName) {
      return res.status(400).json({ error: 'username, password, and fullName are required' });
    }
    if (!phone) {
      return res.status(400).json({ error: 'WhatsApp phone is required (e.g. 03001234567 or 923001234567)' });
    }
    const existing = await query('SELECT id FROM users WHERE username = :username', { username });
    if (existing.length) {
      return res.status(409).json({ error: 'Username already exists' });
    }
    const normalizedRole = role === 'Admin' ? 'Admin' : 'Staff';
    const passwordHash = await bcrypt.hash(password, 10);
    const timestamp = Date.now();
    const result = await query(
      `INSERT INTO users (username, password_hash, full_name, role, phone, timestamp)
       VALUES (:username, :passwordHash, :fullName, :role, :phone, :timestamp)`,
      { username, passwordHash, fullName, role: normalizedRole, phone, timestamp }
    );
    const user = mapUser({
      id: result.insertId,
      username,
      full_name: fullName,
      role: normalizedRole,
      phone,
      timestamp
    });

    // Fire-and-forget WhatsApp welcome with credentials + portal URL
    notifyWelcome({ phone, fullName, username, password }).catch((err) => {
      console.error('[waha] welcome failed', err.message);
    });

    res.status(201).json({ user, whatsappQueued: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.delete('/users/:id', authRequired, adminRequired, async (req, res) => {
  try {
    await query('DELETE FROM users WHERE id = :id', { id: req.params.id });
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/expenses', authRequired, async (req, res) => {
  try {
    let rows;
    if (req.user.role === 'Admin') {
      rows = await query('SELECT * FROM expenses ORDER BY timestamp DESC');
    } else {
      rows = await query(
        `SELECT * FROM expenses
         WHERE staff_name = :fullName OR staff_name = :username
         ORDER BY timestamp DESC`,
        { fullName: req.user.fullName, username: req.user.username }
      );
    }
    res.json(rows.map(mapExpense));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.post('/expenses', authRequired, async (req, res) => {
  try {
    const body = req.body || {};
    const amount = Number(body.amount);
    const category = body.category;
    const description = body.description || '';
    if (!Number.isFinite(amount) || !category) {
      return res.status(400).json({ error: 'amount and category are required' });
    }
    const timestamp = body.timestamp || Date.now();
    const staffName = body.staffName || req.user.fullName || req.user.username;
    const result = await query(
      `INSERT INTO expenses
        (amount, category, description, timestamp, staff_name, status, receipt_uri,
         approval_timestamp, admin_notes, is_synced, latitude, longitude, location_address)
       VALUES
        (:amount, :category, :description, :timestamp, :staffName, :status, :receiptUri,
         :approvalTimestamp, :adminNotes, 1, :latitude, :longitude, :locationAddress)`,
      {
        amount,
        category,
        description,
        timestamp,
        staffName,
        status: body.status || 'PENDING',
        receiptUri: body.receiptUri || null,
        approvalTimestamp: body.approvalTimestamp || null,
        adminNotes: body.adminNotes || null,
        latitude: body.latitude ?? null,
        longitude: body.longitude ?? null,
        locationAddress: body.locationAddress || null
      }
    );
    const rows = await query('SELECT * FROM expenses WHERE id = :id', { id: result.insertId });
    const expense = mapExpense(rows[0]);

    const me = await query('SELECT phone FROM users WHERE id = :id LIMIT 1', { id: req.user.id });
    notifyExpenseSubmitted({
      staffPhone: me[0]?.phone || null,
      adminPhone: adminAlertPhone(),
      expense,
      staffName
    }).catch((err) => console.error('[waha] expense submit notify failed', err.message));

    res.status(201).json(expense);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.put('/expenses/:id', authRequired, async (req, res) => {
  try {
    const id = req.params.id;
    const existing = await query('SELECT * FROM expenses WHERE id = :id', { id });
    if (!existing.length) return res.status(404).json({ error: 'Expense not found' });

    const current = existing[0];
    if (
      req.user.role !== 'Admin' &&
      current.staff_name !== req.user.fullName &&
      current.staff_name !== req.user.username
    ) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    const body = req.body || {};
    let status = current.status;
    let approvalTimestamp = current.approval_timestamp;
    let adminNotes = current.admin_notes;

    if (req.user.role === 'Admin') {
      if (body.status) status = body.status;
      if (body.adminNotes !== undefined) adminNotes = body.adminNotes;
      if (body.status === 'APPROVED' || body.status === 'REJECTED') {
        approvalTimestamp = body.approvalTimestamp || Date.now();
      }
    }

    await query(
      `UPDATE expenses SET
        amount = :amount,
        category = :category,
        description = :description,
        status = :status,
        receipt_uri = :receiptUri,
        approval_timestamp = :approvalTimestamp,
        admin_notes = :adminNotes,
        latitude = :latitude,
        longitude = :longitude,
        location_address = :locationAddress,
        is_synced = 1
       WHERE id = :id`,
      {
        id,
        amount: body.amount ?? current.amount,
        category: body.category ?? current.category,
        description: body.description ?? current.description,
        status,
        receiptUri: body.receiptUri !== undefined ? body.receiptUri : current.receipt_uri,
        approvalTimestamp,
        adminNotes,
        latitude: body.latitude !== undefined ? body.latitude : current.latitude,
        longitude: body.longitude !== undefined ? body.longitude : current.longitude,
        locationAddress: body.locationAddress !== undefined ? body.locationAddress : current.location_address
      }
    );
    const rows = await query('SELECT * FROM expenses WHERE id = :id', { id });
    const expense = mapExpense(rows[0]);

    if (
      req.user.role === 'Admin' &&
      (status === 'APPROVED' || status === 'REJECTED') &&
      status !== current.status
    ) {
      const staffPhone = await findUserPhoneByName(current.staff_name);
      notifyExpenseDecision({
        staffPhone,
        expense,
        status
      }).catch((err) => console.error('[waha] expense decision notify failed', err.message));
    }

    res.json(expense);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.delete('/expenses/:id', authRequired, async (req, res) => {
  try {
    const existing = await query('SELECT * FROM expenses WHERE id = :id', { id: req.params.id });
    if (!existing.length) return res.status(404).json({ error: 'Expense not found' });
    const current = existing[0];
    if (
      req.user.role !== 'Admin' &&
      current.staff_name !== req.user.fullName &&
      current.staff_name !== req.user.username
    ) {
      return res.status(403).json({ error: 'Forbidden' });
    }
    await query('DELETE FROM expenses WHERE id = :id', { id: req.params.id });
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/allocations', authRequired, async (_req, res) => {
  try {
    const rows = await query('SELECT * FROM budget_allocations ORDER BY timestamp DESC');
    res.json(rows.map(mapAllocation));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.post('/allocations', authRequired, adminRequired, async (req, res) => {
  try {
    const amount = Number(req.body?.amount);
    const description = req.body?.description || '';
    if (!Number.isFinite(amount)) {
      return res.status(400).json({ error: 'amount is required' });
    }
    const timestamp = req.body?.timestamp || Date.now();
    const result = await query(
      `INSERT INTO budget_allocations (amount, description, timestamp)
       VALUES (:amount, :description, :timestamp)`,
      { amount, description, timestamp }
    );
    const rows = await query('SELECT * FROM budget_allocations WHERE id = :id', { id: result.insertId });
    res.status(201).json(mapAllocation(rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.delete('/allocations/:id', authRequired, adminRequired, async (req, res) => {
  try {
    await query('DELETE FROM budget_allocations WHERE id = :id', { id: req.params.id });
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/attendance', authRequired, async (req, res) => {
  try {
    let rows;
    if (req.user.role === 'Admin') {
      rows = await query('SELECT * FROM attendance ORDER BY timestamp DESC');
    } else {
      rows = await query(
        `SELECT * FROM attendance
         WHERE staff_name = :fullName OR staff_name = :username
         ORDER BY timestamp DESC`,
        { fullName: req.user.fullName, username: req.user.username }
      );
    }
    res.json(rows.map(mapAttendance));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.post('/attendance', authRequired, async (req, res) => {
  try {
    const body = req.body || {};
    const type = body.type === 'OUT' ? 'OUT' : 'IN';
    const latitude = Number(body.latitude);
    const longitude = Number(body.longitude);
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      return res.status(400).json({ error: 'latitude and longitude are required' });
    }
    const timestamp = body.timestamp || Date.now();
    const staffName = body.staffName || req.user.fullName || req.user.username;
    const locationAddress = body.locationAddress || '';
    const result = await query(
      `INSERT INTO attendance
        (staff_name, type, timestamp, latitude, longitude, location_address, is_synced)
       VALUES
        (:staffName, :type, :timestamp, :latitude, :longitude, :locationAddress, 1)`,
      { staffName, type, timestamp, latitude, longitude, locationAddress }
    );
    const rows = await query('SELECT * FROM attendance WHERE id = :id', { id: result.insertId });
    const attendance = mapAttendance(rows[0]);

    const me = await query('SELECT phone FROM users WHERE id = :id LIMIT 1', { id: req.user.id });
    notifyDuty({
      phone: me[0]?.phone || null,
      adminPhone: adminAlertPhone(),
      staffName,
      type,
      locationAddress,
      timestamp
    }).catch((err) => console.error('[waha] duty notify failed', err.message));

    res.status(201).json(attendance);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.delete('/attendance/:id', authRequired, async (req, res) => {
  try {
    const existing = await query('SELECT * FROM attendance WHERE id = :id', { id: req.params.id });
    if (!existing.length) return res.status(404).json({ error: 'Attendance not found' });
    const current = existing[0];
    if (
      req.user.role !== 'Admin' &&
      current.staff_name !== req.user.fullName &&
      current.staff_name !== req.user.username
    ) {
      return res.status(403).json({ error: 'Forbidden' });
    }
    await query('DELETE FROM attendance WHERE id = :id', { id: req.params.id });
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
