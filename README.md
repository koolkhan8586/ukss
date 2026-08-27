# UK Security Solutions — Expense Portal

Offline-first Android expense manager with a **server-backed** portal at
**https://exp.ukssolution.com** (nginx → Node.js → MySQL).

## Components

| Path | Purpose |
|------|---------|
| `app/` | Android (Kotlin / Compose) client |
| `server/` | Node.js API + web UI |
| `deploy/nginx/` | nginx vhost for `exp.ukssolution.com` |
| `deploy/deploy.sh` | Contabo / aaPanel deploy script |
| `docker-compose.yml` | Local MySQL + API for development |

## Quick local API (Docker)

```bash
docker compose up --build
# Web UI: http://localhost:3000
# API health: http://localhost:3000/api/health
# Default admin: admin / ChangeMe123!
```

## Production deploy

See [deploy/README.md](deploy/README.md). You need:

1. aaPanel site bound to `exp.ukssolution.com`
2. MySQL database credentials in `server/.env`
3. nginx reverse-proxy to `127.0.0.1:3000`

## Android

1. Open in Android Studio
2. API base URL: `https://exp.ukssolution.com/api/` (`ApiConfig.kt`)
3. Optional: set `GEMINI_API_KEY` in `.env` (see `.env.example`)
