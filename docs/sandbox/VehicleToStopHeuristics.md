# Vehicle-to-Stop heuristics - OTP Sandbox Extension

## Contact Info

- Leonard Ehrenfried ([mail@leonard.io](mailto:mail@leonard.io))

## Changelog

- Create initial
  implementation [#3906](https://github.com/opentripplanner/OpenTripPlanner/pull/3906)

## Documentation

This feature is meant to improve the performance and result quality for routing requests where a
vehicle (car, bike, scooter) is ridden to a stop where transit is boarded.

Before this feature existed a search for nearby stops was executed finding all candidate stops for
boarding transit. For walking this yields a low number of stops but when driving a car this can
easily mean to an entire city of stops, since the default was a drive of 45 minutes.

Having a very long driving time has several problems:

- the access search itself is comparatively slow
- having many candidate stops slows down the transit search as many routes have to be checked
- often the quickest route would be to drive almost all the way to the destination and board transit
  for a single stop

We did not want to lower the maximum access time since in rural regions 45 minutes might be a useful
maximum, but we want it to scale with the density of well-connected stops.

### Vehicle-to-stop heuristic

In order to improve the Park+Ride and Bike+Ride results we reduced the number of candidate stops
with the following heuristic:

1. When a stop is encountered check which routes depart from it
2. Each route adds to an "importance score"
3. Modes which are fast (RAIL, SUBWAY, FERRY) have a higher score than for example BUS
4. Once a maximum score is reached, the search is complete

The code for this is located in `VehicleToStopSkipEdgeStrategy.java`.

### Bicycle-on-transit heuristic

This heuristic works slightly differently in that it doesn't assign a score but simply stops the
access search when a certain number of routes were encountered that allow you to take your bike onto
transit.

The code for this is located in `BikeToStopSkipEdgeStrategy.java`.

### Configuration

Enable the feature by adding it to the ```otp-config.json```:

```json
// otp-config.json
{
  "otpFeatures": {
    "VehicleToStopHeuristics": true
  }
}
```

### Collaborators wanted

Since the current settings, scores and weights are hardcoded in the source code we are looking for
collaborators that can help to make it more adaptable for different environments.

These are some the goals for the future:

- make the scores that are assigned for routes of a certain mode configurable in JSON
- pre-calculate stop importance scores during the graph build

If you want to help making this feature more flexible, please
contact [Leonard Ehrenfried](mailto:mail@leonard.io)
or use the regular channels of communication outlined
in [CONTRIBUTING.md](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/CONTRIBUTING.md#primary-channels-of-communication)