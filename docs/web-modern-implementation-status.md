<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements. See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License. You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Apache Hop Modern Web - Implementation Status

> 本文只记录已经形成的实现事实与当前集成状态，不定义技术架构，不替代任务 Issue，也不是小时级进度日志。
>
> 唯一技术与架构依据：`docs/web-modern-architecture-plan.md`。
> 本文由 Task 5 / Controller 单点维护。

## 使用规则

- 判断“应该怎么实现”：看 Architecture Plan。
- 判断“代码现在实际上是什么”：以真实 branch HEAD、测试和 GitHub Actions 为准。
- 判断“哪些实现事实已经稳定、可作为后续上下文”：看本文。
- 判断“当前正在交付什么”：看 #1～#6 的 Active Package / Gate。
- Issue 历史评论与 Worker Report 是运行记录；旧 revision、旧 HEAD 或已被后续事实替代的内容只作为历史。
- 若本文与真实代码/Actions 冲突，以真实代码/Actions 为准，并由 Task 5 修正本文。

## 基线

- Apache Hop 基线：`f7c2694a0b66a151eb244a7e9c11beff46b18421`
- Architecture branch：`design/web-modern-architecture`
- PoC integration branch：`experiment/web-modern-poc`
- Worker branches：
  - `experiment/web-modern-task-1`
  - `experiment/web-modern-task-2`
  - `experiment/web-modern-task-3`
  - `experiment/web-modern-task-4`
  - `experiment/web-modern-task-5`

## 当前稳定事实

### Task 1 — Web Foundation

Current observed head: `f02be05fc945f2d5be53f9805bfea701cc4e896a`

已确认：
- 正式 `web/` parent 与 `web/api` carrier 已建立。
- 根构建通过 opt-in Web profile 接入，没有把 Modern Web 强制加入默认构建。
- SWT/RWT-free `WebSessionScope<T>` 与会话隔离/生命周期相关测试已存在。
- 该 HEAD 的 Hop PR Build (Code) 与 Hop PR Build (Documentation) 均已完成并成功。

当前集成判断：**READY_FOR_GATE_REVIEW**。

### Task 2 — Document / Graph / Commands / Undo

Current observed head: `6ccae95afa5e0dbd3985563ad36b6e386f3916a9`

已确认：
- 已形成 SWT-free Pipeline 编辑命令与 undo/redo 的实质实现，包括 transform/hop 的核心编辑语义。
- 最新提交继续修正 PipelineEditor undo assertion / disabled-hop redo 相关测试语义。
- 最新 Code Actions 当前仍在验证中。

当前集成判断：**IN_PROGRESS / PENDING_LATEST_VALIDATION**。

### Task 3 — Config / Schema / Plugin Web

Current observed head: `0c39848ba8695b767029cece072eb11ddb9c1932`

已确认：
- A2 Config JSON transport 已有功能实现基础。
- 当前 A2 尚未完成 named metadata reference、password/sensitive transport、代表性真实 Meta 验证的完整闭环。
- 最近多轮 Code Actions 被格式/验证问题反复占用，最新已知 Code Actions 仍失败。

当前集成判断：**IN_PROGRESS / PACKAGE_NOT_YET_CLOSED**。

### Task 4 — Execution / SPA / SSE

Current observed head: `525c8781bd08f962990cec0676141d3a7169061f`

已确认：
- ExecutionRegistry、ExecutionEventBuffer/replay、lifecycle、Log、Metrics 等 execution-domain 能力已有连贯实现与测试。
- `3bf1d280cfce65a2fb4b6992f30275fa8702c634` 的 Code / Documentation Actions 已成功。
- 最新 `525c8781bd` 增加 startup-failure lifecycle boundary；Worker 已报告定向测试 10/10 通过，最新 Code Actions 仍在验证中。
- 根级 `hop-web-api` 只是临时载体，不能作为正式模块布局进入 PoC。

当前集成判断：**DOMAIN_CANDIDATE / LAYOUT_NOT_ADMISSIBLE / PENDING_LATEST_VALIDATION**。

### Task 5 — Controller / Integration

已确认：
- #1～#4 已统一使用 Mission + Active Package + Acceptance + Dependencies。
- #7 已定义 Package、Health Signal、自恢复、stale-state 与 Integration Gate 协调协议。
- #6 为当前 PoC Integration Gate。
- Task 5 是跨 Task 迁移和 PoC 集成唯一协调者。

## 当前 PoC 状态

截至本文初始化：
- 尚无 Worker implementation slice 被确认完成 Gate 并正式纳入 `experiment/web-modern-poc`。
- Task 1 是当前第一个可立即进行 Gate Review 的基础切片。
- Task 4 是当前最成熟的跨 Task 后续候选，但必须只迁移 execution-domain 类/测试到 Task 1 的正式 `web/api`，不得携带根级临时 `hop-web-api` 布局。

## 当前关键路径

`Task 1 formal web/api Gate Review → admit foundation → Task 4 domain migration into formal web/api → revalidate → first cross-task PoC slice`

Task 2 / Task 3 可并行继续各自领域 Package，不应因为上述关键路径停止有效工作。

## 已废弃的上下文

- 各 Worker 分支下的 `docs/web-modern-progress/*` 旧进度文件已废弃，不再作为事实源，也不应继续维护。
- 小时级进展写入 Worker Report；稳定实现事实由 Task 5 低频压缩到本文。
- 不再通过新增 per-task progress.md、日报或重复 TODO 文档维护同一份状态。

## 更新条件

只有以下事实发生实质变化时才更新本文：
- Active Package 完成并形成稳定验收结论；
- Integration Gate 状态变化；
- slice 正式进入 PoC；
- 某接口/能力成为可依赖的稳定实现；
- 临时实现被废弃或迁移；
- 项目关键路径发生实质变化。

普通 CI 等待、临时 push 失败、单轮 Worker 动作、未验证猜测不写入本文。

## Last Updated

2026-09-24
