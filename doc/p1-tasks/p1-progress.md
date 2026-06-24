# P1 阶段总体进度

> 更新日期：2026-06-17

---

## 模块完成状态

| # | 模块 | 状态 | 完成度 | 备注 |
|---|------|------|--------|------|
| 1 | **agent-qr-common** | ⬜ 未开始 | 0/6 | Result、BusinessException、MybatisPlusConfig、SpringContextUtil、5个Event |
| 2 | **agent-qr-auth** | ⬜ 未开始 | 0/7 | AuthController、AuthService、PasswordUtil、JwtUtil、JwtAuthFilter |
| 3 | **agent-qr-user** | ⬜ 未开始 | 0/6 | SysUser、SysUserMapper、MyMetaObjectHandler、DTO、AdminController |
| 4 | **agent-qr-knowledge** | ⬜ 未开始 | 0/11 | KnowledgeController、DocumentCommandService、FileStorage、Parsers、TextSplitter、Listeners |
| 5 | **agent-qr-rag** | ⬜ 未开始 | 0/12 | LLMProvider、EmbeddingProvider、DeepSeek实现、ChromaRetriever、ChatQueryService、PromptTemplate |
| 6 | **agent-qr-statistics** | ⬜ 未开始 | 0/7 | DailyStats、StatisticsQueryService、StatisticsController、StatisticsUpdateListener |
| 7 | **agent-qr-web** | ⬜ 未开始 | 0/10 | 启动类、GlobalExceptionHandler、CorsConfig、SecurityConfig、AsyncConfig、application.yml |
| — | **根 pom.xml 调整** | ⬜ 未开始 | — | 新增 rag/user 模块、修正 web 模块名 |

---

## 开发顺序建议

```
第 1 步：agent-qr-web（模块重命名 + 根 pom 调整 + 配置文件）
          ↓
第 2 步：agent-qr-common（所有模块都依赖它，需最先完成）
          ↓
第 3 步：agent-qr-user（被 auth 模块依赖）
          ↓
第 4 步：agent-qr-auth（依赖 user 模块）
          ↓
第 5 步：agent-qr-rag（Provider 策略接口 + DeepSeek 实现）
          ↓
第 6 步：agent-qr-knowledge（依赖 common + rag）
          ↓
第 7 步：agent-qr-statistics（依赖 common + knowledge + user）
```

---

## 依赖关系速查

```
                    ┌─────────────────┐
                    │  agent-qr-web   │  ← 启动类、全局配置、配置文件
                    └───────┬─────────┘
                            │ 依赖所有业务模块
        ┌───────────────────┼───────────────────────────┐
        │                   │                           │
  ┌─────┴─────┐      ┌──────┴──────┐            ┌──────┴──────┐
  │   auth    │      │  knowledge   │            │ statistics  │
  └─────┬─────┘      └──┬─────┬────┘            └──┬─────┬────┘
        │               │     │                    │     │
        │          ┌────┘     └────┐          ┌────┘     │
        │          ▼              ▼          ▼          │
        │    ┌──────────┐  ┌──────────┐  ┌──────────┐  │
        │    │   rag    │  │  common  │  │   user   │  │
        │    └──────────┘  └──────────┘  └──────────┘  │
        │         │              ▲              ▲       │
        └─────────┼──────────────┘              │       │
                  └─────────────────────────────┘       │
                  ┌─────────────────────────────────────┘
                  │
            ┌─────┴─────┐
            │   user    │  ← auth / knowledge / statistics 都依赖
            └───────────┘
```
