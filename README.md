# QueryGen - Text-to-SQL Application

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-19-red.svg)](https://angular.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

A production-ready Text-to-SQL application powered by machine learning that converts natural language queries into SQL statements. Built with Spring Boot, Angular, and a custom-trained LLM model (Qwen2.5-Coder base).

## 🚀 Features

### Core Functionality
- 🤖 **AI-Powered Query Generation** - Convert natural language to SQL using a custom-trained dialog-aware LLM.
- 📊 **Schema Management** - Import, create, and manage database schemas, tables, and columns.
- 💬 **Dialog-Aware Chat** - Interactive chat interface with multi-turn conversation support (2-turn context).
- 🔍 **Advanced Search** - Search through your schemas and conversation history.
- 📤 **Data Export** - Export your chat history in CSV, JSON, or Markdown formats.

### Admin Panel
- 🛠️ **Template Management** - Admins can create and manage global database templates available to all users.
- 📋 **System Overview** - Management interface for system-wide schemas.

### Security & Authentication
- 🛡️ **Comprehensive Auth** - JWT-based stateless authentication with Access and Refresh tokens.
- ✅ **Email Verification** - Mandatory email verification on registration.
- 🔑 **Password Management** - Secure change password and forgot/reset password flows.
- 👤 **Profile Management** - Update user profile information or delete account.
- 🛡️ **Rate Limiting** - Protection against brute force and DDoS attacks.
- 🔒 **Security Headers** - XSS protection and secure cookie management.

### Production Features
- 🐳 **Docker Support** - Fully containerized with Docker Compose.
- 📧 **Email Service** - Automated notifications using JavaMail.
- 📊 **Monitoring** - Spring Boot Actuator for health and performance metrics.
- 🔄 **Auto-Scaling Ready** - Stateless backend architecture.
- 📝 **API Documentation** - Interactive Swagger UI (OpenAPI 3.1).
- 🔧 **Flexible Configuration** - Environment-based configuration with `.env` support.

## 📋 Prerequisites

- **Docker & Docker Compose** (v3.8+)
- **Java 17+** (for local development)
- **Node.js 18+** (for frontend development)
- **PostgreSQL 15+** (for local development)
- **Python 3.10+** (for LLM server)
- **NVIDIA Container Toolkit** (Recommended for LLM performance)

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

Edit `.env` and update the following values:

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
MAIL_USERNAME=your-gmail@gmail.com
MAIL_PASSWORD=your-gmail-app-password

# Frontend Configuration
FRONTEND_URL=http://localhost:4200
FRONTEND_PORT=4200

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
- **LLM Server API**: http://localhost:8000/docs

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
                     │ (FastAPI/Py)│
                     └─────────────┘
```

### Technology Stack

#### Backend
- **Spring Boot 3.5** - Core framework
- **Spring Security** - Auth & Security
- **Spring Data JPA** - Persistence
- **JWT (jjwt)** - Token management
- **MapStruct** - Entity-DTO mapping
- **Lombok** - Code reduction
- **Bucket4j** - Rate limiting
- **Caffeine** - L1 Caching

#### Frontend
- **Angular 19** - UI Framework
- **TypeScript** - Type safety
- **Bootstrap 5 & Icons** - Styling
- **FontAwesome** - Icons
- **RxJS** - Reactive state

#### LLM Server
- **FastAPI** - Python web framework
- **PyTorch / Transformers** - Model inference
- **SQLGlot** - SQL validation
- **Custom Model** - Qwen2.5-Coder fine-tuned for Text-to-SQL

## 📊 API Endpoints

### 1. Authentication
- `POST /api/v1/auth/register` - Register
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/refresh-token` - Refresh JWT
- `POST /api/v1/auth/verify-email` - Verify account
- `GET /api/v1/auth/me` - Current user profile
- `PUT /api/v1/auth/profile` - Update profile

### 2. Schema Management
- `POST /api/v1/schemas/import` - Import JSON schema
- `GET /api/v1/schemas/templates` - Get system templates
- `GET /api/v1/schemas/databases` - List user databases
- `POST /api/v1/schemas/databases` - Create database

### 3. Chat Bot
- `POST /api/v1/chatbot/chats` - New chat
- `POST /api/v1/chatbot/chats/{id}/messages` - Send query
- `PATCH /api/v1/chatbot/chats/{id}/messages/{msgId}/feedback` - Give feedback
- `GET /api/v1/chatbot/chats/{id}/export/{format}` - Export (csv/json/md)

### 4. Search
- `GET /api/v1/search/schema` - Search databases
- `GET /api/v1/search/chatbot` - Search chat history
- **Caffeine Cache** - Multi-layer caching with smart, role-aware eviction logic (Admin-only template clearing).
- **Advanced Logging** - AOP-based automated method tracking and performance monitoring.
- **Enhanced Rate Limiting** - Bucket4j protection optimized for modern SPAs (100 req/min).

## 🔐 Security

- **Role-Based Cache Security** - Only authorized admin actions can trigger global cache evictions.
- **SQL Validation** - LLM output is validated before being returned.
- **Optimized Rate Limiting** - Balanced protection (100 req/min) to prevent abuse while ensuring smooth UX.
- **Structured Error Tracking** - Comprehensive exception logging with full stack traces for faster debugging.
- **Stateless Session** - Fully stateless API for scalability.
## 📝 License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.

Developed as a graduation project at **Alanya Alaaddin Keykubat University**.

## 👥 Team

| Role | Name |
|------|------|
| Developer | Ramazan Bozkurt |
| Developer | Abdulkadir Sözgen |
| Advisor | Prof. Dr. Yılmaz Kemal Yüce |

---
**Made with ❤️ as a graduation project — Alanya Alaaddin Keykubat University**