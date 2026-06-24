# agent-qr-data-quality — P2 任务清单（★ 新模块）

> 数据质量检查模块：规则链引擎（完整性 → 唯一性 → 格式 → 编码 → 长度）、质检报告生成、合格率阻断判定。

---

## 1. Maven 模块初始化

- [ ] **1.1** 创建 `agent-qr-data-quality/` 目录结构
  - `pom.xml`
  - `src/main/java/org/example/agent_qr/dataquality/`
  - 子包：`checker/`、`rule/`、`entity/`、`util/`

- [ ] **1.2** 配置 `agent-qr-data-quality/pom.xml`
  - `groupId`: `org.example`
  - `artifactId`: `agent-qr-data-quality`
  - 依赖：`agent-qr-common`、`juniversalchardet`、`spring-boot-starter`

---

## 2. 质检规则接口 — QualityRule

- [ ] **2.1** 创建 `org.example.agent_qr.dataquality.rule.QualityRule` 接口
  - 方法 `String getName()`：返回规则名称
  - 方法 `RuleResult evaluate(Map<String, Object> record)`：评估单条记录，返回检查结果

---

## 3. 完整性检查规则 — CompletenessRule

- [ ] **3.1** 创建 `org.example.agent_qr.dataquality.rule.CompletenessRule` 类
  - 实现 `QualityRule`
  - 注解 `@Component`
  - 方法 `getName()`：返回 "完整性"
  - 方法 `evaluate(record)`：
    1. 检查 content / text 等关键字段是否为空
    2. 为空 → `RuleResult.fail("内容字段为空，无法生成有效知识片段")`
    3. 非空 → `RuleResult.pass()`

---

## 4. 编码检查规则 — EncodingRule

- [ ] **4.1** 创建 `org.example.agent_qr.dataquality.rule.EncodingRule` 类
  - 实现 `QualityRule`
  - 注解 `@Component`
  - 属性：`CharsetDetector charsetDetector`
  - 方法 `getName()`：返回 "编码"
  - 方法 `evaluate(record)`：
    1. 遍历 record 所有字段，对 String 值进行字符集检测
    2. 若检测到非 UTF-8 编码 → `RuleResult.fail("字段 '{name}' 编码为 {charset}，需转UTF-8")`
    3. 全部 UTF-8 → `RuleResult.pass()`

---

## 5. 格式检查规则 — FormatRule

- [ ] **5.1** 创建 `org.example.agent_qr.dataquality.rule.FormatRule` 类
  - 实现 `QualityRule`
  - 注解 `@Component`
  - 方法 `getName()`：返回 "格式"
  - 方法 `evaluate(record)`：
    1. 检查日期字段格式（yyyy-MM-dd）
    2. 检查数字字段格式（合法的 Decimal）
    3. 检查百分比字段范围 [0, 100]
    4. 异常 → `RuleResult.fail("字段 '{name}' 格式异常")`
    5. 全部通过 → `RuleResult.pass()`

---

## 6. 字符集检测工具 — CharsetDetector

- [ ] **6.1** 创建 `org.example.agent_qr.dataquality.util.CharsetDetector` 类
  - 注解 `@Component`
  - 方法 `detect(String text)`：使用 juniversalchardet 检测字符串编码
    1. 置信度 ≥ 0.8 → 返回检测到的编码
    2. 置信度 < 0.8 → 回退尝试 [UTF-8, GBK, GB2312, ISO-8859-1, Windows-1252] → 选首个成功解码的
    3. 全部失败 → 返回 null

---

## 7. 数据质量检查引擎 — DataQualityChecker

- [ ] **7.1** 创建 `org.example.agent_qr.dataquality.checker.DataQualityChecker` 类
  - 注解 `@Component`、`@Slf4j`
  - 属性：`List<QualityRule> rules`（Spring 自动注入所有 QualityRule 实现）、`blockThreshold`（默认 0.5）
  - 方法 `check(String batchId, List<Map<String, Object>> rawData)`：
    1. 遍历每条记录 → 按规则链顺序执行所有规则（完整性 → 唯一性 → 格式 → 编码 → 长度）
    2. 跨记录唯一性检查：`MD5(record.values())` 哈希去重
    3. 统计 passCount / failCount / passRate
    4. passRate < blockThreshold → `report.setBlocked(true)` 阻断整批
    5. 返回 `QualityReport`

---

## 8. 质检相关实体

### 8.1 QualityReport

- [ ] **8.1.1** 创建 `org.example.agent_qr.dataquality.entity.QualityReport` 类
  - 注解 `@Data`
  - 属性：`String batchId`、`int totalCount`、`int passCount`、`int failCount`、`double passRate`、`boolean blocked`、`List<QualityFailure> failures`

### 8.2 RuleResult

- [ ] **8.2.1** 创建 `org.example.agent_qr.dataquality.entity.RuleResult` 类
  - 属性：`boolean passed`、`String reason`
  - 静态方法 `pass()`：返回 passed=true
  - 静态方法 `fail(String reason)`：返回 passed=false + reason

### 8.3 QualityFailure

- [ ] **8.3.1** 创建 `org.example.agent_qr.dataquality.entity.QualityFailure` 类
  - 注解 `@Data`
  - 属性：`String ruleName`、`int recordIndex`、`String reason`

---

## 9. DataQualityPassedEvent 事件类

- [ ] **9.1** 创建 `org.example.agent_qr.common.event.DataQualityPassedEvent` 类（位于 agent-qr-common 模块）
  - 继承 `ApplicationEvent`
  - 字段：`QualityReport report`、`List<Map<String, Object>> passedData`、`String syncBatchId`
