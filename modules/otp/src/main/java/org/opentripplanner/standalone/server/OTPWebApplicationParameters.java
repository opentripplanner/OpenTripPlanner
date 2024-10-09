package org.opentripplanner.standalone.server;

import java.util.List;

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
}
