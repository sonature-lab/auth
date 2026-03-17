# Project Status

> Last updated: 2026-03-17

## Overview

| 항목 | 상태 |
|------|------|
| Phase | **Phase 3 완료 (Closed)** |
| 현재 진행률 | Phase 3: 100% (Sprint 3.1~3.8 완료) |
| 블로커 | 없음 |
| 전체 테스트 | 298개 통과 |

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

### Phase 2: OAuth2 / Social Login (DONE)

| 기능 | 상태 | 테스트 | 비고 |
|------|------|--------|------|
| User Entity | **Done** | - | Sprint 2.1 |
| 회원가입/로그인 | **Done** | 13 tests | /api/v1/auth/signup, /login |
| OAuth2 Auth Server | **Done** | 7 tests | Sprint 2.2 |
| PKCE 지원 | **Done** | - | Public client 필수 |
| OIDC Discovery | **Done** | - | /.well-known/openid-configuration |
| JWK Set | **Done** | - | /oauth2/jwks |
| Social Login | **Done** | 19 tests | Sprint 2.3 (Google, GitHub, Kakao) |
| Consent 화면 | **Done** | 17 tests | Sprint 2.4 (Thymeleaf + Scope 관리) |

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
| `/api/v1/tenants` | POST | **Done** | 3 |
| `/api/v1/tenants/{slug}` | GET | **Done** | 3 |
| `/api/v1/tenants/{slug}/members` | POST | **Done** | 3 |
| `/api/v1/tenants/{slug}/members` | GET | **Done** | 3 |
| `/api/v1/tenants/{slug}/members/{userId}` | DELETE | **Done** | 3 |
| `/api/v1/tenants/{slug}/members/{userId}/role` | PUT | **Done** | 3 |

---

### Phase 3: Multi-tenant + RBAC (DONE)

| 기능 | 상태 | 테스트 | 비고 |
|------|------|--------|------|
| Tenant Entity | **Done** | - | Sprint 3.1 |
| Tenant CRUD + 멤버 관리 | **Done** | 19 tests | /api/v1/tenants |
| Role + Permission | **Done** | 25 tests | Sprint 3.2 (OWNER/ADMIN/MEMBER/VIEWER + 8 permissions) |
| Authorization 적용 | **Done** | 26 tests | Sprint 3.3 (TenantContext + @RequirePermission + AOP) |
| Tenant 격리 | **Done** | 7 tests | Sprint 3.4 (RefreshToken/OAuth2Client tenant_id, row-level isolation) |
| Tenant Isolation Hardening | **Done** | 5 tests | Sprint 3.5 (cross-tenant validation, OAuth2Client tenant-scoped queries, composite index) |
| Security Hardening | **Done** | 14 tests | Sprint 3.6 (permitAll 세분화, JWK 외부화, issuer 환경변수화, ThreadLocal 안전성) |
| Quality Fixes | **Done** | 18 tests | Sprint 3.7 (에러 핸들링, DTO 검증, 테스트 보강, 스케줄링, N+1 해결) |
| Design Decisions | **Done** | 8 tests | Sprint 3.8 (SELECT FOR UPDATE, Bucket4j Rate Limiting, Caffeine 캐싱, ADR 4건) |

---

## Test Coverage

**테스트 현황**: 298개 전체 통과

| 카테고리 | 테스트 수 |
|----------|----------|
| Domain 단위 테스트 | 35 |
| Provider 단위 테스트 | 27 |
| Service 단위 테스트 | 12 |
| 통합 테스트 (JWT) | 19 |
| 통합 테스트 (PASETO) | 7 |
| 통합 테스트 (Auth) | 7 |
| 통합 테스트 (OAuth2) | 7 |
| Social Login 단위 테스트 | 13 |
| Social Login 통합 테스트 | 6 |
| Scope 단위 테스트 | 12 |
| Consent 통합 테스트 | 4 |
| Tenant 단위 테스트 | 26 |
| Tenant 통합 테스트 | 19 |
| Role/Permission 단위 테스트 | 25 |
| TenantContextFilter 단위 테스트 | 9 |
| PermissionAspect 단위 테스트 | 6 |
| Authorization 통합 테스트 | 11 |
| Tenant 격리 통합 테스트 | 12 |
| Security Hardening 테스트 | 14 |
| RefreshTokenService 단위 테스트 | 10 |
| TokenRefreshUseCase 단위 테스트 | 6 |
| RefreshTokenScheduler 단위 테스트 | 2 |
| RateLimitFilter 단위 테스트 | 7 |
| TenantContextFilter 단위 테스트 (캐싱 업데이트) | 1 |

---

## Blockers

현재 블로커 없음.

---

## Next Actions

Phase 3 완료 (Sprint 3.1~3.8 전체). Phase 4+는 별도 private repository (`auth-enterprise`)에서 진행 예정.
