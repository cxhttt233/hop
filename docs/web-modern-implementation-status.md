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
> 唯一技术与架构依据：`docs/web-modern-architecture-plan.md`。

## Baseline

- Apache Hop baseline：`f7c2694a0b66a151eb244a7e9c11beff46b18421`
- PoC integration branch：`experiment/web-modern-poc`

## 2026-09-24 Product Refocus

项目主线已调整为 **React 原生浏览器编辑器优先**。

### Goal
`real .hpl → Graph JSON → React Flow → semantic edit → React config/metadata → save/reload`

### Repository Strategy
- 计划新建 standalone 产品仓库：`cxhttt233/hop-modern-web`。
- standalone repo 不复制 Apache Hop 全量源码。
- `cxhttt233/hop` fork 继续作为 Hop 引擎基线、必要 core/engine patch 和兼容验证源。

### Asset Classification
**Keep / reuse**
- T1 WebSessionScope / minimal web carrier
- T2 commands / undo
- T3 Config JSON / Schema
- T4 Execution domain

**Current priority**
- T2 Graph Projection JSON + save/reload
- T3 Config/Metadata Web contract
- T4 React + React Flow + native config/metadata UI
- T1 only fills blockers needed by the vertical slice

**Deferred**
- further Execution SSE/reconnect/metrics
- deep Project/Environment/VFS infrastructure not used by the current UI
- server SVG as primary canvas
- plugin Web UI long tail

### Product Gate
Phase 0 is not complete until a user can edit a real Pipeline in React without entering RAP `/ui`, configure at least one real Transform and one Metadata type, save/reload, and keep Apache Hop compatibility.

## Stable Implementation Facts

### Task 1 — Web Foundation

Evidence commits：`ceefd235db`、`f02be05fc945f2d5be53f9805bfea701cc4e896a`。

已确认并纳入 PoC：
- 正式 `web/` parent 与 `web/api` carrier。
- 根构建通过 opt-in Web profile 接入，默认构建不被强制改变。
- SWT/RWT-free `WebSessionScope<T>` 与最小隔离/生命周期测试。
- Task 1 功能 HEAD 的 Code 与 Documentation Actions 成功。

Integration status：**ADMITTED**。

### Task 2 — Document / Graph / Commands / Undo

Evidence commits：`e61d515bca`、`f99620e679`、`6ccae95afa5e0dbd3985563ad36b6e386f3916a9`。

已确认：
- SWT-free Pipeline 编辑命令与 undo/redo 已形成连贯实现，覆盖 transform/hop 核心编辑语义。
- `6ccae95afa` Code Actions 成功，R1 已满足当前 Package 的核心验收并通过 Controller Gate Review。

Integration status：**GATE_REVIEW_PASSED / ADMISSION_ORDER_DEFERRED**。

### Task 3 — Config / Schema / Plugin Web

Evidence commit：`55497fc98a8cdc6ff655a859ed85d1f06fef899d`。

已确认：
- A2 Config JSON transport 已有功能基础。
- provider-aware named metadata read、password/sensitive transport、代表性真实 Meta 验证仍是当前验收缺口；Worker 已有后续实现提交，但在 Actions 收敛前不提升为稳定事实。

Integration status：**IN_PROGRESS / PACKAGE_NOT_YET_CLOSED**。

### Task 4 — Execution / SPA / SSE

Evidence commits：`3bf1d280cfce65a2fb4b6992f30275fa8702c634`、`525c8781bd08f962990cec0676141d3a7169061f`、PoC migration `915ba62fa169437c6dea326940ec6ba81ef417ed`。

已确认：
- ExecutionRegistry、ExecutionEventBuffer/replay、lifecycle、Log、Metrics 已形成连贯领域实现与测试。
- `525c8781bd` Code Actions 成功，execution backend domain candidate 验证完成。
- 根级 `hop-web-api` 临时载体已被丢弃；Controller 仅将 `org/apache/hop/web/api/execution/*` 领域类/测试迁入 PoC 正式 `web/api`，并补齐正式 carrier 所需依赖。
- 迁移后的 PoC GitHub Actions 是最终 admission 门禁。

Integration status：**MIGRATED_TO_FORMAL_WEB_API / REVALIDATION_PENDING**。

## Current PoC State

- Task 1 Web Foundation 已正式纳入 PoC。
- Task 4 execution domain 已迁入正式 `web/api`，等待 migration commit `915ba62f` 的 GitHub Actions revalidation 后确认 admission。
- Task 2 R1 已通过 Gate Review，按集成顺序等待 Task 4 migration revalidation 后进入 PoC。
- Task 1 R2、Task 2 R2、Task 3 A2、Task 4 R2 可并行推进。

## Current Critical Path

`Task 1 foundation admitted → Task 4 execution-domain migrated → GitHub Actions revalidation → admit Task 4 R1 → admit Task 2 R1 → first cross-task PoC slice`

## Context Hygiene

已废弃 `docs/web-modern-progress/*`。小时级动作留在 Worker Report；当前任务定义在 Task Issue；稳定事实在本文；技术路线在 Architecture Plan。

## Last Updated

2026-09-24
