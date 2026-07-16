# Agent Management System

AI Agent 全生命周期管理平台 —— 对智能体、工具、工作流、知识库、监控与成本进行统一编排与管理。

## 项目概览

| 模块 | 说明 |
|------|------|
| **Agent 管理** | 创建、配置、发布智能体，关联工具与知识库，管理 Prompt 版本 |
| **工具管理** | 注册 API / MCP 工具，配置参数与凭证，测试连通性 |
| **工作流编排** | 可视化画布拖拽编排 Agent 与工具的执行流程，支持条件分支与人工审批 |
| **知识库** | 文档上传（支持大文件分片），向量检索，与 Agent 绑定 |
| **会话管理** | 创建会话，SSE 流式对话，查看执行步骤与工具调用链路 |
| **监控面板** | Agent 健康度、Token 用量趋势、错误日志、告警规则配置 |
| **成本管理** | 预算设置、Token 消耗明细、费用分析 |
| **审计日志** | 全操作审计记录（AOP 切面自动采集），支持按工作空间查询 |
| **用户与权限** | 注册 / 登录（JWT），RBAC 角色控制（ADMIN / MANAGER / DEVELOPER / VIEWER） |
| **工作空间** | 多租户隔离，成员邀请与管理 |

## 技术栈

### 后端

| 组件 | 技术选型 | 版本 |
|------|---------|------|
| 框架 | Spring Boot | 2.7.18 |
| JDK | OpenJDK | 1.8 |
| ORM | MyBatis-Plus | 3.5.3.1 |
| 数据库 | MySQL | 8.x |
| 连接池 | Druid | 1.2.20 |
| 缓存 | Redis + Lettuce | — |
| 认证 | Spring Security + JWT (jjwt) | 0.11.5 |
| 工具库 | Hutool + Lombok | 5.8.25 |
| AOP | spring-boot-starter-aop | — |

### 前端（独立仓库）

| 组件 | 技术选型 |
|------|---------|
| 框架 | Vue 3 + TypeScript |
| 构建 | Vite |
| UI | Element Plus |
| 状态管理 | Pinia |
| 流程编排 | Vue Flow |
| HTTP | Axios |

## 项目结构

```
backend/
├── src/main/java/com/agentmanagement/
│   ├── AgentManagementApplication.java    # 启动类（@MapperScan）
│   ├── common/                            # 通用组件
│   │   ├── annotation/                    #   自定义注解（@AuditLog）
│   │   ├── aspect/                        #   AOP 切面（审计日志自动采集）
│   │   ├── Result.java                    #   统一响应 {code, message, data}
│   │   ├── PageResult.java                #   分页响应 {list, total, page, pageSize}
│   │   ├── ResultCode.java                #   业务状态码（1xxx 通用 / 20xx 数据 / 21xx Agent / 22xx 工具）
│   │   ├── BusinessException.java         #   业务异常
│   │   └── GlobalExceptionHandler.java    #   全局异常处理
│   ├── configuration/
│   │   └── PageConfiguration.java         #   分页插件配置
│   ├── controller/                        # RESTful 接口层
│   │   ├── AuthController.java            #     认证（登录 / 注册）
│   │   ├── AgentController.java           #     Agent CRUD + 状态变更
│   │   ├── ToolController.java            #     工具 CRUD + 测试
│   │   ├── WorkflowController.java        #     工作流 CRUD + 画布保存
│   │   ├── KnowledgeBaseController.java   #     知识库 + 文档管理
│   │   ├── MonitorController.java         #     监控面板 + 告警规则
│   │   ├── CostController.java            #     成本 / 预算管理
│   │   ├── AuditLogController.java        #     审计日志查询
│   │   └── HealthController.java          #     健康检查
│   ├── entity/                            # 数据库实体（30 个）
│   ├── form/                              # 请求表单（参数校验）
│   ├── vo/                                # 视图对象（响应结构）
│   ├── mapper/                            # MyBatis-Plus Mapper
│   ├── security/                          # 安全模块
│   │   ├── SecurityConfig.java            #     Spring Security 配置
│   │   ├── JwtAuthenticationFilter.java   #     JWT 认证过滤器
│   │   ├── SecurityUtils.java             #     安全上下文工具
│   │   └── WorkspaceInterceptor.java      #     工作空间隔离拦截器
│   └── service/                           # 业务逻辑层
│       └── impl/                          #     9 个 Service 实现
├── src/main/resources/
│   └── application.yml                    # 应用配置
└── pom.xml
```

