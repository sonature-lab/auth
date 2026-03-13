# Roadmap

> MVP 체크리스트 및 향후 계획

---

## MVP (2 Weeks)

### Week 1: Core Token Infrastructure

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

---

### Week 2: Production Ready

#### Day 6 - PASETO v4.public
- [ ] V4PublicProvider (Ed25519)
- [ ] KeyManager Ed25519 키 로딩
- [ ] PasetoController mode 선택
- [ ] OpenAPI 문서화

#### Day 7 - Security & E2E
- [ ] ApiKeyAuthenticationFilter
- [ ] RateLimitFilter
- [ ] 에러 코드 체계 완성
- [ ] E2E 테스트

#### Day 8-9 - TypeScript SDK
- [ ] SDK 프로젝트 초기화 (tsup)
- [ ] types.ts, client.ts
- [ ] jwt.ts, paseto.ts
- [ ] errors.ts
- [ ] 빌드 (ESM + CJS)
- [ ] README + 예제

#### Day 10 - Docker & Monitoring
- [ ] Dockerfile (multi-stage)
- [ ] docker-compose.yml
- [ ] Prometheus 메트릭
- [ ] Grafana 대시보드
- [ ] Health check 검증

#### Day 11-12 - Deployment & Docs
- [ ] OCI 인스턴스 설정
- [ ] 환경변수 설정
- [ ] 애플리케이션 배포
- [ ] TLS 설정
- [ ] API 문서 완성
- [ ] README 업데이트
- [ ] generate-keys.sh 스크립트
- [ ] CHANGELOG, LICENSE

#### Day 13-14 - Testing & Release
- [ ] 전체 테스트 + 커버리지 80%+
- [ ] 버그 수정
- [ ] 성능 테스트 (p99 < 10ms)
- [ ] 코드 리뷰 + 리팩토링
- [ ] SDK npm 배포 준비
- [ ] GitHub 릴리스 (v0.1.0)
- [ ] 프로덕션 최종 검증

---

## Post-MVP

### P1 - Next Version
- [ ] JWE 지원 (암호화된 JWT)
- [ ] HTTP/3 (QUIC)
- [ ] Redis로 Refresh Token 저장 전환
- [ ] API Key DB 관리

### P2 - Important
- [ ] 암호 알고리즘 서버 분리
- [ ] 키 관리 서비스 (로테이션, 버전 관리)
- [ ] API Key별 Rate Limit
- [ ] 사용량 통계

### P3 - Nice to Have
- [ ] 라이선스 인증 (상용 기능)
- [ ] 토큰 family 개념
- [ ] 멀티 테넌시

---

## Milestones

| Version | Target | Status |
|---------|--------|--------|
| v0.1.0 | MVP Release | In Progress |
| v0.2.0 | JWE + HTTP/3 | Planned |
| v0.3.0 | Key Management | Planned |
| v1.0.0 | Production Stable | Planned |
