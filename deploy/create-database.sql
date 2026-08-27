-- Create MySQL database + user for UKSS Expense Portal
-- MySQL validate_password requires a STRONG password:
--   length 8+, mixed upper/lower, number, and special character
--
-- 1) Edit CHANGE_THIS... below to your own strong password
-- 2) sudo mysql < /home/ukss/deploy/create-database.sql
-- 3) Put the SAME password in server/.env as DB_PASSWORD

CREATE DATABASE IF NOT EXISTS ukss_expense
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Drop leftover user from failed attempts (safe if missing)
DROP USER IF EXISTS 'ukss_expense'@'localhost';

CREATE USER 'ukss_expense'@'localhost'
  IDENTIFIED BY 'UkssExp@2026!Db';

GRANT ALL PRIVILEGES ON ukss_expense.* TO 'ukss_expense'@'localhost';
FLUSH PRIVILEGES;
