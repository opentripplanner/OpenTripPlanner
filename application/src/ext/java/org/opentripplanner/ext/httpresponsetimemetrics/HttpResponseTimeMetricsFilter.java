package org.opentripplanner.ext.httpresponsetimemetrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * A Jersey filter that records HTTP request response times with client identification.
 * <p>
 * The client is identified by a configurable HTTP header. Only monitored clients
 * (configured via {@code server.httpResponseTimeMetrics.monitoredClients}) are tracked individually;
 * unknown or missing client names are grouped under the "other" tag to prevent cardinality explosion.
 * <p>
 * The metric {@code http.client.requests} is recorded as a Timer with percentile histograms,
 * allowing analysis of response time distribution per client.
 * <p>
 * All timers are pre-created at startup for each combination of monitored client and endpoint
 * to ensure predictable metric cardinality.
 */
public class HttpResponseTimeMetricsFilter
  implements ContainerRequestFilter, ContainerResponseFilter {

  static final String CLIENT_TAG = "client";
  static final String URI_TAG = "uri";

  private static final String START_TIME_PROPERTY = "metrics.startTime";
  private static final String ENDPOINT_PROPERTY = "metrics.endpoint";
  private static final String OTHER_CLIENT = "other";

  private final String clientHeader;
  private final Set<String> monitoredClients;
  private final Set<String> monitoredEndpoints;
  private final Map<TimerKey, Timer> timers;

  private record TimerKey(String client, String endpoint) {}

  /**
   * Creates a filter for recording HTTP response time metrics.
   *
   * @param clientHeader the HTTP header name used to identify the client
   * @param monitoredClients the set of client names to track individually (case-insensitive)
   * @param monitoredEndpoints the set of endpoint paths to monitor (matched by suffix)
   * @param metricName the name of the metric to record
   * @param minExpectedResponseTime minimum expected response time for histogram buckets
   * @param maxExpectedResponseTime maximum expected response time for histogram buckets
   * @param registry the meter registry to record metrics to
   */
  public HttpResponseTimeMetricsFilter(
    String clientHeader,
    Set<String> monitoredClients,
    Set<String> monitoredEndpoints,
    String metricName,
    Duration minExpectedResponseTime,
    Duration maxExpectedResponseTime,
    MeterRegistry registry
  ) {
    this.clientHeader = clientHeader;
    this.monitoredClients = monitoredClients
      .stream()
      .map(s -> s.toLowerCase(Locale.ROOT))
      .collect(Collectors.toUnmodifiableSet());
    this.monitoredEndpoints = Set.copyOf(monitoredEndpoints);
    this.timers = createTimers(
      metricName,
      Objects.requireNonNull(minExpectedResponseTime),
      Objects.requireNonNull(maxExpectedResponseTime),
      registry
    );
  }

  /**
   * Creates a filter using the global meter registry.
   *
   * @param clientHeader the HTTP header name used to identify the client
   * @param monitoredClients the set of client names to track individually
   * @param monitoredEndpoints the set of endpoint paths to monitor
   * @param metricName the name of the metric to record
   * @param minExpectedResponseTime minimum expected response time for histogram buckets
   * @param maxExpectedResponseTime maximum expected response time for histogram buckets
   */
  public HttpResponseTimeMetricsFilter(
    String clientHeader,
    Set<String> monitoredClients,
    Set<String> monitoredEndpoints,
    String metricName,
    Duration minExpectedResponseTime,
    Duration maxExpectedResponseTime
  ) {
    this(
      clientHeader,
      monitoredClients,
      monitoredEndpoints,
      metricName,
      minExpectedResponseTime,
      maxExpectedResponseTime,
      Metrics.globalRegistry
    );
  }

  private Map<TimerKey, Timer> createTimers(
    String metricName,
    Duration minExpectedResponseTime,
    Duration maxExpectedResponseTime,
    MeterRegistry registry
  ) {
    var allClients = Stream.concat(monitoredClients.stream(), Stream.of(OTHER_CLIENT)).toList();
    var result = new HashMap<TimerKey, Timer>();

    for (String client : allClients) {
      for (String endpoint : monitoredEndpoints) {
        var key = new TimerKey(client, endpoint);
        var timer = Timer.builder(metricName)
          .description("HTTP request response time by client")
          .tag(CLIENT_TAG, client)
          .tag(URI_TAG, endpoint)
          .publishPercentileHistogram()
          .minimumExpectedValue(minExpectedResponseTime)
          .maximumExpectedValue(maxExpectedResponseTime)
          .register(registry);
        result.put(key, timer);
      }
    }
    return Map.copyOf(result);
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String path = getRequestPath(requestContext);
    String matchedEndpoint = findMatchingEndpoint(path);
    if (matchedEndpoint == null) {
      return;
    }
    requestContext.setProperty(START_TIME_PROPERTY, System.nanoTime());
    requestContext.setProperty(ENDPOINT_PROPERTY, matchedEndpoint);
  }

  @Nullable
  private String findMatchingEndpoint(String path) {
    for (String endpoint : monitoredEndpoints) {
      if (path.endsWith(endpoint)) {
        return endpoint;
      }
    }
    return null;
  }

  private static String getRequestPath(ContainerRequestContext requestContext) {
    return requestContext.getUriInfo().getRequestUri().getPath();
  }

  @Override
  public void filter(
    ContainerRequestContext requestContext,
    ContainerResponseContext responseContext
  ) {
    Long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);
    if (startTime == null) {
      return;
    }

    String endpoint = (String) requestContext.getProperty(ENDPOINT_PROPERTY);
    String clientName = requestContext.getHeaderString(clientHeader);
    String clientTag = resolveClientTag(clientName);

    long duration = System.nanoTime() - startTime;

    Timer timer = timers.get(new TimerKey(clientTag, endpoint));
    timer.record(duration, TimeUnit.NANOSECONDS);
  }

  private String resolveClientTag(@Nullable String clientName) {
    if (clientName != null) {
      String lowercaseName = clientName.toLowerCase(Locale.ROOT);
      if (monitoredClients.contains(lowercaseName)) {
        return lowercaseName;
      }
    }
    return OTHER_CLIENT;
  }
}
