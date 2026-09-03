-- ============================================================
-- V5: schema 同步脚本
-- 适用对象：按旧版 docs/database-schema.sql（30 张表版本）建库的环境。
-- 新库请直接执行更新后的 docs/database-schema.sql（已含全部 31 张表），
-- 无需本脚本。
-- 注意：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，
--       执行时若报 Duplicate column 错误，说明该列已存在，跳过该行即可。
-- ============================================================

USE agent_management;

-- 1. agent 表：AI 连接配置（来自 V2，旧库可能未执行）
ALTER TABLE `agent` ADD COLUMN `ai_base_url` VARCHAR(500) DEFAULT NULL COMMENT 'AI API Base URL(如 https://api.openai.com/v1)';
ALTER TABLE `agent` ADD COLUMN `ai_api_key` VARCHAR(500) DEFAULT NULL COMMENT 'AI API Key';
ALTER TABLE `agent` ADD COLUMN `ai_model` VARCHAR(100) DEFAULT NULL COMMENT 'AI 模型名称(如 gpt-4o, deepseek-chat)';

-- 2. agent 表：Token 价格配置（来自 V3，对话计费使用）
ALTER TABLE `agent` ADD COLUMN `input_price_per_million` DECIMAL(10,4) DEFAULT NULL COMMENT '输入token单价(美元/百万token)';
ALTER TABLE `agent` ADD COLUMN `cached_input_price_per_million` DECIMAL(10,4) DEFAULT NULL COMMENT '缓存命中输入token单价(美元/百万token)';
ALTER TABLE `agent` ADD COLUMN `output_price_per_million` DECIMAL(10,4) DEFAULT NULL COMMENT '输出token单价(美元/百万token)';

-- 3. budget 表：创建者（来自 V4，账户级数据隔离）
ALTER TABLE `budget` ADD COLUMN `created_by` BIGINT DEFAULT NULL COMMENT '创建者用户ID' AFTER `enabled`;
ALTER TABLE `budget` ADD INDEX `idx_created_by` (`created_by`);

-- 4. 文档分块表（RAG 检索核心表，来自 V2；IF NOT EXISTS 幂等可重复执行）
CREATE TABLE IF NOT EXISTS `document_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分块ID',
  `document_id` BIGINT NOT NULL COMMENT '文档ID',
  `knowledge_base_id` BIGINT NOT NULL COMMENT '知识库ID',
  `chunk_index` INT NOT NULL COMMENT '块序号(从0开始)',
  `content` TEXT NOT NULL COMMENT '文本内容',
  `token_count` INT DEFAULT 0 COMMENT 'Token数(估算)',
  `embedding` JSON DEFAULT NULL COMMENT '向量JSON数组[float,float,...]',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_kb_id` (`knowledge_base_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分块表';
