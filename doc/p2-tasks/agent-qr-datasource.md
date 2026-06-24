# agent-qr-datasource — P2 任务清单（★ 新模块）

> 多源数据接入模块：统一数据源连接器策略接口、JDBC/REST/S3 三种连接器实现、定时同步调度器、数据源管理服务。

---

## 1. Maven 模块初始化

- [ ] **1.1** 创建 `agent-qr-datasource/` 目录结构
  - `pom.xml`
  - `src/main/java/org/example/agent_qr/datasource/`
  - 子包：`connector/`、`service/`、`scheduler/`、`entity/`、`mapper/`、`dto/`

- [ ] **1.2** 配置 `agent-qr-datasource/pom.xml`
  - `groupId`: `org.example`
  - `artifactId`: `agent-qr-datasource`
  - 依赖：`agent-qr-common`、`spring-boot-starter-web`（RestTemplate）、`aws-sdk-s3`（S3）、`mysql-connector-j`

---

## 2. DataSourceConnector 策略接口

- [ ] **2.1** 创建 `org.example.agent_qr.datasource.connector.DataSourceConnector` 接口
  - 方法 `String getType()`：返回连接器类型标识（JDBC / REST / S3）
  - 方法 `ConnectionTestResult testConnection(Map<String, Object> config)`：连通性测试
  - 方法 `SyncResult fullSync(SyncContext context)`：全量数据同步
  - 方法 `SyncResult incrementalSync(SyncContext context, String lastCursor)`：增量数据同步

---

## 3. JDBC 数据库连接器 — JdbcConnector

- [ ] **3.1** 创建 `org.example.agent_qr.datasource.connector.JdbcConnector` 类
  - 实现 `DataSourceConnector`
  - 注解 `@Component`、`@Slf4j`
  - 方法 `getType()`：返回 "JDBC"
  - 方法 `testConnection(config)`：
    1. 从 config 提取 url / username / password
    2. `DriverManager.getConnection()` 建立连接
    3. 获取 DatabaseMetaData（产品名、版本）
    4. 记录延迟 ms
    5. 返回 `ConnectionTestResult`（success / latencyMs / dbProduct / dbVersion / errorMsg）
  - 方法 `fullSync(context)`：
    1. 从 config 获取 tableNames
    2. 遍历每张表 → `SELECT * FROM {tableName}`
    3. 逐行转换为 `Map<String, Object>`（LinkedHashMap 保序）
    4. 返回 `SyncResult`（totalRows / rawData / nextCursor=null）
  - 方法 `incrementalSync(context, lastCursor)`：
    1. 获取 cursorField（如 update_time）
    2. `SELECT * FROM {tableName} WHERE {cursorField} > ? ORDER BY {cursorField} ASC`
    3. 遍历结果，逐行转换，更新游标为最后一条的 cursorField 值
    4. 返回 `SyncResult`（totalRows / rawData / nextCursor=newCursor）

---

## 4. REST API 连接器 — RestApiConnector

- [ ] **4.1** 创建 `org.example.agent_qr.datasource.connector.RestApiConnector` 类
  - 实现 `DataSourceConnector`
  - 注解 `@Component`、`@Slf4j`
  - 属性：`RestTemplate restTemplate`
  - 方法 `getType()`：返回 "REST"
  - 方法 `testConnection(config)`：HTTP HEAD 请求 → 检查状态码 2xx
  - 方法 `fullSync(context)`：
    1. 从 config 获取 baseUrl
    2. 循环分页拉取：`GET {baseUrl}?cursor={cursor}` → 解析响应体 `List<Map<String, Object>>`
    3. 从响应头 `X-Next-Cursor` 获取下一页游标
    4. 合并全部结果
    5. 返回 `SyncResult`（totalRows / rawData）
  - 方法 `incrementalSync(context, lastCursor)`：同 fullSync，传入游标参数

---

## 5. S3 / 文件系统连接器 — S3Connector

- [ ] **5.1** 创建 `org.example.agent_qr.datasource.connector.S3Connector` 类
  - 实现 `DataSourceConnector`
  - 注解 `@Component`、`@Slf4j`
  - 属性：`AmazonS3 s3Client`
  - 方法 `getType()`：返回 "S3"
  - 方法 `testConnection(config)`：`s3Client.listObjectsV2` 请求 1 个对象 → 验证连通性
  - 方法 `fullSync(context)`：
    1. `s3Client.listObjectsV2(bucket, prefix)` 分页列出对象
    2. 对每个对象，过滤支持的文件格式（pdf / docx / txt / md / csv / json）
    3. 下载文件内容 `s3Client.getObject()` → 读取为 UTF-8 字符串
    4. 构建 `Map<String, Object>`（key / size / lastModified / bucket / content）
    5. 返回 `SyncResult`
  - 方法 `incrementalSync(context, lastCursor)`：
    1. lastCursor 格式为 ISO 8601 时间戳
    2. 过滤 lastModified > cursorTime 的文件
    3. 返回新文件列表 + 最新文件时间戳作为 newCursor
  - 私有方法 `isSupportedFormat(String key)`：检查扩展名

---

