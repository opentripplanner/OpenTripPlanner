package org.opentripplanner.standalone.server;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import org.opentripplanner.framework.application.LogMDCSupport;
import org.opentripplanner.framework.lang.StringUtils;

/**
 * This filter manages OTP request trace parameters. A trace parameter can be read from the
 * HTTP headers and written to the HTTP response. If the header is missing or the value is
 * empty a trace parameter value can be generated. The value is made available in the logs
 * as well if they have an associated logKey.
 * <p>
 * The {@link org.opentripplanner.framework.concurrent.OtpRequestThreadFactory} work together
 * with this filter to propagate the log context to all threads.
 */
public class RequestTraceFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final String MIN_ID_BASE = "1000000";
  private static final long MIN_ID = Long.parseLong(MIN_ID_BASE, Character.MAX_RADIX);
  private static final long MAX_ID = Long.parseLong(MIN_ID_BASE + "0", Character.MAX_RADIX);

  /**
   * The regular Random is good enough for generating new correlationIds, but we want
   * to properly seed it. Many otp instances might get started at the same time so using
   * the current time is not good enough. We do not use UUID because an 32 bit int is
   * enough and easier to read.
   */
  private static final Random ID_GEN = new Random(new SecureRandom().nextLong());

  /**
   * Allow HTTP header values to be inserted into the logs if it matches this pattern.
   * If this is to strict a PATTERN should be added to the configuration parameters.
   * The pattern specified accepts all characters except control characters and vertical
   * space. The length must be between 1 and 512 characters.
   */
  static final Pattern HTTP_HEADER_VALUE_CHECK = Pattern.compile("[^\\p{Cntrl}\\v]{1,512}");

  /**
   * This can not be final since it is injected at startup time.
   */
  private static List<RequestTraceParameter> traceParameters;

  public static void init(List<RequestTraceParameter> parameters) {
    var copy = new ArrayList<>(parameters);
    // Reverse list to give precedence to elements in the beginning of the list. If the same
    // logKey or responseHeader is used more than one time the first element will over-write
    // the second when saved in the response and in the MDC map.
    Collections.reverse(copy);
    traceParameters = List.copyOf(copy);
  }

  /**
   * Loop through all trance-parameters and insert key/value into the log context. The value is
   * read from the http-request header or generated.
   */
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    for (var it : traceParameters) {
      if (it.hasLogKey()) {
        String value = null;
        if (it.hasHttpRequestHeader()) {
          value = retrieveHTTPHeaderValue(requestContext, it.httpRequestHeader());
        }
        if (value == null && it.generateIdIfMissing()) {
          value = generateUniqueId();
        }
        // Put value, even if it is empty. This will clear any value set in the thread from
        // before. This happens if the previous request did not clean up after it-self.
        LogMDCSupport.putLocal(it.logKey(), value);
      }
    }
  }

  /**
   * Insert all trace-parameters in http response headers. The value is taken from the request
   * header, or log context, or generated.
   */
  @Override
  public void filter(
    ContainerRequestContext requestContext,
    ContainerResponseContext responseContext
  ) throws IOException {
    for (var it : traceParameters) {
      if (it.hasHttpResponseHeader()) {
        var value = resolveValueForResponse(it, requestContext);
        setHttpResponseHeaderValue(responseContext, it.httpResponseHeader(), value);
      }
      if (it.hasLogKey()) {
        LogMDCSupport.removeLocal(it.logKey());
      }
    }
  }

  /**
   * Find the value for the trace parameter:
   * <ol>
   *   <li>from the request http header, or</li>
   *   <li>from the log context, or</li>
   *   <li>generate a new unique value.</li>
   * </ol>
   */
  private String resolveValueForResponse(
    RequestTraceParameter p,
    ContainerRequestContext requestContext
  ) {
    String value = null;

    if (p.hasHttpRequestHeader()) {
      value = retrieveHTTPHeaderValue(requestContext, p.httpRequestHeader());
    }
    if (value == null && p.hasLogKey()) {
      value = LogMDCSupport.getLocalValue(p.logKey());
    }
    if (value == null && p.generateIdIfMissing()) {
      value = generateUniqueId();
    }
    return value;
  }

  private void setHttpResponseHeaderValue(
    ContainerResponseContext responseContext,
    String header,
    String value
  ) {
    if (StringUtils.hasValue(value)) {
      responseContext.getHeaders().add(header, value);
    }
  }

  private static String generateUniqueId() {
    long v = ID_GEN.nextLong(MIN_ID, MAX_ID);
    return Long.toString(v, Character.MAX_RADIX);
  }

  private static String retrieveHTTPHeaderValue(
    ContainerRequestContext requestContext,
    String httpHeader
  ) {
    String value = requestContext.getHeaderString(httpHeader);
    if (StringUtils.hasNoValue(value)) {
      return null;
    }
    return HTTP_HEADER_VALUE_CHECK.matcher(value).matches() ? value : null;
  }
}
