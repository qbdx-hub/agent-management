-- ============================================================
-- V6: 移动端支持（用户偏好 + API 密钥）
-- 配套《docs/移动端接入方案.md》M2 里程碑。
-- 无 Flyway，需手动执行；重复执行遇 Duplicate column/table 跳过即可。
-- ============================================================

USE agent_management;

-- 1. user 表：用户级偏好（移动端「模型偏好」「通知设置」持久化，JSON 列）
ALTER TABLE `user` ADD COLUMN `preferences` JSON DEFAULT NULL COMMENT '用户偏好JSON(默认模型/温度/最大输出/回复风格/通知开关)';

-- 2. API 密钥表（移动端「API 密钥管理」；只存 SHA-256 摘要，明文不落库，限 5 个/用户）
-- 注：库中曾存在早期迭代的旧版 api_key 表（provider/key_prefix 结构，已废弃无代码引用），
-- 用 DROP 保证本脚本在任何环境上都能确定性地得到下述结构。
DROP TABLE IF EXISTS `api_key`;
CREATE TABLE IF NOT EXISTS `api_key` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '密钥ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `name` VARCHAR(64) NOT NULL COMMENT '密钥名称',
  `key_hash` CHAR(64) NOT NULL COMMENT 'SHA-256摘要(hex)，明文不落库',
  `mask` VARCHAR(32) NOT NULL COMMENT '展示掩码(如 sk-my-****-****-8f2a)',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态：active/disabled',
  `last_used_at` DATETIME DEFAULT NULL COMMENT '最后使用时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_hash` (`key_hash`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API密钥表';
