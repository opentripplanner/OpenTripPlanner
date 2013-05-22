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
package org.opentripplanner.api.ws;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONException;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.routing.core.RoutingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.spring.Autowire;

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
 * 
 * @throws JSONException
 */
@Path("/plan") // NOTE - /ws/plan is the full path. The prefix is added by the servlet's web.xml.
@XmlRootElement
@Autowire
public class Planner extends RoutingResource {

    private static final Logger LOG = LoggerFactory.getLogger(Planner.class);
    @Autowired public PlanGenerator planGenerator;
    // We inject info about the incoming request so we can include the incoming query 
    // parameters in the outgoing response. This is a TriMet requirement.
    @Context UriInfo uriInfo;
    
    /** Java is immensely painful. TODO: Guava should cover this. */
    interface OneArgFunc<T,U> {
        public T call(U arg);
    }
    private Response wrapGenerate(OneArgFunc<TripPlan, RoutingRequest> func) {

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
            TripPlan plan = func.call(request);
            response.setPlan(plan);
        } catch (Exception e) {
            PlannerError error = new PlannerError(e);
            e.printStackTrace();
            response.setError(error);
        } finally {
            if (request != null) 
                request.cleanup();
        }
        return response;
    }

    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response getItineraries() throws JSONException {
        return wrapGenerate(new OneArgFunc<TripPlan, RoutingRequest>() {
            public TripPlan call(RoutingRequest request) {
                return planGenerator.generate(request);
            }});
    }

    @GET
    @Path("/first")
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response getFirstTrip() throws JSONException {

        return wrapGenerate(new OneArgFunc<TripPlan, RoutingRequest>() {
            public TripPlan call(RoutingRequest request) {
                return planGenerator.generateFirstTrip(request);
            }});
    }

    @GET
    @Path("/last")
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response getLastTrip() throws JSONException {

        return wrapGenerate(new OneArgFunc<TripPlan, RoutingRequest>() {
            public TripPlan call(RoutingRequest request) {
                return planGenerator.generateLastTrip(request);
            }});
    }
}
