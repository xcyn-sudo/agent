package org.example.agent_qr.rag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.catalog.dto.DomainRoutingResult;
import org.example.agent_qr.catalog.router.DomainRouter;
import org.example.agent_qr.common.event.AnswerGeneratedEvent;
import org.example.agent_qr.rag.circuitbreaker.LLMCircuitBreaker;
import org.example.agent_qr.rag.entity.Message;
import org.example.agent_qr.rag.entity.RetrievedDocument;
import org.example.agent_qr.rag.filter.FilterCondition;
import org.example.agent_qr.rag.mapper.MessageMapper;
import org.example.agent_qr.rag.prompt.PromptTemplate;
import org.example.agent_qr.rag.provider.EmbeddingProvider;
import org.example.agent_qr.rag.provider.LLMProvider;
import org.example.agent_qr.rag.provider.ProviderFactory;
import org.example.agent_qr.rag.retriever.HybridRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 问答核心服务。
 * <p>
 * P1 原有：同步 RAG 问答（ask 方法）。
 * P2 扩展：SSE 流式输出（askStream 方法），集成混合检索、熔断器和域路由。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Service
public class ChatQueryService {

    private final ProviderFactory providerFactory;
    private final HybridRetriever hybridRetriever;
    private final PromptTemplate promptTemplate;
    private final ConversationService conversationService;
    private final MessageMapper messageMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final LLMCircuitBreaker circuitBreaker;

    /** P2 域路由器 — 可选注入，避免与 catalog 模块的硬循环依赖 */
    @Autowired(required = false)
    private DomainRouter domainRouter;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ChatQueryService(ProviderFactory providerFactory,
                            HybridRetriever hybridRetriever,
                            PromptTemplate promptTemplate,
                            ConversationService conversationService,
                            MessageMapper messageMapper,
                            ApplicationEventPublisher eventPublisher,
                            LLMCircuitBreaker circuitBreaker) {
        this.providerFactory = providerFactory;
        this.hybridRetriever = hybridRetriever;
        this.promptTemplate = promptTemplate;
        this.conversationService = conversationService;
        this.messageMapper = messageMapper;
        this.eventPublisher = eventPublisher;
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * 执行同步问答流程（P1 保留）。
     */
    public Map<String, Object> ask(String query, Long conversationId, Long userId) {
        // 1. 会话管理
        if (conversationId == null) {
            conversationId = conversationService.createConversation(userId, query);
        }

        // 2. 保存用户消息
        Message userMessage = new Message();
        userMessage.setConversationId(conversationId);
        userMessage.setRole("user");
        userMessage.setContent(query);
        messageMapper.insert(userMessage);

        // 3. 消息计数 +1
        conversationService.incrementMessageCount(conversationId);

        // 4. 向量化用户问题
        EmbeddingProvider embeddingProvider = providerFactory.getEmbeddingProvider();
        float[] queryEmbedding = embeddingProvider.embed(query);

        // 5. P2: 域路由 + 混合检索（domainRouter 可选）
        DomainRoutingResult routing = resolveRouting(query);
        List<RetrievedDocument> retrievedDocs = hybridRetriever.hybridSearch(
                query, queryEmbedding, routing, List.of());

        // 6. 无结果处理
        String answer;
        String sourcesJson = "[]";
        List<Map<String, Object>> sources = new ArrayList<>();

        if (retrievedDocs.isEmpty()) {
            answer = "知识库中暂无相关信息";
            log.info("混合检索无结果，conversationId={}", conversationId);
        } else {
            String contextText = retrievedDocs.stream()
                    .map(doc -> String.format("【%s】\n%s", doc.getDocumentTitle(), doc.getContent()))
                    .collect(Collectors.joining("\n\n"));

            String prompt = promptTemplate.build(query, contextText);

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage(prompt));

            LLMProvider llmProvider = circuitBreaker.getActiveProvider();
            try {
                answer = llmProvider.generate(messages);
                circuitBreaker.recordSuccess();
            } catch (Exception e) {
                circuitBreaker.recordFailure();
                log.error("LLM 调用失败", e);
                answer = "抱歉，AI 服务暂时不可用，请稍后重试";
            }

            for (RetrievedDocument doc : retrievedDocs) {
                Map<String, Object> sourceMap = new HashMap<>();
                sourceMap.put("documentId", doc.getDocumentId());
                sourceMap.put("documentTitle", doc.getDocumentTitle());
                sourceMap.put("content", doc.getContent());
                sourceMap.put("similarity", doc.getSimilarity());
                sources.add(sourceMap);
            }
            try {
                sourcesJson = OBJECT_MAPPER.writeValueAsString(sources);
            } catch (JsonProcessingException e) {
                log.error("序列化 sources 失败", e);
            }
        }

        // 保存助手消息
        saveMessage(conversationId, "assistant", answer, sourcesJson);
        conversationService.incrementMessageCount(conversationId);

        // 发布事件
        eventPublisher.publishEvent(new AnswerGeneratedEvent(this, userId, conversationId));

        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("conversationId", conversationId);
        result.put("sources", sources);
        log.info("问答流程完成，conversationId={}, 检索文档数={}", conversationId, retrievedDocs.size());
        return result;
    }

