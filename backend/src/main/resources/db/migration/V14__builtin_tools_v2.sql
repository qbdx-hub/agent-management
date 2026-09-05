-- V14: 内置工具第二批扩容（34 个）—— 计算/时间/文本编码/文件操作/网络查询/通知，市场总量 16 → 50
-- 分派实现：BuiltinToolServiceImpl.execute() switch → service/builtin/handler/*（Calc/Text/File/Net）
-- 外部依赖仅 zxing（二维码，已入 pom）；open-meteo / ip-api.com 免密钥外部 API；其余纯 JDK/Jackson/pdfbox。
-- 幂等：name 已存在则跳过。parameters 与 handler 取参名一一对应，改动需同步代码。

INSERT INTO tool (workspace_id, created_by, name, display_name, description, category, type, status,
                  method, endpoint_timeout, parameters, retry_on_fail, max_retries, icon)
SELECT 1, 1, t.name, t.display_name, t.description, t.category, 'builtin', 'active',
       'GET', 0, t.params, 0, 0, t.icon
FROM (
  -- ==================== 计算 ====================
  SELECT 'calculator' AS name, '计算器' AS display_name,
         '精确数学表达式求值：+ - * / % ^ 括号、函数 sqrt/sin/cos/tan/abs/round/floor/ceil/log/ln/pow/min/max、常量 pi/e。不拼接执行脚本，天然防注入' AS description,
         'compute' AS category, 'calculator' AS icon,
         '[{"name":"expression","type":"string","required":true,"description":"数学表达式，如 (2+3)*sqrt(16)、pow(2,10)、round(3.14159, 2)"}]' AS params
  UNION ALL
  SELECT 'unit_convert', '单位换算',
         '长度/重量/面积/体积/速度/数据大小/温度的常用单位互转，如 kg→lb、km/h→m/s',
         'compute', 'split',
         '[{"name":"value","type":"number","required":true,"description":"要换算的数值"},{"name":"from","type":"string","required":true,"description":"源单位：mm/cm/m/km/in/ft/mi、mg/g/kg/t/lb/oz、m2/km2/ha/mu/ft2、ml/l/m3/gal、mps/kmh/knot、b/kb/mb/gb/tb、c/f/k"},{"name":"to","type":"string","required":true,"description":"目标单位，须与源单位同类别"}]' AS params
  UNION ALL
  SELECT 'number_base_convert', '进制转换',
         '2-36 进制任意互转，支持超出 long 范围的大数（如二进制↔十六进制）',
         'compute', 'binary',
         '[{"name":"value","type":"string","required":true,"description":"待转换的数值字符串，如 ff1010 或 1010"},{"name":"from_base","type":"integer","required":false,"description":"源进制 2-36，默认 10"},{"name":"to_base","type":"integer","required":false,"description":"目标进制 2-36，默认 16"}]' AS params
  UNION ALL
  SELECT 'random_generator', '随机数/密码生成',
         '安全随机数（指定范围）、UUID、强密码（可配长度与字符类别，保证每类至少一位）',
         'compute', 'dice',
         '[{"name":"type","type":"string","required":false,"description":"number（默认）或 uuid 或 password"},{"name":"min","type":"integer","required":false,"description":"随机数下界，默认 0"},{"name":"max","type":"integer","required":false,"description":"随机数上界，默认 100"},{"name":"length","type":"integer","required":false,"description":"密码长度 4-128，默认 16"},{"name":"uppercase","type":"boolean","required":false,"description":"密码含大写字母"},{"name":"lowercase","type":"boolean","required":false,"description":"密码含小写字母"},{"name":"digits","type":"boolean","required":false,"description":"密码含数字"},{"name":"symbols","type":"boolean","required":false,"description":"密码含特殊符号"}]' AS params
  UNION ALL
  SELECT 'loan_calc', '房贷计算器',
         '等额本息/等额本金两种还款方式的月供、总利息与还款总额测算',
         'compute', 'coins',
         '[{"name":"amount","type":"number","required":true,"description":"贷款总额（元）"},{"name":"annual_rate","type":"number","required":true,"description":"年利率，百分数，如 3.6 表示 3.6%"},{"name":"years","type":"integer","required":false,"description":"贷款年限，默认 30"},{"name":"method","type":"string","required":false,"description":"average_capital_plus_interest（等额本息，默认）或 average_capital（等额本金）"}]' AS params
  UNION ALL
  SELECT 'color_convert', '颜色格式转换',
         'HEX/RGB/HSL 三种颜色格式互转，输入任一格式输出全部三种',
         'compute', 'palette',
         '[{"name":"color","type":"string","required":true,"description":"颜色值：#FF5733、rgb(255,87,51) 或 hsl(11,100%,60%)"}]' AS params
  -- ==================== 时间 ====================
  UNION ALL
  SELECT 'current_time', '当前时间',
         '获取指定时区的当前日期时间（ISO 格式、星期、Unix 时间戳）',
         'compute', 'clock',
         '[{"name":"timezone","type":"string","required":false,"description":"IANA 时区，如 Asia/Shanghai、UTC，默认系统时区"}]' AS params
  UNION ALL
  SELECT 'date_calculator', '日期计算器',
         '日期加减天数/工作日、两个日期相差天数、星期几推算',
         'compute', 'calendar',
         '[{"name":"date","type":"string","required":true,"description":"起始日期，格式 yyyy-MM-dd"},{"name":"date2","type":"string","required":false,"description":"结束日期；提供时计算 date 与 date2 相差天数"},{"name":"days","type":"integer","required":false,"description":"推算 date 加 N 天（可为负）后的日期"},{"name":"workdays","type":"integer","required":false,"description":"推算 date 加 N 个工作日（跳过周末）后的日期"}]' AS params
  UNION ALL
  SELECT 'timestamp_convert', '时间戳转换',
         'Unix 时间戳（秒/毫秒自动识别）与日期字符串双向转换',
         'compute', 'clock',
         '[{"name":"timestamp","type":"string","required":false,"description":"Unix 时间戳，秒/毫秒自动识别，转成日期"},{"name":"date","type":"string","required":false,"description":"日期字符串 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss，转成时间戳"},{"name":"timezone","type":"string","required":false,"description":"IANA 时区，默认系统时区"}]' AS params
  UNION ALL
  SELECT 'cron_next', 'Cron 下次执行',
         '解析 Spring 风格 6 位 cron 表达式，计算之后 N 次执行时间点',
         'compute', 'repeat',
         '[{"name":"expression","type":"string","required":true,"description":"cron 表达式（6 位：秒 分 时 日 月 周），如 0 0 9 * * MON-FRI"},{"name":"count","type":"integer","required":false,"description":"展示后续执行次数 1-10，默认 3"}]' AS params
  -- ==================== 文本与编码 ====================
  UNION ALL
  SELECT 'text_stats', '字数统计',
         '统计文本的总字符数、中文字数、英文单词数、行数与 UTF-8 字节数',
         'compute', 'chart-bar',
         '[{"name":"text","type":"string","required":true,"description":"要统计的文本"}]' AS params
  UNION ALL
  SELECT 'text_transform', '文本变换',
         '大写/小写/去空白/反转/行去重/行排序/添加行号，一个工具覆盖常用文本批处理',
         'compute', 'wand',
         '[{"name":"text","type":"string","required":true,"description":"要处理的文本"},{"name":"op","type":"string","required":true,"description":"upper/lower/trim/reverse/unique（行去重）/sort（行排序）/number（加行号）"}]' AS params
  UNION ALL
  SELECT 'regex_tool', '正则工具',
         '正则匹配提取（列出全部命中与捕获组）/整串校验/批量替换',
         'compute', 'regex',
         '[{"name":"text","type":"string","required":true,"description":"目标文本"},{"name":"pattern","type":"string","required":true,"description":"正则表达式"},{"name":"mode","type":"string","required":false,"description":"find（默认，列匹配）/match（校验）/replace（替换）"},{"name":"replacement","type":"string","required":false,"description":"mode=replace 时的替换文本，可用 $1 引用捕获组"}]' AS params
  UNION ALL
  SELECT 'base64_codec', 'Base64 编解码',
         '文本 Base64 编码/解码，支持 URL-safe 变体',
         'compute', 'code',
         '[{"name":"text","type":"string","required":true,"description":"要编解码的文本"},{"name":"action","type":"string","required":true,"description":"encode 或 decode"},{"name":"url_safe","type":"boolean","required":false,"description":"是否 URL-safe 字母表，默认 false"}]' AS params
  UNION ALL
  SELECT 'hash_calculator', '哈希计算',
         '计算 MD5/SHA-1/SHA-256 摘要，输入文本或沙箱内文件（≤10MB）',
         'compute', 'hash',
         '[{"name":"algorithm","type":"string","required":true,"description":"md5/sha1/sha256"},{"name":"text","type":"string","required":false,"description":"要哈希的文本（与 path 二选一）"},{"name":"path","type":"string","required":false,"description":"沙箱内文件相对路径（与 text 二选一）"}]' AS params
  UNION ALL
  SELECT 'url_codec', 'URL 编解码',
         'URL 百分号编码/解码（UTF-8），如中文转 %E4%B8%AD',
         'compute', 'world',
         '[{"name":"text","type":"string","required":true,"description":"要编解码的文本"},{"name":"action","type":"string","required":true,"description":"encode 或 decode"}]' AS params
  UNION ALL
  SELECT 'json_tool', 'JSON 工具',
         'JSON 校验/格式化/压缩，并支持点路径取值（如 user.name、items[0].id）',
         'compute', 'code',
         '[{"name":"text","type":"string","required":true,"description":"JSON 文本"},{"name":"action","type":"string","required":false,"description":"validate/format（默认）/minify/get"},{"name":"path","type":"string","required":false,"description":"action=get 时的点路径，如 data.items[0].name"}]' AS params
  UNION ALL
  SELECT 'csv_json_convert', 'CSV↔JSON 转换',
         'CSV 与 JSON 数组互转，支持自定义分隔符、引号包裹与转义',
         'compute', 'table',
         '[{"name":"text","type":"string","required":true,"description":"CSV 或 JSON 数组文本"},{"name":"direction","type":"string","required":true,"description":"csv_to_json 或 json_to_csv"},{"name":"delimiter","type":"string","required":false,"description":"分隔符，默认逗号"}]' AS params
  UNION ALL
  SELECT 'text_diff', '文本对比',
         '两段文本的行级差异对比（LCS 算法），输出新增/删除行',
         'compute', 'git-compare',
         '[{"name":"text1","type":"string","required":true,"description":"原文"},{"name":"text2","type":"string","required":true,"description":"新文"}]' AS params
  -- ==================== 文件操作（沙箱内） ====================
  UNION ALL
  SELECT 'create_dir', '创建目录',
         '在沙箱内递归创建目录（mkdir -p 语义）',
         'operate', 'folder-plus',
         '[{"name":"path","type":"string","required":true,"description":"目录相对路径，如 docs/2026/09"}]' AS params
  UNION ALL
  SELECT 'delete_path', '删除文件/目录',
         '删除沙箱内文件或目录（目录递归删除，拒绝删除沙箱根）',
         'operate', 'trash',
         '[{"name":"path","type":"string","required":true,"description":"要删除的相对路径"}]' AS params
  UNION ALL
  SELECT 'move_path', '移动/重命名',
         '移动或重命名沙箱内文件/目录，目标已存在时拒绝',
         'operate', 'arrows-left-right',
         '[{"name":"from","type":"string","required":true,"description":"源路径"},{"name":"to","type":"string","required":true,"description":"目标路径"}]' AS params
  UNION ALL
  SELECT 'copy_path', '复制文件/目录',
         '复制沙箱内文件或目录（递归，单文件 ≤10MB）',
         'operate', 'copy',
         '[{"name":"from","type":"string","required":true,"description":"源路径"},{"name":"to","type":"string","required":true,"description":"目标路径"}]' AS params
  UNION ALL
  SELECT 'file_info', '文件信息',
         '查看沙箱内文件/目录的大小、修改时间、行数，图片附加像素尺寸',
         'operate', 'clipboard',
         '[{"name":"path","type":"string","required":true,"description":"文件或目录相对路径"}]' AS params
  UNION ALL
  SELECT 'zip_pack', '打包 zip',
         '把沙箱内文件/目录打包为 zip（≤5000 项，单文件 ≤10MB）',
         'operate', 'package',
         '[{"name":"source","type":"string","required":true,"description":"要打包的文件或目录相对路径"},{"name":"zip_path","type":"string","required":true,"description":"输出的 zip 相对路径，如 backup.zip"}]' AS params
  UNION ALL
  SELECT 'zip_unpack', '解压 zip',
         '解压 zip 到沙箱目录，带 zip-slip 路径穿越防护与 50MB 解压总量上限',
         'operate', 'package',
         '[{"name":"zip_path","type":"string","required":true,"description":"zip 文件相对路径"},{"name":"dest","type":"string","required":false,"description":"解压目标目录，默认沙箱根"}]' AS params
  -- ==================== 网络查询 ====================
  UNION ALL
  SELECT 'http_request', 'HTTP 请求',
         '通用 REST 调用：GET/POST/PUT/DELETE/PATCH，自定义请求头与请求体；拒绝内网地址，响应截断防失控',
         'perceive', 'api',
         '[{"name":"url","type":"string","required":true,"description":"完整 http/https 地址"},{"name":"method","type":"string","required":false,"description":"GET（默认）/POST/PUT/DELETE/PATCH"},{"name":"headers","type":"string","required":false,"description":"请求头，每行一个 K: V"},{"name":"body","type":"string","required":false,"description":"请求体（通常为 JSON 字符串）"}]' AS params
  UNION ALL
  SELECT 'weather_forecast', '天气预报',
         '查询全球城市当前天气与未来最多 7 天预报（open-meteo 免密钥，中文天气描述）',
         'search', 'cloud',
         '[{"name":"city","type":"string","required":true,"description":"城市名，支持中英文，如 北京、Tokyo"},{"name":"days","type":"integer","required":false,"description":"预报天数 1-7，默认 3"}]' AS params
  UNION ALL
  SELECT 'ip_lookup', 'IP 归属地查询',
         '查询 IP 的国家/省市/运营商（ip-api.com 免密钥中文）；不传 ip 时查询本机出口 IP',
         'search', 'world',
         '[{"name":"ip","type":"string","required":false,"description":"要查询的 IPv4/IPv6，留空查出口 IP"}]' AS params
  UNION ALL
  SELECT 'dns_lookup', '域名解析',
         '解析域名的全部 IP 记录（A/AAAA），内网地址会特别标注',
         'search', 'server',
         '[{"name":"host","type":"string","required":true,"description":"域名，如 github.com"}]' AS params
  UNION ALL
  SELECT 'url_metadata', '网页元信息',
         '抓取网页的 title、description 与 og 标签（og:title/og:image 等），比整页抓取更轻量',
         'perceive', 'link',
         '[{"name":"url","type":"string","required":true,"description":"完整 http/https 地址"}]' AS params
  UNION ALL
  SELECT 'qr_generate', '二维码生成',
         '把文本/链接生成为二维码 SVG 矢量图，保存到沙箱（可下载后直接使用）',
         'operate', 'qrcode',
         '[{"name":"content","type":"string","required":true,"description":"二维码内容（文本或 URL，≤1000 字符）"},{"name":"file_name","type":"string","required":false,"description":"输出文件名，默认 qr-时间戳.svg"}]' AS params
  UNION ALL
  SELECT 'pdf_extract_text', 'PDF 文本提取',
         '提取沙箱内 PDF 的文本内容，可指定页码范围（扫描件无文本会明确提示）',
         'perceive', 'file-text',
         '[{"name":"path","type":"string","required":true,"description":"PDF 相对路径"},{"name":"pages","type":"string","required":false,"description":"页码范围，如 3 或 1-5，默认全部页"}]' AS params
  -- ==================== 通知 ====================
  UNION ALL
  SELECT 'webhook_notify', 'Webhook 推送',
         '把消息 POST 到企业微信/钉钉/飞书等机器人 webhook 地址（内容非 JSON 时自动包装）',
         'notify', 'bell',
         '[{"name":"url","type":"string","required":true,"description":"机器人 webhook 完整地址（仅 http/https，拒绝内网）"},{"name":"content","type":"string","required":true,"description":"要推送的内容，纯文本或完整 JSON"}]' AS params
) t
WHERE NOT EXISTS (SELECT 1 FROM tool x WHERE x.name = t.name);

-- 旧 8 个内置工具（V9 建立时未配 icon，市场页一直兜底 robot 图标）补齐图标
UPDATE tool SET icon = 'file-text' WHERE name = 'read_file'   AND type = 'builtin';
UPDATE tool SET icon = 'pencil'     WHERE name = 'write_file' AND type = 'builtin';
UPDATE tool SET icon = 'clipboard'  WHERE name = 'edit_file'  AND type = 'builtin';
UPDATE tool SET icon = 'list'       WHERE name = 'list_files' AND type = 'builtin';
UPDATE tool SET icon = 'search'     WHERE name = 'search_files' AND type = 'builtin';
UPDATE tool SET icon = 'cpu'        WHERE name = 'run_command'  AND type = 'builtin';
UPDATE tool SET icon = 'search'     WHERE name = 'web_search'   AND type = 'builtin';
UPDATE tool SET icon = 'world'      WHERE name = 'web_fetch'    AND type = 'builtin';
