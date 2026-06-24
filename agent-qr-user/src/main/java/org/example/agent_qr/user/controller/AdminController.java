package org.example.agent_qr.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.common.Result;
import org.example.agent_qr.user.dto.CreateUserDTO;
import org.example.agent_qr.user.dto.UpdateUserDTO;
import org.example.agent_qr.user.dto.UpdateUserStatusDTO;
import org.example.agent_qr.user.entity.SysUser;
import org.example.agent_qr.user.mapper.SysUserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员用户管理控制器。
 * <p>
 * 提供用户 CRUD 和状态管理接口，所有接口仅限管理员角色访问。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SysUserMapper sysUserMapper;

    /**
     * 分页查询用户列表，支持按用户名或真实姓名模糊搜索。
     *
     * @param page    当前页码，默认 1
     * @param size    每页条数，默认 10
     * @param keyword 搜索关键字（可选）
     * @return 分页用户列表
     */
    @GetMapping("/users")
    public Result<IPage<SysUser>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        log.info("查询用户列表: page={}, size={}, keyword={}", page, size, keyword);
        Page<SysUser> pageParam = new Page<>(page, size);
        IPage<SysUser> result = sysUserMapper.selectPage(pageParam, keyword);
        return Result.success(result);
    }

    /**
     * 创建新用户。
     * <p>
     * 校验用户名唯一性，使用 BCrypt(强度12) 加密密码后保存。
     * 角色默认为 "user"，状态默认 1（启用）。
     * </p>
     *
     * @param dto 创建用户请求
     * @return 操作结果
     */
    @PostMapping("/users")
    public Result<Void> createUser(@Valid @RequestBody CreateUserDTO dto) {
        log.info("创建用户: username={}", dto.getUsername());

        // 检查用户名唯一性
        SysUser existingUser = sysUserMapper.selectByUsername(dto.getUsername());
        if (existingUser != null) {
            throw new BusinessException("用户名已存在: " + dto.getUsername());
        }

        // 构建新用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(new BCryptPasswordEncoder(12).encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(StringUtils.hasText(dto.getRole()) ? dto.getRole() : "user");
        user.setStatus(1);

        sysUserMapper.insert(user);
        log.info("用户创建成功: id={}, username={}", user.getId(), user.getUsername());
        return Result.success();
    }

    /**
     * 更新用户信息。
     * <p>
     * 仅更新传入的非空字段。
     * </p>
     *
     * @param id  用户 ID
     * @param dto 更新用户请求
     * @return 操作结果
     */
    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO dto) {
        log.info("更新用户: id={}", id);

        // 查询用户是否存在
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在: id=" + id);
        }

        // 仅更新非空字段
        if (StringUtils.hasText(dto.getRealName())) {
            user.setRealName(dto.getRealName());
        }
        if (StringUtils.hasText(dto.getEmail())) {
            user.setEmail(dto.getEmail());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            user.setPhone(dto.getPhone());
        }
        if (StringUtils.hasText(dto.getRole())) {
            user.setRole(dto.getRole());
        }

        sysUserMapper.updateById(user);
        log.info("用户更新成功: id={}", id);
        return Result.success();
    }

    /**
     * 更新用户启用/禁用状态。
     * <p>
     * 状态值由请求体中的 {@code status} 字段传入，
     * 经由 Bean Validation 校验合法性（仅允许 0 或 1）。
     * </p>
     *
     * @param id  用户 ID
     * @param dto 状态更新请求，包含 status 字段
     * @return 操作结果
     */
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                         @Valid @RequestBody UpdateUserStatusDTO dto) {
        log.info("更新用户状态: id={}, status={}", id, dto.getStatus());

        // 查询用户是否存在
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在: id=" + id);
        }

        user.setStatus(dto.getStatus());
        sysUserMapper.updateById(user);
        log.info("用户状态更新成功: id={}, status={}", id, dto.getStatus());
        return Result.success();
    }
}
