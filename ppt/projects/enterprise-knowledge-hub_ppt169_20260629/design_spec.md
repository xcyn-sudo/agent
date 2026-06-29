# enterprise-knowledge-hub — Design Spec

> Human-readable design narrative. Machine-readable execution contract: `spec_lock.md`.

## I. Project Information

| Item | Value |
| ---- | ----- |
| **Project Name** | enterprise-knowledge-hub |
| **Canvas Format** | PPT 16:9 (1280×720) |
| **Page Count** | 21 |
| **Design Style** | blueprint — 工程蓝图美学，暗纸白线，技术注解风格 |
| **Target Audience** | 中高层管理者、客户决策者 |
| **Use Case** | 面向决策者的技术产品汇报，20-25分钟演讲+会后阅读 |
| **Delivery Purpose** | `balanced` — 商业演示兼阅读，适中的信息密度 |
| **Content Strategy** | 平衡（紧密跟随源文档大纲，保留21页结构与叙事逻辑） |
| **Created Date** | 2026-06-29 |

---

## II. Canvas Specification

| Property | Value |
| -------- | ----- |
| **Format** | PPT 16:9 |
| **Dimensions** | 1280×720 |
| **viewBox** | `0 0 1280 720` |
| **Margins** | 左右 60px，上下 50px |
| **Content Area** | 1160×620 (标题区 1160×100, 内容区 1160×480, 页脚区 1160×40) |

---

## III. Visual Theme

### Theme Style

- **Mode**: `narrative` — 故事线驱动（政策东风→企业痛点→解决方案→技术硬实力→落地效果→未来展望），含冲突-解决的叙事弧线
- **Visual style**: `blueprint` — 工程蓝图美学：暗纸底色，细单线框，等距投影，技术标注语言，网格底纹
- **Theme**: Dark theme（全暗色底，仅内容密集型文字页可选浅色面板）
- **Tone**: 技术权威、精确克制、工程专业感

### Color Scheme

| Role | HEX | Purpose |
| ---- | --- | ------- |
| **Background** | `#0D1117` | 页面底色（蓝图暗纸） |
| **Secondary bg** | `#1A1F36` | 卡片底色、次级区块 |
| **Primary** | `#1A73E8` | 结构线框主色（蓝图蓝线）、标题装饰、图标 |
| **Accent** | `#00C853` | 关键状态高亮、正向指标、聚焦点 |
| **Secondary accent** | `#7C4DFF` | 次级标注、渐变过渡、图表辅色 |
| **Body text** | `#E8ECF0` | 正文（暗底亮字） |
| **Secondary text** | `#9CA3AF` | 说明文字、标注 |
| **Tertiary text** | `#6B7280` | 补充信息、页脚 |
| **Border/divider** | `#2A3040` | 卡片边框、分割线 |
| **Success** | `#00C853` | 正向指标（绿色家族） |
| **Warning** | `#FF1744` | 阻断/拒绝/下降（红色家族） |
| **Surface** | `#151B28` | 面板浮层 |
| **Grid** | `#1E2640` | 蓝图网格线 |

### AI Image Strategy

- **Image Rendering**: `blueprint`
- **Image Palette**: `cool-corporate`

### Gradient Scheme

```xml
<!-- 蓝图网格底纹 -->
<pattern id="blueprintGrid" width="40" height="40" patternUnits="userSpaceOnUse">
  <rect width="40" height="40" fill="none"/>
  <line x1="0" y1="40" x2="40" y2="40" stroke="#1E2640" stroke-width="0.5"/>
  <line x1="40" y1="0" x2="40" y2="40" stroke="#1E2640" stroke-width="0.5"/>
</pattern>

<!-- 底部渐变遮罩（蓝图纸边缘暗角） -->
<linearGradient id="vignetteBottom" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0%" stop-color="#0D1117" stop-opacity="0"/>
  <stop offset="85%" stop-color="#0D1117" stop-opacity="0.4"/>
  <stop offset="100%" stop-color="#0D1117" stop-opacity="0.8"/>
</linearGradient>

<!-- 标题强调渐变 -->
<linearGradient id="titleAccent" x1="0" y1="0" x2="1" y2="0">
  <stop offset="0%" stop-color="#1A73E8"/>
  <stop offset="100%" stop-color="#7C4DFF"/>
</linearGradient>
```

---

## IV. Typography System

### Font Plan

