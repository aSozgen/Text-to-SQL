# Text-to-SQL Application - Production Setup Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Environment Configuration](#environment-configuration)
3. [Email Configuration](#email-configuration)
4. [Docker Deployment](#docker-deployment)
5. [Development Setup](#development-setup)
6. [Security Best Practices](#security-best-practices)
7. [API Documentation](#api-documentation)

## Prerequisites

- Docker & Docker Compose (v3.8+)
- Java 17+ (for local development)
- Node.js 18+ & npm (for frontend development)
- PostgreSQL 15+ (for local development)
- Python 3.9+ (for LLM server)

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
POSTGRES_PORT=5432

# Backend Configuration
BACKEND_PORT=8080
HIBERNATE_DDL_AUTO=update  # Use 'validate' in production

# JWT Configuration - IMPORTANT: Change these in production!
JWT_SECRET=GENERATE_A_SECURE_32_CHARACTER_SECRET_KEY_HERE
JWT_EXPIRATION=86400000  # 24 hours in milliseconds

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Frontend Configuration
FRONTEND_URL=http://localhost:4200  # Update for production

# Logging
LOG_LEVEL=INFO  # Use DEBUG for development
```

## Email Configuration

### Gmail Setup

1. **Enable 2-Factor Authentication** on your Gmail account
2. **Generate an App Password**:
   - Go to Google Account Settings > Security
   - Select "2-Step Verification"
   - At the bottom, select "App passwords"
   - Generate a new app password for "Mail"
   - Copy the 16-character password

3. **Update `.env` file**:
```env
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-16-char-app-password
```

### Other Email Providers

For other SMTP providers, update:
```env
MAIL_HOST=smtp.your-provider.com
MAIL_PORT=587  # or 465 for SSL
```

## Docker Deployment

### Production Deployment

1. **Build and start all services**:
```bash
docker-compose up -d --build
```

2. **Check service health**:
```bash
docker-compose ps
docker-compose logs backend
docker-compose logs db
docker-compose logs llm
```

3. **View logs**:
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
```

4. **Stop services**:
```bash
docker-compose down
```

5. **Stop and remove volumes** (⚠️ This deletes all data):
```bash
docker-compose down -v
```

### Service URLs

- **Backend API**: http://localhost:8080
- **API Documentation (Swagger)**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **LLM Server**: http://localhost:8000
- **Database**: localhost:5432
- **Frontend** (dev): http://localhost:4200

## Development Setup

### Backend (Spring Boot)

```bash
cd backend

# Install dependencies
./mvnw clean install

# Run application
./mvnw spring-boot:run

# Or with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend (Angular)

```bash
cd frontend/texttosql

# Install dependencies
npm install

# Run development server
npm start

# Build for production
npm run build

# Generate API client from OpenAPI spec
npm run open-api-gen
```

### Database Migrations

The application uses JPA/Hibernate for database management. In production:

1. Set `HIBERNATE_DDL_AUTO=validate` in `.env`
2. Use a proper migration tool like Flyway or Liquibase
3. Never use `ddl-auto=create` or `ddl-auto=create-drop` in production

## Security Best Practices

### 1. JWT Secret

Generate a secure JWT secret:
```bash
# Using OpenSSL
openssl rand -hex 32

# Using Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

### 2. Database Password

Use a strong password:
```bash
# Generate random password
openssl rand -base64 32
```

### 3. HTTPS in Production

- Always use HTTPS in production
- Update `FRONTEND_URL` to use HTTPS
- Configure SSL certificates (Let's Encrypt recommended)

### 4. Environment Variables

- Never commit `.env` file to version control
- Use Docker secrets or a secrets manager in production
- Rotate credentials regularly

### 5. Rate Limiting

The application includes rate limiting:
- Default: 20 requests per 60 seconds
- Configure in `application.yml`:
```yaml
rate-limit:
  enabled: true
  limit-for-period: 20
  period-in-seconds: 60
```

## API Documentation

### Authentication Endpoints

#### Register
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "securePassword123"
}
```

#### Verify Email
```http
POST /api/v1/auth/verify-email?token=VERIFICATION_TOKEN
```

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "securePassword123"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

#### Forgot Password
```http
POST /api/v1/auth/forgot-password
Content-Type: application/json

{
  "email": "john@example.com"
}
```

#### Reset Password
```http
POST /api/v1/auth/reset-password
Content-Type: application/json

{
  "token": "RESET_TOKEN",
  "newPassword": "newSecurePassword123"
}
```

### Protected Endpoints

All other endpoints require authentication:
```http
Authorization: Bearer YOUR_JWT_TOKEN
```

## Features

### ✅ Implemented Features

1. **Email Verification**
   - Email verification required on registration
   - Verification links expire after 24 hours
   - Resend verification option

2. **Password Reset**
   - Forgot password functionality
   - Secure reset tokens (1-hour expiration)
   - Email notifications

3. **Production-Ready Security**
   - JWT-based authentication
   - BCrypt password hashing
   - Rate limiting
   - XSS protection
   - CORS configuration
   - Non-root Docker containers

4. **Database Management**
   - PostgreSQL with connection pooling
   - Proper indexing
   - Health checks

5. **Monitoring**
   - Spring Boot Actuator
   - Health endpoints
   - Docker health checks
   - Structured logging

6. **Docker Optimization**
   - Multi-stage builds
   - Minimal base images (Alpine)
   - Layer caching
   - Resource limits
   - Health checks

## Troubleshooting

### Email Not Sending

1. Check SMTP credentials in `.env`
2. Verify Gmail app password (not regular password)
3. Check backend logs: `docker-compose logs backend`

### Database Connection Issues

1. Check if PostgreSQL is running: `docker-compose ps db`
2. Verify database credentials in `.env`
3. Check logs: `docker-compose logs db`

### Port Already in Use

```bash
# Check what's using the port
netstat -ano | findstr :8080  # Windows
lsof -i :8080                  # Linux/Mac

# Change port in .env
BACKEND_PORT=8081
```

### Container Won't Start

```bash
# Remove all containers and volumes
docker-compose down -v

# Rebuild without cache
docker-compose build --no-cache

# Start again
docker-compose up -d
```

## Maintenance

### Backup Database

```bash
# Backup
docker exec texttosql-postgres pg_dump -U postgres texttosql > backup.sql

# Restore
docker exec -i texttosql-postgres psql -U postgres texttosql < backup.sql
```

### Update Application

```bash
# Pull latest changes
git pull origin main

# Rebuild and restart
docker-compose down
docker-compose up -d --build
```

### View Logs

```bash
# Backend logs
docker-compose logs -f backend

# All logs
docker-compose logs -f

# Last 100 lines
docker-compose logs --tail=100 backend
```

## Support

For issues and questions:
- Check the API documentation: http://localhost:8080/swagger-ui.html
- Review application logs
- Check Docker container health: `docker-compose ps`
