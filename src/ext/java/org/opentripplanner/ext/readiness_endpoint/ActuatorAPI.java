package org.opentripplanner.ext.readiness_endpoint;

import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path("/actuators")
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class ActuatorAPI {

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
        return Response.status(Response.Status.OK).entity(
            "{\n"
            + "  \"status\" : \"UP\""
            + "\n}" )
            .type("application/json").build();
    }
}
