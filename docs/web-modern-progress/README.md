<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements. See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0.
-->

# Apache Hop Modern Web - 集成进度

> 唯一实现依据：`docs/web-modern-architecture-plan.md`

## 当前状态

| 任务 | 方向 | 状态 | 当前集成判断 |
|---|---|---|---|
| Task 1 | Web 基础设施 / Session / Security / Project / Environment / VFS | 进行中 | 已锚定 `web/api` + `WebSessionScope` 首个切片；尚无代码 Actions，不集成 |
| Task 2 | Document Model / Graph / SVG / Commands / Save / Undo | 进行中 | 已锚定 Document Model 生命周期与 pipeline open/read 首个切片；尚无代码 Actions，不集成 |
| Task 3 | Transform / Action Config / Schema / Plugin Web / Config UI | 进行中 | 已锚定 A2 JSON 往返与 A3 Schema 最小切片；尚无代码 Actions，不集成 |
| Task 4 | SPA / Canvas / Execution / Log / Metrics / SSE / Execution UI | 进行中 | 已完成执行/日志源码核实，下一步 ExecutionRegistry + SSE ring buffer；文档 Actions 已成功，尚无代码 Actions |
| Task 5 | Assembly / Integration / Tests / Benchmark / Compatibility / Actions / 总协调 | 进行中 | 四个任务均已启动；当前没有满足代码集成门槛的稳定切片 |

## PoC 集成主线

- 分支：`experiment/web-modern-poc`
- 集成原则：只接收符合架构文档、必要测试通过且 GitHub Actions 正常的稳定阶段成果。
- 本轮判断：Task 1～4 均未达到“实现 + 必要测试 + 代码 Actions 通过”，不进行提前合并。

## 当前优先级

1. Task 1 完成 `web/api` 最小 Maven 骨架、`WebSessionScope<T>` 与生命周期测试，为后续公共 API 基础设施提供稳定落点。
2. Task 2 完成 Document Model 生命周期与 pipeline open/read 最小可测试切片。
3. Task 3 完成 A2 注解属性 JSON 往返测试，再推进 A3 Schema 最小字段映射。
4. Task 4 在不抢改公共 Maven/API 的前提下完成独立可测试的 ExecutionRegistry、事件 seq/replay 与有界缓冲。
5. Task 5 在上述切片产生代码 Actions 后执行架构边界、测试、兼容性与集成审查。

## 集成门槛

- 对准 `docs/web-modern-architecture-plan.md` 对应目标与模块边界。
- 实现、必要测试均已 commit 并 push。
- GitHub Actions 对对应稳定提交验证通过。
- 不引入未经验证的核心架构变更；实际方案问题需保留源码、错误、验证结果和最小调整建议。
- 合入 `experiment/web-modern-poc` 后继续保持可审查、可回滚。

## 当前阻塞 / 依赖

- Task 4 已建立分支并开始推进，上一轮“未启动”状态已解除。
- 当前主要依赖不是架构阻塞，而是 Task 1～4 尚未形成通过代码 Actions 的首个稳定切片。
- 公共 Maven / assembly / workflow 为高冲突区域，在 Task 1 基础模块落地前不由 Task 5 抢先修改。

## Actions 快照

- Task 4：文档 workflow run `35850662833` 已成功；run `35854411987` 本轮检查时仍在运行。
- Task 1～3：进度文件均记录尚无代码实现 Actions。

## 最后更新

- 2026-09-23 19:35 +08:00
