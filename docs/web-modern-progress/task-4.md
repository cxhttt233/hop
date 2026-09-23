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

- 已实现首个可测试代码切片：新增 `hop-web-api` 模块中的 `ExecutionRegistry` 与每执行 `ExecutionEventBuffer`，支持执行注册/显式删除/完成标记/TTL 清理，以及有界事件缓冲、单调 `seq` 和按 `Last-Event-ID` 语义 replay；覆盖重复 ID、运行中执行不被 TTL 回收、缓冲淘汰与每执行独立序列测试。
- 已将 `hop-web-api` 接入根 Maven `base` profile，使现有 Code Actions 能实际编译和执行该模块测试。
- 已接入 `PipelineExecutionLifecycle`：注册 finished listener 后按 `prepareExecution → startThreads` 启动，写入 preparing/running/finished 事件并在完成时标记 Registry；补充代理引擎单测覆盖调用顺序、状态事件与完成回收标记。

## 下一步
- 继续观察本分支 Code Actions；当前 fork 的 `gh run list --branch experiment/web-modern-task-4` 暂未返回新 run。
- 已实现 1s `ExecutionMetricsPublisher`，从现有引擎 `getEngineMetrics()/getComponents()` 采集 Execution UI 所需组件指标并写入执行事件缓冲。
- 下一步实现日志事件生产；随后接 Jersey SSE resource、15s 心跳和 `Last-Event-ID` 重连。

## 最近提交
- `fix(web): pin junit version for api module`（已 push）
- `feat(web): bridge pipeline execution lifecycle`
- `feat(web): add execution event replay core`
- `docs: record task 4 execution source findings`
- `docs: initialize web modern task 4 progress`

## Actions
- 初始化提交触发 `Hop PR Build (Documentation)`，run `35850662833`，结果 `success`。
- 首个代码切片已提交；上一轮 Maven model 失败已通过显式 `${junit.version}` 修复并于本轮 push。
- 本轮 Docker/JDK21 Maven 定向构建已启动，但首次解析根 POM 大量 BOM 仍在下载依赖，尚未得到有效编译结论；未将其记为通过。

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
- 2026-09-24 04:30 +08:00
