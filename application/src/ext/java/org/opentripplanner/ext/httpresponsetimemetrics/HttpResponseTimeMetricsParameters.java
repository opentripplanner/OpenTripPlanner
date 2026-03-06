package org.opentripplanner.ext.httpresponsetimemetrics;

import java.time.Duration;
import java.util.Set;

/**
 * Parameters for HTTP response time metrics.
 *
 * @param clientHeader the HTTP header name used to identify the client
 * @param monitoredClients the set of client names to track individually
 * @param monitoredEndpoints the set of endpoint paths to monitor for metrics
 * @param metricName the name of the metric to record
 * @param minExpectedResponseTime minimum expected response time for histogram buckets
 * @param maxExpectedResponseTime maximum expected response time for histogram buckets
 */
public record HttpResponseTimeMetricsParameters(
  String clientHeader,
  Set<String> monitoredClients,
  Set<String> monitoredEndpoints,
  String metricName,
  Duration minExpectedResponseTime,
  Duration maxExpectedResponseTime
) {
  public static final String DEFAULT_CLIENT_HEADER = "x-client-name";
  public static final Set<String> DEFAULT_MONITORED_ENDPOINTS = Set.of(
    "/transmodel/v3",
    "/gtfs/v1/"
  );
  public static final String DEFAULT_METRIC_NAME = "otp_http_server_requests";
  public static final Duration DEFAULT_MIN_EXPECTED_RESPONSE_TIME = Duration.ofMillis(10);
  public static final Duration DEFAULT_MAX_EXPECTED_RESPONSE_TIME = Duration.ofMillis(10_000);

  public HttpResponseTimeMetricsParameters {
    monitoredClients = Set.copyOf(monitoredClients);
    monitoredEndpoints = Set.copyOf(monitoredEndpoints);
  }
}
