package org.example.agent_qr.common;

/**
 * 业务异常类，用于统一抛出业务层异常。
 * 由 GlobalExceptionHandler 统一捕获处理。
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    /**
     * 使用默认错误码 400 创建业务异常。
     *
     * @param message 异常消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    /**
     * 使用自定义错误码创建业务异常。
     *
     * @param code    错误码
     * @param message 异常消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
