<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Hop 现代 Web 架构方案（design/web-modern-architecture）

- 基线：Apache Hop `main` @ `f7c2694a`（本分支当前与上游完全一致，尚无业务代码改动）
- 版本：2.20.0-SNAPSHOT，Java 21，Jetty 12.1.12 (ee11)，Jersey 4.0.2，Jackson 2.21.5，RAP 3.x
- 文档性质：架构研究与落地方案。本文所有判断均基于对当前源码的实际阅读，关键处给出模块 / 类 / 行号。

---

## 0. 结论摘要

1. **执行内核已经是 headless 的，不需要重写。** `core` + `engine`（含 Pipeline/Workflow 模型、`.hpl/.hwf` XML 序列化、执行引擎、日志、Execution Info、元数据、VFS、插件注册表）在 `hop-run`、`HopServer` 里已经无 GUI 运行多年。新的 Web 层只替换表现层。
2. **今天的 Hop Web 不是"Web 架构"，而是"桌面 GUI 跑在服务器上 + RAP 把 SWT 控件树镜像到浏览器"。** 每个浏览器会话在服务器上持有一整套 `HopGui`（`rap/HopGuiImpl` 用 RWT `SingletonUtil` 按会话切分），画布、对话框、日志窗全部是服务端 SWT 对象。这是它交互延迟高、难扩展、难做多用户与前端生态对接的根因。
3. **源码里已经存在三条"可换表现层"的缝**，新方案完全建立在这三条缝上：
   - `core/scope/IHopScope`：环境态（元数据提供者、VFS 命名空间）的作用域抽象。`rap/RapSessionScope` 已证明"一个 JVM 多个用户各自项目"可行。
   - `engine/pipeline/canvas/PipelineCanvasSvgRenderer` + `engine/core/gui/SvgGc` + `AreaOwner`：SWT-free 的服务端 SVG 渲染与命中图（hit-map），Hop Web 现在的画布就是这样画出来的。
   - 注解驱动模型 + 现成 JSON API：`@HopMetadataProperty`（175 个 transform Meta 类已使用）、`@GuiWidgetElement`/`GuiCompositeWidgets`（自动生成表单）、`engine/www/api/HopApiApplication`（Jersey，已挂在 Hop Web war 和 HopServer 上）。
4. **推荐架构**：
   - 后端：**不引入 Spring Boot**。沿用仓库已有的 Jakarta Servlet + JAX-RS(Jersey) + Jackson + 嵌入式 Jetty，新增 `hop-web-api` 模块，实现"文档服务器"模型（服务端持有 `PipelineMeta`/`WorkflowMeta` 与撤销栈，暴露语义化命令 + 渲染快照 + SSE 事件流）。
   - 前端：独立 SPA（TypeScript，推荐 React；契约与前端框架无关，Vue 3 亦可），画布采用**服务端权威 SVG 渲染 + 客户端交互层**，配置界面采用**Schema 驱动通用表单 + 插件可选自定义 Web 对话框**的分层策略。
   - 部署：第一阶段与现有 RAP `/ui` 共存于同一个 war、同一套认证过滤器、同一个 plugins 目录；后期提供独立 `hop-web` 启动器，RAP 变为可选。
5. **PoC（6–8 周，1–2 人）验证五个假设**：会话作用域可脱离 RAP、Transform 配置可注解驱动 JSON 往返且不破坏 XML、服务端 SVG + 客户端交互延迟可接受、执行/日志/指标可通过 SSE 实时到达浏览器、插件可以在不改 core 的前提下自带 Web UI。
6. **上游路径**：以"可选、增量、零侵入模型契约"的独立模块形式提交 HIP；抽象层改动拆成对上游本身有价值的小 PR（JSON transform 序列化、`IHopScope` Web 实现、安全过滤器下沉到独立模块等）。

---

## 1. 现状分析：源码里的真实边界

### 1.1 模块与规模

| 模块 | Java 文件 | 行数 | 角色 |
|---|---:|---:|---|
| `core` | 729 | 132,690 | 插件注册表、元数据 API、VFS、变量、日志、加密、安全模型、GUI 插件注解（`core/gui/plugin`） |
| `engine` | 896 | 148,481 | Pipeline/Workflow 模型与 XML、执行引擎、Painter/SvgGc、Execution Info、`www`（HopServer + Jersey API）、undo |
| `ui` | 665 | 174,620 | SWT HopGui：透视图、图编辑器、对话框基类、通用控件、元数据编辑器 |
| `rcp` | 58 | 11,213 | 桌面端 facade 实现 |
| `rap` | 76 | 11,636 | Web 端 facade 实现、RWT 引导、画布 SVG 桥、安全过滤器、explorer 文件服务、JS 资源 |
| `plugins` | 4,536 | 1,021,571 | 152 个 transform、52 个 action、数据库、引擎、tech、misc（projects/git/…） |
| `web-tests` | 21 | 3,736 | Selenium 画布测试、`/hop/*` RBAC 与跨站测试、curl 脚本 |

插件中的 SWT 对话框：`plugins/transforms` 下 182 个 `*Dialog.java`，`plugins/actions` 下 57 个，合计约 140,472 行；最大的几个：`TextFileInputDialog` 3,459 行、`MailDialog` 2,531、`ExcelInputDialog` 2,256、`GetXmlDataDialog` 2,201、`RestDialog` 2,163；典型的 `TableInputDialog` 1,040、`SelectValuesDialog` 962。

### 1.2 执行内核：`core` + `engine` 已经 headless

- **模型**：`engine/.../pipeline/PipelineMeta`（`@HopMetadataProperty(key="info")`、`key="transform"`、`groupKey="order", key="hop"`，`loadXml` 在第 1795 行附近走注解反序列化）；`TransformMeta` 的 type/name/copies/distribute/GUI/partitioning 等字段全部是 `@HopMetadataProperty`，`getXml()`（第 200 行）拼接 `transform.getXml()`，缺失插件通过 `XmlMetadataUtil.preserveMissingPluginXml` 原样保留。`.hpl/.hwf` 的读写只需要 `IHopMetadataProvider + IVariables + PluginRegistry`，不需要任何 GUI。
- **模型编辑 API 是 SWT-free 的**：`PipelineMeta.addTransform/removeTransform/addPipelineHop/removePipelineHop/findTransform/getTransformFields/getPrevTransformFields`，撤销栈在 `engine/.../base/AbstractMeta`（`addUndo/nextUndo/viewNextUndo`）与 `engine/.../core/undo/ChangeAction`（无 SWT import）。自动布局 `engine/.../pipeline/PipelineMetaLayout`。
- **Painter 是 SWT-free 的**：`engine/.../core/gui/{IGc, SvgGc, BasePainter, AreaOwner}`、`engine/.../pipeline/PipelinePainter`、`engine/.../workflow/WorkflowPainter`；`ui/.../shared/SwtGc` 是唯一的 SWT 实现。`engine/.../pipeline/canvas/PipelineCanvasSvgRenderer.render(Context)` 直接产出 `CanvasSvgRenderResult(svgXml, areaOwners, viewPort, graphPort)`。Painter 上有 `PipelinePainterStart/Transform/Arrow/End` 扩展点（projects 插件的 `DrawEnvironmentOnPipelineExtensionPoint` 就挂在这里）。
- **引擎**：`PipelineEngineFactory.createPipelineEngine(runConfigName, metadataProvider, pipelineMeta)`；`IPipelineEngine.prepareExecution/startThreads/stopAll/pauseExecution/addExecutionFinishedListener/getEngineMetrics/getComponents`；`IEngineComponent` 暴露 linesRead/Written/Input/Output/Rejected/Updated、errors、status、`addRowListener`。预览：`PipelinePreviewFactory.generatePreviewPipeline`。GUI 启动执行的代码路径是 `ui/.../HopGuiPipelineGraph:6112`（createPipelineEngine）→ `:6346`（prepareExecution）→ `:6511`（startThreads）→ `:6192`（addExecutionFinishedListener）。
- **日志**：`core/.../logging/HopLogStore`（进程级单例）→ `LoggingBuffer.getLogBufferFromTo(parentChannelId, includeGeneral, from, to)`、`addLoggingEventListener(IHopLoggingEventListener)`；`LoggingRegistry.getLogChannelChildren` 给出子通道树；`HopLogStore.discardLines` 用于释放（`HopGuiPipelineLogDelegate:180` 就这样做）。
- **执行历史**：`engine/.../execution/IExecutionInfoLocation`（registerExecution/updateExecutionState/getExecutionState/getExecutionStateLoggingText/registerData/getExecutionIds/findExecutions/getExecutionData…），实现有 File、CachingFile、CachingDatabase、Remote、Neo4j、Elastic、OpenSearch。
- **现成 JSON API**：`engine/.../www/api/HopApiApplication`（Jersey `ResourceConfig`，显式注册资源，Javadoc 明确说明"classpath 扫描在 Hop 插件类加载器下工作不良"），资源有 `ExecutionResource(POST /execute/sync)`、`LocationResource(/location/{name}/executions/…，完整的执行历史 CRUD)`、`MetadataResource(/metadata/types, /metadata/list/{key}, GET/POST/DELETE /metadata/{key}/{name}，输出与磁盘文件一致的 JSON)`、`PluginsResource`。挂载点 `/hop/api/v1`，HopServer 的 `WebServer`（Jetty 12 ee11 `ServletContextHandler`）与 Hop Web 的 `web.xml` 都挂了它。
- **进程级单例**（多用户服务器必须知道的）：`PluginRegistry`、`HopConfig`、`LoggingRegistry`/`HopLogStore`、`ExtensionPointMap`、`GuiRegistry`、`Variables.getADefaultVariableSpace()`、`HopVfs.getFileSystemManager()`、`AuditManager`（静态 provider）、`HopSecurity`（静态 provider）。其中元数据提供者与 VFS 命名空间已经通过 `IHopScope` 做成可换作用域（`HopMetadataInstance.setScope`、`HopVfsNamespaces.setScope`）。

### 1.3 桌面 GUI：`ui` + `rcp`

