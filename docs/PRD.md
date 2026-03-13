# Sonature Auth - Product Requirements Document

## 1. 개요

### 1.1 프로젝트 정의
JWT/PASETO 기반 토큰 발급/검증 프레임워크. 오픈소스로 공개 예정.

### 1.2 목표
- 2주 내 MVP 완성
- 표준 준수 (RFC 7519 JWT, PASETO v4)
- 프로덕션 레디 품질
- TypeScript SDK 기본 제공

### 1.3 비목표 (MVP 제외)
- 사용자 관리 (회원가입/로그인)
- OAuth2.1 Authorization Server
- RBAC (Role-Based Access Control)
- JWE (암호화된 JWT)
- HTTP/3

---

## 2. 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Kotlin 1.9 |
| Runtime | JDK 21 (Virtual Threads) |
| Framework | Spring Boot 3.5 |
| Protocol | HTTP/2 |
| Database | PostgreSQL (prod) / H2 (dev) |
| Container | Docker |
| Monitoring | Prometheus + Grafana |
| Deploy | OCI Free Tier (ARM 4 OCPU, 24GB) |
| Client SDK | TypeScript |

---

## 3. 기능 요구사항

### 3.1 JWT 지원

| 기능 | 설명 |
|------|------|
| 발급 | Access Token, Refresh Token 생성 |
| 검증 | 서명 검증, 만료 확인, 클레임 검증 |
| 알고리즘 | HS256 (HMAC-SHA256), RS256 (RSA-SHA256) |

**JWT 클레임 (RFC 7519)**
```json
{
  "iss": "sonature-auth",
  "sub": "user-id",
  "aud": "client-id",
  "exp": 1234567890,
  "iat": 1234567890,
  "jti": "unique-token-id"
}
```

### 3.2 PASETO 지원

| 기능 | 설명 |
|------|------|
| 버전 | v4 (local, public) |
| local | 대칭키 암호화 (XChaCha20-Poly1305) |
| public | 비대칭키 서명 (Ed25519) |

### 3.3 API 엔드포인트

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/jwt/issue` | POST | JWT 토큰 발급 |
| `/api/v1/jwt/verify` | POST | JWT 토큰 검증 |
| `/api/v1/jwt/refresh` | POST | JWT 토큰 갱신 |
| `/api/v1/paseto/issue` | POST | PASETO 토큰 발급 |
| `/api/v1/paseto/verify` | POST | PASETO 토큰 검증 |
| `/health` | GET | 헬스체크 |
| `/metrics` | GET | Prometheus 메트릭 |

### 3.4 TypeScript SDK

```typescript
import { SonatureAuth } from '@sonature/auth-sdk';

const auth = new SonatureAuth({
  baseUrl: 'https://auth.sonature.io',
  apiKey: 'your-api-key'
});

// JWT 발급
const token = await auth.jwt.issue({
  subject: 'user-123',
  audience: 'my-app',
  expiresIn: '15m'
});

