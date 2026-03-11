# QueryGen - Text-to-SQL Application

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-19-red.svg)](https://angular.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

A production-ready Text-to-SQL application powered by machine learning that converts natural language queries into SQL statements. Built with Spring Boot, Angular, and a custom-trained LLM model.

## 🚀 Features

### Core Functionality
- 🤖 **AI-Powered Query Generation** - Convert natural language to SQL using custom LLM
- 📊 **Schema Management** - Import and manage database schemas
- 💬 **Interactive Chat Interface** - Conversational AI for query generation
- 🔍 **Search Functionality** - Search through schemas and conversations

### Security & Authentication
- ✅ **Email Verification** - Mandatory email verification on registration
- 🔐 **JWT Authentication** - Secure token-based authentication
- 🔑 **Password Reset** - Secure forgot password functionality
- 🛡️ **Rate Limiting** - Protection against brute force attacks
- 🔒 **XSS Protection** - Security headers and input sanitization

### Production Features
- 🐳 **Docker Support** - Fully containerized with Docker Compose
- 📧 **Email Service** - Automated email notifications
- 📊 **Health Monitoring** - Spring Boot Actuator endpoints
- 🔄 **Auto-Scaling Ready** - Stateless architecture
- 📝 **API Documentation** - Interactive Swagger UI
- 🔧 **Environment Configuration** - Flexible .env configuration

## 📋 Prerequisites

- **Docker & Docker Compose** (v3.8+)
- **Java 17+** (for local development)
- **Node.js 18+** (for frontend development)
- **PostgreSQL 15+** (for local development)
- **Python 3.9+** (for LLM server)

## 🏃 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/ramazanbozkurt-dev/text-to-sql
cd text-to-sql
```

### 2. Configure Environment

```bash
cp .env.example .env
```

Edit `.env` and update the following required values:

```env
# Database Configuration
POSTGRES_URL=jdbc:postgresql://db:5432/texttosql
POSTGRES_USER=postgres
POSTGRES_PASSWORD=CHANGE_THIS_TO_SECURE_PASSWORD

# Backend Configuration
BACKEND_PORT=8080
HIBERNATE_DDL_AUTO=update

# JWT Configuration
JWT_SECRET=GENERATE_A_SECURE_32_CHARACTER_SECRET_KEY_HERE
JWT_EXPIRATION=86400000

# LLM Configuration
LLM_URL=http://llm:8000

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-gmail@gmail.com
MAIL_PASSWORD=your-gmail-app-password  # Use a Gmail App Password, not your regular password

# Frontend Configuration
FRONTEND_URL=http://localhost:4200

# Logging
LOG_LEVEL=INFO
```

### 3. Start with Docker

```bash
docker-compose up -d --build
```

### 4. Access the Application

- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

## 📚 Documentation

- **[Setup Guide](SETUP.md)** - Detailed setup and configuration
- **[API Documentation](http://localhost:8080/swagger-ui.html)** - Interactive API docs (when running)
- **[Architecture](#architecture)** - System architecture overview

## 🏗️ Architecture

### System Overview

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Angular   │─────▶│  Spring Boot │─────▶│ PostgreSQL  │
│  Frontend   │◀─────│   Backend    │◀─────│  Database   │
└─────────────┘      └──────────────┘      └─────────────┘
                            │
                            │
                            ▼
                     ┌─────────────┐
                     │  LLM Server │
                     │   (Python)  │
                     └─────────────┘
```

### Technology Stack

#### Backend
- **Spring Boot 3.5** - Enterprise Java framework
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Database access
- **JWT** - Token-based authentication
- **MapStruct** - DTO mapping
- **Lombok** - Boilerplate reduction
- **JavaMail** - Email service

#### Frontend
- **Angular 19** - Modern web framework
- **TypeScript** - Type-safe JavaScript
- **Bootstrap 5** - UI components
- **RxJS** - Reactive programming

#### Database
- **PostgreSQL 15** - Relational database
- **HikariCP** - Connection pooling

#### ML/AI
- **Custom LLM Model** - Text-to-SQL transformation
- **Python** - Model serving

## 🔧 Development

### Backend Development

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend Development

```bash
cd frontend/texttosql
npm install
npm start
```

### Run Tests

```bash
# Backend
cd backend
./mvnw test

# Frontend
cd frontend/texttosql
npm test
```

## 📊 API Endpoints

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/register` | Register new user | No |
| POST | `/api/v1/auth/login` | Login user | No |
| POST | `/api/v1/auth/verify-email` | Verify email | No |
| POST | `/api/v1/auth/resend-verification` | Resend verification email | No |
| POST | `/api/v1/auth/forgot-password` | Request password reset | No |
| POST | `/api/v1/auth/reset-password` | Reset password | No |
| GET | `/api/v1/auth/me` | Get current user | Yes |

### Schema Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/schemas/import` | Import database schema | Yes |
| GET | `/api/v1/schemas/databases` | List all schemas | Yes |
| GET | `/api/v1/schemas/databases/{id}` | Get schema details | Yes |
| POST | `/api/v1/schemas/databases` | Create new schema | Yes |

### Chat

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/chatbot/chats` | Create new chat | Yes |
| GET | `/api/v1/chatbot/chats` | List all chats | Yes |
| POST | `/api/v1/chatbot/chats/{id}/messages` | Send message | Yes |
| GET | `/api/v1/chatbot/chats/{id}/export/csv` | Export chat as CSV | Yes |
| GET | `/api/v1/chatbot/chats/{id}/export/json` | Export chat as JSON | Yes |
| GET | `/api/v1/chatbot/chats/{id}/export/markdown` | Export chat as Markdown | Yes |

For complete API documentation, visit [Swagger UI](http://localhost:8080/swagger-ui.html) when the application is running.

## 🔐 Security Features

### Implemented Security Measures

1. **Authentication**
   - JWT-based stateless authentication
   - BCrypt password hashing (strength 10)
   - Email verification required
   - Secure password reset flow

2. **Authorization**
   - Role-based access control (RBAC)
   - Method-level security
   - Protected endpoints

3. **API Security**
   - Rate limiting (20 req/min per IP)
   - CORS configuration
   - XSS protection headers
   - Content Security Policy

4. **Database Security**
   - Connection pooling with HikariCP
   - Prepared statements (SQL injection prevention)
   - Encrypted connections

5. **Docker Security**
   - Non-root users in containers
   - Minimal base images (Alpine)
   - Security scanning ready

## 📈 Performance

### Optimization Features

- **LLM Response Caching** - Caffeine cache with 30-minute TTL (10-20x faster for repeated queries)
- **Retry Logic** - Automatic retry with exponential backoff (up to 3 attempts)
- **Database Connection Pooling** - HikariCP with optimized settings
- **Multi-stage Docker Builds** - Reduced image sizes (~50% smaller)
- **JVM Tuning** - G1GC with optimized heap settings
- **Lazy Loading** - Efficient entity relationships

## 🐳 Docker Configuration

### Container Resources

| Service | CPU Limit | Memory Limit |
|---------|-----------|--------------|
| PostgreSQL | 1 core | 512 MB |
| Backend | 1 core | 1 GB |
| LLM Server | 2 cores | 2 GB |

### Health Checks

All services include health checks:
- **Database**: PostgreSQL ready check
- **Backend**: Actuator health endpoint
- **LLM**: Custom health endpoint

## 🚀 Deployment

### Production Checklist

- [ ] Update `JWT_SECRET` with secure random value (32+ characters)
- [ ] Change default `POSTGRES_PASSWORD`
- [ ] Configure email SMTP settings (use Gmail App Password)
- [ ] Set `HIBERNATE_DDL_AUTO=validate`
- [ ] Set `FRONTEND_URL` to your production domain
- [ ] Enable HTTPS
- [ ] Configure proper CORS origins
- [ ] Set `LOG_LEVEL=WARN` for production
- [ ] Set up database backups
- [ ] Configure log aggregation
- [ ] Set up monitoring/alerting

### Environment Variables Reference

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://db:5432/texttosql` |
| `POSTGRES_USER` | Database username | `postgres` |
| `POSTGRES_PASSWORD` | Database password | *(required)* |
| `BACKEND_PORT` | Backend server port | `8080` |
| `HIBERNATE_DDL_AUTO` | Hibernate DDL strategy | `update` |
| `JWT_SECRET` | JWT signing secret (32+ chars) | *(required)* |
| `JWT_EXPIRATION` | JWT expiry in ms | `86400000` (24h) |
| `LLM_URL` | LLM server URL | `http://llm:8000` |
| `MAIL_HOST` | SMTP host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | SMTP username / sender email | *(required)* |
| `MAIL_PASSWORD` | Gmail App Password | *(required)* |
| `FRONTEND_URL` | Frontend base URL (for email links) | `http://localhost:4200` |
| `LOG_LEVEL` | Root logging level | `INFO` |

## 📝 License

This project is a graduation project developed at Alanya Alaaddin Keykubat University.

## 👥 Team

| Role | Name                        |
|------|-----------------------------|
| Developer | Ramazan Bozkurt             |
| Developer | Abdulkadir Sözgen           |
| Advisor | Prof. Dr. Yılmaz Kemal Yüce |

## 🙏 Acknowledgments

- Built with Spring Boot and Angular
- LLM model custom-trained for Text-to-SQL conversion
- PostgreSQL for reliable data storage

---

**Made with ❤️ as a graduation project — Alanya Alaaddin Keykubat University**