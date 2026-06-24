package org.example.agent_qr.catalog.service;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.catalog.entity.CatalogTree;
import org.example.agent_qr.catalog.entity.DomainNode;
import org.example.agent_qr.catalog.entity.EntityNode;
import org.example.agent_qr.catalog.entity.SourceNode;
import org.example.agent_qr.common.event.DataETLedEvent;
import org.example.agent_qr.datasource.entity.DataSourceConfig;
import org.example.agent_qr.datasource.mapper.DataSourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识目录服务。
 * <p>
 * 构建三级目录树（域 → 数据源 → 实体），
 * 仅依赖 agent-qr-datasource，不依赖 agent-qr-knowledge（避免循环依赖）。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Service
public class KnowledgeCatalogService {

    @Autowired
    private DataSourceMapper dataSourceMapper;

    /**
     * 获取完整的三级目录树。
     * <p>
     * 一级：按 domain 分组的 DomainNode<br>
     * 二级：每个域下的数据源 SourceNode<br>
     * 三级：每个数据源下的实体 EntityNode（基于数据源元数据构建）
     * </p>
     *
     * @return 目录树
     */
    public CatalogTree getCatalogTree() {
        List<DataSourceConfig> activeSources = dataSourceMapper.selectAllActive();

        // 按 domain 分组
        Map<String, List<DataSourceConfig>> domainGroups = activeSources.stream()
                .collect(Collectors.groupingBy(
                        ds -> ds.getDomain() != null ? ds.getDomain() : "COMMON",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<DomainNode> domains = new ArrayList<>();
        for (Map.Entry<String, List<DataSourceConfig>> entry : domainGroups.entrySet()) {
            String domainName = entry.getKey();
            List<DataSourceConfig> sources = entry.getValue();

            List<SourceNode> sourceNodes = new ArrayList<>();
            for (DataSourceConfig ds : sources) {
                List<EntityNode> entities = getEntitiesForSource(ds);
                SourceNode sourceNode = new SourceNode(
                        ds.getId(),
                        ds.getSourceName(),
                        ds.getSourceType(),
                        ds.getLastSyncAt(),
                        ds.getTotalSynced(),
                        entities
                );
                sourceNodes.add(sourceNode);
            }

            DomainNode domainNode = new DomainNode(domainName, sources.size(), sourceNodes);
            domains.add(domainNode);
        }

        log.debug("目录树构建完成: {} 个域", domains.size());
        return new CatalogTree(domains);
    }

    /**
     * 获取数据源下的实体列表。
     * <p>
     * 基于数据源类型和名称构建实体节点，不再依赖 knowledge 模块。
     * </p>
     */
    private List<EntityNode> getEntitiesForSource(DataSourceConfig ds) {
        List<EntityNode> entities = new ArrayList<>();

        // 根据数据源类型推断实体
        String entityName = ds.getSourceName();
        String entityType = mapSourceTypeToEntityType(ds.getSourceType());

        // 如果 totalSynced 有值则使用，否则设为 0
        int recordCount = ds.getTotalSynced() != null ? ds.getTotalSynced() : 0;
        entities.add(new EntityNode(entityName, entityType, recordCount));

        return entities;
    }

    /**
     * 将数据源类型映射为实体类型。
     */
    private String mapSourceTypeToEntityType(String sourceType) {
        if (sourceType == null) {
            return EntityNode.TYPE_TABLE;
        }
        return switch (sourceType.toUpperCase()) {
            case "JDBC" -> EntityNode.TYPE_TABLE;
            case "REST" -> EntityNode.TYPE_API;
            case "S3" -> EntityNode.TYPE_FILE;
            default -> EntityNode.TYPE_TABLE;
        };
    }

    /**
     * 监听 ETL 完成事件，更新目录索引。
     *
     * @param event ETL 完成事件
     */
    @Async
    @EventListener
    public void onDataETLed(DataETLedEvent event) {
        log.info("收到 ETL 完成事件，更新目录索引: domain={}, sourceName={}, entityCount={}",
                event.getDomain(), event.getSourceName(), event.getEntityCount());
        // 目录索引更新为惰性计算（下次调用 getCatalogTree 时重建），
        // 这里仅记录日志，实际索引在 getCatalogTree() 中实时构建。
    }
}
