package org.opentripplanner.ext.readiness_endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;


@Path("/actuators")
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class ActuatorAPI {

    private static final Logger LOG = LoggerFactory.getLogger(ActuatorAPI.class);

    private final Router router;

    public ActuatorAPI(@Context OTPServer otpServer) {
        this.router = otpServer.getRouter();
    }

    /**
     * List the actuator endpoints available
     */
    @GET
    @Path("")
    public Response actuator() {
        return Response.status(Response.Status.OK).entity(
            "{\n"
            + "  \"_links\" : {\n"
            + "    \"self\" : {\n"
            + "      \"href\" : \"/actuator\", \n"
            + "    },\n"
            + "    \"health\" : {\n"
            + "      \"href\" : \"/actuator/health\"\n"
            + "    } "
            + "\n}" )
            .type("application/json").build();
    }

    /**
     * Return 200 when the instance is ready to use
    */
    @GET
    @Path("/health")
    public Response health() {
        if (router.graph.updaterManager != null) {
            Collection<String> waitingUpdaters = router.graph.updaterManager.waitingUpdaters();

            if (!waitingUpdaters.isEmpty()) {
                LOG.info("Graph ready, waiting for updaters: {}", waitingUpdaters);
                throw new WebApplicationException(Response
                    .status(Response.Status.NOT_FOUND)
                    .entity("Graph ready, waiting for updaters: " + waitingUpdaters + "\n")
                    .type("text/plain")
                    .build());
            }
        }

        return Response.status(Response.Status.OK).entity(
            "{\n"
            + "  \"status\" : \"UP\""
            + "\n}" )
            .type("application/json").build();
    }
}
