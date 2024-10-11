package org.opentripplanner.standalone.server;

import org.opentripplanner.framework.lang.StringUtils;

/**
 * OTP supports tracing user requests across log events and "outside" services. It does so by
 * allowing http-request-header parameters to be included in log events and in the http response.
 * OTP can also generate a unique value to insert in the logs and in the response, if the value is
 * missing in the request.
 * <p>
 * <b>Correlation-ID example</b>
 * <p>
 * A common use-case in a service oriented environment is to use a correlation id to identify
 * all log messages across multiple services the serve the same user initiated request. This
 * can be done by setting the "X-Correlation-ID" http header in the http facade/gateway. Then
 * all services must add this to all log messages for this request and also set the header in
 * the response. To configure OTP to do this, add a parameter like this:
 * <pre>
 * {
 *   "httpRequestHeader" : "X-Correlation-ID",
 *   "httpResponseHeader" : "X-Correlation-ID",
 *   "logKey" : "correlationId",
 *   "generateIdIfMissing" : true
 * }
 * </pre>
 *
 * @param httpRequestHeader   The HTTP request header to look for.
 * @param httpResponseHeader  The HTTP response header to set. If not set the value is not included
 *                            in the response.
 * @param logKey              The key used to store the value in the log events using the logging
 *                            framework(Mapped Diagnostic Context).
 * @param generateIdIfMissing If {@code true}, OTP will generate a value if no value is passed in.
 */
public record RequestTraceParameter(
  String httpRequestHeader,
  String httpResponseHeader,
  String logKey,
  boolean generateIdIfMissing
) {
  public RequestTraceParameter {
    if (!(StringUtils.hasValue(httpRequestHeader) || generateIdIfMissing)) {
      throw new IllegalArgumentException(
        "At least one source is required: 'httpRequestHeader' and/or 'generateIdIfMissing'"
      );
    }
    if (!(StringUtils.hasValue(httpResponseHeader) || StringUtils.hasValue(logKey))) {
      throw new IllegalArgumentException(
        "At least one target is required: 'httpResponseHeader' and/or 'logKey'"
      );
    }
  }

  public boolean hasHttpRequestHeader() {
    return StringUtils.hasValue(httpRequestHeader);
  }

  public boolean hasHttpResponseHeader() {
    return StringUtils.hasValue(httpResponseHeader);
  }

  public boolean hasLogKey() {
    return StringUtils.hasValue(logKey);
  }
}
