-- AI 配置 + 文档分块向量表
-- 如果 agent 表的 ai_base_url/ai_api_key/ai_model 列已存在，则跳过 ALTER TABLE 部分

-- 1. agent 表新增 AI 配置字段（如果列不存在才执行）
-- 注意：MySQL 不支持 ADD COLUMN IF NOT EXISTS，如果报 Duplicate column 错误说明已存在，跳过即可
-- ALTER TABLE `agent` ADD COLUMN `ai_base_url` VARCHAR(500) DEFAULT NULL COMMENT 'AI API Base URL';
-- ALTER TABLE `agent` ADD COLUMN `ai_api_key` VARCHAR(500) DEFAULT NULL COMMENT 'AI API Key';
-- ALTER TABLE `agent` ADD COLUMN `ai_model` VARCHAR(100) DEFAULT NULL COMMENT 'AI 模型名称';

-- 2. 文档分块表（存储分块文本 + 向量）
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
