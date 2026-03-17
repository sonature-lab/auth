# Sonature Auth

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![JDK 21](https://img.shields.io/badge/JDK-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg)](https://kotlinlang.org/)

[한국어](README.ko.md)

A production-ready JWT/PASETO token framework and OAuth2 Authorization Server, designed as a shared authentication foundation for the Sonature ecosystem.

### Core Features (v0.3.0)

- **JWT Issuance & Verification** (HS256, RS256)
- **PASETO v4 Support** (local, public)
- **OAuth2 Authorization Server** (Authorization Code + PKCE)
- **User Authentication** (signup, login)
- **Social Login** (Google, GitHub, Kakao)
- **OIDC Support** (Discovery, JWK Set)
- **Consent UI** with customizable scopes
- **Multi-tenant** with Row-level Isolation
- **RBAC** (Role-Based Access Control with 4 roles, 8 permissions)
- **Refresh Token Rotation** with Pessimistic Locking
- **Rate Limiting** (Bucket4j, IP-based)
- **Tenant Slug Caching** (Caffeine)
- **298 Tests** (unit + integration)
- **TypeScript SDK** included

---

## Quick Start

### Docker (Recommended)

```bash
# 1. Clone the repository
git clone https://github.com/sonature-lab/auth.git
cd auth

# 2. Run with Docker
docker-compose up -d

# 3. Health check
curl http://localhost:8080/health
```

### Local Development

```bash
# 1. Prerequisites
# - JDK 21+
# - Gradle 8+

# 2. Generate keys
chmod +x scripts/generate-keys.sh
./scripts/generate-keys.sh

# 3. Set environment variables
export JWT_HS256_SECRET=$(openssl rand -base64 32)
export API_KEYS=sk_test_development

# 4. Run
./gradlew bootRun

# 5. Swagger UI
open http://localhost:8080/swagger-ui/index.html
```

---

## API Overview

### Authentication

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/auth/signup` | POST | Create account (email + password) |
| `/api/v1/auth/login` | POST | Login and receive JWT token pair |

### Token API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/jwt/issue` | POST | Issue JWT access token |
| `/api/v1/jwt/issue-pair` | POST | Issue JWT access + refresh token pair |
| `/api/v1/jwt/verify` | POST | Verify JWT token |
| `/api/v1/jwt/refresh` | POST | Refresh JWT token (Token Rotation) |
| `/api/v1/paseto/issue` | POST | Issue PASETO v4.local token |
| `/api/v1/paseto/verify` | POST | Verify PASETO token |

### Social Login

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/oauth2/authorization/google` | GET | Start Google login |
| `/oauth2/authorization/github` | GET | Start GitHub login |
| `/oauth2/authorization/kakao` | GET | Start Kakao login |

### OAuth2 / OIDC

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/oauth2/authorize` | GET | Authorization Code request (PKCE) |
| `/oauth2/consent` | GET | Consent screen (scope approval) |
| `/oauth2/token` | POST | Token exchange |
| `/oauth2/jwks` | GET | JWK Set (public keys) |
| `/.well-known/openid-configuration` | GET | OIDC Discovery |

### Infrastructure

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |
| `/metrics` | GET | Prometheus metrics |

### Multi-tenant

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/tenants` | POST | Create tenant |
| `/api/v1/tenants/{slug}` | GET | Get tenant by slug |
| `/api/v1/tenants/{slug}/members` | POST | Add member |
| `/api/v1/tenants/{slug}/members` | GET | List members |
| `/api/v1/tenants/{slug}/members/{userId}` | DELETE | Remove member |
| `/api/v1/tenants/{slug}/members/{userId}/role` | PUT | Change member role |

### Example: Signup & Login

```bash
# Signup
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "mypassword123",
    "name": "John Doe"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "mypassword123"
  }'
```

**Response:**
```json
{
  "data": {
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "name": "John Doe",
      "provider": "LOCAL"
    },
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "accessExpiresIn": 900,
    "refreshExpiresIn": 604800
  },
  "requestId": "req_abc123"
}
```

### Example: Issue JWT (Direct API)

```bash
curl -X POST http://localhost:8080/api/v1/jwt/issue \
  -H "X-API-Key: sk_test_development" \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "user-123",
    "algorithm": "HS256",
    "expiresIn": 900
  }'
