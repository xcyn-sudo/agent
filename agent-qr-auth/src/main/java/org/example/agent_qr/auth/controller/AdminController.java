package org.example.agent_qr.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.auth.evaluator.AbacEvaluator;
import org.example.agent_qr.auth.principal.UserPrincipal;
import org.example.agent_qr.common.BusinessException;
import org.example.agent_qr.common.Result;
import org.example.agent_qr.user.dto.CreateUserDTO;
import org.example.agent_qr.user.dto.UpdateUserDTO;
import org.example.agent_qr.user.dto.UpdateUserStatusDTO;
import org.example.agent_qr.user.entity.SysUser;
import org.example.agent_qr.user.mapper.SysUserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * 管理员用户管理控制器（P2 ABAC 升级）。
 * <p>
 * 提供用户 CRUD 和状态管理接口，权限由 ABAC 属性（职级 + 密级）控制，
 * 不再依赖单一 ROLE_ADMIN 角色。
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
    private final AbacEvaluator abacEvaluator;

    /**
     * 从 SecurityContext 获取当前登录用户的 UserPrincipal。
     *
     * @return 当前用户主体
     * @throws BusinessException 如果无法获取用户信息
     */
    private UserPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        throw new BusinessException(401, "无法获取当前用户信息");
    }

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
     * 权限：需职级 >= 经理 且 密级 >= 机密。
     * 校验用户名唯一性，使用 BCrypt(强度12) 加密密码后保存。
     * 角色默认为 "user"，状态默认 1（启用）。
     * </p>
     *
     * @param dto 创建用户请求
     * @return 操作结果
     */
    @PostMapping("/users")
    public Result<Void> createUser(@Valid @RequestBody CreateUserDTO dto) {
        UserPrincipal principal = getCurrentPrincipal();

        // ABAC 权限检查
        if (!abacEvaluator.canCreateUser(principal)) {
            throw new BusinessException(403, "权限不足：创建用户需要职级>=经理且密级>=机密");
        }

        log.info("创建用户: username={}, 操作者={}", dto.getUsername(), principal.getUsername());

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

        // ★ P2 ABAC 字段写入
        if (StringUtils.hasText(dto.getDepartment())) {
            user.setDepartment(dto.getDepartment());
        }
        if (dto.getClearanceLevel() != null) {
            user.setClearanceLevel(dto.getClearanceLevel());
        }
        if (StringUtils.hasText(dto.getAllowedDomains())) {
            user.setAllowedDomains(dto.getAllowedDomains());
        }
        if (StringUtils.hasText(dto.getTitle())) {
            user.setTitle(dto.getTitle());
        }

        sysUserMapper.insert(user);
        log.info("用户创建成功: id={}, username={}", user.getId(), user.getUsername());
        return Result.success();
    }

    /**
     * 更新用户信息。
     * <p>
     * 权限：只能编辑职级和密级都低于自己的用户，或编辑自身（有字段限制）。
     * 仅更新传入的非空字段。
     * </p>
     *
     * @param id  用户 ID
     * @param dto 更新用户请求
     * @return 操作结果
     */
    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO dto) {
        UserPrincipal principal = getCurrentPrincipal();

        // 查询用户是否存在
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在: id=" + id);
        }

        // ABAC 权限检查
        if (!abacEvaluator.canModifyUser(principal, id, user.getTitle(), user.getClearanceLevel())) {
            throw new BusinessException(403, "权限不足：只能编辑职级和密级都低于自己的用户");
        }

        log.info("更新用户: id={}, 操作者={}", id, principal.getUsername());

        // ★ 自编辑限制：禁止提权、改域、改部门
        boolean isSelf = principal.getUserId().equals(id);
        if (isSelf) {
            if (StringUtils.hasText(dto.getTitle())) {
                throw new BusinessException(403, "不允许修改自己的职级");
            }
            if (dto.getClearanceLevel() != null) {
                throw new BusinessException(403, "不允许修改自己的密级");
            }
            if (StringUtils.hasText(dto.getRole())) {
                throw new BusinessException(403, "不允许修改自己的角色");
            }
            if (StringUtils.hasText(dto.getDepartment())) {
                throw new BusinessException(403, "不允许修改自己的所属部门");
            }
            if (StringUtils.hasText(dto.getAllowedDomains())) {
                throw new BusinessException(403, "不允许修改自己的允许访问域");
            }
        }

        // 仅更新非空字段（基础信息）
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

        // ★ P2 ABAC 字段更新
        if (StringUtils.hasText(dto.getDepartment())) {
            user.setDepartment(dto.getDepartment());
        }
        if (dto.getClearanceLevel() != null) {
            user.setClearanceLevel(dto.getClearanceLevel());
        }
        if (StringUtils.hasText(dto.getAllowedDomains())) {
            user.setAllowedDomains(dto.getAllowedDomains());
        }
        if (StringUtils.hasText(dto.getTitle())) {
            user.setTitle(dto.getTitle());
        }

        sysUserMapper.updateById(user);
        log.info("用户更新成功: id={}", id);
        return Result.success();
    }

    /**
     * 更新用户启用/禁用状态。
     * <p>
     * 权限：只能对职级和密级都低于自己的用户操作。
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
        UserPrincipal principal = getCurrentPrincipal();

        // 查询用户是否存在
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在: id=" + id);
        }

        // ABAC 权限检查（与编辑相同规则）
        if (!abacEvaluator.canModifyUser(principal, id, user.getTitle(), user.getClearanceLevel())) {
            throw new BusinessException(403, "权限不足：只能对职级和密级都低于自己的用户执行禁用/启用操作");
        }

        log.info("更新用户状态: id={}, status={}, 操作者={}", id, dto.getStatus(), principal.getUsername());

        user.setStatus(dto.getStatus());
        sysUserMapper.updateById(user);
        log.info("用户状态更新成功: id={}, status={}", id, dto.getStatus());
        return Result.success();
    }
}
