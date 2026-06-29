#!/bin/bash
# ============================================
# Agent-QR 生产环境一键部署脚本
# 运行于阿里云 ECS 上
#
# 用法:
#   chmod +x scripts/deploy.sh
#   ./scripts/deploy.sh              # 构建并启动所有服务
#   ./scripts/deploy.sh pull         # 仅拉取最新镜像并重启
#   ./scripts/deploy.sh restart      # 重启所有服务
#   ./scripts/deploy.sh stop         # 停止所有服务
#   ./scripts/deploy.sh logs         # 查看实时日志
#   ./scripts/deploy.sh status       # 查看服务状态
#   ./scripts/deploy.sh clean        # 清理所有容器和数据（危险！）
# ============================================

set -e

PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
cd "$PROJECT_DIR"

COMPOSE_FILE="docker-compose.prod.yml"
PROJECT_NAME="agent-qr"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Docker 环境
check_docker() {
  if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: 未找到 Docker，请先安装 Docker${NC}"
    exit 1
  fi
  if ! docker compose version &> /dev/null; then
    echo -e "${RED}错误: 需要 Docker Compose v2，请升级 Docker${NC}"
    exit 1
  fi
}

# 检查 .env 文件
check_env() {
  if [ ! -f .env ]; then
    echo -e "${YELLOW}提示: 未找到 .env 文件，从 .env.example 创建...${NC}"
    if [ -f .env.example ]; then
      cp .env.example .env
      echo -e "${RED}已创建 .env 文件，请编辑后重新运行！${NC}"
      echo -e "${RED}必须填写: DEEPSEEK_API_KEY, DASHSCOPE_API_KEY, JWT_SECRET${NC}"
      exit 0
    else
      echo -e "${RED}错误: 未找到 .env.example${NC}"
      exit 1
    fi
  fi
}

# 创建必要目录
prepare_dirs() {
  if [ ! -d uploads ]; then
    mkdir -p uploads
    echo "已创建 uploads 目录"
  fi
  if [ ! -d nginx/conf.d ]; then
    mkdir -p nginx/conf.d
  fi
}

# 构建并启动
start_services() {
  echo -e "${GREEN}=========================================="
  echo "  Agent-QR 生产环境部署"
  echo "==========================================${NC}"

  check_docker
  check_env
  prepare_dirs

  echo ""
  echo "正在构建并启动所有服务..."
  docker compose -f "$COMPOSE_FILE" up -d --build

  echo ""
  echo -e "${GREEN}=========================================="
  echo "  服务启动完成！"
  echo "=========================================="
  echo "  前端页面:    http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'ECS_IP')"
  echo "  API 地址:    http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'ECS_IP')/api"
  echo "==========================================${NC}"
  echo ""
  echo "提示: 使用 '$0 logs' 查看实时日志"
  echo "提示: 使用 '$0 status' 查看服务状态"
}

# 拉取最新镜像并重启
pull_and_restart() {
  echo "拉取最新镜像..."
  docker compose -f "$COMPOSE_FILE" pull
  echo "更新服务..."
  docker compose -f "$COMPOSE_FILE" up -d --remove-orphans
  echo -e "${GREEN}服务已更新！${NC}"

  # 清理旧镜像（保留 72 小时内的）
  docker image prune -a --filter "until=72h" -f
  echo "已清理 72 小时前的旧镜像"
}

# 重启服务
restart_services() {
  echo "重启所有服务..."
  docker compose -f "$COMPOSE_FILE" restart
  echo -e "${GREEN}服务已重启${NC}"
}

# 停止服务
stop_services() {
  echo "停止所有服务..."
  docker compose -f "$COMPOSE_FILE" down
  echo -e "${GREEN}服务已停止${NC}"
}

# 查看日志
view_logs() {
  docker compose -f "$COMPOSE_FILE" logs -f --tail=100
}

# 查看状态
view_status() {
  echo -e "${GREEN}=== 容器状态 ===${NC}"
  docker compose -f "$COMPOSE_FILE" ps
  echo ""
  echo -e "${GREEN}=== 资源占用 ===${NC}"
  docker stats --no-stream $(docker compose -f "$COMPOSE_FILE" ps -q) 2>/dev/null || true
}

# 清理所有
clean_all() {
  echo -e "${RED}=========================================="
  echo "  警告: 将删除所有容器、数据卷和镜像！"
  echo "  此操作不可逆！"
  echo "==========================================${NC}"
  read -p "确认继续? 输入 'yes' 确认: " CONFIRM
  if [ "$CONFIRM" = "yes" ]; then
    docker compose -f "$COMPOSE_FILE" down -v --rmi all
    echo -e "${RED}已清理所有容器、数据卷和镜像${NC}"
    echo "注意: uploads 目录未被删除，请手动处理: rm -rf uploads/"
  else
    echo "已取消"
  fi
}

# 主入口
case "${1:-start}" in
  start)
    start_services
    ;;
  pull)
    pull_and_restart
    ;;
  restart)
    restart_services
    ;;
  stop)
    stop_services
    ;;
  logs)
    view_logs
    ;;
  status)
    view_status
    ;;
  clean)
    clean_all
    ;;
  *)
    echo "用法: $0 {start|pull|restart|stop|logs|status|clean}"
    echo ""
    echo "  start   构建并启动所有服务（默认）"
    echo "  pull    拉取最新镜像并重启（CI/CD 用）"
    echo "  restart 重启所有服务"
    echo "  stop    停止所有服务"
    echo "  logs    查看实时日志"
    echo "  status  查看服务状态和资源占用"
    echo "  clean   清理所有容器和数据（不可逆）"
    exit 1
    ;;
esac
