# Windows 团队协作搭建指南

> Vue 3 + Spring Boot + Redis + MySQL，Windows 环境，从零开始

---

## 一、协作工具链总览

```
┌──────────────────────────────────────────────────────────────────┐
│                         协作工具链                                │
├──────────┬──────────┬──────────┬──────────┬──────────┬──────────┤
│  代码托管  │  项目管理  │  即时通讯  │  文档协作  │  CI/CD  │  API联调 │
├──────────┼──────────┼──────────┼──────────┼──────────┼──────────┤
│  GitHub   │  GitHub   │  企业微信  │  语雀/   │  GitHub  │  Apifox/ │
│  /Gitee   │  Issues/  │  /飞书/   │  Notion/ │  Actions │  Postman │
│           │  Projects │  Slack    │  飞书文档 │          │          │
└──────────┴──────────┴──────────┴──────────┴──────────┴──────────┘
```

---

## 二、第一步：Git 版本控制

### 2.1 每个人安装 Git for Windows

```powershell
# 1. 下载安装 Git for Windows
#    https://git-scm.com/download/win
#
# 2. 安装时关键选项：
#    - 编辑器选 VS Code
#    - 默认分支名选 main
#    - PATH 选 "Git from the command line and also from 3rd-party software"
#    - 换行符选 "Checkout as-is, commit as-is"（非常重要！）

# 3. 安装后每个成员执行（替换为自己的信息）：
git config --global user.name "你的姓名"
git config --global user.email "你的邮箱@company.com"
git config --global core.autocrlf false   # 关键！避免 Windows 换行符灾难
git config --global pull.rebase true
git config --global init.defaultBranch main
```

### 2.2 创建项目仓库

```powershell
# 方案1：GitHub（推荐，免费私有仓库 + Actions CI/CD）
# 方案2：Gitee（国内访问更快）
# 方案3：自建 GitLab/Gitea（数据完全私有）

# 一个人在平台创建仓库后，添加队友为 Collaborator
```

### 2.3 初始化并推送

```powershell
# 项目负责人在 E:\agent_management 下执行：
cd E:\agent_management
git init
git add .
git commit -m "chore: 初始化项目结构"
git branch -M main
git remote add origin https://github.com/your-team/agent-management.git
git push -u origin main
```

### 2.4 其他人克隆

```powershell
git clone https://github.com/your-team/agent-management.git
cd agent-management
```

---

## 三、第二步：统一开发环境（核心）

### 3.1 Node.js（前端 Vue 3）— nvm-windows

```powershell
# 下载安装 nvm-windows：
# https://github.com/coreybutler/nvm-windows/releases → nvm-setup.exe

nvm install 20.18.0
nvm use 20.18.0
node -v      # 确认版本一致
```

项目根目录放 `.nvmrc`：
```
20.18.0
```

### 3.2 JDK（后端 Spring Boot）— 统一 JDK 版本

```powershell
# 推荐 JDK 17（Spring Boot 3.x 最低要求）或 JDK 21（最新 LTS）

# 方式1：winget 安装（简单）
winget install Microsoft.OpenJDK.21

# 方式2：手动下载安装
# https://adoptium.net/download/ → JDK 21 LTS → .msi 安装包

# 配置环境变量（MSI 安装包通常自动配置）
# 验证安装：
java -version
javac -version

# ⚠️ 如果装了多个 JDK，需要在环境变量中确保 JAVA_HOME 指向正确版本：
# 系统属性 → 环境变量 → 新建系统变量：
#   变量名：JAVA_HOME
#   变量值：C:\Program Files\Eclipse Adoptium\jdk-21.0.4.8-hotspot\
```

### 3.3 Maven（构建工具）— 统一构建配置

```powershell
# 下载 Maven：
# https://maven.apache.org/download.cgi → Binary zip archive

# 解压到 C:\tools\apache-maven-3.9.9
# 添加环境变量：
#   MAVEN_HOME = C:\tools\apache-maven-3.9.9
#   Path 追加 %MAVEN_HOME%\bin

# 验证：
mvn -version

# ⚠️ 关键：在项目 backend 目录的 pom.xml 中锁定 Java 版本（见项目结构章节）
# 这样所有人编译时版本一致，不会出现"你电脑能跑我电脑不行"
```

