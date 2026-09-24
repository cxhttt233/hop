# Hop Modern Web — Standalone Repository Migration

## Decision

The modern browser product should move to a standalone repository:

`cxhttt233/hop-modern-web`

The standalone repository must **not** vendor or copy the Apache Hop source tree.

`cxhttt233/hop` remains the engine fork and compatibility source.

## Why

The product goal is to replace SWT/RAP presentation with a React-native browser editor while preserving the Hop engine, plugins, metadata and `.hpl/.hwf` compatibility.

Keeping the complete Hop monorepo as the product workspace causes three avoidable problems:

1. product work is buried inside a very large Maven/source tree;
2. frontend work inherits unrelated Hop build/test/context cost;
3. agents naturally optimize backend internals instead of the visible browser product.

The standalone boundary makes the product dependency explicit:

`Modern Web product → Hop engine APIs/artifacts`

instead of:

`Modern Web product = modified copy of all Hop source`.

## What stays in the Hop fork

Keep changes in `cxhttt233/hop` only when they need to modify or expose engine semantics that belong upstream:

- Graph projection primitives/DTO support that must live next to `PipelineMeta` / `WorkflowMeta`;
- semantic document commands and undo/redo extraction;
- save/reload compatibility helpers;
- Config JSON serialization semantics that must understand Hop annotations;
- narrowly scoped metadata/config reflection helpers;
- other small SWT-free `core/engine` changes that are plausible upstream patches.

These changes should remain small, independently testable and avoid depending on the standalone web product.

## What moves to the standalone repository

Move or recreate as product code:

- React + TypeScript app;
- React Flow canvas;
- app shell, routing and browser state;
- native browser components:
  - VariableInput
  - EditableTable
  - MetadataPicker
  - MonacoEditor
  - VfsPicker
- Transform config panels;
- Metadata browser/editor;
- thin JAX-RS/Jakarta adapter resources;
- web session/document registries that are product-specific;
- API contracts and browser DTOs;
- E2E/Playwright tests;
- product GitHub Actions;
- product architecture/status docs.

## Existing asset mapping

| Existing asset | Treatment |
|---|---|
| T1 WebSessionScope / web carrier | migrate only product-specific pieces; do not keep the whole Hop web module structure by default |
| T2 commands / undo | keep engine semantics in Hop fork; expose through standalone adapter |
| T2 SVG renderer | keep in Hop fork as compatibility/reference/fallback |
| T3 Config JSON / Schema | keep engine-aware serialization helpers in Hop fork; move web schema/API/rendering to standalone |
| T4 ExecutionRegistry/EventBuffer/Log/Metrics | retain as validated deferred asset; migrate later when execution UI becomes current product work |
| RAP / SWT UI | do not migrate |
| Hop plugins source tree | do not migrate |
| Hop core/engine source tree | do not migrate |

## Recommended standalone layout

```
hop-modern-web/
├─ README.md
├─ docs/
│  ├─ architecture.md
│  ├─ implementation-status.md
│  └─ hop-compatibility.md
├─ app/                       # React + TypeScript + React Flow
│  ├─ src/
│  │  ├─ canvas/
│  │  ├─ config/
│  │  ├─ metadata/
│  │  ├─ vfs/
│  │  └─ api/
│  └─ package.json
├─ server/                    # thin Java/JAX-RS product adapter
│  ├─ src/main/java/
│  └─ pom.xml
├─ contracts/                 # API schemas/examples if useful
├─ e2e/                       # Playwright/browser vertical-slice tests
├─ samples/                   # small .hpl fixtures only
└─ .github/workflows/
```

Avoid creating copies of `core/`, `engine/`, `ui/`, `rap/` or `plugins/`.

## Hop dependency model

During CI:

1. checkout `cxhttt233/hop` at a pinned compatible commit;
2. build/install only the Maven artifacts required by the standalone server where feasible;
3. build/test the standalone Java adapter;
4. build the React app;
5. start the standalone server with test Hop plugins/config;
6. run Playwright against the React vertical slice.

When suitable Hop artifacts are available from Maven repositories, prefer normal Maven dependencies and remove unnecessary source checkout/build steps.

## Phase 0 vertical slice

The first product milestone is:

`real .hpl → Graph JSON → React Flow → semantic command → React config/metadata → save/reload`

Required visible capabilities:

- open a real Pipeline without entering RAP;
- render it in React Flow;
- select/pan/zoom/move/add/delete/connect/undo;
- edit Table Input using MetadataPicker + SQL Monaco;
- edit at least one Database Connection metadata object;
- save and reload;
- verify Apache Hop still opens/executes the saved file.

Execution/SSE/Metrics, deep multi-user infrastructure and plugin UI long-tail are deferred until this path exists.

## Migration rule

Do not move code merely because it already exists.

For every existing class, ask:

1. Does this class contain Hop engine semantics?
   - yes → keep in Hop fork.
2. Is it product/browser/API adapter code?
   - yes → migrate/recreate in standalone repo.
3. Is it only supporting the old RAP/SVG-first/Execution-first direction?
   - yes → retain as historical/deferred asset or remove from the product path.

