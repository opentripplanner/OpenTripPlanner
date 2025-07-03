# Actuator API

## Contact Info

- Entur, Norway

## Changelog

- Initial implementation of readiness endpoint (November 2019)
- Prometheus metrics added using Micrometer (October 2021)
- GraphQL metrics added to prometheus export (November 2021)

## Documentation

This provides endpoints for checking the health status of the OTP instance. It can be useful when
running OTP in a container.

The API will be at the endpoint `http://localhost:8080/otp/actuators` and follows the Spring Boot
actuator API standard.

### Configuration

To enable this you need to add the feature `ActuatorAPI`.

```json
// otp-config.json
{
  "otpFeatures" : {
    "ActuatorAPI": true
  }
}
```

### Endpoints

#### /otp/actuators/health

The health endpoints returns an 200 OK status code once the graph is loaded and all updaters are
ready. Otherwise, a 404 NOT FOUND is returned.

#### /otp/actuators/prometheus

Prometheus metrics are returned using Micrometer. The default JVM and jersey metrics are enabled.

Also, GraphQL timing metrics are exported under `graphql.timer.query` and `graphql.timer.resolver`,
if the GraphQL endpoints are enabled.

### Tracing tags

To enable tracing tags, configure them for the
[Transmodel API](../RouterConfiguration.md#transmodelApi_tracingHeaderTags) or
[GTFS API](../RouterConfiguration.md#gtfsApi_tracingTags).
If configured correctly, the tracing tags appear in the Prometheus metrics, for example:
```
...
graphql_timer_resolver_seconds_count{example-header-or-query-parameter-name="value",operationName="__UNKNOWN__",parent="QueryType"} 9
graphql_timer_resolver_seconds_sum{example-header-or-query-parameter-name="value",operationName="__UNKNOWN__",parent="QueryType"} 10.621173848
graphql_timer_resolver_seconds_max{example-header-or-query-parameter-name="value",operationName="__UNKNOWN__",parent="QueryType"} 1.997706365
...
```
