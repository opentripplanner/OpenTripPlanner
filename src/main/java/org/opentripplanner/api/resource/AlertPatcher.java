package org.opentripplanner.api.resource;

import static org.opentripplanner.api.resource.ServerInfo.Q;

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
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.api.model.alertpatch.AlertPatchCreationResponse;
import org.opentripplanner.api.model.alertpatch.AlertPatchResponse;
import org.opentripplanner.api.model.alertpatch.AlertPatchSet;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.services.AlertPatchService;

@Path("/patch")
@XmlRootElement
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
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public AlertPatchResponse getStopPatches(@QueryParam("agency") String agency,
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
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public AlertPatchResponse getRoutePatches(@QueryParam("agency") String agency,
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
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
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
