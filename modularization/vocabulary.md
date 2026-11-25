# Domain vocabulary

Status: DRAFT for review. Nothing is classified until this list is agreed.

Every class gets exactly one domain from this list (see `plan.md`). The label says what the class
models or decides for the business. The current package is not evidence of the domain. Many
packages mix several domains.

Sandbox extensions are labeled by domain too. A separate flag marks them as sandbox, because a
sandbox is a deployment unit and not a domain. For example, `ext/fares` is domain `fares`.

## How to label a class

1. What business concept does the class model, or what decision does it make? That is the domain.
2. If the class does two jobs, label it with the domain of the main job and mark it `split`. It
   will be split later. Do not invent a mixed domain.
3. Mapping classes (A to B) belong to the domain that owns the use of the result, not the source.
   Example: a mapper from GTFS routes to OTP routes belongs to `gtfs-import`. A mapper from an
   `Itinerary` to the GraphQL response belongs to the API domain.
4. Parameter classes belong to the domain that consumes them. The config classes that fill them
   belong to `config`.
5. A class used by almost everything and with no business meaning is `foundation`. A class used by
   almost everything but with business meaning is a hub. Label it with the best domain, mark it
   `hub`, and list it in the hub report.
6. When two domains fit equally well, pick the more specific one and write the other in the notes
   column.

## Layers

Dependencies point downwards only. A domain may use domains in lower layers, never in the same or
a higher layer, unless the vocabulary says otherwise. Same-layer links go through ids or an SPI
owned by the consumer.

| Layer | Meaning | Domains |
|---|---|---|
| L0 | Foundation | `foundation` |
| L1 | Data domains: own entities and the data they hold | `transit-model`, `street-network`, `vehicle-rental`, `vehicle-parking`, `transfers` |
| L2 | Domain services and engines | `raptor`, `street-routing`, `flex`, `realtime-trip`, `realtime-alerts`, `realtime-vehicle-position`, `emissions`, `place-search` |
| L3 | Use cases | `trip-planning`, `itinerary-filtering`, `transfer-optimization`, `trip-plan-model`, `routing-request`, `refetch` |
| L4 | Data import and graph build | `osm-import`, `gtfs-import`, `netex-import`, `graph-build`, `graph-storage`, `datastore` |
| L5 | APIs and updaters (adapters) | `api-gtfs`, `api-transmodel`, `api-other`, `updater-framework` and the updater adapters |
| L6 | Application | `config`, `runtime`, `application-wiring` |

The layers are a starting hypothesis. The classification will show which edges do not fit, and
those will be decided case by case.

## Domains

### L0

| Domain | Responsibility | Typical content | Currently in |
|---|---|---|---|
| `foundation` | Business-neutral building blocks | ids, time, cost, distance, `I18NString`, accessibility, deduplicator, DI helpers, CSV/JSON/HTTP/retry/token helpers, collections, test fixtures | `utils`, `core/*`, `framework/*` |

### L1 Data domains

| Domain | Responsibility | Typical content | Currently in |
|---|---|---|---|
| `transit-model` | The scheduled transit network: entities, timetables, patterns, calendars, and read access to them | `Route`, `Trip`, `TripPattern`, `StopLocation`, `Station`, `Operator`, `Agency`, `Timetable`, `StopTime`, `Frequency`, `PickDrop`, `TripTimeOnDate`, calendars, `TimetableRepository`, `TransitService`, stop consolidation | `transit/model`, `transit/service`, `transit/repository`, `model/calendar`, `model/*` (stop times), `ext/stopconsolidation` |
| `street-network` | The street and path graph: vertices, edges, street search, linking, street cost | `StreetEdge`, `Vertex`, `VertexLinker`, street search state, turn restrictions, street details, elevation, edge naming, data overlay, island pruning | `street`, `astar`, `routing/linking`, `ext/dataoverlay`, `ext/edgenaming`, `service/streetdetails` |
| `vehicle-rental` | Rental vehicles and stations, networks, geofencing | rental places, networks, geofencing zones, GBFS feed model | `street/.../vehiclerental`, `gbfs`, `ext/vehiclerentalgeofencing`, `ext/vehiclerentalservicedirectory` |
| `vehicle-parking` | Parking facilities and their availability | parking lots, entrances, tags, state | `street/.../vehicleparking`, `ext/vehicleparking`, `ext/parkAndRideApi` |
| `transfers` | Where and how a passenger changes between trips and stops | regular transfers, constrained transfers (GTFS/NeTEx), transfer parameters and generation, nearby stop finding | `transit/transfer`, `transfer/*`, `place/nearbystopfinder`, `ext/transferanalyzer` |

### L2 Domain services and engines

