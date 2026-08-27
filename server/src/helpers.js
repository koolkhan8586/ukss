const jwt = require('jsonwebtoken');

function authRequired(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) {
    return res.status(401).json({ error: 'Unauthorized' });
  }
  try {
    req.user = jwt.verify(token, process.env.JWT_SECRET || 'dev-secret');
    return next();
  } catch {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
}

function adminRequired(req, res, next) {
  if (!req.user || req.user.role !== 'Admin') {
    return res.status(403).json({ error: 'Admin access required' });
  }
  return next();
}

function mapUser(row) {
  if (!row) return null;
  return {
    id: Number(row.id),
    username: row.username,
    fullName: row.full_name,
    role: row.role,
    phone: row.phone || null,
    timestamp: Number(row.timestamp)
  };
}

function mapExpense(row) {
  return {
    id: Number(row.id),
    amount: Number(row.amount),
    category: row.category,
    description: row.description,
    timestamp: Number(row.timestamp),
    staffName: row.staff_name,
    status: row.status,
    receiptUri: row.receipt_uri,
    approvalTimestamp: row.approval_timestamp == null ? null : Number(row.approval_timestamp),
    adminNotes: row.admin_notes,
    isSynced: Boolean(row.is_synced),
    latitude: row.latitude == null ? null : Number(row.latitude),
    longitude: row.longitude == null ? null : Number(row.longitude),
    locationAddress: row.location_address
  };
}

function mapAllocation(row) {
  return {
    id: Number(row.id),
    amount: Number(row.amount),
    description: row.description,
    timestamp: Number(row.timestamp)
  };
}

function mapAttendance(row) {
  return {
    id: Number(row.id),
    staffName: row.staff_name,
    type: row.type,
    timestamp: Number(row.timestamp),
    latitude: Number(row.latitude),
    longitude: Number(row.longitude),
    locationAddress: row.location_address,
    isSynced: Boolean(row.is_synced)
  };
}

module.exports = {
  authRequired,
  adminRequired,
  mapUser,
  mapExpense,
  mapAllocation,
  mapAttendance
};
