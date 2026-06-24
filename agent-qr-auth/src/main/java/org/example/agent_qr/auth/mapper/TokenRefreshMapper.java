package org.example.agent_qr.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.agent_qr.auth.entity.TokenRefresh;

/**
 * Refresh Token Mapper，提供 Token 持久化操作。
 *
 * @author agent-qr
 */
@Mapper
public interface TokenRefreshMapper extends BaseMapper<TokenRefresh> {

    /**
     * 根据 Token 值查询未撤销的记录。
     *
     * @param token Refresh Token 字符串
     * @return TokenRefresh 实体，不存在返回 null
     */
    @Select("SELECT * FROM token_refresh WHERE token = #{token} AND revoked = 0")
    TokenRefresh selectByToken(@Param("token") String token);

    /**
     * 撤销指定用户的所有 Refresh Token。
     *
     * @param userId 用户 ID
     * @return 影响行数
     */
    @Update("UPDATE token_refresh SET revoked = 1 WHERE user_id = #{userId}")
    int revokeByUserId(@Param("userId") Long userId);
}
