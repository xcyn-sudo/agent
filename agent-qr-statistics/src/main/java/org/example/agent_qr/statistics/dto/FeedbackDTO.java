package org.example.agent_qr.statistics.dto;

import lombok.Data;

/**
 * 满意度反馈请求 DTO。
 *
 * @author agent-qr
 */
@Data
public class FeedbackDTO {

    /** 反馈类型：positive（点赞）/ negative（点踩） */
    private String feedback;

    /** 反馈原因（可选） */
    private String reason;
}
