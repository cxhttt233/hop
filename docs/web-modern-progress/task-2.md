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
- 已实现 `deleteTransform`：删除 Transform 时同步清理关联 Hop，并用 linked `DeletePipelineHop` + `DeleteTransform` ChangeAction 保留可恢复的图结构与原索引。
- 本轮新增 R10 SWT-free `PipelineUndoApplier` 与 `PipelineEditor.undo/redo`，覆盖 transform/hop 新增删除、hop 变更、transform 位置，并支持 linked ChangeAction 组合动作。
- 已补 hop mutation 单元测试，并修正测试中 ActionType 为当前源码实际枚举名。
- 已定位并修复 `05dd2fe38f` 的 engine 测试失败：`AbstractMeta.addUndo` 恢复 `nextAlso` 记录；hop 查找包含 disabled hop；启停走 `PipelineMeta.setHopEnabled` 清缓存；Undo hop 替换走可写的 `setPipelineHop` API。
- 已新增 `addTransform` SWT-free 命令，校验空名/重名，并覆盖 NewTransform undo/redo 往返。

## 下一步
- 等待 `f99620e679` 的 Actions 验证；若失败继续按真实测试/构建日志修复。
- Actions 稳定后继续 Transform 变更命令，并进入 SVG render snapshot / Save 的紧邻切片。
- `web/api` 骨架落地后再接 DocumentResource，不越界代替任务 1。

## 最近提交
- `f99620e679 feat: add headless pipeline transform command`。
- `5e7a95bd1a fix: stabilize pipeline command undo semantics`。
- `05dd2fe38f style: format pipeline undo applier`。
- `f8f289ca29 style: format pipeline editor commands`。
- `e61d515bca feat: add headless pipeline undo redo`。

## Actions
- `05dd2fe38f` 的 Code run `35915581651`：RAT / Checkstyle / Spotless / UI tests 通过，Maven engine tests 失败；4 个失败点均已在 `5e7a95bd1a` 修复。
- `5e7a95bd1a` Code run `35922561724` 已触发；随后 `f99620e679` push 将触发最新权威验证。
- 腾讯云当前无宿主 Java；已启动 JDK 21 Docker 定向测试环境下载依赖，GitHub Actions 仍作为最终验证。

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
- 2026-09-24
