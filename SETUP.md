# Text-to-SQL Application - Production Setup Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Environment Configuration](#environment-configuration)
3. [Email Configuration](#email-configuration)
4. [Docker Deployment](#docker-deployment)
5. [Development Setup](#development-setup)
6. [Administration](#administration)
7. [Security & Performance](#security--performance)
8. [API Documentation](#api-documentation)

## Prerequisites

- **Docker & Docker Compose** (v3.8+)
- **Java 17+** (for local development)
- **Node.js 18+ & npm** (for frontend development)
- **PostgreSQL 15+** (for local development)
- **Python 3.10+** (for LLM server)
- **NVIDIA Container Toolkit** (Recommended for LLM performance)

## Environment Configuration

### 1. Copy the example environment file

```bash
cp .env.example .env
```

### 2. Update the `.env` file with your configuration

```env
# Database Configuration
POSTGRES_DB=texttosql
POSTGRES_USER=postgres
POSTGRES_PASSWORD=CHANGE_THIS_TO_SECURE_PASSWORD
POSTGRES_PORT=5432:5432

# Backend Configuration
BACKEND_PORT=8080
HIBERNATE_DDL_AUTO=update

# JWT Configuration
JWT_SECRET=GENERATE_A_SECURE_32_CHARACTER_SECRET_KEY_HERE
JWT_ACCESS_EXPIRATION=900000        # 15 minutes
JWT_REFRESH_EXPIRATION=2592000000   # 30 days

# LLM Configuration
LLM_URL=http://llm:8000
LLM_PORT=8000
MODEL_PATH=hf.co/abdlkdr/QueryGen_Qwen2.5_Coder

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-gmail-app-password

# Frontend Configuration
FRONTEND_URL=http://localhost:4200
FRONTEND_PORT=4200

# Logging
LOG_LEVEL=INFO
```

## Email Configuration

The application uses an automated email system for:
- User registration verification
- Password reset requests
- Support contact messages (Sent to `MAIL_USERNAME`)

### Gmail Setup

1. **Enable 2-Factor Authentication** on your Gmail account.
2. **Generate an App Password**:
   - Google Account > Security > 2-Step Verification > App passwords.
   - Select "Other" and name it "QueryGen".
   - Copy the 16-character code into `MAIL_PASSWORD`.

## Docker Deployment

### Production Deployment

1. **Build and start all services**:
```bash
docker-compose up -d --build
```

2. **Check service health**:
```bash
docker-compose ps
```

### Service URLs

- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080
- **Swagger Documentation**: http://localhost:8080/swagger-ui.html
- **LLM Server API**: http://localhost:8000/docs

## Development Setup

### Backend (Spring Boot)

```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

### Frontend (Angular)

```bash
cd frontend/texttosql
npm install
npm start
# To update API models after backend changes:
npm run open-api-gen
```

## Administration

### Initial Admin Setup
Administrative roles are managed via the database for maximum security.

1. Connect to your PostgreSQL database.
2. Run the following command:
```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'your-admin-email@example.com';
```

### Admin Panel
Once logged in as an admin, you can access the **Admin Panel** from the navigation bar to manage global database templates.

## Security & Performance

### 1. Rate Limiting
- **Optimized for SPAs**: 100 requests per 60 seconds (IP-based).
- Configured in `application.yml` via Bucket4j.

### 2. Caching Strategy
- **Caffeine Cache**: Used for LLM responses and Template Schemas.
- **Smart Eviction**: Template cache is only cleared when an Admin modifies a template, ensuring zero impact on normal user performance.

### 3. Observability
- **AOP Logging**: Automatic method tracking and performance monitoring in `logs/backend.log`.
- **Log Rotation**: Daily archiving in `logs/archived/` with a 30-day retention policy.

## API Documentation

### Key Endpoint Groups
1. **Auth**: `/api/v1/auth/*` (Login, Register, Refresh, Profile)
2. **Schemas**: `/api/v1/schemas/*` (Import, Templates, Databases)
3. **Chat**: `/api/v1/chatbot/*` (Messages, Export, Feedback)
4. **Search**: `/api/v1/search/*` (Advanced schema/chat search)
5. **Support**: `/api/v1/support/contact` (Public contact form)

## Troubleshooting

### Build Failures
- Ensure Java 17 and Node.js 18+ are correctly installed.
- Run `./mvnw clean` and `npm cache clean --force` if issues persist.

### LLM Service Issues
- Check `MODEL_PATH` in `.env`.
- Ensure the LLM container has enough RAM (min 8GB recommended) or GPU access.

### Missing Admin Panel
- Clear your browser's LocalStorage or log out and back in after updating your role in the database.
