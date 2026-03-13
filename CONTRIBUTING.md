# Contributing to Sonature Auth

Sonature Auth에 기여해 주셔서 감사합니다!

## Development Setup

### 요구사항

- JDK 21+
- Gradle 8+
- OpenSSL 1.1.1+ (키 생성용)
- (Optional) Docker

### 환경 설정

```bash
# 1. 저장소 클론
git clone https://github.com/sonature/auth.git
cd auth

# 2. 키 생성
chmod +x scripts/generate-keys.sh
./scripts/generate-keys.sh

# 3. 환경변수 설정 (개발용)
export JWT_HS256_SECRET=$(openssl rand -base64 32)
export API_KEYS=sk_test_development

# 4. 빌드 및 테스트
./gradlew build

# 5. 실행
./gradlew bootRun
```

### IDE 설정

**IntelliJ IDEA (권장)**
1. `File > Open` 에서 프로젝트 폴더 선택
2. Gradle import 자동 실행
3. JDK 21 설정 확인: `File > Project Structure > Project SDK`

## Branch Strategy

```
main           # 프로덕션 릴리스
├── develop    # 개발 통합 브랜치
│   ├── feature/xxx    # 기능 개발
│   ├── fix/xxx        # 버그 수정
│   └── docs/xxx       # 문서 작업
└── release/x.y.z      # 릴리스 준비
```

### 브랜치 네이밍

| 유형 | 패턴 | 예시 |
|------|------|------|
| 기능 | `feature/<description>` | `feature/jwt-rs256` |
| 버그 | `fix/<description>` | `fix/token-expiration` |
| 문서 | `docs/<description>` | `docs/api-examples` |
| 리팩토링 | `refactor/<description>` | `refactor/clean-architecture` |

## Commit Convention

[Conventional Commits](https://www.conventionalcommits.org/) 형식을 따릅니다.

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type

| Type | 설명 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서 변경 |
| `style` | 코드 포맷팅 (동작 변경 없음) |
| `refactor` | 리팩토링 |
| `test` | 테스트 추가/수정 |
| `chore` | 빌드, 설정 변경 |

### 예시

```
feat(jwt): add RS256 algorithm support

- Implement Rs256Provider with jjwt library
- Add RSA key loading to KeyManager
- Update JwtController to accept algorithm parameter

Closes #123
```

## Pull Request Guide

### PR 전 체크리스트

- [ ] `./gradlew test` 통과
- [ ] `./gradlew jacocoTestReport` - 커버리지 80%+ 유지
- [ ] 새 기능은 테스트 포함
- [ ] CHANGELOG.md 업데이트 (Unreleased 섹션)
- [ ] 문서 업데이트 (필요시)

### PR 템플릿

```markdown
## Summary
변경 사항 요약

## Changes
- 변경 1
- 변경 2

## Test Plan
- [ ] 테스트 1
- [ ] 테스트 2

## Related Issues
Closes #xxx
```

## Code Style

### Kotlin

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) 준수
- 4 spaces 들여쓰기
- 최대 줄 길이: 120자

### 네이밍

```kotlin
// 클래스: PascalCase
class JwtService

// 함수/변수: camelCase
fun issueToken()
val accessToken

// 상수: SCREAMING_SNAKE_CASE
const val DEFAULT_EXPIRATION = 900000L

// 패키지: 소문자
package com.sonature.auth.domain.token
```

### 테스트 네이밍

```kotlin
// Unit Test: *Test.kt
class JwtServiceTest

// Integration Test: *IntegrationTest.kt
class JwtControllerIntegrationTest

// 테스트 메소드: backticks 허용
@Test
fun `should issue valid JWT token with HS256`() { }
```

## Architecture Guidelines

### Clean Architecture 원칙

```
presentation → application → domain ← infrastructure
```

- **domain**: 비즈니스 로직, 외부 의존성 없음
- **application**: Use Case, Service, Port 인터페이스
- **infrastructure**: 외부 시스템 연동 (DB, Crypto)
- **presentation**: API Controller, DTO

### 의존성 규칙

- Domain은 다른 레이어에 의존하지 않음
- Application은 Domain에만 의존
- Infrastructure와 Presentation은 Application에 의존

## Questions?

- Issue를 통해 질문해 주세요
- 기존 Issue/PR을 먼저 검색해 주세요

감사합니다!
