-- ============================================================
-- Agent 管理系统 - 数据库建表脚本 (完整版)
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- 对齐前端 src/types/*.ts 所有字段
-- ============================================================

-- 设置客户端编码为 utf8mb4（解决 Windows 中文乱码）
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET CHARACTER_SET_CONNECTION = utf8mb4;

CREATE DATABASE IF NOT EXISTS agent_management
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE agent_management;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 工作空间表
-- ============================================================
CREATE TABLE IF NOT EXISTS `workspace` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '工作空间ID',
  `name` VARCHAR(100) NOT NULL COMMENT '工作空间名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `icon` VARCHAR(50) DEFAULT '🏢' COMMENT '图标',
  `owner_id` BIGINT NOT NULL COMMENT '创建者ID',
  `max_agents` INT NOT NULL DEFAULT 50 COMMENT '最大Agent数',
  `max_members` INT NOT NULL DEFAULT 20 COMMENT '最大成员数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  -- WorkspaceSettings 字段
  `default_model_provider` VARCHAR(50) DEFAULT NULL COMMENT '默认模型提供商',
  `session_retention_days` INT DEFAULT 30 COMMENT '会话保留天数',
  `auto_archive_days` INT DEFAULT 7 COMMENT '自动归档天数',
  `max_tokens_per_task` BIGINT DEFAULT 100000 COMMENT '单任务最大Token数',
  `language` VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作空间表';

-- ============================================================
-- 3. 工作空间成员表
-- ============================================================
CREATE TABLE IF NOT EXISTS `workspace_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT '角色: owner/admin/member',
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `last_active_at` DATETIME DEFAULT NULL COMMENT '最后活跃时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workspace_user` (`workspace_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作空间成员表';

-- ============================================================
-- 4. 角色表
-- ============================================================
CREATE TABLE IF NOT EXISTS `role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `workspace_id` BIGINT DEFAULT NULL COMMENT '工作空间ID(NULL=系统角色)',
  `name` VARCHAR(50) NOT NULL COMMENT '角色标识',
  `label` VARCHAR(50) DEFAULT NULL COMMENT '角色显示名',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `permissions` JSON DEFAULT NULL COMMENT '权限列表JSON',
  `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统角色',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ============================================================
-- 5. 用户角色关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `workspace_id` BIGINT DEFAULT NULL COMMENT '工作空间ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role_ws` (`user_id`, `role_id`, `workspace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ============================================================
-- 6. Agent 表
-- ============================================================
CREATE TABLE IF NOT EXISTS `agent` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Agent ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `name` VARCHAR(100) NOT NULL COMMENT '名称',
  `display_name` VARCHAR(100) DEFAULT NULL COMMENT '显示名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/testing/published/paused/archived',
  `tags` JSON DEFAULT NULL COMMENT '标签JSON数组',

  -- 模型配置
  `model_provider` VARCHAR(50) DEFAULT NULL COMMENT '模型提供商',
  `model_name` VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
  `temperature` DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度',
  `max_tokens` INT DEFAULT 4096 COMMENT '最大Token数',
  `top_p` DECIMAL(3,2) DEFAULT 1.00 COMMENT 'Top P',

  -- 提示词配置
  `system_prompt` TEXT DEFAULT NULL COMMENT '系统提示词',
  `prompt_variables` JSON DEFAULT NULL COMMENT '提示词变量JSON',

  -- 记忆配置
  `memory_strategy` VARCHAR(20) DEFAULT 'buffer' COMMENT '记忆策略: buffer/summary/vector/sliding_window/full',
  `working_window` INT DEFAULT 10 COMMENT '工作窗口(消息轮数)',
  `long_term_enabled` TINYINT DEFAULT 0 COMMENT '是否启用长期记忆',
  `knowledge_base_ids` JSON DEFAULT NULL COMMENT '绑定知识库ID数组JSON',

  -- 执行配置
  `execution_mode` VARCHAR(20) DEFAULT 'auto' COMMENT '执行模式: auto/semi/manual',
  `max_iterations` INT DEFAULT 10 COMMENT '最大迭代次数',
  `timeout` INT DEFAULT 60000 COMMENT '超时时间(ms)',
  `reflection_enabled` TINYINT DEFAULT 1 COMMENT '是否启用反思',
  `reflection_depth` INT DEFAULT 1 COMMENT '反思深度',
  `output_schema` JSON DEFAULT NULL COMMENT '输出格式Schema JSON',

  -- 统计(冗余字段，定期同步)
  `total_sessions` BIGINT DEFAULT 0 COMMENT '总会话数',
  `total_messages` BIGINT DEFAULT 0 COMMENT '总消息数',
  `total_tokens` BIGINT DEFAULT 0 COMMENT '总Token数',
  `total_cost` DECIMAL(12,6) DEFAULT 0.000000 COMMENT '总费用(美元)',
  `success_rate` DECIMAL(5,2) DEFAULT 100.00 COMMENT '成功率(%)',
  `avg_latency_ms` INT DEFAULT 0 COMMENT '平均延迟(ms)',

  `created_by` BIGINT NOT NULL COMMENT '创建者ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent表';

-- ============================================================
-- 7. Agent 提示词版本表
-- ============================================================
CREATE TABLE IF NOT EXISTS `agent_prompt_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '版本ID',
  `agent_id` BIGINT NOT NULL COMMENT 'Agent ID',
  `version_number` VARCHAR(20) NOT NULL COMMENT '版本号',
  `system_prompt` TEXT NOT NULL COMMENT '系统提示词内容',
  `prompt_variables` JSON DEFAULT NULL COMMENT '变量定义JSON',
  `change_note` VARCHAR(500) DEFAULT NULL COMMENT '变更说明',
  `changed_by` BIGINT NOT NULL COMMENT '创建者ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_id` (`agent_id`),
  UNIQUE KEY `uk_agent_version` (`agent_id`, `version_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent提示词版本表';

-- ============================================================
-- 8. Agent 工具绑定表
-- ============================================================
CREATE TABLE IF NOT EXISTS `agent_tool_binding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `agent_id` BIGINT NOT NULL COMMENT 'Agent ID',
  `tool_id` BIGINT NOT NULL COMMENT '工具ID',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `config` JSON DEFAULT NULL COMMENT '绑定配置JSON',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_tool` (`agent_id`, `tool_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent工具绑定表';

-- ============================================================
-- 9. 工具表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tool` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '工具ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `name` VARCHAR(100) NOT NULL COMMENT '工具标识名',
  `display_name` VARCHAR(100) DEFAULT NULL COMMENT '工具显示名',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `category` VARCHAR(50) NOT NULL DEFAULT 'custom' COMMENT '分类: search/compute/operate/perceive/notify/custom',
  `icon` VARCHAR(500) DEFAULT NULL COMMENT '图标URL',
  `version` VARCHAR(20) DEFAULT '1.0.0' COMMENT '版本号',
  `type` VARCHAR(20) NOT NULL DEFAULT 'api' COMMENT '类型: api/mcp/builtin',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/inactive/error',

  -- 端点配置
  `endpoint_url` VARCHAR(500) DEFAULT NULL COMMENT '端点URL',
  `method` VARCHAR(10) DEFAULT 'POST' COMMENT 'HTTP方法',
  `headers` JSON DEFAULT NULL COMMENT '请求头JSON',
  `endpoint_timeout` INT DEFAULT 30000 COMMENT '端点超时时间(ms)',

  -- 参数与响应
  `parameters` JSON DEFAULT NULL COMMENT '参数定义JSON',
  `response_mapping` VARCHAR(500) DEFAULT NULL COMMENT '响应映射表达式',

  -- 认证
  `credential_ref` VARCHAR(200) DEFAULT NULL COMMENT '凭证引用标识',
  `auth_type` VARCHAR(20) DEFAULT 'none' COMMENT '认证类型: none/api_key/bearer/oauth',
  `auth_config` JSON DEFAULT NULL COMMENT '认证配置JSON',

  -- 重试
  `retry_on_fail` TINYINT DEFAULT 1 COMMENT '失败是否重试',
  `max_retries` INT DEFAULT 3 COMMENT '最大重试次数',

  -- MCP 配置
  `mcp_server_name` VARCHAR(100) DEFAULT NULL COMMENT 'MCP服务名',
  `mcp_transport` VARCHAR(20) DEFAULT NULL COMMENT 'MCP传输方式: stdio/sse',
  `mcp_command` VARCHAR(500) DEFAULT NULL COMMENT 'MCP启动命令',
  `mcp_args` JSON DEFAULT NULL COMMENT 'MCP命令参数JSON',
  `mcp_env_vars` JSON DEFAULT NULL COMMENT 'MCP环境变量JSON',

  -- 统计
  `bind_agent_count` INT DEFAULT 0 COMMENT '绑定Agent数',
  `total_calls` BIGINT DEFAULT 0 COMMENT '总调用次数',
  `success_rate` DECIMAL(5,2) DEFAULT 100.00 COMMENT '成功率(%)',
  `avg_latency_ms` INT DEFAULT 0 COMMENT '平均延迟(ms)',
  `p99_latency_ms` INT DEFAULT 0 COMMENT 'P99延迟(ms)',

  `created_by` BIGINT NOT NULL COMMENT '创建者ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具表';

-- ============================================================
-- 10. 工具调用记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `tool_call_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `tool_id` BIGINT NOT NULL COMMENT '工具ID',
  `agent_id` BIGINT DEFAULT NULL COMMENT 'Agent ID',
  `session_id` BIGINT DEFAULT NULL COMMENT '会话ID',
  `step_id` BIGINT DEFAULT NULL COMMENT '执行步骤ID',
  `params` JSON DEFAULT NULL COMMENT '调用参数JSON',
  `result_summary` VARCHAR(500) DEFAULT NULL COMMENT '结果摘要',
  `success` TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功',
  `latency_ms` INT DEFAULT NULL COMMENT '延迟(ms)',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tool_id` (`tool_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具调用记录表';

-- ============================================================
-- 11. 会话表
-- ============================================================
CREATE TABLE IF NOT EXISTS `session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `agent_id` BIGINT NOT NULL COMMENT 'Agent ID',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/completed/stopped/error',
  `execution_mode` VARCHAR(20) DEFAULT 'auto' COMMENT '执行模式: auto/step_by_step/plan_only',
  `variables` JSON DEFAULT NULL COMMENT '会话变量JSON(key-value)',
  `total_tokens` BIGINT DEFAULT 0 COMMENT '总Token数',
  `total_cost` DECIMAL(10,6) DEFAULT 0.000000 COMMENT '总费用(美元)',
  `latency` INT DEFAULT 0 COMMENT '总延迟(ms)',
  `message_count` INT DEFAULT 0 COMMENT '消息数',
  `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
  `ended_at` DATETIME DEFAULT NULL COMMENT '结束时间',
  `created_by` BIGINT NOT NULL COMMENT '创建者ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- ============================================================
-- 12. 消息表
-- ============================================================
CREATE TABLE IF NOT EXISTS `message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色: user/assistant/system/tool',
  `content` TEXT DEFAULT NULL COMMENT '消息内容',
  `mode` VARCHAR(20) DEFAULT NULL COMMENT '发送时执行模式: auto/step_by_step/plan_only',
  `attachments` JSON DEFAULT NULL COMMENT '附件路径JSON数组',
  `token_input` BIGINT DEFAULT 0 COMMENT '输入Token数',
  `token_output` BIGINT DEFAULT 0 COMMENT '输出Token数',
  `token_total` BIGINT DEFAULT 0 COMMENT '总Token数',
  `token_cost` DECIMAL(10,6) DEFAULT 0.000000 COMMENT '费用(美元)',
  `latency` INT DEFAULT NULL COMMENT '延迟(ms)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ============================================================
-- 13. 执行步骤表
-- ============================================================
CREATE TABLE IF NOT EXISTS `execution_step` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '步骤ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `message_id` BIGINT DEFAULT NULL COMMENT '关联消息ID',
  `sequence` INT NOT NULL DEFAULT 0 COMMENT '步骤序号',
  `step_type` VARCHAR(20) NOT NULL COMMENT '类型: thinking/tool_call/tool_result/reflection/message',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '步骤标题',
  `content` TEXT DEFAULT NULL COMMENT '步骤内容',
  `tool_name` VARCHAR(100) DEFAULT NULL COMMENT '工具名称',
  `tool_icon` VARCHAR(500) DEFAULT NULL COMMENT '工具图标',
  `tool_input` JSON DEFAULT NULL COMMENT '工具输入JSON(对应request)',
  `tool_output` JSON DEFAULT NULL COMMENT '工具输出JSON(对应response)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/running/success/error/skipped',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `duration_ms` INT DEFAULT NULL COMMENT '耗时(ms)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行步骤表';

-- ============================================================
-- 14. 错误日志表 (监控模块)
-- ============================================================
CREATE TABLE IF NOT EXISTS `error_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '错误ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `agent_id` BIGINT DEFAULT NULL COMMENT 'Agent ID',
  `agent_name` VARCHAR(100) DEFAULT NULL COMMENT 'Agent名称(冗余)',
  `session_id` BIGINT DEFAULT NULL COMMENT '会话ID',
  `step_id` BIGINT DEFAULT NULL COMMENT '执行步骤ID',
  `error_type` VARCHAR(50) NOT NULL COMMENT '错误类型: tool_timeout/db_timeout/api_error/llm_error/unknown',
  `error_message` TEXT NOT NULL COMMENT '错误信息',
  `stack_trace` TEXT DEFAULT NULL COMMENT '堆栈跟踪',
  `occurred_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_error_type` (`error_type`),
  KEY `idx_occurred_at` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错误日志表';

-- ============================================================
-- 15. 告警规则表
-- ============================================================
CREATE TABLE IF NOT EXISTS `alert_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `metric` VARCHAR(30) NOT NULL COMMENT '指标: success_rate/p99_latency/daily_tokens/error_rate/cost',
  `target_type` VARCHAR(20) DEFAULT 'workspace' COMMENT '目标类型: workspace/agent',
  `target_id` BIGINT DEFAULT NULL COMMENT '目标ID(NULL=整个工作空间)',
  `condition` VARCHAR(10) NOT NULL DEFAULT 'gt' COMMENT '条件: lt/gt/lte/gte',
  `threshold` DECIMAL(12,2) NOT NULL COMMENT '阈值',
  `duration` VARCHAR(20) DEFAULT '0m' COMMENT '持续时间: 0m/5m/1h/1d',
  `severity` VARCHAR(20) NOT NULL DEFAULT 'warning' COMMENT '级别: info/warning/critical',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `notify_channels` JSON DEFAULT NULL COMMENT '通知渠道JSON(["feishu","email","webhook"])',
  `created_by` BIGINT NOT NULL COMMENT '创建者ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警规则表';

-- ============================================================
-- 16. 告警记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `alert_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `rule_id` BIGINT NOT NULL COMMENT '规则ID',
  `rule_name` VARCHAR(100) DEFAULT NULL COMMENT '规则名称(冗余)',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `agent_id` BIGINT DEFAULT NULL COMMENT '关联Agent ID',
  `severity` VARCHAR(20) NOT NULL COMMENT '级别',
  `message` VARCHAR(500) NOT NULL COMMENT '告警信息',
  `current_value` VARCHAR(100) DEFAULT NULL COMMENT '当前值',
  `threshold_value` VARCHAR(100) DEFAULT NULL COMMENT '阈值',
  `status` VARCHAR(20) NOT NULL DEFAULT 'triggered' COMMENT '状态: triggered/resolved/ignored',
  `triggered_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
  `resolved_at` DATETIME DEFAULT NULL COMMENT '解决时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_status` (`status`),
  KEY `idx_triggered_at` (`triggered_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警记录表';

-- ============================================================
-- 17. 费用记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `cost_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `agent_id` BIGINT DEFAULT NULL COMMENT 'Agent ID',
  `agent_name` VARCHAR(100) DEFAULT NULL COMMENT 'Agent名称(冗余)',
  `session_id` BIGINT DEFAULT NULL COMMENT '会话ID',
  `model_provider` VARCHAR(50) DEFAULT NULL COMMENT '模型提供商',
  `model_name` VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
  `token_input` BIGINT DEFAULT 0 COMMENT '输入Token数',
  `token_output` BIGINT DEFAULT 0 COMMENT '输出Token数',
  `total_tokens` BIGINT DEFAULT 0 COMMENT '总Token数',
  `cost` DECIMAL(10,6) NOT NULL DEFAULT 0.000000 COMMENT '费用(美元)',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '用户名(冗余)',
  `recorded_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_model_name` (`model_name`),
  KEY `idx_recorded_at` (`recorded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用记录表';

-- ============================================================
-- 18. 预算配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `budget` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预算ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `name` VARCHAR(100) NOT NULL COMMENT '预算名称',
  `scope` VARCHAR(20) NOT NULL DEFAULT 'workspace' COMMENT '范围: workspace/user/agent',
  `scope_id` BIGINT DEFAULT NULL COMMENT '范围ID(用户ID或AgentID)',
  `period` VARCHAR(20) NOT NULL DEFAULT 'monthly' COMMENT '周期: daily/monthly',
  `limit_amount` DECIMAL(10,2) NOT NULL COMMENT '预算限额(美元)',
  `current_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '当前已用',
  `warn_percent` INT DEFAULT 80 COMMENT '告警阈值(百分比)',
  `meltdown_enabled` TINYINT DEFAULT 0 COMMENT '超支熔断是否启用',
  `notify_channels` JSON DEFAULT NULL COMMENT '通知渠道JSON',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算配置表';

-- ============================================================
-- 19. 审计日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `user_id` BIGINT NOT NULL COMMENT '操作者ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '操作者用户名(冗余)',
  `action` VARCHAR(50) NOT NULL COMMENT '操作类型: agent.create/agent.update/agent.delete/tool.register...',
  `action_label` VARCHAR(50) DEFAULT NULL COMMENT '操作显示名',
  `resource_type` VARCHAR(50) NOT NULL COMMENT '资源类型: agent/tool/session/workspace/member',
  `resource_id` BIGINT DEFAULT NULL COMMENT '资源ID',
  `resource_name` VARCHAR(100) DEFAULT NULL COMMENT '资源名称',
  `detail` VARCHAR(500) DEFAULT NULL COMMENT '详情',
  `result` VARCHAR(20) DEFAULT 'success' COMMENT '结果: success/failure',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'UserAgent',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_action` (`action`),
  KEY `idx_resource_type` (`resource_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- ============================================================
-- 20. 审批规则表
-- ============================================================
CREATE TABLE IF NOT EXISTS `approval_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `resource_type` VARCHAR(50) NOT NULL COMMENT '资源类型: agent/tool',
  `trigger_action` VARCHAR(50) NOT NULL COMMENT '触发操作: publish/register/delete',
  `trigger_condition` VARCHAR(500) DEFAULT NULL COMMENT '触发条件JSON',
  `approver_role` VARCHAR(50) NOT NULL COMMENT '审批角色',
  `required_approvals` INT DEFAULT 1 COMMENT '所需审批人数',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批规则表';

-- ============================================================
-- 21. 审批记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `approval` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审批ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `rule_id` BIGINT DEFAULT NULL COMMENT '规则ID',
  `rule_name` VARCHAR(100) DEFAULT NULL COMMENT '规则名称(冗余)',
  `resource_type` VARCHAR(50) NOT NULL COMMENT '资源类型',
  `resource_id` BIGINT NOT NULL COMMENT '资源ID',
  `resource_name` VARCHAR(100) DEFAULT NULL COMMENT '资源名称',
  `action` VARCHAR(50) NOT NULL COMMENT '操作',
  `detail` VARCHAR(500) DEFAULT NULL COMMENT '详情',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
  `applicant_name` VARCHAR(50) DEFAULT NULL COMMENT '申请人名称(冗余)',
  `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID',
  `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '审批人名称(冗余)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/approved/rejected',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `resolved_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_status` (`status`),
  KEY `idx_applicant_id` (`applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录表';

-- ============================================================
-- 22. API Key 表
-- ============================================================
CREATE TABLE IF NOT EXISTS `api_key` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Key ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `provider` VARCHAR(50) DEFAULT NULL COMMENT '提供商: openai/anthropic/...',
  `name` VARCHAR(100) NOT NULL COMMENT '名称',
  `key_prefix` VARCHAR(20) NOT NULL COMMENT 'Key前缀(用于显示)',
  `key_hash` VARCHAR(255) NOT NULL COMMENT 'Key哈希值',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认Key',
  `scopes` JSON DEFAULT NULL COMMENT '权限范围JSON',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/revoked',
  `last_used_at` DATETIME DEFAULT NULL COMMENT '最后使用时间',
  `expires_at` DATETIME DEFAULT NULL COMMENT '过期时间',
  `created_by` BIGINT NOT NULL COMMENT '创建者ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_provider` (`provider`),
  KEY `idx_key_prefix` (`key_prefix`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API Key表';

-- ============================================================
-- 23. 工作流表
-- ============================================================
CREATE TABLE IF NOT EXISTS `workflow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '工作流ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `name` VARCHAR(100) NOT NULL COMMENT '名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/active/archived',
  `trigger_type` VARCHAR(20) DEFAULT 'manual' COMMENT '触发方式: manual/scheduled/event',
  `trigger_config` JSON DEFAULT NULL COMMENT '触发配置JSON',
  `created_by` BIGINT NOT NULL COMMENT '创建者ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流表';

-- ============================================================
-- 24. 工作流节点表
-- ============================================================
CREATE TABLE IF NOT EXISTS `workflow_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  `workflow_id` BIGINT NOT NULL COMMENT '工作流ID',
  `node_id` VARCHAR(50) NOT NULL COMMENT '节点唯一标识(前端生成)',
  `type` VARCHAR(30) NOT NULL COMMENT '节点类型: start/agent/tool/condition/end',
  `label` VARCHAR(100) NOT NULL COMMENT '节点标签',
  `agent_id` BIGINT DEFAULT NULL COMMENT '关联Agent ID',
  `tool_id` BIGINT DEFAULT NULL COMMENT '关联工具ID',
  `config` JSON DEFAULT NULL COMMENT '节点配置JSON',
  `position_x` DOUBLE DEFAULT 0 COMMENT 'X坐标',
  `position_y` DOUBLE DEFAULT 0 COMMENT 'Y坐标',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_id` (`workflow_id`),
  UNIQUE KEY `uk_workflow_node` (`workflow_id`, `node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流节点表';

-- ============================================================
-- 25. 工作流边表(节点连线)
-- ============================================================
CREATE TABLE IF NOT EXISTS `workflow_edge` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '边ID',
  `workflow_id` BIGINT NOT NULL COMMENT '工作流ID',
  `edge_id` VARCHAR(50) NOT NULL COMMENT '边唯一标识(前端生成)',
  `source_node_id` VARCHAR(50) NOT NULL COMMENT '源节点ID',
  `target_node_id` VARCHAR(50) NOT NULL COMMENT '目标节点ID',
  `label` VARCHAR(100) DEFAULT NULL COMMENT '边标签',
  `condition` JSON DEFAULT NULL COMMENT '条件表达式JSON',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_id` (`workflow_id`),
  UNIQUE KEY `uk_workflow_edge` (`workflow_id`, `edge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流边表';

-- ============================================================
-- 26. 工作流运行记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `workflow_run` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '运行ID',
  `workflow_id` BIGINT NOT NULL COMMENT '工作流ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/running/completed/failed',
  `input` JSON DEFAULT NULL COMMENT '输入JSON',
  `output` JSON DEFAULT NULL COMMENT '输出JSON',
  `node_results` JSON DEFAULT NULL COMMENT '各节点结果JSON',
  `error` TEXT DEFAULT NULL COMMENT '错误信息',
  `total_tokens` BIGINT DEFAULT 0 COMMENT '总Token数',
  `total_cost` DECIMAL(10,6) DEFAULT 0.000000 COMMENT '总费用',
  `duration` INT DEFAULT NULL COMMENT '总耗时(ms)',
  `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
  `ended_at` DATETIME DEFAULT NULL COMMENT '结束时间',
  `triggered_by` BIGINT NOT NULL COMMENT '触发者ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流运行记录表';

-- ============================================================
-- 27. 知识库表
-- ============================================================
CREATE TABLE IF NOT EXISTS `knowledge_base` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `name` VARCHAR(100) NOT NULL COMMENT '名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `type` VARCHAR(20) NOT NULL DEFAULT 'vector' COMMENT '类型: vector/keyword/hybrid',
  `embedding_model` VARCHAR(100) DEFAULT NULL COMMENT '嵌入模型',
  `document_count` INT DEFAULT 0 COMMENT '文档数',
  `total_tokens` BIGINT DEFAULT 0 COMMENT '总Token数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/building/error',
  `config` JSON DEFAULT NULL COMMENT '配置JSON(chunk_size/overlap等)',
  `created_by` BIGINT NOT NULL COMMENT '创建者ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- ============================================================
-- 28. 文档表
-- ============================================================
CREATE TABLE IF NOT EXISTS `document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `knowledge_base_id` BIGINT NOT NULL COMMENT '知识库ID',
  `name` VARCHAR(200) NOT NULL COMMENT '文件名',
  `file_type` VARCHAR(20) NOT NULL COMMENT '文件类型: pdf/txt/md/docx/csv',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小(bytes)',
  `file_url` VARCHAR(500) DEFAULT NULL COMMENT '文件存储URL',
  `chunk_count` INT DEFAULT 0 COMMENT '分块数',
  `total_tokens` BIGINT DEFAULT 0 COMMENT '总Token数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/processing/completed/failed',
  `error` TEXT DEFAULT NULL COMMENT '错误信息',
  `metadata` JSON DEFAULT NULL COMMENT '元数据JSON',
  `uploaded_by` BIGINT NOT NULL COMMENT '上传者ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_base_id` (`knowledge_base_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- ============================================================
-- 29. 活动日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `activity_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `workspace_id` BIGINT NOT NULL COMMENT '工作空间ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '用户名(冗余)',
  `type` VARCHAR(50) NOT NULL COMMENT '类型: agent.created/tool.create/member.invite/session.start...',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `related_id` BIGINT DEFAULT NULL COMMENT '关联资源ID',
  `related_type` VARCHAR(50) DEFAULT NULL COMMENT '关联资源类型',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_workspace_id` (`workspace_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动日志表';

-- ============================================================
-- 30. 模型定价参考表(可选)
-- ============================================================
CREATE TABLE IF NOT EXISTS `model_pricing` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `provider` VARCHAR(50) NOT NULL COMMENT '提供商',
  `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
  `display_name` VARCHAR(100) DEFAULT NULL COMMENT '显示名称',
  `max_tokens` INT DEFAULT 4096 COMMENT '最大Token数',
  `input_price_per_1k` DECIMAL(10,6) DEFAULT 0.000000 COMMENT '输入价格(每1K token)',
  `output_price_per_1k` DECIMAL(10,6) DEFAULT 0.000000 COMMENT '输出价格(每1K token)',
  `enabled` TINYINT DEFAULT 1 COMMENT '是否可用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_model` (`provider`, `model_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型定价参考表';


-- ============================================================
-- 初始数据
-- ============================================================

-- 默认用户 (密码: admin123, BCrypt)
INSERT INTO `user` (`id`, `username`, `password`, `email`, `nickname`, `status`) VALUES
(1, 'admin', '$2a$10$h9U/iaAWHby8rZTpCr5Zgu5AwuDlk.wt0ODR4G.zQHirOxVdSLf9y', 'admin@example.com', '管理员', 1),
(2, 'dev1', '$2a$10$h9U/iaAWHby8rZTpCr5Zgu5AwuDlk.wt0ODR4G.zQHirOxVdSLf9y', 'dev1@example.com', '开发者1', 1),
(3, 'dev2', '$2a$10$h9U/iaAWHby8rZTpCr5Zgu5AwuDlk.wt0ODR4G.zQHirOxVdSLf9y', 'dev2@example.com', '开发者2', 1);

-- 默认工作空间
INSERT INTO `workspace` (`id`, `name`, `description`, `icon`, `owner_id`, `default_model_provider`, `language`) VALUES
(1, '默认工作空间', '系统默认工作空间', '🏢', 1, 'openai', 'zh-CN');

-- 工作空间成员
INSERT INTO `workspace_member` (`workspace_id`, `user_id`, `role`) VALUES
(1, 1, 'owner'),
(1, 2, 'admin'),
(1, 3, 'member');

-- 系统角色
INSERT INTO `role` (`id`, `name`, `label`, `description`, `permissions`, `is_system`) VALUES
(1, 'super_admin', '超级管理员', '拥有所有权限', '["*"]', 1),
(2, 'admin', '管理员', '管理工作空间和成员', '["workspace:*","agent:*","tool:*","session:*","monitor:*","cost:read"]', 1),
(3, 'developer', '开发者', '开发和调试Agent', '["agent:*","tool:read","session:*","monitor:read"]', 1),
(4, 'observer', '观察者', '只读权限', '["agent:read","tool:read","session:read","monitor:read"]', 1);

-- 用户角色关联
INSERT INTO `user_role` (`user_id`, `role_id`, `workspace_id`) VALUES
(1, 1, 1),
(2, 3, 1),
(3, 4, 1);

-- 默认告警规则
INSERT INTO `alert_rule` (`workspace_id`, `name`, `metric`, `target_type`, `condition`, `threshold`, `duration`, `severity`, `notify_channels`, `created_by`) VALUES
(1, 'Token用量过高', 'daily_tokens', 'workspace', 'gt', 100000, '1d', 'warning', '["webhook"]', 1),
(1, '错误率超限', 'error_rate', 'workspace', 'gt', 10, '1h', 'critical', '["webhook","email"]', 1);

-- 默认审批规则
INSERT INTO `approval_rule` (`workspace_id`, `name`, `description`, `resource_type`, `trigger_action`, `approver_role`, `required_approvals`) VALUES
(1, 'Agent发布审批', '发布Agent到生产环境需要审批', 'agent', 'publish', 'admin', 1),
(1, '工具注册审批', '注册新工具需要审批', 'tool', 'register', 'admin', 1);

-- 默认模型定价
INSERT INTO `model_pricing` (`provider`, `model_name`, `display_name`, `max_tokens`, `input_price_per_1k`, `output_price_per_1k`) VALUES
('openai', 'gpt-4o', 'GPT-4o', 128000, 0.002500, 0.010000),
('openai', 'gpt-4o-mini', 'GPT-4o Mini', 128000, 0.000150, 0.000600),
('openai', 'gpt-3.5-turbo', 'GPT-3.5 Turbo', 16385, 0.000500, 0.001500),
('anthropic', 'claude-sonnet-4-6', 'Claude Sonnet 4.6', 200000, 0.003000, 0.015000),
('anthropic', 'claude-haiku-4-5', 'Claude Haiku 4.5', 200000, 0.000800, 0.004000),
('deepseek', 'deepseek-chat', 'DeepSeek Chat', 65536, 0.000140, 0.000280),
('deepseek', 'deepseek-reasoner', 'DeepSeek Reasoner', 65536, 0.000550, 0.002190);

-- ============================================================
-- 完成
-- ============================================================
SELECT '✅ 数据库初始化完成！' AS message;
SELECT COUNT(*) AS '总表数' FROM information_schema.tables WHERE table_schema = 'agent_management';
