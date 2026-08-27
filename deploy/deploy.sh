#!/usr/bin/env bash
# Deploy UKSS Expense Portal to Contabo/aaPanel for exp.ukssolution.com
set -euo pipefail

APP_DIR="${APP_DIR:-/www/wwwroot/exp.ukssolution.com}"
REPO_SERVER_DIR="$(cd "$(dirname "$0")/../server" && pwd)"
SERVICE_NAME="ukss-expense"

echo "==> Syncing server files to ${APP_DIR}"
mkdir -p "${APP_DIR}"
rsync -a --delete \
  --exclude node_modules \
  --exclude .env \
  "${REPO_SERVER_DIR}/" "${APP_DIR}/"

if [[ ! -f "${APP_DIR}/.env" ]]; then
  cp "${APP_DIR}/.env.example" "${APP_DIR}/.env"
  echo "Created ${APP_DIR}/.env — edit DB_* values before migrate."
fi

cd "${APP_DIR}"
npm install --omit=dev
npm run migrate

if command -v systemctl >/dev/null 2>&1; then
  cat > "/etc/systemd/system/${SERVICE_NAME}.service" <<EOF
[Unit]
Description=UKSS Expense Portal (exp.ukssolution.com)
After=network.target mysql.service mariadb.service

[Service]
Type=simple
WorkingDirectory=${APP_DIR}
EnvironmentFile=${APP_DIR}/.env
ExecStart=/usr/bin/node ${APP_DIR}/src/index.js
Restart=always
RestartSec=3
User=www
Group=www

[Install]
WantedBy=multi-user.target
EOF
  systemctl daemon-reload
  systemctl enable --now "${SERVICE_NAME}"
  systemctl restart "${SERVICE_NAME}"
  echo "==> systemd service ${SERVICE_NAME} started"
else
  echo "systemctl not found — start manually: cd ${APP_DIR} && npm start"
fi

NGINX_SRC="$(cd "$(dirname "$0")/nginx" && pwd)/exp.ukssolution.com.conf"
if [[ -d /www/server/panel/vhost/nginx ]]; then
  cp "${NGINX_SRC}" /www/server/panel/vhost/nginx/exp.ukssolution.com.conf
  nginx -t && nginx -s reload
  echo "==> aaPanel nginx vhost installed and reloaded"
elif [[ -d /etc/nginx/sites-available ]]; then
  cp "${NGINX_SRC}" /etc/nginx/sites-available/exp.ukssolution.com
  ln -sf /etc/nginx/sites-available/exp.ukssolution.com /etc/nginx/sites-enabled/exp.ukssolution.com
  nginx -t && systemctl reload nginx
  echo "==> nginx site enabled and reloaded"
else
  echo "Copy deploy/nginx/exp.ukssolution.com.conf into your nginx vhost folder manually."
fi

echo "==> Done. Open https://exp.ukssolution.com"
