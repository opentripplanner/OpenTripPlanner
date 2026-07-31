# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Project Overview

OTP2 (OpenTripPlanner), active development branch `dev-2.x`.

**Tech Stack:** Java 25, Maven, GraphQL API, GTFS/NeTEx transit data, OpenStreetMap

## Essential Build Commands

```bash
# Build and run tests (also produces the shaded JAR: otp-shaded/target/otp-shaded-VERSION.jar)
mvn package

# Build without running tests
mvn package -DskipTests

# Run tests
mvn test

# Run tests with code coverage
mvn jacoco:prepare-agent test

# Skip prettier during local builds
mvn test -Dps          # equivalently: mvn test -PprettierSkip

# Regenerate API/itinerary snapshot tests
mvn clean -Pclean-test-snapshots
```

## Code Formatting & Quality

Quality gates run in the Maven `validate` phase and fail the build on violations:

- **Prettier** auto-formats Java (`.prettierrc.yml`: 100-char width, 2-space indent). CI enforces
  it; check with `mvn prettier:check`. Skip locally with `-Dps`.
- **Checkstyle** runs against `checkstyle.xml`. Skip with `-Dcs` (or `-PcheckstyleSkip`).

OpenRewrite (recipes in `openrewrite.yml`, e.g. removing unused imports) does **not** run
automatically — invoke it explicitly with `mvn validate -Prewrite` (or the `-Drw` shortcut).

## Running OTP

Main class: `org.opentripplanner.standalone.OTPMain`

```bash
# From the shaded JAR (use -Xmx2G when running from the IDE as well)
java -Xmx2G -jar otp-shaded/target/otp-shaded-VERSION.jar [args]
```

## Testing

- Unit tests: JUnit 5, organized by package structure
- Assertions: prefer the Google Truth library, especially for collections and optionals — it reads
  far better than the JUnit equivalents
- Snapshot tests: API and itinerary tests use `.snap` files stored in git (regenerate with
  `-Pclean-test-snapshots`)
- Speed tests: `test/performance/` (see Performance Testing)
- Test data: use the smallest possible OSM extracts (see `doc/user/Preparing-OSM.md`)

```bash
# Single test class / single method
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName
```

## Architecture Overview

### Module Structure

Multi-module Maven project (root `pom.xml` `<modules>`):

- **utils**: low-level utilities shared by all modules
- **domain-core**: core domain primitives (`org.opentripplanner.core`) — `FeedScopedId`, `Cost`,
  `Distance`, `I18NString`, `Accessibility`, plus framework (deduplicator, DI, resources)
- **raptor**: transit routing engine — `raptor/src/main/java/org/opentripplanner/raptor/`. Isolated:
  **zero dependencies on OTP code** (only utilities). Performance-critical; data is supplied via the
  SPI in `raptor/spi`.
- **astar**: generic A\* shortest-path engine — `astar/src/main/java/org/opentripplanner/astar/`
  (`AStar`, `AStarBuilder`, `spi/`, `strategy/`, `model/`)
- **street**: street graph model, linking and search —
  `street/src/main/java/org/opentripplanner/street/` (`model/edge/StreetEdge`,
  `model/vertex/Vertex`, `linking/VertexLinker`, `search/`, `geometry/`) plus the
  `service/vehicleparking` and `service/vehiclerental` domain services
- **gtfs-realtime-protobuf**: GTFS-RT protocol buffer definitions
- **application**: main OTP application — `application/src/main/java/org/opentripplanner/`. Key
  packages: `routing`, `transit`, `gtfs`, `netex`, `osm`, `graph_builder`, `apis`, `updater`,
  `service`, `standalone`, `streetadapter`
- **otp-shaded**: produces the unified shaded JAR with all dependencies
- **test/integration**: integration tests

### Key Components

**Transit Routing (Raptor)** — `raptor/src/main/java/org/opentripplanner/raptor/`

- Range Raptor with multi-criteria pareto-optimal search; isolated from OTP (data supplied via SPI
  in `raptor/spi`). Test all changes with SpeedTest (see Performance Testing).
- `RoutingService` (`application/.../routing/api/RoutingService.java`) is the top-level routing
  entry point. The OTP↔Raptor mapping (`RaptorRequest`/`RaptorPath`) lives in
  `application/.../routing/algorithm/raptoradapter/` (e.g. `RaptorRequestMapper`,
  `RaptorRoutingRequestTransitData`, `TransitRouter`).

**Graph Building**

- GTFS import: `application/src/main/java/org/opentripplanner/gtfs/`
- NeTEx import: `application/src/main/java/org/opentripplanner/netex/`
- OSM processing: `application/src/main/java/org/opentripplanner/osm/`
- Graph builder: `application/src/main/java/org/opentripplanner/graph_builder/`

**Routing Components**

- Routing algorithm: `application/src/main/java/org/opentripplanner/routing/algorithm/`
- Transfer optimization: `.../routing/algorithm/transferoptimization/`
- Itinerary filter chain: `.../routing/algorithm/filterchain/`
- Street routing (A\*): `astar/src/main/java/org/opentripplanner/astar/` (generic engine) +
  `street/src/main/java/org/opentripplanner/street/search/` (street-search layer:
  `StreetSearchBuilder`, heuristics, state)

