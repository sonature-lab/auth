# Original Scope (Archived)

이 문서는 프로젝트 방향 전환 전 원래 스코프를 기록합니다.
변경일: 2026-01-28

---

## 원래 프로젝트 개요

OAuth2.1 / JWT-based **Authentication and Authorization service**.
Sonature 마이크로서비스 아키텍처를 위한 중앙 집중식 ID 및 접근 관리 솔루션.

## 원래 Features

- OAuth2.1 Authorization Server
- JWT Access / Refresh Token issuance
- User Registration / Login API
- Role-Based Access Control (RBAC)

## 원래 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| /api/auth/signup | POST | 사용자 등록 |
| /api/auth/login | POST | 로그인 및 JWT 토큰 발급 |
| /api/auth/refresh | POST | Refresh 토큰으로 새 Access 토큰 발급 |
| /api/auth/logout | POST | 로그아웃 |

## 원래 Security

- 비밀번호: BCrypt 해싱
- JWT 서명: RSA private/public key pair

## 제거된 의존성

- `spring-boot-starter-oauth2-authorization-server`

---

## 변경 이유

범용 JWT/PASETO 토큰 프레임워크로 방향 전환.
오픈소스 공개 목적으로 더 단순하고 범용적인 토큰 발급/검증 서비스로 재정의.

새 스코프는 [PRD.md](../PRD.md) 참조.
