-- ============================================================
-- V7: 模型定价目录刷新（2026-09-03）
-- ------------------------------------------------------------
-- 背景：
--   1. 旧目录里的型号已大量停用：
--      - DeepSeek：deepseek-chat / deepseek-reasoner 于 2026-07-24 弃用，
--        现役为 deepseek-v4-flash / deepseek-v4-pro（1M 上下文）
--      - OpenAI：gpt-3.5-turbo 已退役
--      - Kimi：moonshot-v1 系列 2026-08-31 下线
--      - 小米 MiMo：V2 系列 2026-06-30 下线，现役 V2.5 系列
--   2. 按需求补充 Kimi / MiniMax / 智谱 GLM / DeepSeek / 小米 五家现役模型。
-- 价格说明：
--   input_price_per_1k / output_price_per_1k 统一为「美元 / 千 token」，
--   国内厂商按人民币牌价 ÷ 7.2 汇率折算，display_name 中保留人民币参考价（每百万 token）。
--   max_tokens 列沿用旧目录惯例，存的是上下文窗口大小。
-- 各厂商 OpenAI 兼容 Base URL（前端选择供应商时自动回填）：
--   deepseek  https://api.deepseek.com
--   zhipu     https://open.bigmodel.cn/api/paas/v4
--   moonshot  https://api.moonshot.cn/v1
--   minimax   https://api.minimaxi.com/v1
--   xiaomi    https://api.xiaomimimo.com/v1
--   openai    https://api.openai.com/v1
-- ============================================================

DELETE FROM `model_pricing`;

INSERT INTO `model_pricing` (`provider`, `model_name`, `display_name`, `max_tokens`, `input_price_per_1k`, `output_price_per_1k`) VALUES
-- DeepSeek（官网牌价：V4-Flash 输入¥1/输出¥2，V4-Pro 输入¥3/输出¥6，1M 上下文）
('deepseek', 'deepseek-v4-flash', 'DeepSeek V4-Flash（¥1/¥2 每百万）', 1000000, 0.000139, 0.000278),
('deepseek', 'deepseek-v4-pro',   'DeepSeek V4-Pro（¥3/¥6 每百万）',   1000000, 0.000417, 0.000833),
-- 智谱 GLM（GLM-5.3 / 5.2 牌价 输入¥8/输出¥28；5.3-Flash 为旗舰 1/10 价；4.7-Flash 免费）
('zhipu', 'glm-5.3',       'GLM-5.3 旗舰（¥8/¥28 每百万）',   1000000, 0.001111, 0.003889),
('zhipu', 'glm-5.3-flash', 'GLM-5.3-Flash 普惠（¥0.8/¥2.8）', 1000000, 0.000111, 0.000389),
('zhipu', 'glm-5.2',       'GLM-5.2 长程任务（¥8/¥28 每百万）', 1000000, 0.001111, 0.003889),
('zhipu', 'glm-4.7-flash', 'GLM-4.7-Flash（免费）',            200000, 0.000000, 0.000000),
-- Kimi 月之暗面（官网牌价，K3 为 1M 上下文旗舰，K2.7 Code / K2.6 为 256K）
('moonshot', 'kimi-k3',        'Kimi K3 旗舰（¥20/¥100 每百万）',   1048576, 0.002778, 0.013889),
('moonshot', 'kimi-k2.7-code', 'Kimi K2.7 Code（¥6.5/¥27 每百万）',  262144, 0.000903, 0.003750),
('moonshot', 'kimi-k2.6',      'Kimi K2.6（¥6.5/¥27 每百万）',       262144, 0.000903, 0.003750),
-- MiniMax（官网按量计费：M3 永久五折价 输入¥2.1/输出¥8.4，1M 上下文；M2.7 同价）
('minimax', 'MiniMax-M3',   'MiniMax M3（¥2.1/¥8.4 每百万·五折）', 1000000, 0.000292, 0.001167),
('minimax', 'MiniMax-M2.7', 'MiniMax M2.7（¥2.1/¥8.4 每百万）',     1000000, 0.000292, 0.001167),
-- 小米 MiMo（官网牌价：V2.5 全模态 输入¥1/输出¥2，V2.5 Pro 万亿参数 输入¥3/输出¥6，均 1M 上下文）
('xiaomi', 'mimo-v2.5',     'MiMo V2.5 全模态（¥1/¥2 每百万）', 1000000, 0.000139, 0.000278),
('xiaomi', 'mimo-v2.5-pro', 'MiMo V2.5 Pro（¥3/¥6 每百万）',    1000000, 0.000417, 0.000833),
-- OpenAI（GPT-5 系列 $1.25/$10、Mini $0.25/$2、Nano $0.05/$0.40 每百万）
('openai', 'gpt-5',       'GPT-5',       400000, 0.001250, 0.010000),
('openai', 'gpt-5-mini',  'GPT-5 Mini',  400000, 0.000250, 0.002000),
('openai', 'gpt-5-nano',  'GPT-5 Nano',  400000, 0.000050, 0.000400),
('openai', 'gpt-4o',      'GPT-4o',      128000, 0.002500, 0.010000),
('openai', 'gpt-4o-mini', 'GPT-4o Mini', 128000, 0.000150, 0.000600),
-- Anthropic（官方原生为 Anthropic 协议，需经 OpenAI 兼容网关接入本系统）
('anthropic', 'claude-opus-5',    'Claude Opus 5',     1000000, 0.005000, 0.025000),
('anthropic', 'claude-sonnet-5',  'Claude Sonnet 5',   1000000, 0.003000, 0.015000),
('anthropic', 'claude-sonnet-4-6', 'Claude Sonnet 4.6', 1000000, 0.003000, 0.015000),
('anthropic', 'claude-haiku-4-5',  'Claude Haiku 4.5',   200000, 0.001000, 0.005000);
