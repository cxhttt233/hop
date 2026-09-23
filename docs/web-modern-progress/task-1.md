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

# Task 1 - Web 基础设施 / Maven / Session / Security / Project / Environment / VFS

状态：进行中

## 当前进展
- 已从 `design/web-modern-architecture` 建立 `experiment/web-modern-task-1`。
- 已按架构文档确认首个实现切片：Phase 0 第 1 周骨架，优先 A1 `WebSessionScope<T>` 与 Session 基础设施；后续接 Project / Environment / VFS。
- 已核对 RAP `RapSessionScope<T>`：现有实现依赖 RWT `SingletonUtil`，新 Web API 需要 HttpSession 槽、请求线程绑定与 `InheritableThreadLocal`，不能直接复用 RAP 类。
- 已建立 `web/api` (`hop-web-api`) opt-in Maven 骨架，并实现 SWT/RWT-free `WebSessionScope<T>`：绑定 HttpSession 时严格读取会话槽，无请求线程时使用 `InheritableThreadLocal` 供执行线程继承。
- 已加入会话隔离、空会话不回退、执行子线程继承和 remove 生命周期测试。

## 下一步
- 完成请求级 `WebSessionFilter` 绑定/解绑，并接入 `WebUserSession` / `WebSessionRegistry` 最小模型，为 `/api/v2/session` 与 Project/Environment 切换提供作用域基础。
- 用 GitHub Actions 验证最小模块构建和测试后再扩展 Security/VFS。

## 最近提交
- `chore: initialize modern web task 1 progress`（本进度文件）

## Actions
- 本轮本地容器 Maven 验证已启动；首次依赖解析仍在运行。稳定切片 push 后检查 GitHub Actions。

## 架构文档对应章节
- 3 推荐架构：`hop-web-api` / `hop-web-security`
- 4.1 会话与用户隔离
- 4.2 项目与环境
- 4.3 文件与 VFS
- 7 抽象 A1、A6
- 10.4 工作分解与第一步实现方式（骨架，第 1 周）

## 问题 / 依赖
- 暂无阻塞。
- 公共 Maven/assembly 文件存在与其他并行任务冲突风险；先把任务 1 实现收敛在 `web/api`，公共装配改动仅在达到可验证切片时进行。

## 最后更新
- 2026-09-23 21:40 +08:00