**Typography direction**: 经典工程字 — clean sans + monospace，标题 SimHei 粗壮有力，正文微软雅黑清晰，代码 Consolas 等宽

| Role | Chinese | English | Fallback tail |
| ---- | ------- | ------- | ------------- |
| **Title** | `SimHei` | `Arial` | `"Microsoft YaHei", sans-serif` |
| **Body** | `"Microsoft YaHei", "PingFang SC"` | `Arial` | `sans-serif` |
| **Emphasis** | same as Body | `Arial` | `sans-serif` |
| **Code** | — | `Consolas, "Courier New"` | `monospace` |

**Per-role font stacks**:

- Title: `SimHei, Arial, "Microsoft YaHei", sans-serif`
- Body: `"Microsoft YaHei", "PingFang SC", Arial, sans-serif`
- Emphasis: same as Body
- Code: `Consolas, "Courier New", monospace`

### Font Size Hierarchy

**Baseline (unitless px)**: Body = **24px**

| Role | Ratio | Size (px) | Weight |
| ---- | ----- | --------- | ------ |
| Cover title | 3x | 72 | Bold |
| Chapter/section opener | 2.1x | 50 | Bold |
| Page title | 1.75x | 42 | Bold |
| Hero number | 2x | 48 | Bold |
| Subtitle | 1.33x | 32 | SemiBold |
| Lead / core message | 1.25x | 30 | Regular |
| Subheading | 1.17x | 28 | SemiBold |
| **Body** | **1x** | **24** | Regular |
| Annotation / caption | 0.75x | 18 | Regular |
| Page number / footnote | 0.67x | 16 | Regular |
| Chart annotation | 0.67x | 16 | Regular |

---

## V. Layout Principles

### Page Structure

- **Header area**: 标题区 y=50-130, 高度 80px, 含页面标题 + 可选副标题/导语
- **Content area**: y=140-670, 高度 530px, 自由布局
- **Footer area**: y=680-720, 高度 40px, 含页码 + 可选来源标注

### Layout Pattern Library

| Pattern | Suitable Scenarios |
| ------- | ----------------- |
| **Single column centered** | P01 封面, P05 核心命题, P21 结尾 |
| **Timeline horizontal** | P02 政策时间轴 |
| **Three-column cards** | P03 三层面卡片, P08 ROI三阶段, P18 用户场景 |
| **Numbered vertical list** | P04 五大痛点 |
| **Asymmetric split** | P06 产品定位（左定义+右指标）, P10 技术选型（左表格+右决策）, P15 问答对比 |
| **Layered horizontal bands** | P09 五层架构 |
| **2×3 icon grid** | P11 六宫格亮点 |
| **Pipeline flow** | P12 检索流程, P13 数据入库管道 |
| **Center-radiating** | P14 核心功能全景 |
| **Top-bottom split** | P16 知识管理（上：生命周期+下：多源接入） |
| **Scenario cards + flow** | P17 权限模型 |
| **Three-phase horizontal** | P19 分期路线图 |
| **Data table** | P20 动效汇总表 |

### Spacing Specification

**Universal**:

| Element | Value |
| ------- | ----- |
| Safe margin from canvas edge | 60px |
| Content block gap | 32px |
| Icon-text gap | 12px |

**Card-based layouts**:

| Element | Value |
| ------- | ----- |
| Card gap | 24px |
| Card padding | 28px |
| Card border radius | 4px (blueprint style: near-sharp, slight rounding) |
| Single-row card height | 530px |
| Three-column card width | 360px |

---

## VI. Icon Usage Specification

### Source

- **Built-in icon library**: `tabler-outline` (stroke line art, `stroke-width: 2`)
- **Usage method**: `<use data-icon="tabler-outline/icon-name" width="32" height="32" .../>`

### Recommended Icon List