    /**
     * SSE 流式问答（P2 新增）。
     */
    public void askStream(String query, Long conversationId, Long userId, SseEmitter emitter) {
        try {
            if (conversationId == null) {
                conversationId = conversationService.createConversation(userId, query);
            }

            Message userMessage = new Message();
            userMessage.setConversationId(conversationId);
            userMessage.setRole("user");
            userMessage.setContent(query);
            messageMapper.insert(userMessage);
            conversationService.incrementMessageCount(conversationId);

            EmbeddingProvider embeddingProvider = providerFactory.getEmbeddingProvider();
            float[] queryEmbedding = embeddingProvider.embed(query);
            DomainRoutingResult routing = resolveRouting(query);
            List<RetrievedDocument> retrievedDocs = hybridRetriever.hybridSearch(
                    query, queryEmbedding, routing, List.of());

            List<Map<String, Object>> sources = new ArrayList<>();
            if (!retrievedDocs.isEmpty()) {
                for (RetrievedDocument doc : retrievedDocs) {
                    Map<String, Object> sourceMap = new HashMap<>();
                    sourceMap.put("documentId", doc.getDocumentId());
                    sourceMap.put("documentTitle", doc.getDocumentTitle());
                    sourceMap.put("content", doc.getContent());
                    sourceMap.put("similarity", doc.getSimilarity());
                    sources.add(sourceMap);
                }
            }

            String contextText = retrievedDocs.stream()
                    .map(doc -> String.format("【%s】\n%s", doc.getDocumentTitle(), doc.getContent()))
                    .collect(Collectors.joining("\n\n"));
            String prompt = promptTemplate.build(query, contextText);

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage(prompt));

            LLMProvider llmProvider = circuitBreaker.getActiveProvider();
            StringBuilder fullAnswer = new StringBuilder();

            final Long finalConversationId = conversationId;
            llmProvider.generateStream(messages)
                    .doOnNext(token -> {
                        fullAnswer.append(token);
                        sendSseEvent(emitter, "token", token);
                    })
                    .doOnComplete(() -> {
                        try {
                            String sourcesJson = OBJECT_MAPPER.writeValueAsString(sources);
                            saveMessage(finalConversationId, "assistant", fullAnswer.toString(), sourcesJson);
                            conversationService.incrementMessageCount(finalConversationId);

                            Map<String, Object> doneData = new HashMap<>();
                            doneData.put("conversationId", finalConversationId);
                            doneData.put("sources", sources);
                            doneData.put("answer", fullAnswer.toString());
                            sendSseEvent(emitter, "done", doneData);

                            eventPublisher.publishEvent(new AnswerGeneratedEvent(this, userId, finalConversationId));
                            circuitBreaker.recordSuccess();
                            emitter.complete();

                            log.info("SSE 流式问答完成，conversationId={}", finalConversationId);
                        } catch (Exception e) {
                            log.error("SSE 完成处理失败", e);
                            emitter.completeWithError(e);
                        }
                    })
                    .doOnError(error -> {
                        log.error("SSE 流式生成失败", error);
                        circuitBreaker.recordFailure();
                        sendSseEvent(emitter, "error", Map.of("message", error.getMessage()));

                        if (!fullAnswer.isEmpty()) {
                            saveMessage(finalConversationId, "assistant",
                                    fullAnswer.toString() + "\n[生成中断]", "[]");
                        }
                        emitter.completeWithError(error);
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("SSE 流式问答初始化失败", e);
            sendSseEvent(emitter, "error", Map.of("message", e.getMessage()));
            emitter.completeWithError(e);
        }
    }

    /**
     * 解析域路由（DomainRouter 可选，不可用时降级到全局检索）。
     */
    private DomainRoutingResult resolveRouting(String query) {
        if (domainRouter != null) {
            try {
                return domainRouter.route(query);
            } catch (Exception e) {
                log.warn("域路由失败，降级到全局检索: {}", e.getMessage());
            }
        }
        return DomainRoutingResult.fallback();
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.error("SSE 事件发送失败: eventName={}", eventName, e);
        }
    }

    private void saveMessage(Long conversationId, String role, String content, String sources) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setSources(sources);
        messageMapper.insert(message);
    }
}
