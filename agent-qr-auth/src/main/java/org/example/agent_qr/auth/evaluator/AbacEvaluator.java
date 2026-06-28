package org.example.agent_qr.auth.evaluator;

import lombok.extern.slf4j.Slf4j;
import org.example.agent_qr.auth.principal.UserPrincipal;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ABAC（基于属性的访问控制）评估器。
 * <p>
 * 提供基于用户属性的细粒度权限判断方法，供
 * {@code @PreAuthorize} 注解或 Service 层手动调用。
 * 所有拒绝分支均记录结构化 warn 日志用于审计。
 * </p>
 * <p>
 * ★ 不依赖 agent-qr-knowledge（避免循环依赖）。
 * 文档级检查方法接受 domain/sensitivityLevel 参数而非 documentId。
 * </p>
 *
 * @author agent-qr
 */
@Slf4j
@Component("abac")
public class AbacEvaluator {

    /**
     * 职级 → 数字级别映射，用于大小比较。
     */
    private static final Map<String, Integer> TITLE_ORDER = Map.of(
            "employee", 1,
            "manager", 2,
            "director", 3
    );

    /**
     * 将职级字符串转为数字级别，未知职级返回 0。
     */
    private int getTitleLevel(String title) {
        if (title == null) {
            return 0;
        }
        return TITLE_ORDER.getOrDefault(title, 0);
    }

    /**
     * 判断用户是否可以查询指定业务域。
     *
     * @param user   当前用户主体
     * @param domain 业务域
     * @return true 表示有权限
     */
    public boolean canQueryDomain(UserPrincipal user, String domain) {
        if (user.isAdmin()) {
            return true;
        }
        boolean hasAccess = user.hasDomainAccess(domain);
        if (!hasAccess) {
            log.warn("ABAC 拒绝 - canQueryDomain: userId={}, username={}, department={}, " +
                            "allowedDomains={}, requestedDomain={}",
                    user.getUserId(), user.getUsername(), user.getDepartment(),
                    user.getAllowedDomains(), domain);
        }
        return hasAccess;
    }

    /**
     * 判断用户是否可以访问具有指定属性的文档。
     * <p>
     * Admin 允许所有；普通用户需通过密级检查和域检查。
     * 调用方（knowledge 模块）负责从数据库查询文档的 domain 和 sensitivityLevel 后传入。
     * </p>
     *
     * @param user             当前用户主体
     * @param domain           文档所属业务域
     * @param sensitivityLevel 文档敏感级别
     * @return true 表示有权限
     */
    public boolean canAccessDocument(UserPrincipal user, String domain, Integer sensitivityLevel) {
        if (user.isAdmin()) {
            return true;
        }

        // 密级检查
        if (sensitivityLevel != null && !user.hasClearance(sensitivityLevel)) {
            log.warn("ABAC 拒绝 - canAccessDocument(密级不足): userId={}, username={}, " +
                            "userClearance={}, docSensitivity={}",
                    user.getUserId(), user.getUsername(), user.getClearanceLevel(),
                    sensitivityLevel);
            return false;
        }

        // 域检查
        if (domain != null && !user.hasDomainAccess(domain)) {
            log.warn("ABAC 拒绝 - canAccessDocument(域不匹配): userId={}, username={}, " +
                            "userDomains={}, docDomain={}",
                    user.getUserId(), user.getUsername(), user.getAllowedDomains(), domain);
            return false;
        }

        return true;
    }

    /**
     * 判断用户是否可以向指定域上传文档。
     * <p>
     * 需要 canQueryDomain 权限，且职级为 manager 或 director。
     * </p>
     *
     * @param user   当前用户主体
     * @param domain 目标业务域
     * @return true 表示有权限
     */
    public boolean canUploadToDomain(UserPrincipal user, String domain) {
        if (user.isAdmin()) {
            return true;
        }

        if (!canQueryDomain(user, domain)) {
            return false;
        }

        if (!"manager".equals(user.getTitle()) && !"director".equals(user.getTitle())) {
            log.warn("ABAC 拒绝 - canUploadToDomain(职级不足): userId={}, username={}, " +
                            "title={}, domain={}",
                    user.getUserId(), user.getUsername(), user.getTitle(), domain);
            return false;
        }

        return true;
    }

