# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OpenTripPlanner (OTP) is an open source multi-modal trip planner focusing on travel by scheduled public transportation combined with bicycling, walking, and mobility services. This is OTP2 (dev-2.x branch), the second major version under active development.

**Tech Stack:** Java 21, Maven, GraphQL API, GTFS/NeTEx transit data, OpenStreetMap

## Essential Build Commands

```bash
# Build project and run tests
mvn package

# Build without running tests
mvn package -DskipTests

# Run tests
mvn test

# Run tests with code coverage
mvn jacoco:prepare-agent test

# Run integration tests (tagged with @Tag("integration"))
mvn test -Pit

# Build the shaded JAR (unified JAR with all dependencies)
# Output: otp-shaded/target/otp-shaded-VERSION.jar
mvn package

# Skip prettier formatting during build (useful for faster builds)
mvn test -Dps

# Clean test snapshots (for regenerating API/itinerary snapshot tests)
mvn clean -Pclean-test-snapshots
```

## Code Formatting & Quality

```bash
# Prettier formats Java code automatically during build (validate phase)
# Configuration: .prettierrc.yml (100 char width, 2 space indent)

# To check formatting (used in CI)
mvn prettier:check

# Spotless removes unused imports
# Runs automatically during build (validate phase)

# OpenRewrite for code refactoring and modernization
# Run with the -Prewrite (or -Drw shortcut):
mvn validate -Prewrite
```

## Running OTP

Main class: `org.opentripplanner.standalone.OTPMain`

```bash
# Run from IDE: Set main class and JVM args -Xmx2G
# Run from JAR: java -Xmx2G -jar otp-shaded/target/otp-shaded-VERSION.jar [args]
```

## Testing

- Unit tests: JUnit 5, organized by package structure
- Integration tests: Use `-Pit` profile, tagged with `@Tag("integration")`
- Snapshot tests: API and itinerary tests use `.snap` files stored in git
- Speed tests: Located in `test/performance/`, run after each PR merge
- Test data: Use smallest possible OSM extracts (see `doc/user/Preparing-OSM.md`)

```bash
# Run a single test class
mvn test -Dtest=ClassName

# Run specific test method
mvn test -Dtest=ClassName#methodName

# Speed test (requires pre-built graph)
mvn exec:java -Dexec.mainClass="org.opentripplanner.transit.speed_test.SpeedTest" \
  -Dexec.classpathScope=test -Dexec.args="--dir=test/performance/norway -p md -n 4 -i 3 -0"
```

## Architecture Overview

### Module Structure

OTP is a multi-module Maven project:

- **application**: Main OTP application code
  - `src/main/java/org/opentripplanner/` - Core source
  - Key packages: `routing`, `transit`, `street`, `gtfs`, `netex`, `apis`, `updater`
- **raptor**: Transit routing engine (isolated, zero dependencies on OTP)
- **utils**: Utility classes
- **gtfs-realtime-protobuf**: GTFS-RT protocol buffer definitions
- **otp-shaded**: Produces unified JAR with all dependencies
- **test/integration**: Integration tests

### Key Components

**Transit Routing (Raptor)**
- Location: `raptor/src/main/java/org/opentripplanner/raptor/`
- The core transit routing algorithm, based on the Raptor paper (Microsoft 2012)
- Implements Range Raptor with multi-criteria pareto-optimal search
- Completely isolated from OTP - no dependencies on OTP code, only utility classes
- Performance-critical: test all changes with SpeedTest
- OTP provides data via SPI in `raptor/spi`
- `RoutingService` maps between OTP domain and Raptor's `RaptorRequest`/`RaptorPath`

**Graph Building**
- GTFS import: `application/src/main/java/org/opentripplanner/gtfs/`
- NeTEx import: `application/src/main/java/org/opentripplanner/netex/`
- OSM processing: `application/src/main/java/org/opentripplanner/osm/`
- Graph builder: `application/src/main/java/org/opentripplanner/graph_builder/`

**Routing Components**
- Routing algorithm: `application/src/main/java/org/opentripplanner/routing/algorithm/`
- Transfer optimization: `routing/algorithm/transferoptimization/`
- Itinerary filter chain: `routing/algorithm/filterchain/`
- Street routing (A*): `application/src/main/java/org/opentripplanner/astar/`

**APIs**
- GraphQL API: `application/src/main/java/org/opentripplanner/apis/`
- GTFS GraphQL schema: Documented at http://localhost:8080/graphiql
- Real-time updaters: `application/src/main/java/org/opentripplanner/updater/`

**Configuration**
- Location: `application/src/main/java/org/opentripplanner/standalone/config/`
- Loads and parses JSON config files into POJOs
- Config types: build-config.json, router-config.json, otp-config.json

### Design Model

OTP follows a layered architecture:

