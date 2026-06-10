# Application Warmup

Runs GraphQL trip queries in a background thread during OTP startup to warm up the application
(JIT compilation, GraphQL schema caches, routing data structures, etc.) before production traffic
arrives.

## Lifecycle

1. `ConstructApplication` obtains a `WarmupLauncher` from the Dagger factory
   (`configure.WarmupModule` wires it) and calls `start()` after Raptor transit data is created
   and updaters are configured.
2. A daemon thread sends sequential queries through the configured GraphQL API (TransModel or GTFS),
   exercising the full stack: GraphQL parsing, data fetchers, routing (Raptor + A*), itinerary
   filtering, and response serialization.
3. It alternates between depart-at / arrive-by and cycles through access/egress modes.
4. The thread stops when the health probe reports "UP" (all updaters primed).

## Design

The public API of the module lives in the `api` subpackage and exposes only value objects:
`WarmupParameters` (the configured parameter values) and `WarmupApi` (which GraphQL API to exercise).
`WarmupConfig` (in `standalone.config.routerconfig`) reads the JSON config section and produces a
`WarmupParameters` instance.

`WarmupQueryStrategy` is the strategy interface with two implementations:
- `TransmodelWarmupQueryExecutor` -- builds and executes TransModel `trip` queries.
- `GtfsWarmupQueryExecutor` -- builds and executes GTFS `planConnection` queries.

Each executor owns a `ModeCombinations` helper that holds the configured access/egress mode lists
and maps a running query counter to the next access/egress pair via modulo arithmetic.

## Configuration

Configured via the `warmup` section in `router-config.json`. See `WarmupConfig` for details.
