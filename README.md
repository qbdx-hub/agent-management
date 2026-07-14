# Agent 管理系统

AI Agent 全生命周期管理平台，支持 Agent 创建、调试、监控、编排和知识库管理。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3.5 + TypeScript 6 + Vite 8 + Element Plus + Pinia |
| 后端 | Spring Boot 3 + MyBatis-Plus (开发中) |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| 实时通信 | SSE (Server-Sent Events) |

## 项目结构

```
agent-management/
├── docs/                           # 项目文档
│   ├── PRD-agent-management-system.md    # 产品需求文档
│   ├── frontend-architecture.md          # 前端架构设计
│   ├── api-specification.md              # API 接口规范 (74个接口)
│   ├── database-schema.sql               # 数据库建表脚本 (30张表)
│   └── database-module-mapping.md        # 表与模块对应关系
├── frontend/                       # 前端项目
│   ├── src/
│   │   ├── api/                    # API 接口层 (8个模块)
│   │   ├── components/             # 公共组件
│   │   │   ├── common/             # 通用业务组件
│   │   │   └── layout/             # 布局组件
│   │   ├── mock/                   # Mock 数据
│   │   ├── router/                 # 路由 (22个路由 + 3级守卫)
│   │   ├── stores/                 # Pinia 状态管理 (5个Store)
│   │   ├── types/                  # TypeScript 类型定义
│   │   ├── utils/                  # 工具函数
│   │   └── views/                  # 页面组件 (10个模块)
│   ├── .env.development            # 开发环境变量
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
└── backend/                        # 后端项目 (待开发)
```

## 功能模块

| 模块 | 路径 | 说明 |
|------|------|------|
| 仪表盘 | `/dashboard` | Agent 卡片、统计概览、快速创建 |
| Agent 管理 | `/agent/*` | Agent CRUD、模型配置、提示词编辑、工具绑定、记忆配置 |
| 工具市场 | `/tool/*` | 工具注册、测试、MCP 支持 |
| 会话控制台 | `/session/*` | 实时对话、SSE 流式、执行步骤可视化 |
| 监控中心 | `/monitor/*` | Agent 健康、Token 趋势、告警规则、链路追踪 |
| 费用管理 | `/cost/*` | 费用统计、预算配置、模型/Agent 分布 |
| 安全审计 | `/security/*` | 角色权限、审计日志、审批流程、API Key |
| 工作空间 | `/workspace/*` | 成员管理、工作空间设置 |
| 编排引擎 | `/orchestration/*` | 可视化工作流编辑器、多 Agent 协作 |
| 知识库 | `/knowledge/*` | 文档上传、向量化、RAG 检索 |

## 快速开始

### 前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173

> 默认开启 Mock 模式，无需后端即可体验全部页面。

### 数据库

```sql
-- 导入数据库（MySQL 8.0+）
SET NAMES utf8mb4;
source docs/database-schema.sql;
```

默认用户：`admin` / `admin123`

### 后端（待开发）

```bash
cd backend
mvn spring-boot:run
```

## 开发文档

- [产品需求文档 (PRD)](docs/PRD-agent-management-system.md)
- [前端架构设计](docs/frontend-architecture.md)
- [API 接口规范](docs/api-specification.md)
- [数据库建表脚本](docs/database-schema.sql)
- [数据库表-模块对应关系](docs/database-module-mapping.md)

## 团队协作

三人共用一个数据库，数据库在开发者的本机上：

1. 数据库主机开放 MySQL 3306 端口（防火墙放行）
2. MySQL 创建远程用户 `dev@'%'`
3. 同事修改 `application-dev.yml` 中的数据库地址为主机 IP

详见 [团队协作指南](team-collaboration-guide.md)
