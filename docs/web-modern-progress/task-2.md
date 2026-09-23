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
- 本轮继续实现 `addHop` / `deleteHop`：校验节点存在、自连接和重复 hop，调用 `PipelineMeta` 原生图 API，并分别记录 `NewHop` / `DeleteHop` undo action；已补单元测试。
- 上一轮 Code Actions run `35867287340` 已成功完成。

## 下一步
- 处理本轮 hop commands 的 Code Actions；通过后继续 A5 的 hop enabled/flip 与 transform 删除等紧邻命令语义。
- 随后实现架构要求的 SWT-free undo/redo 应用层；`web/api` 骨架落地后再接 DocumentResource，不越界代替任务 1。

## 最近提交
- `4d903a28 test: cover headless pipeline hop commands`。
- `40269b13 feat: add headless pipeline hop commands`。
- `346add6d feat: add headless pipeline move command`。

## Actions
- `346add6d` 的 Hop PR Build (Code) run `35867287340`：success。
- `4d903a28` 已由 push 触发新一轮 Actions，等待结果。
- 本机 Maven wrapper 默认缓存无写权限；改用临时 Maven home 后又发现 `JAVA_HOME` 未配置，因此仍以 GitHub Actions 为权威验证。

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
