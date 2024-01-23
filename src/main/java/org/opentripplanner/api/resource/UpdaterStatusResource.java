package org.opentripplanner.api.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.GraphUpdaterStatus;

/**
 * Report the status of the graph updaters via a web service.
 */
@SuppressWarnings("FieldMayBeFinal")
@Path("/routers/{ignoreRouterId}/updaters")
@Produces(MediaType.APPLICATION_JSON)
public class UpdaterStatusResource {

  private final TransitService transitService;

  public UpdaterStatusResource(
    @Context TransitService transitService,
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated @PathParam("ignoreRouterId") String ignoreRouterId
  ) {
    this.transitService = transitService;
  }

  /** Return a list of all agencies in the graph. */
  @GET
  public Response getUpdaters() {
    GraphUpdaterStatus updaterStatus = transitService.getUpdaterStatus();
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
    GraphUpdaterStatus updaterStatus = transitService.getUpdaterStatus();
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
