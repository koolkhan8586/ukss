# Deploy UKSS Expense Portal to exp.ukssolution.com

This repo now includes a Node.js + MySQL backend and nginx config so the
Expense Portal runs at **https://exp.ukssolution.com** on your Contabo/aaPanel server.

## What was wrong before

`exp.ukssolution.com` already pointed at your Contabo IP (Cloudflare), but aaPanel
showed **Website not found** — the domain was not bound to an app yet. The Android
app also used a local Room database only.

## Architecture

```
Browser / Android app
        │
        ▼
 nginx (exp.ukssolution.com :443)
        │  reverse proxy
        ▼
 Node.js API  127.0.0.1:3000
        │
        ▼
 MySQL / MariaDB  (aaPanel database)
```

## 1. Create site + database in aaPanel

1. **Website → Add site**
   - Domain: `exp.ukssolution.com`
   - Web server: **Nginx**
   - Create a **MySQL** database (note name, user, password)
2. Issue SSL (Let's Encrypt) for `exp.ukssolution.com` in aaPanel.
3. DNS: `exp` A-record → your Contabo public IP (already working if Cloudflare proxy is on).

## 2. Deploy code on the VPS

On the server (as root):

```bash
cd /path/to/ukss
git pull
chmod +x deploy/deploy.sh
# Edit DB credentials first:
cp server/.env.example /www/wwwroot/exp.ukssolution.com/.env
nano /www/wwwroot/exp.ukssolution.com/.env
# Then:
APP_DIR=/www/wwwroot/exp.ukssolution.com ./deploy/deploy.sh
```

Or manually:

```bash
rsync -a server/ /www/wwwroot/exp.ukssolution.com/
cd /www/wwwroot/exp.ukssolution.com
cp .env.example .env   # set DB_* and JWT_SECRET
npm install --omit=dev
npm run migrate
# install nginx conf from deploy/nginx/exp.ukssolution.com.conf
# start with systemd or: npm start
```

## 3. Required `.env` values

| Variable | Example |
|----------|---------|
| `APP_URL` | `https://exp.ukssolution.com` |
| `PORT` | `3000` |
| `DB_HOST` | `127.0.0.1` |
| `DB_PORT` | `3306` |
| `DB_NAME` | aaPanel DB name |
| `DB_USER` | aaPanel DB user |
| `DB_PASSWORD` | aaPanel DB password |
| `JWT_SECRET` | long random string |
| `ADMIN_USERNAME` | `admin` |
| `ADMIN_PASSWORD` | strong password |

## 4. Verify

```bash
curl -s https://exp.ukssolution.com/api/health
# {"ok":true,"service":"ukss-expense","host":"exp.ukssolution.com"}
```

Open https://exp.ukssolution.com and sign in with the seeded admin.

## 5. Android app

The app API base URL is hard-coded to:

`https://exp.ukssolution.com/api/`

Login/register and expense/attendance writes go to the server MySQL when online,
and still cache into Room for offline use.
