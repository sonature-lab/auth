# Architecture

> Sonature Auth 아키텍처 문서

---

## Overview

Clean Architecture 기반 JWT/PASETO 토큰 프레임워크.

```
┌─────────────────────────────────────────────────────────────┐
│                    External Systems                          │
│              (Clients, Monitoring, Database)                 │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                   Presentation Layer                         │
│            (Controllers, Request/Response DTOs)              │
├─────────────────────────────────────────────────────────────┤
│                   Application Layer                          │
│              (Services, Use Cases, Commands)                 │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                             │
│            (Entities, Value Objects, Interfaces)             │
├─────────────────────────────────────────────────────────────┤
│                  Infrastructure Layer                        │
│     (Crypto Providers, Repositories, External Adapters)      │
└─────────────────────────────────────────────────────────────┘
```

---

## Folder Structure

```
src/main/kotlin/com/sonature/auth/
├── SonatureAuthApplication.kt
│
├── domain/                          # [Enterprise Business Rules]
│   ├── token/
│   │   ├── model/                   # Token, TokenClaims, Algorithm
│   │   └── exception/               # TokenExpiredException, etc.
│   ├── key/
│   │   └── model/                   # SigningKey, VerificationKey
│   ├── apikey/
│   │   └── model/                   # ApiKey
│   └── refreshtoken/
│       └── model/                   # RefreshToken (Entity)
│
├── application/                     # [Application Business Rules]
│   ├── port/
│   │   ├── input/                   # Use Cases (JwtIssueUseCase, etc.)
│   │   └── output/                  # TokenProvider, KeyManager, Repository
│   ├── service/                     # JwtService, PasetoService
│   └── dto/                         # Command, Result DTOs
│
├── infrastructure/                  # [Frameworks & Drivers]
│   ├── config/                      # SecurityConfig, JwtConfig
│   ├── persistence/                 # JPA Entity, Repository Adapter
│   ├── crypto/
│   │   ├── jwt/                     # Hs256Provider, Rs256Provider
│   │   └── paseto/                  # V4LocalProvider, V4PublicProvider
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

## Core Interfaces

### TokenProvider

토큰 발급/검증을 추상화. 알고리즘별 구현체 교체 가능.

```kotlin
interface TokenProvider {
    fun issue(claims: TokenClaims, config: TokenConfig): String
    fun verify(token: String): TokenClaims
    fun supportedAlgorithm(): Algorithm
}
```

**구현체:**
- `Hs256Provider` - HMAC-SHA256 (대칭키)
- `Rs256Provider` - RSA-SHA256 (비대칭키)
- `V4LocalProvider` - PASETO v4.local (XChaCha20-Poly1305)
- `V4PublicProvider` - PASETO v4.public (Ed25519)

### KeyManager

키 로딩/관리를 추상화. 환경변수, 파일, Vault 등 소스 교체 가능.

```kotlin
interface KeyManager {
    fun getSigningKey(algorithm: Algorithm): Key
    fun getVerificationKey(algorithm: Algorithm): Key
}
```

**구현체:**
- `EnvironmentKeyManager` - 환경변수에서 키 로딩 (MVP)
- `VaultKeyManager` - HashiCorp Vault (향후)

### RefreshTokenRepository

Refresh Token 저장/조회를 추상화.

```kotlin
interface RefreshTokenRepository {
    fun save(token: RefreshToken): RefreshToken
    fun findByTokenHash(hash: String): RefreshToken?
    fun revokeBySubject(subject: String)
}
```

---

## Data Flow

### Token Issue Flow

```
Client Request
     │
     ▼
┌─────────────┐
│ Controller  │ ─── Request DTO 검증
└─────────────┘
     │
     ▼
┌─────────────┐
│  Service    │ ─── 비즈니스 로직
└─────────────┘
     │
     ▼
┌─────────────┐
│ KeyManager  │ ─── 서명 키 조회
└─────────────┘
     │
     ▼
┌─────────────────┐
│ TokenProvider   │ ─── 토큰 생성
└─────────────────┘
     │
     ▼
Response (Token)
```

### Token Verify Flow

```
Client Request (Token)
     │
     ▼
┌─────────────┐
│ Controller  │ ─── 토큰 추출
└─────────────┘
     │
     ▼
┌─────────────┐
│  Service    │
└─────────────┘
     │
     ▼
┌─────────────┐
│ KeyManager  │ ─── 검증 키 조회
└─────────────┘
     │
     ▼
┌─────────────────┐
│ TokenProvider   │ ─── 서명 검증 + 만료 확인
└─────────────────┘
     │
     ▼
Response (Claims)
```

### Refresh Token Flow

```
Client Request (Refresh Token)
     │
     ▼
