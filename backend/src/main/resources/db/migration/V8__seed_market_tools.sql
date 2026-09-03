-- V8: 工具市场预置常用公共 API 工具（幂等，按 name 去重）
-- 全部为免费、无需密钥的公开接口，实测国内网络可达。
-- 工具市场不做账户/工作空间隔离（产品决策 2026-09-03），所有账户可见、可绑定、可调用。

INSERT INTO tool (workspace_id, created_by, name, display_name, description, category, type, status,
                  endpoint_url, method, endpoint_timeout, parameters, response_mapping,
                  retry_on_fail, max_retries)
SELECT 1, 1, t.name, t.display_name, t.description, t.category, 'api', 'active',
       t.endpoint_url, 'GET', t.timeout_ms, t.params, t.resp_mapping, 1, 1
FROM (
  SELECT 'query_weather' AS name, '天气查询' AS display_name,
         '查询全球任意城市当前天气，返回温度、风速、风向、天气代码。需先提供城市经纬度（可用 geocode_city 工具查询）' AS description,
         'perceive' AS category,
         'https://api.open-meteo.com/v1/forecast?current_weather=true' AS endpoint_url,
         10000 AS timeout_ms,
         '[{"name":"latitude","type":"number","required":true,"description":"纬度，如北京 39.9"},{"name":"longitude","type":"number","required":true,"description":"经度，如北京 116.4"},{"name":"timezone","type":"string","required":false,"description":"时区，如 Asia/Shanghai，默认自动"}]' AS params,
         '$.current_weather' AS resp_mapping
  UNION ALL
  SELECT 'geocode_city', '城市定位',
         '根据城市名查询经纬度坐标和人口、时区等信息，配合天气查询使用',
         'perceive',
         'https://geocoding-api.open-meteo.com/v1/search?language=zh',
         10000,
         '[{"name":"name","type":"string","required":true,"description":"城市名，支持中英文，如 北京"},{"name":"count","type":"integer","required":false,"description":"返回结果数量，默认 5"}]',
         '$.results[0]'
  UNION ALL
  SELECT 'query_exchange_rate', '汇率换算',
         '查询实时汇率并换算金额，支持全球主要货币（USD/CNY/EUR/JPY/GBP/HKD 等）',
         'compute',
         'https://api.frankfurter.app/latest',
         10000,
         '[{"name":"from","type":"string","required":false,"description":"源货币代码，默认 USD"},{"name":"to","type":"string","required":false,"description":"目标货币代码，默认 CNY，多个用逗号分隔"},{"name":"amount","type":"number","required":false,"description":"换算金额，默认 1"}]',
         NULL
  UNION ALL
  SELECT 'translate_text', '文本翻译',
         '文本翻译，支持全球主要语言互译（中英日韩法德等）',
         'compute',
         'https://api.mymemory.translated.net/get',
         15000,
         '[{"name":"q","type":"string","required":true,"description":"要翻译的文本"},{"name":"langpair","type":"string","required":true,"description":"语言对，格式 源语言|目标语言，如 en|zh-CN、zh-CN|en、ja|zh-CN"}]',
         '$.responseData.translatedText'
  UNION ALL
  SELECT 'query_world_time', '世界时钟',
         '查询全球任意时区的当前精确时间（年月日时分秒、星期几）',
         'perceive',
         'https://timeapi.io/api/Time/current/zone',
         10000,
         '[{"name":"timeZone","type":"string","required":true,"description":"IANA 时区名，如 Asia/Shanghai、America/New_York、Europe/London"}]',
         NULL
  UNION ALL
  SELECT 'make_short_url', '短链接生成',
         '把长网址缩短为短链接（tinyurl.com），方便分享',
         'operate',
         'https://tinyurl.com/api-create.php',
         12000,
         '[{"name":"url","type":"string","required":true,"description":"要缩短的完整网址，如 https://example.com/very/long/path"}]',
         NULL
  UNION ALL
  SELECT 'search_github_repos', 'GitHub 仓库搜索',
         '搜索 GitHub 开源仓库，返回仓库名、描述、星数、语言、地址等信息',
         'search',
         'https://api.github.com/search/repositories',
         15000,
         '[{"name":"q","type":"string","required":true,"description":"搜索关键词，如 spring boot、vue3"},{"name":"sort","type":"string","required":false,"description":"排序字段：stars、forks、updated，默认按最佳匹配"},{"name":"per_page","type":"integer","required":false,"description":"每页数量，默认 5"}]',
         NULL
  UNION ALL
  SELECT 'daily_hitokoto', '每日一言',
         '获取随机一言（来自动画、文学、哲学、诗词等的经典句子）',
         'perceive',
         'https://v1.hitokoto.cn/',
         10000,
         '[{"name":"c","type":"string","required":false,"description":"句子类型：a=动画 b=文学 c=哲学 d=诗词 i=诗词 e=原创，默认随机"}]',
         NULL
) t
WHERE NOT EXISTS (SELECT 1 FROM tool x WHERE x.name = t.name);
