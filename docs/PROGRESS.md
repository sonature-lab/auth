# Progress Log

> 일일 진행 기록. 새 항목은 상단에 추가.

---

## 2026-03-14 - Sprint 3.5: Tenant Isolation Hardening (P3-S005)

### Completed
- Domain Layer
  - `TenantMismatchException` — 신규 생성 (cross-tenant token refresh 차단용)
  - `TenantContext` — `tenantId: UUID?` 필드 추가 (런타임 tenant identity)
- Infrastructure Layer
  - `TenantContextFilter` — `TenantRepository` 주입, slug → tenantId 조회 로직 추가
- Application Layer
  - `TokenRefreshUseCase` — cross-tenant validation 로직 추가 (TenantMismatchException throw)
  - 글로벌 토큰(tenantId=null)은 모든 tenant context에서 refresh 허용
- Domain Repository
  - `OAuth2ClientRepository` — 3개 tenant-scoped 쿼리 메서드 추가:
    - `findByClientIdAndTenantId`
    - `findByClientIdAndTenantIdIsNull` (글로벌 client 조회)
    - `findAllByTenantId`
- DB 인덱스
  - `RefreshTokenEntity` — composite index `idx_refresh_tokens_subject_tenant` (subject + tenant_id)
- 테스트 작성
  - `TenantIsolationIntegrationTest` — 5개 엣지케이스 추가 (cross-tenant, global token, mixed revoke, OAuth2Client scoped)
  - `TenantContextFilterTest` — TenantRepository mock 추가 (필터 변경 반영)
  - 총 257개 테스트 전체 통과 (기존 252 + 신규 5)

### Decisions Made
1. **글로벌 토큰 허용 정책 (User 승인)**: `tenantId=null` 토큰은 어떤 tenant context에서도 refresh 허용. 관리자 토큰 등 tenant 비귀속 토큰의 운영 편의성 확보.
2. **OAuth2Client 하위 호환**: 기존 `findByClientId` 메서드 변경 없이 신규 tenant-scoped 메서드 추가.
3. **복합 인덱스**: `revokeAllBySubjectAndTenant` 쿼리 성능을 위해 subject+tenant_id 복합 인덱스 추가.
4. **TenantContext 설계 변경**: `TenantContextFilter`가 TenantRepository에 의존하여 slug → tenantId 조회. Context 객체에 UUID를 직접 보유.

### Changed Files
```
src/main/kotlin/.../tenant/exception/TenantMismatchException.kt (신규)
src/main/kotlin/.../tenant/context/TenantContext.kt (수정)
src/main/kotlin/.../security/TenantContextFilter.kt (수정)
src/main/kotlin/.../usecase/TokenRefreshUseCase.kt (수정)
src/main/kotlin/.../oauth2/repository/OAuth2ClientRepository.kt (수정)
src/main/kotlin/.../refresh/entity/RefreshTokenEntity.kt (수정)
src/test/.../tenant/TenantIsolationIntegrationTest.kt (수정)
src/test/.../security/TenantContextFilterTest.kt (수정)
```

---

## 2026-03-14 - Sprint 3.4: 기존 기능 Tenant 격리

### Completed
- Domain Layer
  - `RefreshTokenEntity` — tenantId (nullable UUID) 컬럼 추가, tenant 인덱스 추가
  - `OAuth2ClientEntity` — tenantId (nullable UUID) 컬럼 추가, tenant 인덱스 추가
  - `RefreshTokenRepository` — revokeAllBySubjectAndTenant() 쿼리 추가
- Application Layer
  - `RefreshTokenService` — storeRefreshToken/rotateToken에 tenantId 전달
  - `TokenRefreshUseCase` — issueTokenPair에 tenantId 파라미터 추가
- 테스트 작성
  - `TenantIsolationIntegrationTest` (7 tests) — tenant 격리, 토큰 회전, cross-tenant 격리
  - 기존 테스트 수정 (AuthServiceTest, OAuth2LoginSuccessHandlerTest mock 파라미터)
  - 총 252개 테스트 전체 통과

