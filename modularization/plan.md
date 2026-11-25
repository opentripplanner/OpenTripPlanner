# OTP modularization plan

Status: draft. This branch holds the plan, tooling and test harness. It contains no changes to
`src`.

## Goal

Split OTP into functional domain modules with clear APIs and single responsibilities.

- The `application` module is small and only wires the application together.
- Coupling is reduced. Each module has module tests that run without the full graph.
- Transit entities (`Route`, `Trip`, `StopLocation`, ...) belong to the transit model. Other
  modules keep a `FeedScopedId`, and a small local projection of the fields their logic needs.
- Configuration is an adapter. Modules own parameter types and never depend on configuration.
  The config layer in `application` maps JSON fields to parameter types (one field may feed
  several parameter classes) and documents the parameters.
- Each sandbox extension becomes its own Maven module under a `sandbox` parent. Core code has no
  `OTPFeature` conditionals; core exposes extension points and `application` wires the sandboxes.
- Fares are a sandbox too. The core model contains fares today, so fares are extracted from the
  core into a sandbox module, and the core gets extension points for it (itinerary
  post-processing, result attachment, API contribution, import hooks, request parameters,
  configuration, graph storage). See the sandbox section of `vocabulary.md`.

## Problem

Classes were added to packages and modules without a clear domain. This created cycles, and
unrelated features share packages and classes. Package-level dependency analysis only reflects
this accidental structure, so the target design is derived from what each class does.

## Strategy

### 1. Classify by feature, then derive modules

- Draft a fixed domain vocabulary (`vocabulary.md`), reviewed by maintainers.
- Label every class with a domain, based on what it models or decides. Dependencies and package
  names are supporting evidence only. Result: `classification.csv` with columns class, current
  package, domain, confidence, config-dependent flag, sandbox flag, notes.
- Rank current packages by the number of domains they contain. This is the worklist.
- Identify hub classes (`Graph`, `RouteRequest`, `Itinerary`, `OtpServerRequestContext`, ...) that
  serve several domains. They are split by feature.

### 2. Resolve cross-domain edges after classification

For each remaining edge, in this order:

1. Wrong label, or a class doing two jobs: reassign or split it.
2. Reference to a transit entity: use a `FeedScopedId`, and a local projection if fields are
   needed.
3. Real behavioral dependency: an SPI owned by the consumer. This is the last resort.

### 3. Two kinds of steps

- **Semantic steps**: splitting classes, introducing parameter types, replacing entity references
  with ids, creating extension points. Done inside `application` on dev-2.x, no behavior change,
  small PRs. Each is independently mergeable upstream.
- **Mechanical steps**: moving classes into modules, repackaging, imports, `module-info`, POMs,
  test relocation. Fully scripted from `classification.csv`.

### 4. Extraction order

Bottom-up: leaf domains first, then middle layers, then APIs and updaters. Sandboxes come after
the core domains, because extension points need stable core APIs. The inventory of sandbox hooks
is built early and informs the core API design.

### 5. Boundary enforcement

`module-info.java` exports only API packages. Architecture rules with a ratchet file of allowed
violations that can only shrink. No module outside `application` may import config classes.

### 6. Tests

- Before moving code, add characterization tests at the current seam where tests are missing.
- A shared test-fixtures artifact with builders and in-memory fakes for each SPI.
- Contract tests per SPI that real implementations and fakes both pass.

## Branches

All branches are based on `dev-2.x` of `opentripplanner/OpenTripPlanner`.

| Branch | Content | Edited by |
|---|---|---|
| `modularization/tooling` | Everything under `modularization/` plus CI workflows. No `src` changes. | Everyone, via PR |
| `modularization/step-*` | One semantic step each, branched from dev-2.x, also upstream PR candidates. | The step owner |
| `modularization/migrated` | Generated: dev-2.x, plus the listed steps, plus the scripted moves. Rebuilt and force-pushed by CI, never hand-edited. Contains an info file with the source SHAs. | CI only |

The pipeline config lists the steps to include and their order. A step is removed from the list
when it has been merged into dev-2.x.

## Staying up to date with dev-2.x

- `migrated` is rebuilt from the current dev-2.x. It is not rebased or merged.
- Reproducibility check: replaying the pipeline on dev-2.x commit X must give the same tree as the
  stored result. Test it on two or three different dev-2.x commits.
- Unclassified-class gate: classes new in dev-2.x that are missing from `classification.csv` are
  listed and block the build until assigned. New tests move with the same scripts.
- New cross-module violations are reported against the ratchet file.
- Bug fixes in code that has been split need a human to port them. Keep a list of these
  conflicts; it shows which split decisions are expensive.
- Upstream semantic steps early. The longer they stay unmerged, the more rebasing they cost.

## Compliance harness

Location: `modularization/harness/`, a standalone Maven project outside the OTP reactor.

It is black-box. It runs the shaded jar built from either tree, so it does not depend on OTP
packages. It compares:

- graph build output on fixed inputs, then routing results for fixed requests. Inputs: GTFS, NeTEx,
  OSM and elevation, in several datasets
- the GraphQL schemas
- generated configuration documentation and JSON schema (not built yet)
- real-time updater results on generated input: SIRI-ET from files, and vehicle rental from GBFS
  feeds (v2.3 and v3.0, stations and free-floating, with geofencing)

Fares are not covered, by decision. They become a sandbox. When the extraction starts, a fare
dataset is added so the extraction can be checked.

`approved-discrepancies` lists each accepted difference with a reason and an owner. The harness
fails on any difference not listed, and on any listed entry that no longer occurs.

The harness must report zero differences when run on plain dev-2.x against itself.

## Deliverables, in order

1. `vocabulary.md`, reviewed.
2. The compliance harness and the approved-discrepancies file, green on plain dev-2.x.
3. `classification.csv`, the config-to-parameter inventory, the sandbox hook inventory, and the
   unclassified-class gate.
4. The semantic steps in `application`, then the scripted move pipeline.
5. CI: PR checks on the tooling branch (script tests, harness tests, gate) and a nightly rebuild
   of `migrated`. GitHub scheduled workflows run only from the default branch, so on this
   repository the rebuild is started manually or by pushes to the tooling branch.

## Definition of done for a module

- A documented public API, and no dependency on `application`.
- Module tests that run without the full graph.
- No config dependency. No direct reference to transit entities owned by another module.

Metrics: the number of boundary violations, the size of `application`, the share of tests that
need the full graph.

## Open questions

- Does the target stay on Maven modules with `module-info` enforcement, or does it move to
  Jenesis? Decide when the first scripted move is ready.
- Is the result a reference for upstream PRs, or the base of an OTP v3? The structure above
  supports both.
