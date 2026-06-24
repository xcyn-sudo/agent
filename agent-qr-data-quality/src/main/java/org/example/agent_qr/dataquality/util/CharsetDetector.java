package org.example.agent_qr.dataquality.util;

import lombok.extern.slf4j.Slf4j;
import org.mozilla.universalchardet.UniversalDetector;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

/**
 * 字符集检测工具。
 * <p>
 * 使用 juniversalchardet 检测文本编码，
 * 置信度 ≥ 0.8 返回检测结果，否则回退尝试常见编码序列。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class CharsetDetector {

    /** 回退编码序列（按优先级排序） */
    private static final String[] FALLBACK_CHARSETS = {
            "UTF-8", "GBK", "GB2312", "ISO-8859-1", "Windows-1252"
    };

    /**
     * 检测文本的字符编码。
     *
     * @param text 待检测文本
     * @return 检测到的编码名称，检测失败返回 "UTF-8"
     */
    public String detect(String text) {
        if (text == null || text.isEmpty()) {
            return "UTF-8";
        }

        byte[] bytes = text.getBytes(Charset.defaultCharset());

        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(bytes, 0, bytes.length);
        detector.dataEnd();

        String detectedCharset = detector.getDetectedCharset();
        if (detectedCharset != null) {
            log.debug("字符编码检测: detected={}, confidence=high", detectedCharset);
            return detectedCharset;
        }

        // 回退尝试常见编码
        for (String charset : FALLBACK_CHARSETS) {
            try {
                byte[] testBytes = text.getBytes(charset);
                String decoded = new String(testBytes, charset);
                if (text.equals(decoded)) {
                    return charset;
                }
            } catch (Exception ignored) {
                // 编码不支持，继续尝试下一个
            }
        }

        return "UTF-8";
    }
}
