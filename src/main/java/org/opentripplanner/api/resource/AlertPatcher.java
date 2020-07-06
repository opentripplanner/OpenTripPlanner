package org.opentripplanner.api.resource;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.services.AlertPatchService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/patch")
public class AlertPatcher {

    @Context AlertPatchService alertPatchService; // FIXME inject Application

    /**
     * Return a list of all patches that apply to a given stop
     *
     * @return Returns either an XML or a JSON document, depending on the HTTP Accept header of the
     *         client making the request.
     */
    @GET
    @Path("/stopPatches")
    @Produces({ MediaType.APPLICATION_JSON })
    public Collection<AlertPatch> getStopPatches(@QueryParam("agency") String agency,
            @QueryParam("id") String id) {

        return alertPatchService.getStopPatches(new FeedScopedId(agency, id));
    }

    /**
     * Return a list of all patches that apply to a given route
     *
     * @return Returns either an XML or a JSON document, depending on the HTTP Accept header of the
     *         client making the request.
     */
    @GET
    @Path("/routePatches")
    @Produces({ MediaType.APPLICATION_JSON })
    public Collection<AlertPatch> getRoutePatches(@QueryParam("agency") String agency,
            @QueryParam("id") String id) {

        return alertPatchService.getRoutePatches(new FeedScopedId(agency, id));
    }
}
