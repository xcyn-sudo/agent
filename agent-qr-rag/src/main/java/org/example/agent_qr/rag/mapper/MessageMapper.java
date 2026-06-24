package org.example.agent_qr.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.agent_qr.rag.entity.Message;

import java.util.List;

/**
 * 消息数据访问层，提供消息相关的数据库操作。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动拥有基础 CRUD 方法，
 * 同时扩展了按会话 ID 查询消息列表方法。
 * </p>
 *
 * @author agent-qr
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 根据会话 ID 查询消息列表，按创建时间升序排列。
     *
     * @param conversationId 会话 ID
     * @return 消息列表
     */
    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} ORDER BY create_time ASC")
    List<Message> selectByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 更新消息的满意度反馈（P2 新增）。
     */
    @Update("UPDATE chat_message SET feedback = #{feedback}, feedback_reason = #{reason} WHERE id = #{messageId}")
    int updateFeedback(@Param("messageId") Long messageId,
                       @Param("feedback") String feedback,
                       @Param("reason") String reason);
}
