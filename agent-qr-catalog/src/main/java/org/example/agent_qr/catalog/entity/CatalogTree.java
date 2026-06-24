package org.example.agent_qr.catalog.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 三级知识目录树。
 * <p>
 * 顶层结构，包含按业务域（domain）分组的一级节点列表。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogTree {

    /** 域节点列表 */
    private List<DomainNode> domains = new ArrayList<>();

    /**
     * 创建空目录树。
     */
    public static CatalogTree empty() {
        return new CatalogTree(new ArrayList<>());
    }
}
