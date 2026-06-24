# P2 阶段总体进度

> 更新日期：2026-06-23
>
> 阶段目标：混合检索 + 流式输出 + ABAC 属性权限 + 多源接入 + ETL 管道 + 知识目录 + 数据一致性补偿

---

## 模块完成状态

| # | 模块 | 阶段 | 状态 | 子任务数 | 备注 |
|---|------|------|------|----------|------|
| 1 | **agent-qr-common**（P2 扩展） | P2 | ⬜ 未开始 | 10 | TraceIdFilter、MdcTaskDecorator、CaffeineConfig、DLQ 基础设施 |
| 2 | **agent-qr-auth**（P2 扩展） | P2 | ⬜ 未开始 | 13 | UserPrincipal、AbacEvaluator、JwtUtil(ABAC+双Token)、RefreshTokenService |
| 3 | **agent-qr-user**（P2 扩展） | P2 | ⬜ 未开始 | 4 | SysUser ABAC 字段扩展 |
| 4 | **agent-qr-knowledge**（P2 扩展） | P2 | ⬜ 未开始 | 12 | 软删除v2、PDF流式+表格+OCR、DOCX表格Markdown、DLQ集成 |
| 5 | **agent-qr-rag**（P2 扩展） | P2 | ⬜ 未开始 | 18 | SSE流式、混合检索、Ollama Provider、LLM熔断、结构化过滤、批量向量化 |
| 6 | **agent-qr-statistics**（P2 扩展） | P2 | ⬜ 未开始 | 9 | 满意度统计、FeedbackService |
| 7 | **agent-qr-web**（P2 扩展） | P2 | ⬜ 未开始 | 6 | AsyncConfigV2 四池隔离、application-p2.yml、p2-schema.sql |
| 8 | **agent-qr-compensation** ★ | P2 新增 | ⬜ 未开始 | 7 | 孤儿向量扫描、DocumentDeleteV2、DeleteTask |
| 9 | **agent-qr-datasource** ★ | P2 新增 | ⬜ 未开始 | 13 | DataSourceConnector、JDBC/REST/S3 Connector、SyncScheduler |
| 10 | **agent-qr-data-quality** ★ | P2 新增 | ⬜ 未开始 | 12 | 规则链引擎、完整性/编码/格式检查、CharsetDetector |
| 11 | **agent-qr-etl** ★ | P2 新增 | ⬜ 未开始 | 9 | DataNormalizer、StructuredDataConverter、FieldMappingEngine |
| 12 | **agent-qr-catalog** ★ | P2 新增 | ⬜ 未开始 | 8 | KnowledgeCatalogService、DomainRouter(关键词)、三级目录树 |
| — | **根 pom.xml 调整** | P2 | ⬜ 未开始 | — | 注册 5 个新模块 |
| — | **P2 DDL 迁移脚本** | P2 | ⬜ 未开始 | — | dlq_message / token_refresh / delete_task / data_source_config / kb_chunk_structured 等 |

> ★ = P2 阶段新增 Maven 模块

---

## P2 总体进度

| 总子任务数 | 已完成 | 进行中 | 完成率 |
|-----------|--------|--------|--------|
| ~121 | 0 | 0 | 0% |

---

## 开发顺序建议

P2 阶段依赖 P1 全部产出，开发顺序按内部依赖关系编排：

```
第 1 步：根 pom.xml 调整（注册 5 个新模块）
          ↓
第 2 步：agent-qr-common P2（DLQ + TraceId + Caffeine）
          ↓
第 3 步：agent-qr-user P2（SysUser ABAC 字段扩展）
          ↓
第 4 步：agent-qr-auth P2（ABAC 权限体系、双 Token）← 依赖 user
          ↓
第 5 步：agent-qr-datasource ★（数据源接入层）← 独立模块，被 etl/catalog 依赖
          ↓
第 6 步：agent-qr-catalog ★（知识目录 + DomainRouter）← 依赖 datasource
          ↓
第 7 步：agent-qr-data-quality ★（数据质量检查）← 独立模块
          ↓
第 8 步：agent-qr-etl ★（ETL 标准化管道）← 依赖 datasource
          ↓
第 9 步：agent-qr-rag P2（混合检索 + 流式 + 熔断 + 结构化过滤）← 依赖 catalog + common
          ↓
第 10 步：agent-qr-knowledge P2（软删除v2 事件发布 + 解析增强 + DLQ 集成）← 依赖 rag + common（★ 不依赖 compensation）
          ↓
第 11 步：agent-qr-compensation ★（数据一致性补偿）← 依赖 knowledge + common（★ 单向依赖，通过事件解耦）
          ↓
第 12 步：agent-qr-statistics P2（满意度统计）← 依赖 knowledge + user
          ↓
第 13 步：agent-qr-web P2（汇聚配置 + 新模块依赖 + p2-schema.sql）
```

---

## 模块依赖关系速查

