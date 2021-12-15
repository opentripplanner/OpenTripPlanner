package org.opentripplanner.ext.actuator;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path("/actuators")
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class ActuatorAPI {

    private static final Logger LOG = LoggerFactory.getLogger(ActuatorAPI.class);

    /**
     * List the actuator endpoints available
     */
    @GET
    public Response actuator(@Context UriInfo uriInfo) {
        return Response.status(Response.Status.OK).entity(String.format(
            "{\n"
            + "  \"_links\" : {\n"
            + "    \"self\" : {\n"
            + "      \"href\" : \"%1$s\",\n"
            + "      \"templated\" : false\n"
            + "    },\n"
            + "    \"health\" : {\n"
            + "      \"href\" : \"%1$s/health\",\n"
            + "      \"templated\" : false\n"
            + "    },\n"
            + "    \"prometheus\" : {\n"
            + "      \"href\" : \"%1$s/prometheus\",\n"
            + "      \"templated\" : false\n"
            + "    }\n"
            + "  }\n"
            + "}", uriInfo.getRequestUri().toString()))
            .type("application/json").build();
    }

    /**
     * Return 200 when the instance is ready to use
     */
    @GET
    @Path("/health")
    public Response health(@Context OTPServer otpServer) {
        GraphUpdaterManager updaterManager = otpServer.getRouter().graph.updaterManager;
        if (updaterManager != null) {
            var listUnprimedUpdaters = updaterManager.listUnprimedUpdaters();

            if (!listUnprimedUpdaters.isEmpty()) {
                LOG.info("Graph ready, waiting for updaters: {}", listUnprimedUpdaters);
                throw new WebApplicationException(Response
                    .status(Response.Status.NOT_FOUND)
                    .entity("Graph ready, waiting for updaters: " + listUnprimedUpdaters + "\n")
                    .type("text/plain")
                    .build());
            }
        }

        return Response.status(Response.Status.OK).entity(
            "{\n"
            + "  \"status\" : \"UP\""
            + "\n}")
            .type("application/json").build();
    }

    /**
     * Returns micrometer metrics in a prometheus structured format.
     */
    @GET
    @Path("/prometheus")
    public Response prometheus(@Context PrometheusMeterRegistry prometheusRegistry) {
        return Response.status(Response.Status.OK)
            .entity(prometheusRegistry.scrape())
            .type("text/plain")
            .build();
    }
}