┌─────────────────────┐
│ RefreshTokenService │
└─────────────────────┘
     │
     ├── 1. token_hash로 DB 조회
     ├── 2. revoked_at 확인 (탈취 감지)
     ├── 3. expires_at 확인
     ├── 4. 기존 토큰 revoke
     ├── 5. 새 토큰 발급 (Rotation)
     │
     ▼
Response (New Access + Refresh Token)
```

---

## Security Layers

```
┌────────────────────────────────────────┐
│           TLS 1.3 (HTTPS)              │  ← Transport Security
├────────────────────────────────────────┤
│         API Key Filter                 │  ← Authentication
├────────────────────────────────────────┤
│        Rate Limit Filter               │  ← DoS Protection
├────────────────────────────────────────┤
│         Input Validation               │  ← Request Validation
├────────────────────────────────────────┤
│        Business Logic                  │
└────────────────────────────────────────┘
```

---

## Algorithm Support

### JWT

| Algorithm | Type | Key Size | Use Case |
|-----------|------|----------|----------|
| HS256 | Symmetric | 256 bits | 단일 서비스, 빠른 검증 |
| RS256 | Asymmetric | 2048 bits | 분산 시스템, 공개키 배포 |

### PASETO v4

| Mode | Type | Algorithm | Use Case |
|------|------|-----------|----------|
| local | Symmetric | XChaCha20-Poly1305 | 암호화 + 인증 |
| public | Asymmetric | Ed25519 | 서명만, JWT RS256 대안 |

---

## Configuration

### Environment Variables

```bash
# JWT Keys
JWT_HS256_SECRET=<32+ bytes base64>
JWT_RS256_PRIVATE_KEY=<PEM format>
JWT_RS256_PUBLIC_KEY=<PEM format>

# PASETO Keys
PASETO_SECRET_KEY=<32 bytes base64>
PASETO_PRIVATE_KEY=<Ed25519 PEM>
PASETO_PUBLIC_KEY=<Ed25519 PEM>

# API Keys
API_KEYS=sk_live_xxx,sk_test_yyy

# Database
DATABASE_URL=jdbc:postgresql://...
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
```

---

## Future: Crypto Server Separation

현재 아키텍처는 암호화 서버 분리를 대비한 인터페이스 설계.

```
┌─────────────────┐      ┌─────────────────┐
│  Auth Server    │ ───▶ │  Crypto Server  │
│  (Public)       │      │  (Private)      │
└─────────────────┘      └─────────────────┘
        │                        │
        │                        ├── Custom Algorithms
        │                        ├── HSM Integration
        │                        └── Key Rotation
        │
        └── TokenProvider 구현체만 교체
```

`TokenProvider`, `KeyManager` 인터페이스를 통해 gRPC 기반 원격 구현체로 교체 가능.

---

## Diagrams (Mermaid)

### System Context

```mermaid
C4Context
    title Sonature Auth - System Context

    Person(client, "Client Application", "JWT/PASETO 토큰을 사용하는 애플리케이션")

    System(auth, "Sonature Auth", "JWT/PASETO 토큰 발급/검증 서비스")

    SystemDb(db, "PostgreSQL", "Refresh Token 저장")
    System_Ext(prometheus, "Prometheus", "메트릭 수집")
    System_Ext(grafana, "Grafana", "모니터링 대시보드")

    Rel(client, auth, "API 호출", "HTTPS")
    Rel(auth, db, "토큰 저장/조회", "JDBC")
    Rel(prometheus, auth, "메트릭 스크래핑", "/metrics")
    Rel(grafana, prometheus, "쿼리")
```

### Token Issue Sequence

```mermaid
sequenceDiagram
    participant C as Client
    participant F as ApiKeyFilter
    participant R as RateLimitFilter
    participant Ctrl as JwtController
    participant Svc as JwtService
    participant KM as KeyManager
    participant TP as TokenProvider

    C->>F: POST /api/v1/jwt/issue
    F->>F: Validate API Key
    F->>R: Pass
    R->>R: Check Rate Limit
    R->>Ctrl: Pass
    Ctrl->>Ctrl: Validate Request
    Ctrl->>Svc: issueToken(command)
    Svc->>KM: getSigningKey(HS256)
    KM-->>Svc: SecretKey
    Svc->>TP: issue(claims, config)
    TP-->>Svc: JWT Token
    Svc-->>Ctrl: TokenResult
    Ctrl-->>C: 200 OK {accessToken, refreshToken}
