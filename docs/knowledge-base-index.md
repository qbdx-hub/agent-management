# Agent 管理系统 - 实训知识库索引

本知识库包含云南民族大学软件工程项目综合实训的全部文档资料。

## 📁 文档目录

### 项目文档
| 文档 | 说明 | 路径 |
|------|------|------|
| [项目文档-童倍](项目文档-童倍.md) | 完整的项目设计文档，包含需求分析、技术架构、数据库设计等 | `docs/项目文档-童倍.md` |
| [答辩答疑表](答辩答疑表.md) | 答辩问答记录，涵盖系统架构、技术实现等核心问题 | `docs/答辩答疑表.md` |

### 个人文档
| 文档 | 说明 | 路径 |
|------|------|------|
| [实习报告-童倍](实习报告-童倍.md) | 毕业实习报告，记录实习过程和收获 | `docs/实习报告-童倍.md` |
| [实训日志-童倍](实训日志-童倍.md) | 每日实训记录，包含学习内容、问题和解决方案 | `docs/实训日志-童倍.md` |

### 系统设计文档
| 文档 | 说明 | 路径 |
|------|------|------|
| [PRD 产品需求文档](PRD-agent-management-system.md) | 完整的产品需求规格说明 | `docs/PRD-agent-management-system.md` |
| [API 接口规范](api-specification.md) | RESTful API 接口定义 | `docs/api-specification.md` |
| [前端架构设计](frontend-architecture.md) | Vue 3 前端架构说明 | `docs/frontend-architecture.md` |
| [后端开发规范](backend-conventions.md) | Spring Boot 后端开发规范 | `docs/backend-conventions.md` |
| [数据库建表脚本](database-schema.sql) | MySQL 8.0 数据库表结构 | `docs/database-schema.sql` |
| [数据库模块映射](database-module-mapping.md) | 表与功能模块对应关系 | `docs/database-module-mapping.md` |

## 📋 核心知识点总结

### 1. 系统架构
- **前端**: Vue 3.5 + TypeScript + Vite 8 + Element Plus + Pinia
- **后端**: Spring Boot 2.7.18 + MyBatis-Plus + Spring Security
- **数据库**: MySQL 8.0 (utf8mb4) + Redis 7
- **通信**: RESTful API + SSE 流式推送

### 2. 功能模块（10大模块）
1. **Agent 管理** - 创建、配置、版本管理
2. **工具市场** - API 注册、MCP 协议支持
3. **编排引擎** - 可视化工作流、多 Agent 协作
4. **知识库** - 文档上传、RAG 检索
5. **会话控制台** - 实时对话、SSE 流式
6. **监控中心** - 健康检查、告警规则
7. **成本管理** - Token 统计、预算配置
8. **安全审计** - RBAC 权限、操作日志
9. **工作空间** - 成员管理、权限控制
10. **认证授权** - JWT Token、登录注册

### 3. 技术亮点
- **Vue Flow** - 可视化工作流编排画布
- **SSE** - Agent 执行过程实时推送
- **RAG** - 知识库检索增强生成
- **RBAC** - 角色权限访问控制
- **审计日志** - 全链路操作追溯

### 4. 数据库设计
- 共 **30+ 张数据表**
- 以工作空间(workspace)为隔离单元
- JSON 字段存储复杂配置（如 Agent 的 model_config）
- 统一字段: created_at, updated_at, created_by, workspace_id

## 🔗 相关资源

- **GitHub 仓库**: https://github.com/qbdx-hub/agent-management
- **前端地址**: http://localhost:5173
- **后端地址**: http://localhost:8080

## 📅 更新记录

| 日期 | 内容 |
|------|------|
| 2026-07-16 | 创建知识库，导入实训文档 |
| 2026-07-14 | 后端完整实现，前端接通真实接口 |
| 2026-07-13 | 成本管理、监控面板功能完成 |
