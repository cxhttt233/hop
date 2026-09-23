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
- 当前优先阶段按正式架构 4.7、4.8 与 10.4 锚定到第 6 周“执行与 SSE”；Canvas/SPA 依赖任务 2/3 提供文档、渲染和配置接口后再完成纵向集成。
- 已实现 `ExecutionRegistry` 与每执行有界 `ExecutionEventBuffer`：执行注册/删除/完成/TTL 回收、单调 `seq`、按 `Last-Event-ID` replay；覆盖重复 ID、运行中执行不被 TTL 回收、缓冲淘汰和独立序列测试。
- 已实现 `PipelineExecutionLifecycle`：finished listener 注册后按 `prepareExecution → startThreads` 启动，写入 preparing/running/finished 事件并标记完成；已有代理引擎单测覆盖调用顺序、状态事件与完成标记。
- 已实现 1s `ExecutionMetricsPublisher`，从 `getEngineMetrics()/getComponents()` 采集 Execution UI 所需组件指标并写入事件缓冲。
- 已实现 `ExecutionLogPublisher`：通过 `LoggingBuffer.addLoggingEventListener` 实时监听，按根 `logChannelId` + `LoggingRegistry` 子通道过滤并写入事件缓冲，关闭时解除 listener。
- 已补齐 Metrics/Log publisher 单测：验证组件指标快照、执行日志通道过滤和事件写入；当前领域测试共 9 个全部通过。
- 实时 listener 不携带 `BufferLine.nr`：SSE 使用 Registry 自身 seq；REST 增量日志继续使用 HopLogStore 行号，SSE 日志不伪造行号，不修改 core 日志结构。

## 下一步
- Task 1 正式 `web/api` 骨架落地后接 Jersey SSE resource；Task 4 先继续完善可迁移的 15s heartbeat / `Last-Event-ID` replay 领域流逻辑，不扩展共享 Web 脚手架。
- SSE resource 稳定后继续 4.7 的执行详情、log from/max、stop/pause/resume/delete 端点，再进入 Execution UI 纵向集成。

## 最近提交
- `e98e190b1c fix(web): add core compile dependency`
- `c757fcc03c feat(web): publish execution log events`
- `06b39cc1b7 feat(web): publish execution metrics events`
- `30be23e751 test(web): cover pipeline execution lifecycle`
- `489f3db0c6 fix(web): pin junit version for api module`

## Actions / 验证
- 初始化提交的 `Hop PR Build (Documentation)` run `35850662833`：success。
- 本轮清理开发机未使用 Docker 镜像/构建缓存，释放约 7.4GB，解除磁盘 100% 导致的 Git/Maven 阻塞。
- 首次 `compile` 暴露 `hop-web-api` 缺少 `hop-core` 编译依赖；已最小补齐。随后 Docker + JDK21 执行 `mvn -pl hop-web-api -am -DskipTests -Drat.skip=true package`：BUILD SUCCESS。
- 安装 core/engine reactor 依赖后执行 `mvn -pl hop-web-api -Drat.skip=true test`：BUILD SUCCESS，7 tests，0 failures/errors/skips。
- Docker + JDK21 重新验证 `mvn -pl hop-web-api -Drat.skip=true test`：BUILD SUCCESS，9 tests，0 failures/errors/skips。
- 已执行官方 `spotless:apply` 一次性规范现有 Task 4 Java/POM 格式，随后测试仍全部通过。
- 最新提交 push 后继续观察 Actions，若 CI 失败优先修真实原因。

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
- Web Session/Security/API 基础设施由任务 1 负责；任务 4 不提前重做公共能力。
- 架构附录 B 要求实测 Jersey SSE 与 Jetty 12 ee11 / Tomcat 10 兼容性；以实现测试证据为准。
- 当前未发现需要修改正式架构的阻塞问题。

## 最后更新
- 2026-09-24 07:34 +08:00
