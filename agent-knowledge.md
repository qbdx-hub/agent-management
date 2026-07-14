# AI Agent 知识体系

## 一、Agent 基础概念

### 1.1 什么是 AI Agent

AI Agent（智能体）是一种能够**自主感知环境、做出决策并执行动作**的智能系统。与传统的 LLM 一问一答模式不同，Agent 具备：

- **自主性 (Autonomy)**：能独立规划和执行任务，无需人类逐步指导
- **目标驱动 (Goal-driven)**：围绕用户给定的目标，自行拆解并逐步达成
- **工具使用 (Tool Use)**：能够调用外部工具（API、数据库、文件系统等）完成子任务
- **环境感知 (Perception)**：从执行结果中获取反馈，动态调整策略
- **持久记忆 (Memory)**：跨会话保存信息，实现长期上下文管理

### 1.2 Agent 的核心公式

```
Agent = LLM + Planning + Memory + Tool Use + Reflection
```

| 组件 | 作用 | 典型实现 |
|------|------|---------|
| LLM | 理解、推理、生成的核心引擎 | GPT-4, Claude, Gemini, DeepSeek |
| Planning | 任务分解与执行路线规划 | ReAct, Plan-and-Solve, Tree of Thoughts |
| Memory | 短期/长期信息存储与检索 | 向量数据库, 摘要记忆, 关键值存储 |
| Tool Use | 与外部世界交互的能力 | Function Calling, MCP 协议, API 集成 |
| Reflection | 自我评估与纠错 | Self-Reflection, Reflexion, CRITIC |

### 1.3 Agent 与普通 LLM 调用的区别

| 维度 | 普通 LLM 调用 | AI Agent |
|------|-------------|----------|
| 交互模式 | 单轮/多轮对话 | 自主循环执行 |
| 能力边界 | 仅文本生成 | 可操作外部世界 |
| 错误处理 | 需人工纠正 | 自主检测与修复 |
| 任务复杂度 | 简单问答、摘要 | 多步骤复杂任务 |
| 上下文管理 | 有限窗口 | 持久记忆+检索 |
| 工具使用 | 无 | 可调用任意工具 |

---

## 二、Agent 架构模式

### 2.1 ReAct（Reasoning + Acting）— 最经典模式

ReAct 交替进行**推理**和**行动**，是大多数 Agent 框架的基石：

```
思考 → 行动 → 观察 → 思考 → 行动 → 观察 → ... → 结束
```

**特点**：
- 将推理过程与工具调用交织在一起
- 每一步都产生可解释的中间输出
- 适合需要持续决策的任务

### 2.2 Plan-and-Execute（计划-执行）

先制定完整计划，再逐步执行：

```
目标 → 拆解计划 → 执行步骤1 → 执行步骤2 → ... → 验证结果 → 调整计划 → ...
```

**优点**：整体规划能力强，减少短视行为  
**缺点**：初始计划可能不准确，需要后续调整

### 2.3 Multi-Agent（多智能体协作）

多个专业化 Agent 分工协作：

```
管理者Agent → 分配任务
  ├─ 代码Agent → 写代码
  ├─ 审查Agent → 审查代码
  ├─ 测试Agent → 运行测试
  └─ 部署Agent → 部署上线
```

**常见角色分工**：
- **Manager/Orchestrator**：任务调度与分配
- **Specialist**：各领域专家（编码、测试、设计等）
- **Critic/Reviewer**：检查、验证、提出改进
- **Synthesizer**：汇总多人输出，形成最终产物

### 2.4 Hierarchical Agent（层级Agent）

类似公司组织架构，形成树状管控：

```
顶层Agent（CEO级别）
├─ 技术领导Agent
│   ├─ 前端Agent
│   └─ 后端Agent
├─ 产品Agent
└─ 质量Agent
```

### 2.5 Swarm/Orchestrator Pattern

**Orchestrator**：中央调度器，显式管理 Agent 间交互  
**Swarm**：去中心化，Agent 间通过环境/黑板/消息自发协调

| 模式 | 优点 | 缺点 |
|------|------|------|
| Orchestrator | 可控性高、流程清晰 | 单点瓶颈、扩展性受限 |
| Swarm | 弹性好、可扩展 | 难以预测、调试复杂 |

---

## 三、核心能力详解

