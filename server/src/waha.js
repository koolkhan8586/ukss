const http = require('http');
const https = require('https');
const { URL } = require('url');

const APP_URL = process.env.APP_URL || 'https://exp.ukssolution.com';

function formatMoneyRs(amount) {
  const n = Number(amount) || 0;
  return `Rs. ${n.toLocaleString('en-PK', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
}

/**
 * Normalize phone to WAHA chatId.
 * Accepts: 03001234567, +923001234567, 923001234567
 */
function toChatId(phone) {
  if (!phone) return null;
  let digits = String(phone).replace(/\D/g, '');
  if (!digits) return null;
  if (digits.startsWith('0') && digits.length === 11) {
    digits = `92${digits.slice(1)}`;
  }
  return `${digits}@c.us`;
}

function getJson(urlString, headers) {
  return new Promise((resolve, reject) => {
    const url = new URL(urlString);
    const lib = url.protocol === 'https:' ? https : http;
    const req = lib.request(
      {
        protocol: url.protocol,
        hostname: url.hostname,
        port: url.port || (url.protocol === 'https:' ? 443 : 80),
        path: `${url.pathname}${url.search}`,
        method: 'GET',
        headers: { Accept: 'application/json', ...headers }
      },
      (res) => {
        let raw = '';
        res.on('data', (chunk) => { raw += chunk; });
        res.on('end', () => {
          let parsed = {};
          try { parsed = JSON.parse(raw || '{}'); } catch { parsed = { raw }; }
          resolve({ status: res.statusCode || 0, body: parsed });
        });
      }
    );
    req.on('error', reject);
    req.end();
  });
}

function wahaHeaders() {
  const headers = { Accept: 'application/json' };
  if (process.env.WAHA_API_KEY) {
    headers['X-Api-Key'] = process.env.WAHA_API_KEY;
  }
  return headers;
}

async function checkSessionStatus() {
  const baseUrl = (process.env.WAHA_BASE_URL || '').replace(/\/$/, '');
  if (!baseUrl) {
    return {
      configured: false,
      connected: false,
      status: 'not_configured',
      message: 'WAHA_BASE_URL is not set in server/.env'
    };
  }
  const session = process.env.WAHA_SESSION || 'default';
  try {
    const res = await getJson(`${baseUrl}/api/sessions/${encodeURIComponent(session)}`, wahaHeaders());
    if (res.status === 404) {
      return {
        configured: true,
        connected: false,
        status: 'session_not_found',
        session,
        message: `Session "${session}" not found in WAHA`
      };
    }
    if (res.status < 200 || res.status >= 300) {
      return {
        configured: true,
        connected: false,
        status: 'error',
        session,
        message: `WAHA returned HTTP ${res.status}`
      };
    }
    const state = String(res.body?.status || res.body?.state || '').toUpperCase();
    const working = state === 'WORKING' || state === 'CONNECTED' || state === 'OPEN';
    return {
      configured: true,
      connected: working,
      status: state || 'unknown',
      session,
      message: working ? 'WhatsApp is connected via WAHA' : `Session status: ${state || 'unknown'}`
    };
  } catch (err) {
    return {
      configured: true,
      connected: false,
      status: 'unreachable',
      session,
      message: `Cannot reach WAHA: ${err.message}`
    };
  }
}

function postJson(urlString, headers, payload) {
  return new Promise((resolve, reject) => {
    const url = new URL(urlString);
    const lib = url.protocol === 'https:' ? https : http;
    const body = JSON.stringify(payload);
    const req = lib.request(
      {
        protocol: url.protocol,
        hostname: url.hostname,
        port: url.port || (url.protocol === 'https:' ? 443 : 80),
        path: `${url.pathname}${url.search}`,
        method: 'POST',
        headers: {
          ...headers,
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(body)
        }
      },
      (res) => {
        let raw = '';
        res.on('data', (chunk) => { raw += chunk; });
        res.on('end', () => {
          let parsed = {};
          try { parsed = JSON.parse(raw || '{}'); } catch { parsed = { raw }; }
          resolve({ status: res.statusCode || 0, body: parsed });
        });
      }
    );
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

async function sendWhatsApp(phone, text) {
  const baseUrl = (process.env.WAHA_BASE_URL || '').replace(/\/$/, '');
  if (!baseUrl) {
    console.warn('[waha] WAHA_BASE_URL not set — skipping WhatsApp');
    return { skipped: true, reason: 'WAHA_BASE_URL missing' };
  }
  const chatId = toChatId(phone);
  if (!chatId) {
    return { skipped: true, reason: 'invalid phone' };
  }

  const session = process.env.WAHA_SESSION || 'default';
  const headers = wahaHeaders();

  try {
    const res = await postJson(`${baseUrl}/api/sendText`, headers, { session, chatId, text });
    if (res.status < 200 || res.status >= 300) {
      console.error('[waha] send failed', res.status, res.body);
      return { ok: false, status: res.status, body: res.body };
    }
    return { ok: true, body: res.body };
  } catch (err) {
    console.error('[waha] send error', err.message);
    return { ok: false, error: err.message };
  }
}

async function notifyWelcome({ phone, fullName, username, password }) {
  const text =
    `Welcome to UK Security Solutions Expense Portal\n\n` +
    `Hi ${fullName},\n` +
    `Your account has been created.\n\n` +
    `Portal: ${APP_URL}\n` +
    `Username: ${username}\n` +
    `Password: ${password}\n\n` +
    `You can submit expenses and mark Duty In / Duty Out from the portal.`;
  return sendWhatsApp(phone, text);
}

async function notifyExpenseSubmitted({ staffPhone, adminPhone, expense, staffName }) {
  const staffText =
    `Expense submitted\n\n` +
    `Amount: ${formatMoneyRs(expense.amount)}\n` +
    `Category: ${expense.category}\n` +
    `Status: PENDING\n` +
    `Desc: ${expense.description}\n\n` +
    `Portal: ${APP_URL}`;
  const adminText =
    `New expense request\n\n` +
    `Staff: ${staffName}\n` +
    `Amount: ${formatMoneyRs(expense.amount)}\n` +
    `Category: ${expense.category}\n` +
    `Desc: ${expense.description}\n\n` +
    `Review: ${APP_URL}`;

  const results = [];
  if (staffPhone) results.push(await sendWhatsApp(staffPhone, staffText));
  if (adminPhone) results.push(await sendWhatsApp(adminPhone, adminText));
  return results;
}

async function notifyExpenseDecision({ staffPhone, expense, status }) {
  const text =
    `Expense ${status}\n\n` +
    `Amount: ${formatMoneyRs(expense.amount)}\n` +
    `Category: ${expense.category}\n` +
    `Desc: ${expense.description}\n` +
    (expense.adminNotes ? `Notes: ${expense.adminNotes}\n` : '') +
    `\nPortal: ${APP_URL}`;
  return sendWhatsApp(staffPhone, text);
}

async function notifyDuty({ phone, adminPhone, staffName, type, locationAddress, timestamp }) {
  const when = new Date(Number(timestamp) || Date.now()).toLocaleString('en-PK');
  const staffText =
    `Duty ${type}\n\n` +
    `Staff: ${staffName}\n` +
    `Time: ${when}\n` +
    `Location: ${locationAddress || 'N/A'}\n\n` +
    `Portal: ${APP_URL}`;
  const adminText =
    `Duty ${type} alert\n\n` +
    `${staffName} marked Duty ${type}\n` +
    `Time: ${when}\n` +
    `Location: ${locationAddress || 'N/A'}`;

  const results = [];
  if (phone) results.push(await sendWhatsApp(phone, staffText));
  if (adminPhone) results.push(await sendWhatsApp(adminPhone, adminText));
  return results;
}

async function notifyPasswordReset({ phone, fullName, username, password }) {
  const text =
    `Password reset — UK Security Solutions\n\n` +
    `Hi ${fullName},\n` +
    `Your portal password was reset by admin.\n\n` +
    `Portal: ${APP_URL}\n` +
    `Username: ${username}\n` +
    `New password: ${password}\n\n` +
    `Please sign in and change your password from Account settings.`;
  return sendWhatsApp(phone, text);
}

module.exports = {
  formatMoneyRs,
  toChatId,
  sendWhatsApp,
  checkSessionStatus,
  notifyWelcome,
  notifyPasswordReset,
  notifyExpenseSubmitted,
  notifyExpenseDecision,
  notifyDuty
};
