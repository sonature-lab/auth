#!/bin/bash
#
# Sonature Auth - Key Generation Script
#
# 이 스크립트는 JWT/PASETO 토큰 서명에 필요한 암호화 키를 생성합니다.
# 생성된 키는 환경변수로 설정하여 사용합니다.
#
# 사용법:
#   chmod +x scripts/generate-keys.sh
#   ./scripts/generate-keys.sh
#
# 요구사항:
#   - OpenSSL 1.1.1+ (Ed25519 지원)
#

set -e

OUTPUT_DIR="${1:-.}"
mkdir -p "$OUTPUT_DIR"

echo "========================================"
echo "  Sonature Auth - Key Generator"
echo "========================================"
echo ""

# ========================================
# 1. JWT HS256 Secret (HMAC-SHA256)
# ========================================
echo "[1/4] Generating HS256 Secret..."
HS256_SECRET=$(openssl rand -base64 32)
echo "JWT_HS256_SECRET=$HS256_SECRET"
echo ""

# ========================================
# 2. JWT RS256 Key Pair (RSA-SHA256)
# ========================================
echo "[2/4] Generating RS256 Key Pair..."
RS256_PRIVATE_KEY_FILE="$OUTPUT_DIR/rs256-private.pem"
RS256_PUBLIC_KEY_FILE="$OUTPUT_DIR/rs256-public.pem"

openssl genrsa -out "$RS256_PRIVATE_KEY_FILE" 2048 2>/dev/null
openssl rsa -in "$RS256_PRIVATE_KEY_FILE" -pubout -out "$RS256_PUBLIC_KEY_FILE" 2>/dev/null

echo "  Private Key: $RS256_PRIVATE_KEY_FILE"
echo "  Public Key:  $RS256_PUBLIC_KEY_FILE"
echo ""

# ========================================
# 3. PASETO v4.public Key Pair (Ed25519)
# ========================================
echo "[3/4] Generating Ed25519 Key Pair (PASETO v4.public)..."
ED25519_PRIVATE_KEY_FILE="$OUTPUT_DIR/ed25519-private.pem"
ED25519_PUBLIC_KEY_FILE="$OUTPUT_DIR/ed25519-public.pem"

openssl genpkey -algorithm Ed25519 -out "$ED25519_PRIVATE_KEY_FILE" 2>/dev/null
openssl pkey -in "$ED25519_PRIVATE_KEY_FILE" -pubout -out "$ED25519_PUBLIC_KEY_FILE" 2>/dev/null

echo "  Private Key: $ED25519_PRIVATE_KEY_FILE"
echo "  Public Key:  $ED25519_PUBLIC_KEY_FILE"
echo ""

# ========================================
# 4. PASETO v4.local Secret (XChaCha20-Poly1305)
# ========================================
echo "[4/4] Generating PASETO v4.local Secret..."
PASETO_SECRET=$(openssl rand -base64 32)
echo "PASETO_SECRET_KEY=$PASETO_SECRET"
echo ""

# ========================================
# Summary
# ========================================
echo "========================================"
echo "  Generation Complete!"
echo "========================================"
echo ""
echo "환경변수 설정 예시 (.env 파일):"
echo ""
echo "# JWT"
echo "JWT_HS256_SECRET=$HS256_SECRET"
echo "JWT_RS256_PRIVATE_KEY_FILE=$RS256_PRIVATE_KEY_FILE"
echo "JWT_RS256_PUBLIC_KEY_FILE=$RS256_PUBLIC_KEY_FILE"
echo ""
echo "# PASETO"
echo "PASETO_SECRET_KEY=$PASETO_SECRET"
echo "PASETO_PRIVATE_KEY_FILE=$ED25519_PRIVATE_KEY_FILE"
echo "PASETO_PUBLIC_KEY_FILE=$ED25519_PUBLIC_KEY_FILE"
echo ""
echo "# API Keys (콤마로 구분)"
echo "API_KEYS=sk_live_$(openssl rand -hex 12),sk_test_$(openssl rand -hex 12)"
echo ""
echo "========================================"
echo ""
echo "보안 주의사항:"
echo "  - 생성된 키 파일(.pem)은 안전한 곳에 보관하세요"
echo "  - 절대로 Git에 커밋하지 마세요 (.gitignore에 추가)"
echo "  - 프로덕션에서는 시크릿 매니저 사용을 권장합니다"
echo ""