### 3.4 MySQL（本地开发数据库）— 统一安装

```powershell
# 方式1：MySQL Installer（推荐，带图形界面）
# https://dev.mysql.com/downloads/installer/
# 安装时记好：
#   - 端口：3306（默认）
#   - root 密码：团队约定一个统一的本地密码，如 admin123

# 方式2：Docker Desktop（更干净，不污染系统）
# 先安装 Docker Desktop：https://www.docker.com/products/docker-desktop/
docker run -d --name mysql-dev `
  -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=admin123 `
  -e MYSQL_DATABASE=agent_management `
  mysql:8.0

# 每个人的数据库名、用户名、密码保持一致
# 这样 application-dev.yml 可以提交到仓库
```

### 3.5 Redis（本地缓存）— 统一安装

```powershell
# 方式1：Docker Desktop（推荐）
docker run -d --name redis-dev -p 6379:6379 redis:7-alpine

# 方式2：Windows 原生安装
# Redis 官方不支持 Windows。用 Memurai（Redis Windows 兼容版）：
# https://www.memurai.com/get-memurai → Developer Edition（免费）
# 安装后自动作为 Windows 服务运行，端口 6379

# 方式3：WSL2 内安装
# wsl --install -d Ubuntu
# wsl sudo apt update && sudo apt install redis-server -y
```

### 3.6 IDE 统一：推荐 IntelliJ IDEA + VS Code

```
前后端都用 VS Code → 装 Java 插件包即可，轻量
但 Spring Boot 开发体验 IntelliJ IDEA Community 版（免费）更好

推荐分工：
  ✅ 前端开发者 → VS Code + Volar 插件
  ✅ 后端开发者 → IntelliJ IDEA Community + Spring Boot Helper 插件
  ✅ 全栈开发者 → IntelliJ IDEA + VS Code 都装
```

#### VS Code 共享配置 `.vscode/extensions.json`：

```json
{
  "recommendations": [
    "Vue.volar",
    "dbaeumer.vscode-eslint",
    "esbenp.prettier-vscode",
    "redhat.java",
    "vscjava.vscode-maven",
    "vscjava.vscode-spring-boot-dashboard",
    "vmware.vscode-boot-dev-pack",
    "eamodio.gitlens",
    "github.vscode-pull-request-github",
    "streetsidesoftware.code-spell-checker"
  ]
}
```

#### `.vscode/settings.json`：

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.tabSize": 2,
  "files.eol": "\n",
  "[vue]": {
    "editor.defaultFormatter": "Vue.volar"
  },
  "[java]": {
    "editor.tabSize": 4,
    "editor.defaultFormatter": "redhat.java"
  },
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "C:/Program Files/Eclipse Adoptium/jdk-21.0.4.8-hotspot"
    }
  ],
  "java.compile.nullAnalysis.mode": "automatic",
  "files.exclude": {
    "**/.classpath": true,
    "**/.project": true,
    "**/.settings": true,
    "**/.factorypath": true
  }
}
```

---

## 四、第三步：项目结构（Vue 3 + Spring Boot）

```
agent-management/
├── .github/
│   ├── workflows/
│   │   └── ci.yml
│   └── PULL_REQUEST_TEMPLATE.md
├── .vscode/                    # VS Code 共享配置
│   ├── extensions.json
│   └── settings.json
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── api/               # 接口请求封装
│   │   ├── components/        # 公共组件
│   │   ├── views/             # 页面
│   │   ├── router/            # 路由
│   │   ├── stores/            # Pinia 状态管理
│   │   └── utils/             # 工具函数
│   ├── package.json
│   ├── pnpm-lock.yaml
│   ├── vite.config.ts
│   └── .env.development       # 开发环境变量（可提交）
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/agent/
│   │   ├── controller/        # 接口层
│   │   ├── service/           # 业务逻辑层
│   │   ├── repository/        # 数据访问层（MyBatis-Plus Mapper）
│   │   ├── entity/            # 数据库实体
│   │   ├── dto/               # 数据传输对象
│   │   ├── config/            # 配置类
│   │   └── common/            # 公共类（统一返回、异常处理等）
│   ├── src/main/resources/
│   │   ├── application.yml           # 主配置
│   │   ├── application-dev.yml       # 开发环境（可提交，统一本地配置）
│   │   └── application-prod.yml      # 生产环境（敏感信息不提交）
│   ├── src/test/java/         # 测试
│   ├── pom.xml                # Maven 依赖配置
│   └── sql/                   # 数据库初始化脚本
│       └── init.sql
├── docs/                      # 项目文档
│   ├── agent-knowledge.md
│   ├── api-design.md
│   └── architecture.md
├── .gitignore
├── .gitattributes
├── .editorconfig
└── README.md
```

