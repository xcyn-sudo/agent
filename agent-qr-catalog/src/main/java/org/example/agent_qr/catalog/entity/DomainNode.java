package org.example.agent_qr.catalog.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 域节点（一级目录）。
 * <p>
 * 对应一个业务域，包含该域下的数据源列表。
 * </p>
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DomainNode {

    /** 域名称：HR / FINANCE / RD / SALES / COMMON */
    private String domainName;

    /** 该域下的数据源数量 */
    private int sourceCount;

    /** 该域下所有数据源的实体总数 */
    private int totalEntities;

    /** 该域下的数据源节点列表（二级目录） */
    private List<SourceNode> sources = new ArrayList<>();
}
