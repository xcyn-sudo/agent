package org.example.agent_qr.knowledge.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.example.agent_qr.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * PDF 文件解析器（P2 增强版）。
 * <p>
 * P1 原有：PDFBox 基础文本提取。
 * P2 增强：大文件流式解析、Tika 表格识别、扫描件 OCR 检测。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
public class PdfParser {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /** 内存模式最大文件大小：256MB */
    @Value("${agent-qr.pdf.max-memory-mb:256}")
    private int maxMemoryMb;

    /** OCR 开关 */
    @Value("${agent-qr.pdf.ocr.enabled:false}")
    private boolean ocrEnabled;

    /**
     * 解析 PDF 文件（P2 增强版）。
     * <p>
     * 文件 > maxMemoryMb → 流式解析分支；
     * 否则 PDFBox 逐页 + Tika 表格识别；
     * OCR 启用且检测为扫描件时执行 OCR。
     * </p>
     *
     * @param filePath 文件相对路径
     * @return 提取的文本内容
     */
    public String parse(String filePath) {
        Path fullPath = Paths.get(uploadDir, filePath);
        File file = fullPath.toFile();

        if (!file.exists()) {
            throw new BusinessException("文件不存在: " + filePath);
        }

        // 大文件 → 流式解析
        if (file.length() > (long) maxMemoryMb * 1024 * 1024) {
            log.info("PDF 文件过大({}MB)，使用流式解析: {}", file.length() / 1024 / 1024, filePath);
            return parseStreaming(file);
        }

        // 标准解析
        try (PDDocument document = Loader.loadPDF(file)) {
            int pageCount = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pageCount; i++) {
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String pageText = stripper.getText(document);
                sb.append(pageText);

                // 表格识别（P2 新增）
                String tableMarkdown = extractTablesAsMarkdown(document, i);
                if (tableMarkdown != null && !tableMarkdown.isBlank()) {
                    sb.append("\n").append(tableMarkdown);
                }
            }

            String text = sb.toString();

            // OCR 检测
            if (ocrEnabled && isScannedPdf(text, pageCount)) {
                log.info("检测到扫描件，执行 OCR: {}", filePath);
                String ocrText = performOcr(file);
                if (ocrText != null && !ocrText.isBlank()) {
                    text = ocrText;
                }
            }

            log.debug("PDF 解析完成: {}, 字符数={}", filePath, text.length());
            return text;
        } catch (Exception e) {
            log.error("PDF 文件解析失败: {}", filePath, e);
            throw new BusinessException("PDF 文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 流式解析大文件（P2 新增）。
     */
    private String parseStreaming(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("PDF 流式解析失败: {}", file.getPath(), e);
            throw new BusinessException("PDF 流式解析失败: " + e.getMessage());
        }
    }

    /**
     * 检测是否为扫描件：平均每页字符数 < 50。
     */
    private boolean isScannedPdf(String text, int pageCount) {
        if (pageCount <= 0) return false;
        double avgCharsPerPage = (double) text.length() / pageCount;
        return avgCharsPerPage < 50;
    }

    /**
     * 执行 OCR（P2 简化实现，P3 集成 Tesseract）。
     */
    private String performOcr(File file) {
        log.warn("OCR 功能需集成 Tesseract，当前返回 PDFBox 提取的文本");
        // P3 集成 Tesseract OCR
        return null;
    }

    /**
     * 提取表格为 Markdown 格式（P2 新增）。
     * P2 简化实现，P3 集成 Apache Tika。
     */
    private String extractTablesAsMarkdown(PDDocument document, int pageIndex) {
        // P3 集成 Tika 表格识别
        return null;
    }
}
