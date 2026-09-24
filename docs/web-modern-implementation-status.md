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
- `6ccae95afa` Code Actions 成功，R1 已满足当前 Package 的核心验收。

Integration status：**READY_FOR_GATE_REVIEW**。

### Task 3 — Config / Schema / Plugin Web

Evidence commit：`55497fc98a8cdc6ff655a859ed85d1f06fef899d`。

已确认：
- A2 Config JSON transport 已有功能基础。
- provider-aware named metadata read、password/sensitive transport、代表性真实 Meta 验证仍是当前验收缺口。

Integration status：**IN_PROGRESS / PACKAGE_NOT_YET_CLOSED**。

### Task 4 — Execution / SPA / SSE

Evidence commits：`3bf1d280cfce65a2fb4b6992f30275fa8702c634`、`525c8781bd08f962990cec0676141d3a7169061f`。

已确认：
- ExecutionRegistry、ExecutionEventBuffer/replay、lifecycle、Log、Metrics 已形成连贯领域实现与测试。
- `525c8781bd` Code Actions 成功，当前 execution backend domain candidate 验证完成。
- 根级 `hop-web-api` 是临时载体，不能进入 PoC；只迁移领域类/测试到正式 `web/api`。

Integration status：**DOMAIN_VALIDATED / MIGRATION_REQUIRED**。

## Current PoC State

- Task 1 Web Foundation 已正式纳入 PoC。
- 下一关键动作：迁移 Task 4 execution-domain 类/测试到正式 `web/api`，丢弃根级临时模块布局并重新验证。
- Task 2 R1 已转为 Gate Review 候选；Task 3 继续 A2。

## Current Critical Path

`Task 1 foundation admitted → migrate Task 4 execution-domain into formal web/api → revalidate → first cross-task PoC slice`

## Context Hygiene

已废弃 `docs/web-modern-progress/*`。小时级动作留在 Worker Report；当前任务定义在 Task Issue；稳定事实在本文；技术路线在 Architecture Plan。

## Last Updated

2026-09-24
