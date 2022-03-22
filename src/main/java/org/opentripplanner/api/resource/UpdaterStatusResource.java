package org.opentripplanner.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;

/**
 * Report the status of the graph updaters via a web service.
 */
@SuppressWarnings("FieldMayBeFinal")
@Path("/routers/{ignoreRouterId}/updaters")
@Produces(MediaType.APPLICATION_JSON)
public class UpdaterStatusResource {

    /** Choose short or long form of results. */
    @QueryParam("detail") private boolean detail = false;


    private final Router router;

    public UpdaterStatusResource (
            @Context OTPServer otpServer,
            /**
             * @deprecated The support for multiple routers are removed from OTP2.
             * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
             */
            @Deprecated @PathParam("ignoreRouterId") String ignoreRouterId
    ) {
        router = otpServer.getRouter();
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
