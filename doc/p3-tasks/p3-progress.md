# P3 阶段总体进度

> 更新日期：2026-06-28
> 阶段目标：CQRS 读写分离 + Embedding 语义域路由 + 向量维度管理 + Provider 切换决策优化 + 前端全面增强
> 前置依赖：P1 + P2 全部完成

---

## 模块整体状态

| # | 模块 | 阶段 | 状态 | 子任务数 | 备注 |
|---|------|------|------|----------|------|
| 1 | **agent-qr-common**（P3 扩展） | P3 | ⬜ 未开始 | 7 | ReadWriteRoutingDataSource、ReadWriteDataSourceAspect |
| 2 | **agent-qr-rag**（P3 扩展） | P3 | ⬜ 未开始 | 15 | DomainRouterV2、EmbeddingDimensionManager、ProviderDecisionEngine |
| 3 | **agent-qr-catalog**（P3 扩展） | P3 | ⬜ 未开始 | 7 | DomainRouterV2（Embedding 语义版） |
| 4 | **agent-qr-web**（P3 扩展） | P3 | ⬜ 未开始 | 10 | application-p3.yml、CQRS Bean 装配 |
| 5 | **agent-qr-web-frontend**（P3 增强） | P3 | ⬜ 未开始 | 20+ | WebSocket、i18n、移动端、ABAC 细粒度、知识图谱 |

---

## 总览统计

| 指标 | 数值 |
|------|------|
| 总模块数 | 5 |
| 总子任务数 | 约 60 |
| 已完成 | 0 |
| 进行中 | 0 |
| 完成率 | 0% |

---

## 开发顺序

```
第 1 步：agent-qr-common P3（CQRS 基础组件）
          ↓
第 2 步：agent-qr-web P3（P3 配置文件 + CQRS Bean 装配）
          ↓
第 3 步：agent-qr-rag P3（EmbeddingDimensionManager + DomainRouterV2 + ProviderDecisionEngine）
          ↓
第 4 步：agent-qr-catalog P3（DomainRouterV2 目录语义集成）
          ↓
第 5 步：agent-qr-web-frontend P3（前端全面增强）
```

**开发顺序说明**：
1. **common 优先**：CQRS 组件是基础设施，rag 模块的 `EmbeddingDimensionManager` 和 web 模块的 CQRS 配置都依赖它
2. **web 次之**：`application-p3.yml` 提供全阶段配置基础，后续模块都需要读取 P3 配置
3. **rag 核心**：P3 最核心的变更在 rag 模块（语义路由 + 维度管理 + Provider 决策），工作量最大
4. **catalog 配套**：依赖 rag 模块的 `EmbeddingProvider`，语义路由的域描述生成
5. **前端最后**：依赖后端 API 稳定后实施，且工作量最大

---

## 依赖关系图

```
agent-qr-common (CQRS)
    ├──→ agent-qr-web (CQRS Bean 装配 + application-p3.yml)
    │         └──→ agent-qr-rag (EmbeddingDimensionManager 读取配置)
    │
    └──→ agent-qr-rag (DomainRouterV2 可能依赖 common 工具类)
              │
              ├──→ agent-qr-catalog (DomainRouterV2 依赖 EmbeddingProvider)
              │
              └──→ agent-qr-web-frontend (依赖后端所有 API 稳定)
```

---

## P3 核心组件一览

| 类名 | 所在模块 | 功能简述 |
|------|---------|---------|
| `ReadWriteRoutingDataSource` | agent-qr-common | 基于 ThreadLocal 的读写数据源动态路由 |
| `ReadWriteDataSourceAspect` | agent-qr-common | `@Transactional(readOnly)` 切面自动切换数据源 |
| `EmbeddingDimensionManager` | agent-qr-rag | ChromaDB Collection 按 Provider/维度自动隔离 |
| `DomainRouterV2` | agent-qr-rag | Embedding 余弦相似度语义域路由（替代 P2 关键词） |
| `ProviderDecisionEngine` | agent-qr-rag | 根据熔断器状态自动选择 LLM/Embedding Provider |
| `DomainRouterV2` | agent-qr-catalog | 基于目录树动态生成域 Embedding 描述 |
| `CqrsDataSourceConfig` | agent-qr-web | P3 Profile 下的读写数据源 Bean 装配 |
| `application-p3.yml` | agent-qr-web | P3 阶段全量配置（数据源/路由/Provider/维度） |
| `useWebSocket` | agent-qr-web-frontend | WebSocket 连接管理与自动重连 |
| `vue-i18n` 集成 | agent-qr-web-frontend | 中英文国际化语言包与切换 |
| `v-permission` 指令 | agent-qr-web-frontend | ABAC 按钮/字段级细粒度权限控制 |
| `KnowledgeGraph` | agent-qr-web-frontend | ECharts 力导向知识图谱可视化 |

---

## P2 → P3 升级映射

| P2 实现 | P3 升级 | 影响范围 |
|---------|---------|---------|
| 单数据源 | `ReadWriteRoutingDataSource` 读写分离 | common + web |
| `DomainRouter`（关键词匹配） | `DomainRouterV2`（Embedding 语义） | rag + catalog |
| 手动管理 Collection | `EmbeddingDimensionManager` 自动维度检测 | rag |
| `application-p2.yml` 手动切换 Provider | `ProviderDecisionEngine` 熔断自动切换 | rag |
| SSE 流式（POST） | WebSocket 双向通信 | frontend + web |
| ABAC 页面/菜单级 | `v-permission` 按钮/字段级 | frontend |
| 硬编码中文 | `vue-i18n` 多语言 | frontend |
| 桌面端 | 响应式移动端适配 | frontend |
| 三级树静态浏览 | 知识图谱可视化 | frontend |
| 事后质检报告 | 实时规则配置 UI | frontend |

---

## 关键设计决策

1. **CQRS 初期从库可指向主库**：读库可先复用主库实例，后续平滑迁移只读副本
2. **DomainRouterV2 与 P2 DomainRouter 共存**：语义路由优先，不可用时降级关键词路由
3. **Collection 命名嵌入模型 ID**：`kb_{provider}_{model}` 方案确保不同 Embedding 模型向量隔离
4. **前端分批实施**：WebSocket + i18n + ABAC（第一批）→ 移动端 + 图表（第二批）→ 图谱 + 语音（第三批）
5. **Catalog 与 Rag 的 DomainRouterV2 可合并**：域 Embedding 预计算放 catalog，运行时路由匹配放 rag

---

> **说明**：P3 阶段不新增 Maven 模块，所有变更在已有模块上进行扩展。
> P3 无新增 DDL（CQRS 为基础设施层变更，语义路由为算法升级，不涉及新表）。
