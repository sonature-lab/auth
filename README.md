# Sonature Auth

OAuth2.1 / JWT-based **Authentication and Authorization service**.  
This project provides a centralized identity and access management solution for the Sonature microservices architecture.

---

## ✨ Features
- **OAuth2.1 Authorization Server**
- **JWT Access / Refresh Token issuance**
- **User Registration / Login API**
- **Role-Based Access Control (RBAC)**
- Built with **Spring Boot 3 + Kotlin + Gradle**

---

## 📂 Project Structure
auth/
├─ src/main/kotlin/com/sonature/auth # Core application code
├─ src/main/resources # Application configs
├─ build.gradle # Gradle build file
└─ settings.gradle # Project settingss

---

## 🚀 Getting Started

### 1. Requirements
- JDK 17+
- Gradle 8+
- (Optional) Docker

### 2. Run locally
```bash
./gradlew bootRun

📌 API Documentation
Swagger UI is available at:
http://localhost:8080/swagger-ui/index.html
OpenAPI specification (openapi.json) can be exported for external documentation tools if needed.

📌 API Overview
Endpoint	Method	Description
/api/auth/signup	POST	User registration
/api/auth/login	POST	Login & issue JWT tokens
/api/auth/refresh	POST	Issue new access token with refresh
/api/auth/logout	POST	User logout

🔒 Security
Password hashing with BCrypt
JWT signing with RSA private/public key pair
HTTPS strongly recommended in production
📜 License
This project is licensed under the MIT License.

---
