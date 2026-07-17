-- budget 表添加 created_by 字段，用于账户级数据隔离
ALTER TABLE budget ADD COLUMN created_by BIGINT DEFAULT NULL COMMENT '创建者用户ID' AFTER enabled;
UPDATE budget SET created_by = 1 WHERE created_by IS NULL;
ALTER TABLE budget ADD INDEX idx_created_by (created_by);
