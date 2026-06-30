package org.example.agent_qr.rag.classifier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 查询意图分类器，将用户查询分为语义类（SEMANTIC）和聚合类（AGGREGATION）。
 * <p>
 * 分类策略：规则匹配优先（零延迟、零成本），覆盖 80%+ 中文聚合查询表达。
 * LLM 兜底分类为可选扩展（当前默认关闭，仅规则匹配）。
 * </p>
 *
 * <h3>两类查询的区别</h3>
 * <ul>
 *   <li><b>SEMANTIC</b>：需要最相关的文档段落（"离职流程是什么"），走现有混合检索管道</li>
 *   <li><b>AGGREGATION</b>：需要完整的数据记录列表（"有哪些人已经离职"），走聚合查询路径</li>
 * </ul>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class QueryIntentClassifier {

    /** 查询意图类型 */
    public enum IntentType {
        /** 聚合类：列举/统计，需要完整数据集 */
        AGGREGATION,
        /** 语义类：需要最相关文档 */
        SEMANTIC
    }

    /**
     * 分类用户查询意图。
     * <p>
     * 当前实现：纯规则匹配（正则关键词）。
     * 未来可扩展 LLM 兜底分类（仅规则未命中时）。
     * </p>
     *
     * @param query 用户自然语言查询
     * @return 意图类型
     */
    public IntentType classify(String query) {
        if (query == null || query.isBlank()) {
            return IntentType.SEMANTIC;
        }

        if (matchesAggregationPattern(query)) {
            log.debug("查询意图分类: AGGREGATION — query=\"{}\"", query);
            return IntentType.AGGREGATION;
        }

        log.debug("查询意图分类: SEMANTIC — query=\"{}\"", query);
        return IntentType.SEMANTIC;
    }

    /**
     * 聚合类查询关键词模式匹配。
     * <p>
     * <b>列举模式</b>：哪些人|有哪些|列出|都有谁|所有.*的|名单|哪些.*已经|都有哪些|全部.*的<br>
     * <b>统计模式</b>：有多少|统计|一共|总计|数量|几个|多少人|计数|总共|汇总|合计
     * </p>
     *
     * @param query 用户查询
     * @return true 如果匹配聚合查询模式
     */
    private boolean matchesAggregationPattern(String query) {
        // 列举模式 — 用户想要完整的记录列表
        if (query.matches(".*(哪些人|有哪些|列出|都有谁|所有.*的|名单|哪些.*已经|都有哪些|全部.*的).*")) {
            return true;
        }
        // 统计模式 — 用户想要计数/汇总
        if (query.matches(".*(有多少|统计|一共|总计|数量|几个|多少人|计数|总共|汇总|合计).*")) {
            return true;
        }
        return false;
    }
}
