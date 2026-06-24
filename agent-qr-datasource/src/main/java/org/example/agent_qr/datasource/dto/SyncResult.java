package org.example.agent_qr.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 数据同步结果。
 *
 * @author agent-qr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult {

    /** 同步的总行数 */
    private int totalRows;

    /** 同步获取的原始数据 */
    private List<Map<String, Object>> rawData;

    /** 下次增量同步的游标 */
    private String nextCursor;

    /**
     * 创建空同步结果。
     */
    public static SyncResult empty() {
        return new SyncResult(0, List.of(), null);
    }
}