- **Use Case Services**: Stateless services combining multiple domain services
- **Domain Services**: Business logic for specific domains (transit, vehicle position, etc.)
- **Domain Models**: Encapsulate business areas with separate Service and Repository
- **Repositories**: Maintain domain models, used by updaters

Each documented component has a `package.md` file in its source directory.

## Development Guidelines (from DEVELOPMENT_DECISION_RECORDS.md)

**Code Quality**
- Scout Rule: Leave code better than you found it
- Follow naming conventions from GTFS, NeTEx, or existing OTP code
- Write JavaDoc for all public types, methods, and fields
- Document business intention and decisions, not just logic
- Respect OOP principles: encapsulation, single responsibility, abstraction
- Avoid feature envy and code duplication (DRY)

**Architecture**
- Use dependency injection (manual DI or Dagger)
- Module wiring goes in `<module-name>/configure/<Module>Module.java`
- Keep modules isolated with `api`, `spi`, and mapping layers
- Avoid circular dependencies

**Types**
- Prefer immutable types over mutable
- Use builders where appropriate
- Be careful with records - only if proper encapsulation is possible

**Testing**
- Full branch test coverage preferred for non-trivial code
- Unit tests for all business logic
- Keep integration/system tests to a minimum
- Test at the lowest practical level

**Code Style**
- Prettier formats code automatically (enforced in CI)
- Spotless removes unused imports
- Line width: 100 characters, 2-space indent

## Contribution Process

1. Discuss changes in Gitter chat or developer meetings (twice weekly)
2. Create GitHub issue for non-trivial changes
3. Reference issue in PR with "addresses #123"
4. PR requires 2 approvals from different organizations
5. All tests must pass, code must compile
6. Participate in developer meetings for faster PR progress

**Important:** This is an established project with high code quality expectations. Significant changes require discussion, documentation, and organizational commitment to maintenance.

## Git Workflow

**IMPORTANT: Protected Branches**
- **NEVER push directly to these branches:** `dev-2.x`, `main`, `master`, `main_config`
- Always use feature branches for development work
- When asked to push changes, always ask which remote to use before pushing
- Ask which remote a feature branches should be pushed to for creating PRs to upstream

**Branch Structure:**
- Main development branch: `dev-2.x`
- Release branch: `master` (releases only, fast-forward merges from dev-2.x)
- All changes via pull requests with code review
- Use Gitflow-derived branching model
- Break large changes into smaller PRs tied together with an "epic issue"

**Creating Pull Requests:**
1. Create a feature branch (e.g., `my-feature`)
2. Make commits on the feature branch
3. Ask which remote to push the feature branch to: `git push -u entur my-feature`
4. Create PR in upstream repository (`opentripplanner/OpenTripPlanner`) using `gh pr create --repo opentripplanner/OpenTripPlanner --head entur:my-feature --base dev-2.x`

## Documentation

- User docs: `doc/user/` (Markdown, built with MkDocs)
- Architecture docs: `ARCHITECTURE.md`, various `package.md` files
- Developer guide: `doc/user/Developers-Guide.md`
- Decision records: `DEVELOPMENT_DECISION_RECORDS.md`
- Generated docs: https://docs.opentripplanner.org/

```bash
# Build docs locally
pip install -r doc/user/requirements.txt
mkdocs serve
```

## Performance Testing

Performance is critical. The SpeedTest runs automatically after each merged PR.

```bash
# Run speed test locally (requires pre-built graph in test/performance/<location>)
mvn exec:java -Dexec.mainClass="org.opentripplanner.transit.speed_test.SpeedTest" \
  -Dexec.classpathScope=test -Dexec.args="--dir=test/performance/norway -p md -n 4 -i 3 -0"
```

Dashboard: https://otp-performance.leonard.io/

**Critical:** All changes to Raptor must be tested with SpeedTest to ensure no performance regression.

## Project Structure Notes

- Maven multi-module project
- Java 21 target
- Serialization version ID tracked in `pom.xml` property `otp.serialization.version.id`
- Shaded JAR output: `otp-shaded/target/otp-shaded-VERSION.jar`
- Client code: `client/src/` (MapLibre-based JavaScript client for testing)

## Key Dependencies

- Jersey (REST framework)
- Jackson (JSON)
- GeoTools (geospatial operations)
- JTS Topology Suite (geometry)
- Google Dagger (dependency injection)
- JUnit 5, Mockito, Google Truth (testing)
- Lucene (search indexing)
- Micrometer (metrics)
- Protocol Buffers (GTFS-RT)

## Sandbox Features

Features not yet part of core OTP can be developed as Sandbox extensions. These must:
- Be in their own package
- Use feature flags (disabled by default)
- Have conditional code blocks in core OTP
- See: http://docs.opentripplanner.org/en/latest/SandboxExtension/
- Use comments sparingly. Only comment complex code.