| Purpose | Icon Path | Page |
| ------- | --------- | ---- |
| 搜索/查找 | `tabler-outline/search` | P04, P06 |
| 数据库 | `tabler-outline/database` | P09, P13 |
| 盾牌/安全 | `tabler-outline/shield-check` | P11, P17 |
| 网络/节点 | `tabler-outline/network` | P09, P12 |
| 用户/人物 | `tabler-outline/users` | P18 |
| 文件/文档 | `tabler-outline/file-text` | P16 |
| 时钟/时间 | `tabler-outline/clock` | P02, P19 |
| 图表 | `tabler-outline/chart-bar` | P07, P08 |
| 设置/齿轮 | `tabler-outline/settings` | P09, P10 |
| 眼睛/可见 | `tabler-outline/eye` | P04, P17 |
| 链接/连接 | `tabler-outline/link` | P13, P16 |
| 上传 | `tabler-outline/upload` | P13, P16 |
| 消息/对话 | `tabler-outline/message-circle` | P15 |
| 对勾 | `tabler-outline/check` | P19 |
| 箭头右 | `tabler-outline/arrow-right` | P02, P19 |
| 趋势向上 | `tabler-outline/trending-up` | P08 |
| 下载 | `tabler-outline/download` | P13 |
| 星形 | `tabler-outline/star` | P01, P21 |
| 代码 | `tabler-outline/code` | P10 |
| 锁 | `tabler-outline/lock` | P17 |
| 路线图 | `tabler-outline/road` | P19 |

---

## VII. Visualization Reference List

Catalog read: 71 templates

| Page | Template | Path | Summary-quote (verbatim) | Usage |
| ---- | -------- | ---- | ------------------------ | ----- |
| P02 | timeline | `templates/charts/timeline.svg` | "Pick for 3-8 milestone events on a horizontal time axis (no duration). Skip for tasks with start/end ranges (use gantt_chart) or vertical layout (use roadmap_vertical)." | 四阶段政策时间轴，从左到右展示2017-2026政策跃迁 |
| P03 | vertical_pillars | `templates/charts/vertical_pillars.svg` | "Pick for 1×3 / 1×4 / 1×5 vertical column layout where each pillar = one independent category with title + bullets — PEST (Political/Economic/Social/Technological), four-pillar strategy overview, side-by-side independent categories." | 战略/产业/治理三支柱卡片 |
| P04 | vertical_list | `templates/charts/vertical_list.svg` | "Pick for 3-6 numbered key points each with a short description — design principles, core tenets, action items, key takeaways, recommendations, executive summary points." | 五大痛点编号列表，每项含图标+数据 |
| P06 | dumbbell_chart | `templates/charts/dumbbell_chart.svg` | "Pick for before-vs-after or two-state difference across 5-10 items. Skip for single snapshot (use bar_chart) or 3+ states (use grouped_bar_chart)." | 改善前后四项关键指标对比 |
| P08 | kpi_cards | `templates/charts/kpi_cards.svg` | "Pick for 4-8 standalone numeric metrics shown as overview cards (2x2 or 1x4) — exec summary opener, dashboard headline, quarterly recap, results-at-a-glance." | 三阶段ROI指标卡片（投入/收益/覆盖率）+回本时间线 |
| P09 | layered_architecture | `templates/charts/layered_architecture.svg` | "Pick for 3-4 horizontal architecture layers (presentation/service/data), 2-4 module cards per layer, each card = title + 1-line description (description required, even if source brief)." | 五层技术架构（前端→API网关→业务服务→AI引擎→数据存储） |
| P11 | icon_grid | `templates/charts/icon_grid.svg` | "Pick for 4-9 parallel features/capabilities/services as icon cards — feature grid, service lineup, benefits matrix, brand values, product highlights." | 六大系统亮点六宫格 |
| P12 | pipeline_with_stages | `templates/charts/pipeline_with_stages.svg` | "Pick for 3-5 horizontal pipeline stages, each = title + 1-line description + output artifact, connected by arrows (data pipelines, ETL, build pipelines)." | 问答检索全链路（意图解析→双路召回→RRF融合→Rerank→LLM生成） |
| P13 | pipeline_with_stages | `templates/charts/pipeline_with_stages.svg` | (same as above) | 数据入库八步管道（接入网关→质量检查→ETL→目录索引→切片向量化→三存储） |
| P14 | hub_spoke | `templates/charts/hub_spoke.svg` | "Pick for 1 core capability + 4-8 surrounding capabilities (platform/ecosystem); each spoke = title or title + 1-2 line description." | 核心功能全景：中心=企业知识中枢，8个功能模块环绕 |
| P19 | numbered_steps | `templates/charts/numbered_steps.svg` | "Pick for 3-6 horizontal sequential steps with numeric emphasis — how-it-works section, getting-started guide, methodology overview, implementation phases." | P1/P2/P3三阶段落地路线图 |
| P20 | basic_table | `templates/charts/basic_table.svg` | "Pick for plain tabular text/number grid, 3-8 columns. Skip if cells need visual bars (use consulting_table) or qualitative scores (use harvey_balls_table)." | 十种动效类型对照表 |

