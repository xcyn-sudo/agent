package org.example.agent_qr.etl.normalizer;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.datasource.entity.DataSourceConfig;
import org.example.agent_qr.etl.entity.CanonicalRecord;
import org.example.agent_qr.etl.entity.FieldMapping;
import org.example.agent_qr.etl.engine.FieldMappingEngine;
import org.example.agent_qr.etl.enums.DataType;
import org.example.agent_qr.etl.converter.StructuredDataConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据标准化器 — ETL 管道入口。
 * <p>
 * 对原始数据执行三步标准化流程：
 * <ol>
 *   <li>分类：根据数据类型特征判断 STRUCTURED/SEMI_STRUCTURED/UNSTRUCTURED</li>
 *   <li>映射：通过 FieldMappingEngine 将源字段映射为标准字段</li>
 *   <li>转换：通过 StructuredDataConverter 生成自然语言文本</li>
 * </ol>
 * 最终输出 CanonicalRecord 列表。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class DataNormalizer {

    @Autowired
    private FieldMappingEngine fieldMappingEngine;

    @Autowired
    private StructuredDataConverter structuredDataConverter;

    /**
     * 对原始数据执行标准化处理。
     *
     * @param rawData 原始数据记录列表
     * @param config  数据源配置（含字段映射）
     * @param batchId 同步批次 ID
     * @return 标准化记录列表
     */
    @SuppressWarnings("unchecked")
    public List<CanonicalRecord> normalize(List<Map<String, Object>> rawData,
                                           DataSourceConfig config,
                                           String batchId) {
        List<CanonicalRecord> records = new ArrayList<>();

        if (rawData == null || rawData.isEmpty()) {
            return records;
        }

        // 解析字段映射配置
        List<FieldMapping> fieldMappings = parseFieldMappings(config.getFieldMapping());
        String sourceName = config.getSourceName();
        String domain = config.getDomain();

        for (Map<String, Object> rawRecord : rawData) {
            // 1. 分类
            DataType dataType = classify(rawRecord);

            // 2. 字段映射
            Map<String, Object> mappedRecord = fieldMappingEngine.apply(rawRecord, fieldMappings);

            // 3. 生成标准化文本
            String canonicalText;
            if (dataType == DataType.STRUCTURED) {
                canonicalText = structuredDataConverter.convert(mappedRecord, fieldMappings, sourceName);
            } else if (dataType == DataType.UNSTRUCTURED) {
                // 非结构化数据直接取内容字段
                canonicalText = extractUnstructuredText(rawRecord);
            } else {
                // 半结构化：取 rawRecord 的 JSON 字符串表示
                canonicalText = rawRecord.toString();
            }

            CanonicalRecord record = CanonicalRecord.builder()
                    .sourceSystem(sourceName)
                    .domain(domain)
                    .dataType(dataType)
                    .canonicalText(canonicalText)
                    .metadata(mappedRecord)
                    .datasourceId(config.getId())
                    .syncBatchId(batchId)
                    .build();

            records.add(record);
        }

        log.info("数据标准化完成: sourceName={}, totalRecords={}, batchId={}",
                sourceName, records.size(), batchId);
        return records;
    }

    /**
     * 分类：根据记录内容判断数据类型。
     * <p>
     * 含 _file_type 字段 → UNSTRUCTURED<br>
     * JDBC 数据源（多字段）→ STRUCTURED<br>
     * 含嵌套结构 → SEMI_STRUCTURED
     * </p>
     */
    private DataType classify(Map<String, Object> record) {
        // 文件类型标记 → 非结构化
        if (record.containsKey("_file_type") || record.containsKey("_file_key")) {
            return DataType.UNSTRUCTURED;
        }

        // 检查是否有嵌套对象（JSON）
        for (Object value : record.values()) {
            if (value instanceof Map || value instanceof List) {
                return DataType.SEMI_STRUCTURED;
            }
        }

        // 默认结构化
        return DataType.STRUCTURED;
    }

    /**
     * 从非结构化记录中提取文本内容。
     */
    private String extractUnstructuredText(Map<String, Object> record) {
        // 优先取 _content 字段（S3 文件内容）
        Object content = record.get("_content");
        if (content != null && !content.toString().isBlank()) {
            return content.toString();
        }
        // 回退到 content 或 text 字段
        Object text = record.getOrDefault("content", record.get("text"));
        return text != null ? text.toString() : "";
    }

    /**
     * 解析字段映射配置 JSON。
     */
    @SuppressWarnings("unchecked")
    private List<FieldMapping> parseFieldMappings(String fieldMappingJson) {
        if (fieldMappingJson == null || fieldMappingJson.isBlank()) {
            return List.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> rawList = mapper.readValue(fieldMappingJson, List.class);
            List<FieldMapping> mappings = new ArrayList<>();
            for (Map<String, Object> raw : rawList) {
                FieldMapping fm = FieldMapping.builder()
                        .canonicalField((String) raw.get("canonicalField"))
                        .sourceField((String) raw.get("sourceField"))
                        .displayName((String) raw.get("displayName"))
                        .template((String) raw.get("template"))
                        .unit((String) raw.get("unit"))
                        .transformRule((String) raw.get("transformRule"))
                        .dictMapping((Map<String, String>) raw.get("dictMapping"))
                        .priority(raw.get("priority") != null ? ((Number) raw.get("priority")).intValue() : 99)
                        .status((String) raw.getOrDefault("status", FieldMapping.STATUS_ACTIVE))
                        .build();
                mappings.add(fm);
            }
            return mappings;
        } catch (Exception e) {
            log.error("字段映射配置解析失败: {}", e.getMessage());
            return List.of();
        }
    }
}
