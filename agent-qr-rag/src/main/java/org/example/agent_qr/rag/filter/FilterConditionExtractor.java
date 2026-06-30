package org.example.agent_qr.rag.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.rag.filter.mapper.ChunkStructuredFilterMapper;
import org.example.agent_qr.rag.provider.LLMProvider;
import org.example.agent_qr.rag.provider.ProviderFactory;
import org.example.agent_qr.rag.provider.dashscope.DashScopeLLMProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LLM 驱动的结构化过滤条件提取器。
 * <p>
 * 从用户自然语言查询中自动提取结构化过滤条件（数值范围、日期范围、枚举值），
 * 转换为 {@link FilterCondition} 列表，用于 MySQL B+ 树前置过滤。
 * </p>
 *
 * <h3>降级策略</h3>
 * <p>
 * 任何环节异常均返回空列表，确保不影响问答主流程可用性：
 * <ul>
 *   <li>enabled=false → 返回 List.of()</li>
 *   <li>域为空或 fallbackToGlobal → 返回 List.of()</li>
 *   <li>域下无结构化字段 → 返回 List.of()</li>
 *   <li>LLM 调用异常 → 返回 List.of()</li>
 *   <li>JSON 解析失败 → 返回 List.of()</li>
 *   <li>字段名/类型/枚举值校验失败 → 丢弃该条件</li>
 * </ul>
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class FilterConditionExtractor {

    @Value("${agent-qr.filter.llm-extract.enabled:false}")
    private boolean enabled;

    @Value("${agent-qr.filter.llm-extract.timeout-seconds:5}")
    private int timeoutSeconds;

    @Autowired
    private ChunkStructuredFilterMapper chunkStructuredFilterMapper;

    @Autowired
    private ProviderFactory providerFactory;

    /** DashScope LLM Provider — 可选注入，优先用于提取（轻量模型 qwen-turbo） */
    @Autowired(required = false)
    private DashScopeLLMProvider dashScopeLLMProvider;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 系统 Prompt 模板：Few-shot 示例引导 LLM 提取结构化条件 */
    private static final String EXTRACTION_SYSTEM_PROMPT = """
            你是一个结构化信息提取助手。用户会提出自然语言查询，你需要从中提取结构化过滤条件。

            ## 规则
            1. 仅提取查询中明确提到的过滤条件，不要臆造。
            2. 每个条件必须包含 fieldName（字段名）、fieldType（NUMBER/DATE/ENUM/STRING）、operator（EQ/GT/GTE/LT/LTE/BETWEEN）和 value（或 minValue/maxValue）。
            3. 数值范围使用 GT/GTE/LT/LTE 或 BETWEEN 操作符。
            4. 日期使用标准格式 yyyy-MM-dd。
            5. 枚举值必须精确匹配可选值列表中的某个值。
            6. 如果查询中没有可提取的结构化条件，返回空数组 []。

            ## 输出格式
            仅输出 JSON 数组，不要包含任何其他文字。每个元素格式：
            {"fieldName": "...", "fieldType": "NUMBER|DATE|ENUM|STRING", "operator": "EQ|GT|GTE|LT|LTE|BETWEEN", "value": "...", "minValue": "...", "maxValue": "..."}

            ## 示例
            用户查询："研发部月收入超过2万的员工有哪些"
            可用字段：department(ENUM:[研发部,销售部,财务部]), monthly_income(NUMBER), hire_date(DATE)
            输出：[{"fieldName":"department","fieldType":"ENUM","operator":"EQ","value":"研发部"},{"fieldName":"monthly_income","fieldType":"NUMBER","operator":"GT","value":"20000"}]

            用户查询："2024年入职的员工"
            可用字段：name(STRING), hire_date(DATE), department(ENUM:[RD,HR])
            输出：[{"fieldName":"hire_date","fieldType":"DATE","operator":"GTE","value":"2024-01-01","minValue":"2024-01-01","maxValue":"2024-12-31"}]

            用户查询："公司有哪些规章制度"
            可用字段：department(ENUM:[RD,HR]), doc_type(ENUM:[制度,流程])
            输出：[]

            用户查询："金额在1万到5万之间的合同"
            可用字段：contract_amount(NUMBER), contract_date(DATE), status(ENUM:[已签,待签])
            输出：[{"fieldName":"contract_amount","fieldType":"NUMBER","operator":"BETWEEN","minValue":"10000","maxValue":"50000"}]

            请严格按照以上格式输出，不要包含 ```json 标记或其他文字。""";

    /**
     * 从用户查询中提取结构化过滤条件。
     *
     * @param query  用户自然语言查询
     * @param domain 业务域（用于查询可用字段定义）
     * @return 过滤条件列表（提取失败时返回空列表）
     */
    public List<FilterCondition> extract(String query, String domain) {
        if (!enabled) {
            log.debug("FilterConditionExtractor 未启用，跳过提取");
            return List.of();
        }

        if (domain == null || domain.isBlank()) {
            log.debug("域为空，跳过过滤条件提取");
            return List.of();
        }

        try {
            // 1. 获取域下可用字段定义
            List<FieldDefinition> availableFields = getAvailableFields(domain);
            if (availableFields.isEmpty()) {
                log.debug("域 {} 下无结构化字段，跳过提取", domain);
                return List.of();
            }

            // 2. 构造 Prompt
            String userPrompt = buildExtractionPrompt(query, availableFields);

            // 3. 调用 LLM
            String llmResponse = callLLM(userPrompt);
            if (llmResponse == null || llmResponse.isBlank()) {
                log.warn("LLM 返回空响应，query={}", query);
                return List.of();
            }

            // 4. 解析 JSON
            List<FilterCondition> conditions = parseResponse(llmResponse);
            int rawCount = conditions.size();

            // 5. 校验
            List<FilterCondition> validConditions = validate(conditions, availableFields);
            int validCount = validConditions.size();

            log.info("结构化过滤条件提取完成: domain={}, query={}, raw={}, valid={}",
                    domain, query, rawCount, validCount);

            if (validCount > 0) {
                log.debug("有效过滤条件: {}", validConditions.stream()
                        .map(c -> c.getFieldName() + " " + c.getOperator() + " " +
                                (c.getValue() != null ? c.getValue() :
                                        c.getMinValue() + "~" + c.getMaxValue()))
                        .collect(Collectors.joining(", ")));
            }

            return validConditions;

        } catch (Exception e) {
            log.warn("结构化过滤条件提取异常，降级全量检索: query={}", query, e);
            return List.of();
        }
    }

    /**
     * 查询域下所有可用字段定义及其枚举值。
     */
    private List<FieldDefinition> getAvailableFields(String domain) {
        try {
            List<FieldDefinition> fields = chunkStructuredFilterMapper
                    .selectDistinctFieldsByDomain(domain);

            // 对 ENUM 类型字段，查询其可选值
            for (FieldDefinition field : fields) {
                if ("ENUM".equals(field.getFieldType())) {
                    List<String> enumValues = chunkStructuredFilterMapper
                            .selectEnumValues(field.getFieldName(), domain);
                    field.setEnumValues(enumValues);
                }
            }

            log.debug("域 {} 可用字段: {}", domain, fields.stream()
                    .map(f -> f.getFieldName() + "(" + f.getFieldType()
                            + (f.getEnumValues() != null ? ":" + f.getEnumValues() : "") + ")")
                    .collect(Collectors.joining(", ")));

            return fields;
        } catch (Exception e) {
            log.warn("查询域 {} 可用字段异常", domain, e);
            return List.of();
        }
    }

    /**
     * 构造发送给 LLM 的用户 Prompt（含可用字段信息）。
     */
    private String buildExtractionPrompt(String query, List<FieldDefinition> fields) {
        StringBuilder fieldsDesc = new StringBuilder();
        for (FieldDefinition f : fields) {
            if (fieldsDesc.length() > 0) {
                fieldsDesc.append(", ");
            }
            fieldsDesc.append(f.getFieldName()).append("(").append(f.getFieldType());
            if (f.getEnumValues() != null && !f.getEnumValues().isEmpty()) {
                fieldsDesc.append(":[").append(String.join(",", f.getEnumValues())).append("]");
            }
            fieldsDesc.append(")");
        }

        return String.format("用户查询：\"%s\"\n可用字段：%s\n请提取结构化过滤条件：",
                query, fieldsDesc.toString());
    }

    /**
     * 调用 LLM 进行条件提取。
     * 优先使用 DashScope（轻量模型），降级到 ProviderFactory.getLLMProvider()。
     */
    private String callLLM(String userPrompt) {
        LLMProvider llmProvider;
        if (dashScopeLLMProvider != null) {
            llmProvider = dashScopeLLMProvider;
            log.debug("使用 DashScope LLM 进行过滤条件提取");
        } else {
            llmProvider = providerFactory.getLLMProvider();
            log.debug("DashScope 不可用，降级到默认 LLM Provider 进行过滤条件提取");
        }

        List<ChatMessage> messages = List.of(
                new SystemMessage(EXTRACTION_SYSTEM_PROMPT),
                new UserMessage(userPrompt)
        );

        return llmProvider.generate(messages);
    }

    /**
     * 解析 LLM 返回的 JSON 响应。
     * <p>
     * 支持两种格式：
     * <ul>
     *   <li>纯 JSON 数组：[{...}, {...}]</li>
     *   <li>Markdown 代码块包裹：```json [...] ```</li>
     * </ul>
     * </p>
     */
    List<FilterCondition> parseResponse(String llmResponse) {
        String json = llmResponse.trim();

        // 清理 markdown 代码块包裹
        if (json.startsWith("```")) {
            // 移除开头的 ```json 或 ```
            int startIdx = json.indexOf('\n');
            if (startIdx > 0) {
                json = json.substring(startIdx + 1);
            } else {
                json = json.substring(3);
            }
            // 移除结尾的 ```
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3).trim();
            }
        }

        // 尝试提取 JSON 数组（处理 LLM 在数组前后添加额外文字的情况）
        int arrayStart = json.indexOf('[');
        int arrayEnd = json.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            json = json.substring(arrayStart, arrayEnd + 1);
        }

        try {
            List<FilterCondition> conditions = OBJECT_MAPPER.readValue(
                    json, new TypeReference<List<FilterCondition>>() {});
            log.debug("解析到 {} 条原始过滤条件", conditions.size());
            return conditions;
        } catch (JsonProcessingException e) {
            log.warn("JSON 解析失败，response preview: {}",
                    llmResponse.length() > 300 ? llmResponse.substring(0, 300) + "..." : llmResponse);
            return List.of();
        }
    }

    /**
     * 校验过滤条件的合法性。
     * <p>
     * 校验规则：
     * <ul>
     *   <li>fieldName 必须在可用字段集合中</li>
     *   <li>fieldType 必须与可用字段定义一致</li>
     *   <li>ENUM 类型的 value 必须在枚举值列表中</li>
     * </ul>
     * 不合法的条件将被丢弃（不阻断其他条件）。
     * </p>
     *
     * @param conditions LLM 返回的原始条件列表
     * @param validFields 域下合法的字段定义
     * @return 校验通过的过滤条件列表
     */
    List<FilterCondition> validate(List<FilterCondition> conditions,
                                   List<FieldDefinition> validFields) {
        // 构建合法字段名 → 字段定义 的映射
        Map<String, FieldDefinition> fieldMap = validFields.stream()
                .collect(Collectors.toMap(
                        FieldDefinition::getFieldName,
                        f -> f,
                        (a, b) -> a));

        List<FilterCondition> valid = new ArrayList<>();
        for (FilterCondition condition : conditions) {
            String fieldName = condition.getFieldName();
            if (fieldName == null || fieldName.isBlank()) {
                log.debug("丢弃条件：字段名为空");
                continue;
            }

            FieldDefinition fieldDef = fieldMap.get(fieldName);
            if (fieldDef == null) {
                log.debug("丢弃条件：字段名 {} 不在合法集合中", fieldName);
                continue;
            }

            // 校验字段类型一致性
            String expectedType = fieldDef.getFieldType();
            String actualType = condition.getFieldType();
            if (actualType != null && !actualType.equals(expectedType)) {
                log.debug("丢弃条件：字段 {} 类型不匹配，期望 {}，实际 {}",
                        fieldName, expectedType, actualType);
                continue;
            }
            // 用系统记录的字段类型覆盖 LLM 可能错误的类型
            condition.setFieldType(expectedType);

            // 对 ENUM 类型，校验 value 是否在枚举值列表中
            if ("ENUM".equals(expectedType) && condition.getValue() != null) {
                List<String> enumValues = fieldDef.getEnumValues();
                if (enumValues != null && !enumValues.isEmpty()
                        && !enumValues.contains(condition.getValue())) {
                    log.debug("丢弃条件：字段 {} 的值 {} 不在枚举列表中 {}",
                            fieldName, condition.getValue(), enumValues);
                    continue;
                }
            }

            valid.add(condition);
        }

        return valid;
    }
}
