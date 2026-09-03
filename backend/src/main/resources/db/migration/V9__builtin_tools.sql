-- V9: 内置工具（builtin）种子 —— 让 Agent 作用于真实世界：文件读写编辑、Glob/Grep、命令执行、网页抓取/搜索
-- 执行面：会话级沙箱目录（agent.sandbox.root/session-{sessionId}），路径越界拒绝；命令有超时/输出上限；
-- web_fetch 拒绝内网地址。工具名与 BuiltinToolServiceImpl 的分派一一对应，schema 变更需同步代码。

INSERT INTO tool (workspace_id, created_by, name, display_name, description, category, type, status,
                  method, endpoint_timeout, parameters, retry_on_fail, max_retries)
SELECT 1, 1, t.name, t.display_name, t.description, t.category, 'builtin', 'active',
       'GET', 0, t.params, 0, 0
FROM (
  SELECT 'read_file' AS name, '读取文件' AS display_name,
         '读取会话沙箱内的文本文件，支持 offset/limit 分段读取大文件' AS description,
         'operate' AS category,
         '[{"name":"path","type":"string","required":true,"description":"文件相对路径，如 docs/笔记.md"},{"name":"offset","type":"integer","required":false,"description":"起始行号（1 起），默认从头"},{"name":"limit","type":"integer","required":false,"description":"读取行数，默认 400"}]' AS params
  UNION ALL
  SELECT 'write_file', '写入文件',
         '创建或覆盖沙箱内的文本文件（UTF-8），自动创建父目录',
         'operate',
         '[{"name":"path","type":"string","required":true,"description":"文件相对路径，如 reports/summary.md"},{"name":"content","type":"string","required":true,"description":"要写入的完整内容"}]' AS params
  UNION ALL
  SELECT 'edit_file', '编辑文件',
         '对已有文件做精确替换式编辑：old_string 必须与文件内容完全一致（含缩进换行），唯一命中才替换',
         'operate',
         '[{"name":"path","type":"string","required":true,"description":"文件相对路径"},{"name":"old_string","type":"string","required":true,"description":"要被替换的精确原文"},{"name":"new_string","type":"string","required":true,"description":"替换后的文本"},{"name":"replace_all","type":"boolean","required":false,"description":"多处命中时是否全部替换，默认 false"}]' AS params
  UNION ALL
  SELECT 'list_files', '列举文件',
         '按 glob 模式列举沙箱内文件，如 **/*.java、docs/*.md、*',
         'operate',
         '[{"name":"pattern","type":"string","required":false,"description":"glob 模式，默认 **/* 匹配全部"},{"name":"path","type":"string","required":false,"description":"基准目录，默认沙箱根"}]' AS params
  UNION ALL
  SELECT 'search_files', '搜索文件内容',
         '在沙箱文本文件内搜索关键词或正则（类似 grep），返回 文件:行号: 内容',
         'search',
         '[{"name":"pattern","type":"string","required":true,"description":"关键词或正则表达式"},{"name":"path","type":"string","required":false,"description":"搜索的基准目录，默认沙箱根"},{"name":"is_regex","type":"boolean","required":false,"description":"pattern 是否为正则，默认 false"},{"name":"max_results","type":"integer","required":false,"description":"最多返回匹配行数，默认 50，上限 100"}]' AS params
  UNION ALL
  SELECT 'run_command', '执行命令',
         '在会话沙箱目录内执行 shell 命令并返回输出（Windows 用 cmd /c，Linux 用 sh -c），有超时与输出上限',
         'operate',
         '[{"name":"command","type":"string","required":true,"description":"要执行的命令，如 dir、python x.py、npm test"},{"name":"timeout_ms","type":"integer","required":false,"description":"超时毫秒数，默认 30000，上限 120000"}]' AS params
  UNION ALL
  SELECT 'web_search', '网页搜索',
         '必应网页搜索，返回标题、链接、摘要（无需密钥）',
         'search',
         '[{"name":"query","type":"string","required":true,"description":"搜索关键词"},{"name":"count","type":"integer","required":false,"description":"返回结果条数 1-10，默认 5"}]' AS params
  UNION ALL
  SELECT 'web_fetch', '网页抓取',
         '抓取公网 URL 的内容并转为文本（支持网页/JSON/纯文本，拒绝内网地址）',
         'perceive',
         '[{"name":"url","type":"string","required":true,"description":"要抓取的完整 http/https 地址"},{"name":"max_length","type":"integer","required":false,"description":"返回文本最大字符数，默认 8000"}]' AS params
) t
WHERE NOT EXISTS (SELECT 1 FROM tool x WHERE x.name = t.name);
