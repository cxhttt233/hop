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

# Task 4 - SPA / Canvas / Execution / Log / Metrics / SSE / Execution UI

状态：进行中

## 当前进展
- 已从 `design/web-modern-architecture` 创建 `experiment/web-modern-task-4`。
- 已核对正式架构方案中任务 4 的核心边界：SPA 客户端 UI 状态、服务端权威 SVG + 客户端交互层、ExecutionRegistry、日志/指标 SSE、Execution UI。
- 当前优先阶段按 PoC 实现顺序锚定到第 6 周“执行与 SSE”；Canvas/SPA 依赖任务 2/3 提供文档、渲染和配置接口后再完成纵向集成。
- 已核实执行引擎入口：`PipelineEngineFactory.createPipelineEngine(parentVariables, runConfigurationName, metadataProvider, pipelineMeta)` 会装载运行配置、继承变量/参数并注入 metadata provider；`IPipelineEngine` 已直接提供 `prepareExecution/startThreads`、`getEngineMetrics`、`stopAll/pauseExecution/resumeExecution`、状态查询和 started/finished listener，符合 4.7 的抽象边界，无需为 Local/Remote/Beam 另造执行接口。
- 已核实日志增量基础：`HopLogStore` 暴露全局单调行号和 `getLogBufferFromTo(parentLogChannelId, ..., from, to)`；`LoggingBuffer` 已通过 `LoggingRegistry.getLogChannelChildren` 包含子通道，并使用有界 buffer；实时监听由 `addLoggingEventListener` 提供。因此 REST log 的 `from/max` 和 SSE 重连补齐可直接建立在现有日志序号之上。
- 发现一个实现细节：实时 listener 回调只收到 `HopLoggingEvent`，不携带 `BufferLine.nr`；为保证 SSE 的事件 seq 与日志补齐语义清晰，优先采用 ExecutionRegistry 自己的单调 SSE seq，同时日志 payload 保留 HopLogStore 行号。暂不修改 core 日志结构。

## 下一步
- 核对任务 1 的 Web/API 模块骨架是否已落到可复用分支；若尚未进入 task-4 基线，则避免抢改公共 Maven/API 文件，先实现可独立审查的 ExecutionRegistry/事件缓冲核心切片及测试。
- 实现 ExecutionRegistry 最小生命周期：注册、会话归属、状态/控制代理、完成时间、显式删除、TTL 清理接口，并为 SSE 维护有界事件 ring buffer + 单调 seq/replay。
- 增加 ExecutionRegistry 生命周期、事件序列/replay/有界缓冲测试，再触发代码 Actions 验证。

## 最近提交
- `docs: record task 4 execution source findings`
- `docs: initialize web modern task 4 progress`

## Actions
- 初始化提交触发 `Hop PR Build (Documentation)`，run `35850662833`，结果 `success`。
- 尚未有代码实现提交，因此代码构建 Actions 尚未触发。

## 架构文档对应章节
- 2.2 前端：TypeScript SPA
- 4.7 执行 / 日志 / 指标 API
- 4.8 实时通道（SSE）
- 5 画布实现
- 8 横切关注点：执行、日志、多用户状态
- 10.1 PoC 目标
- 10.2 H3 / H4
- 10.4 第 5～6 周实现顺序

## 问题 / 依赖
- Document/Graph/SVG/Commands 由任务 2 负责；Transform/Action 配置由任务 3 负责。
- Web Session/Security/API 基础设施由任务 1 负责。任务 4 不提前重做这些公共能力。
- 架构附录 B 要求实际核实 Jersey SSE 与 Jetty 12 ee11 / Tomcat 10 的兼容性；实现阶段以测试证据为准。
- 当前未发现需要修改正式架构的阻塞问题。

## 最后更新
- 2026-09-23 18:55 +08:00