### Decisions Made
1. **tenantId nullable**: 하위 호환 유지 — null이면 글로벌 컨텍스트
2. **Token rotation preserves tenantId**: 회전 시 원래 토큰의 tenantId 유지
3. **Tenant-scoped revocation**: 특정 tenant 내 토큰만 revoke 가능 (revokeAllBySubjectAndTenant)
4. **Phase 3 완료**: 공개 저장소에서의 개발 완료, Phase 4+는 private repo (auth-enterprise)에서 진행

---

## 2026-03-14 - Sprint 3.3: Authorization 적용

### Completed
- Infrastructure Layer
  - `TenantContext` + `TenantContextHolder` — ThreadLocal 기반 tenant context
  - `TenantContextFilter` — X-Tenant-Slug 헤더 + Bearer JWT → tenant context 설정
  - `@RequirePermission` annotation + `PermissionAspect` (AOP) — 메서드 레벨 Permission 체크
- Domain Layer
  - `AuthorizationException` sealed class — InsufficientPermission, TenantContextRequired, InvalidAccessToken
- Application Layer
  - `AuthService.buildUserClaims()` — JWT claims에 tenant memberships (slug + role) 포함
- API Layer
  - `TenantController` — addMember(@RequirePermission MEMBER_INVITE), changeMemberRole(MEMBER_ROLE_CHANGE), removeMember(MEMBER_REMOVE)
  - `GlobalExceptionHandler` — 3개 Authorization 예외 핸들러 추가
- Configuration
  - `build.gradle` — spring-boot-starter-aop 추가
  - `AuthorizationServerConfig` — TenantContextFilter 등록
- 테스트 작성
  - `TenantContextFilterTest` (9 tests) — 컨텍스트 설정/해제, 토큰 파싱, edge cases
  - `PermissionAspectTest` (6 tests) — 권한별 proceed/deny 검증
  - `AuthorizationIntegrationTest` (11 tests) — OWNER/ADMIN/MEMBER/VIEWER 권한 E2E
  - `TenantControllerIntegrationTest` 확장 (17 → 19 tests) — 권한 테스트 추가
  - `AuthServiceTest` — tenantMembershipRepository mock 추가
  - 총 245개 테스트 전체 통과

### Decisions Made
1. **ThreadLocal TenantContext**: Virtual Thread 환경에서도 요청 스코프 내 안전 (Spring Boot 3.5 기본 설정)
2. **X-Tenant-Slug 헤더**: 멀티 테넌트 API 호출 시 tenant 식별
3. **JWT claims 구조**: `tenants: [{slug, role}, ...]` 배열 — 다중 테넌트 멤버십 지원
4. **AOP 기반 Permission 체크**: 컨트롤러 코드 깔끔하게 유지, 관심사 분리

---

## 2026-03-14 - Sprint 3.2: Role + Permission 체계

### Completed
- Domain Layer
  - `TenantRole` enum (OWNER, ADMIN, MEMBER, VIEWER) — Permission 매핑 + hasPermission()
  - `Permission` enum (8개: TENANT_MANAGE, TENANT_DELETE, MEMBER_INVITE, MEMBER_REMOVE, MEMBER_ROLE_CHANGE, API_KEY_MANAGE, AUTH_READ, AUTH_WRITE)
  - `TenantMembershipEntity` — role 필드 추가 (default: MEMBER)
- Application Layer
  - `TenantService` — createTenant에 creatorUserId (OWNER 자동 등록), addMember에 role, changeMemberRole, getMemberRole
- API Layer
  - `PUT /api/v1/tenants/{slug}/members/{userId}/role` — 역할 변경 엔드포인트
  - DTOs: ChangeMemberRoleRequest, AddMemberWithRoleRequest, TenantMemberResponse에 role 추가
- 테스트 작성
  - `TenantRolePermissionTest` (25 tests) — Role-Permission 매핑 전체 검증
  - `TenantServiceTest` 확장 (14 → 26 tests)
  - `TenantControllerIntegrationTest` 확장 (11 → 17 tests)
  - 총 217개 테스트 전체 통과

### Decisions Made
1. **Role Hierarchy**: OWNER (8 perms) > ADMIN (7, no TENANT_DELETE) > MEMBER (2: AUTH_READ/WRITE) > VIEWER (1: AUTH_READ)
2. **createTenant에 OWNER 자동 등록**: creatorUserId optional (하위 호환)
3. **Permission 적용은 Sprint 3.3에서**: 여기서는 모델만 정의