### 4.1 Maven pom.xml 关键配置（统一 JDK 版本）

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

### 4.2 application-dev.yml（统一本地配置，可提交到仓库）

```yaml
# 这个文件提交到 Git，所有人本地开发环境一致
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/agent_management?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: admin123      # 约定统一本地密码
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
      # 本地 Redis 不用密码

server:
  port: 8080
```

> ⚠️ **生产环境密码**放在 `application-prod.yml` 中，该文件**不提交到 Git**，通过 CI/CD 注入或运维手动管理。

### 4.3 前端 .env.development（统一接口地址，可提交）

```env
# 后端接口地址
VITE_API_BASE_URL=http://localhost:8080

# 应用标题
VITE_APP_TITLE=Agent 管理系统
```

### 4.4 .gitignore

```gitignore
# ===== Node / Vue =====
node_modules/
dist/
.env.local
.env.*.local
*.tsbuildinfo

# ===== Java / Maven =====
target/
!.mvn/wrapper/maven-wrapper.jar
*.class
*.jar
*.war
*.log

# ===== IDE =====
.idea/
.classpath
.project
.settings/
.factorypath
*.swp
*.swo

# ===== 敏感配置 =====
application-prod.yml
application-prod.properties
*.jks
*.p12

# ===== 环境变量 =====
.env
.env.*
!.env.example
!.env.development

# ===== 构建产物 =====
build/
*.log
coverage/
hs_err_pid*

# ===== OS =====
.DS_Store
Thumbs.db
Desktop.ini
```

---

## 五、第四步：Git 分支协作规范

### 5.1 分支模型（简化 Git Flow）

```
main ─────────────────●─────●──────●─ 生产分支，只合并不直接提交
                       \   / \    /
develop ────────────────●──●───●──●─ 日常开发主分支
                         \     /
feature/xxx ──────────────●───●──── 功能分支
                          \
fix/xxx ───────────────────●──────── Bug 修复分支
```

### 5.2 分支命名

```bash
feature/agent-list          # 新功能
fix/token-calc-error        # Bug 修复
refactor/extract-common     # 重构
docs/api-design             # 文档
chore/update-spring-boot    # 依赖升级
```

### 5.3 每人每天的完整工作流

```powershell
# ===== 早上：同步最新代码 =====
git checkout develop
git pull origin develop

# ===== 开始新功能 =====
git checkout -b feature/agent-create develop

# ===== 开发过程中频繁小步提交 =====
git add .
git commit -m "feat: Agent 创建页面基本布局"

git add .
git commit -m "feat: 对接 Agent 创建接口，联调通过"

# ===== 推送前：同步 develop 最新代码 =====
git fetch origin
git rebase origin/develop
# 如果有冲突 → 手动解决 → git add . → git rebase --continue

# ===== 推送到远程 =====
git push origin feature/agent-create

# ===== 去 GitHub 创建 Pull Request =====
# → 等队友 Review → CI 全绿 → 合并到 develop
```

### 5.4 Commit Message 规范

```
<type>: <中文简述>

type：
  feat     — 新功能
  fix      — 修复 bug
  docs     — 文档
  style    — 格式化（不影响逻辑）
  refactor — 重构
  test     — 测试
  chore    — 构建/依赖变更

示例：
  feat: Agent 创建页面
  fix: 修复 Redis 连接超时未处理异常
  docs: 补充后端接口文档
  refactor: 抽取公共响应拦截器
```

---

## 六、第五步：Code Review 流程

### 6.1 PR 模板（`.github/PULL_REQUEST_TEMPLATE.md`）

