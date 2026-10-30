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

<!-- PARAMETERS-TABLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

| Config Parameter                                                                           |          Type          | Summary                                                                                                          |  Req./Opt. | Default Value    | Since |
|--------------------------------------------------------------------------------------------|:----------------------:|------------------------------------------------------------------------------------------------------------------|:----------:|------------------|:-----:|
| [bicycle](#transfers_bicycle)                                                              |        `object`        | The configuration of the bicycle transfers.                                                                      | *Optional* |                  |  2.10 |
|    [maxDuration](#transfers_bicycle_maxDuration)                                           |       `duration`       | The longest bicycle transfer, measured with the user preferences.                                                | *Optional* |                  |  2.10 |
|    [maxDurationAllowAllModes](#transfers_bicycle_maxDurationAllowAllModes)                 |       `duration`       | The longest bicycle transfer between any two stops.                                                              | *Optional* |                  |  2.10 |
|    [maxDurationAllowsCar](#transfers_bicycle_maxDurationAllowsCar)                         |       `duration`       | The longest bicycle transfer between stops that allow cars.                                                      | *Optional* |                  |  2.10 |
|    [profiles](#transfers_bicycle_profiles)                                                 |       `object[]`       | The bicycle transfer profiles.                                                                                   | *Optional* |                  |  2.10 |
|          [id](#transfers_bicycle_profiles_0_id)                                            |        `string`        | The id of the profile.                                                                                           | *Required* |                  |  2.10 |
|          [parent](#transfers_bicycle_profiles_0_parent)                                    |        `string`        | The id of the profile this profile is based on.                                                                  | *Required* |                  |  2.10 |
|          [reuseLimit](#transfers_bicycle_profiles_0_reuseLimit)                            | `cost-linear-function` | How much more the path of the parent may cost, to be used by this profile.                                       | *Optional* | `"15s + 0.02 t"` |  2.10 |
|          [userPreferences](#transfers_bicycle_profiles_0_userPreferences)                  |        `object`        | Street-search preferences used to discover and cost this profile's paths.                                        | *Optional* |                  |  2.10 |
|             reluctance                                                                     |        `double`        | A multiplier for the cost of riding a bicycle.                                                                   | *Optional* | `2.0`            |  2.10 |
|             speed                                                                          |        `double`        | Bicycle speed, in meters per second.                                                                             | *Optional* | `5.0`            |  2.10 |
|             [optimize](#transfers_bicycle_profiles_0_userPreferences_optimize)             |        `object`        | How the route of the bicycle is optimized.                                                                       | *Optional* |                  |  2.10 |
|                safety                                                                      |        `double`        | The importance of the safety, the safest streets.                                                                | *Optional* | `0.0`            |  2.10 |
|                slope                                                                       |        `double`        | The importance of the slope, the flattest streets.                                                               | *Optional* | `0.0`            |  2.10 |
|                time                                                                        |        `double`        | The importance of the time, the shortest duration.                                                               | *Optional* | `0.0`            |  2.10 |
|             [walk](#transfers_bicycle_profiles_0_userPreferences_walk)                     |        `object`        | The preferences for walking with the bicycle.                                                                    | *Optional* |                  |  2.10 |
|                [reluctance](#transfers_bicycle_profiles_0_userPreferences_walk_reluctance) |        `double`        | A multiplier for the cost of walking with the bicycle.                                                           | *Optional* | `5.0`            |  2.10 |
|                speed                                                                       |        `double`        | The speed of walking with the bicycle, in meters per second.                                                     | *Optional* | `1.35`           |  2.10 |
|    walking                                                                                 |        `object`        | Parameters for walking with the bicycle, on top of the `walk` user preferences.                                  | *Optional* |                  |  2.10 |
|       mountDismountCost                                                                    |        `integer`       | The cost of hopping on or off the bicycle.                                                                       | *Optional* | `0`              |  2.10 |
|       mountDismountTime                                                                    |       `duration`       | The time it takes the user to hop on or off the bicycle.                                                         | *Optional* | `"PT0S"`         |  2.10 |
|       stairsReluctance                                                                     |        `double`        | How bad is it to walk the bicycle up/down a flight of stairs, on top of the reluctance.                          | *Optional* | `2.0`            |  2.10 |
|    [warmup](#transfers_bicycle_warmup)                                                     |       `object[]`       | The user preferences to create the transfers for when the server starts.                                         | *Optional* |                  |  2.10 |
| [car](#transfers_car)                                                                      |        `object`        | The configuration of the car transfers.                                                                          | *Optional* |                  |  2.10 |
|    [maxDuration](#transfers_car_maxDuration)                                               |       `duration`       | The longest car transfer, measured with the user preferences.                                                    | *Optional* | `"PT30M"`        |  2.10 |
|    [profiles](#transfers_car_profiles)                                                     |       `object[]`       | The car transfer profiles.                                                                                       | *Optional* |                  |  2.10 |
|          [id](#transfers_car_profiles_0_id)                                                |        `string`        | The id of the profile.                                                                                           | *Required* |                  |  2.10 |
|          [userPreferences](#transfers_car_profiles_0_userPreferences)                      |        `object`        | Street-search preferences used to discover and cost this profile's paths.                                        | *Optional* |                  |  2.10 |
|             reluctance                                                                     |        `double`        | A multiplier for the cost of driving.                                                                            | *Optional* | `2.0`            |  2.10 |
|    [warmup](#transfers_car_warmup)                                                         |       `object[]`       | The user preferences to create the transfers for when the server starts.                                         | *Optional* |                  |  2.10 |
| [flex](#transfers_flex)                                                                    |        `object`        | The configuration of the transfers created for flex routing.                                                     | *Optional* |                  |  2.10 |
|    maxDuration                                                                             |       `duration`       | Transfers up to this duration with a mode-specific speed value will be pre-calculated and included in the Graph. | *Optional* | `"PT30M"`        |  2.10 |
|    [requests](#transfers_flex_requests)                                                    |       `object[]`       | Routing requests to use for pre-calculating stop-to-stop transfers.                                              | *Optional* |                  |  2.10 |
| via                                                                                        |        `object`        | The configuration of the transfers between a via coordinate and the stops.                                       | *Optional* |                  |  2.10 |
|    maxDuration                                                                             |       `duration`       | The longest transfer between a via coordinate and a stop.                                                        | *Optional* | `"PT30M"`        |  2.10 |
| [walk](#transfers_walk)                                                                    |        `object`        | The configuration of the walk transfers.                                                                         | *Optional* |                  |  2.10 |
|    [maxDuration](#transfers_walk_maxDuration)                                              |       `duration`       | The longest walk transfer, measured with the user preferences.                                                   | *Optional* | `"PT30M"`        |  2.10 |
|    stairsReluctance                                                                        |        `double`        | A multiplier to specify how bad walking on stairs is, on top of the reluctance of the user preferences.          | *Optional* | `2.0`            |  2.10 |
|    stairsTimeFactor                                                                        |        `double`        | How much more time does it take to walk a flight of stairs compared to walking a similar horizontal length.      | *Optional* | `3.0`            |  2.10 |
|    escalator                                                                               |        `object`        | Escalator preferences.                                                                                           | *Optional* |                  |  2.10 |
|       reluctance                                                                           |        `double`        | A multiplier for how bad being in an escalator is compared to being in transit for equal lengths of time         | *Optional* | `1.5`            |  2.10 |
|       speed                                                                                |        `double`        | How fast does an escalator move horizontally, in m/s?                                                            | *Optional* | `0.45`           |  2.10 |
|    [profiles](#transfers_walk_profiles)                                                    |       `object[]`       | The walk transfer profiles.                                                                                      | *Optional* |                  |  2.10 |
|       { object }                                                                           |        `object`        | Nested object in array. The object type is determined by the parameters.                                         | *Optional* |                  |  2.10 |
|          [id](#transfers_walk_profiles_0_id)                                               |        `string`        | The id of the profile.                                                                                           | *Required* |                  |  2.10 |
|          [userPreferences](#transfers_walk_profiles_0_userPreferences)                     |        `object`        | Street-search preferences used to discover and cost this profile's paths.                                        | *Optional* |                  |  2.10 |
|             reluctance                                                                     |        `double`        | A multiplier for the cost of walking.                                                                            | *Optional* | `2.0`            |  2.10 |
|             [safetyFactor](#transfers_walk_profiles_0_userPreferences_safetyFactor)        |        `double`        | How much the safety of the streets is considered.                                                                | *Optional* | `1.0`            |  2.10 |
|             speed                                                                          |        `double`        | Walking speed, in meters per second.                                                                             | *Optional* | `1.35`           |  2.10 |
|       { object }                                                                           |        `object`        | Nested object in array. The object type is determined by the parameters.                                         | *Optional* |                  |  2.10 |
|          [id](#transfers_walk_profiles_1_id)                                               |        `string`        | The id of the profile.                                                                                           | *Required* |                  |  2.10 |
|          [parent](#transfers_walk_profiles_1_parent)                                       |        `string`        | The id of the profile this profile is based on.                                                                  | *Required* |                  |  2.10 |
|          [reuseLimit](#transfers_walk_profiles_1_reuseLimit)                               | `cost-linear-function` | How much more the path of the parent may cost, to be used by this profile.                                       | *Optional* | `"15s + 0.02 t"` |  2.10 |
|          [userPreferences](#transfers_walk_profiles_1_userPreferences)                     |        `object`        | Street-search preferences used to discover and cost this profile's paths.                                        | *Optional* |                  |  2.10 |
|             reluctance                                                                     |        `double`        | A multiplier for the cost of walking.                                                                            | *Optional* | `2.5`            |  2.10 |
|             [safetyFactor](#transfers_walk_profiles_1_userPreferences_safetyFactor)        |        `double`        | How much the safety of the streets is considered.                                                                | *Optional* | `0.5`            |  2.10 |
|             speed                                                                          |        `double`        | Walking speed, in meters per second.                                                                             | *Optional* | `1.4`            |  2.10 |
|    [warmup](#transfers_walk_warmup)                                                        |       `object[]`       | The user preferences to create the transfers for when the server starts.                                         | *Optional* |                  |  2.10 |
|          [id](#transfers_walk_warmup_0_id)                                                 |        `string`        | The id of the warmup entry.                                                                                      | *Required* |                  |  2.10 |
|          parent                                                                            |        `string`        | The id of the profile the transfers are created from.                                                            | *Required* |                  |  2.10 |
|          [userPreferences](#transfers_walk_warmup_0_userPreferences)                       |        `object`        | The user preferences to warm up.                                                                                 | *Optional* |                  |  2.10 |
|             reluctance                                                                     |        `double`        | A multiplier for the cost of walking.                                                                            | *Optional* | `2.5`            |  2.10 |
|             [safetyFactor](#transfers_walk_warmup_0_userPreferences_safetyFactor)          |        `double`        | How much the safety of the streets is considered.                                                                | *Optional* | `0.5`            |  2.10 |
|             speed                                                                          |        `double`        | Walking speed, in meters per second.                                                                             | *Optional* | `1.4`            |  2.10 |
| [wheelchair](#transfers_wheelchair)                                                        |        `object`        | The configuration of the wheelchair transfers.                                                                   | *Optional* |                  |  2.10 |
|    inaccessibleStreetReluctance                                                            |        `double`        | The factor to multiply the cost of traversing a street edge that is not wheelchair-accessible.                   | *Optional* | `25.0`           |  2.10 |
|    [maxDuration](#transfers_wheelchair_maxDuration)                                        |       `duration`       | The longest wheelchair transfer, measured with the user preferences.                                             | *Optional* | `"PT30M"`        |  2.10 |
|    [maxSlope](#transfers_wheelchair_maxSlope)                                              |        `double`        | The maximum slope as a fraction of 1.                                                                            | *Optional* | `0.083`          |  2.10 |
|    slopeExceededReluctance                                                                 |        `double`        | How much streets with high slope should be avoided.                                                              | *Optional* | `1.0`            |  2.10 |
|    stairsReluctance                                                                        |        `double`        | How much stairs should be avoided.                                                                               | *Optional* | `100.0`          |  2.10 |
|    elevator                                                                                |        `object`        | Configuration for when to use inaccessible elevators.                                                            | *Optional* |                  |  2.10 |
|       inaccessibleCost                                                                     |        `integer`       | The cost to add when traversing an entity which is know to be inaccessible.                                      | *Optional* | `3600`           |  2.2  |
|       onlyConsiderAccessible                                                               |        `boolean`       | Whether to only use this entity if it is explicitly marked as wheelchair accessible.                             | *Optional* | `false`          |  2.2  |
|       unknownCost                                                                          |        `integer`       | The cost to add when traversing an entity with unknown accessibility information.                                | *Optional* | `20`             |  2.2  |
|    [profiles](#transfers_wheelchair_profiles)                                              |       `object[]`       | The wheelchair transfer profiles.                                                                                | *Optional* |                  |  2.10 |
|          [id](#transfers_wheelchair_profiles_0_id)                                         |        `string`        | The id of the profile.                                                                                           | *Required* |                  |  2.10 |
|          [parent](#transfers_wheelchair_profiles_0_parent)                                 |        `string`        | The id of the profile this profile is based on.                                                                  | *Required* |                  |  2.10 |
|          [userPreferences](#transfers_wheelchair_profiles_0_userPreferences)               |        `object`        | Street-search preferences used to discover and cost this profile's paths.                                        | *Optional* |                  |  2.10 |
|             reluctance                                                                     |        `double`        | A multiplier for the cost of traveling in a wheelchair.                                                          | *Optional* | `2.0`            |  2.10 |
|             speed                                                                          |        `double`        | Speed of a traveler in a wheelchair, in meters per second.                                                       | *Optional* | `1.35`           |  2.10 |
|    [stop](#transfers_wheelchair_stop)                                                      |        `object`        | Configuration for when to use inaccessible stops.                                                                | *Optional* |                  |  2.10 |
|       inaccessibleCost                                                                     |        `integer`       | The cost to add when traversing an entity which is know to be inaccessible.                                      | *Optional* | `3600`           |  2.2  |
|       onlyConsiderAccessible                                                               |        `boolean`       | Whether to only use this entity if it is explicitly marked as wheelchair accessible.                             | *Optional* | `true`           |  2.2  |
|       unknownCost                                                                          |        `integer`       | The cost to add when traversing an entity with unknown accessibility information.                                | *Optional* | `600`            |  2.2  |
|    [warmup](#transfers_wheelchair_warmup)                                                  |       `object[]`       | The user preferences to create the transfers for when the server starts.                                         | *Optional* |                  |  2.10 |

<!-- PARAMETERS-TABLE END -->

<!-- PARAMETERS-DETAILS BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

<h3 id="transfers_bicycle">bicycle</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers 

The configuration of the bicycle transfers.

The `profiles`, each a discovery search of transfers with its own preferences, the
`maxDuration`, shared by all the profiles of the type, and the `warmup` entries.


<h3 id="transfers_bicycle_maxDuration">maxDuration</h3>

**Since version:** `2.10` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/bicycle 

The longest bicycle transfer, measured with the user preferences.

How far from a stop the transfers are searched when the transfers are generated. It is
shared by all the profiles of the type, and is not inherited from another type or from a
parent profile. The default is 30 minutes.

There are transfers within the duration between the stops that allow bicycles.


<h3 id="transfers_bicycle_maxDurationAllowAllModes">maxDurationAllowAllModes</h3>

**Since version:** `2.10` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/bicycle 

The longest bicycle transfer between any two stops.

A bicycle can cover a long distance, so this is usually short. It must be shorter than
`maxDuration` and `maxDurationAllowsCar`. If it is not set, there are only transfers
between the stops that allow bicycles or cars, as set by `maxDuration` and
`maxDurationAllowsCar`.


<h3 id="transfers_bicycle_maxDurationAllowsCar">maxDurationAllowsCar</h3>

**Since version:** `2.10` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/bicycle 

The longest bicycle transfer between stops that allow cars.

Used for the transfers between two stops that are visited by trips that allow cars, for
example a car ferry, which a bicycle can use too. It must be longer than
`maxDurationAllowAllModes`.


<h3 id="transfers_bicycle_profiles">profiles</h3>

**Since version:** `2.10` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/bicycle 

The bicycle transfer profiles.

Each profile needs an `id`, unique among all the profiles. The `userPreferences` of the
profiles must be unique. A profile without `userPreferences` gets the default ones. A
profile with a `parent` of the same type inherits the `userPreferences` of the parent, and
the paths it has in common with the parent are stored once.


<h3 id="transfers_bicycle_profiles_0_id">id</h3>

**Since version:** `2.10` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /transfers/bicycle/profiles/[0] 

The id of the profile.

Identifies the profile in the logs and the issues reported. It must be unique among all the
profiles. Any string is allowed.


<h3 id="transfers_bicycle_profiles_0_parent">parent</h3>

**Since version:** `2.10` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /transfers/bicycle/profiles/[0] 

The id of the profile this profile is based on.

If the parent is a profile of the same type, the profile inherits the `userPreferences` of
the parent, and values set on the profile override the inherited ones. The root of a type
gets the values that are not set from the defaults. The parent is
generated first, and the paths of the profile that are equivalent to the paths of the
parent are stored once, see `reuseLimit`. The parent can be of another type, but
nothing is inherited from it, it is only used to store the paths once. The parents must
not form a cycle.


<h3 id="transfers_bicycle_profiles_0_reuseLimit">reuseLimit</h3>

**Since version:** `2.10` ∙ **Type:** `cost-linear-function` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"15s + 0.02 t"`   
**Path:** /transfers/bicycle/profiles/[0] 

How much more the path of the parent may cost, to be used by this profile.

A profile with a `parent` finds its own paths, but if the path of the parent costs about the
same for the profile, it uses the path of the parent instead of storing its own. This saves
memory. The paths are equivalent if the difference in generalized cost is within this limit,
a linear function of the cost `x` of the path of the parent, for example `15s + 0.02x`. The
limit applies to cost only, not to duration. A report is logged when the graph is built.

Optional. A profile inherits the limit of its parent of the same type, not of a parent of
another type. If there is none, the default is `15s + 0.02x`.


<h3 id="transfers_bicycle_profiles_0_userPreferences">userPreferences</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/bicycle/profiles/[0] 

Street-search preferences used to discover and cost this profile's paths.

Optional. If it is not set, the preferences of the parent of the same type are used, or the defaults.

<h3 id="transfers_bicycle_profiles_0_userPreferences_optimize">optimize</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/bicycle/profiles/[0]/userPreferences 

How the route of the bicycle is optimized.

The importance of the `time`, the `slope` and the `safety`. At least one must be set, and
the values that are not set are 0.0. The values are relative to each other, for example
`time: 0.1` is the same as `time: 1.0`, and they are rounded so they sum to 1.0. A `time` of
1.0 is the shortest duration, a `slope` of 1.0 is the flattest streets, and a `safety` of
1.0 is the safest streets.


<h3 id="transfers_bicycle_profiles_0_userPreferences_walk">walk</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/bicycle/profiles/[0]/userPreferences 

The preferences for walking with the bicycle.

Used when the traveler pushes the bicycle, for example on stairs.

<h3 id="transfers_bicycle_profiles_0_userPreferences_walk_reluctance">reluctance</h3>

**Since version:** `2.10` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `5.0`   
**Path:** /transfers/bicycle/profiles/[0]/userPreferences/walk 

A multiplier for the cost of walking with the bicycle.

A higher value avoids walking with the bicycle.

<h3 id="transfers_bicycle_warmup">warmup</h3>

**Since version:** `2.10` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/bicycle 

The user preferences to create the transfers for when the server starts.

Each entry needs an `id`, a `parent` and `userPreferences`. The `id` must be unique
among all the profiles and warmup entries, and the `parent` is the `id` of a profile of
the same type. The transfers for the `userPreferences` are created from the
paths of the parent when the server starts, so the first request with these preferences
does not wait for them. No paths are searched for or stored for a warmup entry. Use
warmup for preferences that are common in the requests, but are not worth a profile of
their own.


<h3 id="transfers_car">car</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers 

The configuration of the car transfers.

The `profiles`, each a discovery search of transfers with its own preferences, the
`maxDuration`, shared by all the profiles of the type, and the `warmup` entries.


<h3 id="transfers_car_maxDuration">maxDuration</h3>

**Since version:** `2.10` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT30M"`   
**Path:** /transfers/car 

The longest car transfer, measured with the user preferences.

How far from a stop the transfers are searched when the transfers are generated. It is
shared by all the profiles of the type, and is not inherited from another type or from a
parent profile. The default is 30 minutes.

There are transfers within the duration between the stops that allow cars only, and no
transfers from or to other stops. A car reaches a lot of stops in a short time, so a car
search from all stops does not work, even for small deployments. If there are too many stops
that allow cars, fix the data, or let the OTP developers know the use case.


<h3 id="transfers_car_profiles">profiles</h3>

**Since version:** `2.10` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/car 

The car transfer profiles.

Each profile needs an `id`, unique among all the profiles. The `userPreferences` of the
profiles must be unique. A profile without `userPreferences` gets the default ones. A
profile with a `parent` of the same type inherits the `userPreferences` of the parent, and
the paths it has in common with the parent are stored once.


<h3 id="transfers_car_profiles_0_id">id</h3>

**Since version:** `2.10` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /transfers/car/profiles/[0] 

The id of the profile.

Identifies the profile in the logs and the issues reported. It must be unique among all the
profiles. Any string is allowed.


<h3 id="transfers_car_profiles_0_userPreferences">userPreferences</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/car/profiles/[0] 

Street-search preferences used to discover and cost this profile's paths.

Optional. If it is not set, the preferences of the parent of the same type are used, or the defaults.

<h3 id="transfers_car_warmup">warmup</h3>

**Since version:** `2.10` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/car 

The user preferences to create the transfers for when the server starts.

Each entry needs an `id`, a `parent` and `userPreferences`. The `id` must be unique
among all the profiles and warmup entries, and the `parent` is the `id` of a profile of
the same type. The transfers for the `userPreferences` are created from the
paths of the parent when the server starts, so the first request with these preferences
does not wait for them. No paths are searched for or stored for a warmup entry. Use
warmup for preferences that are common in the requests, but are not worth a profile of
their own.


<h3 id="transfers_flex">flex</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers 

The configuration of the transfers created for flex routing.

These replace the old `maxTransferDuration` and `transferRequests` of the build config.


<h3 id="transfers_flex_requests">requests</h3>

**Since version:** `2.10` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/flex ∙ **See:** [RouteRequest.md](RouteRequest.md) 

Routing requests to use for pre-calculating stop-to-stop transfers.

It will use the street network if OSM data has already been loaded into the graph. Otherwise it
will use straight-line distance between stops.

If not set, the default behavior is to generate stop-to-stop transfers using the default request
with street mode set to WALK. Only the requests with street mode WALK are used.

**Example**

```JSON
// build-config.json
{
  "transfers": {
    "flex": {
      "requests": [
        { "modes": "WALK" },
        { "modes": "WALK", "wheelchairAccessibility": { "enabled": true }}
      ]
    }
  }
}
```


<h3 id="transfers_walk">walk</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers 

The configuration of the walk transfers.

The `profiles`, each a discovery search of transfers with its own preferences, the
`maxDuration`, shared by all the profiles of the type, and the `warmup` entries.


<h3 id="transfers_walk_maxDuration">maxDuration</h3>

**Since version:** `2.10` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT30M"`   
**Path:** /transfers/walk 

The longest walk transfer, measured with the user preferences.

How far from a stop the transfers are searched when the transfers are generated. It is
shared by all the profiles of the type, and is not inherited from another type or from a
parent profile. The default is 30 minutes.

There are transfers between all stops within the duration.


<h3 id="transfers_walk_profiles">profiles</h3>

**Since version:** `2.10` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/walk 

The walk transfer profiles.

Each profile needs an `id`, unique among all the profiles. The `userPreferences` of the
profiles must be unique. A profile without `userPreferences` gets the default ones. A
profile with a `parent` of the same type inherits the `userPreferences` of the parent, and
the paths it has in common with the parent are stored once.


<h3 id="transfers_walk_profiles_0_id">id</h3>

**Since version:** `2.10` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /transfers/walk/profiles/[0] 

The id of the profile.

Identifies the profile in the logs and the issues reported. It must be unique among all the
profiles. Any string is allowed.


<h3 id="transfers_walk_profiles_0_userPreferences">userPreferences</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/walk/profiles/[0] 

Street-search preferences used to discover and cost this profile's paths.

Optional. If it is not set, the preferences of the parent of the same type are used, or the defaults.

<h3 id="transfers_walk_profiles_0_userPreferences_safetyFactor">safetyFactor</h3>

**Since version:** `2.10` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `1.0`   
**Path:** /transfers/walk/profiles/[0]/userPreferences 

How much the safety of the streets is considered.

A value between 0.0 and 1.0. If it is 0.0, the safety is ignored. If it is 1.0, the safety
factor of the street is applied in whole as a multiplier on the cost, on top of the
reluctance.


<h3 id="transfers_walk_profiles_1_id">id</h3>

**Since version:** `2.10` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /transfers/walk/profiles/[1] 

The id of the profile.

Identifies the profile in the logs and the issues reported. It must be unique among all the
profiles. Any string is allowed.


<h3 id="transfers_walk_profiles_1_parent">parent</h3>

**Since version:** `2.10` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /transfers/walk/profiles/[1] 

The id of the profile this profile is based on.

If the parent is a profile of the same type, the profile inherits the `userPreferences` of
the parent, and values set on the profile override the inherited ones. The root of a type
gets the values that are not set from the defaults. The parent is
generated first, and the paths of the profile that are equivalent to the paths of the
parent are stored once, see `reuseLimit`. The parent can be of another type, but
nothing is inherited from it, it is only used to store the paths once. The parents must
not form a cycle.


<h3 id="transfers_walk_profiles_1_reuseLimit">reuseLimit</h3>

**Since version:** `2.10` ∙ **Type:** `cost-linear-function` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"15s + 0.02 t"`   
**Path:** /transfers/walk/profiles/[1] 

How much more the path of the parent may cost, to be used by this profile.

A profile with a `parent` finds its own paths, but if the path of the parent costs about the
same for the profile, it uses the path of the parent instead of storing its own. This saves
memory. The paths are equivalent if the difference in generalized cost is within this limit,
a linear function of the cost `x` of the path of the parent, for example `15s + 0.02x`. The
limit applies to cost only, not to duration. A report is logged when the graph is built.

Optional. A profile inherits the limit of its parent of the same type, not of a parent of
another type. If there is none, the default is `15s + 0.02x`.


<h3 id="transfers_walk_profiles_1_userPreferences">userPreferences</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/walk/profiles/[1] 

Street-search preferences used to discover and cost this profile's paths.

Optional. If it is not set, the preferences of the parent of the same type are used, or the defaults.

<h3 id="transfers_walk_profiles_1_userPreferences_safetyFactor">safetyFactor</h3>

**Since version:** `2.10` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.5`   
**Path:** /transfers/walk/profiles/[1]/userPreferences 

How much the safety of the streets is considered.

A value between 0.0 and 1.0. If it is 0.0, the safety is ignored. If it is 1.0, the safety
factor of the street is applied in whole as a multiplier on the cost, on top of the
reluctance.


<h3 id="transfers_walk_warmup">warmup</h3>

**Since version:** `2.10` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/walk 

The user preferences to create the transfers for when the server starts.

Each entry needs an `id`, a `parent` and `userPreferences`. The `id` must be unique
among all the profiles and warmup entries, and the `parent` is the `id` of a profile of
the same type. The transfers for the `userPreferences` are created from the
paths of the parent when the server starts, so the first request with these preferences
does not wait for them. No paths are searched for or stored for a warmup entry. Use
warmup for preferences that are common in the requests, but are not worth a profile of
their own.


<h3 id="transfers_walk_warmup_0_id">id</h3>

**Since version:** `2.10` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /transfers/walk/warmup/[0] 

The id of the warmup entry.

Identifies the entry in the logs. It must be unique among all the profiles.

<h3 id="transfers_walk_warmup_0_userPreferences">userPreferences</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/walk/warmup/[0] 

The user preferences to warm up.

Unset values are the values of the parent.

<h3 id="transfers_walk_warmup_0_userPreferences_safetyFactor">safetyFactor</h3>

**Since version:** `2.10` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.5`   
**Path:** /transfers/walk/warmup/[0]/userPreferences 

How much the safety of the streets is considered.

A value between 0.0 and 1.0. If it is 0.0, the safety is ignored. If it is 1.0, the safety
factor of the street is applied in whole as a multiplier on the cost, on top of the
reluctance.


<h3 id="transfers_wheelchair">wheelchair</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers 

The configuration of the wheelchair transfers.

The `profiles`, each a discovery search of transfers with its own preferences, the
`maxDuration`, shared by all the profiles of the type, and the `warmup` entries.


<h3 id="transfers_wheelchair_maxDuration">maxDuration</h3>

**Since version:** `2.10` ∙ **Type:** `duration` ∙ **Cardinality:** `Optional` ∙ **Default value:** `"PT30M"`   
**Path:** /transfers/wheelchair 

The longest wheelchair transfer, measured with the user preferences.

How far from a stop the transfers are searched when the transfers are generated. It is
shared by all the profiles of the type, and is not inherited from another type or from a
parent profile. The default is 30 minutes.

There are transfers between all stops within the duration.


<h3 id="transfers_wheelchair_maxSlope">maxSlope</h3>

**Since version:** `2.10` ∙ **Type:** `double` ∙ **Cardinality:** `Optional` ∙ **Default value:** `0.083`   
**Path:** /transfers/wheelchair 

The maximum slope as a fraction of 1.

9 percent would be `0.09`

<h3 id="transfers_wheelchair_profiles">profiles</h3>

**Since version:** `2.10` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/wheelchair 

The wheelchair transfer profiles.

Each profile needs an `id`, unique among all the profiles. The `userPreferences` of the
profiles must be unique. A profile without `userPreferences` gets the default ones. A
profile with a `parent` of the same type inherits the `userPreferences` of the parent, and
the paths it has in common with the parent are stored once.


<h3 id="transfers_wheelchair_profiles_0_id">id</h3>

**Since version:** `2.10` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /transfers/wheelchair/profiles/[0] 

The id of the profile.

Identifies the profile in the logs and the issues reported. It must be unique among all the
profiles. Any string is allowed.


<h3 id="transfers_wheelchair_profiles_0_parent">parent</h3>

**Since version:** `2.10` ∙ **Type:** `string` ∙ **Cardinality:** `Required`   
**Path:** /transfers/wheelchair/profiles/[0] 

The id of the profile this profile is based on.

If the parent is a profile of the same type, the profile inherits the `userPreferences` of
the parent, and values set on the profile override the inherited ones. The root of a type
gets the values that are not set from the defaults. The parent is
generated first, and the paths of the profile that are equivalent to the paths of the
parent are stored once, see `reuseLimit`. The parent can be of another type, but
nothing is inherited from it, it is only used to store the paths once. The parents must
not form a cycle.


<h3 id="transfers_wheelchair_profiles_0_userPreferences">userPreferences</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/wheelchair/profiles/[0] 

Street-search preferences used to discover and cost this profile's paths.

Optional. If it is not set, the preferences of the parent of the same type are used, or the defaults.

<h3 id="transfers_wheelchair_stop">stop</h3>

**Since version:** `2.10` ∙ **Type:** `object` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/wheelchair 

Configuration for when to use inaccessible stops.

The default is to only use the stops that are marked as wheelchair accessible. Many
deployments have little accessibility information, and get almost no transfers with
this default. Set `unknownCost` and `inaccessibleCost` to use the other stops with a
cost.


<h3 id="transfers_wheelchair_warmup">warmup</h3>

**Since version:** `2.10` ∙ **Type:** `object[]` ∙ **Cardinality:** `Optional`   
**Path:** /transfers/wheelchair 

The user preferences to create the transfers for when the server starts.

Each entry needs an `id`, a `parent` and `userPreferences`. The `id` must be unique
among all the profiles and warmup entries, and the `parent` is the `id` of a profile of
the same type. The transfers for the `userPreferences` are created from the
paths of the parent when the server starts, so the first request with these preferences
does not wait for them. No paths are searched for or stored for a warmup entry. Use
warmup for preferences that are common in the requests, but are not worth a profile of
their own.



<!-- PARAMETERS-DETAILS END -->

### Complete example

This example sets every parameter of the `transfers` section:

<!-- JSON-EXAMPLE BEGIN -->
<!-- NOTE! This section is auto-generated. Do not change, change doc in code instead. -->

```JSON
// build-config.json
{
  "transfers" : {
    "walk" : {
      "profiles" : [
        {
          "id" : "walk",
          "userPreferences" : {
            "speed" : 1.4,
            "reluctance" : 2.5,
            "safetyFactor" : 0.5
          }
        },
        {
          "id" : "slow-walk",
          "parent" : "walk",
          "reuseLimit" : "30s + 0.05x",
          "userPreferences" : {
            "speed" : 0.9,
            "reluctance" : 4.0
          }
        }
      ],
      "maxDuration" : "40m",
      "stairsReluctance" : 3.0,
      "stairsTimeFactor" : 2.5,
      "escalator" : {
        "reluctance" : 2.0,
        "speed" : 0.5
      },
      "warmup" : [
        {
          "id" : "avoid-walking",
          "parent" : "walk",
          "userPreferences" : {
            "speed" : 1.15,
            "reluctance" : 10.0
          }
        }
      ]
    },
    "wheelchair" : {
      "profiles" : [
        {
          "id" : "wheelchair",
          "parent" : "walk",
          "userPreferences" : {
            "speed" : 1.0,
            "reluctance" : 3.0
          }
        }
      ],
      "maxDuration" : "20m",
      "stop" : {
        "onlyConsiderAccessible" : false,
        "unknownCost" : 900,
        "inaccessibleCost" : 7200
      },
      "elevator" : {
        "onlyConsiderAccessible" : false,
        "unknownCost" : 300,
        "inaccessibleCost" : 1800
      },
      "inaccessibleStreetReluctance" : 30.0,
      "maxSlope" : 0.05,
      "slopeExceededReluctance" : 2.0,
      "stairsReluctance" : 150.0
    },
    "bicycle" : {
      "profiles" : [
        {
          "id" : "bicycle",
          "parent" : "walk",
          "reuseLimit" : "15s + 0.05x",
          "userPreferences" : {
            "speed" : 6.0,
            "reluctance" : 1.5,
            "optimize" : {
              "time" : 0.5,
              "slope" : 0.2,
              "safety" : 0.3
            },
            "walk" : {
              "speed" : 1.2,
              "reluctance" : 6.0
            }
          }
        }
      ],
      "maxDuration" : "25m",
      "maxDurationAllowAllModes" : "10m",
      "maxDurationAllowsCar" : "45m",
      "walking" : {
        "stairsReluctance" : 12.0,
        "mountDismountTime" : "20s",
        "mountDismountCost" : 30
      }
    },
    "car" : {
      "profiles" : [
        {
          "id" : "car",
          "userPreferences" : {
            "reluctance" : 3.0
          }
        }
      ],
      "maxDuration" : "2h"
    },
    "flex" : {
      "maxDuration" : "45m",
      "requests" : [
        {
          "modes" : "WALK"
        }
      ]
    },
    "via" : {
      "maxDuration" : "20m"
    }
  }
}
```

<!-- JSON-EXAMPLE END -->