| Domain | Responsibility | Typical content | Currently in |
|---|---|---|---|
| `raptor` | The generic transit search engine | everything in the `raptor` module | `raptor/*` |
| `raptor-adapter` | Maps OTP data and requests to the Raptor SPI and back | `RaptorRequestMapper`, `RaptorRoutingRequestTransitData`, transit router, cost calculators, access/egress for transit | `routing/algorithm/raptoradapter` |
| `street-routing` | Street legs for itineraries: access, egress, direct, transfers on street | street search requests, state-to-leg mapping | `routing/algorithm/mapping`, `routing/api` (street parts), `street/.../search` (use side) |
| `flex` | Flexible and demand-responsive transit | flex trips, areas, access/egress, direct flex routing | `ext/flex` |
| `realtime-trip` | Applying real-time changes to trips and timetables | trip updates (GTFS-RT, SIRI), timetable snapshot, trip adders and cancellations, delay prediction | `updater/trip`, `ext/siri`, `ext/empiricaldelay`, `ext/realtimeresolver` |
| `realtime-alerts` | Service alerts and their match to entities | alert model, alert patches, alert service | `updater/alert`, `routing/alertpatch`, `routing/services`, `routing/impl` |
| `realtime-vehicle-position` | Positions and occupancy of vehicles | vehicle positions repository, service | `updater/vehicle_position`, `service/realtimevehicles` |
| `emissions` | Emission estimates for itineraries | emission service, parameters | `ext/emission` |
| `place-search` | Finding places: nearby places, text search | stop and place finder, geocoder, suggestion index | `place/*`, `ext/geocoder` |
| `ride-hailing` | Ride-hailing offers | providers, estimates | `ext/ridehailing`, `ext/taxi` |
| `carpooling` | Carpooling offers | carpooling model and routing | `ext/carpooling` |

### Sandbox domains

Decided: **fares is a sandbox module**, not a core domain. This domain is not in a layer, because
it plugs into the core through extension points.

| Domain | Responsibility | Typical content | Currently in |
|---|---|---|---|
| `fares` | Fare products, fare rules, fare calculation and the fare data of an itinerary | fare products and media, leg and transfer rules, fare services and calculators (Nordic, German, ...), `FareType`, fare import from GTFS and NeTEx | `model/fare`, `routing/fares`, `routing/core`, `ext/fares`, and the fare parts of the importers, the plan model and the APIs |

The core model contains fares today, so the work is an extraction, and the core needs extension
points for it. The list below is the starting inventory. It will be completed by the
classification, which labels every class that mentions fares with `fares` and marks the
core classes that carry fare data as `split`.

1. **Itinerary post-processing:** a hook in `trip-planning` that lets a sandbox decorate a found
   itinerary (today: the fare service called after the filter chain).
2. **Result attachment:** a way to attach typed, sandbox-owned data to an `Itinerary` or `Leg`
   without the plan model knowing the type (today: fare fields on `Itinerary`).
3. **API contribution:** a way for a sandbox to add fields and data fetchers to the GTFS and
   Transmodel GraphQL schemas.
4. **Import hooks:** `gtfs-import` and `netex-import` pass fare data to the sandbox (today: fare
   mappers inside the importers and fare data stored in `TimetableRepository`).
5. **Request parameters:** a way to carry sandbox request parameters, e.g. the fare type, without
   `routing-request` knowing them.
6. **Configuration:** the sandbox owns its parameter types. The config adapter maps JSON to them,
   as for all modules.
7. **Graph storage:** the sandbox registers its data for serialization.

The same list applies, in general form, to every sandbox. Fares is the first one to be worked
out, because it is the largest piece of sandbox data in the core.

### L3 Use cases

| Domain | Responsibility | Typical content | Currently in |
|---|---|---|---|
| `routing-request` | The route request and preferences | `RouteRequest`, preferences, request modes, via requests. Hub: split into per-domain parameter types | `routing/api/request`, `api/parameter` |
| `trip-plan-model` | The result of a trip search | `Itinerary`, `Leg`, `Place`, `TripPlan`, walk steps, paging model, leg and itinerary references. Hub | `model/plan`, `routing/api/response` |
| `trip-planning` | Orchestrates one trip search from request to response | `RoutingWorker`, `RoutingService`, request pre-processing, via search, alternative legs, error handling | `routing/algorithm` (top level), `routing/api` (service), `routing/algorithm/via`, `routing/alternativelegs`, `service/paging` |
| `itinerary-filtering` | Removing and ranking itineraries | the itinerary filter chain, group priority, sort order | `routing/algorithm/filterchain`, `ext/*` filters (e.g. `sorlandsbanen`) |
| `transfer-optimization` | Moving transfers to better places in found paths | transfer optimization service and cost | `routing/algorithm/transferoptimization` |
| `refetch` | Rebuilding an itinerary from a reference | refetch service and reference mappers | `routing/refetch` |

### L4 Data import and graph build

| Domain | Responsibility | Typical content | Currently in |
|---|---|---|---|
| `osm-import` | Reading OpenStreetMap into the street graph | OSM parser, way and node handling, tag mappers, boarding locations | `osm`, `graph_builder/module/osm`, `service/osminfo` |
| `gtfs-import` | Reading GTFS into the transit model | GTFS mappers, loaders | `gtfs` |
| `netex-import` | Reading NeTEx into the transit model | NeTEx mappers, loaders | `netex` |
| `graph-build` | Orchestrating the graph build and quality checks | build modules, linking, pruning, elevation, validation, import issue reporting | `graph_builder/*` |
| `graph-storage` | Saving and loading the graph | graph serialization, Kryo, load/save | `routing/graph`, `kryo`, `graph_builder/module/cache` |
| `datastore` | Accessing input data sources | data source, local and remote files | `datastore`, `ext/datastore` |