- `ui/.../hopgui/HopGui`（3,118 行）持有：`MultiMetadataProvider`、`IVariables`、`PropsUi`、`Shell/Display`、主菜单/工具栏（`GuiMenuWidgets`/`GuiToolbarWidgets`，由 `GuiRegistry` 注解驱动）、`HopPerspectiveManager`、`HopGuiAuditDelegate`、通知系统、终端面板等；`HopGui.getInstance()` 通过 `ISingletonProvider` 间接获取（第 471/548 行）。
- 图编辑器 `HopGuiPipelineGraph`（7,513 行）/`HopGuiWorkflowGraph`（6,057 行）把 SWT 事件处理、选择/拖拽/连线/对齐、44/33 个 `@GuiContextAction`、运行/预览/调试/日志/指标委托（`delegates/HopGuiPipeline*Delegate`）全揉在一起。
- 对话框基类 `ui/.../pipeline/transform/BaseTransformDialog`（1,656 行）；通用控件 `ui/.../core/widget/{TableView(5,107 行), TextVar, ComboVar, MetaSelectionLine, StyledTextComp, TextComposite, PasswordTextVar, LabelTextVar, ConditionEditor…}`。插件对话框对它们的依赖统计（按文件数）：`TextVar` 233、`TableView` 193、`MetaSelectionLine` 96、`ComboVar` 67、`StyledTextComp` 25、`TextComposite` 25、`LabelTextVar` 23、`PasswordTextVar` 20。
- 注解驱动表单：`core/gui/plugin/GuiWidgetElement`（type/label/toolTip/password/variables/comboValuesMethod/order/parentId…，`GuiElementType = NONE, TEXT, MULTI_LINE_TEXT, FILENAME, FOLDER, COMBO, CHECKBOX, METADATA, BUTTON, LINK, COMPOSITE`）由 `ui/.../core/gui/GuiCompositeWidgets`（1,944 行）渲染成 SWT；`@GuiWidgetElement` 在 137 个文件中出现，覆盖 `PipelineRunConfigurationEditor`、`WorkflowRunConfigurationEditor`、`BaseDatabaseMeta`（数据库连接编辑器）、`ExecutionInfoLocationEditor`、`ExecutionDataProfileEditor`、各类 `*ConfigPlugin` 选项页，以及少量 transform 对话框（`ExecSqlDialog`、`DdlDialog`、`ParquetOutputDialog`、`EmbedTextDialog`、`PgVector*Dialog`）。
- 元数据编辑器：`ui/.../core/metadata/MetadataManager` 按 `<MetaClass>Editor` 命名约定反射查找（第 592–601 行），`MetadataEditor<T>` 是 SWT 类（`setWidgetsContent/getWidgetsContent`），共 44 个编辑器。
- 文件类型与透视图：`IHopFileType`、`HopFileTypeRegistry`、`IHopFileTypeHandler`（start/stop/pause/preview/debug/undo/redo/copy/paste/getStateProperties…）接口本身没有 SWT import；`IHopPerspective.initialize(HopGui, Composite)` 是 SWT 绑定的。透视图插件（`@HopPerspectivePlugin`）：Explorer、Metadata、Execution、Configuration、Database、PluginExplore，以及插件提供的 Git/GitCommit/AiAdvisor/Neo4j。
- `rcp` 只提供桌面侧 facade 实现（`CanvasFacadeImpl`、`ContentEditorFacadeImpl(JFace)` 等），`ImplementationLoader.newInstance(type)` 按 `类名 + "Impl"` 反射加载——`rcp` 与 `rap` 提供同名实现类，靠 classpath 上放哪一个 jar 决定（`docker/web.Dockerfile` 删除 `hop-ui-rcp-*.jar`）。

### 1.4 Hop Web：SWT-on-RAP 的真实工作方式

**启动链**（`assemblies/web/src/main/resources/WEB-INF/web.xml` + `rap/`）：

1. Tomcat 10 (`docker/web.Dockerfile: FROM tomcat:10-jdk21`) 加载 war；`HopWebServletContextListener extends RWTServletContextListener`：`HopEnvironment.init(); HopGuiEnvironment.init(); HopSecurityBootstrap.runOnce(); HopServerPluginPermissions.registerLoadedPlugins(); AuditManager.setSessionAuditManagerProvider(new HopWebAuditManagerProvider()); HopSecurity.setProvider(new RapSecurityContextProvider());`，`contextDestroyed` 直接 `System.exit(0)`。
2. `HopWeb implements ApplicationConfiguration`：安装会话作用域 `HopMetadataInstance.setScope(new RapSessionScope<>(MetadataProviderHolder.class))`、`HopVfsNamespaces.setScope(new RapSessionScope<>(VfsNamespaceHolder.class))`；把所有工具栏/元数据/透视图插件的 SVG 注册为 RWT 资源；注册 `CanvasRenderServiceHandler`；注册 JS 资源（`canvas-svg.js`、`canvas-zoom.js`、`log-console.js`、`monaco-editor.js`、`clipboard.js`…）与 light/dark 主题 CSS；两个入口 `/ui`、`/ui-dark`。
3. `HopWebEntryPoint.createContents` 每个浏览器会话：读取审计目录的主题偏好、从 servlet Principal 绑定 `HopSecurityContext`（`RapSecurityContextProvider.bindFromCurrentRequest`）、注册键盘快捷键为 RWT `ACTIVE_KEYS/CANCEL_KEYS`、然后构造该会话自己的 `HopGui`。
4. `web.xml` 其它映射：`ExplorerFileServlet` `/explorer-file/*`（把项目内 HTML/图片以带 token 的路径 URL 提供给 RAP Browser 控件）；`HopServerServlet` `/hop/*`（HopServer 的 XML servlet 与 `@HopServerServlet` 插件）；Jersey `HopApiApplication` `/hop/api/v1/*`；过滤器 `CrossSiteRequestFilter(/hop/*)`、`HopBasicAuthFilter(/*)`、`HopOidcAuthFilter(/*)`、`HopServerAuthorizationFilter(/hop/*)`。

**会话模型**：`rap/HopGuiImpl implements ISingletonProvider` → `SingletonUtil.getSessionInstance(HopGui.class)`；`RapSessionScope` 同时把值放在 RWT UISession 与 `InheritableThreadLocal`（执行线程没有 UISession）。文档注释非常清楚地说明了动机："one JVM serves many people at once… or one user's project silently becomes another's"。这套逻辑就是新 Web 层会话模型的蓝本，只需把"RWT UISession"换成"HttpSession + 请求线程绑定"。

**画布**（`rap/.../canvas/*` + `ui/.../CanvasSvgFacade` + `canvas-svg.js`）：

- `CanvasSvgFacade.renderPipeline(...)` → `rap/CanvasSvgFacadeImpl` → `PipelineCanvasSvgRenderer.render(ctx)` 在服务端画出整幅 SVG 与 `List<AreaOwner>`。
- 结果存进按会话隔离的 `CanvasGraphRegistry`（键为 RWT widget id），`CanvasRenderServiceHandler` 以 `?servicehandler=canvasRender&session=<uuid>&canvas=<id>&rev=<n>` 提供 JSON：`{revision, svg, areas:[{areaType,x,y,width,height,hover,owner:{kind:"transform"|"action"|"note"|"noteLink"|"label"|"pipelineHop"|"workflowHop"|…, name…}}], props}`（`CanvasRenderSnapshot.toJson`、`AreaOwnerJsonSerializer`）。
- 浏览器端 `canvas-svg.js` 把 SVG 叠在 RAP Canvas 的 DOM 上，用 `areas` 在本地做命中测试，hover 通过 RemoteObject `hop.CanvasInteraction` 回传到 `IWebCanvasGraph.handleWebCanvasHover`；**鼠标按下/拖拽/释放仍走 RAP 的 SWT 事件通道到 `HopGuiPipelineGraph`**——这就是"每次拖动都是服务端 SWT 事件处理 + 重绘 + 推送"的来源。
- `AreaOwner.AreaType` 已覆盖 TRANSFORM_ICON/NAME/INFO_ICON/FAILURE_ICON/COPIES_TEXT/OUTPUT_DATA、HOP_*、ACTION_*、NOTE、NOTE_LINK、CUSTOM 等，足以支撑客户端交互。

**已经"Web 原生"、可直接复用的部分**：

| 组件 | 位置 | 与 RWT 的耦合 |
|---|---|---|
| `HopBasicAuthFilter`、`HopOidcAuthFilter`、`HopBearerSupport`、`HopLoginPage`、`HopServerAuthorizationFilter`、`HopAuthenticatedPrincipal/Request` | `rap/.../security` | 无 `org.eclipse` import，纯 Jakarta Servlet |
| `HopSecurityConfig`（NONE/EXTERNAL/BASIC/OAUTH2，`security-config.json`）、`HopSecurityContext`、`Permission`、`HopRole`、`HopUserStore`、`PasswordHasher`、`oidc/HopOidcClient`、`HopSecurityContextResolver`、`CrossSitePolicy` | `core/.../security` | 无 servlet 依赖 |
| `CrossSiteRequestFilter/Handler` | `engine/.../www` | 纯 servlet |
| `ExplorerFileServing`（相对路径校验、扩展名白名单、公开路径构造） | `ui/.../perspective/explorer/web` | 仅依赖 `core.util.Utils`，可下沉 |
| `HopWebAuditManagerProvider`/`HopWebAuditPaths`（每用户 `<HOP_AUDIT_FOLDER>/users/<name>`） | `rap` | 依赖 RWT 取当前用户，逻辑可复用 |
| `PipelineCanvasSvgRenderer`/`WorkflowCanvasSvgRenderer`、`AreaOwner` JSON 形态 | `engine`、`rap` | 渲染器无耦合；JSON 序列化器可原样搬迁（改用 Jackson） |
| Monaco 集成（`rap/pom.xml` 下载 monaco-editor 0.55.1，`ContentEditorFacade`） | `rap` | 说明 Hop Web 已在用现代前端组件 |
| `web-tests`（Selenium、RBAC/CSRF 测试、`api-checks` 脚本） | `web-tests` | 可扩展为新 API 的契约测试 |

### 1.5 插件体系与 Dialog 的绑定方式