**Runners-up considered** (3 entries minimum):

- `comparison_table` | rejected for P06: 仅4项指标对比，dumbbell_chart 的 before/after 哑铃形态更有冲击力
- `process_flow` | rejected for P12: pipeline_with_stages 的"阶段+产出物"结构更匹配 RRF→Rerank→LLM 的数据加工语义
- `roadmap_vertical` | rejected for P19: 三阶段横向展开更贴合源文档的横向时间线表达，numbered_steps 的编号强调适合阶段标识

---

## VIII. Image Resource List

| Filename | Dimensions | Ratio | Purpose | Type | Layout pattern | Acquire Via | Status | Reference | text_policy | page_role |
| -------- | --------- | ----- | ------- | ---- | -------------- | ----------- | ------ | --------- | ----------- | --------- |
| cover_bg.png | 1280×720 | 1.78 | 封面全幅蓝图背景——中心留白区域用于标题叠加 | Background | #1 full-bleed background with floating title + #29 two-stop scrim | ai | Pending | Dark blueprint paper with luminous circuit-board traces radiating from center; calm central void reserved for title overlay; subtle isometric grid backdrop; geometric node markers at periphery | none | hero_page |
| arch_isometric.png | 1280×720 | 1.78 | 五层技术架构等距示意图 | Diagram | #44 background image + native network/architecture diagram | ai | Pending | Isometric 3D schematic of a five-tier technology stack — frontend layer (browser windows) → API gateway (traffic routers) → business services (connected modules) → AI engine (neural network nodes) → data storage (database cylinders); each layer as a distinct horizontal plane with vertical connector lines; blueprint line-art with blue structural lines and green highlight on the AI engine layer | none | local |
| pipeline_schematic.png | 1280×720 | 1.78 | 数据入库管道工程示意图 | Diagram | #44 background image + native network/architecture diagram | ai | Pending | Horizontal eight-stage data pipeline schematic — data sources (ERP/CRM/OA/file servers) on the far left feeding into sequential processing stages drawn as connected machine-boxes: ingestion gateway → quality gate → ETL transformer → catalog indexer → chunking+embedding → triple storage (MySQL/ChromaDB/Lucene); green pass / red block indicators at the quality gate; isometric connector pipes between stages | none | local |

> Image-as-canvas coverage: P09 and P13 both use #44 (background image + native architecture/pipeline diagram), satisfying the ≥1 image-as-canvas page requirement. The blueprint schematic images provide the visual anchor and atmosphere, while native SVG elements carry the editable labels and data.

---

## IX. Content Outline

### Part 1: 开篇与政策背景 (P01–P05)

#### Slide 01 — 封面

- **Cover impact**: 核心主张"让企业知识触手可及"通过工程蓝图美学呈现——暗纸网格底纹上，发光电路线从中心向外辐射，标题叠加在留白中心区，暗示"知识中枢=企业信息基础设施"的隐喻
- **Layout**: #1 full-bleed background with floating title + #29 two-stop scrim — AI生成蓝图风格封面底图全幅铺开，中心区域用渐变遮罩留白，标题/SVG文字层叠其上
- **Title**: 企业知识中枢 — 基于AI Agent的智能知识管理平台
- **Subtitle**: 让每个员工像问资深同事一样，秒级获取企业知识
- **Info**: 汇报人 / 部门 / 2026.06
- **Visualization**: —

#### Slide 02 — 中国AI Agent政策十年跃迁

- **Layout**: timeline horizontal — 横向时间轴从左到右排列四个阶段
- **Title**: 时代背景：中国AI Agent政策十年跃迁
- **Core message**: 从"发展规划"到"智能体立法"，中国用10年构建了全球最系统的AI治理与产业促进体系
- **Visualization**: timeline
- **Content**:
  - 🔵 战略布局 (2017-2022): AI上升为国家战略 · 《新一代人工智能发展规划》三步走
  - 🟢 应用探索 (2023-2024): "人工智能+"元年 · 政府工作报告首提"人工智能+"；大模型备案制度
  - 🟡 爆发规范 (2025): AI Agent元年 · 国发〔2025〕11号：十年规划，2027年智能体普及率超70%
  - 🔴 规模部署 (2026): 智能体写入政府工作报告 · 三部门《智能体规范应用与创新发展实施意见》：19大场景

