# Transfer configuration

Transfers are the walks between two stops that Raptor uses to change from one trip to another. OTP
calculates them when the graph is built, and stores them in the graph. This page describes the
`transfers` section of `build-config.json`, which controls the *regular* transfers between stops.
Flex transfers are still configured with [`transferRequests`](BuildConfiguration.md).

!!! note

    The `transfers` configuration is new and under development. Only walk transfers can be
    configured so far. See [What is not supported yet](#what-is-not-supported-yet).

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

## Example

```json
// build-config.json
{
  "transfers": {
    "defaultMaxDuration": "30m",
    "walk": {
      "profiles": [
        {
          "id": "walk",
          "userPreferences": { "speed": 1.3, "reluctance": 2.0 }
        },
        {
          "id": "slow-walk",
          "userPreferences": { "speed": 0.9, "reluctance": 4.0, "stairs": "DISCOURAGED" }
        }
      ],
      "maxDurations": [{ "maxDuration": "40m" }]
    }
  }
}
```

If the `transfers` section is left out, a single `walk` profile with the preferences of the default
request and a max duration of 30 minutes is used.

## Parameters

| Parameter                              | Type       | Description                                                                     | Default |
|----------------------------------------|------------|---------------------------------------------------------------------------------|---------|
| `defaultMaxDuration`                   | `duration` | The max duration of all profile types that do not have `maxDurations`.          | `"30m"` |
| `walk.profiles`                        | `object[]` | The walk profiles. If none is configured, one with the id `walk` is used.       |         |
| &nbsp;&nbsp;`id`                       | `string`   | **Required.** The id of the profile, unique among all profiles.                 |         |
| &nbsp;&nbsp;`userPreferences`          | `object`   | The preferences the profile is searched with. See below.                        |         |
| &nbsp;&nbsp;`routingRequest`           | `object`   | A [routing request](RouteRequest.md) the search is based on. See below.         |         |
| `walk.maxDurations`                    | `object[]` | The max duration of the walk transfers. One entry. See below.                   |         |

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

- `parent` on a profile, to reuse the stored paths of another profile.
- `warmup`, to fill the cache of transfers with preferences that are common.
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
