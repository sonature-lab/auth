# Sonature Auth

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![JDK 21](https://img.shields.io/badge/JDK-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg)](https://kotlinlang.org/)

[English](README.md)

JWT/PASETO 기반 토큰 발급/검증 프레임워크.

- **JWT 발급/검증** (HS256, RS256)
- **PASETO v4 지원** (local, public)
- **Refresh Token Rotation**
- **Rate Limiting**
- **TypeScript SDK** 기본 제공

---

## 빠른 시작

### Docker (권장)

```bash
# 1. 저장소 클론
git clone https://github.com/sonature/auth.git
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

| Endpoint | Method | 설명 |
|----------|--------|------|
| `/api/v1/jwt/issue` | POST | JWT Access 토큰 발급 |
| `/api/v1/jwt/issue-pair` | POST | JWT Access + Refresh 토큰 쌍 발급 |
| `/api/v1/jwt/verify` | POST | JWT 토큰 검증 |
| `/api/v1/jwt/refresh` | POST | JWT 토큰 갱신 (Token Rotation) |
| `/api/v1/paseto/issue` | POST | PASETO v4.local 토큰 발급 |
| `/api/v1/paseto/verify` | POST | PASETO 토큰 검증 |
| `/health` | GET | 헬스체크 |
| `/metrics` | GET | Prometheus 메트릭 |

### 예시: JWT 발급

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

**응답:**
```json
{
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "requestId": "req_abc123"
}
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

// JWT 발급
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
├── src/main/kotlin/com/sonature/auth/  # 핵심 애플리케이션
│   ├── domain/                          # 비즈니스 엔티티
│   ├── application/                     # 유스케이스, 서비스
│   ├── infrastructure/                  # 암호화, 영속성
│   └── presentation/                    # REST 컨트롤러
├── src/main/resources/                  # 설정 파일
├── src/test/kotlin/                     # 테스트
├── scripts/                             # 유틸리티 스크립트
│   └── generate-keys.sh                 # 키 생성
├── docs/                                # 문서
└── build.gradle                         # 빌드 설정
```

---

## 문서

| 문서 | 설명 |
|------|------|
| [PRD](docs/PRD.md) | 제품 요구사항 |
| [API Reference](docs/API.md) | API 상세 스펙 |
| [Architecture](docs/ARCHITECTURE.md) | 아키텍처 문서 |
| [Implementation Plan](docs/IMPLEMENTATION-PLAN.md) | 구현 계획 |
| [Status](docs/STATUS.md) | 프로젝트 상태 |
| [Roadmap](docs/ROADMAP.md) | 로드맵 |
| [Progress](docs/PROGRESS.md) | 진행 로그 |

---

## 개발

### 빌드 & 테스트

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 커버리지 리포트
./gradlew jacocoTestReport
# 결과: build/reports/jacoco/test/html/index.html

# 실행
./gradlew bootRun
```

### 프로파일

- `dev` (기본): H2 인메모리 DB, Swagger 활성화
- `prod`: PostgreSQL, Swagger 비활성화

```bash
# 프로덕션 프로파일
./gradlew bootRun --args='--spring.profiles.active=prod'
```

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
