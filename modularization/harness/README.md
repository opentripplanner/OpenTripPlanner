# Compliance harness

Checks that the modularized OTP behaves like plain dev-2.x. It is a black box: it only uses the
shaded jar and OTP's HTTP APIs, so it does not depend on any OTP package and runs unchanged
against both trees. It is a standalone Maven project, not part of the OTP reactor.

## Use

```bash
# Build the jar of the tree you want to test, e.g.:
mvn package -DskipTests -Dps -Dcs -pl otp-shaded -am

# Capture the answers of a jar. For each dataset it builds a graph, starts OTP and sends the
# scenarios of the dataset. Optionally name the datasets to run, e.g. oslo,portland
modularization/harness/harness.sh capture <otp-shaded.jar> <out-dir> [dataset,...]

# Compare two captures. Exit code 1 if they differ in a way that is not approved.
modularization/harness/harness.sh compare <baseline-dir> <candidate-dir>

# Capture twice with the same jar and compare. Must give no differences.
modularization/harness/harness.sh selfcheck <otp-shaded.jar> <work-dir>

# Build the graph of one dataset and keep OTP running on port 8080, to explore the data and
# write new scenarios
modularization/harness/harness.sh serve <otp-shaded.jar> <dataset>
```

A capture of all datasets takes about 20 seconds.

The unit tests of the harness run with `mvn -f modularization/harness/pom.xml verify`.

## Datasets

A dataset is a directory `datasets/<name>` with a `dataset.json` and `scenarios/*.json`. The
dataset lists its input files, relative to the root of this repository, and the build and router
configuration. A directory input can be zipped (`zipAs`) and a file renamed (`as`).

The inputs are read from the repository that contains the harness, not from the tree that is
tested. This way both trees get the same input, also after the test resources have moved.

| Dataset | Inputs | What it covers |
|---|---|---|
| `portland` | GTFS, OSM, elevation raster (NED), generated GTFS-RT | GTFS import, transit routing in both APIs, elevation on street edges and in steps, OSM park and ride, both GraphQL schemas. Two GTFS-RT update steps with trip updates (delays with propagation, absolute times, early run, cancelled trip, skipped stop, changed and withdrawn updates), alerts (route, stop, trip and route-type entities, translations, an alert that is removed) and vehicle positions (position, stop relationship, occupancy, vehicles that move, appear and disappear) |
| `oslo` | NeTEx (Nordic profile), OSM, generated SIRI-ET and SIRI-SX | NeTEx import and its mapping in both APIs: authorities, operators, lines, quays, stop places, service journeys, calendars, departures, trip search with transit. Four SIRI-ET update steps: delay and early run, cancelled trip and cancelled stop, extra journey, a later update that replaces an earlier one. Two SIRI-SX steps (`earlyStart` is 30 minutes): ten situations that affect a line, a stop point, a stop place, an operator, a vehicle journey, line stops, an unknown stop, a past period, a future period and two periods (an eleventh has no text and is ignored), then a new version of one, a closed one, one moved to other targets and a new one. Shown as GTFS alerts (also on routes, stops, trips and trip legs) and as Transmodel situations (also on quays, calls and legs) |
| `oslo-rental` | OSM, generated GBFS feeds | Vehicle rental from four GBFS networks: v2.3 stations, v2.3 free-floating scooters with geofencing, v3.0 stations, v3.0 free-floating vehicles with geofencing. Two update steps: station counts and flags change; vehicles are moved, disabled, removed and added and geofencing zones change. Rental trips in both APIs |
| `oslo-parking` | OSM, generated parking feeds | The vehicle parking sandbox updaters: ParkAPI (cars), bicycle ParkAPI, Liipi (facilities, hubs, utilizations), Bikely (POST), Bikeep (GeoJSON) and SIRI-FM (availability only, for lots of a ParkAPI feed that is read once an hour). Three states (initial, and two steps that change availability, close and reopen lots, add and remove lots, change hubs). Shown as GTFS vehicle parkings, lookups by id, and park-and-ride searches that pick another lot when a lot closes or disappears |
| `cobb-flex` | OSM, GTFS-Flex (Cobb County), two GTFS feeds | Flexible transit (the `FlexRouting` sandbox feature, enabled with `otpConfig`): zones, location stops, booking rules, trips with a flex window, flex transfers. Searches with flex access, egress and direct flex, arrive-by, a booking time, and outside the service period, in both APIs |
| `herrenberg` | OSM (German tag mapping) | OSM street import: walk, bicycle and car routing, street steps, OSM parking |
| `skoyen` | OSM (Norway) | The same on a second extract |

The street scenarios are generated from the OSM file with `tools/gen_street_scenarios.py`
(needs `osmium`). The SIRI-ET files and their scenarios come from `tools/gen_siri_et.py` and the SIRI-SX files, steps and scenarios from `tools/gen_siri_sx.py`. The ET scenarios come from
`tools/gen_oslo_rt_scenarios.py`, the GBFS feeds, dataset and scenarios of `oslo-rental` from
`tools/gen_gbfs.py`, and the GTFS-RT protobuf files and scenarios of `portland` from
`tools/gen_gtfs_rt.py` (a small protobuf encoder, no library needed). The output is deterministic and committed. The GBFS feeds are synthetic, in
the shapes of the GBFS test data of OTP, but located in Oslo so they link to the Oslo OSM extract.

