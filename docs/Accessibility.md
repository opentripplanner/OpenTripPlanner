# Accessibility

## Preamble

GTFS and Netex define accessibility primarily in terms of binary access for wheelchair users: it's
either on or off. Whilst it is the desire of the OTP developers to broaden the scope of
accessibility the lack of data limits us to use this definition in the implementation and in this
document.

## Unknown data

Many agencies have the same problem: data on wheelchair-accessibility is, if it exists at all,
patchy. If you only included trips and stops that are explicitly set to be wheelchair-accessible
rather than unknown, it would be hard to get any result at all. For this reason OTP allows you to
configure which sort of unknown information should be taken into account.

## Configuration

If you want to allow trips and stops of unknown wheelchair-accessibility then add the following to
`router-config.json`:

```json
{
  "routingDefaults": {
    "wheelchairAccessibility": {
      "trip": {
        "onlyConsiderAccessible": false,
        "unknownCost": 600,
        "inaccessibleCost": 3600
      },
      "stop": {
        "onlyConsiderAccessible": false,
        "unknownCost": 600,
        "inaccessibleCost": 3600
      },
      "inaccessibleStreetReluctance": 25,
      "maxSlope": 0.08333,
      "slopeExceededReluctance": 1.1,
      "stairsReluctance": 25
    }
  },
  "updaters": []
}
```

The parameters for `stop`, `trip` and `elevator` mean the following:

| name                     |                                                                                                | default |
|--------------------------|------------------------------------------------------------------------------------------------|---------|
| `onlyConsiderAccessible` | Whether to include unknown accessibility and inaccessible stops/trips/elevators in the search. | `false` |
| `unknownCost`            | The cost to add if an entity has unknown wheelchair accessibility                              | 600     |
| `inaccessibleCost`       | The cost to add if an entity is known to be inaccessible                                       | 3600    |

**Note**: Unless your accessibility data coverage is complete you will receive much better results
by setting `onlyConsiderAccessible=false`, because otherwise you receive barely any results.

Other parameters are:

- `inaccessibleStreetReluctance`: if a street is marked as wheelchair-inaccessible this is the
  penalty that is applied for wheelchair users. This should be quite high so that those are only
  chosen as a very last resort. default: 25
- `maxSlope`: the maximum slope that a wheelchair user can use without incurring routing penalties (
  leading to those ways being avoided). default: 0.083 (8.3 %)
- `slopeExceededReluctance`: the multiplier applied to how much you exceed the `maxSlope` and then
  again multiplied with the regular cost of the street. In other words: how steep should the cost
  increase when you exceed the maximum slope. default: 1.1
- `stairsReluctance`: how much should a wheelchair user avoid stairs. This should be quite high so
  that they are used only as a last resort. default: 25

## Accessible transfers

By default OTP only pre-calculates transfers between stops for able-bodied walkers. If they have no
obstacles wheelchair users can use them, too, but there won't be guaranteed to be one.

If you want OTP to also pre-generate wheelchair-accessible transfers use the following configuration
in `build-config.json`:

```json
{
  "transferRequests": [
    {
      "modes": "WALK"
    },
    {
      "modes": "WALK",
      "wheelchairAccessibility": {
        "enabled": true
      }
    }
  ]
}
```

This results in OTP calculating an accessible transfer if the default one is found to be
inaccessible to wheelchair users.

## Example

A full configuration example is available
at [`/docs/examples`](https://github.com/opentripplanner/OpenTripPlanner/tree/dev-2.x/docs/examples/ibi)