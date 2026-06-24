package org.example.agent_qr.knowledge.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.dlq.DeadLetterQueue;
import org.example.agent_qr.common.event.ChunksCreatedEvent;
import org.example.agent_qr.common.event.DocumentParsedEvent;
import org.example.agent_qr.common.event.EmbeddingCompletedEvent;
import org.example.agent_qr.knowledge.entity.Chunk;
import org.example.agent_qr.knowledge.enums.DocumentStatus;
import org.example.agent_qr.knowledge.mapper.ChunkMapper;
import org.example.agent_qr.knowledge.mapper.DocumentMapper;
import org.example.agent_qr.knowledge.splitter.TextSplitter;
import org.example.agent_qr.rag.embedding.BatchEmbeddingService;
import org.example.agent_qr.rag.provider.EmbeddingProvider;
import org.example.agent_qr.rag.provider.ProviderFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 切片向量化事件监听器（P2 增强版）。
 * <p>
 * P1 原有：切片 + 向量化流程。
 * P2 增强：BatchEmbeddingService 攒批处理 + DLQ 死信队列重试。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkEmbeddingListener {

    private final DocumentMapper documentMapper;
    private final ChunkMapper chunkMapper;
    private final TextSplitter textSplitter;
    private final ProviderFactory providerFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final DeadLetterQueue deadLetterQueue;
    private final BatchEmbeddingService batchEmbeddingService;

    /**
     * 处理文档解析完成事件（P2 BatchEmbedding + DLQ 版）。
     */
    @Async("chunkExecutor")
    @EventListener
    public void handleDocumentParsed(DocumentParsedEvent event) {
        Long documentId = event.getDocumentId();
        log.info("开始处理文档切片与向量化: id={}", documentId);

        try {
            // 1. 更新状态为 CHUNKING
            documentMapper.updateStatus(documentId, DocumentStatus.CHUNKING.name());

            // 2. 文本切片
            List<String> chunkTexts = textSplitter.split(event.getContent());
            log.info("文本切片完成: documentId={}, 切片数={}", documentId, chunkTexts.size());

            // 3. 保存切片到数据库
            for (int i = 0; i < chunkTexts.size(); i++) {
                Chunk chunk = new Chunk();
                chunk.setDocumentId(documentId);
                chunk.setChunkIndex(i);
                chunk.setContent(chunkTexts.get(i));
                chunk.setCharCount(chunkTexts.get(i).length());
                chunk.setChromaId("pending");
                chunk.setDeleted(0);
                chunkMapper.insert(chunk);
            }

            // 4. 发布切片创建事件
            eventPublisher.publishEvent(new ChunksCreatedEvent(this, documentId, chunkTexts));

            // 5. 更新状态为 EMBEDDING
            documentMapper.updateStatus(documentId, DocumentStatus.EMBEDDING.name());

            // 6. P2: 使用 BatchEmbeddingService 攒批向量化
            List<Chunk> savedChunks = chunkMapper.selectByDocumentId(documentId);
            int successCount = 0;
            for (Chunk chunk : savedChunks) {
                try {
                    // 提交到攒批队列（异步完成）
                    batchEmbeddingService.submit(chunk)
                            .thenAccept(vector -> {
                                chunk.setChromaId("embedded");
                                chunkMapper.updateById(chunk);
                            })
                            .exceptionally(ex -> {
                                log.error("切片向量化失败: chunkId={}, error={}", chunk.getId(), ex.getMessage());
                                String payload = String.format("{\"chunkId\":%d,\"documentId\":%d}",
                                        chunk.getId(), documentId);
                                deadLetterQueue.enqueue("EMBED", documentId, payload, ex);
                                return null;
                            });
                    successCount++;
                } catch (Exception e) {
                    log.error("切片向量化提交失败: chunkId={}, error={}", chunk.getId(), e.getMessage());
                    String payload = String.format("{\"chunkId\":%d,\"documentId\":%d}",
                            chunk.getId(), documentId);
                    deadLetterQueue.enqueue("EMBED", documentId, payload, e);
                }
                successCount++;
            }

            // 7. 更新状态为 READY
            documentMapper.updateStatus(documentId, DocumentStatus.READY.name());
            log.info("文档处理完成: id={}, 成功切片数={}", documentId, successCount);

            // 8. 发布向量化完成事件
            eventPublisher.publishEvent(new EmbeddingCompletedEvent(this, documentId, successCount));

        } catch (Exception e) {
            log.error("文档切片/向量化处理失败: id={}, error={}", documentId, e.getMessage(), e);
            documentMapper.updateStatus(documentId, DocumentStatus.FAILED.name());
            documentMapper.updateErrorMsg(documentId, "处理失败: " + e.getMessage());

            // P2: 死信队列入队
            String payload = String.format("{\"documentId\":%d}", documentId);
            deadLetterQueue.enqueue("CHUNK", documentId, payload, e);
        }
    }
}
