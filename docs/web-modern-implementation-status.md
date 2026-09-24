# Apache Hop Modern Web — Implementation Status

> Stable implementation facts only. The sole architecture authority is
> `design/web-modern-architecture:docs/web-modern-architecture-plan.md`, especially section **0A (2026-09-24 product refocus)**.

## Current Product Goal

`real .hpl → Graph JSON → React Flow → semantic edit → React config/metadata → save/reload`

The Phase 0 core path must not enter RAP `/ui`.

## Repository Strategy

- Target standalone product repository: `cxhttt233/hop-modern-web` — **not created yet**.
- `cxhttt233/hop` remains the Apache Hop engine fork / compatibility source.
- Do not copy the Hop monorepo into the standalone product repository.
- Existing `experiment/web-modern-poc` is now a **legacy migration source**, not the future product mainline.

## Stable Assets

### Task 1 — Session/Foundation

Evidence:
- `ceefd235db`
- `f02be05fc945f2d5be53f9805bfea701cc4e896a`

Keep:
- SWT/RWT-free `WebSessionScope<T>` ideas and tests.
- Minimal session registry/lifecycle logic where needed.

Treatment:
- Product-specific session/backend code should be recreated/migrated into standalone `server/`.
- Do not keep expanding Hop-root `web/` modules simply for continuity.

### Task 2 — Commands / Undo

Evidence:
- `e61d515bca`
- `f99620e679`
- `6ccae95afa5e0dbd3985563ad36b6e386f3916a9`

Keep in Hop fork:
- semantic Pipeline editing commands;
- undo/redo extraction;
- future Graph Projection helpers that genuinely belong in `engine`;
- save/reload compatibility tests.

Next product-aligned work:
- Graph Projection for React Flow;
- command → save → reload semantic round-trip.

### Task 3 — Config JSON

Evidence:
- `55497fc98a8cdc6ff655a859ed85d1f06fef899d`

Keep in Hop fork:
- Hop-annotation-aware Config JSON serializer semantics.

Next product-aligned work:
- provider-aware metadata references;
- sensitive/password semantics;
- React-consumable schema contract;
- Table Input / Select Values / Sort Rows;
- Database Connection metadata round-trip.

### Task 4 — Execution Domain

Evidence:
- `3bf1d280cfce65a2fb4b6992f30275fa8702c634`
- `525c8781bd08f962990cec0676141d3a7169061f`

Status:
**VALIDATED BUT DEFERRED**

ExecutionRegistry / EventBuffer / Log / Metrics / lifecycle are useful assets, but further SSE/reconnect/metrics work is paused until the React editor vertical slice exists.

These classes belong in the standalone product server later, not as a reason to keep growing experimental Hop-root Web modules.

## Source Migration Inventory

Keep in Hop fork:
- `engine/.../PipelineEditor.java`
- `engine/.../PipelineUndoApplier.java`
- required small `AbstractMeta` changes
- `core/.../ConfigJsonSerializer.java`
- matching tests
- future narrowly scoped Graph Projection engine helpers

Move/recreate in standalone:
- React app / React Flow
- product-specific session/document registries
- thin JAX-RS adapter
- Config/Metadata HTTP contracts
- MetadataPicker / VariableInput / EditableTable / Monaco / VfsPicker
- E2E tests
- deferred execution-domain server code when needed

Do not migrate:
- full `core/`, `engine/`, `ui/`, `rap/`, `plugins/`
- temporary root `hop-web-api` module layout
- old experimental Maven Web layout merely for continuity

## Current Gate

**REFOCUSING / STANDALONE REPOSITORY CREATION REQUIRED**

No new product implementation should be admitted into the old PoC simply because it is technically green.

The next product admission must shorten the path to the React vertical slice.

## Current Critical Path

`create standalone repo → initialize React/server/CI → Graph JSON → React Flow → config/metadata UI → save/reload compatibility`

Task 2 and Task 3 engine/core work may proceed in the Hop fork only when it is directly required by this path.

## Automation State

Task 1–5 automations are paused during the repository-boundary migration.

## References

- `docs/web-modern-architecture-plan.md` section 0A
- `docs/web-modern-repository-migration.md`
- `docs/web-modern-migration-inventory.md`
- Issue #8 — standalone repository migration

## Last Updated

2026-09-24
