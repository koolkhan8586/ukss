# Contabo Ubuntu deploy — /home/ukss (no aaPanel)

Your app lives at **`/home/ukss`**. The Node API is inside **`/home/ukss/server`**
(not the repo root). That is why `npm install` in `/home/ukss` failed.

## One-time setup on the VPS

```bash
cd /home/ukss

# Get the branch that contains /server
git fetch origin
git checkout cursor/exp-nginx-server-db-52c0
# or merge it into main first, then: git pull

# Configure MySQL (use YOUR server database)
cp server/.env.example server/.env
nano server/.env
```

Set at least:

```env
APP_URL=https://exp.ukssolution.com
PORT=3000
JWT_SECRET=pick-a-long-random-string
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=your_mysql_database
DB_USER=your_mysql_user
DB_PASSWORD=your_mysql_password
ADMIN_USERNAME=admin
ADMIN_PASSWORD=ChangeMe123!
```

Create the MySQL database/user if needed:

```bash
sudo mysql -e "CREATE DATABASE IF NOT EXISTS ukss_expense CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'ukss_expense'@'localhost' IDENTIFIED BY 'STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON ukss_expense.* TO 'ukss_expense'@'localhost';
FLUSH PRIVILEGES;"
```

Then deploy:

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
npm start   # test in foreground first

# nginx
sudo cp /home/ukss/deploy/nginx/exp.ukssolution.com.conf /etc/nginx/sites-available/exp.ukssolution.com
sudo ln -sf /etc/nginx/sites-available/exp.ukssolution.com /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

Optional SSL (origin cert; Cloudflare Full mode):

```bash
sudo certbot --nginx -d exp.ukssolution.com
```

With **Cloudflare Flexible**, HTTP on port 80 is enough.

## Verify

```bash
curl -s http://127.0.0.1:3000/api/health
curl -sI http://exp.ukssolution.com
systemctl status ukss-expense
```
