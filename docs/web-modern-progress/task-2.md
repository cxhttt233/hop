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

## 下一步
- 按架构文档的文档服务器/API 契约定位首个最小实现切片，先建立 Document Model 生命周期与 pipeline 文档打开/读取骨架，再接语义化 commands、render/save/undo。
- 补齐对应单元测试并接入现有 Maven/Actions 验证路径。

## 最近提交
- 初始化 Task 2 进度文件（本提交）。

## Actions
- 尚未产生 Task 2 实现提交；下一稳定切片提交后触发并检查 Actions。

## 架构文档对应章节
- 0 结论摘要：文档服务器模型、服务端权威 SVG。
- 1.2 执行内核：PipelineMeta/WorkflowMeta、模型编辑 API、AbstractMeta/ChangeAction、PipelineCanvasSvgRenderer/SvgGc/AreaOwner。
- 后续实现严格继续按 `docs/web-modern-architecture-plan.md` 中 Document Model / Graph / Commands / Render / Save / Undo 对应契约推进。

## 问题 / 依赖
- 当前无架构阻塞。
- Task 2 分支此前不存在，已从正式架构分支创建；未修改 PoC 集成主线或其他任务分支。

## 最后更新
- 2026-09-23
