package org.opentripplanner.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.updater.GraphUpdaterStatus;

/**
 * Report the status of the graph updaters via a web service.
 */
@SuppressWarnings("FieldMayBeFinal")
@Path("/routers/{ignoreRouterId}/updaters")
@Produces(MediaType.APPLICATION_JSON)
public class UpdaterStatusResource {

  private final OtpServerRequestContext serverContext;

  public UpdaterStatusResource(
    @Context OtpServerRequestContext serverContext,
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated @PathParam("ignoreRouterId") String ignoreRouterId
  ) {
    this.serverContext = serverContext;
  }

  /** Return a list of all agencies in the graph. */
  @GET
  public Response getUpdaters() {
    GraphUpdaterStatus updaterStatus = serverContext.transitService().getUpdaterStatus();
    if (updaterStatus == null) {
      return Response.status(Response.Status.NOT_FOUND).entity("No updaters running.").build();
    }
    return Response
      .status(Response.Status.OK)
      .entity(updaterStatus.getUpdaterDescriptions())
      .build();
  }

  /** Return status for a specific updater. */
  @GET
  @Path("/{updaterId}")
  public Response getUpdaters(@PathParam("updaterId") int updaterId) {
    GraphUpdaterStatus updaterStatus = serverContext.transitService().getUpdaterStatus();
    if (updaterStatus == null) {
      return Response.status(Response.Status.NOT_FOUND).entity("No updaters running.").build();
    }
    Class<?> updater = updaterStatus.getUpdaterClass(updaterId);
    if (updater == null) {
      return Response.status(Response.Status.NOT_FOUND).entity("No updater with that ID.").build();
    }
    return Response.status(Response.Status.OK).entity(updater).build();
  }
}
