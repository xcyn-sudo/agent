package org.example.agent_qr.knowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.auth.evaluator.AbacEvaluator;
import org.example.agent_qr.auth.principal.UserPrincipal;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.common.event.DocumentDeleteRequestedEvent;
import org.example.agent_qr.common.event.DocumentUploadedEvent;
import org.example.agent_qr.knowledge.entity.Document;
import org.example.agent_qr.knowledge.enums.DocumentStatus;
import org.example.agent_qr.knowledge.mapper.ChunkMapper;
import org.example.agent_qr.knowledge.mapper.DocumentMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * 文档命令服务（P2 扩展：ABAC 权限检查 + 软删除）。
 *
 * @author agent-qr
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentCommandService {

    private final DocumentMapper documentMapper;
    private final ChunkMapper chunkMapper;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final AbacEvaluator abacEvaluator;

    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "docx", "txt", "md");
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    @Transactional
    public Document uploadDocument(MultipartFile file, String title, Long userId,
                                    String domain, Integer sensitivityLevel) {
        String originalFilename = file.getOriginalFilename();
        String ext = getFileExtension(originalFilename);

        if (!ALLOWED_TYPES.contains(ext)) {
            throw new BusinessException("不支持的文件类型: " + ext + "，仅支持 pdf、docx、txt、md");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过50MB");
        }

        String filePath = fileStorageService.store(file);
        String docTitle = (title == null || title.isBlank()) ? originalFilename : title;

        Document doc = new Document();
        doc.setTitle(docTitle);
        doc.setFileName(originalFilename);
        doc.setFilePath(filePath);
        doc.setFileType(ext);
        doc.setFileSize(file.getSize());
        doc.setStatus(DocumentStatus.UPLOADED);
        doc.setUploadUserId(userId);
        doc.setDomain(domain);
        doc.setSensitivityLevel(sensitivityLevel);
        doc.setSensitivityLabel(mapSensitivityLabel(sensitivityLevel));
        doc.setDeleted(0);

        documentMapper.insert(doc);

        eventPublisher.publishEvent(new DocumentUploadedEvent(
                this, doc.getId(), filePath, originalFilename, ext, userId));

        log.info("文档上传成功: id={}, title={}, domain={}, sensitivityLevel={}",
                doc.getId(), docTitle, domain, sensitivityLevel);
        return doc;
    }

    /**
     * 请求软删除文档 — 含 ABAC 检查。
     */
    @Transactional
    public void requestDeleteDocument(Long documentId) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在");
        }

        // ABAC 文档级检查
        UserPrincipal user = getCurrentUser();
        if (!abacEvaluator.canDeleteDocument(user, doc.getDomain(), doc.getSensitivityLevel())) {
            throw new AccessDeniedException("无权删除该文档");
        }

        // 标记 DELETING
        documentMapper.updateStatus(documentId, DocumentStatus.DELETING.name());

        // 收集关联信息
        List<Long> chunkIds = chunkMapper.selectByDocumentId(documentId).stream()
                .map(org.example.agent_qr.knowledge.entity.Chunk::getId)
                .toList();
        List<String> chromaIds = chunkMapper.selectChromaIdsByDocumentId(documentId);

        // 发布事件
        DocumentDeleteRequestedEvent event = new DocumentDeleteRequestedEvent(
                documentId, chunkIds, chromaIds, doc.getFilePath());
        eventPublisher.publishEvent(event);

        log.info("文档软删除请求已发布: documentId={}, chunkCount={}, chromaIdCount={}",
                documentId, chunkIds.size(), chromaIds.size());
    }

    @Deprecated
    @Transactional
    public void deleteDocument(Long documentId) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在");
        }
        fileStorageService.delete(doc.getFilePath());
        chunkMapper.deleteByDocumentId(documentId);
        documentMapper.deleteById(documentId);
        log.info("文档已物理删除: id={}, fileName={}", documentId, doc.getFileName());
    }

    private String mapSensitivityLabel(Integer level) {
        if (level == null) return "公开";
        return switch (level) {
            case 1 -> "内部";
            case 2 -> "机密";
            case 3 -> "绝密";
            default -> "公开";
        };
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private UserPrincipal getCurrentUser() {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        throw new AccessDeniedException("无法获取当前用户信息");
    }
}