```
                             ┌───────────────────────────────────┐
                             │         agent-qr-web              │
                             │  (依赖全部模块 + application-p2.yml) │
                             └──────────────┬────────────────────┘
                                            │
        ┌───────────────┬───────────┬───────┼───────────┬───────────────┐
        │               │           │       │           │               │
        ▼               ▼           ▼       ▼           ▼               ▼
  ┌──────────┐  ┌────────────┐ ┌──────────┐ ┌──────────────┐ ┌──────────────┐
  │  auth   │  │ knowledge  │ │   rag    │ │ statistics   │ │ compensation │
  │  (P2)   │  │   (P2)     │ │  (P2)    │ │    (P2)      │ │    (★新增)    │
  └────┬─────┘  └──┬───┬─────┘ └──┬───┬───┘ └──┬───┬───────┘ └──┬───────────┘
       │           │   │          │   │        │   │            │
       │      ┌────┘   └────┐     │   │   ┌────┘   │       ┌────┘
       │      │             │     │   │   │        │       │
       ▼      ▼             ▼     ▼   ▼   ▼        ▼       ▼
  ┌──────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐
  │  user   │ │  common (P2) │ │   catalog ★  │ │  knowledge (P1 base) │
  │  (P2)   │ │ DLQ/TraceId  │ │  DomainRouter│ │                      │
  └──────────┘ └──────────────┘ └──────┬───────┘ └──────────────────────┘
                                       │
                                ┌──────┴───────┐
                                │ datasource ★ │
                                │ (统一接入网关) │
                                └──────┬───────┘
                                       │
                                ┌──────┴───────┐
                                │  etl ★      │
                                │ (标准化管道)  │
                                └──────────────┘
                                       │
                                ┌──────┴───────────┐
                                │ data-quality ★  │
                                │ (质检引擎)       │
                                └─────────────────┘
```

### 关键依赖说明

| 依赖方 | 被依赖方 | 说明 |
|--------|---------|------|
| `agent-qr-auth` | `agent-qr-user` | ABAC 属性读取 SysUser 扩展字段 |
| `agent-qr-rag` | `agent-qr-catalog` | HybridRetriever 调用 DomainRouter 进行域裁剪 |
| `agent-qr-etl` | `agent-qr-datasource` | DataNormalizer 读取 DataSourceConfig 字段映射 |
| `agent-qr-catalog` | `agent-qr-datasource` | KnowledgeCatalogService 读取数据源列表 |
| `agent-qr-compensation` | `agent-qr-knowledge` | ★ 单向依赖：OrphanVectorScanner / DocumentDeleteListener 依赖 ChunkMapper / DocumentMapper |
| `agent-qr-compensation` | `agent-qr-common` | DLQ 重试、指数退避、DocumentDeleteRequestedEvent |

> ★ **循环依赖解耦设计**：`agent-qr-knowledge` 与 `agent-qr-compensation` 之间通过**事件驱动**实现解耦，避免 Maven 双向依赖：
> - `knowledge` 只发布 `DocumentDeleteRequestedEvent` 事件（定义在 `agent-qr-common`），**不注入 compensation 的任何类**
> - `compensation` 单向依赖 `knowledge`（访问 ChunkMapper / DocumentMapper），并通过 `@TransactionalEventListener(phase = AFTER_COMMIT)` 监听删除事件
> - 依赖链：`compensation → knowledge → common ← compensation`（通过 common 事件类解耦，无循环）

---

## P2 阶段新增 Maven 模块清单

| 模块 | groupId:artifactId | 包路径 |
|------|-------------------|--------|
| agent-qr-compensation | `org.example:agent-qr-compensation` | `org.example.agent_qr.compensation` |
| agent-qr-datasource | `org.example:agent-qr-datasource` | `org.example.agent_qr.datasource` |
| agent-qr-data-quality | `org.example:agent-qr-data-quality` | `org.example.agent_qr.dataquality` |
| agent-qr-etl | `org.example:agent-qr-etl` | `org.example.agent_qr.etl` |
| agent-qr-catalog | `org.example:agent-qr-catalog` | `org.example.agent_qr.catalog` |

所有新增模块均以 `agent-qr-common` 为基础依赖，禁止反向依赖业务模块。

---

## P2 阶段新增 DDL 汇总

| 类型 | 表名 | 所属模块 | 说明 |
|------|------|---------|------|
| CREATE | `dlq_message` | common | 死信队列消息表 |
| CREATE | `token_refresh` | auth | Refresh Token 持久化表 |
| CREATE | `delete_task` | compensation | ChromaDB 物理删除任务表 |
| CREATE | `data_source_config` | datasource | 外部数据源配置表 |
| CREATE | `kb_chunk_structured` | rag | 结构化字段 B+树索引表 |
| ALTER | `sys_user` | user | 新加 department / clearance_level / allowed_domains / title |
| ALTER | `kb_document` | knowledge | 新加 domain / sensitivity_level / sensitivity_label / deleted / error_msg |
| ALTER | `kb_chunk` | knowledge | 新加 deleted |
| ALTER | `chat_message` | statistics | 新加 feedback / feedback_reason |
| ALTER | `stat_daily` | statistics | 新加 positive_count / negative_count |

所有 DDL 统一收敛到 `agent-qr-web/src/main/resources/db/p2-schema.sql`。
