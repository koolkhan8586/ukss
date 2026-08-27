const XLSX = require('xlsx');

function formatTs(ts) {
  return new Date(Number(ts)).toLocaleString('en-PK');
}

function monthRange(monthStr) {
  const [y, m] = String(monthStr).split('-').map(Number);
  if (!y || !m || m < 1 || m > 12) return null;
  const start = new Date(y, m - 1, 1).getTime();
  const end = new Date(y, m, 0, 23, 59, 59, 999).getTime();
  return { start, end, label: `${y}-${String(m).padStart(2, '0')}` };
}

function sendWorkbook(res, filename, sheetName, rows) {
  const wb = XLSX.utils.book_new();
  const ws = XLSX.utils.json_to_sheet(rows);
  XLSX.utils.book_append_sheet(wb, ws, sheetName);
  const buf = XLSX.write(wb, { type: 'buffer', bookType: 'xlsx' });
  res.setHeader(
    'Content-Type',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  );
  res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
  res.send(buf);
}

function buildExpensesWorkbook(expenses) {
  return expenses.map((e) => ({
    ID: e.id,
    'Staff Name': e.staffName,
    'Amount (Rs)': e.amount,
    Category: e.category,
    Description: e.description,
    Status: e.status,
    Date: formatTs(e.timestamp),
    'Admin Notes': e.adminNotes || '',
    Latitude: e.latitude ?? '',
    Longitude: e.longitude ?? '',
    Location: e.locationAddress || ''
  }));
}

function buildAttendanceWorkbook(attendance, monthLabel) {
  return attendance.map((a) => ({
    ID: a.id,
    'Staff Name': a.staffName,
    Type: a.type,
    Date: formatTs(a.timestamp),
    Month: monthLabel,
    Latitude: a.latitude,
    Longitude: a.longitude,
    Location: a.locationAddress || ''
  }));
}

module.exports = {
  monthRange,
  sendWorkbook,
  buildExpensesWorkbook,
  buildAttendanceWorkbook
};
