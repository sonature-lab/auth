# Sonature Auth

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![JDK 21](https://img.shields.io/badge/JDK-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg)](https://kotlinlang.org/)

[English](README.md)

프로덕션 레디 JWT/PASETO 토큰 프레임워크 + OAuth2 Authorization Server. Sonature 생태계의 공통 인증 기반입니다.

### 핵심 기능 (v0.2.0)

- **JWT 발급/검증** (HS256, RS256)
- **PASETO v4 지원** (local, public)
- **OAuth2 Authorization Server** (Authorization Code + PKCE)
- **사용자 인증** (회원가입, 로그인)
- **소셜 로그인** (Google, GitHub, Kakao)
- **OIDC 지원** (Discovery, JWK Set)
- **Consent UI** (커스텀 scope 관리)
- **Refresh Token Rotation**
- **Rate Limiting**
- **149개 테스트** (단위 + 통합)
- **TypeScript SDK** 기본 제공

---

## 빠른 시작

### Docker (권장)

```bash
# 1. 저장소 클론
git clone https://github.com/sonature-lab/auth.git
cd auth

# 2. Docker로 실행
docker-compose up -d

# 3. 확인
curl http://localhost:8080/health
```

### 로컬 개발

```bash
# 1. 요구사항
# - JDK 21+
# - Gradle 8+

# 2. 키 생성
chmod +x scripts/generate-keys.sh
./scripts/generate-keys.sh

# 3. 환경변수 설정
export JWT_HS256_SECRET=$(openssl rand -base64 32)
export API_KEYS=sk_test_development

# 4. 실행
./gradlew bootRun

# 5. Swagger UI
open http://localhost:8080/swagger-ui/index.html
```

---

## API 개요

### 인증

| Endpoint | Method | 설명 |
|----------|--------|------|
| `/api/v1/auth/signup` | POST | 회원가입 (이메일 + 비밀번호) |
| `/api/v1/auth/login` | POST | 로그인 후 JWT 토큰 쌍 발급 |

### 토큰 API

| Endpoint | Method | 설명 |
|----------|--------|------|
| `/api/v1/jwt/issue` | POST | JWT Access 토큰 발급 |
| `/api/v1/jwt/issue-pair` | POST | JWT Access + Refresh 토큰 쌍 발급 |
| `/api/v1/jwt/verify` | POST | JWT 토큰 검증 |
| `/api/v1/jwt/refresh` | POST | JWT 토큰 갱신 (Token Rotation) |
| `/api/v1/paseto/issue` | POST | PASETO v4.local 토큰 발급 |
| `/api/v1/paseto/verify` | POST | PASETO 토큰 검증 |

### 소셜 로그인

| Endpoint | Method | 설명 |
|----------|--------|------|
| `/oauth2/authorization/google` | GET | Google 로그인 시작 |
| `/oauth2/authorization/github` | GET | GitHub 로그인 시작 |
| `/oauth2/authorization/kakao` | GET | Kakao 로그인 시작 |

### OAuth2 / OIDC

| Endpoint | Method | 설명 |
|----------|--------|------|
| `/oauth2/authorize` | GET | Authorization Code 요청 (PKCE) |
| `/oauth2/consent` | GET | Consent 화면 (scope 승인) |
| `/oauth2/token` | POST | 토큰 교환 |
| `/oauth2/jwks` | GET | JWK Set (공개키) |
| `/.well-known/openid-configuration` | GET | OIDC Discovery |

### 인프라

| Endpoint | Method | 설명 |
|----------|--------|------|
| `/health` | GET | 헬스체크 |
| `/metrics` | GET | Prometheus 메트릭 |

### 예시: 회원가입 & 로그인

```bash
# 회원가입
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "mypassword123",
    "name": "홍길동"
  }'

# 로그인
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "mypassword123"
  }'
```

**응답:**
```json
{
  "data": {
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "name": "홍길동",
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

### 예시: JWT 직접 발급

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

## 설정

### 환경변수

| 변수 | 설명 | 필수 |
|------|------|------|
| `JWT_HS256_SECRET` | HS256 시크릿 키 (32+ bytes) | Yes |
| `JWT_RS256_PRIVATE_KEY` | RS256 개인키 (PEM) | RS256 사용 시 |
| `JWT_RS256_PUBLIC_KEY` | RS256 공개키 (PEM) | RS256 사용 시 |
| `PASETO_SECRET_KEY` | PASETO v4.local 키 | PASETO 사용 시 |
| `PASETO_PRIVATE_KEY` | Ed25519 개인키 | PASETO public 사용 시 |
| `API_KEYS` | 허용된 API Key 목록 (콤마 구분) | Yes |
| `DATABASE_URL` | PostgreSQL JDBC URL | 프로덕션만 |
| `GOOGLE_CLIENT_ID` | Google OAuth2 Client ID | 소셜 로그인 시 |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 Client Secret | 소셜 로그인 시 |
| `GITHUB_CLIENT_ID` | GitHub OAuth2 Client ID | 소셜 로그인 시 |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth2 Client Secret | 소셜 로그인 시 |
| `KAKAO_CLIENT_ID` | Kakao OAuth2 Client ID | 소셜 로그인 시 |
| `KAKAO_CLIENT_SECRET` | Kakao OAuth2 Client Secret | 소셜 로그인 시 |

### 키 생성

```bash
./scripts/generate-keys.sh
```

이 스크립트는 모든 필요한 암호화 키를 생성합니다:
- HS256 Secret (HMAC-SHA256)
- RS256 Key Pair (RSA 2048)
- Ed25519 Key Pair (PASETO v4.public)
- PASETO v4.local Secret

---

## OAuth2 클라이언트

`dev` 프로파일에서 테스트 클라이언트가 자동 등록됩니다:

| Client ID | 타입 | PKCE | Redirect URIs |
|-----------|------|------|---------------|
| `sonature-dev-client` | Public (SPA/Mobile) | 필수 | `localhost:3000/callback`, `localhost:5173/callback` |
| `sonature-backend-client` | Confidential (Server) | 선택 | `localhost:8081/callback` |

### Scope

| Scope | 설명 |
|-------|------|
| `openid` | OpenID Connect 신원 확인 |
| `profile` | 사용자 프로필 정보 |
| `email` | 이메일 주소 |
| `auth:read` | 인증 정보 읽기 |
| `auth:write` | 인증 정보 수정 |

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

// 회원가입
const { user, accessToken } = await auth.signup({
  email: 'user@example.com',
  password: 'mypassword123',
  name: '홍길동'
});

// 로그인
const tokens = await auth.login({
  email: 'user@example.com',
  password: 'mypassword123'
});

// JWT 직접 발급
const { accessToken, refreshToken } = await auth.jwt.issue({
  subject: 'user-123',
  expiresIn: 900
});

// JWT 검증
const { valid, claims } = await auth.jwt.verify(accessToken);

// JWT 갱신
const newTokens = await auth.jwt.refresh(refreshToken);

// PASETO 발급
const { token } = await auth.paseto.issue({
  subject: 'user-123',
  mode: 'local'
});
```

---

## 프로젝트 구조

```
auth/
├── src/main/kotlin/com/sonature/auth/
│   ├── domain/
│   │   ├── token/                # 토큰 모델, 예외
│   │   ├── user/                 # 사용자 엔티티, 인증 예외
│   │   ├── oauth2/               # OAuth2 클라이언트 엔티티, scope 정의
│   │   └── refresh/              # Refresh 토큰 엔티티
│   ├── application/
│   │   ├── service/              # AuthService, JwtService, PasetoService
│   │   ├── usecase/              # TokenRefreshUseCase
│   │   └── port/output/          # TokenProvider, KeyManager 인터페이스
│   ├── infrastructure/
│   │   ├── config/               # Security, AuthorizationServer, UserDetails
│   │   ├── crypto/               # JWT/PASETO 프로바이더, 키 관리
│   │   └── oauth2/               # 소셜 로그인, OAuth2 사용자 서비스
│   └── api/
│       ├── v1/                   # REST 컨트롤러 (auth, jwt, paseto)
│       └── oauth2/               # ConsentController
├── src/main/resources/templates/ # Thymeleaf 템플릿 (consent)
├── src/test/kotlin/              # 149개 테스트 (단위 + 통합)
├── docs/                         # 문서
└── build.gradle
```

---

## 문서

| 문서 | 설명 |
|------|------|
| [PRD](docs/PRD.md) | 제품 요구사항 |
| [API Reference](docs/API.md) | API 상세 스펙 |
| [Architecture](docs/ARCHITECTURE.md) | 아키텍처 문서 |
| [Status](docs/STATUS.md) | 프로젝트 상태 |
| [Roadmap](docs/ROADMAP.md) | 로드맵 |
| [Progress](docs/PROGRESS.md) | 진행 로그 |

---

## 개발

### 빌드 & 테스트

```bash
# 빌드
./gradlew build

# 테스트 (149개)
./gradlew test

# 커버리지 리포트
./gradlew jacocoTestReport

# 실행
./gradlew bootRun
```

### 프로파일

- `dev` (기본): H2 인메모리 DB, Swagger 활성화, 테스트 OAuth2 클라이언트
- `prod`: PostgreSQL, Swagger 비활성화

```bash
# 프로덕션 프로파일
./gradlew bootRun --args='--spring.profiles.active=prod'
```

---

## 로드맵

| Phase | 설명 | 상태 |
|-------|------|------|
| Phase 1 | JWT/PASETO 토큰 프레임워크 | **완료** |
| Phase 2 | OAuth2 / 소셜 로그인 / Consent | **완료** |
| Phase 3 | 멀티테넌트 + RBAC | 예정 |

> Phase 4 이후 (SSO Hub, 고급 보안)는 별도 저장소에서 개발됩니다.

자세한 내용은 [ROADMAP.md](docs/ROADMAP.md)를 참고하세요.

---

## 기여

기여를 환영합니다! [CONTRIBUTING.md](CONTRIBUTING.md)를 참고해 주세요.

---

## 라이선스

이 프로젝트는 MIT 라이선스로 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참고하세요.

---

## 감사의 글

- [jjwt](https://github.com/jwtk/jjwt) - Java용 JWT 라이브러리
- [paseto4j](https://github.com/nbaars/paseto4j) - Java용 PASETO v4 라이브러리
- [Spring Boot](https://spring.io/projects/spring-boot) - 애플리케이션 프레임워크
- [Spring Authorization Server](https://spring.io/projects/spring-authorization-server) - OAuth2 Authorization Server
