const fs = require('fs');
const path = require('path');
const bcrypt = require('bcryptjs');
const mysql = require('mysql2/promise');
require('dotenv').config();

async function migrate() {
  const {
    DB_HOST = '127.0.0.1',
    DB_PORT = '3306',
    DB_USER,
    DB_PASSWORD,
    DB_NAME,
    ADMIN_USERNAME = 'admin',
    ADMIN_PASSWORD = 'ChangeMe123!',
    ADMIN_FULL_NAME = 'Portal Admin'
  } = process.env;

  if (!DB_USER || !DB_PASSWORD || !DB_NAME) {
    throw new Error('DB_USER, DB_PASSWORD, and DB_NAME are required in server/.env');
  }

  const rootConn = await mysql.createConnection({
    host: DB_HOST,
    port: Number(DB_PORT),
    user: DB_USER,
    password: DB_PASSWORD,
    multipleStatements: true
  });

  const schema = fs.readFileSync(path.join(__dirname, '../sql/schema.sql'), 'utf8');
  // Replace CREATE DATABASE / USE so aaPanel-created DBs still work when user lacks CREATE privilege
  const statements = schema
    .replace(/CREATE DATABASE IF NOT EXISTS[\s\S]*?;/i, '')
    .replace(/USE\s+\w+\s*;/i, '')
    .trim();

  await rootConn.query(`CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`);
  await rootConn.changeUser({ database: DB_NAME });
  await rootConn.query(statements);

  const [users] = await rootConn.query('SELECT COUNT(*) AS c FROM users');
  if (users[0].c === 0) {
    const hash = await bcrypt.hash(ADMIN_PASSWORD, 10);
    await rootConn.query(
      'INSERT INTO users (username, password_hash, full_name, role, timestamp) VALUES (?, ?, ?, ?, ?)',
      [ADMIN_USERNAME, hash, ADMIN_FULL_NAME, 'Admin', Date.now()]
    );
    console.log(`Seeded admin user: ${ADMIN_USERNAME}`);
  }

  await rootConn.end();
  console.log(`Migration complete for database: ${DB_NAME}`);
}

migrate().catch((err) => {
  console.error(err);
  process.exit(1);
});