#### Slide 03 — 政策东风下的企业机遇

- **Layout**: vertical_pillars — 三列纵向卡片
- **Title**: 政策东风下的企业机遇
- **Core message**: 企业构建AI Agent知识平台，既是顺应国策、也是解决真实痛点的必然选择
- **Visualization**: vertical_pillars
- **Content**:
  - 战略层面：国发〔2025〕11号 · 2027普及率>70% · 2035全面智能经济
  - 产业层面："模数共振"行动 · 共建智能体工厂 · 19大应用场景落地
  - 治理层面：智能体分类分级 · 敏感领域严格备案 · 可控/可审计/可追责

#### Slide 04 — 企业知识管理的现实困境

- **Layout**: vertical_list — 五大痛点编号列表，每项含图标+关键数据
- **Title**: 企业知识管理的现实困境
- **Core message**: 知识散落、数据孤岛、经验流失、安全裸奔、重复问答——五个问题每年吞噬数千工时
- **Visualization**: vertical_list
- **Content**:
  - ① 🔍 知识散落：找一份制度文件要23分钟 → 200人公司年损失56,000工时
  - ② 🏝️ 数据孤岛：查一个信息要切4个系统 → 无统一入口
  - ③ 🧠 经验流失：核心员工离职=知识归零 → 故障处理30min→4小时
  - ④ 🔓 安全裸奔：薪资文件谁都能搜到 → 无细粒度权限
  - ⑤ 🔄 重复问答：HR总监第47次回答"年假怎么算" → 70%问题是重复的

#### Slide 05 — 核心命题

- **Layout**: Single column centered — 居中大字，纯排版页面
- **Title**: 核心命题
- **Core message**: 如何让企业知识，从"人脑记忆+散落文件"，变成"一个框、10秒钟、精准答案"？
- **Visualization**: —
- **Content**:
  - 居中大字体展示核心问题
  - 下方小字：这就是企业知识中枢要解决的问题
  - 底部渐变光条从左扫到右（SVG动画暗示）

---

### Part 2: 解决方案与降本增效 (P06–P08)

#### Slide 06 — 产品定位与核心指标

- **Layout**: Asymmetric split (4:6) — 左：一句话定位 + 右：改善前后指标对比
- **Title**: 产品定位：企业知识中枢
- **Core message**: 不是替代人，是让每个人不再把时间浪费在"找东西"上
- **Visualization**: dumbbell_chart
- **Content**:
  - **左侧一句话定位**：🔦 让每个员工都能像问资深同事一样，秒级获取企业知识
  - **右侧改善前后对比**：单次查找耗时 23min→10s (99.3%↓) · 跨部门信息获取 4h→30s (99.8%↓) · 重复问题人工回答 100%→20% (80%↓) · 新人上手周期 2天→0.5天 (75%↓)

#### Slide 07 — 降本增效：单次问答成本解剖

- **Layout**: Asymmetric split — 左：传统模式成本分解 + 右：AI知识中枢成本
- **Title**: 降本增效：单次问答成本解剖
- **Core message**: 每次问答节省63.3元 · 年化节约49万元（200人企业）
- **Visualization**: —
- **Content**:
  - **传统模式（加权平均）**：60% 自己翻文件 30.7元/次 · 30% 钉钉问人 50.8元/次 · 10% 找不到→开会 300元/次 → 综合：63.6元/次
  - **AI知识中枢**：0.27元/次 · 10秒出答案 · 附来源引用
  - **底部大字**：99.6%↓ 成本降低

#### Slide 08 — 降本增效：ROI投资回报全景

- **Layout**: kpi_cards (1×3) + timeline
- **Title**: 降本增效：ROI投资回报全景
- **Core message**: 第10个月回本 · 3年ROI=221% · 5年ROI=733%
- **Visualization**: kpi_cards
- **Content**:
  - **第一阶段MVP（第1-3月）**：投入33.5万 · 年化收益27.2万 · 2域/50人
  - **第二阶段增强（第4-5月）**：投入+13万 · 年化收益60.5万 · 4域/200人
  - **第三阶段全面（第6-8月）**：投入+15万 · 年化收益≥115万 · 全员
  - **底部回本时间线**：累计投入56万 → 三年累计收益≥180万

---

### Part 3: 技术栈与亮点展示 (P09–P13)

