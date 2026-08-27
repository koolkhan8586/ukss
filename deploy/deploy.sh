#!/usr/bin/env bash
# Deploy UKSS Expense Portal on Contabo Ubuntu (/home/ukss)
# Usage (from repo root):
#   sudo ./deploy/deploy.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="${APP_DIR:-${REPO_ROOT}/server}"
SERVICE_NAME="ukss-expense"
NGINX_AVAILABLE="/etc/nginx/sites-available/exp.ukssolution.com"
NGINX_ENABLED="/etc/nginx/sites-enabled/exp.ukssolution.com"

echo "==> App directory: ${APP_DIR}"

if [[ ! -f "${APP_DIR}/package.json" ]]; then
  echo "ERROR: ${APP_DIR}/package.json not found."
  echo "Pull the branch that contains /server first, e.g.:"
  echo "  cd /home/ukss && git fetch origin && git checkout cursor/exp-nginx-server-db-52c0"
  exit 1
fi

if [[ ! -f "${APP_DIR}/.env" ]]; then
  cp "${APP_DIR}/.env.example" "${APP_DIR}/.env"
  echo "Created ${APP_DIR}/.env — edit DB_* / JWT_SECRET, then re-run this script."
  echo "  nano ${APP_DIR}/.env"
  exit 1
fi

cd "${APP_DIR}"
npm install --omit=dev
npm run migrate

NODE_BIN="$(command -v node)"
cat > "/etc/systemd/system/${SERVICE_NAME}.service" <<EOF
[Unit]
Description=UKSS Expense Portal (exp.ukssolution.com)
After=network.target mysql.service mariadb.service

[Service]
Type=simple
WorkingDirectory=${APP_DIR}
EnvironmentFile=${APP_DIR}/.env
ExecStart=${NODE_BIN} ${APP_DIR}/src/index.js
Restart=always
RestartSec=3
User=root
Group=root

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable --now "${SERVICE_NAME}"
systemctl restart "${SERVICE_NAME}"
echo "==> systemd service ${SERVICE_NAME} started"

cp "${REPO_ROOT}/deploy/nginx/exp.ukssolution.com.conf" "${NGINX_AVAILABLE}"
ln -sf "${NGINX_AVAILABLE}" "${NGINX_ENABLED}"
nginx -t
systemctl reload nginx
echo "==> nginx site enabled: exp.ukssolution.com"

echo
echo "==> Done."
echo "    Health: curl -s http://127.0.0.1:3007/api/health"
echo "    Public: https://exp.ukssolution.com"
echo "    Optional SSL: certbot --nginx -d exp.ukssolution.com"
