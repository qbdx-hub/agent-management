# 前端图标清单（图标替换用）

> **怎么用**：每个图标都有一个【语义名】。换图标时告诉我「把【语义名】换成【新 Element 图标名】」，我会定位并替换所有相关位置。
> 例如：把【新建/添加按钮】的 `Plus` 换成 `CirclePlus`；或把【菜单-工作台】换成 `House`。
> 也可以只换某一处：如「只换 Dashboard 的【注册工具按钮】」。
> 图标名查 Element Plus 官方图标库：<https://element-plus.org/zh-CN/component/icon.html>（名字用 PascalCase，如 `CirclePlus`、`View`、`Edit`、`Delete`、`Refresh`、`Download`、`House`）。

> 图标注册方式：`main.ts` 全局注册了 Element Plus 全部图标，所以模板里直接写图标组件名（无需 import）。

---

## 一、功能 / 交互按钮图标

| # | 语义名 | 当前图标 | 用在（文件） |
|---|---|---|---|
| 1 | 【新建/添加按钮】 | `Plus` | DashboardView、AgentList、BudgetConfig、KnowledgeList、WorkflowList、WorkflowEditor(×3)、AlertConfig、SessionHistory、ToolMarket、MemberManage、ToolRegister |
| 2 | 【返回按钮】 | `ArrowLeft` | BudgetConfig、TraceDetail、KnowledgeDetail、WorkflowRun、WorkflowEditor、AlertConfig、SessionConsole |
| 3 | 【下拉/展开箭头】 | `ArrowDown` | TopHeader、SessionConsole |
| 4 | 【侧栏展开按钮】 | `Expand` | TopHeader（侧栏折叠时显示） |
| 5 | 【侧栏收起按钮】 | `Fold` | TopHeader（侧栏展开时显示） |
| 6 | 【用户头像占位】 | `UserFilled` | TopHeader、MemberManage |
| 7 | 【搜索框前缀】 | `Search` | ToolMarket |
| 8 | 【对话按钮】 | `ChatDotRound` | AgentDetail |
| 9 | 【会话历史按钮】 | `List` | AgentDetail |
| 10 | 【已绑定勾选】 | `CircleCheckFilled` | ToolBinding |
| 11 | 【文件上传区】 | `UploadFilled` | KnowledgeDetail |
| 12 | 【保存按钮】 | `Check` | WorkflowEditor |
| 13 | 【运行按钮】 | `VideoPlay` | WorkflowEditor |
| 14 | 【停止按钮】 | `VideoPause` | SessionConsole |
| 15 | 【发送按钮】 | `Promotion` | SessionConsole |
| 16 | 【注册工具按钮】 | `SetUp` | DashboardView |
| 17 | 【查看监控按钮】 | `Monitor` | DashboardView |

## 二、侧栏菜单图标（`router/routes.ts` 的 `meta.icon` 字符串）

> 由 `SidebarMenu.vue` 用 `<component :is="item.icon">` 渲染。

| # | 语义名 | 当前图标 | 菜单项 |
|---|---|---|---|
| 18 | 【菜单-工作台】 | `HomeFilled` | 工作台 |
| 19 | 【菜单-Agent管理】 | `Cpu` | Agent 管理 |
| 20 | 【菜单-工具市场】 | `SetUp` | 工具市场 |
| 21 | 【菜单-Agent编排】 | `Share` | Agent 编排 |
| 22 | 【菜单-知识库】 | `Collection` | 知识库 |
| 23 | 【菜单-监控面板】 | `Monitor` | 监控面板 |
| 24 | 【菜单-成本管理】 | `Money` | 成本管理 |
| 25 | 【菜单-审计日志】 | `Lock` | 审计日志 |
| 26 | 【菜单-空间设置】 | `Setting` | 空间设置 |

## 三、Emoji 工具/头像图标（字符串，非图标库）

> 这些是工具和 Agent 的 `avatar`/`icon` 字段存的 emoji 字符。换的话改对应字符串即可（告诉我「把工具搜索图标 🔍 换成 🛰️」之类）。

| Emoji | 位置 |
|---|---|
| 🔍 ⚡ 📁 🐙 📧 🖼️ 🗄️ 💬 | `mock/tools.ts` 各工具 |
| 🔧 | `ToolRegister.vue` 默认图标 |
| 🤖 等 | Agent 的 avatar 字段（用户创建时自填） |

## 四、自定义内联 SVG（手写 path，非图标库）

> 登录/注册页的 logo 和输入框前缀图标是内联 `<svg><path/></svg>`，不在这套 Element 图标体系内，需单独改 SVG path。

| 位置 | 说明 |
|---|---|
| `LoginView.vue` | logo + 用户名/密码输入框前缀 SVG |
| `RegisterView.vue` | logo + 各输入框前缀 SVG |

---

## 替换方式速查

| 类型 | 改什么 |
|---|---|
| 功能按钮图标（一） | 改对应 `.vue` 里 `<el-icon><Xxx /></el-icon>` 的组件名（`Xxx` 换成新图标名） |
| 菜单图标（二） | 改 `router/routes.ts` 对应路由的 `meta: { icon: 'Xxx' }` 字符串 |
| Emoji（三） | 改 `mock/tools.ts` 或表单默认值的 emoji 字符串 |
| 自定义 SVG（四） | 改对应 `.vue` 的 `<path d="...">` |

**注意**：
- 新图标名必须是 Element Plus 图标库中存在的（PascalCase）。
- 【新建/添加按钮】(#1) 涉及 10+ 个文件，确认要全局替换时我会一次全改。
- #16【注册工具按钮】和 #20【菜单-工具市场】当前都是 `SetUp`、#17【查看监控】和 #23【菜单-监控面板】都是 `Monitor`——它们位置不同，可分别换。
