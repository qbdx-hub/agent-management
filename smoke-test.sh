#!/bin/bash
# 冒烟测试：注册登录 -> 各核心模块 -> 账户隔离验证
# 注意：Windows 下 curl 命令行参数会按 ANSI 代码页转码，body 一律用 ASCII，
# 中文检索词用 URL 百分号编码（不经过转码，安全）。
BASE="http://localhost:8080/api/v1"
PASS=0; FAIL=0
TS=$(date +%s)
U1="testuser_$TS"
U2="other_$TS"
TOKEN=""; TOKEN2=""
LAST_BODY=""

req() { # method, path, data, token -> sets CODE and BODY
  local m="$1" p="$2" d="$3" t="$4"
  local args=(-s -w $'\n%{http_code}' -X "$m" "$BASE$p" -H "Content-Type: application/json")
  [ -n "$d" ] && args+=(-d "$d")
  if [ -n "$t" ]; then
    args+=(-H "Authorization: Bearer $t" -H "X-Workspace-Id: ${WS_ID:-1}")
  fi
  local out; out=$(curl "${args[@]}" 2>&1)
  CODE=$(echo "$out" | tail -1)
  LAST_BODY=$(echo "$out" | sed '$d')
}

check_ok() { # name — 期望 HTTP 200 且业务 code=0
  local name="$1"
  if [ "$CODE" = "200" ] && echo "$LAST_BODY" | grep -q '"code":0'; then
    PASS=$((PASS+1)); echo "PASS $name"
  else
    FAIL=$((FAIL+1)); echo "FAIL $name (http=$CODE)"
    echo "      body: $(echo "$LAST_BODY" | head -c 250)"
  fi
}
check_fail() { # name — 期望业务 code!=0（HTTP 可能为 200/4xx）
  local name="$1"
  if echo "$LAST_BODY" | grep -q '"code":0'; then
    FAIL=$((FAIL+1)); echo "FAIL $name — 业务调用竟然成功: $(echo "$LAST_BODY" | head -c 150)"
  else
    PASS=$((PASS+1)); echo "PASS $name (http=$CODE, code!=0)"
  fi
}
check_http() { # name, expected_http — 纯 HTTP 层断言
  local name="$1" exp="$2"
  if [ "$CODE" = "$exp" ]; then
    PASS=$((PASS+1)); echo "PASS $name ($CODE)"
  else
    FAIL=$((FAIL+1)); echo "FAIL $name (http=$CODE != $exp) body: $(echo "$LAST_BODY" | head -c 200)"
  fi
}

jfield() { # 字符串字段第一个匹配
  echo "$LAST_BODY" | grep -o "\"$1\":\"[^\"]*\"" | head -1 | sed 's/.*:"//;s/"$//'
}
jnum() { # 数字字段第一个匹配
  echo "$LAST_BODY" | grep -o "\"$1\":[0-9][0-9]*" | head -1 | grep -o '[0-9][0-9]*'
}

echo "===== 1. 认证模块 ====="
req POST /auth/register "{\"username\":\"$U1\",\"nickname\":\"Smoke Tester\",\"email\":\"$U1@test.com\",\"password\":\"Test@123456\"}"
check_ok "注册新用户 $U1"

req POST /auth/login "{\"username\":\"$U1\",\"password\":\"Test@123456\"}"
check_ok "登录 $U1"
TOKEN=$(jfield token)
echo "      token: ${TOKEN:0:30}..."

