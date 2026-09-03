-- 成功率口径统一修复：
-- 1) 历史脏数据清理：无会话/无调用的记录，成功率置 NULL（此前默认 100，前端格式化后显示 10000.0%）
-- 2) 列默认值改为 NULL：新建记录不再预填虚假的 100%
UPDATE agent SET success_rate = NULL WHERE total_sessions IS NULL OR total_sessions = 0;
UPDATE tool SET success_rate = NULL WHERE total_calls IS NULL OR total_calls = 0;

ALTER TABLE agent MODIFY COLUMN success_rate DECIMAL(5,2) NULL DEFAULT NULL
  COMMENT '成功率(%)，无会话数据为 NULL';
ALTER TABLE tool MODIFY COLUMN success_rate DECIMAL(5,2) NULL DEFAULT NULL
  COMMENT '成功率(%)，无调用数据为 NULL';