**APIs**

- GraphQL APIs: `application/src/main/java/org/opentripplanner/apis/` (GraphiQL at
  `http://localhost:8080/graphiql` when running)
- Real-time updaters: `application/src/main/java/org/opentripplanner/updater/`

**Configuration**

- `application/src/main/java/org/opentripplanner/standalone/config/` parses JSON config into POJOs
- Config files: `build-config.json`, `router-config.json`, `otp-config.json`

### Design Model

OTP follows a layered model (documented in `ARCHITECTURE.md`) that determines where new code
belongs:

- **Use Case Service**: stateless service that orchestrates several domain services; usually has no
  model of its own.
- **Domain Model**: encapsulates a business area (e.g. transit, vehicle position). A complex domain
  splits into a **Service** (read access, backs the HTTP/GraphQL endpoints) and a **Repository**
  (holds the model; written to by updaters). The split is applied consistently across the codebase
  (~25 `*Service` / ~18 `*Repository` interfaces).

Each documented component has a `package.md` file in its source directory.

## Development Guidelines

Highlights below; full guidelines in `DEVELOPMENT_DECISION_RECORDS.md`.

**Code Quality**

- Scout Rule: leave code better than you found it
- Follow naming conventions from GTFS, NeTEx, or existing OTP code
- Write JavaDoc for public types, methods and fields; document business intention, not just logic
- Use comments sparingly — only for complex code

**Architecture**

- Dependency injection via Dagger (with manual DI where simpler)
- Module wiring goes in `<module-name>/configure/<Module>Module.java`
- Keep modules isolated with `api`, `spi`, and mapping layers; avoid circular dependencies

**Types**

- Prefer immutable types and builders; use records only where proper encapsulation holds

**Testing**

- Unit-test business logic at the lowest practical level; keep integration/system tests to a
  minimum; full branch coverage preferred for non-trivial code

## Git Workflow

**Protected branches — NEVER push directly:** `dev-2.x`, `main`, `master`, `main_config`. Always
work on a feature branch, and **ask which remote to use before pushing**.

- Main development branch: `dev-2.x`; release branch: `master` (fast-forward merges from `dev-2.x`).
- Reference the issue in the PR with a GitHub keyword, e.g. `related to #123` or `closes #123`.

**Creating a cross-fork PR:**

```bash
git push -u entur my-feature
gh pr create --repo opentripplanner/OpenTripPlanner --head entur:my-feature --base dev-2.x
```

## Documentation

- User docs: `doc/user/` (Markdown, MkDocs); developer guide: `doc/user/Developers-Guide.md`
- Architecture: `ARCHITECTURE.md` and per-component `package.md` files
- Decision records: `DEVELOPMENT_DECISION_RECORDS.md`
- Generated docs: https://docs.opentripplanner.org/

```bash
# Build docs locally
pip install -r doc/user/requirements.txt
mkdocs serve
```

## Performance Testing

Performance is critical. **All changes to Raptor must be tested with SpeedTest** (requires a
pre-built graph in `test/performance/<location>` — see `test/performance/README.md` for setup):

```bash
mvn --projects application exec:java -Dexec.mainClass="org.opentripplanner.transit.speed_test.SpeedTest" \
  -Dexec.classpathScope=test -Dexec.args="--dir=test/performance/norway -p md -n 4 -i 3 -0"
```

SpeedTest also runs automatically after each merged PR; results are tracked at
https://otp-performance.leonard.io/.

## Project Structure Notes

- Serialization version ID is the `pom.xml` property `otp.serialization.version.id` — **bump it when
  changing the graph format**, or previously-built graphs fail to load.
- Client code: `client/src/` (MapLibre-based JS test client)

## Key Dependencies

- **DI:** Google Dagger
- **GraphQL:** GraphQL-Java (engine for the TransModel and GTFS APIs)
- **REST:** Jersey, served via an embedded **Grizzly2** HTTP server (not Tomcat/Jetty)
- **JSON:** Jackson
- **Geometry:** JTS Topology Suite, GeoTools
- **Search:** Lucene · **Metrics:** Micrometer · **GTFS-RT:** Protocol Buffers
- **Testing:** JUnit 5, Mockito, Google Truth

## Sandbox Features

Optional features not yet in core OTP are Sandbox extensions:

- Live in their own Maven source root: `application/src/ext/java/org/opentripplanner/ext/<feature>/`
  (tests in `application/src/ext-test/java/`, resources in `application/src/ext/resources/`),
  registered as extra source roots in `application/pom.xml`.
- Gated by a feature flag in `org.opentripplanner.framework.application.OTPFeature`
  (`sandbox = true`, `enabledByDefault = false`), toggled via the `otpFeatures` section of
  `otp-config.json`.
- Have conditional code blocks in core OTP.
- See: http://docs.opentripplanner.org/en/latest/SandboxExtension/
