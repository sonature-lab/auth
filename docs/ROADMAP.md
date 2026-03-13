# Roadmap

> 프로젝트 로드맵 및 진행 상황

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

## Phase 2: OAuth2 / Social Login (IN PROGRESS)

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

### Sprint 2.4 - OIDC + Consent (NEXT)
- [ ] Consent 화면 구현 (Thymeleaf 최소 UI)
- [ ] Scope 관리 세분화
- [ ] 통합 테스트 + SDK 업데이트

---

## Phase 3: Multi-tenant + RBAC (PLANNED)
- [ ] 테넌트(Organization) 관리
- [ ] 테넌트별 격리된 RBAC (Row-level isolation)
- [ ] Permission 기반 권한 체계
- [ ] 테넌트별 API Key 발급/관리

## Phase 4: SSO Hub (PLANNED)
- [ ] 모든 Sonature 서비스 SSO 통합
- [ ] Session 관리 + Cross-service 토큰 교환
- [ ] Service Registry

## Phase 5: Advanced Security (PLANNED)
- [ ] MFA (TOTP, WebAuthn/Passkey)
- [ ] Device fingerprinting + 이상 로그인 탐지
- [ ] Audit Log, IP allowlist/blocklist

## Phase 6: Open Source + SaaS (PLANNED)
- [ ] 오픈소스 Core 공개
- [ ] SaaS Pro tier

---

## Milestones

| Version | Target | Status |
|---------|--------|--------|
| v0.1.0 | Phase 1 - Token Framework | Done |
| v0.2.0 | Phase 2 - OAuth2 / Social Login | In Progress |
| v0.3.0 | Phase 3 - Multi-tenant + RBAC | Planned |
| v1.0.0 | Phase 4 - SSO Hub + Production Stable | Planned |
