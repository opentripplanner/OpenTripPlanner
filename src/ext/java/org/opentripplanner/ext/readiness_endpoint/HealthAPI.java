package org.opentripplanner.ext.readiness_endpoint;

import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path("/routers/{routerId}/health")
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class HealthAPI {

    private final Router router;

    public HealthAPI(@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        this.router = otpServer.getRouter(routerId);
    }

    /**
     * Return 200 when the instance is ready to use
     */
    @GET
    @Path("/ready")
    public Response isReady() {
        return Response.status(Response.Status.OK).build();
    }
}
