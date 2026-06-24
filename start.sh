#!/bin/bash
# ============================================
# Agent-QR Docker 一键启动脚本 (Linux/Mac)
# P1 阶段
#
# 用法:
#   chmod +x start.sh
#   ./start.sh              # 启动所有服务
#   ./start.sh stop         # 停止所有服务
#   ./start.sh restart      # 重启所有服务
#   ./start.sh logs         # 查看日志
#   ./start.sh status       # 查看服务状态
#   ./start.sh clean        # 清理所有容器和数据
# ============================================

set -e

COMPOSE_FILE="docker-compose.yml"
PROJECT_NAME="agent-qr"

# 检查 Docker 环境
check_docker() {
  if ! command -v docker &> /dev/null; then
    echo "错误: 未找到 Docker，请先安装 Docker"
    exit 1
  fi
  if ! docker compose version &> /dev/null; then
    echo "错误: 需要 Docker Compose v2，请升级 Docker Desktop"
    exit 1
  fi
}

# 检查 .env 文件
check_env() {
  if [ ! -f .env ]; then
    echo "提示: 未找到 .env 文件，从 .env.example 创建默认配置..."
    if [ -f .env.example ]; then
      cp .env.example .env
      echo "已创建 .env 文件，请编辑其中的敏感配置后重新运行"
      echo "尤其是: DEEPSEEK_API_KEY, JWT_SECRET"
      exit 0
    else
      echo "警告: 未找到 .env.example，将使用默认配置"
    fi
  fi
}

# 创建上传目录
prepare_uploads_dir() {
  if [ ! -d uploads ]; then
    mkdir -p uploads
    echo "已创建 uploads 目录"
  fi
}

# 启动服务
start_services() {
  echo "=========================================="
  echo "  Agent-QR 容器化部署 (P1 阶段)"
  echo "=========================================="

  check_docker
  check_env
  prepare_uploads_dir

  echo ""
  docker compose -f "$COMPOSE_FILE" up -d --build
  echo ""
  echo "=========================================="
  echo "  服务启动中..."
  echo "=========================================="
  echo "  MySQL:        localhost:${MYSQL_PORT:-3308}"
  echo "  ChromaDB:     localhost:${CHROMA_PORT:-8000}"
  echo "  Backend API:  http://localhost:${BACKEND_PORT:-9090}"
  echo "=========================================="
  echo ""
  echo "提示: 使用 './start.sh logs' 查看实时日志"
  echo "提示: 使用 './start.sh status' 查看服务状态"
}

# 停止服务
stop_services() {
  echo "停止所有服务..."
  docker compose -f "$COMPOSE_FILE" down
  echo "服务已停止"
}

# 重启服务
restart_services() {
  stop_services
  start_services
}

# 查看日志
view_logs() {
  docker compose -f "$COMPOSE_FILE" logs -f --tail=100
}

# 查看服务状态
view_status() {
  docker compose -f "$COMPOSE_FILE" ps
}

# 清理所有
clean_all() {
  echo "警告: 将删除所有容器、数据卷和镜像!"
  read -p "确认继续? (y/N) " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker compose -f "$COMPOSE_FILE" down -v --rmi all
    echo "已清理所有容器、数据卷和镜像"
    echo "注意: uploads 目录未被删除，请手动处理: rm -rf uploads/"
  fi
}

# 主入口
case "${1:-start}" in
  start)
    start_services
    ;;
  stop)
    stop_services
    ;;
  restart)
    restart_services
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
    echo "用法: $0 {start|stop|restart|logs|status|clean}"
    echo ""
    echo "  start   启动所有服务（默认）"
    echo "  stop    停止所有服务"
    echo "  restart 重启所有服务"
    echo "  logs    查看实时日志"
    echo "  status  查看服务状态"
    echo "  clean   清理所有容器和数据（不可逆）"
    exit 1
    ;;
esac
