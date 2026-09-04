-- 空间设置改版：Agent 执行环境策略（对标 Claude Code 的项目级权限模型）
-- 1) 删除从未被业务消费的死列（entity/form/VO 同步收窄）
-- 2) 新增 3 个真实生效的策略列

ALTER TABLE workspace
  DROP COLUMN default_model_provider,
  DROP COLUMN language,
  DROP COLUMN session_retention_days,
  DROP COLUMN auto_archive_days,
  DROP COLUMN max_tokens_per_task,
  DROP COLUMN max_agents,
  DROP COLUMN max_members,
  ADD COLUMN shared_workdir TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '共享工作目录：0-每会话独立沙箱(ws-{id}/session-{id})，1-空间内会话共享文件区(ws-{id})',
  ADD COLUMN allow_outside_sandbox TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '允许沙箱外运行总闸：0-空间内禁止授权逃逸沙箱，1-允许（成员才可在会话中开启沙箱外运行）',
  ADD COLUMN disabled_tools VARCHAR(500) NULL DEFAULT NULL
    COMMENT '空间级禁用的内置工具名，逗号分隔（如 run_command,web_fetch），NULL/空=全部允许';
