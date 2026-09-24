# Hop Modern Web — Existing Asset Inventory

Baseline: `f7c2694a0b66a151eb244a7e9c11beff46b18421`

This inventory classifies the current Worker changes for the standalone-repository refocus.

## Keep in Hop fork

These changes modify Hop engine/core semantics and should remain small upstream-style patches.

### Task 2
- `engine/src/main/java/org/apache/hop/base/AbstractMeta.java`
- `engine/src/main/java/org/apache/hop/pipeline/editor/PipelineEditor.java`
- `engine/src/main/java/org/apache/hop/pipeline/editor/PipelineUndoApplier.java`
- `engine/src/test/java/org/apache/hop/pipeline/editor/PipelineEditorTest.java`

Reason: semantic document commands / undo belong next to `PipelineMeta`, not in the React product.

The current `PipelineCanvasSvgRendererTest` changes should be reviewed only as regression/reference work; SVG is no longer the primary browser canvas.

### Task 3
- `core/src/main/java/org/apache/hop/metadata/serializer/json/ConfigJsonSerializer.java`
- `core/src/test/java/org/apache/hop/metadata/serializer/json/ConfigJsonSerializerTest.java`

Reason: this serializer understands Hop metadata annotations and is useful beyond the browser product.

## Migrate/recreate in standalone repo

### Task 1
- `web/api/.../session/WebSessionScope.java`
- `web/api/.../session/WebSessionRegistry.java`
- matching tests

Treatment: move/recreate only the product-specific session adapter pieces under standalone `server/`. Do not preserve the old Hop-root `web/` Maven layout merely for historical continuity.

### Task 4 / current PoC execution code
- `ExecutionEvent`
- `ExecutionEventBuffer`
- `ExecutionEventStream`
- `ExecutionLogPublisher`
- `ExecutionMetricsPublisher`
- `ExecutionRegistry`
- `PipelineExecutionLifecycle`
- matching tests

Treatment: these are useful product-server assets but are **deferred** until the editor vertical slice exists. Migrate them later into standalone `server/`; do not keep extending them in the Hop fork.

## Do not migrate as product source

- root `pom.xml` changes whose only purpose is to add old experimental Web modules;
- `web/pom.xml`, `web/api/pom.xml`, `web/security/pom.xml` as a required product layout;
- root temporary `hop-web-api/` module layout;
- RAP/SWT UI source;
- Hop plugin source tree;
- the full Hop `core/` / `engine/` source trees.

## Current conclusion

The useful work does **not** require carrying the Hop monorepo into the modern product repository.

At present the truly Hop-native patch surface is small:
- Pipeline editor/undo extraction in `engine`;
- Config JSON serializer in `core`;
- future Graph Projection helpers only where they genuinely belong in `engine`.

Everything else can live in the standalone product repository and consume the Hop fork as a dependency/build input.
