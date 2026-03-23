package org.opentripplanner.ext.httpresponsetimemetrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opentripplanner.ext.httpresponsetimemetrics.HttpResponseTimeMetricsFilter.CLIENT_TAG;
import static org.opentripplanner.ext.httpresponsetimemetrics.HttpResponseTimeMetricsFilter.URI_TAG;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.server.GrizzlyQueueWaitProbe;

/**
 * Note: The unit tests relies on mocks to abstract out the micrometer API (request and response objects).
 */
class HttpResponseTimeMetricsFilterTest {

  private static final String CLIENT_HEADER = "et-client-name";
  private static final String TRANSMODEL_ENDPOINT = "/transmodel/v3";
  private static final String GTFS_ENDPOINT = "/gtfs/v1/";
  private static final Set<String> MONITORED_ENDPOINTS = Set.of(TRANSMODEL_ENDPOINT, GTFS_ENDPOINT);
  private static final String METRIC_NAME = "otp_http_server_requests";
  private static final Duration MIN_EXPECTED_RESPONSE_TIME = Duration.ofMillis(10);
  private static final Duration MAX_EXPECTED_RESPONSE_TIME = Duration.ofMillis(10_000);
  private SimpleMeterRegistry registry;
  private HttpResponseTimeMetricsFilter filter;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    filter = new HttpResponseTimeMetricsFilter(
      CLIENT_HEADER,
      Set.of("app1", "app2", "web-client"),
      MONITORED_ENDPOINTS,
      METRIC_NAME,
      MIN_EXPECTED_RESPONSE_TIME,
      MAX_EXPECTED_RESPONSE_TIME,
      registry
    );
  }

  @Test
  void timersArePreCreatedAtStartup() {
    assertNotNull(findTimer("app1", TRANSMODEL_ENDPOINT));
  }

  @Test
  void recordsMetricForMonitoredClient() {
    recordRequest("app1", TRANSMODEL_ENDPOINT);
    assertTimerCount("app1", TRANSMODEL_ENDPOINT, 1);
  }

  @Test
  void matchesClientNameCaseInsensitively() {
    // Send requests with different case variants: "APP1", "App1", "app1"
    recordRequest("APP1", TRANSMODEL_ENDPOINT);
    recordRequest("App1", TRANSMODEL_ENDPOINT);
    recordRequest("app1", TRANSMODEL_ENDPOINT);

    // All three should be recorded under the lowercase "app1" tag
    assertTimerCount("app1", TRANSMODEL_ENDPOINT, 3);
  }

  @Test
  void recordsMetricForUnknownClientAsOther() {
    recordRequest("unknown-app", TRANSMODEL_ENDPOINT);
    assertTimerCount("other", TRANSMODEL_ENDPOINT, 1);
  }

  @Test
  void recordsMetricForMissingHeaderAsOther() {
    recordRequest(null, TRANSMODEL_ENDPOINT);
    assertTimerCount("other", TRANSMODEL_ENDPOINT, 1);
  }

  @Test
  void recordsMetricsForMultipleMonitoredEndpoints() {
    recordRequest("app1", TRANSMODEL_ENDPOINT);
    recordRequest("app1", GTFS_ENDPOINT);

    assertTimerCount("app1", TRANSMODEL_ENDPOINT, 1);
    assertTimerCount("app1", GTFS_ENDPOINT, 1);
  }

  @Test
  void totalTimeIncludesQueueWaitWhenPresent() {
    long queueWaitNanos = Duration.ofMillis(50).toNanos();
    recordRequestWithQueueWait("app1", TRANSMODEL_ENDPOINT, queueWaitNanos);

    var totalTimer = findTotalTimer("app1", TRANSMODEL_ENDPOINT);
    assertNotNull(totalTimer, "Total timer should exist");
    assertEquals(1, totalTimer.count());

    var processingTimer = findTimer("app1", TRANSMODEL_ENDPOINT);
    assertTrue(
      totalTimer.totalTime(TimeUnit.NANOSECONDS) > processingTimer.totalTime(TimeUnit.NANOSECONDS),
      "Total time should be greater than processing time when queue wait is present"
    );
  }

  @Test
  void totalTimeEqualsProcessingTimeWhenNoQueueWait() {
    recordRequest("app1", TRANSMODEL_ENDPOINT);

    var totalTimer = findTotalTimer("app1", TRANSMODEL_ENDPOINT);
    assertNotNull(totalTimer, "Total timer should be pre-created");
    assertEquals(1, totalTimer.count());

    var processingTimer = findTimer("app1", TRANSMODEL_ENDPOINT);
    assertEquals(
      processingTimer.totalTime(TimeUnit.NANOSECONDS),
      totalTimer.totalTime(TimeUnit.NANOSECONDS),
      "Total time should equal processing time when no queue wait"
    );
  }

  @Test
  void requestToUnmonitoredEndpointConsumesQueueWaitThreadLocal() {
    // Simulate a queue wait value left by GrizzlyQueueWaitProbe on the current thread
    var probe = new GrizzlyQueueWaitProbe();
    Runnable task = () -> {};
    probe.onTaskQueueEvent(null, task);
    probe.onTaskDequeueEvent(null, task);

    // Send a request to an unmonitored endpoint — the filter should return early
    // but still consume the ThreadLocal to prevent it leaking to the next request
    var requestContext = mock(ContainerRequestContext.class);
    var uriInfo = mock(UriInfo.class);
    when(uriInfo.getRequestUri()).thenReturn(URI.create("/unmonitored/path"));
    when(requestContext.getUriInfo()).thenReturn(uriInfo);

    filter.filter(requestContext);

    // The ThreadLocal must be cleared
    assertNull(
      GrizzlyQueueWaitProbe.getAndClearQueueWaitNanos(),
      "Queue wait ThreadLocal should be consumed even for unmonitored endpoints"
    );
  }

  @Test
  void totalTimersArePreCreatedAtStartup() {
    assertNotNull(findTotalTimer("app1", TRANSMODEL_ENDPOINT));
    assertNotNull(findTotalTimer("other", TRANSMODEL_ENDPOINT));
  }

  private Timer findTimer(String client, String endpoint) {
    return registry.find(METRIC_NAME).tag(CLIENT_TAG, client).tag(URI_TAG, endpoint).timer();
  }

  private Timer findTotalTimer(String client, String endpoint) {
    return registry
      .find(METRIC_NAME + "_total_time")
      .tag(CLIENT_TAG, client)
      .tag(URI_TAG, endpoint)
      .timer();
  }

  private void assertTimerCount(String client, String endpoint, long expectedCount) {
    Timer timer = findTimer(client, endpoint);
    assertNotNull(
      timer,
      "Timer should exist for client '%s' and endpoint '%s'".formatted(client, endpoint)
    );
    assertEquals(expectedCount, timer.count());
  }

  private void recordRequest(String clientName, String endpoint) {
    recordRequestWithQueueWait(clientName, endpoint, null);
  }

  private void recordRequestWithQueueWait(String clientName, String endpoint, Long queueWaitNanos) {
    var requestContext = mock(ContainerRequestContext.class);
    var responseContext = mock(ContainerResponseContext.class);
    var uriInfo = mock(UriInfo.class);

    when(uriInfo.getRequestUri()).thenReturn(URI.create(endpoint));
    when(requestContext.getUriInfo()).thenReturn(uriInfo);

    filter.filter(requestContext);

    when(requestContext.getProperty("metrics.startTime")).thenReturn(System.nanoTime());
    when(requestContext.getProperty("metrics.endpoint")).thenReturn(endpoint);
    when(requestContext.getProperty("metrics.queueWaitNanos")).thenReturn(queueWaitNanos);
    when(requestContext.getHeaderString(CLIENT_HEADER)).thenReturn(clientName);

    filter.filter(requestContext, responseContext);
  }
}
