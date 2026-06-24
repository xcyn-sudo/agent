package org.example.agent_qr.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.rag.entity.Conversation;
import org.example.agent_qr.rag.mapper.ConversationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会话服务，管理聊天会话的创建、查询和删除。
 *
 * @author agent-qr
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;

    /**
     * 创建新的聊天会话。
     *
     * @param userId 用户 ID
     * @param title  会话标题（取用户首条消息的前 30 个字符）
     * @return 新创建的会话 ID
     */
    public Long createConversation(Long userId, String title) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        // 标题取前 30 个字符
        if (title != null && title.length() > 30) {
            title = title.substring(0, 30);
        }
        conversation.setTitle(title != null ? title : "新会话");
        conversation.setMessageCount(0);
        conversationMapper.insert(conversation);
        log.info("创建会话成功，conversationId={}, userId={}", conversation.getId(), userId);
        return conversation.getId();
    }

    /**
     * 查询用户的所有会话列表，按更新时间倒序排列。
     *
     * @param userId 用户 ID
     * @return 会话列表
     */
    public List<Conversation> listConversations(Long userId) {
        return conversationMapper.selectByUserId(userId);
    }

    /**
     * 自增指定会话的消息计数。
     *
     * @param conversationId 会话 ID
     */
    public void incrementMessageCount(Long conversationId) {
        int rows = conversationMapper.incrementMessageCount(conversationId);
        if (rows == 0) {
            log.warn("消息计数自增失败，conversationId={} 不存在", conversationId);
        }
    }

    /**
     * 物理删除指定会话。
     *
     * @param id 会话 ID
     * @throws BusinessException 如果会话不存在
     */
    public void deleteConversation(Long id) {
        int rows = conversationMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("会话不存在");
        }
        log.info("删除会话成功，conversationId={}", id);
    }
}