// JWT 검증
const payload = await auth.jwt.verify(token);
```

---

## 4. 비기능 요구사항

### 4.1 성능
- 토큰 발급: < 10ms (p99)
- 토큰 검증: < 5ms (p99)
- 동시 연결: 1,000+ (Virtual Threads 활용)

### 4.2 보안
- 키는 환경변수 또는 시크릿 매니저로 관리
- Rate Limiting (IP 기반)
- API Key 인증
- TLS 1.3 필수 (프로덕션)

### 4.3 가용성
- 99.9% uptime 목표
- 무중단 배포 (Rolling Update)

### 4.4 모니터링
- Prometheus 메트릭 노출
- Grafana 대시보드
- 주요 메트릭: 요청 수, 지연시간, 에러율

---

## 5. 토큰 정책

### 5.1 만료 시간

| 토큰 타입 | 기본값 | 최소 | 최대 | 설정 가능 |
|----------|--------|------|------|-----------|
| Access Token | 15분 | 1분 | 1시간 | Yes |
| Refresh Token | 7일 | 1시간 | 30일 | Yes |
| PASETO (local) | 15분 | 1분 | 1시간 | Yes |
| PASETO (public) | 15분 | 1분 | 1시간 | Yes |

### 5.2 Refresh Token 정책
- 단일 사용 (One-Time Use): Refresh 시 새 Refresh Token 발급
- 이전 Refresh Token은 즉시 무효화
- Refresh Token Rotation으로 탈취 감지

### 5.3 Refresh Token 저장

**MVP: PostgreSQL (H2 for dev)**

단일 인스턴스 MVP에서는 RDB로 충분. 향후 Redis 전환 고려.

**스키마**

```sql
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY,
    token_hash      VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256 해시
    subject         VARCHAR(255) NOT NULL,
    client_id       VARCHAR(255),
    issued_at       TIMESTAMP NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    revoked_at      TIMESTAMP,                     -- NULL이면 유효
    replaced_by     UUID REFERENCES refresh_tokens(id),  -- Rotation 추적
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_subject ON refresh_tokens(subject);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
```

**무효화 구현**
1. Refresh 요청 시 `token_hash`로 조회
2. `revoked_at`이 NULL이고 `expires_at` > now인지 확인
3. 새 토큰 발급 후 기존 토큰의 `revoked_at` = now, `replaced_by` = 새 토큰 ID
4. 이미 revoked된 토큰으로 요청 시 → 탈취 감지 → 해당 subject의 모든 토큰 무효화

**향후 (MVP 이후)**
- Redis로 전환 (TTL 자동 만료, 더 빠른 조회)
- 토큰 family 개념 도입 (같은 세션에서 발급된 토큰 그룹 관리)

---

## 6. API Key 관리

### 6.1 MVP: 환경변수

```bash
# 콤마로 구분된 API Key 목록
API_KEYS=sk_live_abc123,sk_live_def456,sk_test_xyz789
```

검증 로직:
```kotlin
fun validateApiKey(key: String): Boolean {
    return apiKeys.contains(key)
}
```

### 6.2 API Key 형식

```
sk_{env}_{random}

- sk: 접두사 (secret key)
- env: live | test
- random: 24자 랜덤 문자열 (base62)
```

예시: `sk_live_a1b2c3d4e5f6g7h8i9j0k1l2`

### 6.3 향후 (MVP 이후): DB 관리

```sql
CREATE TABLE api_keys (
    id              UUID PRIMARY KEY,
    key_hash        VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256
    key_prefix      VARCHAR(20) NOT NULL,         -- sk_live_a1b... (조회용)
    name            VARCHAR(255),
    environment     VARCHAR(10) NOT NULL,         -- live | test
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at    TIMESTAMP,
    revoked_at      TIMESTAMP,
    rate_limit      INT DEFAULT 100               -- 커스텀 Rate Limit
);
```

향후 기능:
- API Key별 Rate Limit 설정
- 사용량 통계
- Key Rotation
- 권한 범위 (scope) 지정

---

## 7. 키 생성

### 7.1 MVP: 수동 생성 + 스크립트 제공

서명 키는 운영자가 직접 생성. 프로젝트에 생성 스크립트 포함.

### 7.2 키 생성 스크립트

```bash
#!/bin/bash
# scripts/generate-keys.sh

echo "=== HS256 Secret ==="
openssl rand -base64 32

echo ""
echo "=== RS256 Key Pair ==="
openssl genrsa -out rs256-private.pem 2048
openssl rsa -in rs256-private.pem -pubout -out rs256-public.pem
echo "Private: rs256-private.pem"
echo "Public: rs256-public.pem"

echo ""
echo "=== Ed25519 Key Pair (PASETO v4.public) ==="
openssl genpkey -algorithm Ed25519 -out ed25519-private.pem
openssl pkey -in ed25519-private.pem -pubout -out ed25519-public.pem
echo "Private: ed25519-private.pem"
echo "Public: ed25519-public.pem"

echo ""
echo "=== PASETO v4.local Secret (32 bytes) ==="
openssl rand -base64 32
```

### 7.3 키 요구사항

| 알고리즘 | 키 타입 | 최소 크기 |
|----------|---------|----------|
| HS256 | Symmetric | 32 bytes (256 bits) |
| RS256 | RSA | 2048 bits |
| Ed25519 | EdDSA | 32 bytes (고정) |
| XChaCha20 | Symmetric | 32 bytes (고정) |

### 7.4 환경변수 설정 예시

```bash
# PEM 파일 내용을 환경변수로 (줄바꿈은 \n으로)
export JWT_RS256_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\nMIIE...\n-----END PRIVATE KEY-----"