```

### Token Verify Sequence

```mermaid
sequenceDiagram
    participant C as Client
    participant Ctrl as JwtController
    participant Svc as JwtService
    participant KM as KeyManager
    participant TP as TokenProvider

    C->>Ctrl: POST /api/v1/jwt/verify
    Ctrl->>Svc: verifyToken(token)
    Svc->>TP: verify(token)
    TP->>TP: Parse Header (extract algorithm)
    TP->>KM: getVerificationKey(algorithm)
    KM-->>TP: Key
    TP->>TP: Verify Signature
    TP->>TP: Check Expiration

    alt Valid Token
        TP-->>Svc: TokenClaims
        Svc-->>Ctrl: VerifyResult(valid=true)
        Ctrl-->>C: 200 OK {claims}
    else Invalid Token
        TP-->>Svc: throw TokenInvalidException
        Svc-->>Ctrl: throw
        Ctrl-->>C: 401 {error: TOKEN_INVALID}
    end
```

### Refresh Token Rotation

```mermaid
sequenceDiagram
    participant C as Client
    participant Svc as RefreshTokenService
    participant Repo as RefreshTokenRepository
    participant TP as TokenProvider

    C->>Svc: refresh(refreshToken)
    Svc->>Svc: hash(refreshToken)
    Svc->>Repo: findByTokenHash(hash)
    Repo-->>Svc: RefreshToken

    alt Token Not Found
        Svc-->>C: 401 TOKEN_INVALID
    else Token Already Revoked (탈취 감지)
        Svc->>Repo: revokeBySubject(subject)
        Note over Repo: 해당 사용자의 모든 토큰 무효화
        Svc-->>C: 401 TOKEN_REVOKED
    else Token Valid
        Svc->>Repo: revoke(oldToken)
        Svc->>TP: issue(newAccessToken)
        Svc->>TP: issue(newRefreshToken)
        Svc->>Repo: save(newRefreshToken)
        Svc-->>C: 200 {accessToken, refreshToken}
    end
```

### Class Diagram - Core Interfaces

```mermaid
classDiagram
    class TokenProvider {
        <<interface>>
        +issue(claims: TokenClaims, config: TokenConfig) String
        +verify(token: String) TokenClaims
        +supportedAlgorithm() Algorithm
    }

    class KeyManager {
        <<interface>>
        +getSigningKey(algorithm: Algorithm) Key
        +getVerificationKey(algorithm: Algorithm) Key
    }

    class RefreshTokenRepository {
        <<interface>>
        +save(token: RefreshToken) RefreshToken
        +findByTokenHash(hash: String) RefreshToken?
        +revokeBySubject(subject: String)
    }

    class Hs256Provider {
        -secretKey: SecretKey
        +issue(claims, config) String
        +verify(token) TokenClaims
    }

    class Rs256Provider {
        -privateKey: RSAPrivateKey
        -publicKey: RSAPublicKey
        +issue(claims, config) String
        +verify(token) TokenClaims
    }

    class V4LocalProvider {
        -secretKey: SecretKey
        +issue(claims, config) String
        +verify(token) TokenClaims
    }

    class V4PublicProvider {
        -privateKey: Ed25519PrivateKey
        -publicKey: Ed25519PublicKey
        +issue(claims, config) String
        +verify(token) TokenClaims
    }

    class EnvironmentKeyManager {
        -keys: Map~Algorithm, Key~
        +getSigningKey(algorithm) Key
        +getVerificationKey(algorithm) Key
    }

    TokenProvider <|.. Hs256Provider
    TokenProvider <|.. Rs256Provider
    TokenProvider <|.. V4LocalProvider
    TokenProvider <|.. V4PublicProvider
    KeyManager <|.. EnvironmentKeyManager

    Hs256Provider --> KeyManager
    Rs256Provider --> KeyManager
    V4LocalProvider --> KeyManager
    V4PublicProvider --> KeyManager
```

### Clean Architecture Layers

```mermaid
flowchart TB
    subgraph Presentation["Presentation Layer"]
        Controller[Controllers]
        ReqDTO[Request DTOs]
        ResDTO[Response DTOs]
    end

    subgraph Application["Application Layer"]
        Service[Services]
        UseCase[Use Cases]
        Port[Ports]
    end

    subgraph Domain["Domain Layer"]
        Entity[Entities]
        VO[Value Objects]
        DomainException[Exceptions]
    end

    subgraph Infrastructure["Infrastructure Layer"]
        Crypto[Crypto Providers]
        Persistence[JPA Repositories]
        Security[Security Filters]
        Config[Configuration]
    end

    Controller --> Service
    Service --> Port
    Port --> Entity

    Crypto -.-> Port
    Persistence -.-> Port
    Security --> Controller

    style Domain fill:#e1f5fe
    style Application fill:#fff3e0
    style Presentation fill:#f3e5f5
    style Infrastructure fill:#e8f5e9
```