#### Slide 09 — 技术架构总览

- **Layout**: layered_architecture — 五层水平架构
- **Title**: 技术架构总览
- **Core message**: 从前端到数据存储，五层全栈架构支撑企业级AI知识中枢
- **Visualization**: layered_architecture
- **Content**:
  - **前端展示层**：Vue3 · TypeScript · Pinia · SCSS · Vite · Axios · SSE流式接收
  - **API网关层**：Spring Boot 3 · REST API · SSE Streaming · JWT认证
  - **业务服务层**：智能问答 | 知识管理 | 权限管控 | 数据接入 · 统计仪表 | 知识目录 | 质量管控 | 反馈闭环（事件驱动+CQRS）
  - **AI引擎层**：LangChain · Ollama(qwen3) · DeepSeek · 混合检索(RRF) · Rerank · Embedding
  - **数据存储层**：MySQL 8.0 | ChromaDB | Lucene/BM25

#### Slide 10 — 技术栈选型详解

- **Layout**: Asymmetric split (5:5) — 左：选型表格 + 右：关键设计决策
- **Title**: 技术栈选型详解
- **Core message**: 全链路内网部署、Provider可切换、事件驱动异步——每一个选型都有明确的架构理由
- **Visualization**: —
- **Content**:
  - **核心技术选型**：前端 Vue3+Vite · 后端 Spring Boot 3 · LLM Ollama(本地)+DeepSeek(备用) · 向量库 ChromaDB · 检索 语义+BM25混合 · 权限 ABAC五维度
  - **关键设计决策**：🏠 数据不出企业（全链路内网）· 🔌 Provider可切换（策略模式）· ⚡ 全链路异步（事件驱动）· 🔄 断点续传（DLQ+3次退避重试）

#### Slide 11 — 系统亮点六宫格

- **Layout**: icon_grid (2×3)
- **Title**: 系统亮点六宫格
- **Core message**: 混合检索、ABAC权限、全链路溯源、多源接入、数据安全、事件驱动——六大核心亮点
- **Visualization**: icon_grid
- **Content**:
  - 🎯 混合检索：语义+关键词双路召回 · RRF融合+重排
  - 🛡️ ABAC权限：5维度细粒度 · 属性级访问控制 · 等保2.0合规
  - 🔍 全链路溯源：每个答案都可追溯到原文 · 审计日志完整
  - 📡 多源接入：JDBC/REST/S3统一接入网关 · ETL标准化
  - 🏠 数据安全：全链路内网 · LLM本地推理 · 可完全离线
  - ⚡ 事件驱动：异步处理管道 · DLQ断点续传 · 质量实时监控

#### Slide 12 — 亮点深挖：混合检索+Rerank

- **Layout**: pipeline_with_stages — 五阶段检索流程图
- **Title**: 亮点深挖：混合检索+Rerank
- **Core message**: 语义+关键词双路召回→RRF融合→Rerank精排——单一语义检索的短板被结构化过滤补齐
- **Visualization**: pipeline_with_stages
- **Content**:
  - 用户问题 → 意图解析+域路由
  - 语义检索通道（ChromaDB, Top-20） ⟷ 关键词检索通道（Lucene BM25, Top-20）
  - RRF加权融合去重（40→10条）
  - Rerank交叉精排（10→5条）
  - LLM生成答案（流式逐字输出）

#### Slide 13 — 亮点深挖：数据全链路入库管道

- **Layout**: pipeline_with_stages (8 stages) + #44 background image + native pipeline diagram
- **Title**: 亮点深挖：数据全链路入库管道
- **Core message**: 从ERP/CRM/OA/文件服务器到MySQL+ChromaDB+Lucene，八步管道实现全链路数据标准化
- **Visualization**: pipeline_with_stages
- **Content**:
  - **数据来源**：ERP(MySQL) · CRM(Oracle) · OA(REST) · 文件服务器(S3/NFS) · 手动上传(PDF/DOCX)
  - **加工管道**：①接入网关(Connector策略,JDBC/REST/S3,全量+增量CDC) → ②质量检查(完整/唯一/格式,合格≥50%放行,<50%阻断) → ③ETL标准化(UTF-8,字典翻译,结构化→自然语言) → ④知识目录索引(域→数据源→实体) → ⑤切片+向量化(500字/块,批量Embedding 32条/批)
  - **存储**：MySQL(元数据) + ChromaDB(向量检索) + Lucene(BM25检索)
  - **底部标注**：🔄 增量同步(CDC) · 🛡️ 质量兜底(自动阻断+告警) · ⚡ 异步解耦(Spring Event)