### 3.1 任务规划 (Planning)

#### 任务分解策略

```
1. Zero-shot Planning — 直接要求 LLM 生成计划
2. Few-shot Planning — 提供示例计划作为参照
3. 分而治之 — 大任务递归拆解直到可执行粒度
4. 动态 re-plan — 执行中根据反馈调整计划
```

#### 常见规划框架

| 框架 | 原理 | 适用场景 |
|------|------|---------|
| Chain-of-Thought (CoT) | 逐步推理 | 需推理的问答 |
| Tree of Thoughts (ToT) | 多路径探索+剪枝 | 复杂推理解题 |
| Graph of Thoughts (GoT) | 图结构思维组合 | 创意生成、综合 |
| ReWOO | 计划-收集-执行分离 | 高效工具调用 |

### 3.2 记忆系统 (Memory)

#### 三种记忆类型

```
┌─────────────────────────────────────┐
│            记忆系统                  │
├─────────────┬───────────┬───────────┤
│  工作记忆    │  短期记忆  │  长期记忆  │
│ (Working)    │ (Short)   │ (Long)    │
├─────────────┼───────────┼───────────┤
│ 当前任务上下文│ 本次会话历史│ 跨会话知识  │
│ LLM上下文窗口│ 摘要/缓存  │ 向量库/RAG │
└─────────────┴───────────┴───────────┘
```

#### 记忆管理策略

- **滑动窗口**：保留最近 N 轮交互
- **摘要压缩**：定期将长对话压缩为摘要
- **向量检索**：基于语义相似度检索历史记忆
- **关键值存储**：提取结构化信息存储
- **反思记忆**：Agent 反思经验教训存入长期记忆

### 3.3 工具使用 (Tool Use)

#### 工具定义规范

```yaml
tool:
  name: search_web
  description: "搜索互联网获取最新信息"
  parameters:
    - name: query
      type: string
      description: "搜索关键词"
      required: true
    - name: max_results
      type: integer
      description: "最大返回数量"
      default: 10
  returns: "包含标题、URL、摘要的结果列表"
```

#### 工具分类

| 类别 | 示例 |
|------|------|
| 查询类 | 数据库查询、API 调用、搜索引擎 |
| 计算类 | 代码执行、数学运算、数据处理 |
| 操作类 | 文件读写、发送邮件、创建工单 |
| 感知类 | 图片识别、语音转文字、文档解析 |
| 通信类 | 调用其他 Agent、通知用户 |
| 环境类 | 浏览器操作、终端命令、UI 操作 |

#### MCP 协议（Model Context Protocol）

MCP 是 Anthropic 提出的 Agent-工具标准化协议：

```
Agent ←→ MCP Client ←→ MCP Server ←→ 实际工具/资源

优势：
- 标准化接口，一次接入多处使用
- 工具提供方无需关注 LLM 适配
- 支持本地和远程工具
```

### 3.4 反思与自纠错 (Reflection)

#### 反思循环

```
执行 → 观察结果 → 自我评估 → 发现问题 → 生成修正方案 → 重新执行
```

#### 常见技术

- **Self-Refine**：Agent 对自己的输出进行迭代改进
- **Reflexion**：基于失败经验的长期记忆学习
- **CRITIC**：外部验证+自我修正
- **Debate**：多个 Agent 辩论，选出最佳方案
- **Verifier 模式**：独立的验证 Agent 检查结果正确性

---

## 四、Agent 框架与工具生态

### 4.1 主流 Agent 开发框架

| 框架 | 语言 | 特点 | 适用场景 |
|------|------|------|---------|
| LangChain/LangGraph | Python/JS | 生态丰富、社区大 | 通用 Agent 开发 |
| AutoGen (Microsoft) | Python | 多Agent对话、人机协作 | 复杂多人协作任务 |
| CrewAI | Python | 角色定义清晰、易上手 | 团队协作场景 |
| MetaGPT | Python | 软件公司SOP模拟 | 软件研发全流程 |
| Dify | Python | 低代码/可视化 | 快速原型/非开发者 |
| Claude Code | CLI | 原生工具使用、worktree | 软件开发Agent |
| OpenAI Agents SDK | Python | 官方出品、轻量 | OpenAI 生态 |
| Google ADK | Python | Google 生态 | Gemini 生态 |
| Semantic Kernel | .NET/Python | 微软生态 | 企业级应用 |

