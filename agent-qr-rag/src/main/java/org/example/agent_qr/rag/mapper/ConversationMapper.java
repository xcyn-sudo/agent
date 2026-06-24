package org.example.agent_qr.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.agent_qr.rag.entity.Conversation;

import java.util.List;

/**
 * 会话数据访问层，提供会话相关的数据库操作。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动拥有基础 CRUD 方法，
 * 同时扩展了按用户查询会话列表和消息计数自增方法。
 * </p>
 *
 * @author agent-qr
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    /**
     * 根据用户 ID 查询会话列表，按更新时间倒序排列。
     *
     * @param userId 用户 ID
     * @return 会话列表
     */
    @Select("SELECT * FROM chat_conversation WHERE user_id = #{userId} ORDER BY update_time DESC")
    List<Conversation> selectByUserId(@Param("userId") Long userId);

    /**
     * 自增指定会话的消息计数。
     *
     * @param id 会话 ID
     * @return 受影响的行数
     */
    @Update("UPDATE chat_conversation SET message_count = message_count + 1 WHERE id = #{id}")
    int incrementMessageCount(@Param("id") Long id);
}
