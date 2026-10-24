# Transfer configuration

Transfers are the walks between two stops that Raptor uses to change from one trip to another. OTP
calculates them when the graph is built, and stores them in the graph. This page describes the
`transfers` section of `build-config.json`, which controls the *regular* transfers between stops.
Flex transfers are still configured with [`transferRequests`](BuildConfiguration.md).

!!! note

    The `transfers` configuration is new (since version 2.10) and under development. Only walk
    transfers can be configured so far. See
    [What is not supported yet](#what-is-not-supported-yet).

## Concepts

**Profile.** A profile is a search for the transfers of one kind of traveler, with its own
preferences, for example a slow walker. The transfers of a profile are stored once in the graph.
When a routing request arrives, OTP picks the profile with the user preferences closest to the
request, and re-calculates the duration and the cost of the stored transfers with the preferences of
the request. The result is cached, so the next request with the same preferences is fast.

**Profile type.** Each profile has a type: `walk`, and later `wheelchair`, `bicycle`, `scooter`
and `car`. The configuration of a type is a block named after the type. Everything that is shared by
the profiles of a type is configured once per type, like the max durations.

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
request needs them, and cached. A warmup entry lets OTP create them when the server starts, for
preferences that are common in the requests but are not worth a profile of their own. See
[Warmup](#warmup).

**Parent.** A profile can be based on another profile, its `parent`. The profile inherits the
`routingRequest` and the `userPreferences` of the parent, and only has to set what is different. The
transfers the profile and its parent have in common are stored once, which saves memory. See
[Parent and deduplication](#parent-and-deduplication).

## Example

```json
// build-config.json
{
  "transfers": {
    "defaultMaxDuration": "30m",
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
          "userPreferences": { "speed": 0.9, "reluctance": 4.0, "stairs": "DISCOURAGED" }
        }
      ],
      "maxDurations": [{ "maxDuration": "40m" }],
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
request and a max duration of 30 minutes is used.

## Parameters

| Parameter                     | Type       | Description                                                                         | Default           |
|-------------------------------|------------|-------------------------------------------------------------------------------------|-------------------|
| `defaultMaxDuration`          | `duration` | The max duration of all profile types that do not have `maxDurations`.              | `"30m"`           |
| `deduplicationLimit`          | `string`   | How much more the path of a parent may cost to be used by a child. See below.       | `"0.005x + 30s"`  |
| `walk.profiles`               | `object[]` | The walk profiles. If none is configured, one with the id `walk` is used.           |                   |
| &nbsp;&nbsp;`id`              | `string`   | **Required.** The id of the profile, unique among all profiles.                     |                   |
| &nbsp;&nbsp;`parent`          | `string`   | The `id` of the profile this profile is based on. See below.                        |                   |
| &nbsp;&nbsp;`userPreferences` | `object`   | The preferences the profile is searched with. See below.                            |                   |
| &nbsp;&nbsp;`routingRequest`  | `object`   | A [routing request](RouteRequest.md) the search is based on. See below.             |                   |
| `walk.maxDurations`           | `object[]` | The max duration of the walk transfers. One entry. See below.                       |                   |
| &nbsp;&nbsp;`maxDuration`     | `duration` | **Required** in the entry. How far from a stop the transfers are searched.          |                   |
| `walk.warmup`                 | `object[]` | Preferences to create transfers for when the server starts. See below.              |                   |
| &nbsp;&nbsp;`id`              | `string`   | **Required.** The id of the entry, unique among all profiles and warmup entries.    |                   |
| &nbsp;&nbsp;`parent`          | `string`   | **Required.** The `id` of the profile the transfers are created from.               |                   |
| &nbsp;&nbsp;`userPreferences` | `object`   | **Required.** The preferences to warm up. Unset values are the ones of the parent.  |                   |

### User preferences

| Parameter     | Type     | Description                                                                         | Default  |
|---------------|----------|-------------------------------------------------------------------------------------|----------|
| `speed`       | `double` | The walking speed in meters per second.                                             | `1.35`   |
| `reluctance`  | `double` | A multiplier for the cost of walking. A higher value avoids walking.                | `2.0`    |
| `stairs`      | `enum`   | `ALLOWED`, `DISCOURAGED` or `FORBIDDEN`.                                            | `ALLOWED`|

### Routing request

`routingRequest` takes the same parameters as the [route request](RouteRequest.md), and is the base
the transfer search is made with. It is there so existing behavior can be kept. The values in
`userPreferences` override the equivalent values of the request. The goal is to move all the
relevant parameters to `userPreferences`, and drop `routingRequest` in the future.

### Parent and deduplication

A profile can name another profile as its `parent` with the `id` of the parent. The `parent` must be
a profile of the same configuration, and the parents must not form a cycle. The profiles can be
declared in any order.

**Inheritance.** The profile inherits the `routingRequest` and the `userPreferences` of the parent.
Everything that is set on the profile overrides the inherited value, and everything that is not set
is the value of the parent. A profile with a parent and without `userPreferences` of its own would
get the same preferences as the parent, and is rejected, since the profiles must be unique.

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

A `warmup` entry creates the transfers for some preferences when the server starts, before the first
request. Use it for the preferences that are common in the requests, for example the default
preferences of a client, but that are not different enough from a profile to be worth a profile of
their own. A warmup entry needs:

- an `id`, unique among all profiles and warmup entries. It is used in the logs.
- a `parent`, the `id` of a profile. The transfers are created from the stored transfers of the
  parent, and the entry inherits the preferences of the parent. A warmup entry can not be a parent.
- `userPreferences`. They must be different from the preferences of every profile and every other
  warmup entry.

No transfers are searched for or stored for a warmup entry, so it adds nothing to the graph. A
request with preferences that are closest to a warmup entry uses the stored transfers of the parent
of the entry, like a request that is closest to the parent.

The server start waits for the transfers to be created. The time of each entry is logged:

```
Warmed up the Routing Transfer Cache for 'avoid-walking' from the paths of 'walk' in 170 ms.
```

Only the entries in `warmup` are created at start. The transfers for the preferences of a profile
are created by the first request that uses them, like any other preferences. The cache holds a
limited number of preference sets, and the least recently used are dropped first.

### Max durations

The max duration is how far from a stop the transfers are searched when the graph is built, measured
with the user preferences of the profile. A longer duration finds more transfers and makes the graph
larger and slower to build. There is one configuration for each profile type, and it is shared by all
the profiles of the type. It replaces `defaultMaxDuration` for the type:

```json
"walk": { "maxDurations": [{ "maxDuration": "40m" }] }
```

The max duration only limits the search. A stored transfer is never dropped when it is
re-calculated for a routing request, even if the request walks slower than the profile.

!!! note

    The old `maxTransferDuration` is not used by the `transfers` configuration. Use
    `defaultMaxDuration` or `maxDurations`.

## What is not supported yet

These parameters are reserved for later. OTP stops with an error if they are used:

- `allowedModes` in `maxDurations`, to give stops that car or bicycle can use a longer duration.

The profile types `wheelchair`, `bicycle`, `scooter` and `car` are not read yet. They are reported
as unexpected parameters.

## Compatibility with older versions

Older versions of OTP do not know the `transfers` section. They report it as an unexpected
parameter and otherwise ignore it, so the same `build-config.json` can be used with both. Only if
OTP is started with `--abortOnUnknownConfig` does an unknown parameter stop the start.

## Related issues

- [#7998](https://github.com/opentripplanner/OpenTripPlanner/issues/7998) - Simplify regular
  transfer configuration
- [#8061](https://github.com/opentripplanner/OpenTripPlanner/issues/8061) - Transfers overview and
  TODO list
