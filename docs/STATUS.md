# Project Status

> 최종 업데이트: 2026-03-13

## Overview

| 항목 | 상태 |
|------|------|
| Phase | **Phase 2 - Sprint 2.2 완료** |
| 현재 진행률 | Phase 2: 50% (Sprint 2.1~2.2 완료, 2.3~2.4 남음) |
| 블로커 | 없음 |
| 전체 테스트 | 114개 통과 |

---

## Feature Status

### Phase 1: Token Framework (DONE)

| 기능 | 상태 | 테스트 | 비고 |
|------|------|--------|------|
| JWT HS256 발급/검증 | **Done** | 9 tests | Hs256Provider |
| JWT RS256 발급/검증 | **Done** | 9 tests | Rs256Provider |
| JWT Refresh Token | **Done** | 8 tests | Token Rotation + 탈취 감지 |
| PASETO v4.local | **Done** | 16 tests | PasetoV4LocalProvider |
| API Key 인증 | **Done** | - | ApiKeyAuthenticationFilter |
| Rate Limiting | **Done** | - | RateLimitFilter |
| Swagger UI | **Done** | - | SpringDoc OpenAPI |
| Prometheus 메트릭 | **Done** | - | Micrometer |

### Phase 2: OAuth2 / Social Login (IN PROGRESS)

| 기능 | 상태 | 테스트 | 비고 |
|------|------|--------|------|
| User Entity | **Done** | - | Sprint 2.1 |
| 회원가입/로그인 | **Done** | 13 tests | /api/v1/auth/signup, /login |
| OAuth2 Auth Server | **Done** | 7 tests | Sprint 2.2 |
| PKCE 지원 | **Done** | - | Public client 필수 |
| OIDC Discovery | **Done** | - | /.well-known/openid-configuration |
| JWK Set | **Done** | - | /oauth2/jwks |
| Social Login | Not Started | - | Sprint 2.3 예정 |
| Consent 화면 | Not Started | - | Sprint 2.4 예정 |

---

## API Endpoints

| Endpoint | Method | Status | Phase |
|----------|--------|--------|-------|
| `/api/v1/jwt/issue` | POST | **Done** | 1 |
| `/api/v1/jwt/issue-pair` | POST | **Done** | 1 |
| `/api/v1/jwt/verify` | POST | **Done** | 1 |
| `/api/v1/jwt/refresh` | POST | **Done** | 1 |
| `/api/v1/paseto/issue` | POST | **Done** | 1 |
| `/api/v1/paseto/verify` | POST | **Done** | 1 |
| `/api/v1/auth/signup` | POST | **Done** | 2 |
| `/api/v1/auth/login` | POST | **Done** | 2 |
| `/oauth2/authorize` | GET | **Done** | 2 |
| `/oauth2/token` | POST | **Done** | 2 |
| `/oauth2/jwks` | GET | **Done** | 2 |
| `/.well-known/openid-configuration` | GET | **Done** | 2 |
| `/login` | GET | **Done** | 2 |

---

## Test Coverage

**테스트 현황**: 114개 전체 통과

| 카테고리 | 테스트 수 |
|----------|----------|
| Domain 단위 테스트 | 35 |
| Provider 단위 테스트 | 27 |
| Service 단위 테스트 | 12 |
| 통합 테스트 (JWT) | 19 |
| 통합 테스트 (PASETO) | 7 |
| 통합 테스트 (Auth) | 7 |
| 통합 테스트 (OAuth2) | 7 |

---

## Blockers

현재 블로커 없음.

---

## Next Actions

1. **Sprint 2.3**: Social Login (Google, GitHub, Kakao)
2. **Sprint 2.4**: Consent 화면 + Scope 관리
3. **Phase 3**: Multi-tenant + RBAC