### 4.2 工具协议标准

| 协议 | 提出方 | 定位 |
|------|--------|------|
| MCP (Model Context Protocol) | Anthropic | Agent-工具标准化 |
| A2A (Agent-to-Agent) | Google | Agent间通信协议 |
| Function Calling (OpenAI格式) | OpenAI | 事实标准，各厂商兼容 |

### 4.3 评估与观测

| 工具/平台 | 用途 |
|-----------|------|
| LangSmith | Agent 全链路追踪与调试 |
| LangFuse | 开源 LLM 可观测性 |
| Braintrust | Agent 评估与实验管理 |
| Weights & Biases | ML 实验追踪（含 LLM） |
| Galileo | Agent 幻觉检测与质量监控 |

---

## 五、Agent 管理系统的核心关注点

### 5.1 Agent 生命周期管理

```
创建 → 配置 → 部署 → 监控 → 评估 → 优化 → 退役
```

#### 各阶段关键动作

| 阶段 | 核心事项 |
|------|---------|
| **创建** | 定义角色、目标、能力边界、权限 |
| **配置** | 选择模型、绑定工具、设定记忆策略 |
| **部署** | 运行环境、资源分配、并行度控制 |
| **监控** | Token 用量、延迟、错误率、成本 |
| **评估** | 任务完成率、输出质量、用户满意度 |
| **优化** | Prompt 调优、工具替换、模型升级 |
| **退役** | 数据清理、权限回收、日志归档 |

### 5.2 安全与治理

```
关键考量：
✅ 身份认证 — 每个 Agent 有独立身份和访问凭证
✅ 权限控制 — 最小权限原则，按需授权
✅ 操作审计 — 所有工具调用和行为记录在案
✅ 内容过滤 — 输入输出内容合规检查
✅ 速率限制 — 防止资源滥用和成本失控
✅ 回滚机制 — 关键操作可撤销
✅ 隔离执行 — Agent 之间互不影响（sandbox/worktree）
✅ 人类审批 — 高风险操作需人类确认
```

### 5.3 评估指标体系

#### 效率指标
- **Token 消耗**：单次任务 Token 用量（含上下文窗口利用率）
- **任务耗时**：从目标到完成的端到端时间
- **步骤数**：完成任务所需的 tool-call 步数
- **工具调用成功率**：工具调用无异常返回的比例

#### 质量指标
- **任务完成率**：成功达成目标的任务占比
- **首次正确率**：无需重试即成功的比例
- **用户满意度**：用户评分/反馈
- **幻觉率**：输出中虚构信息的比例

#### 成本指标
- **单次任务成本**：Token费用 + 工具使用费 + 计算资源费
- **成本效率**：单位成本的产出价值
- **预算偏差**：实际花费 vs 预算的偏差率

### 5.4 Agent 仪表盘 (Dashboard) 设计要点

```
仪表盘应至少包含：

📊 实时概览
  - 活跃 Agent 数 / 总 Agent 数
  - 当前正在执行的任务数
  - 今日 Token 消耗总量

📈 趋势分析
  - 7天/30天 Token 用量趋势
  - 任务量变化趋势
  - 错误率变化趋势

🔍 详情视图
  - 单个 Agent 的执行日志
  - Tool call 链路追踪
  - 每步的输入/输出查看

💰 成本面板
  - 按模型/Agent/项目的费用分解
  - 预算使用进度
  - 异常消费告警

⚠️ 告警中心
  - Agent 执行失败告警
  - 权限越界告警
  - 成本超预算告警
  - 异常行为检测
```

---

## 六、Agent 设计最佳实践

### 6.1 Prompt 工程

```
1. 角色定义要清晰具体
   ❌ "你是一个有用的助手"
   ✅ "你是一个资深Python后端工程师，擅长Django和FastAPI..."

2. 目标表达可量化
   ❌ "分析一下这个代码库"
   ✅ "分析代码库，输出：1) 架构图 2) 依赖关系 3) 三个主要风险点"

3. 工具调用有约束
   ❌ 给Agent所有工具的权限
   ✅ 按任务分配最小必要工具集，避免越权或误用

4. 输出格式结构化
   使用 JSON Schema 或 Markdown 模板约束输出格式

5. 错误处理有预案
   在System Prompt中明确失败后的重试策略和降级方案
```

