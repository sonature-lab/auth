# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Project initialization (Spring Boot 3.5 + Kotlin 1.9)
- Gradle dependencies (JWT, PASETO, Prometheus, JaCoCo)
- PRD document with 17 sections
- Implementation plan (TDD + Clean Architecture)
- Documentation structure (STATUS, PROGRESS, ROADMAP, ARCHITECTURE)
- PRD template for reuse
- Key generation script
- **Day 1**: Clean Architecture foundation
  - Domain models (Token, TokenClaims, Algorithm, TokenType)
  - Port interfaces (TokenProvider, KeyManager)
  - EnvironmentKeyManager with HS256/RS256/PASETO key loading
- **Day 2**: JWT HS256 implementation
  - Hs256Provider with jjwt 0.12.6
  - JwtService (issueAccessToken, verifyToken)
  - JwtController (/api/v1/jwt/issue, /verify)
  - GlobalExceptionHandler for token errors
- **Day 3**: JWT RS256 implementation
  - Rs256Provider with RSA asymmetric signing
  - Algorithm selection via API parameter
- **Day 4**: Refresh Token with Token Rotation
  - RefreshTokenEntity with JPA
  - RefreshTokenService (rotation, theft detection)
  - TokenRefreshUseCase facade
  - /api/v1/jwt/issue-pair, /refresh endpoints
  - Automatic session revocation on token reuse
- **Day 5**: PASETO v4.local implementation
  - PasetoV4LocalProvider with paseto4j-version4
  - PasetoService + PasetoController
  - /api/v1/paseto/issue, /verify endpoints

### Changed
- Project direction from OAuth2.1 to JWT/PASETO token framework

### Deprecated

### Removed
- OAuth2.1 Authorization Server dependency

### Fixed

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
