-- Agent 表添加 token 价格配置字段
ALTER TABLE agent ADD COLUMN input_price_per_million DECIMAL(10,4) DEFAULT NULL COMMENT '输入token单价(美元/百万token)';
ALTER TABLE agent ADD COLUMN cached_input_price_per_million DECIMAL(10,4) DEFAULT NULL COMMENT '缓存命中输入token单价(美元/百万token)';
ALTER TABLE agent ADD COLUMN output_price_per_million DECIMAL(10,4) DEFAULT NULL COMMENT '输出token单价(美元/百万token)';

-- 确保 cost_record 表存在
CREATE TABLE IF NOT EXISTS cost_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL COMMENT '工作空间ID',
    agent_id BIGINT DEFAULT NULL COMMENT 'Agent ID',
    agent_name VARCHAR(100) DEFAULT NULL COMMENT 'Agent名称(冗余)',
    session_id BIGINT DEFAULT NULL COMMENT '会话ID',
    model_provider VARCHAR(50) DEFAULT NULL COMMENT '模型供应商',
    model_name VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
    token_input BIGINT DEFAULT 0 COMMENT '输入token数',
    token_output BIGINT DEFAULT 0 COMMENT '输出token数',
    total_tokens BIGINT DEFAULT 0 COMMENT '总token数',
    cost DECIMAL(12,6) DEFAULT 0 COMMENT '费用(美元)',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    user_name VARCHAR(50) DEFAULT NULL COMMENT '用户名(冗余)',
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_agent_id (agent_id),
    INDEX idx_recorded_at (recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用记录表';
