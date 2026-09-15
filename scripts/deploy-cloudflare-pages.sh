#!/bin/zsh

set -euo pipefail

SCRIPT_DIR="${0:A:h}"
REPO_DIR="${SCRIPT_DIR:h}"
BRANCH="${1:-main}"
LOCAL_CONFIG="$REPO_DIR/scripts/cloudflare-pages/.env.local"
if [[ -f "$LOCAL_CONFIG" ]]; then
  source "$LOCAL_CONFIG"
fi
PAGES_PROJECT="${PAGES_PROJECT:-}"
PUBLIC_DEMO_URL="${PUBLIC_DEMO_URL:-}"
BACKEND_ORIGIN="${BACKEND_ORIGIN:-}"
if [[ -z "$PAGES_PROJECT" || ! "$PAGES_PROJECT" =~ '^[a-z0-9][a-z0-9-]*$' ]]; then
  print -u2 "请在本地配置或环境变量中显式设置合法的 PAGES_PROJECT。"
  exit 1
fi
if [[ -n "$PUBLIC_DEMO_URL" && "$PUBLIC_DEMO_URL" != https://* ]]; then
  print -u2 "PUBLIC_DEMO_URL 必须使用 HTTPS。"
  exit 1
fi
if [[ -n "$BACKEND_ORIGIN" && ! "$BACKEND_ORIGIN" =~ '^https://[A-Za-z0-9.-]+(:[0-9]+)?$' ]]; then
  print -u2 "BACKEND_ORIGIN 必须是 HTTPS 源站地址，不含路径、查询参数或末尾斜杠。"
  exit 1
fi
WRANGLER_VERSION="4.129.0"
WORKER_TEMPLATE="$REPO_DIR/scripts/cloudflare-pages/_worker.template.js"

cd "$REPO_DIR"

if [[ ! "$BRANCH" =~ '^[A-Za-z0-9._/-]+$' ]]; then
  print -u2 "部署分支名不合法：$BRANCH"
  exit 1
fi

for required_command in npm curl sed; do
  if ! command -v "$required_command" >/dev/null 2>&1; then
    print -u2 "缺少命令：$required_command"
    exit 1
  fi
done

if [[ -n "$BACKEND_ORIGIN" ]]; then
  # A configured cloud backend must never silently fall back to this computer.
  curl -fsS --max-time 20 "$BACKEND_ORIGIN/api/businesses/search" >/dev/null
else
for required_command in docker dig rg; do
  if ! command -v "$required_command" >/dev/null 2>&1; then
    print -u2 "缺少命令：$required_command"
    exit 1
  fi
done
if ! docker info >/dev/null 2>&1; then
  print -u2 "Docker Desktop 尚未启动。"
  exit 1
fi

./scripts/ensure-demo-env.sh

# Build the newest application and always create a fresh backend tunnel. Quick
# Tunnel hostnames expire; Pages remains the stable public entry point.
docker compose -f docker-compose.demo.yml --profile share up -d --build mysql backend frontend
docker compose -f docker-compose.demo.yml --profile share rm -sf tunnel >/dev/null 2>&1 || true
docker compose -f docker-compose.demo.yml --profile share up -d tunnel

BACKEND_ORIGIN=""
tunnel_is_reachable() {
  local tunnel_host="${BACKEND_ORIGIN#https://}"
  local tunnel_ip=""

  if curl -fsS --max-time 10 "$BACKEND_ORIGIN/" >/dev/null 2>&1; then
    return 0
  fi

  # Some campus/local DNS resolvers cache a newly-created Quick Tunnel's
  # initial NXDOMAIN. Resolve once through Cloudflare DNS for the health check;
  # Pages itself uses Cloudflare's resolver and is not affected.
  tunnel_ip=$(dig +short A "$tunnel_host" @1.1.1.1 \
    | rg '^[0-9]+(\.[0-9]+){3}$' \
    | head -n 1 || true)
  [[ -n "$tunnel_ip" ]] && \
    curl -fsS --max-time 10 \
      --resolve "$tunnel_host:443:$tunnel_ip" \
      "$BACKEND_ORIGIN/" >/dev/null 2>&1
}

for attempt in {1..60}; do
  BACKEND_ORIGIN=$(docker compose -f docker-compose.demo.yml --profile share logs --no-color tunnel 2>/dev/null \
    | rg -o 'https://[a-z0-9-]+\.trycloudflare\.com' \
    | tail -n 1 || true)
  if [[ -n "$BACKEND_ORIGIN" ]] && tunnel_is_reachable; then
    break
  fi
  BACKEND_ORIGIN=""
  sleep 1
done

if [[ -z "$BACKEND_ORIGIN" ]]; then
  print -u2 "未能取得可用的后端隧道，请查看 tunnel 容器日志。"
  exit 1
fi
fi

print "后端出口已就绪：$BACKEND_ORIGIN"
print "正在构建前端……"
npm --prefix elmclient run build

PAGES_OUTPUT_DIR=$(mktemp -d)
WRANGLER_NPM_CACHE=$(mktemp -d)
cleanup() {
  rm -rf -- "$PAGES_OUTPUT_DIR" "$WRANGLER_NPM_CACHE"
}
trap cleanup EXIT

cp -R elmclient/dist/. "$PAGES_OUTPUT_DIR/"
sed "s|__BACKEND_ORIGIN__|$BACKEND_ORIGIN|g" "$WORKER_TEMPLATE" \
  > "$PAGES_OUTPUT_DIR/_worker.js"

print "正在部署 Cloudflare Pages 项目 $PAGES_PROJECT（分支：$BRANCH）……"
npm --cache "$WRANGLER_NPM_CACHE" exec --yes "wrangler@$WRANGLER_VERSION" -- \
  pages deploy "$PAGES_OUTPUT_DIR" \
  --project-name="$PAGES_PROJECT" \
  --branch="$BRANCH" \
  --commit-dirty=true

if [[ "$BRANCH" == "main" ]]; then
  [[ -z "$PUBLIC_DEMO_URL" ]] || print "演示地址：$PUBLIC_DEMO_URL"
  print "正式分支发布完成，访问地址也可查看上方 Wrangler 输出。"
else
  print "这是预览部署，不会覆盖固定演示地址。"
fi
