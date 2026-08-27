const path = require('path');
const express = require('express');
const cors = require('cors');
require('dotenv').config();

const api = require('./routes/api');

const app = express();
const port = Number(process.env.PORT || 3000);

app.use(cors({
  origin: true,
  credentials: true
}));
app.use(express.json({ limit: '5mb' }));

app.get('/healthz', (_req, res) => {
  res.json({ ok: true });
});

app.use('/api', api);
app.use(express.static(path.join(__dirname, '../public')));

app.get('*', (req, res, next) => {
  if (req.path.startsWith('/api')) return next();
  res.sendFile(path.join(__dirname, '../public/index.html'));
});

app.use((err, _req, res, _next) => {
  console.error(err);
  res.status(500).json({ error: 'Internal server error' });
});

app.listen(port, '127.0.0.1', () => {
  console.log(`UKSS Expense API listening on http://127.0.0.1:${port}`);
  console.log(`Public URL: ${process.env.APP_URL || 'https://exp.ukssolution.com'}`);
});
