# Compliance harness

Checks that the modularized OTP behaves like plain dev-2.x. It is a black box: it only uses the
shaded jar and OTP's HTTP APIs, so it does not depend on any OTP package and runs unchanged
against both trees. It is a standalone Maven project, not part of the OTP reactor.

## Use

```bash
# Build the jar of the tree you want to test, e.g.:
mvn package -DskipTests -Dps -Dcs -pl otp-shaded -am

# Capture the answers of a jar (builds a graph, starts OTP, sends all scenarios)
modularization/harness/harness.sh capture <otp-shaded.jar> <out-dir> [data-dir]

# Compare two captures. Exit code 1 if they differ in a way that is not approved.
modularization/harness/harness.sh compare <baseline-dir> <candidate-dir>

# Capture twice with the same jar and compare. Must give no differences.
modularization/harness/harness.sh selfcheck <otp-shaded.jar> <work-dir>
```

The default data is the Portland test data in `application/src/test/resources/portland`. A capture
takes about 10 seconds.

The unit tests of the harness run with `mvn -f modularization/harness/pom.xml verify`.

## Scenarios

`scenarios/*.json`, one request each. The file name is the scenario name.

| Field | Meaning |
|---|---|
| `api` | `gtfs` (`/otp/gtfs/v1`), `transmodel` (`/otp/transmodel/v3`) or `info` (`GET /otp/`) |
| `query`, `variables` | the GraphQL request |
| `ignore` | path globs of values that are not stable between runs, removed before saving |
| `unordered` | path globs of arrays the API does not order, sorted by content before saving |
| `sortArraysBy` | sort arrays of objects by this text field, used for lists and introspection |

A path looks like `data.stops[3].name`. In a glob, `*` matches any characters.

Current scenarios: both GraphQL schemas (introspection), the stops, routes, patterns, trips and
stop times, nearby search, and several trip searches in both APIs (walking, transit, bicycle,
arrive-by, evening, weekend, outside the service period, preferences).

An error answer is also behavior. `tm-lines` fails on the Portland data in dev-2.x today, so that
failure is the baseline.

## Approved discrepancies

`approved-discrepancies.json` lists accepted differences:

```json
{ "discrepancies": [
  { "scenario": "gtfs-stops", "path": "data.stops[*].name", "reason": "...", "owner": "..." }
] }
```

Every entry needs a reason and an owner. An entry that matches no difference fails the
comparison, so the list can only shrink.

## Known sources of nondeterminism in dev-2.x

Found by `selfcheck`. They are worked around in the scenarios, not hidden:

- `Stop.vehicleMode` in the GTFS API changes between runs for stops with several modes. It is not
  requested in `gtfs-stops`.
- The order of patterns in `stoptimesForServiceDate` and of routes of a stop is not stable. Those
  arrays are listed as `unordered`.

## Not covered yet

- Generated configuration documentation and the JSON schema of the configuration.
- Real-time updaters on recorded feeds.
- More data sets: NeTEx, flex, vehicle rental and parking, fares.
- The graph build output itself, other than through the API answers.
- Performance. SpeedTest stays the tool for that.
