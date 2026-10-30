# Transfer configuration

Transfers are the walks between two stops that transit routing uses to change from one trip to
another. This page describes the `transfers` section of `build-config.json`, which controls the
_regular_ transfers between stops. The transfers of flex routing and of via coordinates are
configured in the `transfers.flex` and `transfers.via` blocks, see
[Flex and via transfers](#flex-and-via-transfers).

!!! note

    The `transfers` configuration is new (since version 2.11) and under development. Walk,
    wheelchair, bicycle and car transfers can be configured.

## Concepts

**Template.** When the graph is built, OTP searches the street graph from each stop and finds the
street paths to the stops nearby. The paths are stored as templates in the serialized graph. A
template does not have a final duration or cost. Templates take up a lot of memory, since they
contain the street path used for any pair of nearby stops, and the number of paths grows with the
number of profiles. To save memory a parent profile can share some of the street paths with its
children if they are almost the same. A reuse limit decides when to share.

**Routing transfer.** A routing transfer is used by the transit routing at request time. OTP creates
the routing transfers from the templates when they are needed. The user preferences are taken from
the routing request: it re-calculates the duration and the generalized cost of every template path.
A routing transfer holds the result, but not the street paths, so routing transfers take up little
memory compared to templates. They are cached, so the next request with the same preferences is
fast.

**Profile.** A profile is the set of templates for one kind of traveler, found with the user
preferences of that traveler, for example a slow walker. When a routing request arrives, OTP picks
the profile with the user preferences closest to the request, and creates the routing transfers from
the templates of the profile. Since the cost of a profile is memory, a profile is only worth it if
its travelers need other paths than the ones of the other profiles. Other preferences are handled
when the routing transfers are created, see [Warmup](#warmup). Profiles are grouped by type (`walk`,
`wheelchair`, `bicycle` or `car`), and each type has its own configuration properties relevant for
that specific type.

**User preferences.** The preferences of a traveler, like walking speed and reluctance, that are
used to find and cost the templates, and to create the routing transfers. A profile without
`userPreferences` gets the ones of its parent of the same type, or the defaults of the type, see
[User preferences](#user-preferences). The user preferences of the profiles must be unique.

## Configuration structure

The `transfers` section has one block for each profile type: `walk`, `wheelchair`, `bicycle` and
`car`. A block has the `profiles` of the type, and the settings that are shared by them:

- `profiles`: the profiles of the type, each with an `id` and optionally `userPreferences`, a
  `parent` and a `reuseLimit`. Each profile makes its own templates, for one kind of traveler.
- `maxDuration`: how far from a stop the transfers are searched.
- `warmup`: preferences that are common in the requests, to create the transfers for at startup.
- the street parameters of the type, for example `stairsReluctance`.

The `flex` and `via` blocks are not profile types, see
[Flex and via transfers](#flex-and-via-transfers). Details are in
[Configuration reference](#configuration-reference).

## Example

```json
// build-config.json
{
  "transfers": {
    "walk": {
      "profiles": [
        {
          "id": "walk",
          "userPreferences": { "speed": 1.3, "reluctance": 2.0 }
        },
        {
          "id": "slow-walk",
          "parent": "walk",
          "reuseLimit": "30s + 0.05x",
          "userPreferences": { "speed": 0.9, "reluctance": 4.0 }
        }
      ],
      "maxDuration": "40m",
      "warmup": [
        {
          "id": "avoid-walking",
          "parent": "walk",
          "userPreferences": { "speed": 1.15, "reluctance": 10.0 }
        }
      ]
    }
  }
}
```

If the `transfers` section is left out, a single `walk` profile with the preferences of the default
request and a max duration of 30 minutes is used. The `maxDuration` of a profile type is optional,
the default is 30 minutes.

A profile type with a block and without `profiles`, also an empty one, gets one profile with the
name of the type as its id and the default preferences. This configures a `wheelchair` profile and
no `walk` profile:

```json
{
  "transfers": {
    "wheelchair": {}
  }
}
```

The `walk` profile is only the default if no profile type is configured, which is also the case with
a `transfers` section that only has `flex` or `via`. Then only walking is supported: a request for
another profile type, like wheelchair or bicycle, has no transfers to use and fails. This is the
same if a profile type is configured: a request for a type that is not configured fails, so
configure all the types the clients ask for.

A routing request is matched to a type by its transfer street mode, and for walking by the
wheelchair flag: a request for a wheelchair accessible trip uses the `wheelchair` profiles, any
other walking request uses the `walk` profiles, a request that transfers by bike uses the `bicycle`
profiles, and a request that transfers by car uses the `car` profiles.

## User preferences

The `walk`, `wheelchair` and `bicycle` types have `speed` and `reluctance`, and the `car` type only
has `reluctance`. The defaults of `walk` and `wheelchair` are the same, and the default `speed` of
`bicycle` is `5.0`. The street model decides which streets and elevators a wheelchair can use, and
where a bicycle or a car can go. The `bicycle` type also has the preferences for walking with the
bicycle, when it is pushed, and how the route is optimized:

The `optimize` values are normalized so they sum to 1.0. If `optimize` is set, the values that are
not set are `0.0`, so `optimize: { time: 0.1 }` is `time: 1.0`, `slope: 0.0` and `safety: 0.0`. If
it is not set in a profile, the profile inherits it from a parent of the same type, and a root gets
it from the request. The default is the safest streets (`safety: 1.0`).

## Street parameters

User preferences are user facing parameters available through the api, parameters that are not use
facing are set in the profile block. They are the same for all profile types, and the default is the
default of the route request defined in code.

!!! Note

    The value in `routingDefaults` of `router-config.json` is not used for the transfers, since the
    router configuration is not available when the graph is built!

When the server starts, OTP logs a warning for each profile type that has one of these parameters
different from the same parameter in `routingDefaults` of `router-config.json`. The user
preferences, like the walking speed, are not compared, they are set by each request.

The elevator settings of the street search (`ElevatorPreferences`), the street-level settings like
`turnReluctance` and `drivingDirection`, and the wheelchair `stairsTimeFactor` are not configurable
for the transfers, they have the default values.

By default the `wheelchair` profile only uses the stops that are marked as wheelchair accessible. If
the accessibility data of the stops is incomplete, almost all the transfers are lost. Set
`unknownCost` and `inaccessibleCost` of `wheelchair.stop` to use the other stops with a cost:

```json
{
  "transfers": {
    "wheelchair": {
      "stop": { "unknownCost": 600, "inaccessibleCost": 3600 }
    }
  }
}
```

## Parent and deduplication

A profile can name another profile as its `parent` with the `id` of the parent. A parent can be of
another type, for example a `wheelchair` profile with the `walk` profile as its parent. A child will
inherit its parent in two ways: **Value Inheritance** and **Street Path Reuse**.

**Value Inheritance.** Values are only inherited within a _profile type_. If the parent is a profile
of the same type, the profile inherits the `userPreferences` and the `reuseLimit` of the parent.
Everything that is set on the profile overrides the inherited value, and everything that is not set
is the value of the parent. A profile with a parent and without `userPreferences` of its own would
get the same preferences as the parent, and is rejected, since the profiles must be unique. A
profile without a parent of the same type is the root of its type, and the values it does not set
are the defaults.

**Street Path Reuse.** A child may reuse the street path between a pair of stops from its parent to
save memory. Each profile still searches for its own transfers, but the transfers that are the same,
or almost the same, as the transfers of the parent are used for both the child and its parent. To do
a fair comparison the parent street path is re-traversed with the child user preferences and the
generalized cost is compared. If the cost is close enough then the street path is shared. If not,
the child keeps the street path found with its own preferences.

"Close enough" is set with `reuseLimit`. It is optional, and inherited if not set. The limit is a
linear function of the cost `x` of the parent street path. For example `15s + 0.02x`: a path that
costs up to 2% and 15 seconds more than the best path is used. A limit of `0s + 0x` only shares the
paths with the same cost. The limit applies to the generalized cost only, not to the duration. This
is acceptable for two reasons:

1. The street graph uses the generalized cost as the main criterion.
2. If the cost is almost the same, the users are not likely to be able to tell them apart and the
   uncertainty in the cost calculation by far exceeds the simplification of the optimization.

The graph build logs how many of the paths of each profile are the paths of its parent:

```
Profile 'slow-walk' paths: 85 142, 82 588 (97%) reused 'walk' - WalkPreferences{speed: 0.9m/s}.
```

Use a parent for profiles that are close to another profile, like a slightly slower walker.

## Warmup

When a routing request arrives, OTP creates the transfers for the preferences of the request, by
re-calculating the duration and the generalized cost of the stored transfers of a profile. This
takes a moment, and it is done by the first request with these preferences. The result is cached, so
the following requests are fast.

When the server starts, before the first request, OTP creates the routing transfers for the _known
set of user preferences_. The set is all profile user preferences and all preferences in the
`warmup` section. Use it for the preferences that are common in the requests, for example the
default preferences of a client, but that are not different enough from a profile to be worth a
profile of their own. A warmup entry needs:

- an `id`, unique among all profiles and warmup entries. It is used in the logs.
- a `parent`, the `id` of a profile. The transfers are created from the stored transfers of the
  parent of the same type, and the entry inherits the preferences of the parent. A warmup entry can
  not be a parent.
- `userPreferences`. They must be different from the preferences of every profile and every other
  warmup entry.

No transfers are searched for or stored for a warmup entry, so it adds nothing to the serialized
graph.

The server start waits for the transfers to be created. The time of each profile and each entry is
logged:

```
Warmed up the Routing Transfer Cache for the profile 'walk' in 150 ms.
Warmed up the Routing Transfer Cache for 'avoid-walking' from the paths of 'walk' in 170 ms.
```

The transfers for other preferences are created by the first request that uses them. The cache holds
a limited number of preference sets, and the least recently used are dropped first, so a large
number of warmup entries and profiles may drop the first ones.

### Finding the warmup entries with the cache report

The cache report lists the user preferences that routing requests used, and how many requests used
each. Use it to find the preferences that are common in your traffic, but have no profile or warmup
entry yet. The report is part of the [Report API](sandbox/ReportApi.md), and the statistics are only
collected if the `ReportApi` feature is on.

1. Enable the `ReportApi` feature and run OTP with real traffic, for example the requests of your
   clients, for some time.
2. Fetch the report: `http://localhost:8080/otp/report/regular-transfers/cache.json`. The entries
   with the most hits come first.
3. Look at the entries with the source `ON_DEMAND`: the preferences of the requests that were not a
   profile or a warmup entry. These are created by the first request that uses them. An entry with
   many hits is a good candidate for a `warmup` entry.
4. Add a `warmup` entry with the same `userPreferences`. Use `closestProfileId` of the entry as
   `parent`, if it is a profile of the same type.

Each entry in the report has:

- `type`: the profile type of the preferences.
- `source`: `PROFILE`, `WARMUP` or `ON_DEMAND`. The first two are created when the server starts.
- `id`: the id of the profile or the warmup entry, and nothing for an `ON_DEMAND` entry.
- `closestProfileId`: the profile the routing transfers are created from.
- `userPreferences`: the user preferences, as text. Values that are not shown are the defaults.
- `hits`: the number of requests that used the preferences.
- `cached`: whether the routing transfers of the preferences are in the cache now.

The report only tracks a limited number of keys. If there are many different preferences, the least
used are dropped, so the report is not complete and the numbers are approximate.

## Max durations

The max duration is how far from a stop the transfers are searched when the graph is built, measured
with the user preferences of the profile. A longer duration finds more transfers and makes the graph
larger and slower to build. There is one `maxDuration` for each profile type, and it is shared by
all the profiles of the type. It is optional, and it is not inherited from another type or from a
parent profile. The default is 30 minutes:

```json
"walk": { "maxDuration": "40m" }
```

`maxDuration` is the duration between the stops the mode can use:

| Type         | `maxDuration` is the max duration between |
| ------------ | ----------------------------------------- |
| `walk`       | all stops                                 |
| `wheelchair` | all stops                                 |
| `bicycle`    | stops that allow bicycles                 |
| `car`        | stops that allow cars                     |

A stop allows bicycles or cars if a trip that allows them visits the stop, for example a ferry. Both
stops of a transfer must allow the mode.

A bicycle can cover a much longer distance than a walker, so the `bicycle` type has two more
parameters, and all three are independent of each other. The longest limit that _applies to a
transfer_ is used:

| Parameter                          | Type       | Description                                                      |
| ---------------------------------- | ---------- | ---------------------------------------------------------------- |
| `bicycle.maxDurationAllowAllModes` | `duration` | The max duration between all stops. Shorter than the two others. |
| `bicycle.maxDuration`              | `duration` | The max duration between stops that allow bicycles.              |
| `bicycle.maxDurationAllowsCar`     | `duration` | The max duration between stops that allow cars.                  |

If none of them is set, `maxDuration` is 30 minutes. `maxDurationAllowAllModes` must be shorter than
`maxDuration` and `maxDurationAllowsCar`, if they are set. If `maxDurationAllowAllModes` is left
out, there are only transfers between the stops that allow bicycles or cars:

```json
"bicycle": {
  "maxDurationAllowAllModes": "20m",
  "maxDuration": "45m",
  "maxDurationAllowsCar": "2h"
}
```

The `car` type only has transfers between stops that allow cars, and none from or to other stops. No
search is made from the other stops, which makes the graph faster to build:

```json
"car": { "maxDuration": "3h" }
```

A car reaches a lot of stops in a short time, so a car search from all stops does not work, even for
small deployments. The max duration only limits the street path search. A stored transfer is never
dropped when it is re-calculated for a routing request, even if the request walks slower than the
profile.

Stops of a station that does not allow transfers have no transfers from or to them.

!!! note

    The old transfer settings are replaced by the profile types, see
    [Migrating from the old configuration](#migrating-from-the-old-configuration).

## Flex and via transfers

| Parameter          | Type       | Description                                                                                                                  | Default             |
| ------------------ | ---------- | ---------------------------------------------------------------------------------------------------------------------------- | ------------------- |
| `flex.maxDuration` | `duration` | The longest transfer between two stops, created for flex routing.                                                            | `"30m"`             |
| `flex.requests`    | `object[]` | The [routing requests](RouteRequest.md) the flex transfers are created with. Only requests with street mode `WALK` are used. | the default request |
| `via.maxDuration`  | `duration` | The longest transfer between a via coordinate and a stop.                                                                    | `"30m"`             |

They replace the old `transferRequests` and `maxTransferDuration` of the build config, which are not
read any more. The flex transfers are only created if the sandbox feature `FlexRouting` is on.

## Migrating from the old configuration (2.10 and earlier)

The old transfer settings are not read anymore. OTP reports them as unexpected parameters when it
starts, and creates the transfers with the defaults of the new configuration, so check the log. This
is where each old setting went:

| Old setting                                                             | New setting                                                                    |
| ----------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| `maxTransferDuration`                                                   | `walk.maxDuration`, `wheelchair.maxDuration`, `flex.maxDuration`.              |
| `transferRequests` with the mode `WALK`                                 | the `walk` block                                                               |
| `transferRequests` with `wheelchairAccessibility: { enabled: true }`    | the `wheelchair` block                                                         |
| `transferRequests` with the mode `BICYCLE` or `CAR`                     | the `bicycle` or `car` block                                                   |
| other parameters of a request in `transferRequests`                     | the [street parameters](#street-parameters) of the type, only those can be set |
| `transferParametersForMode.CAR.carsAllowedStopMaxTransferDuration`      | `car.maxDuration`                                                              |
| `transferParametersForMode.BICYCLE.bikesAllowedStopMaxTransferDuration` | `bicycle.maxDuration`                                                          |
| `disableDefaultTransfers`                                               | leave out `bicycle.maxDurationAllowAllModes`                                   |
| `transit.transferCacheRequests` of `router-config.json`                 | `warmup` entries, see [Warmup](#warmup)                                        |

**Configure every profile type that the clients use.** A request for a transfer mode that is not
configured is not supported. It fails with an error, there is no fallback. If clients ask for
bicycle transfers, add a bicycle profile, for example a small set of paths between all stops:

```json
"bicycle": {
  "maxDurationAllowAllModes": "10m",
  "profiles": [{ "id": "bicycle", "parent": "walk", "reuseLimit": "15s + 0.05x" }]
}
```

Bicycle is the most expensive type, so check the build time and the memory first. The default `walk`
profile is only used if no profile type is configured at all. If you only configure `wheelchair` or
`car`, there are no walk transfers.

**The flex limit is separate now.** The flex transfers used the walk limit, and now they use
`transfers.flex.maxDuration`, `30m` by default. If your walk limit was longer, set it for flex too.

**The wheelchair only uses the stops that are marked accessible** by default. If the accessibility
data of the stops is incomplete, most wheelchair transfers are lost, see
[Street parameters](#street-parameters).

When the server starts, OTP also warns about the street parameters of the transfers that differ from
`routingDefaults` of `router-config.json`.

**Running older versions of OTP with the same configuration.** Older versions do not know the
`transfers` section, see [Migrating configuration](Migrating-Configuration.md). They do not read
`transfers.flex` and `transfers.via` either, so keep the old `transferRequests` and
`maxTransferDuration` for flex and via while both versions run.

## Configuration reference

This is the reference of all the parameters in the `transfers` section of `build-config.json`. The
profile type blocks `walk`, `wheelchair`, `bicycle` and `car` have the same parameters unless stated
otherwise.

<!-- INSERT: PARAMETERS-TABLE -->

<!-- INSERT: PARAMETERS-DETAILS -->

### Complete example

This example sets every parameter of the `transfers` section:

<!-- INSERT: JSON-EXAMPLE -->
