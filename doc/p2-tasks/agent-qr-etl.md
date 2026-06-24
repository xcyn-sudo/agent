# agent-qr-etl — P2 任务清单（★ 新模块）

> ETL 标准化管道模块：多源异构数据统一为 Canonical 格式、字段映射引擎、结构化数据转自然语言。

---

## 1. Maven 模块初始化

- [ ] **1.1** 创建 `agent-qr-etl/` 目录结构
  - `pom.xml`
  - `src/main/java/org/example/agent_qr/etl/`
  - 子包：`normalizer/`、`converter/`、`engine/`、`entity/`、`enums/`

- [ ] **1.2** 配置 `agent-qr-etl/pom.xml`
  - `groupId`: `org.example`
  - `artifactId`: `agent-qr-etl`
  - 依赖：`agent-qr-common`、`agent-qr-datasource`、`spring-boot-starter`

---

## 2. DataType 枚举

- [ ] **2.1** 创建 `org.example.agent_qr.etl.enums.DataType` 枚举
  - 值：`STRUCTURED`（结构化 / 数据库表行）、`SEMI_STRUCTURED`（半结构化 / JSON/XML）、`UNSTRUCTURED`（非结构化 / 文件内容）

---

## 3. CanonicalRecord — 统一标准记录

- [ ] **3.1** 创建 `org.example.agent_qr.etl.entity.CanonicalRecord` 类
  - 注解 `@Data`
  - 属性：`String sourceSystem`、`String domain`、`DataType dataType`、`String canonicalText`（标准化文本）、`Map<String, Object> metadata`、`Long datasourceId`、`String syncBatchId`

---

## 4. FieldMapping — 字段映射配置

- [ ] **4.1** 创建 `org.example.agent_qr.etl.entity.FieldMapping` 类
  - 注解 `@Data`
  - 属性：`String canonicalField`（目标标准字段名）、`String sourceField`（源字段名）、`String displayName`（中文显示名）、`String template`（自然语言模板，如 "{字段中文名}为{值}{单位}"）、`String unit`（单位）、`String transformRule`（转换规则：DATE_TO_CHINESE / MONEY_FORMAT / PERCENTAGE）、`Map<String, String> dictMapping`（字典翻译映射）、`int priority`（排序优先级）、`String status`（ACTIVE / INACTIVE）

---

## 5. 字段映射引擎 — FieldMappingEngine

- [ ] **5.1** 创建 `org.example.agent_qr.etl.engine.FieldMappingEngine` 类
  - 注解 `@Component`、`@Slf4j`
  - 方法 `apply(Map<String, Object> rawRecord, DataSourceConfig config)`：
    1. 获取 config 的 fieldMappings 列表
    2. 对每个映射：从 rawRecord 提取 sourceField 值 → 存入 canonicalField key
    3. 返回 `Map<String, Object>`（key = canonicalField）
  - 方法 `applyDictionary(String rawValue, Map<String, String> dictMapping)`：字典翻译（如 "D01" → "研发部"）
  - 方法 `applyFormat(String value, FieldMapping field)`：格式转换
    - `"DATE_TO_CHINESE"` → "2024-01-15" → "2024年1月15日"
    - `"MONEY_FORMAT"` → "15000.00" → "15,000"
    - `"PERCENTAGE"` → "0.85" → "85.0%"

---

## 6. 结构化数据转自然语言 — StructuredDataConverter

- [ ] **6.1** 创建 `org.example.agent_qr.etl.converter.StructuredDataConverter` 类
  - 注解 `@Component`
  - 属性：`FieldMappingEngine fieldMappingEngine`
  - 方法 `convert(Map<String, Object> mappedRecord, DataSourceConfig config)`：
    1. 添加段落标题：`【{sourceName}】 - {tableComment}`
    2. 按 priority 排序字段
    3. 逐字段应用字典翻译 → 格式转换 → 模板生成自然语言文本
    4. 示例输出："员工张三，所属部门为研发部，月薪15000元，入职日期为2024年1月15日。"
    5. 返回完整段落字符串

---

## 7. DataNormalizer — 数据标准化引擎

- [ ] **7.1** 创建 `org.example.agent_qr.etl.normalizer.DataNormalizer` 类
  - 注解 `@Component`、`@Slf4j`
  - 属性：`FieldMappingEngine fieldMappingEngine`、`StructuredDataConverter structuredConverter`
  - 方法 `normalize(List<Map<String, Object>> rawData, DataSourceConfig config)`：
    1. 遍历每条 raw record
    2. 数据分类 `classify(record, config)`：
       - JDBC → STRUCTURED
       - 含 `_file_type` → UNSTRUCTURED
       - 含 `_json_path` / `_xml_path` → SEMI_STRUCTURED
       - 其他 → UNSTRUCTURED
    3. 字段映射：`fieldMappingEngine.apply(record, config)`
    4. 根据 dataType 生成 canonicalText：
       - STRUCTURED → `structuredConverter.convert(mapped, config)`
       - SEMI_STRUCTURED → 提取关键字段 + 原文
       - UNSTRUCTURED → 直接透传 content
    5. 构建 `CanonicalRecord`
    6. 返回 `List<CanonicalRecord>`

---

## 8. DataETLedEvent 事件类

- [ ] **8.1** 创建 `org.example.agent_qr.common.event.DataETLedEvent` 类（位于 agent-qr-common 模块）
  - 继承 `ApplicationEvent`
  - 字段：`String domain`、`String sourceName`、`int entityCount`、`String syncBatchId`
