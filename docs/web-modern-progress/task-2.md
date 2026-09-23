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
- 已从 `design/web-modern-architecture` 创建独立工作分支 `experiment/web-modern-task-2`。
- 已确认架构基线要求：服务端文档模型持有 `PipelineMeta` / `WorkflowMeta` 与撤销栈；图编辑复用 SWT-free 的模型 API；画布复用 `PipelineCanvasSvgRenderer` / `SvgGc` / `AreaOwner`，采用服务端权威 SVG + hit-map。
- 已确认现有 `PipelineMeta` 图编辑 API、`AbstractMeta` / `ChangeAction` undo 基础以及 `.hpl/.hwf` XML 序列化均不依赖 SWT，可作为本任务实现基础。
- 已按 A5 新增 SWT-free `PipelineEditor` 首个真实命令：`moveTransforms`，支持多节点移动、边界裁剪、重复/未知节点过滤，并把整批移动记录为单个 `PositionTransform` undo action；已补单元测试。

## 下一步
- 等待并处理 `63417cae` 的 Code Actions；通过后继续扩展 `PipelineEditor` 的 add/delete hop/transform 与 undo/redo 应用语义。
- `web/api` 骨架由任务 1 落地前，继续推进 engine 内 A5 命令层，不因跨任务依赖停滞。

## 最近提交
- `63417cae feat: add headless pipeline move command`。

## Actions
- `63417cae` 已触发 Hop PR Build (Code) run `35867287340`；当前 build 与 ui-tests 均运行中。
- 本机 Maven wrapper 因 `/home/ubuntu/.m2` 为 root 所有无法直接使用默认缓存；已改以 Actions 作为权威验证，不视为架构阻塞。

## 架构文档对应章节
- 0 结论摘要：文档服务器模型、服务端权威 SVG。
- 1.2 执行内核：PipelineMeta/WorkflowMeta、模型编辑 API、AbstractMeta/ChangeAction、PipelineCanvasSvgRenderer/SvgGc/AreaOwner。
- 后续实现严格继续按 `docs/web-modern-architecture-plan.md` 中 Document Model / Graph / Commands / Render / Save / Undo 对应契约推进。

## 问题 / 依赖
- 当前无架构阻塞。
- `web/api` 模块尚未出现在本任务分支，因此 DocumentResource 接线暂属任务 1 依赖；Task 2 先实现架构明确要求的 engine A5。
- Task 2 分支此前不存在，已从正式架构分支创建；未修改 PoC 集成主线或其他任务分支。

## 最后更新
- 2026-09-23