- 发现：`core/.../plugins/JarCache` 读 `META-INF/jandex.idx`（`ANNOTATION_INDEX_LOCATION`），`HOP_PLUGIN_BASE_FOLDERS` 指定插件根；每个插件独立 `HopURLClassLoader`（`PluginRegistry.getClassLoader(plugin)`、`getClass(plugin, name)`）。插件 zip 由 `assemblies/shared/hop-plugin-libs.xml` 组装，`version.xml` + jar + lib。
- Transform 对话框：`ui/.../delegates/HopGuiPipelineTransformDelegate.getTransformDialog`（第 87–157 行）先查 `plugin.getClassMap().get(ITransformDialog.class)`，否则 `BaseTransformMeta.getDialogClassName()`（第 518 行：`XxxMeta` → `XxxDialog`，并尝试 `.hop.` → `.hop.ui.`），构造函数签名固定为 `(Shell, IVariables, XxxMeta, PipelineMeta)`；`ITransformDialog.open()` 返回 transform 名。Action 同理（`IActionDialog.open()` 返回 `IAction`，`ActionBase.getDialogClassName()` 第 766 行）。
- 关键事实：**Meta 类只按名字引用 Dialog 类**，所以 HopServer/hop-run 在没有 SWT 的 classpath 上照样加载所有插件；新 Web 层也一样，插件 jar 里的 SWT Dialog 类只是"永远不会被加载的字节码"。
- 插件已有的"贡献服务端资源"机制：`@HopServerServlet` + `IHopServerPlugin`（`HopServerServlet` 按 `HopServerPluginType` 发现，`requiredPermission()` 接入 RBAC）；图标/文档通过 classloader 资源（`SvgCache`、`documentationUrl`）。**目前没有任何插件自带 JS/HTML 资源的机制**。
- 元数据注解：`@HopMetadata(key,name,description,image,category,documentationUrl,hopMetadataPropertyType,classLoaderGroup)`；`@HopMetadataProperty(key,password,storeWithName,storeWithCode,groupKey,defaultBoolean,enumNameWhenNotFound,isExcludedFromSerialization,isExcludedFromInjection,injectionKey,injectionKeyDescription,injectionGroupKey,injectionGroupDescription,hopMetadataPropertyType,injectionConverter,inline)`；`HopMetadataPropertyType` 枚举含 RDBMS_CONNECTION/RDBMS_TABLE/RDBMS_SCHEMA/RDBMS_SQL/FIELD_LIST/STREAM_FIELD/PIPELINE_FILE/FILE_PATH/PIPELINE_RUN_CONFIG/… 共 70 余项。在 transform Meta 上的实际使用：RDBMS_CONNECTION 20 处、RDBMS_TABLE 14、RDBMS_SCHEMA 10、STATIC_SCHEMA_DEFINITION 6、RDBMS_SQL 5…；`injectionKeyDescription` i18n 标签 1,380 处。这是"通用表单"能做到的上限，也是它的天花板：**有类型、标签、密码语义，没有布局、分组顺序和控件联动**。

### 1.6 边界结论

```
浏览器 ──RAP 协议(控件树同步)──► [Tomcat] RWTServlet /ui
                                   │  每会话: HopGui(SWT-on-RWT) ── 透视图/图编辑器/对话框/TableView…
                                   │        │ CanvasSvgFacadeImpl → PipelineCanvasSvgRenderer(engine, SWT-free)
                                   │        └ ImplementationLoader 加载 rap/*Impl
                                   ├─ /hop/*          HopServerServlet (XML servlets + @HopServerServlet 插件)
                                   ├─ /hop/api/v1/*   Jersey HopApiApplication (JSON)
                                   ├─ /explorer-file/* ExplorerFileServlet
                                   └─ 过滤器: CrossSite, BasicAuth, OidcAuth, ServerAuthorization
                                            │
                        core + engine + plugins（headless；被 hop-run / HopServer / Hop Web 共用）
```

- **桌面 GUI 与 Hop Web 之间**：没有架构边界，是同一份 `ui` 代码；差异靠 `EnvironmentUtils.getInstance().isWeb()`（`ui` 里 173 处分支，`HopGui` 15、两个 Graph 各 13、`ExplorerPerspective` 12、`TableView` 8…）和 `*Facade + ImplementationLoader` 抹平。
- **GUI 与执行内核之间**：边界清晰。GUI 通过 `PipelineMeta`/`WorkflowMeta`（模型）、`PipelineEngineFactory`/`IPipelineEngine`（执行）、`HopLogStore`/`LoggingRegistry`（日志）、`IExecutionInfoLocation`（历史）、`IHopMetadataProvider`（元数据）、`HopVfs`（文件）、`PluginRegistry`/`GuiRegistry`（插件与 GUI 贡献描述）调用内核，且这些接口都无 SWT 依赖。
- 因此，**"现代 Web 架构的 Hop Web"= 在这条清晰边界之上，用一个真正的 Web 表现层（API + SPA）替换 `ui`+`rap`，而不是替换或包装内核**。

---

## 2. 技术路线决策

### 2.1 后端：Jakarta Servlet + JAX-RS(Jersey)，不引入 Spring Boot

理由（均来自源码现状）：

1. **仓库里没有任何 Spring/Quarkus/Micronaut 依赖**（全库 `pom.xml` grep 为空），但已有 Jetty 12 (ee11)、Jersey 4、Jackson 2.21、HK2；`HopServer.WebServer` 与 Hop Web war 已用它们提供 JSON API。新增第二套容器/DI/HTTP 栈会带来依赖冲突（Jetty、Jackson、servlet API 版本）、体积（+30MB 级）和上游阻力。
2. **插件类加载器与 classpath 扫描天然冲突**。`HopApiApplication` 的 Javadoc 已写明"classpath scanning behaves badly under Hop's plugin classloaders"，所以资源必须显式注册。Spring 的核心价值（组件扫描、自动配置）在这里恰恰用不上。
3. **Hop 已自带安全/配置/审计体系**：`HopSecurityConfig` 四种模式、RBAC `Permission`、OIDC 客户端、`hop-config.json`、`AuditManager`。用 Spring Security 重做一遍等于分叉安全模型。
4. **JAX-RS 是可移植契约**。若未来某个发行版想用 Quarkus/Spring 作为运行时，JAX-RS 资源类可原样搬迁；反之 Spring MVC 注解则绑定 Spring。
5. **上游可接受性**。沿用现有栈的模块，社区审阅成本最低。

选型细节：
- HTTP/JSON：Jersey 4 + Jackson（复用 `HopJson`/`JacksonFeature.withoutExceptionMappers()` + `HopApiExceptionMapper` 的错误契约）。
- 实时：**SSE 优先**（Jersey `jersey-media-sse`，需新增依赖；单向、经反向代理友好、与 cookie 会话天然兼容）；WebSocket（Jetty 12 ee11）留给后期双向需求（协同光标、远程终端）。
- DI：HK2 `AbstractBinder`（已在 `HopApiApplication` 使用），不引入 CDI。
- 会话：容器 `HttpSession` + 自研 `WebUserSession` 注册表；执行线程用 `InheritableThreadLocal` 继承（照搬 `RapSessionScope` 的两级设计）。
- 运行时：第一阶段挂进现有 war（Tomcat 10）；第二阶段提供嵌入式 Jetty 启动器 `hop-web`（复用 `engine/www/WebServer` 的写法），使 Hop Web 与 HopServer 成为同一进程的两个 handler。

### 2.2 前端：TypeScript SPA，推荐 React，契约框架中立

- 契约（REST/JSON/SSE、Schema、命令、渲染快照、插件 UI bundle 接口）不依赖任何前端框架，PoC 的验收测试全部在 HTTP 层，前端框架可在 PoC 后复核。
- 推荐 **React 18/19 + TypeScript + Vite**：
  - 画布交互层不需要重型图库（服务端渲染 SVG），需要的是稳定的事件状态机与 SVG DOM 操作；若后期做客户端渲染模式，`@xyflow/react`（MIT）是同类里最成熟的。
  - Monaco（Hop Web 已在用 0.55.1）、JSON-Schema 表单（`@rjsf` 或自研渲染器）、虚拟表格等生态最齐。
  - Apache 社区先例：Superset、Airflow 3 均为 React；许可证均为 MIT（ASF Category A）。
- **Vue 3 + Vue Flow 是可接受的替代**（Vue Flow 是 React Flow 的移植，Naive UI/Element Plus 生态成熟）。若实施团队的技能栈以 Vue 为主，应在 PoC 立项时一次性决定，之后不再摇摆；本方案后文以 React 术语描述，但不含任何 React 专属契约。
- UI 组件库选择 MIT 许可的（Ant Design / Radix+shadcn / MUI 均可），并把设计 token 独立成层，保持可替换。
- **交互符合直觉**：在尽量减少不必要文字说明的情况下，用户仍能自然理解页面功能、操作方式和当前状态；高频操作应直接、清晰，避免多余步骤和无意义跳转。
- **前端体验属于验收条件**：信息层级与组件风格保持一致，并进行实际页面和交互检查；功能可用但 UI/UX 明显不合理，不视为完成。
- 构建：`web/app` 为独立 npm 工程（`pnpm` 或 `npm ci`），通过 `frontend-maven-plugin` 固定 Node 版本嵌入 Maven 构建，产物打进 `hop-web-app.jar` 的 `META-INF/resources/`（Servlet 3+ 静态资源）或 war 的 `/app/`。Hop 已用 `download-maven-plugin` 在构建期拉取 Monaco，证明"构建期取前端产物"在上游是可接受的。

### 2.3 画布：服务端权威 SVG 渲染 + 客户端交互层

两种候选：

| | A. 服务端渲染 SVG（复用 Painter） | B. 客户端渲染（React Flow/Vue Flow 从 JSON 画） |
|---|---|---|
| 视觉/语义一致性 | 与桌面/RAP 100% 一致：图标、hop 类型（info/error/copy/partition）、copies 文本、执行状态、慢 transform 指示、markdown 笔记、`PipelinePainter*` 扩展点（projects 环境横幅、Neo4j 等） | 需要用前端重画全部语义；扩展点画的内容不可见 |
| 交互延迟 | 拖拽需回程；用"客户端乐观移动 + mouseup 提交 + rev 缓存"可把感知延迟压到一次渲染 | 原生流畅 |
| 大图 | SVG 体积随节点线性增长（500 节点约数 MB），需增量/视口裁剪 | 天然分块 |
| 工作量 | 渲染 0，交互层中等 | 渲染大、长尾极长 |
| 插件兼容 | 完整（Painter 扩展点、`AreaType.CUSTOM`） | 需要新的前端扩展机制 |

结论：**PoC 与第一阶段采用 A**，同时 API 同步暴露 JSON 图模型（节点/hop/笔记/坐标），保证 B 可在后期作为"客户端渲染模式"叠加，渲染模式成为客户端的选择而非架构的约束。这一点与 Hop Web 现状的本质区别在于：**交互与 UI 状态全部在浏览器，服务端只负责模型与"视图快照"**，不再有服务端 SWT 事件处理和控件树同步。

### 2.4 "文档服务器"模型 vs RAP 的"控件服务器"模型

| | RAP（现状） | 本方案 |
|---|---|---|
| 服务端持有 | 每会话完整 SWT 控件树 + HopGui | 每会话：用户上下文、打开的文档（`PipelineMeta` 等）、撤销栈、运行中的执行 |
| 客户端持有 | 无状态镜像 | 全部 UI 状态（选择、缩放、面板布局、表单草稿） |
| 协议 | 控件属性/事件同步 | 语义化命令（addTransform/moveTransforms/addHop/setTransformConfig/undo…）+ 渲染快照 + SSE 事件 |
| 插件 UI | SWT 类 | Schema 表单 / 声明式布局 / 自带 JS bundle |
| 横向扩展 | 会话粘滞、无法拆分 | 会话粘滞（阶段 1–2）→ 文档状态可外置（阶段 3） |

