# Implementation Plan

> Sonature Auth MVP 구현 계획서

---

## 1. Development Strategy

### TDD + Clean Architecture

**선택 이유:**
1. **보안 크리티컬**: JWT/PASETO 토큰 처리는 TDD로 모든 엣지 케이스 커버 필수
2. **인터페이스 기반 설계**: `TokenProvider`, `KeyManager` 인터페이스 → Clean Architecture 핵심
3. **향후 확장성**: "암호 알고리즘 서버 분리" 계획 → 의존성 역전 원칙 적용
4. **2주 MVP**: DDD 전체 적용은 오버헤드, Clean Architecture 핵심만 적용

### Development Cycle

```
1. [RED]       실패하는 테스트 작성
2. [GREEN]     최소한의 코드로 테스트 통과
3. [REFACTOR]  Clean Architecture 원칙에 맞게 리팩토링
4. [INTEGRATE] 통합 테스트 추가
```

---

## 2. Folder Structure

```
src/main/kotlin/com/sonature/auth/
├── SonatureAuthApplication.kt
│
├── domain/                          # [Enterprise Business Rules]
│   ├── token/model/                 # Token, TokenClaims, Algorithm
│   ├── token/exception/             # TokenExpiredException 등
│   ├── key/model/                   # SigningKey, VerificationKey
│   ├── apikey/model/                # ApiKey
│   └── refreshtoken/model/          # RefreshToken (Entity)
│
├── application/                     # [Application Business Rules]
│   ├── port/input/                  # UseCases (JwtIssueUseCase 등)
│   ├── port/output/                 # TokenProvider, KeyManager, Repository
│   ├── service/                     # JwtService, PasetoService
│   └── dto/                         # Command, Result DTOs
│
├── infrastructure/                  # [Frameworks & Drivers]
│   ├── config/                      # SecurityConfig, JwtConfig
│   ├── persistence/                 # JPA Entity, Repository Adapter
│   ├── crypto/jwt/                  # Hs256Provider, Rs256Provider
│   ├── crypto/paseto/               # V4LocalProvider, V4PublicProvider
│   ├── crypto/key/                  # EnvironmentKeyManager
│   └── security/                    # ApiKeyFilter, RateLimitFilter
│
├── presentation/                    # [Interface Adapters]
│   ├── api/v1/                      # JwtController, PasetoController
│   └── dto/
│       ├── request/                 # API Request DTOs
│       └── response/                # API Response DTOs
│
└── common/
    ├── util/                        # TimeProvider, IdGenerator
    └── logging/                     # RequestLoggingFilter
```

---

## 3. Implementation Schedule

### Week 1: Core Token Infrastructure

#### Day 1 - Project Structure

| Task | Checkpoint |
|------|-----------|
| 패키지 이동 `com.example.demo` → `com.sonature.auth` | Application 실행 |
| Domain 모델 (Token, TokenClaims, Algorithm) | Unit test 통과 |
| Port interfaces (TokenProvider, KeyManager) | 컴파일 성공 |
| EnvironmentKeyManager (HS256 key 로딩) | Key 로딩 테스트 |

#### Day 2 - JWT HS256

| Task | Checkpoint |
|------|-----------|
| Hs256Provider 구현 (jjwt) | 발급/검증 unit test |
| JwtService (Issue, Verify UseCase) | Service unit test |
| JwtController (/issue, /verify) | Swagger UI 호출 |
| Integration test | API 테스트 통과 |

**검증:**
```bash
curl -X POST http://localhost:8080/api/v1/jwt/issue \
  -H "X-API-Key: test-key" \
  -H "Content-Type: application/json" \
  -d '{"subject": "user-123", "algorithm": "HS256"}'
```

#### Day 3 - JWT RS256

| Task | Checkpoint |
|------|-----------|
| KeyManager RS256 키 로딩 | RSA 키 파싱 테스트 |
| Rs256Provider 구현 | RS256 unit test |
| JwtController algorithm 선택 | HS256/RS256 API 작동 |
| GlobalExceptionHandler | 에러 응답 형식 검증 |

#### Day 4 - Refresh Token

| Task | Checkpoint |
|------|-----------|
| RefreshToken Entity + Repository | JPA 매핑 테스트 |
| RefreshTokenService (Rotation) | Rotation unit test |
| /refresh 엔드포인트 | Refresh API 테스트 |
| 탈취 감지 로직 | Security test |

#### Day 5 - PASETO v4.local

