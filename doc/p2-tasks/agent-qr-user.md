# agent-qr-user — P2 任务清单

> 用户管理模块 P2 扩展：SysUser 实体 ABAC 属性字段扩展、数据库表结构变更。

---

## 1. SysUser 实体 ABAC 字段扩展

- [ ] **1.1** 在 `org.example.agent_qr.user.entity.SysUser` 中新增 ABAC 属性字段
  - `private String department` — 所属部门（HR / FINANCE / RD / SALES / COMMON）
  - `private Integer clearanceLevel` — 数据密级（0=公开 1=内部 2=机密 3=绝密）
  - `private String allowedDomains` — 允许访问的业务域（逗号分隔字符串）
  - `private String title` — 职级（employee / manager / director）
  - 以上字段添加对应的 `@TableField` 注解

---

## 2. CreateUserDTO / UpdateUserDTO P2 扩展

- [ ] **2.1** 在 `CreateUserDTO` 中新增字段
  - `department`、`clearanceLevel`、`allowedDomains`、`title`

- [ ] **2.2** 在 `UpdateUserDTO` 中新增字段
  - `department`、`clearanceLevel`、`allowedDomains`、`title`

---

## 3. DDL — sys_user 表 ABAC 字段扩展

- [ ] **3.1** 编写 ALTER TABLE SQL（追加到 p2-schema.sql）
  - `ALTER TABLE sys_user ADD COLUMN department VARCHAR(32) DEFAULT 'COMMON' COMMENT '所属部门'`
  - `ALTER TABLE sys_user ADD COLUMN clearance_level INT DEFAULT 0 COMMENT '数据密级 0=公开 1=内部 2=机密 3=绝密'`
  - `ALTER TABLE sys_user ADD COLUMN allowed_domains VARCHAR(255) DEFAULT '' COMMENT '允许访问的业务域（逗号分隔）'`
  - `ALTER TABLE sys_user ADD COLUMN title VARCHAR(16) DEFAULT 'employee' COMMENT '职级 employee/manager/director'`

---

## 4. pom.xml 依赖

- [ ] **4.1** 确认 `agent-qr-user/pom.xml` 无需新增依赖（ABAC 字段仅需 MySQL 已引入）
