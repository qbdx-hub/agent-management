# Git 命令速查表 🗒️

> 打印出来贴在桌边，忘了就瞄一眼 | 配合 `docs/git-training.md` 使用

---

## 一、日常使用（每天 10 次）

| 我要做什么 | 命令 |
|-----------|------|
| 看看改了啥 | `git status` |
| 查看具体改了什么内容 | `git diff` |
| 把文件加入暂存区 | `git add .` 或 `git add 文件名` |
| 提交 | `git commit -m "feat: 做了什么"` |
| 推送到 GitHub | `git push` |
| 拉取最新代码 | `git pull` |
| 查看提交历史 | `git log --oneline --graph` |

---

## 二、分支操作

| 我要做什么 | 命令 |
|-----------|------|
| 查看所有分支 | `git branch` |
| 创建分支 | `git branch 分支名` |
| 切换分支 | `git checkout 分支名` |
| 创建 + 切换 | `git checkout -b 分支名` |
| 删除分支 | `git branch -d 分支名` |
| 合并分支 | `git checkout main` 然后 `git merge 分支名` |
| 推送新分支到 GitHub | `git push -u origin 分支名` |

---

## 三、撤销与回退

| 我要做什么 | 命令 |
|-----------|------|
| 撤销工作区的改动 | `git restore .` |
| 撤销暂存区（add 反悔） | `git restore --staged .` |
| 修改最近一次 commit 信息 | `git commit --amend -m "新信息"` |
| 回退到某个版本（保留改动） | `git reset --soft <commit-id>` |
| 回退到某个版本（丢弃改动）⚠️ | `git reset --hard <commit-id>` |
| 找回"丢失"的代码 | `git reflog` 然后 `git reset --hard <id>` |

---

## 四、临时代码管理

| 我要做什么 | 命令 |
|-----------|------|
| 暂存手头工作 | `git stash` |
| 查看暂存列表 | `git stash list` |
| 恢复暂存的工作 | `git stash pop` |

---

## 五、冲突解决

```powershell
# 1. pull 或 merge 后出现冲突 → 打开冲突文件
# 2. 找到 <<<<<<< ======= >>>>>>> 标记
# 3. 决定保留什么，删除标记
# 4. 保存文件
git add .
git commit -m "merge: 解决冲突"
```

---

## 六、PR 工作流（记得住的流程）

```
1. git checkout -b feature/xxx        # 开分支
2. 写代码 ...
3. git add . && git commit -m "..."   # 提交
4. git push -u origin feature/xxx     # 推送
5. 去 GitHub → New Pull Request       # 创建 PR
6. 同事 Review → Approve → Merge      # 审查合并
7. git checkout main && git pull      # 切回拉新
8. git branch -d feature/xxx          # 删本地分支
```

---

## 七、提交信息格式

```
feat:     新功能
fix:      修 bug
docs:     文档
style:    格式（不改逻辑）
refactor: 重构
perf:     性能优化
test:     测试
chore:    杂务（依赖、配置）
```

例：`git commit -m "feat: 新增 Agent 工作台页面"`

---

## 八、我搞砸了！怎么办？

| 情况 | 命令 |
|------|------|
| 改错了文件，想回到上次 commit | `git restore 文件名` |
| add 了不该 add 的文件 | `git restore --staged 文件名` |
| commit 信息写错了 | `git commit --amend -m "正确信息"` |
| commit 到错误分支了 | `git cherry-pick <id>` 捡到正确分支 |
| merge 到一半后悔了 | `git merge --abort` |
| 代码"丢了" | `git reflog` 一定能找回来 |
| push 被拒绝 | `git pull` 先拉取，再 `git push` |

---

## 九、一个永远不会忘的心智模型

```
工作目录  ──git add──▶  暂存区  ──git commit──▶  本地仓库  ──git push──▶  GitHub
  (写代码)              (挑选)                  (保存)                (分享)
```

> **每天到公司第一件事**：`git pull`
> **每天下班前最后一件事**：`git push`
> **碰到任何不确定的事**：先 `git status` 看一看
