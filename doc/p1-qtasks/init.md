# P1 前端 — init 模块任务清单

> 模块：项目初始化
>
> 依赖：无
>
> 开发顺序：第 1 步（最先完成，所有模块依赖）

---

## 子任务

- [ ] **1.1 脚手架搭建**
  - 使用 `npm create vite@latest` 创建 Vue3 + TypeScript 项目
  - 项目名称：`agent-qr-web-frontend`
  - 清理默认模板文件

- [ ] **1.2 安装核心依赖**
  - `vue-router`（路由）
  - `pinia`（状态管理）
  - `axios`（HTTP 请求）
  - `element-plus`（UI 组件库）
  - `echarts` + `vue-echarts`（图表）
  - `sass`（SCSS 预处理器）

- [ ] **1.3 安装开发依赖**
  - `@types/node`
  - `unplugin-auto-import`（Element Plus 按需导入）
  - `unplugin-vue-components`（Element Plus 组件按需导入）

- [ ] **1.4 配置 Vite**
  - 配置 `@` 路径别名 → `src/`
  - 配置开发服务器代理：`/api` → `http://localhost:9090`
  - 配置 Element Plus 自动导入插件

- [ ] **1.5 配置 TypeScript**
  - `tsconfig.json`：路径别名、严格模式
  - 创建 `src/types/index.ts`（全局类型定义）

- [ ] **1.6 创建目录结构**
  - `src/api/`、`src/router/`、`src/stores/`
  - `src/views/`、`src/components/`
  - `src/utils/`、`src/styles/`

- [ ] **1.7 创建入口文件**
  - `src/main.ts`：挂载 Vue 应用、注册 Pinia/Router/Element Plus
  - `src/App.vue`：`<router-view />` 根组件
  - `index.html`：设置页面标题和语言

---

## 验证标准

- [ ] `npm run dev` 可正常启动开发服务器
- [ ] 浏览器访问 `http://localhost:5173` 显示空白页面（无报错）

---

> 预计耗时：0.5 天
