# Vantage: Local Development Guide

This document provides instructions for setting up and running the Vantage platform locally for development and demo purposes.

## 1. Prerequisites
- **Java 21** (JDK)
- **Node.js 20+** and npm
- **Docker** and Docker Compose
- **Git**

## 2. Infrastructure Setup
From the root directory, start the local PostgreSQL and RabbitMQ containers:
```bash
docker-compose up -d
```
- PostgreSQL will be available at `localhost:5432` (User: `vantage`, DB: `vantage_dev`)
- RabbitMQ Management will be available at `localhost:15672` (User: `guest`, Pass: `guest`)

## 3. Backend Setup
1. Navigate to the `backend/` directory.
2. Run the Spring Boot application:
   ```bash
   ./gradlew bootRun
   ```
3. The API will start on `http://localhost:8080`.
4. Flyway will automatically run migrations on startup.
5. Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## 4. Frontend Setup
1. Navigate to the `frontend/` directory.
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
4. The React application will be available at `http://localhost:5173`.

## 5. Observability (Optional)
To run the local observability stack (Prometheus + Grafana):
```bash
docker-compose -f docker-compose observability up -d
```
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin/admin)