### L5 APIs and updaters

| Domain | Responsibility | Typical content | Currently in |
|---|---|---|---|
| `api-gtfs` | The GTFS GraphQL API | schema, data fetchers, mappers | `apis/gtfs` |
| `api-transmodel` | The Transmodel GraphQL API | schema, data fetchers, mappers | `apis/transmodel` |
| `api-other` | Other APIs: vector tiles, debug tiles, reports, OJP, actuator, inspector | | `apis/vectortiles`, `inspector`, `ext/vectortiles`, `ext/debugrastertiles`, `ext/reportapi`, `ext/ojp`, `ext/actuator` |
| `api-support` | API-neutral helpers: error mapping, parameter parsing, GraphQL framework | | `apis/support`, `api/*`, `framework/graphql` |
| `updater-framework` | Running real-time and data updaters | updater SPI, polling, scheduling, update results, write domain | `updater/spi`, `updater/support`, `updater/configure` |

The updater adapters (vehicle rental, parking, alerts, trip updates, vehicle positions) are
labeled with the domain they update. The domain owns the update logic. The updater framework
owns only the running of updaters.

### L6 Application

| Domain | Responsibility | Typical content | Currently in |
|---|---|---|---|
| `config` | Mapping JSON configuration into parameter types, and documenting them | config parsing, node adapters, doc generation | `standalone/config` |
| `runtime` | Running the process: server, metrics, warmup, project info, interactive launcher | HTTP server, metrics, warmup, version info | `standalone/server`, `warmup`, `model/projectinfo`, `ext/httpresponsetimemetrics`, `ext/interactivelauncher` |
| `application-wiring` | Creating and connecting the components | Dagger modules, `OTPMain`, application context | `*/configure`, `standalone/configure`, `framework/application` |

## Unclear and cross-cutting cases to decide

These are my open questions. The right answer depends on your judgment of the product.

1. **`transit-model` is large (about 190 classes plus timetables).** Should it split into static
   entities (`transit-model`) and timetables and patterns (`timetable`)? I suggest one domain
   first, and a split when the dependency report shows a clear seam.
2. **Fares:** decided. One sandbox domain, `fares`. The open point is the form of the extension
   points listed in the sandbox section, especially result attachment (2) and API contribution (3).
3. **`trip-plan-model` and `routing-request`:** both are hubs that most domains touch. Should they
   stay as domains with the contracts defined by a small interface set, or be split by feature
   (for example a request part per domain)? The plan says to split them by feature. The
   vocabulary keeps them as domains for now so they can be located, and the hub report will
   propose the split.
4. **`transfers` includes the nearby stop finder.** Is nearby search closer to `place-search`?
5. **`realtime-*`:** are three domains right, or should realtime updaters and their data be part
   of the domain they update (alerts with the transit model)? I chose separate domains because
   they have their own repositories and write paths.
6. **`ride-hailing`, `carpooling` and the `taxi` sandbox:** one `shared-mobility`-like domain, or
   three? Today they are separate sandboxes with different models.
7. **The `*Mapper` classes inside the APIs:** my rule 3 puts them in the API domain. This is
   consistent, but it means each API module will contain its own mapping of `Itinerary`.
8. **Testing code:** test classes follow the domain of the class they test. Shared test builders
   are labeled with the domain of the type they build. Speed tests and the harness are
   labeled `test-support` and stay outside the module split.

## Sandbox mapping (first guess)

| Sandbox | Domain |
|---|---|
| `carpooling` | `carpooling` |
| `fares` | `fares` (sandbox, extracted from the core, see above) |
| `flex` | `flex` |
| `ojp` | `api-other` |
| `vectortiles`, `debugrastertiles`, `reportapi`, `actuator` | `api-other` |
| `empiricaldelay`, `realtimeresolver` | `realtime-trip` |
| `emission` | `emissions` |
| `ridehailing`, `taxi` | `ride-hailing` (taxi: to decide) |
| `dataoverlay`, `edgenaming` | `street-network` |
| `vehicleparking`, `parkAndRideApi` | `vehicle-parking` |
| `vehiclerentalgeofencing`, `vehiclerentalservicedirectory` | `vehicle-rental` |
| `stopconsolidation` | `transit-model` |
| `siri` | `realtime-trip` (SIRI alerts to `realtime-alerts`) |
| `geocoder` | `place-search` |
| `transferanalyzer` | `transfers` |
| `sorlandsbanen` | `itinerary-filtering` |
| `datastore` | `datastore` |
| `httpresponsetimemetrics`, `interactivelauncher` | `runtime` |
| `accessibilityscore` | `trip-plan-model` (to decide) |

This table is from the package names only. The real labels come from reading the classes.
