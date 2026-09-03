# Agent 管理系统

AI Agent 全生命周期管理平台，支持 Agent 创建、调试、监控、编排和知识库管理。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3.5 + TypeScript 6 + Vite 8 + Element Plus + Pinia |
| 后端 | Spring Boot 2.7.18 + MyBatis-Plus + Spring Security |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| 实时通信 | SSE (Server-Sent Events) |

## 项目结构

```
agent-management/
├── docs/                           # 项目文档
│   ├── PRD-agent-management-system.md    # 产品需求文档
│   ├── frontend-architecture.md          # 前端架构设计
│   ├── api-specification.md              # API 接口规范
│   ├── database-schema.sql               # 数据库建表脚本
│   ├── database-module-mapping.md        # 表与模块对应关系
│   └── backend-conventions.md            # 后端开发规范
├── frontend/                       # 前端项目
│   ├── src/
│   │   ├── api/                    # API 接口层
│   │   ├── components/             # 公共组件
│   │   │   ├── common/             # 通用业务组件
│   │   │   └── layout/             # 布局组件
│   │   ├── mock/                   # Mock 数据
│   │   ├── router/                 # 路由
│   │   ├── stores/                 # Pinia 状态管理
│   │   ├── types/                  # TypeScript 类型定义
│   │   ├── utils/                  # 工具函数
│   │   └── views/                  # 页面组件
│   ├── .env.development            # 开发环境变量
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
└── backend/                        # 后端项目
    ├── src/main/java/com/agentmanagement/
    │   ├── controller/             # REST 控制器
    │   ├── service/                # 业务逻辑层
    │   ├── mapper/                 # MyBatis-Plus Mapper
    │   ├── entity/                 # 数据库实体
    │   ├── form/                   # 请求表单 DTO
    │   ├── vo/                     # 响应视图对象 VO
    │   ├── common/                 # 公共类 (Result/ResultCode/异常)
    │   ├── configuration/          # 配置类
    │   ├── security/               # 安全上下文工具
    │   └── annotation/             # 自定义注解 (审计日志)
    └── pom.xml
```

## 功能模块

| 模块 | 前端路由 | 后端接口 | 说明 |
|------|----------|----------|------|
| 仪表盘 | `/dashboard` | - | Agent 卡片、统计概览、快速创建 |
| Agent 管理 | `/agents/*` | `/api/v1/agents` | Agent CRUD、模型配置、提示词编辑、工具绑定 |
| 工具市场 | `/tools/*` | `/api/v1/tools` | 工具注册、连通性测试、调用统计（MCP 协议未实现，仅 API 工具） |
| 会话控制台 | `/agents/:id/chat` | `/api/v1/sessions` | 实时对话、SSE 流式、执行步骤可视化 |
| 监控中心 | `/monitor/*` | `/api/v1/monitor` | Agent 健康、Token 趋势、告警规则（定时评估） |
| 费用管理 | `/cost/*` | `/api/v1/costs`、`/api/v1/budgets` | 费用统计、预算配置与熔断 |
| 安全审计 | `/security/*` | `/api/v1/audit-logs`、`/api/v1/security` | 审计日志、角色权限、审批中心 |
| 工作空间 | `/workspace/*` | `/api/v1/workspaces` | 成员管理、空间设置、空间动态 |
| 编排引擎 | `/orchestration/*` | `/api/v1/workflows` | 可视化画布、工作流执行引擎（含人工审批暂停/恢复） |
| 知识库 | `/knowledge/*` | `/api/v1/knowledge-bases` | 文档直传（≤50MB）、解析分块、向量化、RAG 检索 |
| 认证 | `/login` | `/api/v1/auth` | 登录、注册、JWT 认证 |

## 快速开始

### 数据库

```sql
-- 导入数据库（MySQL 8.0+）
SET NAMES utf8mb4;
source docs/database-schema.sql;
```

默认用户：`admin` / `admin123`

### 后端

```bash
cd backend

# 配置数据库连接（修改 application-dev.yml）
# spring.datasource.url=jdbc:mysql://localhost:3306/agent_management
# spring.datasource.username=dev
# spring.datasource.password=your_password

mvn spring-boot:run
```