---

## 2026-03-14 - Sprint 3.1: Tenant Entity + 기본 관리

### Completed
- Domain Layer
  - `TenantEntity` — name, slug(unique), plan(TenantPlan), status(TenantStatus), timestamps
  - `TenantMembershipEntity` — User ↔ Tenant 다대다 (unique constraint on tenant_id+user_id)
  - `TenantPlan` enum (FREE, PRO, ENTERPRISE)
  - `TenantStatus` enum (ACTIVE, SUSPENDED, DELETED)
  - `TenantException` sealed class (4개: NotFound, SlugAlreadyExists, AlreadyMember, NotMember)
  - `TenantRepository`, `TenantMembershipRepository`
- Application Layer
  - `TenantService` — createTenant, getTenantBySlug, addMember, getMembers, getUserTenants, removeMember
- API Layer
  - `TenantController` — POST/GET tenants, POST/GET/DELETE members
  - DTOs: CreateTenantRequest, TenantResponse, AddMemberRequest, TenantMemberResponse
  - `GlobalExceptionHandler` — Tenant 예외 핸들러 4개 추가
- 테스트 작성
  - `TenantServiceTest` (14 tests) — 단위 테스트
  - `TenantControllerIntegrationTest` (11 tests) — 통합 테스트
  - `SocialLoginIntegrationTest` — FK constraint 수정 (tenantMembership 삭제 순서)
  - 총 178개 테스트 전체 통과

