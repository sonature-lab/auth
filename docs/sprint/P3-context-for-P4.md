# Phase 3 → Phase 4 Context Note

**작성일**: 2026-03-14
**작성자**: tsq-librarian
**대상**: Phase 4 진입 시 참조

---

## Phase 3 핵심 결정사항 (Phase 4에서 주의할 것)

### 1. 글로벌 토큰 정책
`tenantId=null` 토큰은 어떤 tenant context에서도 refresh를 허용한다.
이 정책은 User 승인 사항이다 (2026-03-14). Phase 4에서 관리자 토큰 발급 기능을 추가할 때 이 정책과 충돌하지 않도록 주의.

### 2. TenantContext 구조
`TenantContext`는 현재 `slug: String`과 `tenantId: UUID?` 두 필드를 보유한다.
`TenantContextFilter`가 TenantRepository를 주입받아 slug → UUID 변환을 처리한다.
Phase 4에서 TenantContext를 확장할 경우 이 의존 관계를 고려해야 한다.

### 3. OAuth2Client 격리 상태
`findByClientId`(하위 호환)와 tenant-scoped 메서드(`findByClientIdAndTenantId` 등)가 병존한다.
Spring Authorization Server의 `JpaBackedRegisteredClientRepository`는 아직 `findByClientId`를 사용한다.
Phase 4에서 SSO Hub 구현 시 tenant-scoped 클라이언트 조회로 전환 여부를 검토해야 한다.

### 4. Row-level Isolation 패턴
tenant_id 컬럼 기반. RefreshToken, OAuth2Client에 적용 완료.
Phase 4에서 새로운 엔티티 추가 시 동일 패턴 적용 필요 (schema-per-tenant가 아님을 유의).

### 5. Permission 체계
현재 8개 Permission: TENANT_MANAGE, TENANT_DELETE, MEMBER_INVITE, MEMBER_REMOVE, MEMBER_ROLE_CHANGE, API_KEY_MANAGE, AUTH_READ, AUTH_WRITE.
Phase 4 SSO Hub에서 새로운 Permission이 필요할 경우 TenantRole enum의 매핑을 함께 업데이트해야 한다.

---

## Phase 3 완료 시점 기술 스택 현황

| 구성요소 | 버전/상태 |
|---------|---------|
| Spring Boot | 3.5 |
| Kotlin | 1.9 |
| JDK | 21 (Virtual Threads) |
| Spring Authorization Server | 1.4.5 |
| spring-boot-starter-aop | 적용 완료 |
| spring-boot-starter-thymeleaf | 적용 완료 |
| 테스트 | 257개 통과 |

---

## 미해결 항목

- Phase 4+ 는 `auth-enterprise` private repository에서 진행.
- 공개 저장소(`auth`)에서의 Phase 3까지 개발 완료.

---

## Phase 4 진입 전 체크리스트

- [ ] `auth-enterprise` repository 생성 및 초기 설정
- [ ] Phase 3 코드베이스를 base로 enterprise repo 구성
- [ ] Phase 4 스펙 문서 작성 (SSOT)
- [ ] TimSquad Phase 전환 (`tsq wf set-phase P4`)
