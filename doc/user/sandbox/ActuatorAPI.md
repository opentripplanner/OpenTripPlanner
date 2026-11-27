# Actuator API

## Contact Info

- Entur, Norway

## Changelog

- Initial implementation of readiness endpoint (November 2019)
- Prometheus metrics added using Micrometer (October 2021)
- GraphQL metrics added to prometheus export (November 2021)
- Serve the API on a separate port with `--actuatorPort` (November 2026)

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
  "otpFeatures": {
    "ActuatorAPI": true
  }
}
```

### Serving the API on a separate port

By default, the Actuator API is served on the same port as the public APIs (`--port`, default
8080). The public APIs are usually exposed through an ingress, while the actuator endpoints are
meant only for the platform, like Kubernetes probes and Prometheus scraping. Use the command line
parameter `--actuatorPort` to serve the Actuator API on its own port:

```
java -jar otp-shaded-VERSION.jar --load --actuatorPort 8081 /var/opentripplanner
```

If `--actuatorPort` differs from `--port`:

- The actuator endpoints are served only on the actuator port, and not on the main port anymore.
  No other API is served on the actuator port. The paths stay the same, for example
  `http://localhost:8081/otp/actuators/health`.
- The actuator port has its own small thread pool, so health probes and metrics scraping are
  answered even when the routing requests saturate the main thread pool. Requests to the
  actuator port are not included in the `http.server.requests` metrics.
- The actuator port uses the same network interface as the main port, `--bindAddress`.

If `--actuatorPort` is not set, or equal to `--port`, the Actuator API is served on the main port.
The parameter only has an effect if the `ActuatorAPI` feature is enabled. If it is set while the
feature is disabled, OTP logs a warning, or aborts the startup if `--abortOnUnknownConfig` is set.
When the feature is enabled, OTP checks at startup that the actuator port is available.

The OTP container image exposes port 8081 in addition to 8080, so `8081` is a good choice for the
actuator port.

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
[GTFS API](../RouterConfiguration.md#gtfsApi_tracingTags). If configured correctly, the tracing tags
appear in the Prometheus metrics, for example:

```
...
graphql_timer_resolver_seconds_count{example-header-or-query-parameter-name="value",operationName="__UNKNOWN__",parent="QueryType"} 9
graphql_timer_resolver_seconds_sum{example-header-or-query-parameter-name="value",operationName="__UNKNOWN__",parent="QueryType"} 10.621173848
graphql_timer_resolver_seconds_max{example-header-or-query-parameter-name="value",operationName="__UNKNOWN__",parent="QueryType"} 1.997706365
...
```
