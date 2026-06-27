/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + units[i]
}

/**
 * 格式化日期时间
 */
export function formatDateTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

/**
 * 截断文本
 */
export function truncateText(text: string, maxLength: number): string {
  if (!text) return ''
  return text.length > maxLength ? text.slice(0, maxLength) + '...' : text
}

// ==================== P2 新增格式化函数 ====================

/**
 * 格式化密级 → 中文标签
 */
export function formatSensitivityLevel(level: number): string {
  const map: Record<number, string> = {
    0: '公开',
    1: '内部',
    2: '机密',
    3: '绝密',
  }
  return map[level] ?? '未知'
}

/**
 * 格式化业务域 → 中文标签
 */
export function formatDomain(domain: string): string {
  const map: Record<string, string> = {
    HR: '人力资源',
    FINANCE: '财务管理',
    RD: '研发中心',
    SALES: '销售管理',
    COMMON: '公共部门',
  }
  return map[domain] || domain
}

/**
 * 格式化数据源类型 → 中文标签
 */
export function formatSourceType(type: string): string {
  const map: Record<string, string> = {
    JDBC: '数据库',
    REST: 'REST API',
    S3: '文件系统',
  }
  return map[type] || type
}

/**
 * 格式化同步状态 → 中文标签
 */
export function formatSyncStatus(status: string): string {
  const map: Record<string, string> = {
    ACTIVE: '活跃',
    INACTIVE: '停用',
    ERROR: '异常',
  }
  return map[status] || status
}

/**
 * 格式化合格率 → 百分比字符串
 */
export function formatPassRate(rate: number): string {
  return (rate * 100).toFixed(1) + '%'
}

/**
 * 解析后端返回的逗号分隔域字符串 → 数组
 */
export function parseAllowedDomains(raw: string): string[] {
  if (!raw) return []
  return raw.split(',').map((d) => d.trim()).filter(Boolean)
}