为什么不做"客户端持有模型、服务端只负责保存"：`PipelineMeta` 的语义（`getPrevTransformFields`、`checkTransforms`、变量解析、hasLoop、插件 Meta 的 `getFields`）只存在于 Java，对话框的"Get fields""Test connection""Preview"都需要服务端模型。服务端权威 + 客户端乐观更新是唯一不复制引擎逻辑的方案。

---

## 3. 目标架构

### 3.1 分层

```
┌──────────────────────────────── 浏览器 ────────────────────────────────┐
│ SPA (web/app): 路由/布局 · 画布交互层(SVG overlay) · Schema 表单渲染器 ·  │
│ 插件自定义对话框宿主 · Monaco · 日志/指标面板 · VFS 浏览器 · 元数据编辑器  │
└────────────── REST/JSON  ·  SSE  ·  静态资源(插件 assets) ─────────────┘
┌──────────────────────── hop-web-api (web/api, JAX-RS) ────────────────┐
│ SessionResource · ProjectResource · VfsResource · DocumentResource      │
│ (open/graph/commands/render/save) · TransformConfigResource ·          │
│ FormSchemaService · ExecutionResource(v2, SSE) · MetadataResource(v2) · │
│ PluginResource(palette/icons/assets/i18n) · ContextActionResource       │
│ WebUserSession / WebSessionScope(IHopScope) / DocumentRegistry /        │
│ ExecutionRegistry / PermissionFilter(@RequirePermission)               │
└────────────────────────────────────────────────────────────────────────┘
┌──────────── hop-web-security (web/security, 从 rap 下沉) ──────────────┐
│ HopBasicAuthFilter · HopOidcAuthFilter · HopBearerSupport · LoginPage   │
└────────────────────────────────────────────────────────────────────────┘
┌──────────────── core + engine + plugins（原样复用，headless） ──────────┐
│ PipelineMeta/WorkflowMeta · XML · undo · Painter/SvgGc/CanvasSvgRenderer│
│ PipelineEngineFactory/IPipelineEngine · HopLogStore · ExecutionInfo ·   │
│ IHopMetadataProvider/JsonMetadataProvider · HopVfs · PluginRegistry ·   │
│ GuiRegistry(描述符) · HopSecurity/RBAC · AuditManager · projects 插件   │
└────────────────────────────────────────────────────────────────────────┘
```

### 3.2 模块划分（Maven）

| 模块 | artifactId | 依赖 | 内容 |
|---|---|---|---|
| `web/security` | `hop-web-security` | core, engine, jakarta.servlet | 从 `rap/.../security` 搬迁的过滤器与登录页（rap 改为依赖本模块，行为不变） |
| `web/api` | `hop-web-api` | core, engine, jersey, jackson, `hop-web-security` | **禁止依赖 `hop-ui`**（用 Maven enforcer 断言 classpath 无 `org.eclipse.swt`），保证 SWT-free |
| `web/app` | `hop-web-app` | 无 Java 依赖 | SPA 源码与构建产物 jar |
| `web/server` | `hop-web-server` | api, app, engine(www) | 独立 `hop-web` 启动器（阶段 2） |
| `assemblies/web` | 现有 war | + api, app, security | `web.xml` 增加 `/api/v2/*` 与 `/app/*`，RAP `/ui` 保持 |
| `web-tests` | 现有 | + api 契约测试 | 复用 Selenium/RBAC 测试基础设施 |

模块全部放在新的 `web/` 父目录下，由根 `pom.xml` 的一个 profile（如 `-Pweb-next`）控制是否参与默认构建，便于上游以"可选模块"接纳。

### 3.3 复用 / 抽象 / 替代清单

**原样复用（不改）**

| 领域 | 具体代码 |
|---|---|
| 模型与持久化 | `PipelineMeta`、`TransformMeta`、`PipelineHopMeta`、`NotePadMeta`、`WorkflowMeta`、`ActionMeta`、`WorkflowHopMeta`，`getXml/loadXml`，`XmlMetadataUtil`，`AbstractMeta` undo，`ChangeAction`，`PipelineMetaLayout` |
| 渲染 | `PipelinePainter`、`WorkflowPainter`、`SvgGc`、`HopSvgGraphics2D`、`AreaOwner`、`PipelineCanvasSvgRenderer`、`WorkflowCanvasSvgRenderer`、`CanvasSvgRenderResult`、Painter 扩展点 |
| 执行 | `PipelineEngineFactory`、`IPipelineEngine`、`IWorkflowEngine`、`IEngineComponent`、`EngineMetrics`、`PipelinePreviewFactory`、`IRowListener`、`PipelineRunConfiguration`/`WorkflowRunConfiguration`、所有引擎插件 |
| 日志/历史 | `HopLogStore`、`LoggingBuffer`、`LoggingRegistry`、`IHopLoggingEventListener`、`IExecutionInfoLocation` 及其实现、`LocationResource`（v1 API 直接复用） |
| 元数据 | `IHopMetadataProvider`、`JsonMetadataProvider`、`MultiMetadataProvider`、`JsonMetadataParser`、`@HopMetadata/@HopMetadataProperty`、`MetadataResource`（v1） |
| 文件 | `HopVfs`、`HopVfsNamespaces`、VFS 插件、`ExplorerFileServing` 的校验逻辑 |
| 插件 | `PluginRegistry`、Jandex 发现、插件 classloader、`@HopServerServlet` 服务端插件、`GuiRegistry` 中的 `GuiAction`/`GuiToolbarItem`/快捷键描述符（作为元数据，不作为 SWT） |
| 安全/审计 | `core/security/*`、`rap/security/*`（搬迁）、`CrossSiteRequestFilter`、`AuditManager` 与每用户审计目录策略 |
| 项目 | `ProjectsConfig`、`Project`、`LifecycleEnvironment`、`ProjectsUtil.enableProject(log, projectName, project, variables, configurationFiles, environmentName, hasHopMetadataProvider)`（SWT-free）、projects 的 HopRun/HopServer 扩展点 |
| i18n | `BaseMessages` + 各插件 `messages_*.properties` |
| 部署 | `docker/web.Dockerfile` 的目录布局与环境变量（`HOP_CONFIG_FOLDER`、`HOP_AUDIT_FOLDER`、`HOP_PLUGIN_BASE_FOLDERS`、`HOP_PROJECT_FOLDER`…） |

**抽象（新增 SWT-free 接口或把现有逻辑下沉；对上游单独有价值）**

| 编号 | 内容 | 来源 → 目标 |
|---|---|---|
| A1 | `WebSessionScope<T> implements IHopScope<T>`：HttpSession 槽 + 请求线程绑定 + `InheritableThreadLocal` | 仿 `rap/RapSessionScope` → `web/api` |
| A2 | `TransformMeta`/`ActionMeta` 配置的 JSON (反)序列化器，基于 `@HopMetadataProperty` 反射，与 `XmlMetadataUtil` 共享属性遍历逻辑；未注解者退化为 `{format:"xml", xml}` | 新增 → `core/metadata/serializer/json`（或 engine） |
| A3 | `FormSchemaGenerator`：由 `@HopMetadataProperty` + `HopMetadataPropertyType` + `@GuiWidgetElement` + i18n 生成表单 Schema JSON | 新增 → `core`（SWT-free） |
| A4 | GUI 插件描述符扫描的 SWT-free 版本：`HopGuiEnvironment.initGuiPlugins` 依赖 `org.eclipse.swt.SWT.ESC`，需在 core 提供不依赖 SWT 常量的扫描器（`GuiRegistry` 本身在 core） | `ui/HopGuiEnvironment` → `core` |
| A5 | 图编辑命令层 `PipelineEditor`/`WorkflowEditor`：把 `HopGuiPipelineGraph` 中不依赖 SWT 的编辑语义（新建 hop 的合法性、拆分 hop、对齐/分布、复制粘贴 XML 片段、笔记）提炼为可复用的服务端命令处理器；SWT 图编辑器后续可改为调用它（非必需） | 新增 → `engine` |
| A6 | `ExplorerFileServing` 下沉，供 VFS API 与 RAP 共用 | `ui` → `engine/www` |
| A7 | 安全过滤器下沉为 `hop-web-security` | `rap/security` → `web/security` |
| A8 | 插件 Web 资源约定：`META-INF/hop-web/<pluginId>/form.json`、`dialog.js`、`assets/**`，由 `PluginRegistry.getClassLoader(plugin).getResource` 发现 | 新约定，无需改插件类型 |
| A9 | Web 执行注册表：可选复用 `engine/www/PipelineMap/WorkflowMap` 的 `HopServerObjectEntry` 模型，使 Web 启动的执行也能被现有 `/hop/pipelineStatus`、`sniffTransform` 观察 | 设计决策，PoC 中评估 |

**替代（仅在 Web 部署中被新层取代；桌面继续使用）**

| 被替代 | 替代者 |
|---|---|
| `rap` 全部（RWT 引导、facade 实现、canvas JS 桥、log-console.js、主题 CSS） | `web/api` + `web/app` |
| `ui` 的 HopGui 壳、透视图、`HopGuiPipelineGraph`/`HopGuiWorkflowGraph` 的事件处理、`GuiCompositeWidgets`、`TableView` 等控件、`MetadataEditor` | SPA 路由与组件、Schema 表单渲染器、画布交互层 |
| 插件 `*Dialog`（SWT） | 分层：Schema 通用表单 → `form.json` 声明式布局 → `dialog.js` 自定义对话框（见第 6 节） |
| `web.xml` 中的 `RWTServlet /ui` | 阶段 3 可选移除 |

---

## 4. 数据模型与 API 边界

API 前缀 `/api/v2`（与现有 `/hop/api/v1` 并存；v1 的 `LocationResource`、`MetadataResource` 直接复用，v2 只补会话/项目感知的部分）。统一错误契约沿用 `HopApiExceptionMapper`。所有写操作要求 `Permission`（见 4.10）。

### 4.1 会话与用户上下文

```
WebUserSession
  id                 HttpSession id
  security           HopSecurityContext(username, roleIds, permissions, unrestricted)
  project/environment 当前项目名/环境名
  variables          IVariables（项目/环境变量已合并）
  metadataProvider   MultiMetadataProvider（同 ProjectsUtil.enableProject 构造）
  vfsNamespace       HopVfsNamespace
  audit              per-user IAuditManager（复用 HopWebAuditPaths 策略）
  documents          Map<docId, OpenDocument>
  executions         Map<execId, RunningExecution>
```

