package org.example.agent_qr.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新用户状态请求 DTO。
 * <p>
 * 用于接收管理员禁用/启用用户时的请求数据。
 * 状态值仅允许 0（禁用）或 1（启用）。
 * </p>
 *
 * @author agent-qr
 */
@Data
public class UpdateUserStatusDTO {

    /**
     * 用户状态：1-启用，0-禁用。
     */
    @NotNull(message = "状态值不能为空")
    @Min(value = 0, message = "状态值必须为 0 或 1")
    @Max(value = 1, message = "状态值必须为 0 或 1")
    private Integer status;
}
