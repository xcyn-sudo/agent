package org.example.agent_qr.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应类，用于封装所有 API 返回结果。
 *
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    /**
     * 成功响应（无数据）。
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null, System.currentTimeMillis());
    }

    /**
     * 成功响应（带数据）。
     *
     * @param data 响应数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data, System.currentTimeMillis());
    }

    /**
     * 成功响应（自定义消息，无数据）。
     *
     * @param message 成功消息
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null, System.currentTimeMillis());
    }

    /**
     * 成功响应（自定义消息 + 数据）。
     *
     * @param message 成功消息
     * @param data    响应数据
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data, System.currentTimeMillis());
    }

    /**
     * 错误响应。
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }
}
