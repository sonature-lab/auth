# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Sprint 2.1**: User Entity + Local Authentication
  - UserEntity (email, passwordHash, name, provider, status)
  - AuthProvider enum (LOCAL, GOOGLE, GITHUB, KAKAO)
  - AuthService (signup, login with JWT issuance)
  - AuthController (POST /api/v1/auth/signup, POST /api/v1/auth/login)
  - Auth exceptions (EmailAlreadyExists, InvalidCredentials, UserSuspended)
  - Unit tests (6) + Integration tests (7)
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
