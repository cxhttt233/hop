# Task 3 - Transform / Action Config / Schema / Plugin Web / Config UI

状态：进行中

## 当前进展
- 已建立任务分支 `experiment/web-modern-task-3`。
- 已完整读取正式架构方案并锚定 A2/A3/A8、第 4.5、4.9、6、10 节。
- 当前优先实现 PoC 第 4–5 周的配置 JSON 与 Schema 基础能力，先从 SWT-free 的序列化/Schema 服务切入。

## 下一步
- 阅读 `HopMetadataProperty`、`HopMetadataPropertyType`、`XmlMetadataUtil`、`GuiWidgetElement` 及典型 Transform Meta 源码。
- 确定 A2 最小可测试切片：注解属性 JSON 往返，并验证保存仍走原 XML 格式。
- 随后实现 A3 `FormSchemaGenerator` 的最小字段映射与单元测试。

## 最近提交
- 初始化任务 3 进度文件（本提交）。

## Actions
- 分支刚建立，尚无任务 3 专属 Actions 结果；实现首个稳定切片后触发验证。

## 架构文档对应章节
- 3.2 A2 / A3 / A8
- 4.5 Transform / Action 配置：JSON 形态与表单 Schema
- 4.9 插件资源
- 6 Transform / Action 配置 UI 迁移策略
- 10 第一阶段 PoC（第 4–5 周、第 8 周）

## 问题 / 依赖
- 暂无架构阻塞。
- `experiment/web-modern-task-3` 原先不存在，已从 `experiment/web-modern-poc` 当前提交创建。

## 最后更新
- 2026-09-23 18:44 +08:00