## 快速启动

### 环境要求

- JDK 1.8+
- MySQL 8.x
- Redis
- Maven 3.6+

### 1. 数据库

创建数据库并执行建表脚本：

```sql
CREATE DATABASE IF NOT EXISTS agent_management
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;
```

### 2. 配置

编辑 `src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/agent_management?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```

### 3. 启动

```bash
# 编译
mvn clean package -DskipTests

# 运行
java -jar target/backend-1.0-SNAPSHOT.jar

# 或开发模式
mvn spring-boot:run
```

服务默认运行在 `http://localhost:8080/api/v1`

### 4. 接口概览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 用户登录（返回 JWT） |
| POST | `/auth/register` | 用户注册 |
| GET | `/agents` | Agent 分页列表 |
| POST | `/agents` | 创建 Agent |
| GET | `/agents/{id}` | Agent 详情 |
| PUT | `/agents/{id}` | 更新 Agent |
| PATCH | `/agents/{id}/status` | 变更 Agent 状态 |
| DELETE | `/agents/{id}` | 删除 Agent |
| GET | `/tools` | 工具分页列表 |
| POST | `/tools` | 注册工具 |
| GET | `/tools/{id}` | 工具详情 |
| PUT | `/tools/{id}` | 更新工具 |
| DELETE | `/tools/{id}` | 删除工具 |
| POST | `/tools/{id}/test` | 测试工具 |
| GET | `/workflows` | 工作流列表 |
| POST | `/workflows` | 创建工作流 |
| GET | `/workflows/{id}` | 工作流详情（含画布） |
| PUT | `/workflows/{id}` | 保存工作流画布 |
| DELETE | `/workflows/{id}` | 删除工作流 |
| GET | `/knowledge-bases` | 知识库列表 |
| POST | `/knowledge-bases` | 创建知识库 |
| POST | `/knowledge-bases/{id}/documents` | 上传文档 |
| GET | `/monitor/overview` | 监控概览 |
| GET | `/monitor/token-trend` | Token 趋势 |
| GET | `/monitor/agent-health` | Agent 健康度 |
| GET | `/cost/overview` | 成本概览 |
| POST | `/cost/budget` | 设置预算 |
| GET | `/audit-logs` | 审计日志 |

> 完整接口文档参见各 Controller 源码中的 Javadoc 注释。

## 核心设计

### 统一响应格式

```json
{
  "code": 0,
  "message": "ok",
  "data": { ... }
}
```

- `code = 0` 表示成功，其余为业务错误码
- 分页接口额外返回 `{ list, total, page, pageSize }`

### 认证与权限

- **无状态 JWT**：登录后返回 Token，前端通过 `Authorization: Bearer <token>` 请求头传递
- **RBAC 四级角色**：ADMIN（管理员）> MANAGER（管理者）> DEVELOPER（开发者）> VIEWER（观察者）
- **工作空间隔离**：通过 `X-Workspace-Id` 请求头实现多租户数据隔离

### 审计日志（AOP 自动采集）

通过 `@AuditLog` 注解 + `AuditLogAspect` 切面，自动记录操作日志，无需手动埋点：

```java
@AuditLog(action = "agent.create", label = "创建 Agent", resourceType = "agent")
@PostMapping
public Result<AgentVO> create(@Valid @RequestBody AgentCreateForm form) { ... }
```

- 自动采集：操作者、IP、User-Agent、资源 ID / 名称
- 支持 SpEL 表达式精确定位资源：`@AuditLog(resourceIdExpr = "#id")`
- 失败不阻断业务

### 错误码分段

| 范围 | 模块 |
|------|------|
| 1xxx | 通用（参数校验、未授权、限流等） |
| 20xx | 数据层 |
| 21xx | Agent |
| 22xx | 工具 |
| 2002-2003 | 知识库 / 文档 |

## 相关仓库

- **前端**：[agent-management-frontend](https://github.com/qbdx-hub/agent-management-frontend)（Vue 3 + Element Plus）

## License

MIT
