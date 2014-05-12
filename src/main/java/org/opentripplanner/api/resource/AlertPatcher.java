/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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

import org.onebusaway.gtfs.model.AgencyAndId;
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
        Collection<AlertPatch> alertPatches = alertPatchService.getStopPatches(new AgencyAndId(agency, id));
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
                alertPatchService.getRoutePatches(new AgencyAndId(agency, id));
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

            final AgencyAndId route = alertPatch.getRoute();
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
