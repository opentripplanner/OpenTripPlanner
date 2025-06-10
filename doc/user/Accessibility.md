# Accessibility

## Preamble

In this document and in OTP, the term "accessibility" is used with its most common 
meaning: design of products, devices, services, vehicles, or environments to ensure they are usable by 
people with disabilities. If you have reached this page looking for cumulative opportunities 
accessibility indicators (access to opportunities metrics) please see the [Analysis](Analysis.md) page.

While accessibility is a complex subject, at this point GTFS and Netex mostly represent it very
simply as a yes/no possibility of wheelchair use. While OTP developers hope to broaden the
scope and nuance of accessibility support in OTP, the lack of detailed data from data producers
currently limits implementation and discussion in this document to this binary
"wheelchair accessible" definition.

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
      "elevator": {
        "onlyConsiderAccessible": false
      },
      "inaccessibleStreetReluctance": 25,
      "maxSlope": 0.08333,
      "slopeExceededReluctance": 1,
      "stairsReluctance": 25
    }
  },
  "updaters": []
}
```

The parameters for `stop`, `trip` and `elevator` mean the following:

| name                     |                                                                                                | default |
|--------------------------|------------------------------------------------------------------------------------------------|---------|
| `onlyConsiderAccessible` | Whether to exclude unknown accessibility and inaccessible stops/trips/elevators in the search. | `true`  |
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
- `slopeExceededReluctance`: how steep should the cost increase when you exceed the maximum slope.
  By default, every percent over the limit doubles the cost of the traversal, so if the regular cost
  is 100, being 1 percent over the limit will lead to a cost of 200, 2 percent over will lead to 400
  and so on.
  If you want an even steeper increase then set a value higher than 1. If you want it shallower use
  a value between 0 and 1. To disable the penalty set a value below 0. default: 1
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
at [`/docs/examples`](https://github.com/opentripplanner/OpenTripPlanner/tree/dev-2.x/doc/user/examples/ibi)