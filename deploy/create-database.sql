-- Run once on Contabo as root:
--   sudo mysql < /home/ukss/deploy/create-database.sql
-- Then put the same password into server/.env as DB_PASSWORD

CREATE DATABASE IF NOT EXISTS ukss_expense
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'ukss_expense'@'localhost'
  IDENTIFIED BY 'CHANGE_THIS_DB_PASSWORD';

GRANT ALL PRIVILEGES ON ukss_expense.* TO 'ukss_expense'@'localhost';
FLUSH PRIVILEGES;
