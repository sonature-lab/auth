# Roadmap

> Sonature Auth의 개발 로드맵입니다. Phase 1~2는 완료되어 현재 릴리스에 포함되어 있습니다.

---

## Phase 1: Token Framework (DONE)

### Week 1: Core Token Infrastructure ✅

#### Day 1 - Project Structure ✅
- [x] 패키지 이동 `com.example.demo` → `com.sonature.auth`
- [x] Domain 모델 (Token, TokenClaims, Algorithm)
- [x] Port interfaces (TokenProvider, KeyManager)
- [x] EnvironmentKeyManager (HS256 key loading)

#### Day 2 - JWT HS256 ✅
- [x] Hs256Provider 구현 (jjwt)
- [x] JwtService (Issue, Verify UseCase)
- [x] JwtController (/issue, /verify)
- [x] Integration test

#### Day 3 - JWT RS256 ✅
- [x] KeyManager RS256 키 로딩
- [x] Rs256Provider 구현
- [x] JwtController algorithm 선택
- [x] GlobalExceptionHandler

#### Day 4 - Refresh Token ✅
- [x] RefreshToken Entity + Repository
- [x] RefreshTokenService (Rotation)
- [x] /refresh 엔드포인트
- [x] 탈취 감지 로직

#### Day 5 - PASETO v4.local ✅
- [x] PASETO 도메인 모델 확장
- [x] V4LocalProvider (paseto4j-version4)
- [x] PasetoService + PasetoController
- [x] Integration test

### Week 2: Production Ready ✅

- [x] API Key 인증
- [x] Rate Limiting
- [x] TypeScript SDK
- [x] Docker & Monitoring (Prometheus)
- [x] Swagger / OpenAPI
- [x] README, LICENSE, CHANGELOG

---

## Phase 2: OAuth2 / Social Login (DONE)

### Sprint 2.1 - User Entity + 기본 인증 ✅
- [x] UserEntity (email, passwordHash, provider, status)
- [x] UserRepository + AuthService
- [x] 회원가입/로그인 API (/api/v1/auth/signup, /login)
- [x] Password hashing (bcrypt)
- [x] JWT 발급을 User 기반으로 연동
- [x] Auth 예외 처리 (EmailAlreadyExists, InvalidCredentials, UserSuspended)
- [x] 단위 테스트 6개 + 통합 테스트 7개

### Sprint 2.2 - OAuth2 Authorization Server ✅
- [x] Spring Authorization Server 1.4.5 의존성 추가
- [x] OAuth2ClientEntity + JPA RegisteredClientRepository
- [x] Authorization Code + PKCE flow
- [x] Token Endpoint (/oauth2/token)
- [x] Authorization Endpoint (/oauth2/authorize)
- [x] OIDC Discovery (/.well-known/openid-configuration)
- [x] JWK Set (/oauth2/jwks)
- [x] UserDetailsService (DB 연동)
- [x] Dev 환경 자동 클라이언트 등록 (public + confidential)
- [x] 통합 테스트 7개

### Sprint 2.3 - Social Login ✅
- [x] spring-boot-starter-oauth2-client 의존성 추가
- [x] OAuth2 Client Registration (Google, GitHub, Kakao) 환경변수 기반
- [x] CustomOAuth2UserService (소셜 프로필 → UserEntity 자동 생성/연동)
- [x] OAuth2UserProfileMapper (Provider별 프로필 매핑 전략)
- [x] 소셜 계정 ↔ 로컬 계정 연동 (같은 email 자동 링크)
- [x] OAuth2LoginSuccessHandler (로그인 성공 → JWT 토큰 쌍 발급)
- [x] SecurityConfig에 .oauth2Login() 추가
- [x] 단위 테스트 13개 + 통합 테스트 6개

### Sprint 2.4 - OIDC + Consent ✅
- [x] Thymeleaf 의존성 추가 + Consent 화면 구현 (templates/consent.html)
- [x] ConsentController (GET /oauth2/consent — scope 표시 + 승인/거부)
- [x] ScopeDefinition 모델 (openid, profile, email, auth:read, auth:write)
- [x] Scope별 한글/영문 설명 매핑
- [x] AuthorizationServerConfig consent page 연결
- [x] DevDataInitializer에 커스텀 scope 추가
- [x] 단위 테스트 12개 + 통합 테스트 5개 (총 149개 전체 통과)

---

## Phase 3: Multi-tenant + RBAC (UPCOMING)

This is the next phase for the open-source core.

- [ ] Tenant (Organization) management
- [ ] Row-level isolation RBAC
- [ ] Permission-based authorization
- [ ] Tenant-scoped API Key management

---

> **Phase 4+** (SSO Hub, Advanced Security) is developed in a separate private repository.

---

## Milestones

| Version | Target | Status |
|---------|--------|--------|
| v0.1.0 | Phase 1 — Token Framework | **Done** |
| v0.2.0 | Phase 2 — OAuth2 / Social Login / Consent | **Done** |
| v0.3.0 | Phase 3 — Multi-tenant + RBAC | Upcoming |
| v1.0.0 | Phase 4 — SSO Hub + Production Stable | Upcoming |
