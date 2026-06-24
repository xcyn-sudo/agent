package org.example.agent_qr.knowledge.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 文档状态枚举，描述文档在知识库生命周期中的各个阶段。
 * <p>
 * 状态流转：UPLOADED → PARSING → CHUNKING → EMBEDDING → READY
 * 异常时可能进入 FAILED，删除时进入 DELETING。
 * </p>
 *
 * @author agent-qr
 */
public enum DocumentStatus {

    /** 已上传，等待解析 */
    UPLOADED("已上传"),

    /** 解析中 */
    PARSING("解析中"),

    /** 切片中 */
    CHUNKING("切片中"),

    /** 向量化中 */
    EMBEDDING("向量化中"),

    /** 就绪，可被检索 */
    READY("就绪"),

    /** 处理失败 */
    FAILED("失败"),

    /** 删除中 */
    DELETING("删除中");

    /**
     * MyBatis-Plus 标记：将枚举 name() 存入数据库。
     */
    @EnumValue
    private final String value;

    private final String description;

    DocumentStatus(String description) {
        this.value = this.name();
        this.description = description;
    }

    /**
     * 获取状态的中文描述。
     *
     * @return 中文描述
     */
    public String getDescription() {
        return description;
    }
}
