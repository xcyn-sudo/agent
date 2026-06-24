package org.example.agent_qr.knowledge.parser;

import lombok.RequiredArgsConstructor;
import org.example.agent_qr.common.BusinessException;
import org.springframework.stereotype.Service;

/**
 * 文档解析路由服务，根据文件类型将解析任务分发给对应的解析器。
 * <p>
 * 支持的格式：PDF（PdfParser）、DOCX（DocxParser）、
 * TXT/MD（TextParser）。其他类型将抛出异常。
 * </p>
 *
 * @author agent-qr
 */
@Service
@RequiredArgsConstructor
public class DocumentParserService {

    private final PdfParser pdfParser;
    private final DocxParser docxParser;
    private final TextParser textParser;

    /**
     * 根据文件路径和类型，选择合适的解析器提取文本。
     *
     * @param filePath 文件路径
     * @param fileType 文件类型（扩展名，如 pdf、docx、txt、md）
     * @return 解析后的文本内容
     * @throws BusinessException 如果文件类型不支持
     */
    public String parse(String filePath, String fileType) {
        return switch (fileType.toLowerCase()) {
            case "pdf"  -> pdfParser.parse(filePath);
            case "docx" -> docxParser.parse(filePath);
            case "txt", "md" -> textParser.parse(filePath);
            default -> throw new BusinessException("不支持的文件类型: " + fileType);
        };
    }
}
