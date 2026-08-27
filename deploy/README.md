# Contabo Ubuntu deploy — /home/ukss

App path: **`/home/ukss`**. Node API: **`/home/ukss/server`** (port **3007**).

## 1. Create MySQL database (required once)

```bash
# Edit the password in this file first, then:
nano /home/ukss/deploy/create-database.sql
sudo mysql < /home/ukss/deploy/create-database.sql
```

Or one-liner (replace `STRONG_PASSWORD`):

```bash
sudo mysql -e "
CREATE DATABASE IF NOT EXISTS ukss_expense CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'ukss_expense'@'localhost' IDENTIFIED BY 'STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON ukss_expense.* TO 'ukss_expense'@'localhost';
FLUSH PRIVILEGES;"
```

## 2. Configure `.env`

```bash
cd /home/ukss
cp server/.env.example server/.env
nano server/.env
```

| Variable | What it is |
|----------|------------|
| `PORT` | **3007** (API listen port; nginx proxies to this) |
| `JWT_SECRET` | Random secret used to **sign login tokens**. Not a MySQL password. Anyone with this string can forge logins — keep it private. Generate: `openssl rand -hex 32` |
| `DB_*` | Your MySQL host/name/user/password from step 1 |
| `ADMIN_*` | First admin account created by `npm run migrate` |

Example:

```env
APP_URL=https://exp.ukssolution.com
PORT=3007
JWT_SECRET=paste_output_of_openssl_rand_hex_32
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=ukss_expense
DB_USER=ukss_expense
DB_PASSWORD=STRONG_PASSWORD
ADMIN_USERNAME=admin
ADMIN_PASSWORD=ChangeMe123!
ADMIN_FULL_NAME=Portal Admin
```

## 3. Deploy

```bash
cd /home/ukss
chmod +x deploy/deploy.sh
sudo ./deploy/deploy.sh
```

Or manually:

```bash
cd /home/ukss/server
npm install --omit=dev
npm run migrate
# foreground test: npm start

sudo cp /home/ukss/deploy/nginx/exp.ukssolution.com.conf /etc/nginx/sites-available/exp.ukssolution.com
sudo ln -sf /etc/nginx/sites-available/exp.ukssolution.com /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

Optional SSL: `sudo certbot --nginx -d exp.ukssolution.com`

## 4. Verify

```bash
curl -s http://127.0.0.1:3007/api/health
systemctl status ukss-expense
```
