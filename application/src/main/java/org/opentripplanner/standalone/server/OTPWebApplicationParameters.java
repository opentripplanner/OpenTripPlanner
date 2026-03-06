package org.opentripplanner.standalone.server;

import java.util.List;
import org.opentripplanner.ext.httpresponsetimemetrics.HttpResponseTimeMetricsParameters;

/**
 * Parameters used to configure the {@link OTPWebApplication}.
 */
public interface OTPWebApplicationParameters {
  /**
   * The HTTP request/response trace/correlation-id headers to use.
   */
  List<RequestTraceParameter> traceParameters();

  default boolean requestTraceLoggingEnabled() {
    return traceParameters().stream().anyMatch(RequestTraceParameter::hasLogKey);
  }

  /**
   * Configuration for HTTP response time metrics.
   */
  HttpResponseTimeMetricsParameters httpResponseTimeMetricsParameters();
}