### Decisions Made
1. **TenantMembership**: 중간 테이블 패턴 (Sprint 3.2에서 Role 필드 추가 예정)
2. **slug**: URL-safe 식별자, unique 인덱스
3. **기존 엔티티 무변경**: tenant_id 추가는 Sprint 3.4에서 처리
4. **SecurityConfig**: /api/v1/** permitAll 이미 적용됨 (별도 수정 불필요)

---

## 2026-03-14 - Sprint 2.4: Consent UI + Scope 관리

### Completed
- Infrastructure Layer
  - `ConsentController` — GET /oauth2/consent 화면 렌더링
  - `consent.html` — Thymeleaf 최소 UI (scope 표시, 승인/거부)
  - `AuthorizationServerConfig` — consent page 연결 (.consentPage("/oauth2/consent"))
  - `DevDataInitializer` — 테스트 클라이언트에 auth:read, auth:write scope 추가
- Domain Layer
  - `ScopeDefinition` — scope별 한글/영문 설명 매핑 (openid, profile, email, auth:read, auth:write)
- Build
  - `spring-boot-starter-thymeleaf` 의존성 추가
- 테스트 작성
  - `ScopeDefinitionTest` (12 tests)
  - `ConsentControllerIntegrationTest` (4 tests)
  - 총 149개 테스트 전체 통과

### Decisions Made
1. **Consent UI**: Thymeleaf 최소 UI (인라인 CSS, 외부 프레임워크 없음)
2. **Scope 정의**: ScopeDefinition data class + companion object (5개 scope)
3. **커스텀 scope**: auth:read (인증 정보 읽기), auth:write (인증 정보 수정)
4. **Consent 처리**: Spring Authorization Server 기본 메커니즘 활용 (POST /oauth2/authorize)

### Notes
- Phase 2 전체 완료 (Sprint 2.1~2.4)
- 다음: Phase 3 (Multi-tenant + RBAC)

---

## 2026-03-14 - Sprint 2.3: Social Login (Google, GitHub, Kakao)

### Completed
- Infrastructure Layer
  - `CustomOAuth2UserService` — 소셜 로그인 시 사용자 자동 생성/연동
  - `OAuth2UserProfileMapper` — Provider별 프로필 매핑 (Google, GitHub, Kakao)
  - `OAuth2UserProfile` — 소셜 프로필 VO
  - `OAuth2LoginSuccessHandler` — 로그인 성공 시 JWT 토큰 쌍 발급
- Domain Layer
  - `UserEntity.provider`, `UserEntity.providerId` — val → var 변경 (계정 연동 지원)
- Configuration
  - `application.yml` — OAuth2 Client Registration (Google, GitHub, Kakao) 환경변수 기반
  - `application-test.yml` — 테스트용 OAuth2 Client 설정
  - `AuthorizationServerConfig` — `.oauth2Login()` 추가
- Build
  - `spring-boot-starter-oauth2-client` 의존성 추가
- 테스트 작성
  - `OAuth2UserProfileMapperTest` (7 tests)
  - `CustomOAuth2UserServiceTest` (4 tests)
  - `OAuth2LoginSuccessHandlerTest` (2 tests)
  - `SocialLoginIntegrationTest` (6 tests)
  - 총 133개 테스트 전체 통과

### Decisions Made
1. **프로필 매핑**: OAuth2UserProfileMapper (object) — Strategy 패턴으로 provider별 분리
2. **계정 연동**: 동일 email LOCAL 계정 자동 연결 (provider/providerId 업데이트)
3. **email 필수**: 소셜 로그인 시 email 미제공 → OAuth2AuthenticationException
4. **토큰 발급**: 소셜 로그인 성공 → JSON 응답 (accessToken + refreshToken)
5. **환경변수**: GOOGLE_CLIENT_ID/SECRET, GITHUB_CLIENT_ID/SECRET, KAKAO_CLIENT_ID/SECRET

### Environment Variables (New)
| Variable | Description |
|----------|-------------|
| `GOOGLE_CLIENT_ID` | Google OAuth2 Client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 Client Secret |
| `GITHUB_CLIENT_ID` | GitHub OAuth2 Client ID |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth2 Client Secret |
| `KAKAO_CLIENT_ID` | Kakao OAuth2 Client ID |
| `KAKAO_CLIENT_SECRET` | Kakao OAuth2 Client Secret |

---

## 2026-03-13 - Sprint 2.2: OAuth2 Authorization Server

### Completed
- Infrastructure Layer
  - `AuthorizationServerConfig` — OAuth2 Authorization Server 핵심 설정
  - Authorization Code + PKCE flow
  - OIDC 지원 (Discovery endpoint)
  - JWK Set 자동 생성 (RSA 2048-bit)
  - `JpaBackedRegisteredClientRepository` — JPA 기반 클라이언트 저장소
  - `UserDetailsConfig` — Spring Security UserDetailsService (DB 연동)
  - `DevDataInitializer` — dev/test 환경 자동 클라이언트 등록
- Domain Layer
  - `OAuth2ClientEntity` — OAuth2 클라이언트 등록 정보
  - `OAuth2ClientRepository`
- Build
  - `spring-security-oauth2-authorization-server:1.4.5` 의존성 추가
- 테스트 작성
  - `OAuth2AuthorizationServerTest` (7 tests) — 통합 테스트
  - 총 114개 테스트 전체 통과

### Decisions Made
1. **Authorization Server**: Spring Authorization Server 1.4.5 (Spring Boot 3.5.6 호환)
2. **PKCE**: public client(SPA/Mobile)은 필수, confidential client는 선택
3. **JWK**: 매 시작 시 RSA 2048-bit 자동 생성 (향후 영구 키 전환 예정)
4. **Client 저장**: JPA 기반 (OAuth2ClientEntity → RegisteredClient 변환)
5. **SecurityConfig 분리**: AuthorizationServer(@Order(1)) + Default(@Order(2))

### Dev 테스트 클라이언트
- `sonature-dev-client` — Public (PKCE required), redirect: localhost:3000,5173
- `sonature-backend-client` — Confidential, redirect: localhost:8081

---

## 2026-03-13 - Sprint 2.1: User Entity + 기본 인증

### Completed
- Domain Layer
  - `UserEntity` (email, passwordHash, name, provider, status)
  - `AuthProvider` enum (LOCAL, GOOGLE, GITHUB, KAKAO)
  - `UserStatus` enum (ACTIVE, SUSPENDED, DELETED)
  - `UserRepository` (findByEmail, existsByEmail)
  - `AuthException` sealed class (EmailAlreadyExists, InvalidCredentials, UserNotFound, UserSuspended)
- Application Layer
  - `AuthService` (signup, login) — JWT 발급 연동
- API Layer
  - `AuthController` (POST /api/v1/auth/signup, POST /api/v1/auth/login)
  - DTOs: SignupRequest, LoginRequest, AuthResponse, UserInfo
- Infrastructure
  - `SecurityConfig` — PasswordEncoder Bean 추가 (DelegatingPasswordEncoder)
  - `GlobalExceptionHandler` — Auth 예외 핸들러 4개 추가
- 테스트 작성
  - `AuthServiceTest` (6 tests) — 단위 테스트
  - `AuthControllerIntegrationTest` (7 tests) — 통합 테스트
  - 총 107개 테스트 전체 통과

### Decisions Made
1. **Password hashing**: DelegatingPasswordEncoder (bcrypt 기본, 향후 Argon2 전환 가능)
2. **User-Token 연동**: signup/login 시 user.id를 JWT subject로 사용
3. **Custom claims**: email, provider, name을 토큰에 포함
4. **Validation**: email 형식, 비밀번호 8~128자 제한

---

## 2026-01-29 (Day 5) - PASETO v4.local Implementation

### Completed
- Infrastructure Layer
  - `PasetoV4LocalProvider` 구현 (paseto4j-version4:2024.3)
  - XChaCha20-Poly1305 기반 대칭키 암호화
  - TokenProvider 인터페이스 구현
- Application Layer
  - `PasetoService` 구현 (issueToken, verifyToken)
  - JwtService와 동일한 패턴 적용
- API Layer
  - `PasetoController` (/api/v1/paseto/issue, /api/v1/paseto/verify)
  - Request/Response DTOs (PasetoIssueRequest, PasetoVerifyRequest 등)
- 테스트 작성
  - `PasetoV4LocalProviderTest` (9 tests) - 단위 테스트
  - `PasetoControllerIntegrationTest` (7 tests) - 통합 테스트
  - 총 94개 테스트 전체 통과 (Day 4: 78 + Day 5: 16)

### Decisions Made
1. **라이브러리 선택**: jpaseto 0.7.0은 v4 미지원, paseto4j-version4:2024.3 사용
2. **토큰 형식**: `v4.local.` 접두사로 PASETO 토큰 식별
3. **키 설정**: 32바이트 Base64 인코딩 시크릿 키 (XChaCha20)

### Notes
- 테스트: 94개 전체 통과
- PASETO는 JWT와 달리 암호화된 페이로드 사용 (더 안전)
- Week 1 Core Token Infrastructure 완료!

### Tomorrow (Day 6)
- [ ] PASETO v4.public 구현 (Ed25519)
- [ ] KeyManager Ed25519 키 로딩
- [ ] PasetoController mode 선택

---

## 2026-01-29 (Day 4) - Refresh Token Implementation

### Completed
- Domain Layer
  - `RefreshTokenEntity` JPA Entity (인덱스 포함)
  - `RefreshTokenRepository` Spring Data JPA Repository
  - `TokenPair` Value Object (Access + Refresh)
  - 3개 예외 추가 (RefreshTokenRevokedException, RefreshTokenReusedException, InvalidTokenTypeException)
- Application Layer
  - `RefreshTokenService` 구현 (storeRefreshToken, validateAndConsume, rotateToken, revokeAllTokensForSubject)
  - `TokenRefreshUseCase` 구현 (issueTokenPair, refreshTokens)
  - JwtService 확장 (issueTokenPair, issueRefreshToken, verifyTokenWithType)
- API Layer
  - `JwtController` 확장 (/issue-pair, /refresh)
  - Request/Response DTOs (JwtRefreshRequest, JwtRefreshResponse, JwtTokenPairResponse)
  - `GlobalExceptionHandler` 확장 (3개 핸들러 추가)
- 테스트 작성
  - `JwtRefreshIntegrationTest` (8 tests) - 통합 테스트
  - 총 78개 테스트 전체 통과 (Day 3: 70 + Day 4: 8)

### Decisions Made
1. **Token Rotation**: Refresh 시 새 토큰 발급, 기존 토큰 즉시 폐기
2. **탈취 감지**: 이미 사용된 토큰 재사용 시 해당 subject의 모든 토큰 무효화
3. **DB 저장**: SHA-256 해시로 토큰 저장 (원본 토큰 노출 방지)
4. **트랜잭션**: `REQUIRES_NEW` 전파로 탈취 감지 시 독립 커밋 보장
5. **Self-Injection**: `ObjectProvider`로 같은 클래스 내 프록시 호출 해결

### Notes
- 테스트: 78개 전체 통과
- 복잡한 트랜잭션 경계 문제 해결 (예외 발생 시에도 revoke 커밋 유지)

### Tomorrow (Day 5)
- [x] PASETO v4.local 구현
- [x] V4LocalProvider 구현
- [x] PasetoService + PasetoController

---

## 2026-01-29 (Day 3) - JWT RS256 Implementation

### Completed
- Crypto Layer
  - `Rs256Provider` 구현 (RSA 비대칭키 서명)
  - RSA PrivateKey로 서명, PublicKey로 검증
  - HS256과 동일한 예외 매핑 패턴 적용
- Infrastructure
  - 테스트용 RSA 2048-bit 키 쌍 생성 (`src/test/resources/keys/`)
  - `application-test.yml` 생성 (RS256 키 설정 포함)
- API Integration
  - algorithm 파라미터로 HS256/RS256 자동 선택
  - JwtService가 Spring DI로 모든 TokenProvider 자동 통합
- 테스트 작성
  - `Rs256ProviderTest` (9 tests) - 단위 테스트
  - `JwtControllerIntegrationTest` RS256 테스트 추가 (4 tests)
  - 총 70개 테스트 전체 통과 (Day 2: 57 + Day 3: 13)

### Decisions Made
1. **키 로딩**: PEM 형식 RSA 키를 YAML multiline string으로 직접 설정
2. **테스트 키**: 테스트 전용 RSA 키 별도 생성 (프로덕션 키와 분리)
3. **알고리즘 선택**: API 요청의 algorithm 파라미터로 동적 선택

### Notes
- 테스트: 70개 전체 통과
- RS256은 HS256보다 서명/검증 속도가 느리지만 키 분리 보안에 유리

### Tomorrow (Day 4)
- [ ] Refresh Token 구현
- [ ] Token Refresh Flow
- [ ] Refresh Token 저장소 (In-Memory 또는 H2)

---

## 2026-01-29 (Day 2) - JWT HS256 Implementation

### Completed
- Crypto Layer
  - `Hs256Provider` 구현 (jjwt 0.12.6)
  - JWT 토큰 발급/검증 기능
  - 예외 매핑 (ExpiredJwtException → TokenExpiredException 등)
- Application Layer
  - `JwtService` 구현 (issueAccessToken, verifyToken)
  - TimeProvider, IdGenerator 통합
- API Layer
  - `JwtController` (/api/v1/jwt/issue, /api/v1/jwt/verify)
  - Request/Response DTOs (JwtIssueRequest, JwtVerifyRequest 등)
  - `ApiResponse`, `ApiError` 공통 응답 모델
  - `GlobalExceptionHandler` 전역 예외 처리
- Infrastructure
  - `SecurityConfig` (API 엔드포인트 permitAll)
  - MockK 라이브러리 추가 (build.gradle)
- 테스트 작성
  - `Hs256ProviderTest` (9 tests)
  - `JwtServiceTest` (6 tests)
  - `JwtControllerIntegrationTest` (7 tests)
  - 총 57개 테스트 전체 통과 (Day 1: 35 + Day 2: 22)

### Decisions Made
1. **jjwt 0.12.x API**: `Jwts.parser().verifyWith()` 패턴 사용
2. **API 응답**: `ApiResponse<T>` 래퍼로 일관된 응답 형식
3. **예외 처리**: `GlobalExceptionHandler`에서 중앙 집중 처리
4. **보안**: Day 2는 API Key 인증 없이 기능 구현에 집중

### Notes
- 테스트: 57개 전체 통과
- API 응답 시간: 토큰 발급 < 10ms

### Tomorrow (Day 3)
- [ ] Rs256Provider 구현 (RSA 비대칭키)
- [ ] KeyManager RS256 키 로딩 검증
- [ ] algorithm 파라미터로 HS256/RS256 선택
- [ ] RS256 단위 테스트

---

## 2026-01-29 (Day 1) - Foundation & Clean Architecture

### Completed
- 패키지 구조 마이그레이션
  - `com.example.demo` → `com.sonature.auth`
  - Clean Architecture 폴더 구조 적용
- Domain Layer 구현
  - `Algorithm` enum (HS256, RS256, PASETO_V4_LOCAL, PASETO_V4_PUBLIC)
  - `TokenType` enum (ACCESS, REFRESH)
  - `TokenClaims` Value Object (iss, sub, aud, exp, iat, jti)
  - `Token` Value Object
  - `TokenConfig` 설정 클래스
  - `TokenException` sealed class 계층 (Expired, Invalid, Malformed, UnsupportedAlgorithm)
- Application Layer - Port 인터페이스
  - `TokenProvider` 인터페이스 (issue, verify, supportedAlgorithm)
  - `KeyManager` 인터페이스 (getSigningKey, getVerificationKey, hasKey)
- Infrastructure Layer
  - `KeyConfig` ConfigurationProperties
  - `EnvironmentKeyManager` 구현 (HS256, RS256, PASETO 키 로딩)
- Common Utilities
  - `TimeProvider` 인터페이스 + `SystemTimeProvider`
  - `IdGenerator` 인터페이스 + `UuidGenerator`
- 테스트 작성
  - `AlgorithmTest` (6 tests)
  - `TokenClaimsTest` (12 tests)
  - `TokenConfigTest` (8 tests)
  - `EnvironmentKeyManagerTest` (9 tests)
  - 총 35개 테스트 전체 통과

### Decisions Made
1. **Domain 모델**: Kotlin data class 기반 Value Object 패턴
2. **예외 처리**: sealed class로 타입 안전한 예외 계층 구현
3. **키 관리**: 환경변수 기반 (Base64 인코딩)
4. **테스트 전략**: 각 Domain 모델별 상세 단위 테스트

### Notes
- 빌드 시간: 13초 (clean build)
- 테스트 실행 시간: 0.3초
- 애플리케이션 시작 시간: 1.3초

### Tomorrow (Day 2)
- [ ] JWT HS256 Provider 구현 (Hs256Provider)
- [ ] JwtService 구현 (issue, verify)
- [ ] Provider 단위 테스트 작성
- [ ] Service 통합 테스트 작성

---

## 2026-01-28 (Day 0) - Project Setup

### Completed
- Gradle 의존성 설정
  - JWT: jjwt 0.12.6
  - PASETO: jpaseto 0.7.0
  - Monitoring: micrometer-prometheus
  - Testing: JaCoCo
- JDK 21 설정 (ARM64)
- PRD 작성 완료 (docs/PRD.md)
  - 17개 섹션
  - JWT/PASETO 스펙
  - API 설계
  - 에러 코드 체계
  - Rate Limiting 정책
  - Refresh Token 저장 전략
  - API Key 관리
  - 키 생성 스크립트
- 구현 플랜 작성
  - TDD + Clean Architecture 전략
  - 2주 상세 일정
  - Clean Architecture 폴더 구조
- 문서 정책 수립
  - PRD 템플릿 생성 (.claude/templates/)
  - 상태 추적 문서 (STATUS.md, PROGRESS.md, ROADMAP.md)

### Decisions Made
1. **개발 전략**: TDD + Clean Architecture 하이브리드
   - 보안 크리티컬한 토큰 처리에 TDD 필수
   - 향후 암호 서버 분리 대비 인터페이스 설계
2. **Refresh Token 저장**: PostgreSQL (MVP), Redis (향후)
3. **API Key 관리**: 환경변수 (MVP), DB (향후)

### Notes
- OAuth2.1 스코프에서 토큰 프레임워크로 방향 전환
- 원래 스코프는 docs/archive/original-scope.md에 보관

### Tomorrow (Day 1)
- [ ] 패키지 구조 변경
- [ ] Domain 모델 정의
- [ ] Port 인터페이스 정의
- [ ] KeyManager 구현 시작

---

<!--
## Template

## YYYY-MM-DD (Day N) - Theme

### Completed
- Task 1
- Task 2

### In Progress
- Task 3

### Blocked
- Task 4 (reason)

### Decisions Made
1. Decision 1

### Notes
- Note 1

### Tomorrow
- [ ] Task 5
-->
