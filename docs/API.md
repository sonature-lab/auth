# API Reference

> Sonature Auth REST API 문서

## Base URL

```
Development: http://localhost:8080
Production:  https://auth.sonature.io
```

## Authentication

모든 API 요청에는 `X-API-Key` 헤더가 필요합니다.

```bash
curl -H "X-API-Key: sk_live_your_api_key" https://auth.sonature.io/api/v1/jwt/issue
```

| Header | Required | Description |
|--------|----------|-------------|
| `X-API-Key` | Yes | API 인증 키 |
| `Content-Type` | Yes | `application/json` |

---

## Common Response Format

### Success Response

```json
{
  "data": {
    // Response payload
  },
  "requestId": "req_abc123def456"
}
```

### Error Response

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable message",
    "details": {}
  },
  "requestId": "req_abc123def456",
  "timestamp": "2026-01-28T12:00:00Z"
}
```

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_REQUEST` | 400 | 잘못된 요청 형식 |
| `MISSING_PARAMETER` | 400 | 필수 파라미터 누락 |
| `INVALID_ALGORITHM` | 400 | 지원하지 않는 알고리즘 |
| `UNAUTHORIZED` | 401 | API Key 누락 |
| `INVALID_API_KEY` | 401 | 유효하지 않은 API Key |
| `TOKEN_EXPIRED` | 401 | 토큰 만료 |
| `TOKEN_INVALID` | 401 | 토큰 서명 검증 실패 |
| `TOKEN_MALFORMED` | 401 | 토큰 형식 오류 |
| `RATE_LIMITED` | 429 | Rate Limit 초과 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

---

## Rate Limiting

### Limits

| Endpoint | Limit | Window |
|----------|-------|--------|
| `/api/v1/jwt/issue` | 100 req | 1 min |
| `/api/v1/jwt/verify` | 1,000 req | 1 min |
| `/api/v1/jwt/refresh` | 30 req | 1 min |
| `/api/v1/paseto/*` | 100 req | 1 min |

### Response Headers

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1706443200
```

### Rate Limit Exceeded

```json
HTTP/1.1 429 Too Many Requests
Retry-After: 30

{
  "error": {
    "code": "RATE_LIMITED",
    "message": "Rate limit exceeded. Try again in 30 seconds.",
    "details": {
      "retryAfter": 30
    }
  }
}
```

---

## JWT Endpoints

### POST /api/v1/jwt/issue

JWT 토큰을 발급합니다.

**Request**

```bash
curl -X POST https://auth.sonature.io/api/v1/jwt/issue \
  -H "X-API-Key: sk_live_your_api_key" \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "user-123",
    "audience": "my-app",
    "algorithm": "HS256",
    "expiresIn": 900,
    "claims": {
      "role": "admin",
      "permissions": ["read", "write"]
    }
  }'
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `subject` | string | Yes | 토큰 주체 (사용자 ID 등) |
| `audience` | string | No | 토큰 대상 (클라이언트 ID) |
| `algorithm` | string | No | `HS256` (default) or `RS256` |
| `expiresIn` | number | No | 만료 시간 (초), 기본값 900 (15분) |
| `claims` | object | No | 커스텀 클레임 |

**Response**

```json
{
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "refreshExpiresIn": 604800
  },
  "requestId": "req_abc123"
}
```

---

### POST /api/v1/jwt/verify

JWT 토큰을 검증합니다.

**Request**

```bash
curl -X POST https://auth.sonature.io/api/v1/jwt/verify \
  -H "X-API-Key: sk_live_your_api_key" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `token` | string | Yes | 검증할 JWT 토큰 |

**Response (Valid)**

```json
{
  "data": {
    "valid": true,
    "claims": {
      "iss": "sonature-auth",
      "sub": "user-123",
      "aud": "my-app",
      "exp": 1706444100,
      "iat": 1706443200,
      "jti": "tok_xyz789",
      "role": "admin",
      "permissions": ["read", "write"]
    }
  },
  "requestId": "req_abc123"
}
```

**Response (Invalid)**

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

---

### POST /api/v1/jwt/refresh

Refresh Token으로 새 Access Token을 발급합니다.

**Request**

```bash
curl -X POST https://auth.sonature.io/api/v1/jwt/refresh \
  -H "X-API-Key: sk_live_your_api_key" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `refreshToken` | string | Yes | Refresh Token |

**Response**

```json
{
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "refreshExpiresIn": 604800
  },
  "requestId": "req_abc123"
}
```

> **Note**: Refresh Token Rotation이 적용됩니다. 응답에 포함된 새 `refreshToken`을 사용해야 합니다.

---

## PASETO Endpoints

### POST /api/v1/paseto/issue

PASETO v4 토큰을 발급합니다.

**Request**

```bash
curl -X POST https://auth.sonature.io/api/v1/paseto/issue \
  -H "X-API-Key: sk_live_your_api_key" \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "user-123",
    "mode": "local",
    "expiresIn": 900,
    "claims": {
      "role": "admin"
    }
  }'
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `subject` | string | Yes | 토큰 주체 |
| `mode` | string | No | `local` (default, 암호화) or `public` (서명) |
| `expiresIn` | number | No | 만료 시간 (초), 기본값 900 |
| `claims` | object | No | 커스텀 클레임 |

**Response**

```json
{
  "data": {
    "token": "v4.local.xxx...",
    "tokenType": "PASETO",
    "expiresIn": 900
  },
  "requestId": "req_abc123"
}
```

---

### POST /api/v1/paseto/verify

PASETO 토큰을 검증합니다.

**Request**

```bash
curl -X POST https://auth.sonature.io/api/v1/paseto/verify \
  -H "X-API-Key: sk_live_your_api_key" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "v4.local.xxx..."
  }'
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `token` | string | Yes | 검증할 PASETO 토큰 |

**Response**

```json
{
  "data": {
    "valid": true,
    "claims": {
      "sub": "user-123",
      "exp": "2026-01-28T12:15:00Z",
      "iat": "2026-01-28T12:00:00Z",
      "role": "admin"
    }
  },
  "requestId": "req_abc123"
}
```

---

## Health & Metrics

### GET /health

서버 상태를 확인합니다.

```bash
curl https://auth.sonature.io/health
```

**Response**

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

---

### GET /metrics

Prometheus 메트릭을 반환합니다.

```bash
curl https://auth.sonature.io/metrics
```

**Response**

```
# HELP http_server_requests_seconds
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="POST",uri="/api/v1/jwt/issue"} 1234
http_server_requests_seconds_sum{method="POST",uri="/api/v1/jwt/issue"} 5.678
...
```

---

## SDK Examples

### TypeScript

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

### cURL Examples

```bash
# JWT 발급
curl -X POST http://localhost:8080/api/v1/jwt/issue \
  -H "X-API-Key: sk_test_xxx" \
  -H "Content-Type: application/json" \
  -d '{"subject": "user-123"}'

# JWT 검증
curl -X POST http://localhost:8080/api/v1/jwt/verify \
  -H "X-API-Key: sk_test_xxx" \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJhbGc..."}'

# PASETO 발급
curl -X POST http://localhost:8080/api/v1/paseto/issue \
  -H "X-API-Key: sk_test_xxx" \
  -H "Content-Type: application/json" \
  -d '{"subject": "user-123", "mode": "public"}'
```

---

## OpenAPI Specification

Swagger UI: http://localhost:8080/swagger-ui/index.html

OpenAPI JSON: http://localhost:8080/api-docs
