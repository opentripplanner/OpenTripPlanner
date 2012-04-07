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

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONException;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.ParameterException;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.TrivialPathException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.services.PathServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import com.sun.jersey.api.spring.Autowire;

// NOTE - /ws/plan is the full path -- see web.xml

@Path("/plan")
@XmlRootElement
@Autowire
public class Planner extends SearchResource {

    private static final Logger LOG = LoggerFactory.getLogger(Planner.class);

    private PathServiceFactory pathServiceFactory;

    @Required
    public void setPathServiceFactory(PathServiceFactory pathServiceFactory) {
        this.pathServiceFactory = pathServiceFactory;
    }

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

        /* create response object, containing a copy of all request parameters */
        Response response = new Response(httpServletRequest);

        /* create request */
        Request request;
        try {
            request = buildRequestFromQueryParamFields();
        } catch (ParameterException pe) {
            PlannerError error = new PlannerError(pe.message);
            response.setError(error);
            return response;
        }

        /* use request to generate trip */
        try {
            PlanGenerator generator = new PlanGenerator(request, pathServiceFactory);
            TripPlan plan = generator.generate();
            response.setPlan(plan);
        } catch (VertexNotFoundException e) {
            PlannerError error = new PlannerError(Message.OUTSIDE_BOUNDS);
            error.setMissing(e.getMissing());
            response.setError(error);
        } catch (PathNotFoundException e) {
            PlannerError error = new PlannerError(Message.PATH_NOT_FOUND);
            response.setError(error);
        } catch (LocationNotAccessible e) {
            PlannerError error = new PlannerError(Message.LOCATION_NOT_ACCESSIBLE);
            response.setError(error);
        } catch (TransitTimesException e) {
            // TODO: improve this to distinguish between days/places with no service
            // and dates outside those covered by the feed
            PlannerError error = new PlannerError(Message.NO_TRANSIT_TIMES);
            response.setError(error);
        } catch (TrivialPathException e) {
            PlannerError error = new PlannerError(Message.TOO_CLOSE);
            response.setError(error);
        } catch (Exception e) {
            LOG.error("exception planning trip: ", e);
            PlannerError error = new PlannerError(Message.SYSTEM_ERROR);
            response.setError(error);
        }
        return response;
    }

}
