<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements. See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0.
-->

# Task 3 - Transform / Action Config / Schema / Plugin Web / Config UI

状态：进行中

## 当前进展
- 已建立任务分支 `experiment/web-modern-task-3`，持续以正式架构方案为唯一实现依据。
- 已完成 A2 第一段最小实现：新增 SWT-free `ConfigJsonSerializer`，仅遍历 `@HopMetadataProperty` 字段，传输键遵循 `key()`，支持基础类型、嵌套对象、对象列表、枚举及 `storeWithCode`，并尊重 `isExcludedFromSerialization` / `defaultBoolean`。
- 已增加 JSON 往返单元测试，覆盖 metadata key、枚举 code、嵌套列表、排除字段与 boolean 默认值。
- 确认现有 `JsonMetadataParser`/`JsonMetadataSerializer` 已提供可复用的反射序列化语义；本实现只做配置传输，不改变 `.hpl/.hwf` XML 持久化路径。

## 下一步
- 等待并检查本轮 Hop PR Build (Code)；若失败优先修复编译、格式或测试问题。
- 补齐 A2 的 named metadata reference、password 加密传输及实际 Transform/Action Meta 覆盖测试；未注解 Meta 的 XML fallback 随后实现。
- A2 稳定后进入 A3 `FormSchemaGenerator` 最小字段映射与单元测试。

## 最近提交
- `55497fc98a8cdc6ff655a859ed85d1f06fef899d` — `feat: add annotated config JSON transport codec`
- `590528438aaa70747e2724cfe7a8837cb3265137` — `docs: initialize web modern task 3 progress`

## Actions
- `Hop PR Build (Documentation)`：提交 `5905284` 已通过。
- `Hop PR Build (Code)`：提交 `55497fc` 已触发，当前运行中；build 与 ui-tests jobs 已启动。

## 架构文档对应章节
- 3.3 A2 / A3 / A8
- 4.5 Transform / Action 配置：JSON 形态与表单 Schema
- 4.9 插件资源
- 6 Transform / Action 配置 UI 迁移策略
- 10 第一阶段 PoC（第 4–5 周、第 8 周）

## 问题 / 依赖
- 暂无架构阻塞。
- A2 对 `storeWithName` 的反序列化需要会话 `IHopMetadataProvider` 才能正确解析；当前最小切片对该输入显式拒绝，下一步按架构补齐 provider-aware 解析，不采用猜测或静默降级。

## 最后更新
- 2026-09-23 19:34 +08:00