- 安装：Web 上下文初始化时 `HopMetadataInstance.setScope(new WebSessionScope<>(s -> s.metadataProvider))`、`HopVfsNamespaces.setScope(...)`、`AuditManager.setSessionAuditManagerProvider(...)`、`HopSecurity.setProvider(new WebSecurityContextProvider())`（读取 `HopSecurity.SESSION_CONTEXT_ATTRIBUTE`）。一个 `WebSessionFilter` 在每个请求开始把会话绑定到线程、结束时解绑。
- `GET /api/v2/session` → `{username, roles, permissions[], authMode, project, environment, locale, darkMode}`；`POST /api/v2/session/logout`。
- 认证方式与 RAP 完全一致（同一套过滤器、同一个 cookie），SPA 与 API 同源部署，CSRF 由 `CrossSiteRequestFilter` 的 `Sec-Fetch-Site` 策略覆盖 `/api/v2/*`。

### 4.2 项目与环境

- `GET /projects` → 来自 `ProjectsConfig`（`hop-config.json`）的项目/环境列表。
- `PUT /session/project {project, environment}` → 调用 `ProjectsUtil.enableProject(...)`，重建 `variables`/`metadataProvider`/`vfsNamespace`，触发 `HopExtensionPoint.HopProjectEnvironmentAfterEnabled`（以及 GUI 版本触发的 `HopGuiProjectAfterEnabled`，需评估监听者是否假定 `HopGui` 存在），写审计事件（"最近使用的项目"）。切换项目时关闭未保存文档需客户端先确认。
- 与 `HopWebEntryPoint` 一致：启动时从审计目录恢复上次项目/环境。

### 4.3 文件与 VFS

- `GET /vfs/roots` → `[{id:"project", name, uri}]`（项目根来自 `ProjectHomeExtensionPoint`/`HOP_PROJECT_FOLDER`，可扩展到其它 VFS 连接）。
- `GET /vfs/entries?uri=` → `[{name, uri, folder, size, modified, kind:"pipeline"|"workflow"|"json"|"text"|"binary"}]`（`HopVfs.getFileObject(uri, variables)`）。
- `GET/PUT /vfs/text?uri=`（`If-Match`/`ETag` = lastModified，冲突返回 409）；`POST /vfs/folders`、`/vfs/rename`、`/vfs/delete`（需 `EXPLORER_WRITE`）。
- `GET /vfs/raw/{token}/{relativePath}`：把 `ExplorerFileServlet` 的带 token 路径服务搬到 API，供 HTML 预览/图片。
- 路径安全：必须位于允许的根之内（`ExplorerFileServing.relativePath` 判定），扩展名白名单沿用。

### 4.4 文档（.hpl / .hwf）

```
POST   /documents                 {uri} | {kind:"pipeline"|"workflow", name}   → {docId, kind, name, uri, revision, changed}
GET    /documents/{id}            → 概要
GET    /documents/{id}/graph      → JSON 图模型（客户端渲染/大纲/搜索用）
GET    /documents/{id}/render?rev=&exec=&dark=&mag=   → 渲染快照（304 若 rev 相同）
POST   /documents/{id}/commands   {expectedRevision, commands:[...]}   → {revision, changed, results[]}
POST   /documents/{id}/save       {uri?}  → {uri, revision, lastModified}
POST   /documents/{id}/check      → checkTransforms/checkActions 结果（Hop 的 "verify" 功能）
DELETE /documents/{id}
```

**图模型（`graph`）**

```json
{
  "kind": "pipeline", "name": "load customers", "revision": 12,
  "transforms": [{"name":"Table input","pluginId":"TableInput","x":120,"y":80,"copies":"1","distribute":true,"description":"","icon":"/api/v2/plugins/transforms/TableInput/icon.svg"}],
  "hops": [{"from":"Table input","to":"Sort rows","enabled":true,"error":false,"info":false}],
  "notes": [{"id":0,"x":..,"y":..,"width":..,"height":..,"text":"...","markdown":true}],
  "parameters": [...], "info": {...}
}
```

**渲染快照（`render`）**——与 `CanvasRenderSnapshot.toJson()` 同构，改用 Jackson：

```json
{"revision":12, "svg":"<svg …>", "areas":[{"areaType":"TRANSFORM_ICON","x":..,"y":..,"width":..,"height":..,"hover":true,"owner":{"kind":"transform","name":"Table input"}}], "viewPort":{...}, "graphPort":{...}, "props":{"magnification":1.0,"iconSize":32,"gridSize":16,"dark":false}}
```

服务端用 `PipelineCanvasSvgRenderer.Context` 填充：`variables/pipelineMeta/canvasSize/offset/iconSize/lineWidth/gridSize/noteFont*/zoomFactor/darkMode/contrastingColorStrings`，`pipeline` 字段在传入 `exec=` 时指向运行中的引擎，从而在 SVG 上显示指标与状态（与 RAP 现状一致）。

**命令（`commands`）**：服务端唯一的写入口，每个命令映射到模型 API 并压入撤销栈：

| 命令 | 映射 |
|---|---|
| `addTransform {pluginId, name?, x, y}` | `PluginRegistry.loadClass` → `TransformMeta` → `PipelineMeta.addTransform` |
| `moveTransforms {names[], dx, dy}` / `moveNotes` | `TransformMeta.setLocation` |
| `renameTransform {name, newName}` | `TransformMeta.setName` + hop/引用更新（`PipelineTransformRenamed` 扩展点） |
| `setTransformConfig {name, config}` | A2 反序列化到 Meta 实例 |
| `setTransformProperties {name, copies, distribute, description}` | 对应 setter |
| `deleteTransforms {names[]}` / `deleteHops` / `deleteNotes` | `removeTransform`（`PipelineBeforeDeleteTransforms` 扩展点） |
| `addHop {from, to, streamType?}` | 校验 `hasLoop`、目标是否允许输入 → `addPipelineHop` |
| `setHopEnabled`、`flipHop`、`splitHop {hop, transformName}` | A5 |
| `addNote/updateNote/deleteNotes` | `NotePadMeta` |
| `autoLayout` | `PipelineMetaLayout` |
| `pasteXml {xml, x, y}` / `copyXml {names[]}` | 与桌面剪贴板格式兼容（`HopGuiPipelineClipboardDelegate` 的 XML 片段） |
| `undo` / `redo` | `AbstractMeta.previousUndo/nextUndo` + 反向应用 `ChangeAction`（沿用 `HopGuiUndoDelegate` 的应用逻辑，提炼为 SWT-free） |

Workflow 命令对称：`addAction/addWorkflowHop {from,to,evaluation,unconditional}/…`。

**乐观并发**：`expectedRevision` 不匹配返回 409 与最新 `graph`；同一用户多个标签页各自打开文档实例（PoC 不做同文档共享编辑）；保存时以文件 `lastModified` 做 ETag 检测他人覆盖。

### 4.5 Transform / Action 配置：JSON 形态与表单 Schema

```
GET /documents/{id}/transforms/{name}           → {name, pluginId, copies, distribute, description, config, configFormat}
PUT /documents/{id}/transforms/{name}/config     → 等价于 setTransformConfig 命令
POST /documents/{id}/transforms/{name}/fields/previous    → IRowMeta JSON（getPrevTransformFields）
POST /documents/{id}/transforms/{name}/fields/output      → getTransformFields
GET /plugins/transforms/{pluginId}/form?locale=            → 表单 Schema
```

- `config` 的键 = `@HopMetadataProperty.key`（与 XML 标签名一致，保证文档、注入元数据、搜索共用一套名字）；嵌套对象列表 → 数组；枚举按 `storeWithCode` 输出 code；密码字段按 `password=true` 在传输时保持加密形态（`Encr`），前端只显示掩码。`configFormat: "properties"|"xml"`，未注解的 6 个 Meta（其中 2 个手写 `getXml`）走 `xml`。
- **磁盘格式不变**：JSON 只是传输形态，保存永远经 `PipelineMeta.getXml(variables)`；这是 hop-run/HopServer/git 兼容性的底线。

**表单 Schema**（A3 生成）：

```json
{"pluginId":"TableInput","name":"Table input","category":"Input","documentationUrl":"…","customDialog":null,
 "fields":[
   {"key":"connection","label":"Connection","widget":"metadata","metadataType":"rdbms","variables":true},
   {"key":"sql","label":"SQL","widget":"code","language":"sql","variables":true},
   {"key":"limit","label":"Limit size","widget":"text","variables":true},
   {"key":"execute_each_row","label":"Execute for each row?","widget":"checkbox"},
   {"key":"field","label":"Fields","widget":"table","columns":[{"key":"name","widget":"text"},{"key":"type","widget":"combo","options":["String","Integer",…]}]}],
 "layout":{"tabs":[…]}}
```

推导规则：`HopMetadataPropertyType` → widget（RDBMS_CONNECTION→metadata(rdbms)、RDBMS_TABLE→table-picker、RDBMS_SQL→code(sql)、STREAM_FIELD/FIELD_LIST→combo(previous fields)、PIPELINE_FILE/WORKFLOW_FILE/FILE_PATH→vfs-picker、PIPELINE_RUN_CONFIG→metadata(pipeline-run-configuration)…）；`password` → password；boolean → checkbox；enum → combo；`List<Object>` → table（列由元素类递归推导）；标签取 `injectionKeyDescription` 经 `BaseMessages` 解析，否则人性化 key；`@GuiWidgetElement`（若存在）覆盖 type/label/toolTip/comboValuesMethod/order/variables；`layout` 来自插件 `form.json`（可无）。

### 4.6 元数据

- 复用 v1：`GET /hop/api/v1/metadata/types|list/{key}|{key}/{name}`、`POST`、`DELETE`。**注意**：v1 的 `HopServerApiContext` 从 `HopServerConfig` 取 provider；在 Web 里必须改为从当前会话取（`MultiMetadataProvider` 项目作用域）。方案：v2 提供同形态端点，内部复用 v1 资源逻辑并注入会话 provider。
- 新增：`GET /metadata/types`（富信息：key/name/description/image/category/documentationUrl 来自 `@HopMetadata`）、`GET /metadata/types/{key}/form`（同 A3，输入为元数据类；`PipelineRunConfiguration`、`BaseDatabaseMeta` 等本来就带 `@GuiWidgetElement`，通用表单质量较高）、`POST /metadata/{key}/{name}/actions/{action}`（如数据库 `test`、由插件 `@GuiWidgetElement(type=BUTTON)` 对应的方法，需要 SWT-free 的执行路径）。
- 写入后触发 `HopGuiMetadataObjectCreated/Updated/Deleted` 扩展点，使 projects 插件的 `AutoExportMetadataOn*` 继续工作。

### 4.7 执行、日志、指标、预览、历史