```

---

## Configuration

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `JWT_HS256_SECRET` | HS256 secret key (32+ bytes) | Yes |
| `JWT_RS256_PRIVATE_KEY` | RS256 private key (PEM) | For RS256 |
| `JWT_RS256_PUBLIC_KEY` | RS256 public key (PEM) | For RS256 |
| `PASETO_SECRET_KEY` | PASETO v4.local key | For PASETO |
| `PASETO_PRIVATE_KEY` | Ed25519 private key | For PASETO public |
| `API_KEYS` | Allowed API keys (comma-separated) | Yes |
| `AUTH_JWK_PRIVATE_KEY` | JWK RSA private key (PEM) | Prod only |
| `AUTH_JWK_PUBLIC_KEY` | JWK RSA public key (PEM) | Prod only |
| `AUTH_OAUTH2_ISSUER` | OAuth2 issuer URL | Prod only |
| `DATABASE_URL` | PostgreSQL JDBC URL | Prod only |
| `GOOGLE_CLIENT_ID` | Google OAuth2 Client ID | For Social Login |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 Client Secret | For Social Login |
| `GITHUB_CLIENT_ID` | GitHub OAuth2 Client ID | For Social Login |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth2 Client Secret | For Social Login |
| `KAKAO_CLIENT_ID` | Kakao OAuth2 Client ID | For Social Login |
| `KAKAO_CLIENT_SECRET` | Kakao OAuth2 Client Secret | For Social Login |

### Key Generation

```bash
./scripts/generate-keys.sh
```

This script generates all required cryptographic keys:
- HS256 Secret (HMAC-SHA256)
- RS256 Key Pair (RSA 2048)
- Ed25519 Key Pair (PASETO v4.public)
- PASETO v4.local Secret

---

## OAuth2 Clients

In `dev` profile, two test clients are auto-registered:

| Client ID | Type | PKCE | Redirect URIs |
|-----------|------|------|---------------|
| `sonature-dev-client` | Public (SPA/Mobile) | Required | `localhost:3000/callback`, `localhost:5173/callback` |
| `sonature-backend-client` | Confidential (Server) | Optional | `localhost:8081/callback` |

### Scopes

| Scope | Description |
|-------|-------------|
| `openid` | OpenID Connect identity |
| `profile` | User profile information |
| `email` | Email address |
| `auth:read` | Read authentication info |
| `auth:write` | Modify authentication info |

---

## TypeScript SDK

```bash
npm install @sonature/auth-sdk
```

```typescript
import { SonatureAuth } from '@sonature/auth-sdk';

const auth = new SonatureAuth({
  baseUrl: 'https://auth.sonature.io',
  apiKey: 'sk_live_your_api_key'
});

// Signup
const { user, accessToken } = await auth.signup({
  email: 'user@example.com',
  password: 'mypassword123',
  name: 'John Doe'
});

// Login
const tokens = await auth.login({
  email: 'user@example.com',
  password: 'mypassword123'
});

// Issue JWT (direct)
const { accessToken, refreshToken } = await auth.jwt.issue({
  subject: 'user-123',
  expiresIn: 900
});

// Verify JWT
const { valid, claims } = await auth.jwt.verify(accessToken);

// Refresh JWT
const newTokens = await auth.jwt.refresh(refreshToken);

// Issue PASETO
const { token } = await auth.paseto.issue({
  subject: 'user-123',
  mode: 'local'
});
```

---

## Project Structure

```
auth/
├── src/main/kotlin/com/sonature/auth/
│   ├── domain/
│   │   ├── token/                # Token models, exceptions
│   │   ├── user/                 # User entity, auth exceptions
│   │   ├── oauth2/               # OAuth2 client entity, scope definitions
│   │   └── refresh/              # Refresh token entity
│   ├── application/
│   │   ├── service/              # AuthService, JwtService, PasetoService
│   │   ├── usecase/              # TokenRefreshUseCase
│   │   └── port/output/          # TokenProvider, KeyManager interfaces
│   ├── infrastructure/
│   │   ├── config/               # Security, AuthorizationServer, Cache
│   │   ├── crypto/               # JWT/PASETO providers, key management
│   │   ├── oauth2/               # Social login, OAuth2 user service
│   │   ├── security/             # Filters (RateLimit, TenantContext, JwtBearer)
│   │   └── scheduler/            # Token cleanup scheduler
│   └── api/
│       ├── v1/                   # REST controllers (auth, jwt, paseto, tenants)
│       └── oauth2/               # ConsentController
├── src/main/resources/templates/ # Thymeleaf templates (consent)
├── src/test/kotlin/              # 298 tests (unit + integration)
├── docs/                         # Documentation
└── build.gradle
```

---

## Documentation

| Document | Description |
|----------|-------------|
| [PRD](docs/PRD.md) | Product Requirements |
| [API Reference](docs/API.md) | API Specification |
| [Architecture](docs/ARCHITECTURE.md) | Architecture Document |
| [Status](docs/STATUS.md) | Project Status |
| [Roadmap](docs/ROADMAP.md) | Roadmap |
| [Progress](docs/PROGRESS.md) | Progress Log |

---

## Development

### Build & Test

```bash
# Build
./gradlew build

# Test (298 tests)
./gradlew test

# Coverage report
./gradlew jacocoTestReport

# Run
./gradlew bootRun
```

### Profiles

- `dev` (default): H2 in-memory DB, Swagger enabled, test OAuth2 clients
- `prod`: PostgreSQL, Swagger disabled

```bash
# Production profile
./gradlew bootRun --args='--spring.profiles.active=prod'
```

---

## Roadmap

| Phase | Description | Status |
|-------|-------------|--------|
| Phase 1 | JWT/PASETO Token Framework | **Done** |
| Phase 2 | OAuth2 / Social Login / Consent | **Done** |
| Phase 3 | Multi-tenant + RBAC | **Done** |

> Phase 4+ (SSO Hub, Advanced Security) is developed in a separate private repository.

See [ROADMAP.md](docs/ROADMAP.md) for details.

---

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- [jjwt](https://github.com/jwtk/jjwt) - JWT library for Java
- [paseto4j](https://github.com/nbaars/paseto4j) - PASETO v4 library for Java
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [Spring Authorization Server](https://spring.io/projects/spring-authorization-server) - OAuth2 Authorization Server
