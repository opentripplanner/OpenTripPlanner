package org.opentripplanner.api.resource;


import java.util.Collection;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.api.model.alertpatch.AlertPatchCreationResponse;
import org.opentripplanner.api.model.alertpatch.AlertPatchResponse;
import org.opentripplanner.api.model.alertpatch.AlertPatchSet;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.services.AlertPatchService;

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
    public AlertPatchResponse getStopPatches(@QueryParam("agency") String agency,
    @Produces({ MediaType.APPLICATION_JSON })
            @QueryParam("id") String id) {

        AlertPatchResponse response = new AlertPatchResponse();
        Collection<AlertPatch> alertPatches = alertPatchService.getStopPatches(new FeedScopedId(agency, id));
        for (AlertPatch alertPatch : alertPatches) {
            response.addAlertPatch(alertPatch);
        }
        return response;
    }

    /**
     * Return a list of all patches that apply to a given route
     *
     * @return Returns either an XML or a JSON document, depending on the HTTP Accept header of the
     *         client making the request.
     */
    @GET
    @Path("/routePatches")
    public AlertPatchResponse getRoutePatches(@QueryParam("agency") String agency,
    @Produces({ MediaType.APPLICATION_JSON })
            @QueryParam("id") String id) {

        AlertPatchResponse response = new AlertPatchResponse();
        Collection<AlertPatch> alertPatches =
                alertPatchService.getRoutePatches(new FeedScopedId(agency, id));
        for (AlertPatch alertPatch : alertPatches) {
            response.addAlertPatch(alertPatch);
        }
        return response;
    }

    @RolesAllowed("user")
    @POST
    @Path("/patch")
    public AlertPatchCreationResponse createPatches(AlertPatchSet alertPatches) {
        AlertPatchCreationResponse response = new AlertPatchCreationResponse();
        for (AlertPatch alertPatch : alertPatches.alertPatches) {
            if (alertPatch.getId() == null) {
                response.status = "Every patch must have an id";
                return response;
            }

            final FeedScopedId route = alertPatch.getRoute();
            if (route != null && route.getId().equals("")) {
                response.status = "Every route patch must have a route id";
                return response;
            }
        }
        for (AlertPatch alertPatch : alertPatches.alertPatches) {
            alertPatchService.apply(alertPatch);
        }
        response.status = "OK";
        return response;
    }
}
