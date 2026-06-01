#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8083}"
WEB_BASE="${WEB_BASE:-http://localhost:5174}"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

json_get() {
  local url="$1"
  curl -fsS "$url" -H 'Accept: application/json'
}

json_post() {
  local url="$1"
  local body="$2"
  curl -fsS "$url" \
    -H 'Accept: application/json' \
    -H 'Content-Type: application/json' \
    -d "$body"
}

assert_json_expr() {
  local file="$1"
  local expr="$2"
  local message="$3"
  node -e "
const fs = require('fs');
const data = JSON.parse(fs.readFileSync(process.argv[1], 'utf8'));
if (!($expr)) {
  console.error(process.argv[2]);
  process.exit(1);
}
" "$file" "$message"
}

home_json="$tmp_dir/home.json"
json_get "$API_BASE/api/public/home" > "$home_json"
assert_json_expr "$home_json" "data.success === true && data.data.banners.length >= 1 && data.data.hotHomestays.length >= 1" "home API did not return demo data"

login_json="$tmp_dir/login.json"
json_post "$API_BASE/api/auth/login" '{"username":"admin","password":"admin123"}' > "$login_json"
assert_json_expr "$login_json" "data.success === true && data.data.token && data.data.user.role === 'ADMIN'" "admin login failed"
token="$(node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1], 'utf8')); process.stdout.write(data.data.token);" "$login_json")"

dashboard_json="$tmp_dir/dashboard.json"
curl -fsS "$API_BASE/api/admin/dashboard" \
  -H 'Accept: application/json' \
  -H "Authorization: Bearer $token" > "$dashboard_json"
assert_json_expr "$dashboard_json" "data.success === true && Array.isArray(data.data.orderTrend) && data.data.todos" "admin dashboard API failed"

proxy_json="$tmp_dir/proxy-home.json"
json_get "$WEB_BASE/api/public/home" > "$proxy_json"
assert_json_expr "$proxy_json" "data.success === true && data.data.notices.length >= 1" "frontend proxy did not reach backend"

curl -fsS "$WEB_BASE/" -H 'Accept: text/html' | grep -q '<div id="app">'

echo "Smoke test passed:"
echo "- API: $API_BASE"
echo "- Web: $WEB_BASE"
