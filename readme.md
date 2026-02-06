# HydroSpark — Run Locally

## 1) Install
- Docker Desktop (running)
- Git

## 2) Clone
```bash
git clone https://github.com/Landon-Bragg/HydroSpark-Water-Usage-Billing-System.git
cd HydroSpark-Water-Usage-Billing-System
```

## 3) Frontend env

Create frontend/.env with:

REACT_APP_API_BASE_URL=http://localhost:8080

4) Build + start

From the project root:

docker compose build
docker compose up -d

5) Confirm backend is up
curl http://localhost:8080/actuator/health


Expected:

{"status":"UP", ...}

6) Open app

Go to:

http://localhost:3000

7) Login

admin@hydrospark.com
 / Admin123!

billing@hydrospark.com
 / Billing123!

operations@hydrospark.com
 / Operations123!

support@hydrospark.com
 / Support123!

customer@hydrospark.com
 / Customer123!

8) Stop
docker compose down
