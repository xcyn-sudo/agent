# agent-qr-statistics — P2 任务清单

> 统计模块 P2 扩展：Dashboard 含满意度统计、答案反馈评价（点赞/点踩）。

---

## 1. StatisticsQueryService P2 扩展（含满意度）

- [ ] **1.1** 改造 `org.example.agent_qr.statistics.service.StatisticsQueryService` 类
  - 新增属性注入 `DailyStatsMapper`（确认已有）、`MessageMapper`
  - 方法 `getDashboard()` 中新增统计项：
    - `todayPositive` — 今日点赞数
    - `todayNegative` — 今日点踩数
    - `satisfactionRate` — 满意率 = positive / (positive + negative)
    - `totalFeedbackCount` — 总反馈数
  - 更新 `DashboardVO` 对应字段

---

## 2. FeedbackService — 答案反馈评价

- [ ] **2.1** 创建 `org.example.agent_qr.statistics.service.FeedbackService` 类
  - 注解 `@Service`、`@Slf4j`
  - 属性：`MessageMapper messageMapper`、`DailyStatsMapper dailyStatsMapper`
  - 方法 `submitFeedback(Long messageId, String feedback, String reason, Long userId)`：
    1. 校验消息存在且 role == "assistant"
    2. 调用 `messageMapper.updateFeedback(messageId, feedback, reason)` 更新消息反馈字段
    3. 若 feedback == "positive" → `dailyStatsMapper.incrementPositiveCount(today)`
    4. 若 feedback == "negative" → `dailyStatsMapper.incrementNegativeCount(today)`
    5. 记录日志

---

## 3. MessageMapper P2 扩展

- [ ] **3.1** 在 `org.example.agent_qr.rag.mapper.MessageMapper` 中新增方法
  - `updateFeedback(Long messageId, String feedback, String reason)`：更新 `feedback` 和 `feedback_reason` 字段

---

## 4. DailyStatsMapper P2 扩展

- [ ] **4.1** 在 `org.example.agent_qr.statistics.mapper.DailyStatsMapper` 中新增方法
  - `incrementPositiveCount(LocalDate date)`：`UPDATE stat_daily SET positive_count = positive_count + 1 WHERE stat_date = #{date}`
  - `incrementNegativeCount(LocalDate date)`：`UPDATE stat_daily SET negative_count = negative_count + 1 WHERE stat_date = #{date}`

---

## 5. DDL — chat_message 表扩展

- [ ] **5.1** 编写 ALTER TABLE SQL（追加到 p2-schema.sql）
  - `ALTER TABLE chat_message ADD COLUMN feedback VARCHAR(16) DEFAULT NULL COMMENT '反馈评价 positive/negative'`
  - `ALTER TABLE chat_message ADD COLUMN feedback_reason VARCHAR(500) DEFAULT NULL COMMENT '负面反馈原因'`

---

## 6. DDL — stat_daily 表扩展

- [ ] **6.1** 编写 ALTER TABLE SQL（追加到 p2-schema.sql）
  - `ALTER TABLE stat_daily ADD COLUMN positive_count INT DEFAULT 0 COMMENT '点赞数'`
  - `ALTER TABLE stat_daily ADD COLUMN negative_count INT DEFAULT 0 COMMENT '点踩数'`

---

## 7. DashboardVO P2 扩展

- [ ] **7.1** 在 `org.example.agent_qr.statistics.dto.DashboardVO` 中新增字段
  - `todayPositive`、`todayNegative`、`satisfactionRate`、`totalFeedbackCount`

---

## 8. StatisticsController P2 扩展

- [ ] **8.1** 在 `StatisticsController` 中新增端点
  - `POST /api/statistics/feedback/{messageId}`：接收 `FeedbackDTO(feedback, reason)` → 调用 `feedbackService.submitFeedback()`

---

## 9. pom.xml 依赖

- [ ] **9.1** 确认 `agent-qr-statistics/pom.xml` 无需新增依赖