---

### Part 4: 功能详情与落地路线 (P14–P19)

#### Slide 14 — 核心功能全景图

- **Layout**: hub_spoke — 中心辐射式
- **Title**: 核心功能全景图
- **Core message**: 一个框、10秒、精准答案——八大功能模块围绕企业知识中枢核心协同工作
- **Visualization**: hub_spoke
- **Content**:
  - **中心**：企业知识中枢 — 一个框 · 10秒 · 精准答案
  - **八模块**：智能问答（自然语言提问·流式输出·答案可溯源）· 知识管理（全生命周期·扫描件OCR·表格保留）· 多源接入（JDBC/REST/S3连接器·增量同步·ETL标准化）· 权限管控（ABAC五维·全链路审计日志）· 统计仪表（问答趋势·满意度·热点Top10）· 知识目录（域→源→实体·树形浏览·精准检索）· 质量管控（多维校验·异常阻断·质量报告）· 反馈闭环（👍👎评价·质量持续优化·Prompt迭代）

#### Slide 15 — 智能问答：不止是"聊天"

- **Layout**: Asymmetric split — 左：传统vs知识中枢对比 + 右：四种问答能力 + 底部场景示例
- **Title**: 智能问答：不止是"聊天"
- **Core message**: 从"关键词搜128个文件"到"自然语言问→一个精准答案+来源引用"
- **Visualization**: —
- **Content**:
  - **与传统搜索的差异**：关键词→自然语言 · 128个文件列表→1个精准答案 · 字面匹配→语义理解（薪酬=工资=薪水）· 点开自己找→标注文档名+章节
  - **四种问答能力**：💬语义问答 · 🔢结构化查询 · 📅日期范围 · 📊枚举匹配
  - **底部场景**：员工输入"出差报销需要什么材料？"→ 系统秒回"根据《财务报销制度》：出差申请单+发票原件+行程单，3个工作日内提交"→ 📄来源：《财务报销制度 v3.1》第4章

#### Slide 16 — 知识管理+多源接入

- **Layout**: Top-bottom split — 上：知识管理全生命周期 + 下：多源数据接入能力
- **Title**: 知识管理+多源接入
- **Core message**: 从上传到可检索的全生命周期管理 + 四种数据源接入方式，延迟不超过30分钟
- **Visualization**: —
- **Content**:
  - **知识管理全生命周期**：上传→解析→切片→向量化→入库→可检索→更新→删除 · 状态追踪(UPLOADED→READY/FAILED) · 失败重试(DLQ死信队列,3次退避) · 归档(软删除即刻不可检索)
  - **多源数据接入**：🗄️ 关系型数据库(JDBC连接器) · 🌐 REST API(HTTP连接器) · 📁 文件系统(S3/NFS连接器) · 📎 手动上传(Web界面)
  - **底部标注**：全量同步初始化 + 增量CDC持续更新

#### Slide 17 — 权限管控：五维度细粒度访问控制

- **Layout**: Top-bottom split — 上：ABAC决策示例 + 下：五维度+三级演进
- **Title**: 权限管控：五维度细粒度访问控制
- **Core message**: 角色+部门+密级+职级+时间——五维度ABAC满足等保2.0/GDPR合规要求
- **Visualization**: —
- **Content**:
  - **ABAC决策示例**：张三(HR专员,密级2)请求《2024年薪资方案》(HR域,密级3)→❌拒绝：密级不足 · 李四(HR总监,密级3)请求同一文档→✅允许：密级满足、域匹配
  - **五维度**：角色(admin/user) · 部门(HR/财务/研发隔离) · 密级(公开→内部→机密→绝密) · 职级(manager+可上传) · 时间(时段/有效期)
  - **三级演进**：P1 RBAC基础 → P2 +部门+密级+职级 → P3 ABAC全维度启用

#### Slide 18 — 典型用户场景