| Task | Checkpoint |
|------|-----------|
| PASETO 도메인 모델 확장 | 컴파일 성공 |
| V4LocalProvider (jpaseto) | v4.local unit test |
| PasetoService + PasetoController | /paseto/* API 작동 |
| Integration test | PASETO API 테스트 |

---

### Week 2: Production Ready

#### Day 6 - PASETO v4.public

| Task | Checkpoint |
|------|-----------|
| V4PublicProvider (Ed25519) | v4.public unit test |
| KeyManager Ed25519 키 로딩 | 키 파싱 테스트 |
| PasetoController mode 선택 | local/public API 작동 |
| OpenAPI 문서화 | Swagger UI 완전 |

#### Day 7 - Security & E2E

| Task | Checkpoint |
|------|-----------|
| ApiKeyAuthenticationFilter | 인증 테스트 |
| RateLimitFilter | Rate limit 테스트 |
| 에러 코드 체계 완성 | 에러 응답 일관성 |
| E2E 테스트 | 전체 플로우 통과 |

#### Day 8-9 - TypeScript SDK

| Task |
|------|
| SDK 프로젝트 초기화 (tsup) |
| types.ts (타입 정의) |
| client.ts (HTTP 클라이언트) |
| jwt.ts (issue, verify, refresh) |
| paseto.ts (issue, verify) |
| errors.ts (에러 클래스) |
| 빌드 (ESM + CJS) |
| README + 예제 |

#### Day 10 - Docker & Monitoring

| Task |
|------|
| Dockerfile (multi-stage) |
| docker-compose.yml |
| Prometheus 메트릭 |
| Grafana 대시보드 |
| Health check 검증 |

#### Day 11-12 - Deployment & Docs

| Task |
|------|
| OCI 인스턴스 설정 |
| 환경변수 설정 |
| 애플리케이션 배포 |
| TLS 설정 |
| API 문서 완성 |
| README 업데이트 |
| generate-keys.sh 스크립트 |
| CHANGELOG, LICENSE |

#### Day 13-14 - Testing & Release

| Task |
|------|
| 전체 테스트 + 커버리지 80%+ |
| 버그 수정 |
| 성능 테스트 (p99 < 10ms) |
| 코드 리뷰 + 리팩토링 |
| SDK npm 배포 준비 |
| GitHub 릴리스 (v0.1.0) |
| 프로덕션 최종 검증 |

---

## 4. Dependency Order

```
Domain Models → Ports/Interfaces → KeyManager → Crypto Providers
                                              ↓
                                    JwtService/PasetoService
                                              ↓
                              RefreshTokenService + Repository
                                              ↓
                              ApiKeyFilter + RateLimitFilter
                                              ↓
                                        Controllers
                                              ↓
                                      TypeScript SDK
                                              ↓
                                    Docker + Deployment
```

---

## 5. Key Files

| File | Description |
|------|-------------|
| `SonatureAuthApplication.kt` | 메인 애플리케이션 |
| `build.gradle` | 의존성 설정 (JWT, PASETO, Prometheus) |
| `application.yml` | 개발 환경 설정 |
| `application-prod.yml` | 프로덕션 환경 설정 |
| `docs/PRD.md` | API 스펙, 에러 코드 구현 기준 |

---

## 6. Verification

### Test Commands

```bash
# 전체 테스트
./gradlew test

# Unit 테스트만
./gradlew test --tests "*UnitTest"

# Integration 테스트만
./gradlew test --tests "*IntegrationTest"

# 커버리지 리포트
./gradlew jacocoTestReport
```

### E2E Verification

```bash
# JWT 플로우
curl -X POST http://localhost:8080/api/v1/jwt/issue \
  -H "X-API-Key: test" \
  -H "Content-Type: application/json" \
  -d '{"subject":"u1"}'

curl -X POST http://localhost:8080/api/v1/jwt/verify \
  -H "X-API-Key: test" \
  -H "Content-Type: application/json" \
  -d '{"token":"..."}'

curl -X POST http://localhost:8080/api/v1/jwt/refresh \
  -H "X-API-Key: test" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"..."}'

# PASETO 플로우
curl -X POST http://localhost:8080/api/v1/paseto/issue \
  -H "X-API-Key: test" \
  -H "Content-Type: application/json" \
  -d '{"subject":"u1","mode":"local"}'

# Rate Limit 검증
for i in {1..105}; do
  curl -s http://localhost:8080/api/v1/jwt/issue \
    -H "X-API-Key: test" \
    -H "Content-Type: application/json" \
    -d '{"subject":"test"}'
done
# 100회 후 429 응답 확인
```

### Production Verification

```bash
# Docker 실행
docker-compose up -d

# Health check
curl http://localhost:8080/health
# Expected: {"status": "UP"}

# Prometheus
curl http://localhost:9090/targets
# Expected: sonature-auth target UP

# Grafana
open http://localhost:3000
```

---

## 7. Success Criteria

### MVP Completion Checklist

- [ ] JWT HS256 발급/검증 작동
- [ ] JWT RS256 발급/검증 작동
- [ ] Refresh Token Rotation 작동
- [ ] PASETO v4.local 발급/검증 작동
- [ ] PASETO v4.public 발급/검증 작동
- [ ] API Key 인증 작동
- [ ] Rate Limiting 작동
- [ ] 에러 응답 일관성
- [ ] Unit Test 커버리지 80%+
- [ ] Integration Test 커버리지 70%+
- [ ] TypeScript SDK 배포 준비
- [ ] Docker 이미지 빌드
- [ ] OCI 배포 완료
- [ ] 성능 목표 달성 (p99 < 10ms)
