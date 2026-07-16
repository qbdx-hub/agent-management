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
| Agent 管理 | `/agent/*` | `/api/agents` | Agent CRUD、模型配置、提示词编辑、工具绑定 |
| 工具市场 | `/tool/*` | `/api/tools` | 工具注册、测试、MCP 支持 |
| 会话控制台 | `/session/*` | `/api/sessions` | 实时对话、SSE 流式、执行步骤可视化 |
| 监控中心 | `/monitor/*` | `/api/monitor` | Agent 健康、Token 趋势、告警规则 |
| 费用管理 | `/cost/*` | `/api/costs` | 费用统计、预算配置 |
| 安全审计 | `/security/*` | `/api/audit-logs` | 角色权限、审计日志、API Key |
| 工作空间 | `/workspace/*` | - | 成员管理、工作空间设置 |
| 编排引擎 | `/orchestration/*` | `/api/workflows` | 可视化工作流编辑器、多 Agent 协作 |
| 知识库 | `/knowledge/*` | `/api/knowledge-bases` | 文档上传、向量化、RAG 检索 |
| 认证 | `/login` | `/api/auth` | 登录、注册、JWT 认证 |

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

- **2026-07-14**: 后端完整实现 - Agent/Tool/Workflow/KnowledgeBase/AuditLog/Session/Monitor/Cost 模块
- **2026-07-14**: 前端接通后端真实接口，Vue Flow 编排画布
- **2026-07-13**: 成本管理预算配置 + 监控面板 + 用户个人信息修改