```
POST /documents/{id}/executions   {runConfiguration, logLevel, variables, parameters, clearLog}  → {executionId, logChannelId}
GET  /executions                  → 当前会话的执行列表
GET  /executions/{id}             → {status, startTime, endTime, errors, components:[{name, copy, linesRead, linesWritten, linesInput, linesOutput, linesRejected, linesUpdated, errors, status, duration, bufferSizes}]}
GET  /executions/{id}/log?from=&max=   → {lastLineNr, lines:[{nr, time, level, channelId, message}]}
GET  /executions/{id}/events      SSE: state | metrics | log | finished | error | rows(preview)
POST /executions/{id}/stop | pause | resume
DELETE /executions/{id}           → 释放日志（HopLogStore.discardLines）
POST /documents/{id}/preview      {transform, rows}  → 生成预览流水线（PipelinePreviewFactory）并以 SSE rows 事件回传
```

- 实现完全照搬 `HopGuiPipelineGraph` 的路径：`PipelineEngineFactory.createPipelineEngine(runConfigName, metadataProvider, pipelineMeta)` → `prepareExecution` → `startThreads`，`addExecutionFinishedListener` 推 `finished`；指标由定时器（1s）读取 `getEngineMetrics()`/`getComponents()`；日志由 `LoggingBuffer.addLoggingEventListener` 过滤 `logChannelId` 及其子通道（`LoggingRegistry.getLogChannelChildren`）后推送，重连时用 `from=` 补齐。
- 远程/Beam 引擎无需特殊处理：`IPipelineEngine` 抽象已覆盖（`RemotePipelineEngine` 自己轮询 HopServer）。
- 执行历史：直接调用 v1 `LocationResource`（`/location/{name}/executions…`），Execution 透视图在 Web 里就是这个 API 的客户端；渲染历史执行的画布用 `render?exec=` 传入从 `ExecutionState` 还原的指标（`ExecutionPerspective` 今天也是从 `IExecutionInfoLocation.getExecutionState` 取的）。
- 生命周期：执行归属会话但**不随 HTTP 会话过期而中断**（用户希望关闭浏览器后任务继续）；`ExecutionRegistry` 以 TTL + 显式删除回收，未认领的已完成执行按配置（如 24h）清理日志。这与 HopServer `PipelineMap` 的策略相同（A9 评估直接复用）。

### 4.8 实时通道（SSE）

- 每个文档/执行一个事件流；事件带单调 `seq`，客户端断线用 `Last-Event-ID` 续传；服务端每 15s 心跳。
- 通知系统（`ui/.../notifications`）后期也走同一通道（`notification` 事件）。

### 4.9 插件资源

- `GET /plugins/transforms`、`/plugins/actions`（调色板：id/name/description/category/keywords/image，来自 `PluginRegistry.getPlugins(TransformPluginType)` 与 `@Transform`/`@Action` 注解）。
- `GET /plugins/{type}/{id}/icon.svg`（`SvgCache` + 插件 classloader，等价于 `HopWeb.addResource` 做的事）。
- `GET /plugins/{type}/{id}/assets/{path}`：提供 `META-INF/hop-web/<pluginId>/assets/**`（A8），带 `Content-Type` 白名单与缓存头。
- `GET /plugins/{type}/{id}/form`：Schema（含 `customDialog` 指向 `dialog.js` 的 URL，若存在）。
- i18n：插件标签在服务端按会话 locale 解析后放进 Schema，前端不需要加载 Java 资源包；SPA 自身 chrome 的翻译放在前端。

### 4.10 权限模型映射

复用 `core/security/Permission`：

| 端点 | 权限 |
|---|---|
| `GET /documents/*`、`/vfs/entries|text(GET)`、`/render`、`/graph` | `FILE_VIEW` |
| `POST /documents`（新建） | `FILE_CREATE` |
| `commands`、`PUT config` | `FILE_EDIT` |
| `save` | `FILE_SAVE` |
| `/vfs/delete|rename|folders`、`PUT /vfs/text` | `EXPLORER_WRITE` / `FILE_DELETE` |
| `POST executions`、`preview` | `RUN_EXECUTE` |
| `stop|pause|resume` | `RUN_STOP` |
| 元数据读/写 | `METADATA_READ` / `METADATA_WRITE` |
| 配置页 | `CONFIG_GUI` / `CONFIG_SYSTEM` |
| 用户/角色管理 | `SECURITY_MANAGE` |

实现为 Jersey `ContainerRequestFilter` + `@RequirePermission(Permission.X)` 注解；上下文来自 `HopSecurity.getContext()`（会话绑定）。`HopSecurityPrivilegeMode`（受限/特权模式）与 `HopDialogEditGuard`/`IDialogEditable` 的"只读打开"语义在 Schema 中以 `readOnly` 标记传给前端。

---

## 5. 画布实现

客户端交互层（SPA）：

1. 加载 `render` 快照，把 `svg` 注入一个 `<div>`（`innerHTML`，服务端已保证 SVG 由 Batik 生成、无脚本），在其上叠加透明 SVG `<g>` 作为交互层；`areas` 构建 R-tree/网格索引做命中测试（与 `canvas-svg.js` 的 `getVisibleArea` 等价）。
2. 状态机：`idle → hover(area) → select/多选(lasso) → drag(节点/笔记) → connect(从 TRANSFORM_TARGET_HOP_ICON/ACTION 拖出) → contextMenu`。拖拽期间用 CSS transform 平移对应 `<g>`（服务端 SVG 需为每个 transform/note 输出稳定的 `id`/`data-hop-*` 属性——`SvgGc` 需要小改以写出分组与属性，属于 A5/渲染增强）；mouseup 才发 `moveTransforms`，随后拉取新快照替换。
3. 缩放/平移在客户端（CSS transform），只有当放大倍数导致文字/图标分辨率不足时才请求 `mag=` 重渲染；`props.magnification` 与 `offset` 语义与 `canvas-svg.js` 的 `graphCoords` 相同。
4. 右键/上下文：`GET /documents/{id}/context-actions?target=transform&name=` 返回 `GuiRegistry` 中的 `GuiAction` 描述符（id/name/tooltip/image/category/keywords 本来就是纯数据），第一阶段只暴露一份 Web 原生实现的白名单（edit/delete/duplicate/copy/detach/enable-disable hop/preview/debug/show output fields/help），其余 `@GuiContextAction`（大多调用 SWT 对话框）以"未在 Web 中提供"标记；第二阶段引入 `WebContextActionHandler` 机制让插件按 action id 注册 Web 实现。
5. 双击节点 → 打开配置（第 6 节）。运行中 → 定时以 `render?exec=` 刷新以显示指标（或后期只增量刷新徽标层）。
6. 大图策略：`viewPort/graphPort` 已由 Painter 给出；第一阶段接受整幅 SVG；若 PoC 测得 300+ 节点渲染 > 100ms，则加入"按视口裁剪渲染"（`PipelinePainter` 已有 `showingNavigationView`/`maximum`，扩展为区域裁剪）。

Workflow 画布完全对称（`WorkflowCanvasSvgRenderer`，`ACTION_*`、`WORKFLOW_HOP_*` 区域类型）。

---

## 6. Transform / Action 配置界面迁移

这是整个工程最大的存量：239 个 SWT 对话框、约 14 万行、大量隐藏在对话框里的业务逻辑（"Get fields" 访问数据库、动态启停控件、字段校验）。策略是**分层、按使用频率推进、永不阻塞**：

| 层 | 机制 | 覆盖 | 质量 | 谁来做 |
|---|---|---|---|---|
| T0 通用表单 | Schema（A3）+ 通用渲染器 | 175/181 transform Meta 立即可用；action 视注解覆盖率 | "属性编辑器"级：正确但不精致 | 平台 |
| T1 声明式布局 | 插件 jar 内 `META-INF/hop-web/<id>/form.json`：tabs/groups/order/labels/依赖显示（`visibleWhen`）、按钮绑定服务端动作 | 目标：使用最多的 40–60 个 transform/action | 接近 SWT 对话框 | 平台 + 社区，无需改 Java |
| T2 自定义对话框 | `META-INF/hop-web/<id>/dialog.js`（ES module，默认导出 `mount(container, api)`）；`api` 提供 `getConfig/setConfig/prevFields/metadataPicker/vfsPicker/variables/callAction/monaco` | 复杂对话框（TextFileInput、ExcelInput、GetXmlData、Rest、Mail、JavaScript、Formula、Calculator、Switch/Case、Mapping…约 20 个） | 完全定制 | 插件作者 |
| T2 服务端动作 | 插件通过现有 `@HopServerServlet`/`IHopServerPlugin`（或新增 `@HopWebAction` 方法注解）提供 "get fields from file"、"preview SQL"、"test" 等 | 按需 | — | 插件作者 |
| T3 逃生舱（不作为架构支撑） | 在同一 war 内保留 RAP `/ui`，SPA 以"在经典界面中打开此文件"深链接过渡 | 全部 | 旧体验 | 无开发量 |

通用控件的 Web 对应物（SPA 组件库，作为 T0/T1/T2 共享基础）：

| SWT 控件 | Web 组件 | 依赖 API |
|---|---|---|
| `TextVar`/`LabelTextVar`/`PasswordTextVar` | 变量感知输入（Ctrl+Space 变量选择器，`${}` 高亮） | `GET /session/variables` |
| `ComboVar` | 可输入组合框 | 同上 |
| `TableView` + `ColumnInfo`（TEXT/CCOMBO/BUTTON/TEXT_BUTTON/FORMAT/ICON） | 可编辑表格（行增删、粘贴、列类型：text/combo/button/format），"Get fields" 工具条 | `fields/previous` |
| `MetaSelectionLine` | 元数据选择器（选/新建/编辑） | v1/v2 metadata |
| `StyledTextComp`/`TextComposite` | Monaco（SQL/JS/JSON/Markdown 语法） | — |
| VFS 文件选择对话框（`HopVfsFileDialog`） | VFS 浏览器弹窗 | `/vfs/*` |
| `ConditionEditor` | 条件树编辑器（T2） | — |
| 字段/类型选择、格式选择 | combo + `ValueMetaFactory` 类型列表 | `GET /plugins/value-types` |

排序依据：先跑一遍全体 Meta 的 Schema 生成与 JSON 往返测试得到覆盖率报告，再按样例项目、Marketplace 与社区调查确定 T1/T2 优先清单。**任何时刻，所有 transform 都至少有 T0 可用**，这是与"必须先重写 239 个对话框才能上线"的本质区别。

---

## 7. 插件兼容性与插件自带 Web UI

