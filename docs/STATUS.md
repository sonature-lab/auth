# Project Status

> 최종 업데이트: 2026-01-29

## Overview

| 항목 | 상태 |
|------|------|
| Phase | **Day 5 - PASETO v4.local Complete** |
| MVP 목표일 | 2주 (Day 14) |
| 현재 진행률 | 40% (Week 1 Complete) |
| 블로커 | 없음 |

---

## Feature Status

### Core Features

| 기능 | 상태 | 테스트 | 비고 |
|------|------|--------|------|
| JWT HS256 발급 | **Done** | 9 tests | Hs256Provider |
| JWT HS256 검증 | **Done** | 9 tests | Hs256Provider |
| JWT RS256 발급 | **Done** | 9 tests | Rs256Provider |
| JWT RS256 검증 | **Done** | 9 tests | Rs256Provider |
| JWT Refresh Token | **Done** | 8 tests | Token Rotation + 탈취 감지 |
| PASETO v4.local | **Done** | 16 tests | PasetoV4LocalProvider |
| PASETO v4.public | Not Started | - | Day 6 예정 |

### Infrastructure

| 기능 | 상태 | 비고 |
|------|------|------|
| 프로젝트 구조 | **Done** | Clean Architecture 적용 |
| Domain Models | **Done** | Algorithm, TokenClaims, Token, TokenPair |
| Port Interfaces | **Done** | TokenProvider, KeyManager |
| KeyManager 구현 | **Done** | EnvironmentKeyManager (HS256, RS256, PASETO) |
| JwtService | **Done** | issueAccessToken, verifyToken, issueTokenPair, refreshTokens |
| PasetoService | **Done** | issueToken, verifyToken |
| JwtController | **Done** | /api/v1/jwt/issue, /verify, /issue-pair, /refresh |
| PasetoController | **Done** | /api/v1/paseto/issue, /verify |
| Error Handling | **Done** | GlobalExceptionHandler |
| RefreshToken DB | **Done** | H2/PostgreSQL JPA Entity |
| API Key 인증 | Not Started | Day 7 예정 |
| Rate Limiting | Not Started | Day 7 예정 |
| Docker | Not Started | Day 10 예정 |
| Prometheus | Not Started | Day 10 예정 |

### SDK & Docs

| 기능 | 상태 | 비고 |
|------|------|------|
| TypeScript SDK | Not Started | Day 8-9 예정 |
| OpenAPI Spec | In Progress | Swagger UI 동작 |
| README | Done | 기본 완료 |
| PRD | Done | 17개 섹션 완료 |

---

## Test Coverage

| 레이어 | 목표 | 현재 | 상태 |
|--------|------|------|------|
| Unit | 80% | ~50% | In Progress |
| Integration | 70% | ~30% | In Progress |
| E2E | Core Flows | 0% | Not Started |

**테스트 현황**: 94개 테스트 전체 통과

---

## Completed Tasks

### 2026-01-29 (Day 5)
- [x] PasetoV4LocalProvider 구현 (paseto4j-version4)
- [x] PasetoService 구현 (issueToken, verifyToken)
- [x] PasetoController (/api/v1/paseto/issue, /verify)
- [x] PASETO DTOs (Request/Response)
- [x] PASETO 단위 테스트 (9 tests)
- [x] PASETO 통합 테스트 (7 tests)
- [x] 총 94개 테스트 전체 통과

### 2026-01-29 (Day 4)
- [x] RefreshTokenEntity JPA Entity
- [x] RefreshTokenRepository (Spring Data JPA)
- [x] RefreshTokenService (rotation, theft detection)
- [x] TokenRefreshUseCase facade
- [x] JwtController /issue-pair, /refresh 엔드포인트
- [x] GlobalExceptionHandler 3개 핸들러 추가
- [x] 통합 테스트 8개 추가 (총 78개)

### 2026-01-29 (Day 3)
- [x] Rs256Provider 구현 (RSA 비대칭키)
- [x] 테스트용 RSA 2048-bit 키 쌍 생성
- [x] application-test.yml 설정 (RS256 키 포함)
- [x] API algorithm 파라미터로 HS256/RS256 선택
- [x] Rs256ProviderTest (9 tests)
- [x] RS256 통합 테스트 (4 tests)
- [x] 총 70개 테스트 전체 통과

### 2026-01-29 (Day 2)
- [x] Hs256Provider 구현 (jjwt 0.12.6)
- [x] JwtService 구현 (issueAccessToken, verifyToken)
- [x] JwtController (/api/v1/jwt/issue, /verify)
- [x] API 공통 모델 (ApiResponse, ApiError)
- [x] GlobalExceptionHandler 전역 예외 처리
- [x] SecurityConfig (permitAll 설정)
- [x] 단위 테스트 22개 추가 (총 57개)

### 2026-01-29 (Day 1)
- [x] 패키지 마이그레이션 (`com.example.demo` → `com.sonature.auth`)
- [x] Domain Models (Algorithm, TokenType, TokenClaims, Token, TokenConfig)
- [x] Domain Exceptions (TokenException sealed class 계층)
- [x] Port Interfaces (TokenProvider, KeyManager)
- [x] EnvironmentKeyManager 구현
- [x] Common Utilities (TimeProvider, IdGenerator)
- [x] 단위 테스트 35개 작성 및 통과

### 2026-01-28 (Day 0)
- [x] 프로젝트 초기화 (Spring Boot 3.5 + Kotlin)
- [x] Gradle 의존성 설정 (JWT, PASETO, Prometheus, etc.)
- [x] JDK 21 설정
- [x] PRD 작성 완료 (17개 섹션)
- [x] 구현 플랜 작성 (TDD + Clean Architecture)

---

## Blockers

현재 블로커 없음.

---

## Next Actions

1. **Day 6**: PASETO v4.public 구현 (Ed25519)
2. **Day 6**: KeyManager Ed25519 키 로딩
3. **Day 6**: PasetoController mode 선택
4. **Day 6**: OpenAPI 문서화 완성

---

## API Endpoints

| Endpoint | Method | Status |
|----------|--------|--------|
| `/api/v1/jwt/issue` | POST | **Done** |
| `/api/v1/jwt/issue-pair` | POST | **Done** |
| `/api/v1/jwt/verify` | POST | **Done** |
| `/api/v1/jwt/refresh` | POST | **Done** |
| `/api/v1/paseto/issue` | POST | **Done** |
| `/api/v1/paseto/verify` | POST | **Done** |

---

## Quick Commands

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 실행
./gradlew bootRun

# 커버리지
./gradlew jacocoTestReport

# JWT 토큰 발급 (HS256)
curl -X POST http://localhost:8080/api/v1/jwt/issue \
  -H "Content-Type: application/json" \
  -d '{"subject": "user-123"}'

# JWT 토큰 쌍 발급 (RS256)
curl -X POST http://localhost:8080/api/v1/jwt/issue-pair \
  -H "Content-Type: application/json" \
  -d '{"subject": "user-123", "algorithm": "RS256"}'

# JWT 토큰 갱신
curl -X POST http://localhost:8080/api/v1/jwt/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "eyJ..."}'

# PASETO 토큰 발급
curl -X POST http://localhost:8080/api/v1/paseto/issue \
  -H "Content-Type: application/json" \
  -d '{"subject": "user-123"}'

# PASETO 토큰 검증
curl -X POST http://localhost:8080/api/v1/paseto/verify \
  -H "Content-Type: application/json" \
  -d '{"token": "v4.local.xxx..."}'
```
