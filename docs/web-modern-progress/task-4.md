# Task 4 - SPA / Canvas / Execution / Log / Metrics / SSE / Execution UI

状态：进行中

## 当前进展
- 已从 `design/web-modern-architecture` 创建 `experiment/web-modern-task-4`。
- 已核对正式架构方案中任务 4 的核心边界：SPA 客户端 UI 状态、服务端权威 SVG + 客户端交互层、ExecutionRegistry、日志/指标 SSE、Execution UI。
- 当前优先阶段按 PoC 实现顺序锚定到第 6 周“执行与 SSE”；Canvas/SP​​A 依赖任务 2/3 提供文档、渲染和配置接口后再完成纵向集成。

## 下一步
- 阅读现有 `PipelineEngineFactory`、`IPipelineEngine`、`HopLogStore`/`LoggingBuffer`、`LoggingRegistry` 与 GUI 执行路径，确定 ExecutionRegistry 的最小实现边界。
- 核对 `web/api` 当前由任务 1 建立的模块/API 骨架；若尚未可用，先形成任务 4 可独立审查的 execution/SSE 实现切片，避免修改任务 1 公共文件。
- 增加 ExecutionRegistry 生命周期和事件序列测试，并通过 Actions 验证。

## 最近提交
- `docs: initialize web modern task 4 progress`

## Actions
- 尚未触发本任务实现构建；初始化提交后待检查。

## 架构文档对应章节
- 2.2 前端：TypeScript SPA
- 4.7 执行 / 日志 / 指标 API
- 4.8 实时通道（SSE）
- 8 横切关注点：执行、日志、多用户状态
- 10.1 PoC 目标
- 10.2 H3 / H4
- 10.4 第 5～6 周实现顺序

## 问题 / 依赖
- Document/Graph/SVG/Commands 由任务 2 负责；Transform/Action 配置由任务 3 负责。
- Web Session/Security/API 基础设施由任务 1 负责。任务 4 不提前重做这些公共能力。
- 架构附录 B 要求实际核实 Jersey SSE 与 Jetty 12 ee11 / Tomcat 10 的兼容性；实现阶段以测试证据为准。

## 最后更新
- 2026-09-23 18:35 +08:00
