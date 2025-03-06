package org.opentripplanner.ext.actuator;

import static org.apache.hc.core5.http.HttpHeaders.ACCEPT;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.updater.GraphUpdaterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/actuators")
public class ActuatorAPI {

  private static final Logger LOG = LoggerFactory.getLogger(ActuatorAPI.class);
  public static final String CONTENT_TYPE_004 = "text/plain; version=0.0.4; charset=utf-8";
  public static final String CONTENT_TYPE_OPENMETRICS_100 =
    "application/openmetrics-text; version=1.0.0; charset=utf-8";

  /**
   * List the actuator endpoints available
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response actuator(@Context UriInfo uriInfo) {
    return Response.status(Response.Status.OK)
      .entity(
        String.format(
          """
          {
            "_links" : {
              "self" : {
                "href" : "%1$s",
                "templated" : false
              },
              "health" : {
                "href" : "%1$s/health",
                "templated" : false
              },
              "prometheus" : {
                "href" : "%1$s/prometheus",
                "templated" : false
              }
            }
          }""",
          uriInfo.getRequestUri().toString().replace("$/", "")
        )
      )
      .type("application/json")
      .build();
  }

  /**
   * Return 200 when the instance is ready to use
   */
  @GET
  @Path("/health")
  @Produces(MediaType.APPLICATION_JSON)
  public Response health(@Context OtpServerRequestContext serverContext) {
    GraphUpdaterStatus updaterStatus = serverContext.transitService().getUpdaterStatus();
    if (updaterStatus != null) {
      var listUnprimedUpdaters = updaterStatus.listUnprimedUpdaters();

      if (!listUnprimedUpdaters.isEmpty()) {
        LOG.info("Graph ready, waiting for updaters: {}", listUnprimedUpdaters);
        throw new WebApplicationException(
          Response.status(Response.Status.NOT_FOUND)
            .entity("Graph ready, waiting for updaters: " + listUnprimedUpdaters + "\n")
            .type("text/plain")
            .build()
        );
      }
    }

    return Response.status(Response.Status.OK)
      .entity("{\n" + "  \"status\" : \"UP\"" + "\n}")
      .type("application/json")
      .build();
  }

  /**
   * Returns micrometer metrics in a prometheus structured format.
   */
  @GET
  @Path("/prometheus")
  @Produces({ CONTENT_TYPE_004, CONTENT_TYPE_OPENMETRICS_100 })
  public Response prometheus(
    @Context final PrometheusMeterRegistry prometheusRegistry,
    @HeaderParam(ACCEPT) @DefaultValue("*/*") final String acceptHeader
  ) {
    final var contentType = acceptHeader.contains("application/openmetrics-text")
      ? CONTENT_TYPE_OPENMETRICS_100
      : CONTENT_TYPE_004;

    return Response.status(Response.Status.OK)
      .entity(prometheusRegistry.scrape(contentType))
      .type(contentType)
      .build();
  }
}
