# Task 1 - Web 基础设施 / Maven / Session / Security / Project / Environment / VFS

状态：进行中

## 当前进展
- 已从 `design/web-modern-architecture` 建立 `experiment/web-modern-task-1`。
- 已按架构文档确认首个实现切片：Phase 0 第 1 周骨架，优先 A1 `WebSessionScope<T>` 与 Session 基础设施；后续接 Project / Environment / VFS。
- 已核对 RAP `RapSessionScope<T>`：现有实现依赖 RWT `SingletonUtil`，新 Web API 需要 HttpSession 槽、请求线程绑定与 `InheritableThreadLocal`，不能直接复用 RAP 类。

## 下一步
- 建立 `web/api` 最小 Maven 模块骨架，并实现 SWT/RWT-free 的 `WebSessionScope<T>` 与请求绑定生命周期测试。
- 接入 `WebUserSession` / `WebSessionRegistry` 最小模型，为 `/api/v2/session` 与 Project/Environment 切换提供作用域基础。
- 用 GitHub Actions 验证最小模块构建和测试后再扩展 Security/VFS。

## 最近提交
- `chore: initialize modern web task 1 progress`（本进度文件）

## Actions
- 分支刚建立，尚无本任务 Actions 结果；下一稳定切片提交后检查。

## 架构文档对应章节
- 3 推荐架构：`hop-web-api` / `hop-web-security`
- 4.1 会话与用户隔离
- 4.2 项目与环境
- 4.3 文件与 VFS
- 7 抽象 A1、A6
- 10.4 工作分解与第一步实现方式（骨架，第 1 周）

## 问题 / 依赖
- 暂无阻塞。
- 公共 Maven/assembly 文件存在与其他并行任务冲突风险；先把任务 1 实现收敛在 `web/api`，公共装配改动仅在达到可验证切片时进行。

## 最后更新
- 2026-09-23 18:31 +08:00
