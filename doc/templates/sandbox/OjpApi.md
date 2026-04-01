# OpenJourneyPlanner (OJP) API

## Contact Info

- Leonard Ehrenfried, [mail@leonard.io](mailto:mail@leonard.io)

## Documentation

This sandbox feature implements part of the [OpenJourneyPlanner API](https://opentransportdata.swiss/en/cookbook/open-journey-planner-ojp/) 
which is a standard defined by CEN, the European Committee for Standardization.

The following request types are supported:

- `StopEventRequest`
- `TripRequest`

To enable this turn on `OjpApi` as a feature in `otp-config.json`.

## Configuration

This feature allows a small number of config options. To change the configuration, add the 
following to `router-config.json`.

<!-- INSERT: config -->
