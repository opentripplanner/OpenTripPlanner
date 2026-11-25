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
| `portland` | GTFS, OSM, elevation raster (NED) | GTFS import, transit routing in both APIs, elevation on street edges and in steps, OSM park and ride, both GraphQL schemas |
| `oslo` | NeTEx (Nordic profile), OSM | NeTEx import and its mapping in both APIs: authorities, operators, lines, quays, stop places, service journeys, calendars, departures, trip search with transit |
| `herrenberg` | OSM (German tag mapping) | OSM street import: walk, bicycle and car routing, street steps, OSM parking |
| `skoyen` | OSM (Norway) | The same on a second extract |

The street scenarios are generated from the OSM file with `tools/gen_street_scenarios.py`
(needs `osmium`). The output is deterministic and committed.

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
  patterns of a line, and of the tags of a parking is not stable. Those arrays are `unordered`.
- The Transmodel `lines` query fails with an internal error on these datasets. The scenarios use
  `line(id:)` instead.

## Not covered yet

- Generated configuration documentation and the JSON schema of the configuration.
- Real-time updaters on recorded feeds.
- More input: the EPIP NeTEx profile (the sample in the repository has no stop places, so it
  gives no trips; it needs a real German extract), flex, vehicle rental, fares, GBFS.
- Elevation outside Portland. Only the Portland raster is in the repository.
- The graph build output itself, other than through the API answers.
- Performance. SpeedTest stays the tool for that.
