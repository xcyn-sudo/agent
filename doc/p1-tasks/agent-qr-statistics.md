# agent-qr-statistics — P1 任务清单

> 统计模块：DailyStats 实体、DailyStatsMapper、StatisticsQueryService（基础 Dashboard）、StatisticsController、StatisticsUpdateListener。

---

## 1. DailyStats 实体

- [ ] **1.1** 创建 `org.example.agent_qr.statistics.entity.DailyStats` 类
  - 注解 `@Data`、`@TableName("stat_daily")`
  - 字段：
    | 字段 | 类型 | 说明 |
    |------|------|------|
    | `id` | `Long` | `@TableId(type = IdType.AUTO)` |
    | `statDate` | `LocalDate` | 统计日期 |
    | `qaCount` | `Integer` | 问答数（默认 0） |
    | `userQuestionCount` | `Integer` | 提问用户数（默认 0） |
    | `activeUserCount` | `Integer` | 活跃用户数（默认 0） |
    | `docUploadCount` | `Integer` | 文档上传统计（默认 0） |
    | `createTime` | `LocalDateTime` | `@TableField(fill = FieldFill.INSERT)` |

---

## 2. DailyStatsMapper

- [ ] **2.1** 创建 `org.example.agent_qr.statistics.mapper.DailyStatsMapper` 接口
  - 注解 `@Mapper`，继承 `BaseMapper<DailyStats>`
  - `DailyStats selectByDate(@Param("date") LocalDate date)` — 按日期查统计记录
  - `int incrementDocUploadCount(@Param("date") LocalDate date)` — 文档上传数 +1
  - `int incrementQaCount(@Param("date") LocalDate date, @Param("userId") Long userId)` — 问答数 +1（同时更新活跃用户数）
  - `List<DailyStats> selectWeeklyTrend(@Param("endDate") LocalDate endDate)` — 近 7 天趋势（按日期倒序）

---

## 3. DashboardVO

- [ ] **3.1** 创建 `org.example.agent_qr.statistics.dto.DashboardVO` 类
  - 注解 `@Data`
  - 字段：`Integer todayQA`、`Integer todayNewUsers`、`Long totalDocuments`、`Long totalChunks`、`Long totalUsers`、`List<DailyStats> weeklyTrend`、`Map<String, Long> docTypeDistribution`

---

## 4. StatisticsQueryService

- [ ] **4.1** 创建 `org.example.agent_qr.statistics.service.StatisticsQueryService` 类
  - 注解 `@Service`
  - 注入 `DailyStatsMapper`、`DocumentMapper`（来自 knowledge 模块）、`SysUserMapper`（来自 user 模块）
  - `DashboardVO getDashboard()`：
    1. 查今日 `DailyStats` → 填充今日问答数、今日新用户
    2. 查文档总数、切片总数、用户总数
    3. 查近 7 天问答趋势
    4. 查文档类型分布
    5. 组装返回 `DashboardVO`

---

## 5. StatisticsController

- [ ] **5.1** 创建 `org.example.agent_qr.statistics.controller.StatisticsController` 类
  - 注解 `@RestController`、`@RequestMapping("/api/statistics")`
  - 注入 `StatisticsQueryService`
  - `GET /api/statistics/dashboard` → `Result<DashboardVO>`

---

## 6. StatisticsUpdateListener

- [ ] **6.1** 创建 `org.example.agent_qr.statistics.listener.StatisticsUpdateListener` 类
  - 注解 `@Component`、`@Slf4j`
  - 注入 `DailyStatsMapper`
  - `@EventListener` + `@Async("statExecutor")` 监听 `EmbeddingCompletedEvent`：
    - 当日无记录 → insert docUploadCount=1
    - 当日有记录 → incrementDocUploadCount(+1)
  - `@EventListener` + `@Async("statExecutor")` 监听 `AnswerGeneratedEvent`：
    - 当日无记录 → insert qaCount=1
    - 当日有记录 → incrementQaCount(+1)

---

## 7. pom.xml 依赖

- [ ] **7.1** 在 `agent-qr-statistics/pom.xml` 中配置依赖
  - `agent-qr-common`（模块依赖 — 事件类）
  - `agent-qr-knowledge`（模块依赖 — DocumentMapper）
  - `agent-qr-user`（模块依赖 — SysUserMapper）
  - `mybatis-plus-spring-boot3-starter`