后端默认运行在 http://localhost:8080

### 前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173

> 前端已接通后端真实接口，需先启动后端服务。

## 开发文档

- [产品需求文档 (PRD)](docs/PRD-agent-management-system.md)
- [后端开发规范](docs/backend-conventions.md)
- [前端架构设计](docs/frontend-architecture.md)
- [API 接口规范](docs/api-specification.md)
- [数据库建表脚本](docs/database-schema.sql)
- [数据库表-模块对应关系](docs/database-module-mapping.md)
- [Git 培训教程](docs/git-training.md)
- [Git 命令速查](docs/git-cheatsheet.md)

## 团队协作

三人共用一个数据库，数据库在开发者的本机上：

1. 数据库主机开放 MySQL 3306 端口（防火墙放行）
2. MySQL 创建远程用户 `dev@'%'`
3. 同事修改 `application-dev.yml` 中的数据库地址为主机 IP

详见 [团队协作指南](team-collaboration-guide.md)

## 最近更新

- **2026-09-03**: Agent 执行面升级为真实作用型（对标 Claude Code 类 harness 的工具面）：新增 8 个内置工具（builtin 类型，V9 迁移种子）——read_file/write_file/edit_file/list_files(glob)/search_files(grep)/run_command/web_search(必应)/web_fetch；执行于会话级沙箱（`agent.sandbox.root/session-{sessionId}`），路径越界拒绝、命令超时/输出上限、web_fetch 拒绝内网地址（SSRF 防护）；与 Function Calling 循环、SSE 步骤、tool_call_record 归因、市场统计完全打通；命令输出按系统字符集解码（中文 Windows cmd 为 GBK）
- **2026-09-03**: 工具市场去除账户/工作空间隔离，所有账户可见、可绑定、可调用；预置 8 个免费免密钥常用 API 工具（天气查询、城市定位、汇率换算、文本翻译、世界时钟、短链接生成、GitHub 仓库搜索、每日一言，V8 迁移幂等种子）；修复 GET 工具查询串未 URL 编码导致中文/空格参数请求损坏的问题
- **2026-09-03**: 会话对话接入原生 Function Calling：Agent 绑定的 API 工具自动转为 OpenAI tools 定义随请求下发，模型发起 tool_calls 后真实执行 HTTP（计入 tool_call_record 并归因 agent/session），结果以 role=tool 回传继续生成，SSE 新增 tool_call 事件、前端执行步骤实时展示；多轮工具调用受 Agent 最大迭代数约束（封顶 5 轮）。修复仅提交工具绑定时 `updateAgent` 生成空 SET 语句被 Druid 拒绝导致绑定保存 500 的问题；修复工作流工具节点在异步线程取 SecurityContext 崩溃
- **2026-09-03**: 模型目录刷新为 2026 现役型号（V7 迁移）：DeepSeek V4-Flash/V4-Pro、智谱 GLM-5.3/5.3-Flash/5.2/4.7-Flash、Kimi K3/K2.7 Code/K2.6、MiniMax M3/M2.7、小米 MiMo V2.5/V2.5-Pro 及 GPT-5/Claude 5 系列，共 22 款；PC 端 Agent 创建/配置的供应商与模型下拉改接 `GET /models` 真实接口，选择供应商自动回填 OpenAI 兼容 Base URL、选择模型自动回填官方单价；修复工作流编排页打不开（重复 handleSave 编译错误）
- **2026-09-02**: 功能整改一批（详见 docs/审计整改报告-2026-09-01.md）：SSE 真流式与线程池化、PDF/docx 真解析、工具绑定/连通性测试落库、监控指标接真数据、告警定时评估、预算真扣减与熔断、工作流执行引擎（拓扑执行/条件分支/审批暂停恢复）、空间与安全中心后端补齐、知识库上传改直传（≤50MB）、冗余代码清理
- **2026-07-14**: 后端完整实现 - Agent/Tool/Workflow/KnowledgeBase/AuditLog/Session/Monitor/Cost 模块
- **2026-07-14**: 前端接通后端真实接口，Vue Flow 编排画布
- **2026-07-13**: 成本管理预算配置 + 监控面板 + 用户个人信息修改