```markdown
## 变更内容
<!-- 简述这个 PR 做了什么 -->

## 测试方式
<!-- 如何验证？Postman 测试了哪个接口 / 页面上做了什么操作 -->

## 截图/录屏（前端变更必填）
<!-- 拖入截图 -->

## 关联 Issue
Closes #xxx
```

### 6.2 Review 规则

```
✅ 至少 1 人 Approve 才能合并
✅ CI（前端 lint + 后端编译测试）全部通过才能合并
✅ PR 不要太大（> 400 行建议拆分）
✅ 合并方式：Squash and Merge（压缩成一个干净的 commit）

后端 Review 要点：
  □ 接口有没有做参数校验？
  □ SQL 有没有用参数化查询（防注入）？
  □ 事务边界是否正确？
  □ 异常处理是否统一？
  □ 有没有 N+1 查询问题？

前端 Review 要点：
  □ 组件是否可复用？
  □ 状态管理是否合理（该放 Pinia 的别放组件里）？
  □ 接口异常时 UI 是否有反馈？
  □ 样式是否响应式？
```

---

## 七、第六步：CI/CD（GitHub Actions）

### 7.1 创建 `.github/workflows/ci.yml`

```yaml
name: CI

on:
  pull_request:
    branches: [develop, main]
  push:
    branches: [develop, main]

jobs:
  # ==================== 前端检查 ====================
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: pnpm/action-setup@v2
        with:
          version: 9

      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: 'pnpm'
          cache-dependency-path: frontend/pnpm-lock.yaml

      - name: 安装依赖
        run: cd frontend && pnpm install --frozen-lockfile

      - name: ESLint 检查
        run: cd frontend && pnpm lint

      - name: TypeScript 类型检查
        run: cd frontend && pnpm type-check

      - name: 前端测试
        run: cd frontend && pnpm test --run

      - name: 构建检查（确保能成功打包）
        run: cd frontend && pnpm build

  # ==================== 后端检查 ====================
  backend:
    runs-on: ubuntu-latest

    services:
      # CI 中启动临时 MySQL 供测试使用
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test123
          MYSQL_DATABASE: agent_management_test
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=3

      # CI 中启动临时 Redis
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd="redis-cli ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=3

    steps:
      - uses: actions/checkout@v4

      - name: 设置 JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Maven 编译 + 测试
        run: cd backend && mvn clean test --batch-mode
        env:
          SPRING_DATASOURCE_URL: jdbc:mysql://localhost:3306/agent_management_test?useUnicode=true&characterEncoding=utf-8
          SPRING_DATASOURCE_USERNAME: root
          SPRING_DATASOURCE_PASSWORD: test123
          SPRING_REDIS_HOST: localhost
          SPRING_REDIS_PORT: 6379

      - name: 测试报告
        if: always()
        run: |
          echo "测试报告见 backend/target/surefire-reports/"
```

---

## 八、日常协作节奏

| 时间 | 动作 |
|------|------|
| **早上** | `git pull origin develop`，确认今天任务（看板 Issues） |
| **开发中** | 在自己 feature 分支工作，频繁小步提交 |
| **午饭前** | `git push` 推送到远程，防止意外丢失 |
| **下午** | 继续开发 + Review 队友的 PR |
| **下班前** | 确保分支已推送，更新看板状态 |

### 看板（GitHub Projects）

```
📋 Backlog      →  需求池
🚀 This Sprint  →  本次迭代要做
🏗 In Progress  →  开发中
👀 In Review    →  PR 已提交，等人 Review
✅ Done         →  已合并到 develop
```

---

## 九、常见 Windows 坑及解决方案

### 9.1 换行符灾难（最常见的协作问题）

```powershell
# 症状：整个文件标绿（Git 认为全部改动），或 .sh 脚本在 Linux 上跑不了
# 根治：项目根目录创建 .gitattributes

* text=auto
*.js    text eol=lf
*.ts    text eol=lf
*.vue   text eol=lf
*.json  text eol=lf
*.yaml  text eol=lf
*.yml   text eol=lf
*.xml   text eol=lf
*.java  text eol=lf
*.md    text eol=lf
*.sql   text eol=lf
*.sh    text eol=lf
*.bat   text eol=crlf
*.ps1   text eol=crlf
```

### 9.2 MySQL 时区问题