### 6.2 工具设计原则

```
✅ 单一职责 — 每个工具只做一件事
✅ 明确的输入输出 — 参数和返回值结构清晰
✅ 幂等性 — 对重试友好（GET/READ 天然幂等；CREATE 需去重设计）
✅ 错误标准化 — 统一的错误码和错误信息格式
✅ 有超时控制 — 防止工具调用无限等待
✅ 有速率限制 — 防止高频调用压垮下游
✅ 返回有限制 — 控制单次返回数据量，分页或摘要处理大数据
```

### 6.3 多Agent协作模式

#### Orchestrator-Worker（调度-执行）
```
Orchestrator 进行任务理解和拆分，分配给专业 Worker Agent。
适合：任务类型明确、可分解的场景。
```

#### Peer-to-Peer（平等协作）
```
Agent 之间平等对话，无中心管理者。
适合：需要多角度讨论、头脑风暴的场景。
```

#### Hierarchical（层级管控）
```
类似企业组织架构，上层审批下层决策。
适合：需要审批流程、安全检查的场景。
```

#### Debate/Competition（辩论/竞争）
```
多个 Agent 独立解决同一问题，由评判 Agent 选出最佳方案。
适合：对质量要求极高、无标准答案的场景。
```

### 6.4 上下文窗口管理

```
策略优先级：
1. 仅传递必要信息 — 不要让Agent携带无关历史
2. 使用摘要压缩 — 长对话定期摘要
3. 分阶段执行 — 复杂任务拆分为多个子Agent分别执行
4. 外部记忆检索 — 需要时从向量库获取，而非全量加载
5. 文件引用替代内容 — 传递文件路径而非文件内容
```

---

## 七、常见挑战与解决方案

| 挑战 | 现象 | 解决方案 |
|------|------|---------|
| **无限循环** | Agent 反复执行同一操作 | 设置最大步数限制、循环检测 + 强制跳出 |
| **幻觉工具** | 调用不存在的 API | 工具注册白名单，严格校验 |
| **上下文漂移** | 执行中逐渐偏离原始目标 | 周期性回顾目标，设置中间校验点 |
| **过度工具调用** | 简单问题调用大量不必要的工具 | 成本预算、工具调用计数限制 |
| **输出质量下降** | 长对话后期输出变差 | 摘要重启、分Agent执行 |
| **死锁** | 多Agent相互等待 | 超时机制、死锁检测 |
| **权限滥用** | Agent 执行了不应执行的操作 | 权限沙箱、人类审批门控 |
| **成本失控** | Token 消耗远超预期 | 预算告警、自动熔断 |

---

## 八、Agent 未来趋势

1. **Agentic Workflow 成为主流** — 从单Agent到多Agent工作流编排
2. **Agent与RPA融合** — AI Agent 替代传统规则机器人
3. **垂直领域Agent** — 医疗、法律、金融等专业Agent
4. **Edge Agent** — 端侧运行的轻量Agent
5. **Agent市场/生态** — Agent 的交易市场、插件生态
6. **自我进化Agent** — Agent 能通过反馈持续自我优化
7. **具身Agent** — 与机器人、IoT设备结合的物理世界Agent
8. **人机协同深化** — Agent 辅助而非替代人类决策

---

## 参考资源

### 论文
- ReAct: Synergizing Reasoning and Acting in Language Models (2022)
- Reflexion: Language Agents with Verbal Reinforcement Learning (2023)
- Tree of Thoughts: Deliberate Problem Solving with Large Language Models (2023)
- AutoGen: Enabling Next-Gen LLM Applications via Multi-Agent Conversation (2023)
- MetaGPT: Meta Programming for a Multi-Agent Collaborative Framework (2023)

### 开源项目
- [LangChain/LangGraph](https://github.com/langchain-ai/langgraph)
- [AutoGen (Microsoft)](https://github.com/microsoft/autogen)
- [CrewAI](https://github.com/crewAIInc/crewAI)
- [MetaGPT](https://github.com/geekan/MetaGPT)
- [Dify](https://github.com/langgenius/dify)

### 协议
- [MCP Specification](https://modelcontextprotocol.io)
- [A2A Protocol](https://github.com/google/A2A)
