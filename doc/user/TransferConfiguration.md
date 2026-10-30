# Transfer configuration

Transfers are the walks between two stops that Raptor uses to change from one trip to another. OTP
calculates them when the graph is built, and stores them in the graph. This page describes the
`transfers` section of `build-config.json`, which controls the *regular* transfers between stops.
Flex transfers are still configured with [`transferRequests`](BuildConfiguration.md).

!!! note

    The `transfers` configuration is new (since version 2.10) and under development. Only walk,
    wheelchair, bicycle and car transfers can be configured so far. See
    [What is not supported yet](#what-is-not-supported-yet).

## Concepts

**Profile.** A profile is a search for the transfers of one kind of traveler, with its own
preferences, for example a slow walker. The transfers of a profile are stored once in the graph.
When a routing request arrives, OTP picks the profile with the user preferences closest to the
request, and re-calculates the duration and the cost of the stored transfers with the preferences of
the request. The result is cached, so the next request with the same preferences is fast.

**Profile type.** Each profile has a type: `walk`, `wheelchair`, `bicycle` and `car`, and later
`scooter`. The configuration of a type is a block named after the type. Everything that is shared by
the profiles of a type is configured once per type, like the max durations. A routing request is
matched to a type by its transfer street mode, and for walking by the wheelchair flag: a request
for a wheelchair accessible trip uses the `wheelchair` profiles, any other walking request uses the
`walk` profiles, a request that transfers by bike uses the `bicycle` profiles, and a request that
transfers by car uses the `car` profiles. A request that has no profile of its type is not
accepted, so a `wheelchair` profile is needed to route with wheelchair accessibility, a `bicycle`
profile to transfer by bike, and a `car` profile to transfer by car.

**Id.** Each profile has an `id`, which must be unique among all the profiles. OTP uses it in the
logs and in the issues it reports, for example `Created 101785 regular transfers for profile
'walk'`. Any string is allowed.

**User preferences.** The preferences of a traveler, like walking speed, that are used to search
for and cost the transfers. A profile without `userPreferences` gets them from its `routingRequest`,
or from the default request if it has none. The user preferences of the profiles must be unique.

**Default request.** The default request is the request with the default values of the code. It is
not the `routingDefaults` of `router-config.json`, since the router configuration is not available
when the graph is built.

**Warmup.** The transfers for the preferences of a routing request are created the first time a
request needs them, and cached. OTP creates them for the preferences of every profile when the
server starts. A warmup entry does the same for other preferences that are common in the requests,
but are not worth a profile of their own. See [Warmup](#warmup).

**Parent.** A profile can be based on another profile, its `parent`. If the parent is of the same
type, the profile inherits the `routingRequest` and the `userPreferences` of the parent, and only
has to set what is different. The transfers the profile and its parent have in common are stored
once, which saves memory. See [Parent and deduplication](#parent-and-deduplication).

## Example

```json
// build-config.json
{
  "transfers": {
    "deduplicationLimit": "0.005x + 30s",
    "walk": {
      "profiles": [
        {
          "id": "walk",
          "userPreferences": { "speed": 1.3, "reluctance": 2.0 }
        },
        {
          "id": "slow-walk",
          "parent": "walk",
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
request and a max duration of 30 minutes is used. Each profile type with profiles must set its
`maxDuration`, there is no default.

## Parameters

| Parameter                     | Type       | Description                                                                         | Default           |
|-------------------------------|------------|-------------------------------------------------------------------------------------|-------------------|
| `deduplicationLimit`          | `string`   | How much more the path of a parent may cost to be used by a child. See below.       | `"0.005x + 30s"`  |
| `walk.profiles`               | `object[]` | The walk profiles. If none is configured, one with the id `walk` is used.           |                   |
| &nbsp;&nbsp;`id`              | `string`   | **Required.** The id of the profile, unique among all profiles.                     |                   |
| &nbsp;&nbsp;`parent`          | `string`   | The `id` of the profile this profile is based on. See below.                        |                   |
| &nbsp;&nbsp;`userPreferences` | `object`   | The preferences the profile is searched with. See below.                            |                   |
| &nbsp;&nbsp;`routingRequest`  | `object`   | A [routing request](RouteRequest.md) the search is based on. See below.             |                   |
| `walk.maxDuration`            | `duration` | **Required** if there are walk profiles. How far from a stop the walk transfers are searched. See below. |                   |
| `walk.warmup`                 | `object[]` | Preferences to create transfers for when the server starts. See below.              |                   |
| &nbsp;&nbsp;`id`              | `string`   | **Required.** The id of the entry, unique among all profiles and warmup entries.    |                   |
| &nbsp;&nbsp;`parent`          | `string`   | **Required.** The `id` of a profile of the same type the transfers are created from.|                   |
| &nbsp;&nbsp;`userPreferences` | `object`   | **Required.** The preferences to warm up. Unset values are the ones of the parent.  |                   |

The `wheelchair`, `bicycle` and `car` blocks have the same parameters as the `walk` block, for
example `wheelchair.profiles`, `wheelchair.maxDuration` and `car.warmup`. The `bicycle` block has
two more parameters, see [Max durations](#max-durations).

### User preferences

| Parameter     | Type     | Description                                                                         | Default  |
|---------------|----------|-------------------------------------------------------------------------------------|----------|
| `speed`       | `double` | The speed in meters per second. Not for `car`, it drives the speed of the streets.  | `1.35`   |
| `reluctance`  | `double` | A multiplier for the cost of the transfer. A higher value avoids the transfer.      | `2.0`    |
| `safetyFactor` | `double` | `walk` and `wheelchair` only. How much the safety of the streets is considered, from 0.0 (ignored) to 1.0. | `1.0` |

The `walk`, `wheelchair` and `bicycle` types have `speed` and `reluctance`, and the `car` type only
has `reluctance`. The defaults of `walk` and `wheelchair` are the same, and the default `speed` of
`bicycle` is `5.0`. The street model decides which streets and elevators a wheelchair can use, and
where a bicycle or a car can go. The `bicycle` type also has the preferences for walking with the
bicycle, when it is pushed, and how the route is optimized:

| Parameter                | Type     | Description                                                      | Default |
|--------------------------|----------|------------------------------------------------------------------|---------|
| `bicycle.optimize`       | `object` | How the route is optimized. At least one value is required.      |         |
| &nbsp;&nbsp;`time`       | `double` | The importance of the time. `1.0` is the shortest duration.      | `0.0`   |
| &nbsp;&nbsp;`slope`      | `double` | The importance of the slope. `1.0` is the flattest streets.      | `0.0`   |
| &nbsp;&nbsp;`safety`     | `double` | The importance of the safety. `1.0` is the safest streets.       | `0.0`   |
| `bicycle.walk`           | `object` | The preferences for walking with the bicycle.                    |         |
| &nbsp;&nbsp;`speed`      | `double` | The speed in meters per second.                                  | `1.35`  |
| &nbsp;&nbsp;`reluctance` | `double` | A multiplier for the cost of walking with the bicycle.           | `5.0`   |

The `optimize` values are relative to each other, for example `time: 0.1` is the same as
`time: 1.0`. They are rounded so they sum to 1.0. If `optimize` is set, the values that are not set
are `0.0`, so `optimize: { time: 0.1 }` is `time: 1.0`, `slope: 0.0` and `safety: 0.0`. If it is
not set in a profile, the profile inherits it from a parent of the same type, and a root gets it
from the request. The default is the safest streets (`safety: 1.0`).

The stairs reluctance is not a user preference, since no API takes it as an input of a routing
search. It is a `routingRequest` parameter of the profile, for example `walk.stairsReluctance`,
`wheelchair.stairsReluctance` or `bicycle.walking.stairsReluctance`. It is the value of the
configuration of the profile, not the one in `routingDefaults` of the `router-config.json`, so
copy it to the `routingRequest` of the profiles if it is not the default.

### Routing request

`routingRequest` takes the same parameters as the [route request](RouteRequest.md), and is the base
the transfer search is made with. It is there so existing behavior can be kept. The values in
`userPreferences` override the equivalent values of the request. The goal is to move all the
relevant parameters to `userPreferences`, and drop `routingRequest` in the future.

### Parent and deduplication

A profile can name another profile as its `parent` with the `id` of the parent. The `parent` must be
a profile of the same configuration, and the parents must not form a cycle. The profiles can be
declared in any order. A parent can be of another type, for example a `wheelchair` profile with the
`walk` profile as its parent. The paths are then stored once, but no values are inherited.

**Inheritance.** Values are only inherited within a profile type. If the parent is a profile of the
same type, the profile inherits the `routingRequest` and the `userPreferences` of the parent.
Everything that is set on the profile overrides the inherited value, and everything that is not set
is the value of the parent. A profile with a parent and without `userPreferences` of its own would
get the same preferences as the parent, and is rejected, since the profiles must be unique. A
profile without a parent of the same type is the root of its type, and the values it does not set
are the ones of the request, see [Routing request](#routing-request). A parent of another type is
not inherited from, it is only used for the deduplication below.

**Deduplication.** Each profile still searches for its own transfers, but the transfers that are
the same, or almost the same, as the transfers of the parent are only stored once. For each pair of
stops, OTP calculates the cost of the path of the parent with the preferences of the profile. If it
is close enough to the cost of the best path the profile found itself, the profile uses the path of
the parent. This makes the graph smaller, and the graph file too.

"Close enough" is set with `deduplicationLimit`. It is a linear function of the cost `x` of the
path of the parent, for example `0.005x + 30s`: a path that costs up to 0.5% and 30 seconds more
than the best path is used. The terms can be written in either order, and one of them can be left
out. A limit of `0s` only shares the paths with the same cost. The limit applies to the generalized
cost only, not to the duration. The graph build logs how many of the paths of each profile are the
paths of its parent:

```
Profile 'slow-walk': 84788 of 85142 paths (99.6%) are the paths of the parent 'walk', they are stored once.
```

Use a parent for profiles that are close to another profile, like a slightly slower walker. A
profile that is very different from its parent, like one that avoids stairs, shares fewer paths.
For each transfer of a profile, the graph build re-costs the path of the parent once more.

### Warmup

When a routing request arrives, OTP creates the transfers for the preferences of the request, by
re-calculating the duration and the cost of the stored transfers of a profile. This takes a moment,
and it is done by the first request with these preferences. The result is cached, so the following
requests are fast.

When the server starts, before the first request, OTP creates the transfers for the preferences of
every profile. A `warmup` entry creates them for some more preferences. Use it for the preferences
that are common in the requests, for example the default preferences of a client, but that are not
different enough from a profile to be worth a profile of their own. A warmup entry needs:

- an `id`, unique among all profiles and warmup entries. It is used in the logs.
- a `parent`, the `id` of a profile. The transfers are created from the stored transfers of the
  parent of the same type, and the entry inherits the preferences of the parent. A warmup entry can
  not be a parent.
- `userPreferences`. They must be different from the preferences of every profile and every other
  warmup entry.

No transfers are searched for or stored for a warmup entry, so it adds nothing to the graph. A
request with preferences that are closest to a warmup entry uses the stored transfers of the parent
of the entry, like a request that is closest to the parent.

The server start waits for the transfers to be created. The time of each profile and each entry is
logged:

```
Warmed up the Routing Transfer Cache for the profile 'walk' in 150 ms.
Warmed up the Routing Transfer Cache for 'avoid-walking' from the paths of 'walk' in 170 ms.
```

The transfers for other preferences are created by the first request that uses them. The cache holds
a limited number of preference sets, and the least recently used are dropped first, so a large
number of warmup entries and profiles may drop the first ones.

### Max durations

The max duration is how far from a stop the transfers are searched when the graph is built, measured
with the user preferences of the profile. A longer duration finds more transfers and makes the graph
larger and slower to build. There is one `maxDuration` for each profile type, and it is shared by
all the profiles of the type. It is required for each type with profiles, and it is not inherited
from another type or from a parent profile. There is no default, except the 30 minutes of the
implicit `walk` profile when the `transfers` section is left out:

```json
"walk": { "maxDuration": "40m" }
```

`maxDuration` is the duration between the stops the mode can use:

| Type                  | `maxDuration` is the max duration between          |
|-----------------------|----------------------------------------------------|
| `walk`, `wheelchair`  | all stops                                          |
| `bicycle`             | stops that allow bicycles                          |
| `car`                 | stops that allow cars                              |

The wheelchair is treated as walking for now. A stop allows bicycles or cars if a trip that allows
them visits the stop, for example a ferry. Both stops of a transfer must allow the mode.

A bicycle can cover a much longer distance than a walker, so the `bicycle` type has two more
parameters, and all three are independent of each other. The longest limit that applies to a
transfer is used:

| Parameter                            | Type       | Description                                                       |
|--------------------------------------|------------|-------------------------------------------------------------------|
| `bicycle.maxDurationAllowAllModes`   | `duration` | The max duration between all stops. Shorter than the two others.  |
| `bicycle.maxDuration`                | `duration` | The max duration between stops that allow bicycles.               |
| `bicycle.maxDurationAllowsCar`       | `duration` | The max duration between stops that allow cars.                   |

At least one of them is required. `maxDurationAllowAllModes` must be shorter than `maxDuration` and
`maxDurationAllowsCar`, if they are set. If `maxDurationAllowAllModes` is left out, there are only
transfers between the stops that allow bicycles or cars:

```json
"bicycle": {
  "maxDurationAllowAllModes": "20m",
  "maxDuration": "45m",
  "maxDurationAllowsCar": "2h"
}
```

The `car` type only has transfers between stops that allow cars, and none from or to other stops.
No search is made from the other stops, which makes the graph faster to build:

```json
"car": { "maxDuration": "3h" }
```

A car reaches a lot of stops in a short time, so a car search from all stops does not work, even
for small deployments. If there are too many stops that allow cars, fix the data, or let the OTP
developers know the use case.

The max duration only limits the search. A stored transfer is never dropped when it is
re-calculated for a routing request, even if the request walks slower than the profile.

Stops of a station that does not allow transfers have no transfers from or to them.

!!! note

    The old `maxTransferDuration` is not used by the `transfers` configuration, use `maxDuration`.
    The old per mode `transferParametersForMode` is replaced by the profile types:
    `carsAllowedStopMaxTransferDuration` of the `CAR` mode is the `car` type, and the
    `bicycle` type has `maxDurationAllowsCar` and `maxDuration` (the old
    `bikesAllowedStopMaxTransferDuration`). The `disableDefaultTransfers` is replaced by leaving out
    `bicycle.maxDurationAllowAllModes`. The wheelchair no longer shares the walk max duration, set
    `wheelchair.maxDuration` too.

## What is not supported yet

The profile type `scooter` is not read yet. It is reported as an unexpected parameter.

## Compatibility with older versions

Older versions of OTP do not know the `transfers` section. They report it as an unexpected
parameter and otherwise ignore it, so the same `build-config.json` can be used with both. Only if
OTP is started with `--abortOnUnknownConfig` does an unknown parameter stop the start.

## Related issues

- [#7998](https://github.com/opentripplanner/OpenTripPlanner/issues/7998) - Simplify regular
  transfer configuration
- [#8061](https://github.com/opentripplanner/OpenTripPlanner/issues/8061) - Transfers overview and
  TODO list
