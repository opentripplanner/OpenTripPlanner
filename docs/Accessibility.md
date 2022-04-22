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
      "trips": {
        "onlyConsiderAccessible": false,
        "unknownCost": 600,
        "inaccessibleCost": 3600
      },
      "stops": {
        "onlyConsiderAccessible": false,
        "unknownCost": 600,
        "inaccessibleCost": 3600
      }
    }
  },
  "updaters": []
}
```

The parameters for `stops` and `trips` mean the following:

| name                     |                                                                                                                                         |
|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `onlyConsiderAccessible` | Whether to include unknown accessibility and inaccessible stops/tips in the search. |
| `unknownCost`            | The cost to add if a stop/trip has unknown wheelchair accessibility                                                                     |
| `inaccessibleCost`       | The cost to add if a stop/trip is known to be inaccessible                                                                              |

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
      "wheelchairAccessibility": { "enabled": true }
    }
  ]
}
```

This results in OTP calculating an accessible transfer if the default one is found to be inaccessible
to wheelchair users.

## Example

A full configuration example is available at [`/docs/examples`](https://github.com/opentripplanner/OpenTripPlanner/tree/dev-2.x/docs/examples/ibi)