req GET /auth/me "" "$TOKEN"
check_ok "获取当前用户信息"
WS_ID=$(echo "$LAST_BODY" | grep -o '"workspaces":\[{[^}]*' | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
[ -z "$WS_ID" ] && WS_ID=1
echo "      workspaceId=$WS_ID"

req POST /auth/login "{\"username\":\"$U1\",\"password\":\"WrongPass!\"}"
check_fail "错误密码登录被拒绝"

req GET /agents "" ""
check_http "无 token 访问 -> 401" 401

echo "===== 2. Agent 模块 ====="
req POST /agents "{\"name\":\"smoke-agent-$TS\",\"description\":\"auto smoke test\",\"modelName\":\"gpt-4o-mini\",\"modelProvider\":\"openai\",\"temperature\":0.7,\"maxTokens\":2048,\"tags\":[\"test\"]}" "$TOKEN"
check_ok "创建 Agent"
AGENT_ID=$(jnum id); echo "      agentId=$AGENT_ID"

req GET "/agents/$AGENT_ID" "" "$TOKEN"
check_ok "获取 Agent 详情"

req PATCH "/agents/$AGENT_ID/status" "{\"status\":\"testing\"}" "$TOKEN"
check_ok "更新 Agent 状态(draft->testing)"

req PATCH "/agents/$AGENT_ID/status" "{\"status\":\"published\"}" "$TOKEN"
check_ok "更新 Agent 状态(testing->published)"

req GET "/agents?page=1&size=10" "" "$TOKEN"
check_ok "Agent 列表"

echo "===== 3. 知识库模块 ====="
req POST /knowledge-bases "{\"name\":\"smoke-kb-$TS\",\"description\":\"smoke test kb\",\"type\":\"text\"}" "$TOKEN"
check_ok "创建知识库"
KB_ID=$(jnum id); echo "      kbId=$KB_ID"

req GET "/knowledge-bases/$KB_ID" "" "$TOKEN"
check_ok "获取知识库详情"

# 文档上传为 multipart 接口（RequestParam file）；注意 Windows curl 读不了 /tmp 虚拟路径
echo "Agent management platform supports knowledge base retrieval and chat. Knowledge base search test content. The quick brown fox jumps over the lazy dog." > smoke-doc.tmp
out=$(curl -s -w $'\n%{http_code}' -X POST "$BASE/knowledge-bases/$KB_ID/documents" \
  -H "Authorization: Bearer $TOKEN" -H "X-Workspace-Id: $WS_ID" \
  -F "file=@smoke-doc.tmp;filename=smoke-doc.txt")
CODE=$(echo "$out" | tail -1); LAST_BODY=$(echo "$out" | sed '$d')
check_ok "上传文档(multipart)"
DOC_ID=$(jnum id); echo "      docId=$DOC_ID"

req GET "/knowledge-bases/$KB_ID/documents" "" "$TOKEN"
check_ok "文档列表"

req POST "/knowledge-bases/$KB_ID/documents/$DOC_ID/process" "" "$TOKEN"
check_ok "文档处理(分段/向量化)"

req GET "/knowledge-bases/$KB_ID/search?q=knowledge%20base&topK=5" "" "$TOKEN"
check_ok "知识库检索(q=knowledge base)"

echo "===== 4. 会话模块 ====="
req POST "/agents/$AGENT_ID/sessions" "{\"title\":\"smoke-session-$TS\"}" "$TOKEN"
check_ok "创建会话"
SESSION_ID=$(jnum sessionId); echo "      sessionId=$SESSION_ID"

req GET "/agents/$AGENT_ID/sessions" "" "$TOKEN"
check_ok "会话列表"

req GET "/sessions/$SESSION_ID/messages" "" "$TOKEN"
check_ok "获取会话消息"

echo "===== 5. 工具/编排/监控/成本/审计 ====="
req GET "/tools?page=1&size=10" "" "$TOKEN"; check_ok "工具列表"
req GET "/workflows?page=1&size=10" "" "$TOKEN"; check_ok "工作流列表"
req GET "/monitor/overview?period=24h" "" "$TOKEN"; check_ok "监控总览(period=24h)"
req GET "/cost/overview" "" "$TOKEN"; check_ok "成本总览"
req GET "/cost/budgets" "" "$TOKEN"; check_ok "预算列表"
req GET "/audit-logs?page=1&size=10" "" "$TOKEN"; check_ok "审计日志"

echo "===== 6. 账户隔离验证 ====="
req POST /auth/register "{\"username\":\"$U2\",\"nickname\":\"Other Tester\",\"email\":\"$U2@test.com\",\"password\":\"Test@123456\"}"
check_ok "注册第二个账户"
req POST /auth/login "{\"username\":\"$U2\",\"password\":\"Test@123456\"}"
check_ok "登录第二个账户"
TOKEN2=$(jfield token)

req GET "/agents/$AGENT_ID" "" "$TOKEN2"
check_fail "其他账户访问他人 Agent 被拒"

req PUT "/agents/$AGENT_ID" "{\"name\":\"hacked\"}" "$TOKEN2"
check_fail "其他账户修改他人 Agent 被拒"

req GET "/knowledge-bases/$KB_ID" "" "$TOKEN2"
check_fail "其他账户访问他人知识库被拒"

req DELETE "/sessions/$SESSION_ID" "" "$TOKEN2"
check_fail "其他账户删除他人会话被拒"

echo "===== 7. 清理测试数据 ====="
req DELETE "/sessions/$SESSION_ID" "" "$TOKEN"; check_ok "删除会话"
req DELETE "/knowledge-bases/$KB_ID/documents/$DOC_ID" "" "$TOKEN"; check_ok "删除文档"
req DELETE "/knowledge-bases/$KB_ID" "" "$TOKEN"; check_ok "删除知识库"
req DELETE "/agents/$AGENT_ID" "" "$TOKEN"; check_ok "删除 Agent"

echo ""
echo "=================================="
echo "结果: $PASS 通过, $FAIL 失败"
[ $FAIL -eq 0 ] && echo "ALL PASS" || echo "HAS FAILURES"