## Real-time updates (steps)

A dataset can list `steps` in `dataset.json`. After the scenarios of the dataset have run, each
step in turn:

1. copies its files into a directory that an updater reads (`drops`; with `substitute` the text
   `${workDir}` in the file is replaced by the work directory),
2. waits until the updater has consumed the files (for SIRI the file loader renames `a.xml` to
   `a.xml.ok` or `a.xml.failed`; skipped when `awaitDir` is empty), then `settleMillis` more,
3. records the outcome of the file processing (`updater-files.json`), and
4. runs the scenarios in `scenarios/after-<step>/`.

The updaters read `file:` URLs, e.g. `"url": "file://${workDir}/siri-et"` in `routerConfig`. Do not
name a directory in the work directory with `gtfs` or `netex` in it: OTP takes it for input data.
The GTFS-RT files are replaced in place, and the updaters poll them every second.

Some updaters read from HTTP only (the vehicle parking updaters). The harness serves the directory
`<workDir>/http` on a local port for any method (some updaters POST), and the text `${httpBase}` in
the configuration is replaced with the base URL, e.g. `"url": "${httpBase}/parkapi/lots.json"`. A
step replaces the file with a drop into the directory (`"toDir": "http/parkapi"`). A step with no
`awaitDir` only waits `settleMillis`. The optional `otpConfig` in `dataset.json` is written to
`otp-config.json`, e.g. to turn on a sandbox feature.
Steps build on each other. The SIRI-ET updater drops a delivery that is older than the one before
or older than the start of OTP, so the generated files have increasing response timestamps in
the future.

## Scenarios

`datasets/<name>/scenarios/*.json`, one request each. The file name is the scenario name.

| Field | Meaning |
|---|---|
| `api` | `gtfs` (`/otp/gtfs/v1`), `transmodel` (`/otp/transmodel/v3`) or `info` (`GET /otp/`) |
| `query`, `variables` | the GraphQL request |
| `ignore` | path globs of values that are not stable between runs, removed before saving |
| `unordered` | path globs of arrays the API does not order, sorted by content before saving |
| `sortArraysBy` | sort arrays of objects by this text field, used for lists and introspection |

A path looks like `data.stops[3].name`. In a glob, `*` matches any characters. In the approved
list a scenario is named `<dataset>/<scenario>`, e.g. `oslo/gtfs-stops`.

An error answer is also behavior, so it is compared like any other answer. Some scenarios have
routing errors on purpose, for example a search outside the service period.

## Approved discrepancies

`approved-discrepancies.json` lists accepted differences:

```json
{ "discrepancies": [
  { "scenario": "portland/gtfs-stops", "path": "data.stops[*].name", "reason": "...", "owner": "..." }
] }
```

Every entry needs a reason and an owner. An entry that matches no difference fails the
comparison, so the list can only shrink.

## Known sources of nondeterminism in dev-2.x

Found by `selfcheck`. They are worked around in the scenarios, not hidden:

- `Stop.vehicleMode` in the GTFS API changes between runs for stops with several modes. It is not
  requested.
- The order of patterns in `stoptimesForServiceDate`, of the routes of a stop, of the journey
  patterns of a line, of the patterns of a route, and of the tags of a parking is not stable. Those arrays are `unordered`.
- The Transmodel `lines` query fails with an internal error on these datasets. The scenarios use
  `line(id:)` instead.

## Configuration documentation

`harness.sh capture` also stores the generated configuration documentation under `config-docs/`.
OTP generates the parameter documentation from the configuration code and checks it in under
`doc/user`; the templates in `doc/templates` tell which files are generated (build, router, route
request, updaters, sandbox features, OSM mappers, ...). The documentation is the contract of the
configuration: parameter names, types, defaults, descriptions, examples. Each document is split by
heading, and a changed section is one difference. The documents are read from the source tree, not
from the jar. Set `OTP_TREE` to the tree the jar was built from. The default is this checkout,
which is right for the baseline only.

## Not covered yet

- The JSON schema of the configuration. OTP has none.
- Real-time updaters other than SIRI-ET, SIRI-SX, GTFS-RT (trip updates, alerts, vehicle
  positions), GBFS and the vehicle parking updaters: MQTT, the HTTP request/response and Lite
  variants of SIRI.
- Fares. By decision: they become a sandbox, see `plan.md`.
- More input: the EPIP NeTEx profile (the sample in the repository has no stop places, so it
  gives no trips; it needs a real German extract), NeTEx flexible lines, flex with real-time
  updates, fares.
- Elevation outside Portland. Only the Portland raster is in the repository.
- The graph build output itself, other than through the API answers.
- Performance. SpeedTest stays the tool for that.
