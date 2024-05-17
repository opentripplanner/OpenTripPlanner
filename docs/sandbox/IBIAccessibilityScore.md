# IBI Group Accessibility Score

## Contact Info

- IBI Group ([transitrealtime@ibigroup.com](mailto:transitrealtime@ibigroup.com))

## Documentation

This extension computes a numeric accessibility score between 0 and 1 and adds it to the itinerary
and its legs.

Note: the information to calculate this score are all available to the frontend, however calculating
them on the backend makes life a little easier and changes are automatically applied to all
frontends.

To enable the feature add the following to `router-config.json`:

```json
// router-config.json
{
  "routingDefaults": {
    "itineraryFilters": {
      // add IBI accessibility score between 0 and 1
      "accessibilityScore": true
    }
  }
}
```

The score is only computed when you search for wheelchair-accessible routes.

## Changelog

- Create initial implementation [#4221](https://github.com/opentripplanner/OpenTripPlanner/pull/4221)
