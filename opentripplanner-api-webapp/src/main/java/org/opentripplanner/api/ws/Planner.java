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
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONException;
import org.opentripplanner.api.common.SearchResource;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.routing.core.TraverseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.sun.jersey.api.spring.Autowire;

@Path("/plan") // NOTE - /ws/plan is the full path -- see web.xml
@XmlRootElement
@Autowire
public class Planner extends SearchResource {

    private static final Logger LOG = LoggerFactory.getLogger(Planner.class);

    @Autowired private PlanGenerator planGenerator;

    /**
     * This is the primary entry point for the web service and is used for requesting trip plans.
     * All parameters are passed in the query string.
     * 
     * Some parameters may not be honored by the trip planner for some or all itineraries. For
     * example, maxWalkDistance may be relaxed if the alternative is to not provide a route.
     * 
     * @return Returns either an XML or a JSON document, depending on the HTTP Accept header of the
     *         client making the request.
     * 
     * @throws JSONException
     */
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response getItineraries() throws JSONException {

        // TODO: add Lang / Locale parameter, and thus get localized content (Messages & more...)
        // TODO: test inputs, and prepare an error if we can't use said input.
        // TODO: from/to inputs should be converted / geocoded / etc... here, and maybe send coords
        // / vertext ids to planner (or error back to user)
        // TODO: org.opentripplanner.routing.impl.PathServiceImpl has COOORD parsing. Abstract that
        // out so it's used here too...

        // create response object, containing a copy of all request parameters
        Response response = new Response(httpServletRequest);
        TraverseOptions request = null;
        try {
            // fill in request from query parameters via shared superclass method
            request = buildRequest(); 
            TripPlan plan = planGenerator.generate(request);
            response.setPlan(plan);
        } catch (Exception e) {
            PlannerError error = new PlannerError(e);
            response.setError(error);
        } finally {
            // tear down the routing context (remove temporary edges from edge lists)
            if (request != null && request.rctx != null) {
                int nRemoved = request.rctx.destroy();
                LOG.debug("routing context destroyed ({} temporary edges removed)", nRemoved);
            }
        }
        return response;
    }

}
