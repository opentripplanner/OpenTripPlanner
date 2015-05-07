package org.opentripplanner.api.resource;

import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Report the status of the graph updaters via a web service.
 */
@Path("/routers/{routerId}/updaters")
@Produces(MediaType.APPLICATION_JSON)
public class UpdaterStatusResource {

    private static final Logger LOG = LoggerFactory.getLogger(UpdaterStatusResource.class);

    /** Choose short or long form of results. */
    @QueryParam("detail") private boolean detail = false;

    Router router;

    public UpdaterStatusResource (@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        router = otpServer.getRouter(routerId);
    }

    /** Return a list of all agencies in the graph. */
    @GET
    public Response getUpdaters () {
        GraphUpdaterManager updaterManager = router.graph.updaterManager;
        if (updaterManager == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("No updaters running.").build();
        }
        return Response.status(Response.Status.OK).entity(updaterManager.getUpdaterDescriptions()).build();
    }

    /** Return status for a specific updater. */
    @GET
    @Path("/{updaterId}")
    public Response getUpdaters (@PathParam("updaterId") int updaterId) {
        GraphUpdaterManager updaterManager = router.graph.updaterManager;
        if (updaterManager == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("No updaters running.").build();
        }
        GraphUpdater updater = updaterManager.getUpdater(updaterId);
        if (updater == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("No updater with that ID.").build();
        }
        return Response.status(Response.Status.OK).entity(updater.getClass()).build();
    }

}