```yaml
# Spring Boot 连接字符串务必加上时区参数：
spring.datasource.url: jdbc:mysql://localhost:3306/agent_management?serverTimezone=Asia/Shanghai
```

```sql
-- 或者直接设置 MySQL 全局时区（团队中一个人执行一次即可）：
SET GLOBAL time_zone = '+08:00';
```

### 9.3 Maven 下载慢

```xml
<!-- pom.xml 中添加阿里云镜像，所有人统一配置 -->
<!-- 或者每个人的 ~/.m2/settings.xml 中配置 -->
<mirrors>
  <mirror>
    <id>aliyun</id>
    <name>Aliyun Maven Mirror</name>
    <url>https://maven.aliyun.com/repository/public</url>
    <mirrorOf>central</mirrorOf>
  </mirror>
</mirrors>
```

### 9.4 后端端口冲突

```powershell
# 8080 端口被占用时：
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# 或者个人在 application-dev.yml 中改端口（不要提交此修改）
```

### 9.5 前端 Vite 代理问题

```typescript
// vite.config.ts — 配置代理，解决跨域
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // 后端地址
        changeOrigin: true,
      }
    }
  }
})
```

### 9.6 冲突解决

```powershell
# rebase 冲突时：
git status                # 查看冲突文件
# 手动编辑文件，删掉 <<<<<<< ======= >>>>>>> 标记，保留正确代码
git add .
git rebase --continue

# 想放弃 rebase：
git rebase --abort
```

---

## 十、新成员第一天 Onboarding（复制粘贴版）

```powershell
# ==================== 一次性安装 ====================

# 1. Git
winget install Git.Git
git config --global user.name "你的姓名"
git config --global user.email "你的邮箱"
git config --global core.autocrlf false

# 2. Node.js（nvm-windows 需要手动下载安装）
# https://github.com/coreybutler/nvm-windows/releases → nvm-setup.exe
nvm install 20.18.0
nvm use 20.18.0

# 3. pnpm
npm install -g pnpm

# 4. JDK 21
winget install Microsoft.OpenJDK.21
# 安装后重启终端，验证：
java -version

# 5. Maven
# https://maven.apache.org/download.cgi → 下载 zip
# 解压到 C:\tools\apache-maven-3.9.9
# 环境变量 Path 追加 C:\tools\apache-maven-3.9.9\bin
# 验证：
mvn -version

# 6. MySQL（用 Docker 最省事，或下载 MySQL Installer）
# 先装 Docker Desktop：https://www.docker.com/products/docker-desktop/
docker run -d --name mysql-dev -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=admin123 `
  -e MYSQL_DATABASE=agent_management `
  mysql:8.0

# 7. Redis
docker run -d --name redis-dev -p 6379:6379 redis:7-alpine

# 8. VS Code（插件会在打开项目时自动推荐安装）
winget install Microsoft.VisualStudioCode

# ==================== 拉项目 ====================
git clone https://github.com/your-team/agent-management.git
cd agent-management

# ==================== 初始化数据库表 ====================
# 用 DBeaver/Navicat 连接 localhost:3306 的 MySQL
# 执行 backend/sql/init.sql

# ==================== 跑起来 ====================
# 终端1 — 后端
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 终端2 — 前端
cd frontend
pnpm install
pnpm dev

# 浏览器打开 http://localhost:5173 看到页面就是成功了 🎉
```

---

## 附：工具清单

| 类别 | 推荐工具 | 用途 |
|------|---------|------|
| 代码托管 | GitHub / Gitee | 仓库 + PR + Issues |
| 项目管理 | GitHub Projects | 看板、迭代规划 |
| 即时通讯 | 企业微信 / 飞书 | 日常沟通 + Git 机器人通知 |
| 文档协作 | 语雀 / Notion | API 文档、会议记录、技术方案 |
| CI/CD | GitHub Actions | 自动编译、测试 |
| API 调试 | Apifox（推荐）/ Postman | 接口文档 + Mock + 联调 |
| 数据库管理 | DBeaver（免费）/ Navicat | MySQL 数据查看与管理 |
| Redis 查看 | RedisInsight（免费）/ Another Redis Desktop Manager | Redis 数据浏览 |
| 设计 | Figma / 墨刀 | UI 设计协作 |
