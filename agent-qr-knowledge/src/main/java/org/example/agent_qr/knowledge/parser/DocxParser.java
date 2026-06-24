package org.example.agent_qr.knowledge.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.example.agent_qr.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * DOCX 文件解析器（P2 增强版）。
 * <p>
 * P1 原有：段落文本提取。
 * P2 增强：表格转 Markdown、更完善的元素遍历。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class DocxParser {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * 解析 DOCX 文件（P2 增强版）。
     * <p>
     * 遍历 IBodyElement → 段落提取文本 + XWPFTable → tableToMarkdown。
     * </p>
     *
     * @param filePath 文件相对路径
     * @return 提取的文本内容
     */
    public String parse(String filePath) {
        Path fullPath = Paths.get(uploadDir, filePath);
        try (FileInputStream fis = new FileInputStream(fullPath.toFile());
             XWPFDocument doc = new XWPFDocument(fis)) {

            StringBuilder sb = new StringBuilder();

            for (IBodyElement element : doc.getBodyElements()) {
                switch (element.getElementType()) {
                    case PARAGRAPH -> {
                        XWPFParagraph paragraph = (XWPFParagraph) element;
                        String text = paragraph.getText();
                        if (text != null && !text.isBlank()) {
                            if (sb.length() > 0) {
                                sb.append("\n");
                            }
                            sb.append(text);
                        }
                    }
                    case TABLE -> {
                        XWPFTable table = (XWPFTable) element;
                        String markdown = tableToMarkdown(table);
                        if (!markdown.isBlank()) {
                            if (sb.length() > 0) {
                                sb.append("\n\n");
                            }
                            sb.append(markdown);
                        }
                    }
                    default -> {
                        // 忽略其他元素类型
                    }
                }
            }

            String result = sb.toString();
            log.debug("DOCX 解析完成: {}, 字符数={}", filePath, result.length());
            return result;
        } catch (Exception e) {
            log.error("DOCX 文件解析失败: {}", filePath, e);
            throw new BusinessException("DOCX 文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 将 XWPFTable 转换为 Markdown 表格（P2 新增）。
     */
    private String tableToMarkdown(XWPFTable table) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) {
            return "";
        }

        StringBuilder md = new StringBuilder();
        int maxCols = 0;

        // 第一遍：确定最大列数
        for (XWPFTableRow row : rows) {
            maxCols = Math.max(maxCols, row.getTableCells().size());
        }

        // 构建表头和分隔行
        boolean isFirstRow = true;
        for (XWPFTableRow row : rows) {
            List<XWPFTableCell> cells = row.getTableCells();
            md.append("| ");
            for (int c = 0; c < maxCols; c++) {
                String cellText = c < cells.size() ? cells.get(c).getText().replace("\n", " ") : "";
                md.append(cellText).append(" | ");
            }
            md.append("\n");

            if (isFirstRow) {
                md.append("| ");
                for (int c = 0; c < maxCols; c++) {
                    md.append("--- | ");
                }
                md.append("\n");
                isFirstRow = false;
            }
        }

        return md.toString();
    }
}
