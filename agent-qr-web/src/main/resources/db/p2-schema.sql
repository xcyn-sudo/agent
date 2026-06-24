
-- ============================================================
-- P2 阶段数据库变更脚本
-- 汇聚全部 P2 DDL (5 CREATE TABLE + 5 ALTER TABLE)
-- ============================================================

-- ── 1. 死信队列表 ──
CREATE TABLE IF NOT EXISTS dlq_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(32) NOT NULL COMMENT '事件类型：PARSE/CHUNK/EMBED/DELETE',
    document_id BIGINT COMMENT '关联文档ID',
    payload TEXT COMMENT '原始负载(JSON)',
    error_msg TEXT COMMENT '错误信息',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    next_retry_at DATETIME COMMENT '下次重试时间',
    status VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/DEAD',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status_next_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='死信队列表';

-- ── 2. Refresh Token 表 ──
CREATE TABLE IF NOT EXISTS token_refresh (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    token TEXT NOT NULL COMMENT 'Refresh Token',
    revoked TINYINT(1) DEFAULT 0 COMMENT '是否已撤销',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    INDEX idx_token (token(255)),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Refresh Token表';

-- ── 3. 删除任务表 ──
CREATE TABLE IF NOT EXISTS delete_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL COMMENT '文档ID',
    chroma_ids TEXT COMMENT 'ChromaDB向量ID列表(JSON数组)',
    status VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/DONE/FAILED',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='删除任务表';

-- ── 4. 数据源配置表 ──
CREATE TABLE IF NOT EXISTS data_source_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_name VARCHAR(128) NOT NULL COMMENT '数据源名称',
    source_type VARCHAR(16) NOT NULL COMMENT '数据源类型：JDBC/REST/S3',
    domain VARCHAR(32) COMMENT '所属业务域',
    sync_strategy VARCHAR(16) DEFAULT 'FULL' COMMENT 'FULL/INCREMENTAL',
    cursor_field VARCHAR(64) COMMENT '增量游标字段',
    last_cursor VARCHAR(255) COMMENT '上次游标值',
    connection_config TEXT COMMENT '连接配置(JSON)',
    field_mapping TEXT COMMENT '字段映射配置(JSON)',
    status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE/ERROR',
    total_synced INT DEFAULT 0 COMMENT '累计同步数',
    last_sync_at DATETIME COMMENT '上次同步时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_domain (domain),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';

-- ── 5. 切片结构化字段表 ──
CREATE TABLE IF NOT EXISTS kb_chunk_structured (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chunk_id BIGINT NOT NULL COMMENT '切片ID',
    domain VARCHAR(32) COMMENT '业务域',
    field_name VARCHAR(64) COMMENT '字段名',
    field_value VARCHAR(255) COMMENT '字段值',
    numeric_value DECIMAL(18,4) COMMENT '数值型值',
    date_value DATE COMMENT '日期型值',
    field_type VARCHAR(16) COMMENT 'NUMBER/DATE/ENUM/STRING',
    INDEX idx_domain_field_number (domain, field_name, numeric_value),
    INDEX idx_domain_field_date (domain, field_name, date_value),
    INDEX idx_chunk_id (chunk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='切片结构化字段表';

-- ── ALTER 1: sys_user 新增 ABAC 字段 ──
ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS department VARCHAR(32) COMMENT '所属部门',
    ADD COLUMN IF NOT EXISTS clearance_level INT DEFAULT 0 COMMENT '数据密级',
    ADD COLUMN IF NOT EXISTS allowed_domains VARCHAR(512) COMMENT '允许访问的业务域(逗号分隔)',
    ADD COLUMN IF NOT EXISTS title VARCHAR(32) COMMENT '职级';

-- ── ALTER 2: kb_document 新增 P2 字段 ──
ALTER TABLE kb_document
    ADD COLUMN IF NOT EXISTS domain VARCHAR(32) COMMENT '所属业务域',
    ADD COLUMN IF NOT EXISTS sensitivity_level INT DEFAULT 0 COMMENT '敏感级别',
    ADD COLUMN IF NOT EXISTS sensitivity_label VARCHAR(16) DEFAULT '公开' COMMENT '敏感级别标签',
    ADD COLUMN IF NOT EXISTS deleted INT DEFAULT 0 COMMENT '软删除标记',
    ADD INDEX IF NOT EXISTS idx_domain (domain),
    ADD INDEX IF NOT EXISTS idx_deleted (deleted);

-- ── ALTER 3: kb_chunk 新增 P2 字段 ──
ALTER TABLE kb_chunk
    ADD COLUMN IF NOT EXISTS status VARCHAR(16) DEFAULT 'READY' COMMENT '切片状态',
    ADD COLUMN IF NOT EXISTS deleted INT DEFAULT 0 COMMENT '软删除标记',
    ADD INDEX IF NOT EXISTS idx_deleted (deleted);

-- ── ALTER 4: chat_message 新增满意度反馈字段 ──
ALTER TABLE chat_message
    ADD COLUMN IF NOT EXISTS feedback VARCHAR(16) COMMENT 'positive/negative',
    ADD COLUMN IF NOT EXISTS feedback_reason VARCHAR(512) COMMENT '反馈原因';

-- ── ALTER 5: stat_daily 新增满意度统计字段 ──
ALTER TABLE stat_daily
    ADD COLUMN IF NOT EXISTS positive_count INT DEFAULT 0 COMMENT '点赞数',
    ADD COLUMN IF NOT EXISTS negative_count INT DEFAULT 0 COMMENT '点踩数';