## 6. 同步调度器 — SyncScheduler

- [ ] **6.1** 创建 `org.example.agent_qr.datasource.scheduler.SyncScheduler` 类
  - 注解 `@Component`、`@Slf4j`
  - 属性：`DataSourceMapper dataSourceMapper`、`Map<String, DataSourceConnector> connectorMap`（Spring 自动注入所有 Connector 实现）、`ApplicationEventPublisher eventPublisher`
  - 方法 `scheduleSync(Long datasourceId)`：
    1. 查询数据源配置 → 校验状态为 ACTIVE
    2. 获取对应 Connector
    3. 测试连通性
    4. 判断同步策略：FULL（全量）或 INCREMENTAL（增量，有 lastCursor）
    5. 执行同步
    6. 更新数据源状态：`dataSourceMapper.updateSyncResult(datasourceId, nextCursor, totalRows, now)`
    7. 发布 `DataSyncCompletedEvent`（datasourceId / rawData / syncBatchId）
    8. 异常处理：捕获异常 → 更新状态为 ERROR + 记录日志

---

## 7. DataSourceService — 数据源管理 CRUD

- [ ] **7.1** 创建 `org.example.agent_qr.datasource.service.DataSourceService` 类
  - 注解 `@Service`、`@Slf4j`
  - 属性：`DataSourceMapper dataSourceMapper`、`Map<String, DataSourceConnector> connectorMap`
  - 方法 `create(DataSourceConfig config)`：创建数据源配置
  - 方法 `update(DataSourceConfig config)`：更新数据源配置
  - 方法 `delete(Long id)`：删除数据源配置
  - 方法 `getById(Long id)`：查询单个数据源
  - 方法 `listAll()`：查询全部数据源
  - 方法 `testConnection(Long id)`：查询配置 → 获取 Connector → `testConnection()` → 返回测试结果
  - 方法 `triggerSync(Long id)`：手动触发同步 → 调用 `syncScheduler.scheduleSync(id)`

---

## 8. 实体、Mapper、DTO

### 8.1 DataSourceConfig 实体

- [ ] **8.1.1** 创建 `org.example.agent_qr.datasource.entity.DataSourceConfig` 类
  - 注解 `@Data`、`@TableName("data_source_config")`
  - 属性：`Long id`、`String sourceName`、`String sourceType`（JDBC / REST / S3）、`String domain`、`String syncStrategy`（FULL / INCREMENTAL）、`String cursorField`、`String lastCursor`、`String connectionConfig`（TEXT，JSON 格式）、`String fieldMapping`（TEXT，JSON 格式）、`String status`（ACTIVE / INACTIVE / ERROR）、`Integer totalSynced`、`LocalDateTime lastSyncAt`、`LocalDateTime createTime`、`LocalDateTime updateTime`

### 8.2 DataSourceMapper

- [ ] **8.2.1** 创建 `org.example.agent_qr.datasource.mapper.DataSourceMapper` 接口
  - 继承 `BaseMapper<DataSourceConfig>`
  - 方法 `updateSyncResult(Long id, String lastCursor, Integer totalRows, LocalDateTime lastSyncAt)`：更新同步结果
  - 方法 `updateStatus(Long id, String status)`：更新数据源状态
  - 方法 `selectAll()`：查询所有活跃数据源

### 8.3 DTO 类

- [ ] **8.3.1** 创建 `org.example.agent_qr.datasource.dto.ConnectionTestResult` 类
  - 属性：`boolean success`、`long latencyMs`、`String dbProduct`、`String dbVersion`、`String errorMsg`

- [ ] **8.3.2** 创建 `org.example.agent_qr.datasource.dto.SyncResult` 类
  - 属性：`int totalRows`、`List<Map<String, Object>> rawData`、`String nextCursor`

- [ ] **8.3.3** 创建 `org.example.agent_qr.datasource.dto.SyncContext` 类
  - 属性：`Long datasourceId`、`DataSourceConfig config`、`String syncBatchId`

---

## 9. DataSyncCompletedEvent 事件类

- [ ] **9.1** 创建 `org.example.agent_qr.common.event.DataSyncCompletedEvent` 类（位于 agent-qr-common 模块）
  - 继承 `ApplicationEvent`
  - 字段：`Long datasourceId`、`List<Map<String, Object>> rawData`、`String syncBatchId`

---

## 10. DDL — data_source_config 表

- [ ] **10.1** 编写 `data_source_config` 建表 SQL
  - 字段：`id BIGINT AUTO_INCREMENT PRIMARY KEY`、`source_name VARCHAR(128) NOT NULL`、`source_type VARCHAR(16) NOT NULL`、`domain VARCHAR(32)`、`sync_strategy VARCHAR(16) DEFAULT 'FULL'`、`cursor_field VARCHAR(64)`、`last_cursor VARCHAR(64)`、`connection_config TEXT`、`field_mapping TEXT`、`status VARCHAR(16) DEFAULT 'ACTIVE'`、`total_synced INT DEFAULT 0`、`last_sync_at DATETIME`、`create_time DATETIME`、`update_time DATETIME`
  - 索引：`idx_domain (domain)`、`idx_status (status)`
