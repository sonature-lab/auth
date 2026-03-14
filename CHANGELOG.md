# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Sprint 3.5**: Tenant Isolation Hardening
  - TenantMismatchException: blocks cross-tenant token refresh attempts
  - TenantContext: tenantId (UUID?) field for runtime tenant identity
  - TenantContextFilter: slug → tenantId resolution via TenantRepository
  - TokenRefreshUseCase: cross-tenant validation (Tenant A token rejected in Tenant B context)
  - Global token policy: tenantId=null tokens permitted in any tenant context (approved)
  - OAuth2ClientRepository: findByClientIdAndTenantId, findByClientIdAndTenantIdIsNull, findAllByTenantId
  - RefreshTokenEntity: composite index idx_refresh_tokens_subject_tenant (subject + tenant_id)
  - Integration tests (5 edge cases) — total 257 tests passing
- **Sprint 2.1**: User Entity + Local Authentication
  - UserEntity (email, passwordHash, name, provider, status)
  - AuthProvider enum (LOCAL, GOOGLE, GITHUB, KAKAO)
  - AuthService (signup, login with JWT issuance)
  - AuthController (POST /api/v1/auth/signup, POST /api/v1/auth/login)
  - Auth exceptions (EmailAlreadyExists, InvalidCredentials, UserSuspended)
  - Unit tests (6) + Integration tests (7)
- **Sprint 3.4**: Tenant Isolation
  - RefreshTokenEntity: tenantId column for tenant-scoped tokens
  - OAuth2ClientEntity: tenantId column for tenant-specific clients
  - RefreshTokenRepository: revokeAllBySubjectAndTenant query
  - RefreshTokenService/TokenRefreshUseCase: tenantId propagation
  - Token rotation preserves tenantId
  - Backward compatibility (null tenantId = global)
  - Integration tests (7)
- **Sprint 3.3**: Authorization Enforcement
  - TenantContext + TenantContextHolder (ThreadLocal tenant context)
  - TenantContextFilter (X-Tenant-Slug header + Bearer token → tenant context)
  - @RequirePermission annotation + PermissionAspect (AOP-based permission check)
  - JWT claims에 tenant memberships (slug + role) 포함
  - TenantController endpoints에 Permission 적용 (MEMBER_INVITE, MEMBER_REMOVE, MEMBER_ROLE_CHANGE)
  - Authorization exceptions (InsufficientPermission, TenantContextRequired, InvalidAccessToken)
  - spring-boot-starter-aop dependency
  - Unit tests (15) + Integration tests (13)
- **Sprint 3.2**: Role + Permission System
  - TenantRole enum (OWNER, ADMIN, MEMBER, VIEWER) with permission hierarchy
  - Permission enum (8 permissions)
  - Role-Permission mapping with hasPermission() method
  - TenantMembershipEntity: role field added
  - Auto-OWNER assignment on tenant creation
  - PUT /api/v1/tenants/{slug}/members/{userId}/role endpoint
  - Unit tests (25) + updated existing tests
- **Sprint 3.1**: Tenant Entity + Basic Management
  - TenantEntity (name, slug, plan, status)
  - TenantMembershipEntity (User ↔ Tenant many-to-many)
  - TenantPlan enum (FREE, PRO, ENTERPRISE)
  - TenantService (CRUD + member management)
  - TenantController (POST/GET tenants, POST/GET/DELETE members)
  - Tenant exception handling (4 types)
  - Unit tests (14) + Integration tests (11)
- **Sprint 2.4**: Consent UI + Scope Management
  - ConsentController (GET /oauth2/consent) with Thymeleaf template
  - ScopeDefinition model (openid, profile, email, auth:read, auth:write)
  - Scope descriptions in Korean/English
  - AuthorizationServerConfig custom consent page integration
  - spring-boot-starter-thymeleaf dependency
  - Unit tests (12) + Integration tests (4)
- **Sprint 2.3**: Social Login (Google, GitHub, Kakao)
  - CustomOAuth2UserService (social profile → UserEntity mapping)
  - OAuth2UserProfileMapper with provider-specific strategies
  - Account linking (same email LOCAL account auto-link)
  - OAuth2LoginSuccessHandler (JWT token pair issuance on social login)
  - spring-boot-starter-oauth2-client integration
  - OAuth2 Client Registration config (env var based)
  - Unit tests (13) + Integration tests (6)
- **Sprint 2.2**: OAuth2 Authorization Server
  - Spring Authorization Server 1.4.5 integration
  - Authorization Code + PKCE flow
  - OAuth2ClientEntity with JPA-backed RegisteredClientRepository
  - OIDC Discovery (/.well-known/openid-configuration)
  - JWK Set endpoint (/oauth2/jwks)
  - UserDetailsService (DB-backed)
  - Dev auto-registration of test clients (public + confidential)
  - Integration tests (7)

### Changed
- SecurityConfig split into AuthorizationServerConfig (@Order 1) + defaultSecurityFilterChain (@Order 2)
- SecurityConfig now only provides PasswordEncoder bean
- UserEntity.provider, UserEntity.providerId changed from val to var (for account linking)
- defaultSecurityFilterChain now includes .oauth2Login() with custom user service
- AuthorizationServerConfig now uses custom consent page (/oauth2/consent)
- Dev OAuth2 clients now include auth:read, auth:write scopes

### Security
- Password hashing via DelegatingPasswordEncoder (bcrypt default)
- PKCE required for public OAuth2 clients
- Refresh token reuse disabled in OAuth2 token settings

---

## [0.1.0] - 2026-01-29

### Added
- Project initialization (Spring Boot 3.5 + Kotlin 1.9)
- Clean Architecture foundation (domain/application/infrastructure/presentation)
- JWT token issuance and verification (HS256, RS256) with jjwt 0.12.6
- PASETO v4.local support with paseto4j-version4
- Refresh Token with Token Rotation and theft detection
- API Key authentication
- Rate Limiting
- TypeScript SDK
- Docker support
- Prometheus metrics
- OpenAPI documentation (Swagger UI)
- Key generation script (generate-keys.sh)
- 94 tests passing

### Security
- SHA-256 hashing for Refresh Token storage
- Automatic token family revocation on theft detection

---

## [0.1.0] - TBD

### Added
- JWT token issuance and verification (HS256, RS256)
- PASETO v4 support (local, public)
- Refresh Token with rotation
- API Key authentication
- Rate Limiting
- TypeScript SDK
- Docker support
- Prometheus metrics
- OpenAPI documentation

---

<!--
Template for new versions:

## [X.Y.Z] - YYYY-MM-DD

### Added
- New features

### Changed
- Changes in existing functionality

### Deprecated
- Soon-to-be removed features

### Removed
- Removed features

### Fixed
- Bug fixes

### Security
- Security fixes
-->
