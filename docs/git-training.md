# Git 团队培训教材

> 适合：完全零基础 → 能独立参与团队协作 | 平台：GitHub | 环境：Windows

---

## 目录

- [学前自测：你真的需要 Git 吗？](#学前自测你真的需要-git-吗)
- [第一部分：理解 Git](#第一部分理解-git)
  - [1.1 没有 Git 的世界是什么样的](#11-没有-git-的世界是什么样的)
  - [1.2 Git 解决了什么问题](#12-git-解决了什么问题)
  - [1.2 Git 的核心概念（用生活类比）](#12-git-的核心概念用生活类比)
- [第二部分：安装与配置](#第二部分安装与配置)
  - [2.1 安装 Git for Windows](#21-安装-git-for-windows)
  - [2.2 你是谁——身份配置](#22-你是谁身份配置)
  - [2.3 换行符配置（Windows 必做）](#23-换行符配置windows-必做)
- [第三部分：第一次提交](#第三部分第一次提交)
  - [3.1 把一个项目变成 Git 仓库](#31-把一个项目变成-git-仓库)
  - [3.2 你的第一个 commit](#32-你的第一个-commit)
  - [3.3 理解暂存区](#33-理解暂存区)
- [第四部分：查看与回溯](#第四部分查看与回溯)
  - [4.1 查看状态与历史](#41-查看状态与历史)
  - [4.2 版本回退](#42-版本回退)
  - [4.3 文件级操作](#43-文件级操作)
- [第五部分：分支——Git 的灵魂](#第五部分分支git-的灵魂)
  - [5.1 为什么需要分支](#51-为什么需要分支)
  - [5.2 创建与切换分支](#52-创建与切换分支)
  - [5.3 合并分支（merge）](#53-合并分支-merge)
  - [5.4 冲突是怎么发生的、怎么解决](#54-冲突是怎么发生的怎么解决)
- [第六部分：远程协作——GitHub](#第六部分远程协作github)
  - [6.1 注册 GitHub 并创建远程仓库](#61-注册-github-并创建远程仓库)
  - [6.2 推送与拉取](#62-推送与拉取)
  - [6.3 克隆仓库](#63-克隆仓库)
  - [6.4 多人协作模拟](#64-多人协作模拟)
- [第七部分：Pull Request 工作流](#第七部分pull-request-工作流)
  - [7.1 什么是 PR 和 Code Review](#71-什么是-pr-和-code-review)
  - [7.2 完整的 PR 流程](#72-完整的-pr-流程)
- [第八部分：团队规范](#第八部分团队规范)
  - [8.1 分支命名规范](#81-分支命名规范)
  - [8.2 提交信息规范（Conventional Commits）](#82-提交信息规范conventional-commits)
  - [8.3 分支策略（Git Flow 精简版）](#83-分支策略git-flow-精简版)
- [第九部分：常用场景速查](#第九部分常用场景速查)
- [第十部分：进阶技能](#第十部分进阶技能)
  - [10.1 stash——暂存手头工作](#101-stash暂存手头工作)
  - [10.2 rebase——整理提交历史](#102-rebase整理提交历史)
  - [10.3 cherry-pick——挑选提交](#103-cherry-pick挑选提交)
  - [10.4 reflog——救命稻草](#104-reflog救命稻草)
- [附录：练习与自查](#附录练习与自查)

---

## 学前自测：你真的需要 Git 吗？

先做一个简单的思维实验：

> 你正在做一个项目，改了 5 个文件。三天后，老板说："上周那个版本更好，能改回去吗？"

**没有 Git 你会怎么做？**

```
方案 A：每次改之前把整个文件夹复制一份 →
  ├── agent-management-20260701/
  ├── agent-management-20260702/
  ├── agent-management-20260703-最终版/
  ├── agent-management-20260703-最终版v2/
  └── agent-management-20260703-真正的最终版/

方案 B：凭记忆把改过的代码一行一行删回去
方案 C：和老板说"做不到了"
```

**方案 A 的问题**：一个月后你自己也分不清哪个是哪个；同事想要你的改动，你得把整个文件夹传给他。

这就是 Git 要解决的问题。

---

## 第一部分：理解 Git

### 1.1 没有 Git 的世界是什么样的

用你们的项目举例。假设张三和李四同时改 `agent-knowledge.md`：

- 张三在文件开头加了一段"Agent 的定义"
- 李四在文件开头加了一段"Agent 的历史"

两人都改完了，现在要把改动合到一起。没有 Git，只能：
1. 把两个人的文件放在一起
2. 肉眼对比每一行
3. 手动拼出一个新文件
4. 祈祷没有漏掉谁的改动

这个过程痛苦、易出错、不可逆。

### 1.2 Git 解决了什么问题

Git 本质上是一个**版本控制工具**，它解决四件事：

| 问题 | Git 的答案 |
|------|-----------|
| 代码改坏了想回退 | `git log` 看历史，`git reset` 回退到任意版本 |
| 多人同时改一个文件 | 自动合并 + 冲突提示 |
| 不知道谁改了哪行、为什么改 | `git blame` 每一行都有记录 |
| 新功能写到一半，线上出 bug 要紧急修复 | 切分支，互不干扰 |

### 1.3 Git 的核心概念（用生活类比）

Git 里面最核心的四个概念，用**写论文**来类比：

```
你的论文文件夹  =  仓库 (Repository)
每次保存        =  一次提交 (Commit)
同时写两个版本  =  分支 (Branch)
把论文发给导师  =  推送 (Push) 到远程仓库 (Remote)
```

**仓库 (Repository)**：就是一个被 Git 管理的文件夹。里面的每一个变化都被记录下来。

**提交 (Commit)**：你对文件做的一次"快照"。Git 不是记录"改了哪里"，而是每次都把**整个项目的状态拍一张照片**存下来。别担心，它很聪明，没变的文件只存一个指针，不占空间。

**分支 (Branch)**：一条独立的开发线。就像你写论文时，"初稿"是一个版本，"加了图表"是另一个版本，两个可以同时存在、互不干扰，最后再合并。

**远程 (Remote)**：放在 GitHub / Gitee 等服务器上的仓库副本。团队的"中央"版本。

---

```
┌─────────────────────────────────────────────────────────────┐
│                      你的电脑                                │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │ 工作目录  │ →  │  暂存区   │ →  │ 本地仓库  │              │
│  │(Working) │    │(Staging) │    │  (.git)  │              │
│  │ 写代码    │    │ git add  │    │git commit│              │
│  └──────────┘    └──────────┘    └────┬─────┘              │
│                                       │                    │
│                                  git push / git pull        │
│                                       │                    │
└───────────────────────────────────────┼────────────────────┘
                                        │
                              ┌─────────▼─────────┐
                              │     GitHub 远程     │
                              │     仓库 (Remote)   │
                              └───────────────────┘
```

> **记忆口诀**：「工作区写好代码 → `add` 放进暂存区 → `commit` 保存到本地仓库 → `push` 上传到 GitHub」

---

## 第二部分：安装与配置

### 2.1 安装 Git for Windows

```powershell
# 1. 下载：https://git-scm.com/download/win
#    选 64-bit Git for Windows Setup

# 2. 安装时注意这几个选项（其他都默认即可）：
#    ✅ 编辑器：选 "Use Visual Studio Code as Git's default editor"
#    ✅ 默认分支名：选 "Override the default branch name" → 填 "main"
#    ✅ PATH 环境：选 "Git from the command line and also from 3rd-party software"
#    ✅ 换行符：选 "Checkout as-is, commit as-is"（这步很重要！）
#    ✅ 终端模拟器：选 "Use Windows' default console window"

# 3. 验证安装成功：
git --version
# 应该显示类似：git version 2.47.0.windows.1
```

### 2.2 你是谁——身份配置

Git 会在每一次提交里记录**谁**做了什么。先告诉 Git 你是谁：

```powershell
git config --global user.name "张三"
git config --global user.email "zhangsan@company.com"
```

> ⚠️ 这个邮箱要和 GitHub 注册的邮箱**一致**，否则你的提交头像不会显示！

检查配置是否成功：

```powershell
git config --global --list
```

### 2.3 换行符配置（Windows 必做）

Windows 和 Mac/Linux 的换行符不一样，这是团队协作中最常见的坑：

```
Windows 换行符：\r\n  (CRLF)
Mac/Linux 换行符：\n  (LF)
```

如果不配置，你和同事之间每次 pull/push 都会产生一堆"改了一整文件"的假变动。

```powershell
# Windows 上执行：提交时保留原来的，拉取时不转换
git config --global core.autocrlf false

# 拉取时用 rebase 而不是 merge（保持历史干净）
git config --global pull.rebase true

# 默认主分支名用 main
git config --global init.defaultBranch main
```

---

## 第三部分：第一次提交

### 3.1 把一个项目变成 Git 仓库

现在用你们的真实项目来练手。进入项目根目录：

```powershell
cd E:\agent_management

# 初始化——这一条命令就把当前目录变成了一个 Git 仓库
git init
# 输出：Initialized empty Git repository in E:/agent_management/.git
```

> `.git` 文件夹是 Git 的一切——所有的历史、分支、配置都在里面。**千万不要删它。**

### 3.2 你的第一个 commit

一条命令一条命令跟着做，**别复制粘贴**，手打一遍才记得住：

```powershell
# 第 1 步：看看当前状态
git status
# 你会看到一堆红色的文件名——这些都是"还没被 Git 跟踪"的文件

# 第 2 步：把文件放进暂存区
git add .
# . 表示「当前目录下所有文件」

# 第 3 步：再看看状态
git status
# 现在文件名变绿了——它们已经在暂存区，等着被提交

# 第 4 步：提交！
git commit -m "chore: 初始化项目结构"
# -m 后面是提交信息——用一句话说清楚这次改了什么
```

恭喜！你完成了人生第一个 Git 提交。🎉

### 3.3 理解暂存区

"为什么不能直接 `git commit`，非要先 `git add`？"

这是一个很好的设计。暂存区让你**选择哪些改动放进这一次提交**，而不是一股脑全交。

```
场景：你同时改了 5 个文件
  - 3 个是「修复登录 bug」
  - 2 个是「新增用户头像功能」（还没写完）

这时候：
  git add 登录相关的3个文件
  git commit -m "fix: 修复登录失败的问题"
  
  另外 2 个文件先不改，等到头像功能写完再 add + commit
```

**一个好 commit 的原则**：一次提交只做一件事。

---

## 第四部分：查看与回溯

### 4.1 查看状态与历史

```powershell
# 看哪些文件改了、哪些还没 add
git status

# 看具体改了哪些行
git diff
# 按 q 退出

# 看提交历史
git log
# 输出：
# commit a1b2c3d4e5f6...  ← 这是 commit 的身份证号（SHA-1 哈希）
# Author: 张三 <zhangsan@company.com>
# Date:   Mon Jul 14 2026 10:30:00
#
#     chore: 初始化项目结构

# 简洁版历史（推荐日常用这个）：
git log --oneline --graph --all
```

### 4.2 版本回退

**场景 1："我刚才的 commit 写错了，想改提交信息"**

```powershell
git commit --amend -m "chore: 正确的提交信息"
# ⚠️ 只能改最近一次 commit，而且必须还没 push 到 GitHub
```

**场景 2："我改了一堆东西，想全部丢弃，回到上次 commit 的状态"**

```powershell
git checkout .
# 或更明确的写法：
git restore .
```

> ⚠️ 这个操作**不可逆**——丢弃的改动找不回来。执行前先确认。

**场景 3："我想回到 3 个 commit 之前的状态"**

```powershell
# 先看历史，找到想回到的那个 commit 的 ID
git log --oneline

# 方式 A：回到过去，但保留现在的改动在工作区（安全）
git reset --soft <commit-id>

# 方式 B：回到过去，丢弃所有改动（危险，需确认）
git reset --hard <commit-id>
```

| reset 类型 | 工作区的改动 | 暂存区的改动 | 提交历史 |
|-----------|-------------|-------------|---------|
| `--soft` | ✅ 保留 | ✅ 保留 | 回退到指定 commit |
| `--mixed`（默认）| ✅ 保留 | ❌ 清空 | 回退到指定 commit |
| `--hard` | ❌ **丢弃** | ❌ **丢弃** | 回退到指定 commit |

### 4.3 文件级操作

```powershell
# 只 add 某个文件
git add src/main.js

# 把一个文件从暂存区撤回来（保留你的改动）
git restore --staged src/main.js

# 查看某个文件每一行是谁在什么时候改的
git blame src/main.js

# 删除一个文件（Git 会同时记录这个删除操作）
git rm 旧文件.txt
```

---

## 第五部分：分支——Git 的灵魂

### 5.1 为什么需要分支

回到你们的项目场景：

```
main 分支：线上正在运行的稳定版本

你要开发「Agent 配置中心」这个新功能 →
  创建一个 feature/agent-config 分支 →
  在这个分支上随便改，不会影响 main →
  改完了、测试没问题 →
  合并回 main
```

没有分支的话，你的半成品代码随时可能把同事的环境搞崩。有了分支，每个人在自己的分支上工作，互不干扰。

### 5.2 创建与切换分支

```powershell
# 当前在哪个分支？
git branch
# * main   ← * 表示当前分支

# 创建一个新分支
git branch feature/add-agent-workspace

# 切换到新分支
git checkout feature/add-agent-workspace

# 快捷方式：创建 + 切换一条命令
git checkout -b feature/add-agent-workspace

# 改点东西，然后提交
# ... 编辑文件 ...
git add .
git commit -m "feat: 新增 Agent 工作台页面"

# 切回 main
git checkout main
# 你会发现刚才的改动「消失」了——因为它们属于另一个分支，互不干扰
```

**分支的本质**：一个指向某个 commit 的指针。创建分支极其轻量，几乎是瞬间完成。所以**多开分支是好习惯**。

### 5.3 合并分支（merge）

你的功能开发完了，该合并回 main 了：

```powershell
# 先切换到目标分支（要把东西合并到的分支）
git checkout main

# 把 feature 分支合并进来
git merge feature/add-agent-workspace

# 合并成功！
# 输出：
# Updating a1b2c3d..e4f5g6h
# Fast-forward   ← 这说明合并很顺利，没有冲突
```

**Fast-forward 合并**：当 main 分支在你创建 feature 分支后没有新提交，Git 只需要把 main 的指针往前移——这是最简单的合并。

**三方合并**：当 main 和 feature 分支**都**有新提交时：

```
      main:  A → B → C → D
                          ↘
      feature:      E → F → G

合并时 Git 需要对比 B（分叉点）、D（main 末端）、G（feature 末端）三个版本
自动判断：谁的改动保留？哪里有冲突？
```

### 5.4 冲突是怎么发生的、怎么解决

**冲突不可怕**——它是 Git 在说："你们俩改了同一个地方，我不敢替你做决定。"

**制造一个冲突来练习：**

```powershell
# 第 1 步：从 main 创建两个分支
git checkout -b branch-a
# 编辑 agent-knowledge.md，在第一行加：## Branch A 的标题
git add . && git commit -m "A的改动"

git checkout main
git checkout -b branch-b
# 编辑 agent-knowledge.md，在第一行加：## Branch B 的标题
git add . && git commit -m "B的改动"

# 第 2 步：合并 A，然后尝试合并 B
git checkout main
git merge branch-a   # ✅ 顺利
git merge branch-b   # ❌ 冲突！
```

**解决冲突：**

Git 会在冲突文件里标注：

```
<<<<<<< HEAD
## Branch A 的标题
=======
## Branch B 的标题
>>>>>>> branch-b
```

你的任务：
1. 打开冲突文件
2. 决定保留哪个（或两个都保留，或重新写）
3. 删除 `<<<<<<<` `=======` `>>>>>>>` 这些标记
4. 保存文件

```powershell
# 标记冲突已解决
git add .
git commit -m "merge: 合并 branch-a 和 branch-b，解决标题冲突"

# 完成！
```

> **VS Code 小技巧**：VS Code 会自动高亮冲突区域，顶部有按钮让你一键选择 "Accept Current" / "Accept Incoming" / "Accept Both"。

---

## 第六部分：远程协作——GitHub

### 6.1 注册 GitHub 并创建远程仓库

**第 1 步**：每个人去 https://github.com 注册账号。

**第 2 步**：项目负责人创建一个仓库：
- 点击右上角 `+` → `New repository`
- Repository name：`agent-management`
- 选 **Private**（你们公司的代码不能公开）
- **不要**勾选 "Add a README file"（你们已经有代码了）
- 点击 `Create repository`

**第 3 步**：关联本地仓库到远程：

```powershell
cd E:\agent_management

# 添加远程仓库（把下面的 URL 换成你们自己的）
git remote add origin https://github.com/your-team/agent-management.git

# 看看远程配置
git remote -v
# origin  https://github.com/your-team/agent-management.git (fetch)
# origin  https://github.com/your-team/agent-management.git (push)

# 第一次推送
git push -u origin main
# -u 表示「记住这个对应关系」，以后直接 git push 就行
```

### 6.2 推送与拉取

日常使用就两条核心命令：

```powershell
# 把你的本地提交推送到 GitHub
git push

# 把 GitHub 上别人的提交拉到本地
git pull
```

**一个典型的日常工作流：**

```powershell
# 早上到公司
git pull                  # 拉取昨晚同事推的代码

# 开始干活
git checkout -b feature/xxx
# ... 写代码 ...
git add .
git commit -m "feat: 完成 xxx 功能"

# 推送你的分支到 GitHub
git push -u origin feature/xxx
# 然后去 GitHub 创建 Pull Request
```

### 6.3 克隆仓库

团队其他成员第一次获取项目：

```powershell
# 到你想放项目的目录
cd E:\

# 克隆
git clone https://github.com/your-team/agent-management.git

# 进入项目
cd agent-management

# 你已经有了完整的项目 + 完整的 Git 历史
git log --oneline
```

### 6.4 多人协作模拟

两个人一起练一遍：

| 步骤 | 张三 | 李四 |
|------|------|------|
| 1 | `git checkout -b feat/zhang` | `git checkout -b feat/li` |
| 2 | 改 `agent-knowledge.md` 加一行 | 改 `agent-knowledge.md` 加一行 |
| 3 | `git add . && git commit` | `git add . && git commit` |
| 4 | `git push -u origin feat/zhang` | `git push -u origin feat/li` |
| 5 | GitHub 上创建 PR → 合并 | GitHub 上创建 PR → 发现冲突 |

李四在合并时会遇到冲突——这就是 5.4 节的实战版本。李四需要在 GitHub 网页上（或本地）解决冲突后再合并。

---

## 第七部分：Pull Request 工作流

### 7.1 什么是 PR 和 Code Review

**Pull Request (PR)**：一个"请把我的改动拉取到主分支"的请求。

它不是 Git 的原生功能，是 GitHub 提供的**协作机制**：

```
张三：我写完了「Agent 配置中心」功能，推到了 GitHub 上的 feature/agent-config 分支
张三：我创建一个 PR，请求把这个分支合并到 main
张三：@李四 @王五 请帮忙 Code Review

李四：看了代码，提了 3 条建议
张三：修改后再次推送，PR 自动更新

王五：代码没问题，Approve（批准）
张三：点击 Merge，合并到 main
```

**Code Review 不是找茬**——是互相学习、提前发现 bug、保持代码风格一致。

### 7.2 完整的 PR 流程

```
┌─────────────────────────────────────────────────────────────────┐
│  1. 本地开发                                                     │
│     git checkout -b feature/agent-config                        │
│     写代码... 改代码... 测试...                                    │
│     git add .                                                    │
│     git commit -m "feat: 实现 Agent 配置中心"                      │
│                                                                  │
│  2. 推送分支                                                     │
│     git push -u origin feature/agent-config                      │
│                                                                  │
│  3. 创建 PR                                                      │
│     去 GitHub → Pull requests → New pull request                 │
│     base: main ← compare: feature/agent-config                  │
│     写清楚标题和描述：「做了什么、为什么这样做、怎么测试」            │
│     → Create pull request                                       │
│                                                                  │
│  4. Code Review                                                  │
│     同事看代码 → 提 Comment / Approve / Request Changes           │
│     你根据意见修改 → git add . → git commit → git push           │
│     （PR 会自动更新，不需要重新创建）                                │
│                                                                  │
│  5. 合并                                                         │
│     所有人 Approve 后 → 点击 Merge pull request                  │
│     选 "Squash and merge" 或 "Create a merge commit"             │
│                                                                  │
│  6. 清理                                                         │
│     git checkout main                                           │
│     git pull                      # 拉取合并后的最新代码          │
│     git branch -d feature/agent-config   # 删除本地分支           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 第八部分：团队规范

### 8.1 分支命名规范

统一命名，团队一看就知道这个分支在做什么：

```
feature/<功能名>    新功能开发    例: feature/agent-workspace
fix/<问题名>       修复 bug      例: fix/login-timeout
hotfix/<问题名>    紧急线上修复   例: hotfix/payment-crash
docs/<内容>        文档更新      例: docs/api-reference
refactor/<模块>    代码重构      例: refactor/user-service
```

### 8.2 提交信息规范（Conventional Commits）

统一的提交信息格式，让 `git log` 变得可读：

```
<类型>: <简短描述>

类型必须是下面之一：
  feat     新功能
  fix      修复 bug
  docs     文档改动
  style    代码格式（空格、分号等，不影响逻辑）
  refactor 重构（既不是新功能也不是修 bug）
  perf     性能优化
  test     测试相关
  chore    杂务（构建、依赖、配置等）

好的提交信息 ✅：
  feat: 新增 Agent 工作台页面
  fix: 修复登录超时后白屏的问题
  docs: 补充 API 接口文档
  refactor: 提取公共验证逻辑到 utils

不好的提交信息 ❌：
  update
  改了一下
  fix bug
  WIP
  .
```

### 8.3 分支策略（Git Flow 精简版）

完整 Git Flow 太重了，推荐一个精简版，适合 2-10 人团队：

```
main
  │
  ├── 永远是可以部署的稳定代码
  ├── 只通过 PR 合并，不允许直接推送
  │
  ├── feature/xxx  ← 从 main 分出，合并回 main
  │     └── 一个功能一个分支，完成后合并并删除
  │
  ├── fix/xxx      ← 从 main 分出，合并回 main
  │     └── 修 bug 也用分支，和功能开发一样的流程
  │
  └── hotfix/xxx   ← 紧急修复，从 main 分出，修完马上合并
```

**三条铁律**：
1. **永远不要直接往 main 分支 push**——所有改动通过 PR 合并
2. **一个分支只做一件事**——功能分支不要夹带修 bug
3. **合并完就删分支**——保持仓库干净

---

## 第九部分：常用场景速查

### 场景 1：我在 master 分支上不小心改了代码，还没 commit

```powershell
# 把改动暂存起来
git stash

# 创建并切换到正确的分支
git checkout -b feature/xxx

# 把刚才的改动拿出来
git stash pop
```

### 场景 2：commit 到错误的分支了

```powershell
# 假设你本该在 feature/xxx 上提交，但不小心在 main 上提交了
git log --oneline           # 记录下那个错误 commit 的 ID，比如 abc1234
git checkout feature/xxx
git cherry-pick abc1234     # 把那个 commit 「摘」过来

git checkout main
git reset --hard HEAD~1     # 把 main 上那个错误 commit 删掉
```

### 场景 3：线上出 bug，但我手头功能写到一半

```powershell
# 暂存当前工作
git stash

# 切回 main，创建 hotfix 分支
git checkout main
git checkout -b hotfix/urgent-bug
# 修 bug...
git add . && git commit -m "fix: 紧急修复"
git push -u origin hotfix/urgent-bug
# 去 GitHub 创建 PR → 合并

# 回到手头的工作
git checkout feature/xxx
git stash pop
```

### 场景 4：push 被拒绝

```powershell
git push
# ! [rejected] main -> main (fetch first)
# 错误：别人已经推送了新提交，你的本地落后了

# 解决：
git pull                   # 先拉取最新代码（可能会产生冲突，按 5.4 节解决）
git push                   # 再推送
```

### 场景 5：改了一堆文件，只想提交其中几个

```powershell
# 只 add 特定的文件
git add src/pages/Dashboard.vue src/api/user.ts

# 甚至只 add 文件的一部分（交互式添加）
git add -p src/pages/Dashboard.vue
# 会逐块问你要不要 add 这一段改动
# y = 要  n = 不要  s = 拆成更小块  q = 退出
```

---

## 第十部分：进阶技能

### 10.1 stash——暂存手头工作

```powershell
git stash                    # 暂存所有未提交的改动
git stash list               # 查看所有暂存
git stash pop                # 恢复最近一次暂存并删除记录
git stash apply stash@{1}    # 恢复指定暂存但不删除
git stash drop stash@{0}     # 删除某个暂存
```

### 10.2 rebase——整理提交历史

rebase 重写历史，让提交记录更清晰：

```
merge 的结果（有分叉）：
  * Merge branch 'feature'   ← 多了一条合并提交
  |\
  | * feat: 第二步
  | * feat: 第一步
  |/
  * chore: 上一版

rebase 的结果（线性历史）：
  * feat: 第二步
  * feat: 第一步
  * chore: 上一版
```

```powershell
# 把当前分支的提交「接」到 main 的最新提交后面
git checkout feature/xxx
git rebase main

# 如果有冲突，解决后：
git add .
git rebase --continue
```

> ⚠️ **黄金法则：永远不要 rebase 已经 push 到公共仓库的提交！** 只 rebase 你自己的、还没分享给别人的分支。

### 10.3 cherry-pick——挑选提交

```powershell
# 把某个 commit 「摘」到当前分支
git cherry-pick abc1234

# 一次摘多个
git cherry-pick abc1234 def5678
```

### 10.4 reflog——救命稻草

Git 记录了**所有操作的历史**，即使你 `git reset --hard` 了，也能找回来：

```powershell
git reflog
# 输出类似：
# abc1234 HEAD@{0}: reset: moving to HEAD~1
# def5678 HEAD@{1}: commit: feat: 重要的功能
# ghi9012 HEAD@{2}: commit: chore: 初始化

# 找到你想恢复的那个 commit ID（比如 def5678）
git reset --hard def5678
# 刚才被删掉的代码又回来了！
```

> reflog 默认保留 90 天。只要你不是故意删 `.git` 文件夹，基本都能救回来。

---

## 附录：练习与自查

### 入门练习（每个人都做一遍）

- [ ] 安装 Git，配置 name、email、autocrlf
- [ ] `git init` 初始化一个练习项目
- [ ] 创建 3 个文件，`add → commit`，重复 3 次产生 3 个 commits
- [ ] 用 `git log --oneline --graph` 查看提交历史
- [ ] 用 `git reset --soft HEAD~1` 撤销最近一次 commit（保留改动）
- [ ] 创建两个分支，各自改不同文件，合并回主分支
- [ ] 故意制造冲突并解决

### 协作练习（两个人为一组）

- [ ] A 创建 GitHub 仓库，邀请 B 为 Collaborator
- [ ] B clone 仓库
- [ ] A 和 B 各自创建分支，改同一个文件的不同位置 → push → PR → 合并（应该无冲突）
- [ ] A 和 B 各自创建分支，改同一个文件的同一行 → push → PR → 解决冲突 → 合并
- [ ] 模拟线上 bug 场景：手头有未完成功能 → stash → 切分支修 bug → PR 合并 → stash pop 继续

### 自查清单

能独立完成以下操作，就算掌握了日常 Git 工作流：

- [ ] clone 一个仓库
- [ ] 创建分支并切换
- [ ] 在分支上做改动 → add → commit
- [ ] push 到 GitHub
- [ ] 创建 Pull Request 并写清晰的描述
- [ ] Review 同事的 PR，提有意义的建议
- [ ] 拉取最新代码时解决冲突
- [ ] 用 stash 暂存未完成的工作
- [ ] 用 git log 查看历史和找某个 commit
- [ ] 用 git reset 撤销错误的 commit
- [ ] 用 git reflog 找回"丢失"的代码

---

> 📌 **记住最重要的三件事**：
> 1. **多 commit，小步提交**——一次只做一件事，方便以后查找和回退
> 2. **多开分支**——分支极其轻量，不要怕多，功能完成就删
> 3. **永远通过 PR 合并到 main**——保护主分支就是保护团队的稳定性

> 📖 **推荐资源**：
> - [Git 官方文档](https://git-scm.com/doc)（英文，最权威）
> - [Pro Git 中文版](https://git-scm.com/book/zh/v2)（免费在线书，从入门到精通）
> - [Learn Git Branching](https://learngitbranching.js.org/)（可视化交互式教程，强烈推荐！）
> - [GitHub Skills](https://skills.github.com/)（GitHub 官方互动课程）
