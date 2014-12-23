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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the primary entry point for the trip planning web service.
 * All parameters are passed in the query string. These parameters are defined in the abstract
 * SearchResource superclass, which also has methods for building routing requests from query
 * parameters. In order for inheritance to work, the REST resources are actually request-scoped 
 * rather than singleton-scoped.
 * 
 * Some parameters may not be honored by the trip planner for some or all itineraries. For
 * example, maxWalkDistance may be relaxed if the alternative is to not provide a route.
 * 
 * @return Returns either an XML or a JSON document, depending on the HTTP Accept header of the
 *         client making the request.
 */
@Path("routers/{routerId}/plan") // final element needed here rather than on method to distinguish from routers API
public class Planner extends RoutingResource {

    private static final Logger LOG = LoggerFactory.getLogger(Planner.class);

    // We inject info about the incoming request so we can include the incoming query
    // parameters in the outgoing response. This is a TriMet requirement.
    // Jersey uses @Context to inject internal types and @InjectParam or @Resource for DI objects.
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public Response getItineraries(@Context OTPServer otpServer, @Context UriInfo uriInfo) {

        /*
         * TODO: add Lang / Locale parameter, and thus get localized content (Messages & more...)
         * TODO: from/to inputs should be converted / geocoded / etc... here, and maybe send coords 
         *       or vertex ids to planner (or error back to user)
         * TODO: org.opentripplanner.routing.impl.PathServiceImpl has COOORD parsing. Abstract that
         *       out so it's used here too...
         */
        
        // create response object, containing a copy of all request parameters
        Response response = new Response(uriInfo);
        RoutingRequest request = null;
        try {
            // fill in request from query parameters via shared superclass method
            request = super.buildRequest();
            Router router = otpServer.getRouter(request.routerId);
            TripPlan plan = router.planGenerator.generate(request);
            response.setPlan(plan);
        } catch (Exception e) {
            PlannerError error = new PlannerError(e);
            if(!PlannerError.isPlanningError(e.getClass()))
                LOG.warn("Error while planning path: ", e);
            response.setError(error);
        } finally {
            if (request != null) {
                response.debugOutput = request.rctx.debugOutput;
                request.cleanup(); // TODO verify that this is being done on Analyst web services
            }       
        }
        return response;
    }

}
