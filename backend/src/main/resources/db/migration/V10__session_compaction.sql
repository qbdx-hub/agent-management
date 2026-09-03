-- V10: 会话上下文压缩（阶段2 上下文经济学）
-- context_summary: 超出历史 token 预算的旧消息由 LLM 压缩成的前情提要；
-- summarized_message_id: 摘要覆盖到的最后一条消息，之前的历史不再重复进入上下文。

ALTER TABLE session
  ADD COLUMN context_summary MEDIUMTEXT NULL COMMENT '压缩后的前情提要' AFTER variables,
  ADD COLUMN summarized_message_id BIGINT NULL COMMENT '已被摘要覆盖的最后一条消息ID' AFTER context_summary;
