package org.opentripplanner.standalone.config.routerconfig;

import static org.opentripplanner.standalone.config.framework.json.EnumMapper.docEnumValueList;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_4;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.apis.support.graphql.injectdoc.ApiDocumentationProfile;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.server.OTPWebApplicationParameters;
import org.opentripplanner.standalone.server.RequestTraceParameter;

public class ServerConfig implements OTPWebApplicationParameters {

  private final Duration apiProcessingTimeout;
  private final List<RequestTraceParameter> traceParameters;
  private final ApiDocumentationProfile apiDocumentationProfile;

  public ServerConfig(String parameterName, NodeAdapter root) {
    NodeAdapter c = root
      .of(parameterName)
      .since(V2_4)
      .summary("Configuration for router server.")
      .description(
        """
These parameters are used to configure the router server. Many parameters are specific to a 
domain, these are set in the routing request.
"""
      )
      .asObject();

    this.apiProcessingTimeout =
      c
        .of("apiProcessingTimeout")
        .since(V2_4)
        .summary("Maximum processing time for an API request")
        .description(
          """
This timeout limits the server-side processing time for a given API request. This does not include
network latency nor waiting time in the HTTP server thread pool. The default value is
`-1s`(no timeout). The timeout is applied to all APIs (REST, Transmodel & GTFS GraphQL).
The timeout is not enforced when the parallel routing OTP feature is in use.
"""
        )
        .asDuration(Duration.ofSeconds(-1));

    this.apiDocumentationProfile =
      c
        .of("apiDocumentationProfile")
        .since(V2_7)
        .summary(ApiDocumentationProfile.DEFAULT.typeDescription())
        .description(docEnumValueList(ApiDocumentationProfile.values()))
        .asEnum(ApiDocumentationProfile.DEFAULT);

    this.traceParameters =
      c
        .of("traceParameters")
        .since(V2_4)
        .summary(
          "Trace OTP request using HTTP request/response parameter(s) combined with logging."
        )
        .description(
          """
OTP supports tracing user requests across log events and "outside" services. OTP can insert
http-request-header parameters into all associated log events and into the http response. If the
value is not present in the request, a unique value can be generated. The OTP generated value is
a 6 characters long base 36[0-9a-z] character string.

**Use-case Correlation-ID**

A common use-case in a service oriented environment is to use a _correlation-id_ to identify all log
messages across multiple (micro-)services from the same user. This is done by setting the
"X-Correlation-ID" http header in the http facade/gateway. Use the "traceParameters" to configure
OTP to pick up the correlation id, insert it into the logs and return it. See the example below
on how-to configure the "server.traceParameters" instance.
"""
        )
        .asObjects(t ->
          new RequestTraceParameter(
            t
              .of("httpRequestHeader")
              .since(V2_4)
              .summary("The header-key to use when fetching the trace parameter value")
              .asString(null),
            t
              .of("httpResponseHeader")
              .since(V2_4)
              .summary("The header-key to use when saving the value back into the http response")
              .asString(null),
            t
              .of("logKey")
              .since(V2_4)
              .summary("The log event key used.")
              .description(
                """
OTP stores the key/value pair in the log MDC(Mapped Diagnostic Context). To use it you normally
include the key in the log pattern like this: `%X{LOG-KEY}`. See your log framework for details.
Only log4j and logback support this.
"""
              )
              .asString(null),
            t
              .of("generateIdIfMissing")
              .since(V2_4)
              .summary(
                "If `true` a unique value is generated if no http request header is provided, or " +
                "the value is missing."
              )
              .asBoolean(false)
          )
        );
  }

  public Duration apiProcessingTimeout() {
    return apiProcessingTimeout;
  }

  @Override
  public List<RequestTraceParameter> traceParameters() {
    return traceParameters;
  }

  public ApiDocumentationProfile apiDocumentationProfile() {
    return apiDocumentationProfile;
  }

  public void validate(Duration streetRoutingTimeout) {
    if (
      !apiProcessingTimeout.isNegative() &&
      streetRoutingTimeout.toSeconds() > apiProcessingTimeout.toSeconds()
    ) {
      throw new OtpAppException(
        "streetRoutingTimeout (" +
        streetRoutingTimeout +
        ") must be shorter than apiProcessingTimeout (" +
        apiProcessingTimeout +
        ')'
      );
    }
  }
}
