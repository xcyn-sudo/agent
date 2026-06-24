# agent-qr-user — P1 任务清单

> 用户管理模块：SysUser 实体、SysUserMapper、AdminController（基础 CRUD）。

---

## 1. SysUser 实体（P1 基础字段）

- [ ] **1.1** 创建 `org.example.agent_qr.user.entity.SysUser` 类
  - 注解：`@Data`、`@TableName("sys_user")`
  - P1 字段：
    | 字段 | 类型 | 说明 |
    |------|------|------|
    | `id` | `Long` | `@TableId(type = IdType.AUTO)` |
    | `username` | `String` | 用户名 |
    | `password` | `String` | BCrypt 密文 |
    | `realName` | `String` | 真实姓名 |
    | `email` | `String` | 邮箱 |
    | `phone` | `String` | 手机号 |
    | `role` | `String` | 角色：admin / user |
    | `status` | `Integer` | 状态：1-启用，0-禁用 |
    | `createTime` | `LocalDateTime` | `@TableField(fill = FieldFill.INSERT)` |
    | `updateTime` | `LocalDateTime` | `@TableField(fill = FieldFill.INSERT_UPDATE)` |

---

## 2. SysUserMapper

- [ ] **2.1** 创建 `org.example.agent_qr.user.mapper.SysUserMapper` 接口
  - 注解 `@Mapper`，继承 `BaseMapper<SysUser>`
  - `SysUser selectByUsername(@Param("username") String username)` — 按用户名查用户
  - `IPage<SysUser> selectPage(Page<SysUser> page, @Param("keyword") String keyword)` — 分页关键词搜索
    - 使用动态 SQL：匹配 `username` 或 `real_name` 的 `LIKE` 查询
    - 按 `create_time DESC` 排序

---

## 3. 元对象处理器（自动填充时间）

- [ ] **3.1** 创建 `org.example.agent_qr.user.handler.MyMetaObjectHandler` 类
  - 实现 `MetaObjectHandler`
  - `insertFill()`：自动填充 `createTime`、`updateTime`
  - `updateFill()`：自动填充 `updateTime`

---

## 4. DTO

- [ ] **4.1** 创建 `org.example.agent_qr.user.dto.CreateUserDTO` 类
  - 字段：`username`、`password`、`realName`、`email`、`phone`、`role`
  - 校验：`@NotBlank`（username、password）

- [ ] **4.2** 创建 `org.example.agent_qr.user.dto.UpdateUserDTO` 类
  - 字段：`realName`、`email`、`phone`、`role`

---

## 5. AdminController

- [ ] **5.1** 创建 `org.example.agent_qr.user.controller.AdminController` 类
  - 注解 `@RestController`、`@RequestMapping("/api/admin")`
  - 注入 `SysUserMapper`
  - `GET /api/admin/users` → `Result<IPage<SysUser>>`（参数：`page`、`size`、`keyword`）
  - `POST /api/admin/users` → `Result<Void>`（`@Valid @RequestBody CreateUserDTO`）
  - `PUT /api/admin/users/{id}` → `Result<Void>`（`@PathVariable Long id`、`@Valid @RequestBody UpdateUserDTO`）
  - `PUT /api/admin/users/{id}/status` → `Result<Void>`（`@PathVariable Long id`、`@RequestParam Integer status`）

---

## 6. pom.xml 依赖

- [ ] **6.1** 在 `agent-qr-user/pom.xml` 中配置依赖
  - `agent-qr-common`（模块依赖）
  - `mybatis-plus-spring-boot3-starter`
  - `mysql-connector-j`（MySQL 驱动）