# 또는 파일 경로 지정 (향후 지원)
export JWT_RS256_PRIVATE_KEY_FILE=/secrets/rs256-private.pem
```

### 7.5 향후 (MVP 이후)

- `/api/v1/keys/generate` 엔드포인트 제공
- 키 로테이션 API
- 키 버전 관리 (kid claim 활용)

---

## 8. Rate Limiting

### 8.1 기본 제한

| 엔드포인트 | 제한 | 윈도우 |
|-----------|------|--------|
| `/api/v1/jwt/issue` | 100 req | 1분 |
| `/api/v1/jwt/verify` | 1,000 req | 1분 |
| `/api/v1/jwt/refresh` | 30 req | 1분 |
| `/api/v1/paseto/*` | 100 req | 1분 |
| `/health` | 제한 없음 | - |
| `/metrics` | 60 req | 1분 |

### 8.2 Rate Limit 응답 헤더

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1234567890
```

### 8.3 제한 초과 시
- HTTP 429 Too Many Requests
- `Retry-After` 헤더 포함

---

## 9. 에러 처리

### 9.1 에러 응답 형식

```json
{
  "error": {
    "code": "TOKEN_EXPIRED",
    "message": "The token has expired",
    "details": {
      "expiredAt": "2026-01-28T12:00:00Z"
    }
  },
  "requestId": "req_abc123",
  "timestamp": "2026-01-28T12:05:00Z"
}
```

### 9.2 에러 코드 체계

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `INVALID_REQUEST` | 400 | 잘못된 요청 형식 |
| `MISSING_PARAMETER` | 400 | 필수 파라미터 누락 |
| `INVALID_ALGORITHM` | 400 | 지원하지 않는 알고리즘 |
| `UNAUTHORIZED` | 401 | API Key 누락 또는 무효 |
| `INVALID_API_KEY` | 401 | 유효하지 않은 API Key |
| `TOKEN_EXPIRED` | 401 | 토큰 만료 |
| `TOKEN_INVALID` | 401 | 토큰 서명 검증 실패 |
| `TOKEN_MALFORMED` | 401 | 토큰 형식 오류 |
| `RATE_LIMITED` | 429 | Rate Limit 초과 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |
| `SERVICE_UNAVAILABLE` | 503 | 서비스 일시 중단 |

### 9.3 성공 응답 형식

```json
{
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "expiresIn": 900,
    "tokenType": "Bearer"
  },
  "requestId": "req_abc123"
}
```

---

## 10. 로깅 전략

### 10.1 로그 레벨

| 레벨 | 용도 |
|------|------|
| ERROR | 예외, 시스템 오류 |
| WARN | Rate Limit 초과, 잘못된 토큰 검증 시도 |
| INFO | 요청/응답 요약, 서버 시작/종료 |
| DEBUG | 상세 요청 정보 (개발 환경만) |

### 10.2 로그 포맷 (JSON)

```json
{
  "timestamp": "2026-01-28T12:00:00.000Z",
  "level": "INFO",
  "logger": "com.sonature.auth.api.JwtController",
  "message": "Token issued",
  "requestId": "req_abc123",
  "clientIp": "192.168.1.1",
  "endpoint": "/api/v1/jwt/issue",
  "method": "POST",
  "statusCode": 200,
  "durationMs": 5
}
```

### 10.3 민감 정보 마스킹
- API Key: `sk_...abc` (앞 3자, 뒤 3자만 표시)
- 토큰: 로그에 기록하지 않음
- IP 주소: 프로덕션에서 해시 처리 옵션

---

## 11. 테스트 전략

### 11.1 커버리지 목표

| 유형 | 목표 | 범위 |
|------|------|------|
| Unit Test | 80%+ | Service, Crypto 레이어 |
| Integration Test | 70%+ | API 엔드포인트 |
| E2E Test | 핵심 플로우 | 토큰 발급 → 검증 → 갱신 |

### 11.2 테스트 범위

**Unit Test**
- JWT/PASETO 발급/검증 로직
- 에러 핸들링
- Rate Limiting 로직

**Integration Test**
- 전체 API 엔드포인트
- 인증 (API Key)
- 에러 응답 형식

**E2E Test**
- JWT 전체 플로우: issue → verify → refresh
- PASETO 전체 플로우: issue → verify
- Rate Limiting 동작 확인

### 11.3 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 단위 테스트만
./gradlew test --tests "*UnitTest"

# 통합 테스트만
./gradlew test --tests "*IntegrationTest"

# 커버리지 리포트
./gradlew jacocoTestReport
```

---

## 12. TypeScript SDK

### 12.1 배포

| 항목 | 값 |
|------|-----|
| 패키지명 | `@sonature/auth-sdk` |
| 레지스트리 | npm (public) |
| 라이선스 | MIT |
| 번들러 | tsup (ESM + CJS) |
| 타입 | TypeScript native |

### 12.2 지원 환경
- Node.js 18+
- 브라우저 (fetch 기반)
- Deno, Bun 호환

### 12.3 SDK 구조

```
@sonature/auth-sdk/
├── src/
│   ├── index.ts
│   ├── client.ts
│   ├── jwt.ts
│   ├── paseto.ts
│   ├── types.ts
│   └── errors.ts
├── package.json
├── tsconfig.json
└── README.md
```

### 12.4 에러 처리

```typescript
import { SonatureAuth, TokenExpiredError, RateLimitError } from '@sonature/auth-sdk';

try {
  await auth.jwt.verify(token);
} catch (e) {
  if (e instanceof TokenExpiredError) {
    // 토큰 갱신 로직
  } else if (e instanceof RateLimitError) {
    // 재시도 로직 (e.retryAfter 활용)
  }
}
```

---

## 13. 아키텍처

### 13.1 레이어 구조

```
┌─────────────────────────────────────────┐
│              API Layer                  │
│         (REST Controllers)              │
├─────────────────────────────────────────┤
│            Service Layer                │
│    (JwtService, PasetoService)          │
├─────────────────────────────────────────┤
│           Crypto Interface              │
│    (TokenProvider, KeyManager)          │
├─────────────────────────────────────────┤
│         Crypto Implementation           │
│   (HS256, RS256, Ed25519, XChaCha20)    │
└─────────────────────────────────────────┘
```

### 13.2 인터페이스 설계 (향후 분리 대비)

```kotlin
interface TokenProvider {
    fun issue(claims: TokenClaims, config: TokenConfig): String
    fun verify(token: String): TokenClaims
}

interface KeyManager {
    fun getSigningKey(algorithm: Algorithm): Key
    fun getVerificationKey(algorithm: Algorithm): Key
}
```

향후 암호화 알고리즘 서버 분리 시, `TokenProvider` 구현체만 교체.

---

## 14. 배포

### 14.1 OCI Free Tier 스펙
- ARM 4 OCPU
- 24GB RAM
- 200GB Block Storage

### 14.2 Docker

```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY build/libs/sonature-auth.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 14.3 환경변수

| Variable | Description | Required |
|----------|-------------|----------|
| `JWT_HS256_SECRET` | HS256 시크릿 키 (32+ bytes) | Yes |
| `JWT_RS256_PRIVATE_KEY` | RS256 개인키 (PEM) | Yes |
| `JWT_RS256_PUBLIC_KEY` | RS256 공개키 (PEM) | Yes |
| `PASETO_SECRET_KEY` | PASETO v4.local 키 (32 bytes) | Yes |
| `PASETO_PRIVATE_KEY` | PASETO v4.public Ed25519 개인키 | Yes |
| `DATABASE_URL` | PostgreSQL JDBC URL | Yes (prod) |
| `API_KEYS` | 허용된 API Key 목록 (콤마 구분) | Yes |
| `LOG_LEVEL` | 로그 레벨 (INFO/DEBUG) | No |
| `RATE_LIMIT_ENABLED` | Rate Limiting 활성화 | No (기본 true) |

---

## 15. 마일스톤 (2주)

### Week 1: Core
| Day | Task |
|-----|------|
| 1-2 | 프로젝트 구조, JWT HS256 발급/검증 |
| 3-4 | JWT RS256, Refresh Token |
| 5 | PASETO v4.local |

### Week 2: Production Ready
| Day | Task |
|-----|------|
| 6-7 | PASETO v4.public, API 완성, 에러 처리 |
| 8-9 | TypeScript SDK, npm 배포 |
| 10 | Docker, Prometheus/Grafana |
| 11-12 | OCI 배포, 문서화 |
| 13-14 | 테스트, 버그 수정, 릴리스 |

---

## 16. 향후 로드맵 (MVP 이후)

| 우선순위 | 기능 | 설명 |
|----------|------|------|
| P1 | JWE 지원 | 암호화된 JWT |
| P1 | HTTP/3 | QUIC 프로토콜 |
| P2 | 암호 알고리즘 서버 분리 | Private 서버로 커스텀 알고리즘 |
| P2 | 키 관리 서비스 | 키 로테이션, 버전 관리 |
| P3 | 라이선스 인증 | 상용 기능 라이선스 |

---

## 17. 참고 문서

- [RFC 7519 - JSON Web Token](https://datatracker.ietf.org/doc/html/rfc7519)
- [PASETO Specification](https://paseto.io/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