- **零改动兼容**：插件的 Meta/Transform/Action/Database/VFS/引擎/Execution Info 类原样加载（与 HopServer 相同）；SWT Dialog 类保留在 jar 中供桌面使用，Web 从不加载。
- **发现机制不变**：仍是 Jandex 索引 + `HOP_PLUGIN_BASE_FOLDERS`；Web 资源通过 classloader 资源约定（A8）发现，无需新的插件类型，也不需要修改 `@Transform` 注解；可选地在后续版本给 `@Transform/@Action/@HopMetadata` 增加 `webDialog` 属性作为显式声明。
- **插件 Web UI 契约（草案）**
  - `META-INF/hop-web/<pluginId>/form.json`：布局；版本字段 `schemaVersion`。
  - `META-INF/hop-web/<pluginId>/dialog.js`：ES module，`export default { mount(container, api), unmount() }`；不得访问全局路由/状态；样式作用域化；与宿主通过 `api` 通信。
  - `META-INF/hop-web/<pluginId>/assets/**`：图片/样式/字典。
  - 服务端动作：`@HopServerServlet(id="…", requiredPermission="file.edit")` 现有机制即可提供 JSON 端点（已被 RBAC 覆盖）。
- **透视图插件**：`IHopPerspective` 是 SWT 绑定的；Web 端定义 `IWebPerspective`（路由 id、图标、菜单、`entry.js`）并同样以资源约定发现；Git/Neo4j/AI Advisor 透视图按此逐个迁移（阶段 2）。
- **GUI 扩展点**：`GuiRegistry` 中的工具栏/菜单/快捷键/上下文动作描述符原样读取；执行其方法时若方法签名含 SWT 类型则跳过（`HopGuiEnvironment` 已对 web 做类似排除），否则通过反射调用（很多 `@GuiToolbarElement` 方法本身是调 HopGui 的，需要在 Web 侧提供 `HopGui`-free 的等价宿主对象，属阶段 2 工作）。
- **版本兼容**：`form.json`/`dialog.js` 契约随 `hop-web-api` 版本演进，遵循 Hop 现有的插件版本策略（`version.xml`）。

---

## 8. 横切关注点

| 关注点 | 处理方式 | 依据 |
|---|---|---|
| 执行 | 会话内 `ExecutionRegistry`，引擎实例与桌面完全相同；远程引擎/HopServer 不变；执行不随浏览器关闭而停止 | `PipelineEngineFactory`、`HopGuiPipelineGraph:6112-6598` |
| 日志 | 进程级 `HopLogStore` 按通道过滤 + SSE；每执行上限沿用 `HOP_MAX_LOG_SIZE_IN_LINES`；关闭执行时 `discardLines`；多用户下的内存上限需要一个全局配额（新配置项） | `LoggingBuffer`、`HopGuiPipelineLogDelegate:180` |
| 文件 | VFS API + 项目根限制 + ETag；所有路径经 `HopVfs.getFileObject(uri, variables)`，从不拼接本地路径 | `ExplorerFileServing`、`HopVfsNamespaces` |
| 项目 | 会话级项目/环境，与 `hop-run`/HopServer 的 `ProjectsUtil` 同一代码路径；服务器可配置"锁定项目"（多租户部署时禁止切换） | `ProjectsUtil.enableProject` |
| 元数据 | 会话级 `MultiMetadataProvider`；项目切换时重建；写操作触发 GUI 扩展点保持 auto-export 行为 | `HopMetadataInstance` + `IHopScope` |
| 权限 | `HopSecurityConfig` 四模式 + `Permission` + 现有过滤器；API 级注解；前端按 `permissions[]` 隐藏入口但不以此为安全边界 | `core/security`、`rap/security` |
| 多用户状态 | 每用户：审计目录（最近文件、主题、项目）、`PropsUi` 等价物改为服务端存储在审计目录的 JSON（`HopWebEntryPoint` 已这样存主题）；共享：插件、配置、日志存储；显式不共享：文档实例（阶段 3 再考虑协同） | `HopWebAuditPaths` |
| 并发与一致性 | 文档 revision（乐观锁）、文件 ETag、同文件多用户保存冲突 409 并提示 | — |
| 可观测性 | `/api/v2/health`、会话/文档/执行计数指标（后期 Prometheus 端点）、请求日志走 `LogChannel.UI` 等价通道 | — |
| 主题/i18n | 服务端仅提供 `dark` 给 Painter（`contrastingColorStrings`）；SPA 自管主题；插件标签服务端解析 | `PipelineCanvasSvgRenderer.Context.darkMode` |

---

## 9. 渐进迁移路线

```
Phase 0  PoC（6–8 周）          验证 5 个假设；同 war 共存；/app 入口默认隐藏
Phase 1  日常可用的编辑器（3–4 月） pipeline+workflow 编辑、T0 全量 + T1 前 40、通用元数据编辑器、
                                  执行历史、VFS/Monaco、快捷键、暗色、i18n、通知；/app 成为可选默认
Phase 2  插件 Web UI 生态（持续）  dialog.js/form.json 契约 1.0；复杂对话框迁移；上下文动作/工具栏插件的
                                  Web 宿主；透视图插件（Git/Neo4j/AI）；HopServer 与 Hop Web 同进程
Phase 3  独立 Hop Web             嵌入式 Jetty 启动器；RAP 在 web 装配中变为可选/移除；多用户增强
                                  （文档锁、协作）、水平扩展（会话/文档状态外置）
```

每一阶段都保持：`.hpl/.hwf` 格式不变、插件二进制兼容、桌面 GUI 不受影响、RAP 可回退。

---

## 10. 第一阶段 PoC

### 10.1 目标

用最小的垂直切片证明架构成立，而不是做一个"能看的 demo"。切片：登录（NONE/BASIC）→ 选项目 → 浏览项目文件 → 打开 `.hpl` → 服务端 SVG 画布 + 客户端交互（选择/拖动/新增/连线/删除/撤销）→ 打开 3 个 transform 的**通用表单**（Table input：元数据选择 + SQL Monaco；Select values：字段表格 + Get fields；Sort rows：表格）→ 保存（XML 往返）→ 以 `local` 运行配置运行 → SSE 日志 + 指标叠加 → 停止 → 查看执行历史（v1 API）→ 一个自带 `form.json` + `dialog.js` 的示例插件被发现并渲染。

### 10.2 要验证的技术假设

| # | 假设 | 验证方法 | 通过标准 |
|---|---|---|---|
| H1 | **会话作用域可以脱离 RAP**：`IHopScope` 的 Web 实现能在一个 JVM 里让多个用户各自持有项目/元数据/VFS 命名空间，并被执行线程正确继承 | 嵌入式 Jetty 集成测试：20 个并发会话，各自打开不同项目并运行引用同名连接的流水线 | 0 次跨会话泄漏；执行线程解析到的连接属于发起会话 |
| H2 | **Transform 配置可以注解驱动 JSON 往返而不破坏 XML** | 对 `config/projects/samples` 全部 `.hpl/.hwf`：load → JSON → 反序列化到新 Meta → `getXml` 与原 XML 做语义 diff；再对全部 181 个 transform Meta 生成 Schema | ≥ 90% 的 transform 类型往返无差异，失败清单明确且可归因；Schema 生成 0 异常 |
| H3 | **服务端 SVG + 客户端交互的延迟可接受** | 200 节点流水线：拖动一个节点的端到端时间（mouseup → 新快照渲染完成）与服务端 `render` 耗时 p95 | 服务端 < 50ms，端到端 < 150ms（局域网）；缩放/平移 0 回程 |
| H4 | **执行、日志、指标能通过 SSE 实时到达浏览器且不干扰引擎** | 运行 100 万行的样例流水线，观察日志顺序、丢失、内存；断线重连补齐 | 无丢行、无乱序；服务端每执行日志内存有界；引擎吞吐下降 < 5% |
| H5 | **插件可以在不改 core/ui 的前提下自带 Web UI** | 示例插件 jar 含 `form.json` + `dialog.js`，放进 `plugins/` 目录即可被发现、加载、与宿主 API 交互 | 无需重启前端构建；RBAC 生效；无全局命名空间污染 |

### 10.3 范围外（明确不做）

Workflow 编辑（仅渲染）、笔记编辑、分区/集群、Git/搜索/数据库/AI 透视图、通知、元数据编辑器（仅用 v1 API 读）、上下文动作全集、快捷键全集、i18n 全集、客户端渲染模式、协同编辑。

### 10.4 工作分解与第一步实现方式

1. **骨架（第 1 周）**
   - 新建 `web/pom.xml`、`web/api`、`web/app`、`web/security`（先只把过滤器搬过去，rap 改依赖）；根 pom 增加 `web-next` profile。
   - `HopWebApiApplication extends ResourceConfig`：显式注册资源、`JacksonFeature.withoutExceptionMappers()`、`HopApiExceptionMapper`、HK2 绑定 `WebSessionRegistry`。
   - `HopWebApiContextListener`：`HopEnvironment.init()` + SWT-free 的 GUI 描述符扫描（A4 初版可先只注册 `GuiPluginType` 并跳过含 SWT 的方法）+ 安装 `WebSessionScope` + 审计/安全 provider（完全对照 `HopWebServletContextListener`）。
   - `assemblies/web/web.xml`：Jersey `ServletContainer` 映射 `/api/v2/*`（在现有 auth 过滤器之后），静态 `/app/*`。
   - 验收：`curl -u admin:admin http://localhost:8080/api/v2/session` 返回用户上下文；`GET /api/v2/projects`。
2. **文档与渲染（第 2 周）**：`DocumentResource.open/render/graph`，直接调用 `PipelineCanvasSvgRenderer`；先用一个无框架的 HTML 页面验证 SVG + `areas` 命中（H3 的服务端半边）。
3. **命令与撤销与保存（第 3 周）**：`commands` 端点、`AbstractMeta` 撤销、`save` + ETag；建立 XML 往返测试（H2-A）。
4. **配置 JSON 与 Schema（第 4–5 周）**：A2、A3、`fields/previous`；SPA 通用表单渲染器；覆盖率脚本跑全部 Meta（H2-B）。
5. **执行与 SSE（第 6 周）**：`ExecutionRegistry`、SSE 事件流、指标叠加渲染（H4）。
6. **多会话隔离测试（第 7 周）**：嵌入式 Jetty + 两个项目的自动化测试（H1）。
7. **插件示例（第 8 周）**：`plugins/misc/web-ui-sample`（或 `web/samples`）含 `form.json`/`dialog.js`（H5）；整理 PoC 报告：覆盖率、延迟、失败清单、下一阶段建议。

测试策略：`web/api` 单元测试（序列化、Schema、Scope）；HTTP 契约测试（JUnit + Jetty embedded + Java HttpClient）；`web-tests` 增加一条 Playwright/Selenium 冒烟（打开样例、拖动、保存）。