    /**
     * 判断用户是否可以删除具有指定属性的文档。
     * <p>
     * 需要 canAccessDocument 权限，且职级为 director。
     * </p>
     *
     * @param user             当前用户主体
     * @param domain           文档所属业务域
     * @param sensitivityLevel 文档敏感级别
     * @return true 表示有权限
     */
    public boolean canDeleteDocument(UserPrincipal user, String domain, Integer sensitivityLevel) {
        if (user.isAdmin()) {
            return true;
        }

        if (!canAccessDocument(user, domain, sensitivityLevel)) {
            return false;
        }

        if (!"director".equals(user.getTitle())) {
            log.warn("ABAC 拒绝 - canDeleteDocument(职级不足): userId={}, username={}, " +
                            "title={}, domain={}",
                    user.getUserId(), user.getUsername(), user.getTitle(), domain);
            return false;
        }

        return true;
    }

    /**
     * 判断用户是否可以修改指定用户的信息。
     * <p>
     * 规则（admin 不豁免）：
     * - 编辑自身：允许（字段级限制由 Controller 处理）
     * - 编辑他人：当前用户的职级和密级必须<b>都严格高于</b>目标用户
     * </p>
     *
     * @param user                  当前用户主体
     * @param targetUserId          目标用户 ID
     * @param targetTitle           目标用户职级
     * @param targetClearanceLevel  目标用户密级
     * @return true 表示有权限
     */
    public boolean canModifyUser(UserPrincipal user, Long targetUserId,
                                 String targetTitle, Integer targetClearanceLevel) {
        // 自身 → 允许
        if (user.getUserId().equals(targetUserId)) {
            return true;
        }

        int userTitleLevel = getTitleLevel(user.getTitle());
        int targetTitleLevel = getTitleLevel(targetTitle);
        int userClearance = user.getClearanceLevel() != null ? user.getClearanceLevel() : 0;
        int targetClearance = targetClearanceLevel != null ? targetClearanceLevel : 0;

        boolean titleOk = userTitleLevel > targetTitleLevel;
        boolean clearanceOk = userClearance > targetClearance;

        if (!titleOk || !clearanceOk) {
            log.warn("ABAC 拒绝 - canModifyUser(职级/密级不足): userId={}, username={}, " +
                            "userTitle={}({}), targetTitle={}({}), " +
                            "userClearance={}, targetClearance={}",
                    user.getUserId(), user.getUsername(),
                    user.getTitle(), userTitleLevel, targetTitle, targetTitleLevel,
                    userClearance, targetClearance);
            return false;
        }

        return true;
    }

    /**
     * 判断用户是否可以创建新用户。
     * <p>
     * 规则（admin 不豁免）：职级 >= 经理(manager) 且 密级 >= 机密(2)。
     * </p>
     *
     * @param user 当前用户主体
     * @return true 表示有权限
     */
    public boolean canCreateUser(UserPrincipal user) {
        int titleLevel = getTitleLevel(user.getTitle());
        int clearanceLevel = user.getClearanceLevel() != null ? user.getClearanceLevel() : 0;

        boolean titleOk = titleLevel >= 2; // manager 及以上
        boolean clearanceOk = clearanceLevel >= 2; // 机密及以上

        if (!titleOk || !clearanceOk) {
            log.warn("ABAC 拒绝 - canCreateUser(条件不足): userId={}, username={}, " +
                            "title={}({}), clearanceLevel={}",
                    user.getUserId(), user.getUsername(),
                    user.getTitle(), titleLevel, clearanceLevel);
            return false;
        }

        return true;
    }

    /**
     * 判断用户是否可以查看数据仪表盘。
     * <p>
     * 规则（admin 不豁免）：职级 == 总监(director) 且 密级 == 绝密(3)。
     * </p>
     *
     * @param user 当前用户主体
     * @return true 表示有权限
     */
    public boolean canViewDashboard(UserPrincipal user) {
        boolean titleOk = "director".equals(user.getTitle());
        boolean clearanceOk = user.getClearanceLevel() != null && user.getClearanceLevel() == 3;

        if (!titleOk || !clearanceOk) {
            log.warn("ABAC 拒绝 - canViewDashboard(条件不足): userId={}, username={}, " +
                            "title={}, clearanceLevel={}",
                    user.getUserId(), user.getUsername(),
                    user.getTitle(), user.getClearanceLevel());
            return false;
        }

        return true;
    }

    /**
     * 判断用户是否可以管理数据源。
     * <p>
     * 仅 admin 角色有此权限。
     * </p>
     *
     * @param user 当前用户主体
     * @return true 表示有权限
     */
    public boolean canManageDatasource(UserPrincipal user) {
        if (!user.isAdmin()) {
            log.warn("ABAC 拒绝 - canManageDatasource: userId={}, username={}, role={}",
                    user.getUserId(), user.getUsername(), user.getRole());
        }
        return user.isAdmin();
    }
}
