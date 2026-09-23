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

# Task 2 - Document Model / Graph / SVG Render / Commands / Save / Undo

状态：进行中

## 当前进展
- 已按 A5 新增 SWT-free `PipelineEditor`，实现 `moveTransforms`，支持批量移动、边界裁剪、重复/未知节点过滤，并记录单个 `PositionTransform` undo action。
- 已实现 `addHop` / `deleteHop`，校验节点存在、自连接和重复 hop，并记录 `NewPipelineHop` / `DeletePipelineHop` undo action。
- 已实现 `setHopEnabled` / `flipHop`：复用 `PipelineHopMeta` 原生语义，拒绝 no-op、未知 hop 和反向边冲突；变更前后均 clone 快照并记录 `ChangePipelineHop` undo action。
- 本轮新增 `deleteTransform`：删除 Transform 时同步清理关联 Hop，并用 linked `DeletePipelineHop` + `DeleteTransform` ChangeAction 保留可恢复的图结构与原索引。
- 已补 hop mutation 单元测试，并修正测试中 ActionType 为当前源码实际枚举名。

## 下一步
- 等待本轮提交的 Actions 验证；若失败先修复真实原因。
- 继续架构 R10 要求的 SWT-free undo/redo 应用层，并以 transform+hop 联动删除作为首个组合动作验证。
- `web/api` 骨架落地后再接 DocumentResource，不越界代替任务 1。

## 最近提交
- 本轮待提交：transform 删除命令及 linked undo 测试。
- `a47c93fd fix: snapshot pipeline hop change undo state`。
- `be5b68a9 fix: use pipeline hop undo action types`。
- `3a95d3b5 test: cover headless pipeline hop mutations`。
- `9711c334 feat: add headless pipeline hop mutations`。

## Actions
- `346add6d` 的 Hop PR Build (Code) run `35867287340`：success。
- 本轮新提交已 push；当前 commit status 尚未返回检查结果，后续轮次继续跟踪。
- 本机 Maven wrapper 默认缓存无写权限且 `JAVA_HOME` 未配置，因此仍以 GitHub Actions 为权威验证。

## 架构文档对应章节
- 1.2：`PipelineMeta`/`WorkflowMeta`、模型编辑 API、`AbstractMeta`/`ChangeAction`、SVG renderer。
- A5：SWT-free `PipelineEditor` / `WorkflowEditor` 图编辑命令层。
- 命令契约：`setHopEnabled`、`flipHop`、`splitHop`、`undo` / `redo`。
- R10：提炼 engine 级 UndoApplier。

## 问题 / 依赖
- 当前无架构阻塞。
- 当前源码桌面 Pipeline undo 已改为 gzip XML snapshot (`HopGuiPipelineUndoDelegate` → `HopGuiPipelineGraph.undo/redo`)，而正式架构 R10 仍指定 `ChangeAction` 反向应用。Task 2 继续按正式架构实现 engine 级语义；暂记录此源码差异，尚未证明需要改变核心设计。
- `web/api` 仍是任务 1 直接依赖；不阻塞 engine A5 的独立推进。

## 最后更新
- 2026-09-23
