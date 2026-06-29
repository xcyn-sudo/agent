#!/bin/bash
# ============================================
# Agent-QR 数据库备份脚本
# 配合 crontab 定时执行
#
# 用法:
#   chmod +x scripts/backup.sh
#   ./scripts/backup.sh
#
# Crontab 示例 (每天凌晨 2 点备份):
#   0 2 * * * /opt/agent-qr/scripts/backup.sh >> /var/log/agent-qr-backup.log 2>&1
# ============================================

set -e

PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
cd "$PROJECT_DIR"

# 加载环境变量
if [ -f .env ]; then
  export $(grep -v '^#' .env | grep -v '^$' | xargs)
fi

BACKUP_DIR="${BACKUP_DIR:-./backups}"
DB_CONTAINER="${DB_CONTAINER:-agent-qr-mysql}"
DB_NAME="${MYSQL_DATABASE:-agent_qr}"
DB_USER="root"
DB_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
RETENTION_DAYS=30  # 保留 30 天

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_${TIMESTAMP}.sql.gz"

# 创建备份目录
mkdir -p "$BACKUP_DIR"

echo "[$(date)] 开始备份数据库 $DB_NAME..."

# 检查 MySQL 容器是否运行
if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
  echo "[$(date)] 错误: MySQL 容器未运行"
  exit 1
fi

# 执行备份
docker exec "$DB_CONTAINER" mysqldump \
  -u"$DB_USER" \
  -p"$DB_PASSWORD" \
  --single-transaction \
  --quick \
  --lock-tables=false \
  --default-character-set=utf8mb4 \
  "$DB_NAME" | gzip > "$BACKUP_FILE"

echo "[$(date)] 备份完成: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"

# 清理旧备份
echo "[$(date)] 清理 $RETENTION_DAYS 天前的旧备份..."
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime +$RETENTION_DAYS -delete

echo "[$(date)] 备份任务完成，当前备份数: $(ls "$BACKUP_DIR"/*.sql.gz 2>/dev/null | wc -l)"
