package org.example.agent_qr.knowledge.parser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 纯文本 / Markdown 文件解析器。
 * <p>
 * 直接以 UTF-8 编码读取文件全部内容。
 * 支持 .txt 和 .md 格式。
 * </p>
 *
 * @author agent-qr
 */
@Component
public class TextParser {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * 解析文本文件，返回全部文本内容。
     *
     * @param filePath 文件的相对存储路径（由 FileStorageService 返回）
     * @return 文件文本内容
     * @throws RuntimeException 如果读取文件失败
     */
    public String parse(String filePath) {
        try {
            Path fullPath = Paths.get(uploadDir, filePath);
            return Files.readString(fullPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("文本文件读取失败: " + e.getMessage(), e);
        }
    }
}
