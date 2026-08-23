#!/usr/bin/env bash
# MyVPN 部署脚本：仅部署 target 目录中的 jar，连接宿主机 Docker 里的 MySQL / Redis
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

APP_PORT="${APP_PORT:-9010}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-123456}"
MYSQL_DATABASE="${MYSQL_DATABASE:-myvpn}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
JAR_FILE="${JAR_FILE:-}"

log() { printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"; }
die() { log "错误: $*"; exit 1; }

find_jar() {
  if [[ -n "$JAR_FILE" && -f "$JAR_FILE" ]]; then
    echo "$JAR_FILE"
    return
  fi
  local jar
  jar="$(ls -t target/myvpn-*.jar 2>/dev/null | grep -v '\.original$' | head -1 || true)"
  [[ -n "$jar" ]] || die "未找到 jar，请先执行: mvn package -DskipTests"
  echo "$jar"
}

check_mysql() {
  log "检查 MySQL ${MYSQL_HOST}:${MYSQL_PORT} ..."
  if command -v mysql >/dev/null 2>&1; then
    mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1" >/dev/null 2>&1 \
      || die "无法连接 MySQL，请确认 Docker 中 MySQL 已启动且端口已映射"
  else
    log "未安装 mysql 客户端，跳过连通性检查"
  fi
}

check_redis() {
  log "检查 Redis ${REDIS_HOST}:${REDIS_PORT} ..."
  if command -v redis-cli >/dev/null 2>&1; then
    if [[ -n "$REDIS_PASSWORD" ]]; then
      redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" ping >/dev/null 2>&1 \
        || die "无法连接 Redis"
    else
      redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping >/dev/null 2>&1 \
        || die "无法连接 Redis，请确认 Docker 中 Redis 已启动且端口已映射"
    fi
  else
    log "未安装 redis-cli，跳过连通性检查"
  fi
}

cmd_build() {
  log "打包 jar ..."
  mvn package -DskipTests -q
  find_jar >/dev/null
  log "打包完成: $(find_jar)"
}

cmd_up() {
  local jar
  jar="$(find_jar)"
  export JAR_FILE="$jar"
  check_mysql
  check_redis
  log "使用 jar: $jar"
  log "启动 Docker 容器 ..."
  docker compose up -d --build
  log "部署完成: http://127.0.0.1:${APP_PORT}"
  log "管理后台: http://127.0.0.1:${APP_PORT}/login.html"
}

cmd_down() {
  docker compose down
  log "已停止"
}

cmd_restart() {
  docker compose restart
  log "已重启"
}

cmd_logs() {
  docker compose logs -f --tail=200 myvpn
}

cmd_status() {
  docker compose ps
  echo
  curl -fsS "http://127.0.0.1:${APP_PORT}/api/client/health" && echo || echo "健康检查失败"
}

# 不用 Docker，直接在宿主机运行 jar（连接 127.0.0.1 的 MySQL/Redis）
cmd_run() {
  local jar
  jar="$(find_jar)"
  check_mysql
  check_redis
  log "宿主机运行: $jar"
  exec java ${JAVA_OPTS:--Xms256m -Xmx512m} -jar "$jar" \
    --spring.datasource.url="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
    --spring.datasource.username="$MYSQL_USER" \
    --spring.datasource.password="$MYSQL_PASSWORD" \
    --spring.data.redis.host="$REDIS_HOST" \
    --spring.data.redis.port="$REDIS_PORT" \
    --spring.data.redis.password="$REDIS_PASSWORD"
}

usage() {
  cat <<EOF
用法: ./deploy.sh <命令>

命令:
  build     执行 mvn package 生成 jar
  up        构建镜像并启动（Docker 部署，推荐）
  down      停止并移除容器
  restart   重启容器
  logs      查看日志
  status    查看状态与健康检查
  run       不用 Docker，宿主机直接 java -jar 运行

环境变量（可选）:
  MYSQL_HOST=127.0.0.1   MYSQL_PORT=3306
  MYSQL_USER=root        MYSQL_PASSWORD=123456
  MYSQL_DATABASE=myvpn
  REDIS_HOST=127.0.0.1   REDIS_PORT=6379
  APP_PORT=9010

示例:
  mvn package -DskipTests && ./deploy.sh up
  ./deploy.sh build && ./deploy.sh up
  ./deploy.sh run
EOF
}

case "${1:-}" in
  build)   cmd_build ;;
  up)      cmd_up ;;
  down)    cmd_down ;;
  restart) cmd_restart ;;
  logs)    cmd_logs ;;
  status)  cmd_status ;;
  run)     cmd_run ;;
  *)       usage; exit 1 ;;
esac