- **Layout**: Three-column scenario cards
- **Title**: 典型用户场景
- **Core message**: 覆盖一线员工、中层管理、高层管理、IT管理员四类角色——每个角色都有自己的知识获取方式
- **Visualization**: —
- **Content**:
  - 👤 新员工入职：小王研发部新人输入"新人开发环境搭建完整步骤"→30分钟搞定环境，上手从2天→半天
  - 👥 跨部门协作：张经理研发→销售输入"销售部客户签约最新流程"→30秒了解流程变化，不需打扰销售同事
  - 📋 合规审计：李总CFO输入"2024Q1差旅报销调整政策"→秒级定位签发文件，审计有据可查
  - **底部总述**：覆盖四类用户角色 — 一线员工｜中层管理｜高层管理｜IT管理员

#### Slide 19 — 分期落地路线图

- **Layout**: numbered_steps — 三阶段横向展开
- **Title**: 分期落地路线图
- **Core message**: 8个月三阶段——第1月末内部Demo→第3月末P1上线→第5月末P2上线→第8月末全员覆盖
- **Visualization**: numbered_steps
- **Content**:
  - **P1 MVP (第1-3月)**：✅核心问答 · ✅基础RBAC · ✅文档管理 · ✅Dashboard统计 · ✅用户管理 · 人力2-3人 · 日活30-50人
  - **P2 增强 (第4-5月)**：✅多源数据接入 · ✅混合检索+Rerank · ✅部门+密级权限 · ✅SSE流式输出 · ✅反馈闭环 · ✅ETL标准化 · 人力+2人 · 日活100-200人
  - **P3 全面 (第6-8月)**：✅全业务域覆盖 · ✅ABAC全维度 · ✅知识目录全公司 · ✅模型微调 · ✅SLA监控告警 · 人力+2人 · 日活全员200+

---

### Part 5: 设计附注与结尾 (P20–P21)

#### Slide 20 — 动效设计方案汇总

- **Layout**: basic_table
- **Title**: 动效设计方案汇总
- **Core message**: 十种动效类型覆盖21页——每页动效控制在2-4秒，整体使用Morph平滑过渡
- **Visualization**: basic_table
- **Content**:
  - 十种动效类型对照表：类型 · 应用页面 · 效果描述 · 设计意图
  - 底部建议：整体使用Morph平滑过渡 · 关键数据页深色背景 · 文字页浅色面板保证可读性 · 动效总时长每页2-4秒

#### Slide 21 — 结尾页

- **Closing impact**: 三个核心数字（10秒 · 99.6% · 221%）依次放大弹入，配以"乘政策东风，以AI Agent之力，让企业知识触手可及"——以数据冲击收束叙事弧线
- **Layout**: Single column centered — 居中大字排版
- **Content**:
  - **上半部分**：乘政策东风，以AI Agent之力 · 让企业知识，触手可及
  - **中部三个核心数字**：10秒（从提问到答案）· 99.6%（单次成本降低）· 221%（三年投资回报率）
  - **底部**：感谢聆听 🙏 · 联系方式/二维码 · 企业知识中枢团队 · 2026.06

---

## X. Speaker Notes Requirements

- **Total duration**: 20–25分钟
- **Notes style**: conversational — 如与听众对话，叙事驱动，每页含场景-冲突-解决结构
- **Presentation purpose**: persuade — 说服中高层决策者采纳企业知识中枢方案
- **File naming**: `notes/01_cover.md` ~ `notes/21_closing.md`
- **Content structure**: 每页含演讲要点、时间提示、过渡语

---

## XI. Technical Constraints Reminder

### SVG Generation Must Follow:

1. viewBox: `0 0 1280 720`
2. Background uses `<rect>` elements
3. Text wrapping uses `<tspan>` (`<foreignObject>` FORBIDDEN)
4. Transparency uses `fill-opacity` / `stroke-opacity`; `rgba()` FORBIDDEN
5. FORBIDDEN: `mask`, `<style>`, `class`, `foreignObject`
6. FORBIDDEN: `textPath`, `animate*`, `script`
7. Text characters: write typography & symbols as raw Unicode; HTML named entities FORBIDDEN; XML reserved chars escaped as `&amp;` `&lt;` `&gt;` `&quot;` `&apos;`
8. `marker-start` / `marker-end` conditionally allowed: `<marker>` in `<defs>`, `orient="auto"`, shape triangle/diamond/circle
9. `clipPath` conditionally allowed **only on `<image>` elements**

### PPT Compatibility Rules:

- `<g opacity="...">` FORBIDDEN; set opacity on each child individually
- Image transparency uses overlay mask layer
- Inline styles only; external CSS and `@font-face` FORBIDDEN
