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

> 本文只记录稳定、已验证的实现事实与当前集成状态。
> 不定义技术架构，不替代 Task Issue，不记录小时级进展。
>
> 唯一技术与架构依据：`docs/web-modern-architecture-plan.md`。
> 本文由 Task 5 / Controller 单点维护，位于 PoC integration branch。

## Context Model

- **Architecture Plan**：应该怎么实现、模块边界和技术原则。
- **Implementation Status（本文）**：已经稳定确认的实现事实、可依赖能力和当前集成状态。
- **Issues #1～#6**：当前 Mission / Active Package / Acceptance / Dependencies / Gate。
- **HEAD / tests / Actions / Worker Report**：最新运行事实。

若本文与真实代码、tests 或 Actions 冲突，以真实证据为准，并由 Task 5 修正本文。

## Baseline

- Apache Hop baseline：`f7c2694a0b66a151eb244a7e9c11beff46b18421`
- Architecture branch：`design/web-modern-architecture`
- PoC integration branch：`experiment/web-modern-poc`
- Worker branches：`experiment/web-modern-task-1` ～ `experiment/web-modern-task-5`

本文中的 commit 是 **Evidence Commit**：用于证明某项能力的实现/验证提交，不追踪纯文档、治理或上下文清理 commit，因此不要求等于当前 branch HEAD。

## Stable Implementation Facts

### Task 1 — Web Foundation

Evidence commits：
- `ceefd235db` — HTTP session scope foundation
- `f02be05fc945f2d5be53f9805bfea701cc4e896a` — formal `web/` parent + `web/api` carrier

已确认：
- 正式 `web/` parent 与 `web/api` carrier 已建立。
- 根构建通过 opt-in Web profile 接入，默认构建不被强制改变。
- SWT/RWT-free `WebSessionScope<T>` 与最小隔离/生命周期测试已存在。
- `f02be05f` 的 Hop PR Build (Code) 与 Documentation 均成功。

Integration status：**READY_FOR_GATE_REVIEW**。

### Task 2 — Document / Graph / Commands / Undo

Evidence commits：
- `e61d515bca` — headless pipeline undo/redo
- `f99620e679` — headless pipeline transform command

Latest candidate：
- `6ccae95afa5e0dbd3985563ad36b6e386f3916a9` — 修正 PipelineEditor undo assertion / disabled-hop redo 测试语义

已确认：
- SWT-free Pipeline 编辑命令与 undo/redo 已形成实质实现，覆盖 transform/hop 核心编辑语义。
- 最新 candidate 的 Code Actions 仍需完成权威验证。

Integration status：**IN_PROGRESS / PENDING_LATEST_VALIDATION**。

### Task 3 — Config / Schema / Plugin Web

Evidence commit：
- `55497fc98a8cdc6ff655a859ed85d1f06fef899d` — annotated Config JSON transport codec

已确认：
- A2 Config JSON transport 已有功能基础。
- named metadata reference、password/sensitive transport、代表性真实 Meta 验证尚未形成完整闭环。
- 最近验证链反复受 formatter/validation 影响；该问题是运行状态，不是 Package 定义。

Integration status：**IN_PROGRESS / PACKAGE_NOT_YET_CLOSED**。

### Task 4 — Execution / SPA / SSE

Green evidence commit：
- `3bf1d280cfce65a2fb4b6992f30275fa8702c634` — execution event publishers；Code + Documentation Actions 成功

Latest candidate：
- `525c8781bd08f962990cec0676141d3a7169061f` — startup-failure lifecycle boundary；Worker 定向测试报告 10/10，最新 Code Actions 待权威验证

已确认：
- ExecutionRegistry、ExecutionEventBuffer/replay、lifecycle、Log、Metrics 等 execution-domain 已形成连贯实现与测试。
- 根级 `hop-web-api` 是临时载体，不能作为正式模块布局进入 PoC。
- 正式集成必须只迁移领域类/测试到 Task 1 的 `web/api`。

Integration status：**DOMAIN_CANDIDATE / LAYOUT_NOT_ADMISSIBLE / PENDING_LATEST_VALIDATION**。

### Task 5 — Controller / Integration

已确认：
- #1～#4 使用 Mission + Active Package + Acceptance + Dependencies。
- #7 定义 Package、Health Signal、自恢复、stale-state、context hygiene 与 Integration Gate 协作规则。
- #6 是当前 PoC Integration Gate。
- Task 5 是跨 Task 迁移、Implementation Status 和 PoC integration 的单点协调者。

## Current PoC State

- 当前尚无 Worker implementation slice 被确认完成 Gate 并正式纳入 PoC。
- Task 1 是第一个可立即 Gate Review 的基础切片。
- Task 4 是最成熟的后续跨 Task 候选；必须迁移领域代码到正式 `web/api`，不得合入根级临时 `hop-web-api` 布局。
- 本状态文档本身属于治理/上下文文件，不代表 implementation slice admission。

## Current Critical Path

`Task 1 formal web/api Gate Review → admit foundation → migrate Task 4 execution-domain into formal web/api → revalidate → first cross-task PoC slice`

Task 2 / Task 3 可并行继续各自领域 Package，不因该关键路径停止有效工作。

## Context Hygiene

已废弃：
- `docs/web-modern-progress/*` per-task progress 文件。
- per-task 日报、重复 TODO 或第二套进度表。

规则：
- 小时级动作 → Worker Report。
- 当前任务定义 → Task Issue。
- 稳定实现事实 → 本文。
- 技术路线 → Architecture Plan。
- 旧 revision / 旧 HEAD 的评论只作历史，不作为当前指令。

## Update Conditions

仅在以下事实实质变化时更新本文：
- Package 完成并形成稳定验收结论；
- Integration Gate 状态变化；
- slice 正式进入 PoC；
- 接口/能力成为稳定依赖；
- 临时实现被废弃或迁移；
- 关键路径实质变化。

普通 CI 等待、临时 push/tooling、单轮 Worker 动作、未验证猜测不写入。

## Last Updated

2026-09-24
