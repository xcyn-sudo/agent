package org.example.agent_qr.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.agent_qr.user.entity.SysUser;

import java.time.LocalDate;

/**
 * 系统用户数据访问层，提供用户相关的数据库操作。
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动拥有基础 CRUD 方法，
 * 同时扩展了按用户名查询、分页关键字搜索、按日期统计等自定义方法。
 * </p>
 *
 * @author agent-qr
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名精确查询用户。
     *
     * @param username 用户名
     * @return 匹配的用户实体，不存在则返回 null
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 分页查询用户列表，支持按用户名或真实姓名模糊搜索。
     *
     * @param page    分页对象
     * @param keyword 关键字（模糊匹配 username 或 real_name）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT * FROM sys_user " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (username LIKE CONCAT('%',#{keyword},'%') " +
            "    OR real_name LIKE CONCAT('%',#{keyword},'%'))" +
            "  </if>" +
            "</where>" +
            "ORDER BY create_time DESC" +
            "</script>")
    IPage<SysUser> selectPage(Page<SysUser> page, @Param("keyword") String keyword);

    /**
     * 统计指定日期创建的用户数量。
     *
     * @param date 统计日期
     * @return 该日期创建的用户总数
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE DATE(create_time) = #{date}")
    Long countByDate(@Param("date") LocalDate date);
}