---

## 11. 主要技术风险与缓解

| 风险 | 说明 | 缓解 |
|---|---|---|
| R1 JSON 序列化保真 | 字段顺序、null/默认值、`inline`、`storeWithName/Code`、枚举、嵌套列表、`injectionConverter`、缺失插件 XML 保留、6 个未注解 Meta | XML 语义 diff 测试覆盖全部样例；未注解走 `xml` 形态；与 `XmlMetadataUtil` 共享遍历逻辑避免两套语义 |
| R2 对话框长尾 | 239 个对话框里的业务逻辑（Get fields、动态启停、校验）不在 Meta 里 | T0 保底 + T1/T2 按使用频率推进；服务端动作机制；用覆盖率仪表盘公开进度 |
| R3 多用户进程模型 | 除元数据/VFS 外仍有静态单例（`PropsUi`、`Const.setClientOsProvider`、`HopVfs` 文件系统管理器缓存、数据库连接池、`Variables.getADefaultVariableSpace`） | 与 RAP 面临相同问题，逐个纳入 `IHopScope` 或会话对象；H1 测试作为回归门槛 |
| R4 内存与生命周期 | 日志缓冲、文档实例、执行对象随用户数线性增长 | 全局配额 + TTL + 显式释放；执行与会话解耦；指标可观测 |
| R5 大图 SVG | 体积与渲染时间 | rev 缓存、视口裁剪、后期客户端渲染模式 |
| R6 安全面扩大 | VFS 读写、任意流水线执行、SSE 与 cookie | 全端点 `@RequirePermission`；路径根限制；`CrossSiteRequestFilter`；密码字段不回传明文 |
| R7 构建与发布合规 | Node 工具链进入 Maven；ASF 源码发布不能含二进制；npm 依赖许可证 | `frontend-maven-plugin` 固定版本；仅 Category A 依赖并生成 THIRD-PARTY 清单；RAT 排除生成物；源码包不含 `node_modules` |
| R8 社区接受度与双轨 | 上游近期仍在投入 RAP（`66d37b57` DnD、`86af47d2` clipboard 等） | 可选模块 + HIP + 小 PR 先行；不改 RAP 行为；明确"RAP 是回退路径"而非竞争者 |
| R9 i18n | 插件标签是 Java 资源包 | 服务端解析进 Schema；SPA chrome 独立翻译 |
| R10 撤销/变更语义 | `ChangeAction` 的反向应用逻辑目前在 `HopGuiUndoDelegate`（SWT 侧） | 提炼为 engine 级 `UndoApplier`（A5 的一部分），桌面与 Web 共用 |

---

## 12. 上游可接受性策略

1. **形式**：作为独立可选模块 `web/`（profile 控制）提交，不修改现有模型契约、不改变桌面/RAP 行为、不新增 `hop-ui` 依赖；PoC 阶段甚至可以先在本 fork 完成，再以 HIP（Hop Improvement Proposal）形式在 `dev@hop.apache.org` 讨论。
2. **拆分为对上游本身有价值的小 PR**（不依赖 Web 层也说得通）：
   - `hop-web-security` 下沉（消除 rap 与未来 HopServer 的重复）；
   - Transform 配置 JSON 序列化器（HopServer API、`hop-conf`、元数据注入都能用）；
   - `FormSchemaGenerator`（可作为文档生成、`hop-conf` 校验的基础）；
   - `ExplorerFileServing` 下沉；
   - `HopGuiEnvironment` 的 SWT-free 描述符扫描；
   - `SvgGc` 输出稳定 `id`/`data-*` 属性（RAP 的 `canvas-svg.js` 同样受益）。
3. **不与 RAP 抢跑道**：把 RAP 定义为"经典 Web UI"，新层为"下一代 Web UI"，共享认证、插件、配置、Docker 镜像；用户可通过 URL 选择。只有当新层达到日常可用（Phase 1 末）并被社区验证后，才讨论默认入口切换。
4. **治理与质量**：遵循仓库既有规范（spotless、RAT、Jandex 索引、`web-tests` CI 矩阵）；前端依赖许可证审计进入 CI；文档进入 `docs/hop-dev-manual`（新增 `hopweb-next` 章节）。
5. **避免长期私有 fork 的关键**：上述 A1–A9 抽象一旦进入上游，fork 中只剩 `web/` 目录的纯增量代码，与上游 main 的冲突面趋近于零。

---

## 附录 A：关键源码索引

| 主题 | 位置 |
|---|---|
| RAP 引导 | `rap/src/main/java/org/apache/hop/ui/hopgui/{HopWeb, HopWebEntryPoint, HopWebServletContextListener, HopGuiImpl, RapSessionScope}.java` |
| 作用域抽象 | `core/src/main/java/org/apache/hop/core/scope/IHopScope.java`；`core/.../metadata/util/HopMetadataInstance.java`；`core/.../core/vfs/HopVfsNamespaces.java` |
| Facade 机制 | `ui/src/main/java/org/apache/hop/ui/hopgui/{ImplementationLoader, ISingletonProvider, *Facade}.java`；`ui/.../util/EnvironmentUtils` |
| Web 画布 | `rap/.../canvas/{CanvasRenderServiceHandler, CanvasSvgRendererHandler, CanvasRenderSnapshot, CanvasGraphRegistry, CanvasInteractionHandler, AreaOwnerJsonSerializer}.java`；`rap/src/main/resources/org/apache/hop/ui/hopgui/canvas-svg.js`；`ui/.../hopgui/CanvasSvgFacade.java`；`ui/.../hopgui/shared/IWebCanvasGraph.java` |
| SWT-free 渲染 | `engine/src/main/java/org/apache/hop/pipeline/canvas/PipelineCanvasSvgRenderer.java`；`engine/.../workflow/canvas/WorkflowCanvasSvgRenderer.java`；`engine/.../core/gui/{SvgGc, IGc, BasePainter, AreaOwner}.java`；`engine/.../pipeline/PipelinePainter.java` |
| 模型 | `engine/.../pipeline/PipelineMeta.java`；`engine/.../pipeline/transform/{TransformMeta, BaseTransformMeta, ITransformMeta, ITransformDialog}.java`；`engine/.../base/AbstractMeta.java`；`engine/.../core/undo/ChangeAction.java`；`engine/.../pipeline/PipelineMetaLayout.java` |
| 元数据注解与序列化 | `core/.../metadata/api/{HopMetadata, HopMetadataProperty, HopMetadataPropertyType}.java`；`core/.../metadata/serializer/xml/XmlMetadataUtil.java`；`core/.../metadata/serializer/json/{JsonMetadataParser, JsonMetadataProvider}.java` |
| GUI 注解与自动表单 | `core/src/main/java/org/apache/hop/core/gui/plugin/{GuiWidgetElement, GuiElementType, GuiRegistry, action/GuiAction}.java`；`ui/.../core/gui/GuiCompositeWidgets.java`；`ui/.../hopgui/HopGuiEnvironment.java` |
| 对话框发现 | `ui/.../hopgui/file/pipeline/delegates/HopGuiPipelineTransformDelegate.java:87-157`；`engine/.../pipeline/transform/BaseTransformMeta.java:518`；`engine/.../workflow/action/ActionBase.java:766`；`ui/.../core/metadata/MetadataManager.java:592-601` |
| 执行 | `engine/.../pipeline/engine/{PipelineEngineFactory, IPipelineEngine, IEngineComponent}.java`；`engine/.../pipeline/PipelinePreviewFactory.java`；`ui/.../file/pipeline/HopGuiPipelineGraph.java:6112-6598` |
| 日志 | `core/.../core/logging/{HopLogStore, LoggingBuffer, LoggingRegistry, IHopLoggingEventListener}.java` |
| 执行历史 | `engine/src/main/java/org/apache/hop/execution/IExecutionInfoLocation.java`；`ui/.../perspective/execution/ExecutionPerspective.java` |
| 现有 JSON API | `engine/src/main/java/org/apache/hop/www/api/{HopApiApplication, HopServerApiContext}.java`；`engine/.../www/api/v1/resources/{ExecutionResource, LocationResource, MetadataResource, PluginsResource}.java`；`engine/.../www/{WebServer, HopServerServlet, IHopServerPlugin, CrossSiteRequestFilter}.java` |
| 安全 | `core/src/main/java/org/apache/hop/core/security/*`；`rap/src/main/java/org/apache/hop/ui/hopgui/security/*` |
| 审计 | `rap/.../{HopWebAuditManagerProvider, HopWebAuditPaths}.java` |
| Explorer 文件服务 | `rap/.../explorer/*`；`ui/.../perspective/explorer/web/ExplorerFileServing.java` |
| 项目 | `plugins/misc/projects/src/main/java/org/apache/hop/projects/{util/ProjectsUtil, gui/ProjectsGuiPlugin, config/ProjectsConfig, xp/*}.java` |
| 插件发现 | `core/.../core/plugins/{PluginRegistry, JarCache, BasePluginType}.java`；`assemblies/shared/hop-plugin-libs.xml` |
| 打包与部署 | `assemblies/web/src/main/resources/WEB-INF/web.xml`；`docker/web.Dockerfile`；`docker/run-hop-web-local*.sh`；`docs/hop-dev-manual/modules/ROOT/pages/hopweb/*.adoc` |
| 测试 | `web-tests/src/test/java/org/apache/hop/web/it/*`；`web-tests/api-checks/*` |

## 附录 B：待核实事项（PoC 立项前）

1. `ProjectsGuiPlugin.enableHopGuiProject` 相比 `ProjectsUtil.enableProject` 多做了哪些 GUI 相关步骤（审计事件、变量注入 HopGui、`HopGuiProjectAfterEnabled` 监听者是否依赖 `HopGui`）。
2. `@GuiContextAction`/`@GuiToolbarElement` 方法中有多少签名不含 SWT 类型、可直接反射执行。
3. `HopGuiUndoDelegate` 中 `ChangeAction` 反向应用逻辑的 SWT 依赖程度。
4. Jersey SSE（`jersey-media-sse`）与 Jetty 12 ee11 在 Tomcat 10 war 中的兼容性；Tomcat 与 Jetty 对 SSE 长连接的超时配置。
5. `SvgGc`/`HopSvgGraphics2D` 是否已能为每个元素输出稳定 id（若无，评估 Batik `SVGGraphics2D` 的分组能力或改用手写 SVG writer）。
6. `@Transform`/`@Action` 注解的完整属性列表与 `classLoaderGroup` 对资源发现的影响。
7. `HopServerApiContext` 在 Web 部署下从 `HopServerSingleton` 取 provider 的行为是否与会话作用域冲突（v1 元数据 API 在 Hop Web 中当前实际使用的是哪一个 provider）。
