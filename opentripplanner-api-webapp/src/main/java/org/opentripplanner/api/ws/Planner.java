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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONException;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.services.PathServiceFactory;
import org.springframework.beans.factory.annotation.Required;

import com.sun.jersey.api.spring.Autowire;

// NOTE - /ws/plan is the full path -- see web.xml

@Path("/plan")
@XmlRootElement
@Autowire
public class Planner {

    private static final Logger LOGGER = Logger.getLogger(Planner.class.getCanonicalName());

    private static final int MAX_ITINERARIES = 3;
    private static final int MAX_TRANSFERS = 4;

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
     * @param fromPlace
     *            The start location -- either latitude, longitude pair in degrees or a Vertex
     *            label. For example, <code>40.714476,-74.005966</code> or
     *            <code>mtanyctsubway_A27_S</code>.
     * 
     * @param toPlace
     *            The end location (see fromPlace for format).
     * 
     * @param intermediatePlaces
     *            An unordered list of intermediate locations to be visited (see the fromPlace for
     *            format).
     * 
     * @param date
     *            The date that the trip should depart (or arrive, for requests where arriveBy is
     *            true).
     * 
     * @param time
     *            The time that the trip should depart (or arrive, for requests where arriveBy is
     *            true).
     * 
     * @param routerId
     *            Router ID used when in multiple graph mode. Unused in singleton graph mode.
     * 
     * @param arriveBy
     *            Whether the trip should depart or arrive at the specified date and time.
     * 
     * @param wheelchair
     *            Whether the trip must be wheelchair accessible.
     * 
     * @param maxWalkDistance
     *            The maximum distance (in meters) the user is willing to walk. Defaults to
     *            approximately 1/2 mile.
     * 
     * @param walkSpeed
     *            The user's walking speed in meters/second. Defaults to approximately 3 MPH.
     *
     * @param triangleSafetyFactor
     *            For bike triangle routing, how much safety matters (range 0-1).
     *
     * @param triangleSlopeFactor
     *            For bike triangle routing, how much slope matters (range 0-1).
     *
     * @param triangleTimeFactor
     *            For bike triangle routing, how much time matters (range 0-1).            
     * 
     * @param optimize
     *            The set of characteristics that the user wants to optimize for. @See OptimizeType
     * 
     * @param modes
     *            The set of modes that a user is willing to use.
     * 
     * @param numItineraries
     *            The maximum number of possible itineraries to return.
     *            
     * @param preferredRoutes
     *            The list of preferred routes.
     * 
     * @param unpreferredRoutes
     *            The list of unpreferred routes.
     * 
     * @return Returns either an XML or a JSON document, depending on the HTTP Accept header of the
     *         client making the request.
     * 
     * @throws JSONException
     */
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response getItineraries(
            @QueryParam(RequestInf.FROM) String fromPlace,
            @QueryParam(RequestInf.TO) String toPlace,
            @QueryParam(RequestInf.INTERMEDIATE_PLACES) List<String> intermediatePlaces,
            @DefaultValue("false") @QueryParam(RequestInf.INTERMEDAITE_PLACES_ORDERED) Boolean intermediatePlacesOrdered,
            @QueryParam(RequestInf.DATE) String date,
            @QueryParam(RequestInf.TIME) String time,
            @DefaultValue("") @QueryParam(RequestInf.ROUTER_ID) String routerId,
            @DefaultValue("false") @QueryParam(RequestInf.ARRIVE_BY) Boolean arriveBy,
            @DefaultValue("false") @QueryParam(RequestInf.WHEELCHAIR) Boolean wheelchair,
            @DefaultValue("800") @QueryParam(RequestInf.MAX_WALK_DISTANCE) Double maxWalkDistance,
            @QueryParam(RequestInf.WALK_SPEED) Double walkSpeed,
            @QueryParam(RequestInf.TRIANGLE_SAFETY_FACTOR) Double triangleSafetyFactor,
            @QueryParam(RequestInf.TRIANGLE_SLOPE_FACTOR) Double triangleSlopeFactor,
            @QueryParam(RequestInf.TRIANGLE_TIME_FACTOR) Double triangleTimeFactor,
            @DefaultValue("QUICK") @QueryParam(RequestInf.OPTIMIZE) OptimizeType optimize,
            @DefaultValue("TRANSIT,WALK") @QueryParam(RequestInf.MODE) TraverseModeSet modes,
            @DefaultValue("240") @QueryParam(RequestInf.MIN_TRANSFER_TIME) Integer minTransferTime,
            @DefaultValue("3") @QueryParam(RequestInf.NUMBER_ITINERARIES) Integer numItineraries,
            @DefaultValue("false") @QueryParam(RequestInf.SHOW_INTERMEDIATE_STOPS) Boolean showIntermediateStops,
            @DefaultValue("") @QueryParam(RequestInf.PREFERRED_ROUTES) String preferredRoutes,
            @DefaultValue("") @QueryParam(RequestInf.UNPREFERRED_ROUTES) String unpreferredRoutes,
            @DefaultValue("") @QueryParam(RequestInf.BANNED_ROUTES) String bannedRoutes,
            @DefaultValue("0") @QueryParam(RequestInf.TRANSFER_PENALTY) Integer transferPenalty,
            @DefaultValue("2") @QueryParam(RequestInf.MAX_TRANSFERS) Integer maxTransfers)
            throws JSONException {

        // TODO: add Lang / Locale parameter, and thus get localized content (Messages & more...)

        // TODO: test inputs, and prepare an error if we can't use said input.
        // TODO: from/to inputs should be converted / geocoded / etc... here, and maybe send coords
        // / vertext ids to planner (or error back to user)
        // TODO: org.opentripplanner.routing.impl.PathServiceImpl has COOORD parsing. Abstrct that
        // out so it's used here too...

        /* create request */
        Request request = new Request();
        request.setRouterId(routerId);
        request.setFrom(fromPlace);
        request.setTo(toPlace);
        request.setDateTime(date, time);
        request.setWheelchair(wheelchair);
        if (numItineraries != null) {
            if (numItineraries > MAX_ITINERARIES) {
                numItineraries = MAX_ITINERARIES;
            }
            if (numItineraries < 1) {
                numItineraries = 1;
            }
            request.setNumItineraries(numItineraries);
        }
        if (maxWalkDistance != null) {
            request.setMaxWalkDistance(maxWalkDistance);
        }
        if (walkSpeed != null) {
            request.setWalkSpeed(walkSpeed);
        }
        if (triangleSafetyFactor != null || triangleSlopeFactor != null || triangleTimeFactor != null) {
            if (triangleSafetyFactor == null || triangleSlopeFactor == null || triangleTimeFactor == null) {
                return error(request, Message.UNDERSPECIFIED_TRIANGLE);
            }
            if (optimize == null) {
                optimize = OptimizeType.TRIANGLE;
            }
            if (optimize != OptimizeType.TRIANGLE) {
                return error(request, Message.TRIANGLE_OPTIMIZE_TYPE_NOT_SET);
            }
            if (Math.abs(triangleSafetyFactor + triangleSlopeFactor + triangleTimeFactor - 1) > Math.ulp(1) * 3) {
                return error(request, Message.TRIANGLE_NOT_AFFINE);
            }
            
            request.setTriangleSafetyFactor(triangleSafetyFactor);
            request.setTriangleSlopeFactor(triangleSlopeFactor);
            request.setTriangleTimeFactor(triangleTimeFactor);
        } else if (optimize == OptimizeType.TRIANGLE) {
            return error(request, Message.TRIANGLE_VALUES_NOT_SET);
        }
        if (arriveBy != null && arriveBy) {
            request.setArriveBy(true);
        }
        if (showIntermediateStops != null && showIntermediateStops) {
            request.setShowIntermediateStops(true);
        }
        if (intermediatePlaces != null && intermediatePlaces.size() > 0
                && !intermediatePlaces.get(0).equals("")) {
            request.setIntermediatePlaces(intermediatePlaces);
        }
        if (intermediatePlacesOrdered != null) {
            request.setIntermediatePlacesOrdered(intermediatePlacesOrdered);
        }
        if (preferredRoutes != null && !preferredRoutes.equals("")) {
            String[] table = preferredRoutes.split(",");
            request.setPreferredRoutes(table);
        }
        if (unpreferredRoutes != null && !unpreferredRoutes.equals("")) {
            String[] table = unpreferredRoutes.split(",");
            request.setUnpreferredRoutes(table);
        }
        if (bannedRoutes != null && !bannedRoutes.equals("")) {
            String[] table = bannedRoutes.split(",");
            request.setBannedRoutes(table);
        }

        //replace deprecated optimization preference
        if (optimize == OptimizeType.TRANSFERS) {
            optimize = OptimizeType.QUICK;
            transferPenalty += 1800;
        }
        request.setTransferPenalty(transferPenalty);
        request.setOptimize(optimize);
        request.setModes(modes);
        request.setMinTransferTime(minTransferTime);

        if (maxTransfers != null) {
            if (maxTransfers > MAX_TRANSFERS) {
                maxTransfers = MAX_TRANSFERS;
            }
            request.setMaxTransfers(maxTransfers);
        }
        /* use request to generate trip */
        Response response = new Response(request);
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
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "exception planning trip: ", e);
            PlannerError error = new PlannerError(Message.SYSTEM_ERROR);
            response.setError(error);
        }
        return response;
    }

    private Response error(Request request, Message message) {
        PlannerError error = new PlannerError(message);
        Response response = new Response(request);
        response.setError(error);
        return response;
    }

    
}
