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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
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
import org.onebusaway.gtfs.model.Trip;

import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTEdge;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.edgetype.Turn;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.util.PolylineEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.spring.Autowire;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

// NOTE - /ws/plan is the full path -- see web.xml

@Path("/plan")
@XmlRootElement
@Autowire
public class Planner {

    private static final Logger LOGGER = Logger.getLogger(Planner.class.getCanonicalName());

    private PathService pathservice;

    @Autowired
    public void setPathService(PathService pathService) {
        this.pathservice = pathService;
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
     *            format). <strong>Presently unused.</strong>
     * 
     * @param date
     *            The date that the trip should depart (or arrive, for requests where arriveBy is
     *            true).
     * 
     * @param time
     *            The time that the trip should depart (or arrive, for requests where arriveBy is
     *            true).
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
     * @param optimize
     *            The set of characteristics that the user wants to optimize for. @See OptimizeType
     * 
     * @param modes
     *            The set of modes that a user is willing to use.
     * 
     * @param max
     *            The maximum number of possible itineraries to return.
     * 
     * @return Returns either an XML or a JSON document, depending on the HTTP Accept header of the
     *         client making the request.
     * 
     * @throws JSONException
     */
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response getItineraries(@QueryParam(RequestInf.FROM) String fromPlace,
            @QueryParam(RequestInf.TO) String toPlace,
            @QueryParam(RequestInf.INTERMEDIATE_PLACES) List<String> intermediatePlaces,
            @QueryParam(RequestInf.DATE) String date, @QueryParam(RequestInf.TIME) String time,
            @DefaultValue("false") @QueryParam(RequestInf.ARRIVE_BY) Boolean arriveBy,
            @DefaultValue("false") @QueryParam(RequestInf.WHEELCHAIR) Boolean wheelchair,
            @DefaultValue("800") @QueryParam(RequestInf.MAX_WALK_DISTANCE) Double maxWalkDistance,
            @DefaultValue("1.33") @QueryParam(RequestInf.WALK_SPEED) Double walkSpeed,
            @DefaultValue("QUICK") @QueryParam(RequestInf.OPTIMIZE) OptimizeType optimize,
            @DefaultValue("TRANSIT,WALK") @QueryParam(RequestInf.MODE) TraverseModeSet modes,
            @DefaultValue("3") @QueryParam(RequestInf.NUMBER_ITINERARIES) Integer max)
            throws JSONException {

        // TODO: add Lang / Locale parameter, and thus get localized content  (Messages & more...)

        // TODO: test inputs, and prepare an error if we can't use said input.
        // TODO: from/to inputs should be converted / geocoded / etc... here, and maybe send coords / vertext ids to planner (or error back to user)
        // TODO: org.opentripplanner.routing.impl.PathServiceImpl has COOORD parsing.  Abstrct that out so it's used here too...

        /* create request */
        Request request = new Request();
        request.setFrom(fromPlace);
        request.setTo(toPlace);
        request.setDateTime(date, time);
        request.setWheelchair(wheelchair);

        if (max != null)
            request.setNumItineraries(max);
        if (maxWalkDistance != null)
            request.setWalk(maxWalkDistance);
        if (arriveBy != null && arriveBy)
            request.setArriveBy(true);

        request.setOptimize(optimize);
        request.setModes(modes);

        /* use request to generate trip */
        Response response = new Response(request);
        try {
            TripPlan plan = generatePlan(request);
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
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "exception planning trip: ", e);
            PlannerError error = new PlannerError(Message.SYSTEM_ERROR);
            response.setError(error);
        }

        // TODO: TRANSIT TRIP ERRORS
        //If no paths are returned, and the trip is transit, look at the date & time parameters, to see if that was cause for an issue (eg: schedule calendar might be outside of service dates; service times; etc...)
        //If planed trip is just walking, and a transit trip is requested, and the walk itself is greater than the max walk requested, look at date & time parameters)

        return response;
    }

    /**
     * Generates a TripPlan from a Request;
     * 
     * @param request
     * @return
     */

    private TripPlan generatePlan(Request request) {
        TraverseModeSet modeSet = request.getModeSet();
        assert (modeSet.isValid());
        TraverseOptions options = new TraverseOptions(modeSet);
        options.optimizeFor = request.getOptimize();
        options.back = request.isArriveBy();
        options.wheelchairAccessible = request.getWheelchair();
        if (request.getMaxSlope() > 0) { 
            options.maxSlope = request.getMaxSlope();
        }
        if (request.getWheelchair()) {
            //check if the start and end locations are accessible
            if (!pathservice.isAccessible(request.getFrom(), options.maxSlope) || !pathservice.isAccessible(request.getTo(), options.maxSlope)) {
                throw new LocationNotAccessible();
            }
            
        }
        List<GraphPath> paths = null;
        boolean tooSloped = false;
        try {
            List<String> intermediates = request.getIntermediatePlaces();
            if (intermediates.size() == 0) {
                paths = pathservice.plan(request.getFrom(), request.getTo(), request.getDateTime(),
                        options);
                if (paths == null && request.getWheelchair()) {
                    //There are no paths that meet the user's slope restrictions.
                    //Try again without slope restrictions (and warn user).
                    options.maxSlope = Double.MAX_VALUE; 
                    paths = pathservice.plan(request.getFrom(), request.getTo(), 
                            request.getDateTime(), options);
                    tooSloped = true;
                }
            } else {
                paths = pathservice.plan(request.getFrom(), request.getTo(), intermediates, request
                        .getDateTime(), options);
            }
        } catch (VertexNotFoundException e) {
            LOGGER.log(Level.INFO, "Vertex not found: " + request.getFrom() + " : "
                    + request.getTo(), e);
            throw e;
        }
        if (paths == null || paths.size() == 0) {
            LOGGER
                    .log(Level.INFO, "Path not found: " + request.getFrom() + " : "
                            + request.getTo());
            throw new PathNotFoundException();
        }
        Vector<SPTVertex> vertices = paths.get(0).vertices;
        SPTVertex tripStartVertex = vertices.firstElement();
        SPTVertex tripEndVertex = vertices.lastElement();
        String startName = tripStartVertex.getName();
        String endName = tripEndVertex.getName();
        // Use vertex labels if they don't have names
        if (startName == null) {
            startName = tripStartVertex.getLabel();
        }
        if (endName == null) {
            endName = tripEndVertex.getLabel();
        }
        Place from = new Place(tripStartVertex.getX(), tripStartVertex.getY(), startName);
        Place to = new Place(tripEndVertex.getX(), tripEndVertex.getY(), endName);

        TripPlan plan = new TripPlan(from, to, request.getDateTime());

        for (GraphPath path : paths) {

            Itinerary itinerary = new Itinerary();
            plan.addItinerary(itinerary);

            SPTVertex startVertex = path.vertices.firstElement();
            State startState = startVertex.state;
            SPTVertex endVertex = path.vertices.lastElement();
            State endState = endVertex.state;

            itinerary.startTime = new Date(startState.getTime());
            itinerary.endTime = new Date(endState.getTime());
            itinerary.duration = endState.getTime() - startState.getTime();
            itinerary.fare = path.getCost();
            itinerary.transfers = -1;
            itinerary.tooSloped = tooSloped;
            
            Leg leg = null;
            TraverseMode mode = null;
            Geometry geometry = null;
            String name = null;

            int startWalk = -1;
            int i = -1;
            SPTEdge lastEdge = null;
            double lastElevation = Double.MAX_VALUE;
            for (SPTEdge edge : path.edges) {
                i++;
                Edge graphEdge = edge.payload;

                if (graphEdge instanceof Turn) {
                    continue;
                }
                lastEdge = edge;

                TraverseMode edgeMode = graphEdge.getMode();
                // special case for bicycling on Street edges, where mode cannot be deduced from
                // edge type
                if (graphEdge instanceof Street && modeSet.contains(TraverseMode.BICYCLE)) {
                    edgeMode = TraverseMode.BICYCLE;
                }
                double edgeTime = edge.tov.state.getTime() - edge.fromv.state.getTime();

                if (!edgeMode.isTransit() && edgeMode != TraverseMode.ALIGHTING) {
                    //change of mode
                    if (edgeMode != mode
                            || (!mode.isOnStreetNonTransit() && graphEdge.getName() != name)) {
                        name = graphEdge.getName();
                        if (leg != null) {
                            /* finalize leg */
                            if (startWalk != -1) {
                                leg.walkSteps = getWalkSteps(path.edges.subList(startWalk, i));
                            }
                            Vertex fromv = graphEdge.getFromVertex();
                            Coordinate endCoord = fromv.getCoordinate();
                            leg.to = new Place(endCoord.x, endCoord.y, fromv.getName());
                            leg.to.stopId = fromv.getStopId();
                            leg.endTime = new Date(edge.tov.state.getTime());
                            leg.legGeometry = PolylineEncoder.createEncodings(geometry);
                            leg.duration = edge.tov.state.getTime() - leg.startTime.getTime();
                            leg = null;
                            geometry = null;
                        }

                        /* initialize new leg */
                        leg = new Leg();
                        itinerary.addLeg(leg);

                        leg.startTime = new Date(edge.fromv.state.getTime());
                        leg.route = graphEdge.getName();
                        Trip trip = edge.getTrip();
                        if (trip != null) {
                            leg.headsign = trip.getTripHeadsign();
                            leg.agencyId = trip.getId().getAgencyId();
                        }
                        mode = edgeMode;
                        leg.mode = mode.toString();
                        if (mode == TraverseMode.WALK || mode == TraverseMode.BICYCLE) {
                            startWalk = i;
                        } else {
                            startWalk = -1;
                        }
                        leg.distance = 0.0;
                        Vertex fromv = graphEdge.getFromVertex();
                        Coordinate endCoord = fromv.getCoordinate();
                        leg.from = new Place(endCoord.x, endCoord.y, fromv.getName());
                        leg.from.stopId = fromv.getStopId();
                    }
                }
                Geometry edgeGeometry = graphEdge.getGeometry();
                if (geometry == null) {
                    geometry = edgeGeometry;
                } else {
                    if (edgeGeometry != null) {
                        geometry = geometry.union(edgeGeometry);
                    }
                }

                leg.distance += edge.getDistance();

                if (edgeMode == TraverseMode.TRANSFER) {
                    itinerary.walkTime += edgeTime;
                    itinerary.walkDistance += graphEdge.getDistance();
                    continue;
                }

                if (edgeMode == TraverseMode.BOARDING) {
                    itinerary.transfers++;
                    itinerary.waitingTime += edgeTime;
                    continue;
                }

                if (edgeMode == TraverseMode.WALK || edgeMode == TraverseMode.BICYCLE) {
                    itinerary.walkTime += edgeTime;
                    itinerary.walkDistance += graphEdge.getDistance();
                    if (graphEdge instanceof Street) {
                        PackedCoordinateSequence profile = ((Street)graphEdge).getElevationProfile();
                        if (profile != null) {
                            for (Coordinate coordinate : profile.toCoordinateArray()) {
                                if (lastElevation == Double.MAX_VALUE) {
                                    lastElevation = coordinate.y;
                                    continue;
                                }
                                double elevationChange = lastElevation - coordinate.y;
                                if (elevationChange > 0) {
                                    itinerary.elevationGained += elevationChange;
                                } else {
                                    itinerary.elevationLost -= elevationChange;
                                }
                                lastElevation = coordinate.y;
                            }
                        }
                    }
                }

                if (edgeMode.isTransit()) {
                    itinerary.transitTime += edgeTime;
                    mode = graphEdge.getMode();
                    leg.mode = mode.toString();
                    leg.route = graphEdge.getName();
                }
            }

            Edge graphEdge = lastEdge.payload;

            if (leg != null) {
                /* finalize leg */
                Vertex tov = graphEdge.getToVertex();
                Coordinate endCoord = tov.getCoordinate();
                leg.to = new Place(endCoord.x, endCoord.y, tov.getName());
                leg.to.stopId = tov.getStopId();
                leg.endTime = new Date(lastEdge.tov.state.getTime());
                leg.legGeometry = PolylineEncoder.createEncodings(geometry);
                leg.duration = lastEdge.tov.state.getTime() - leg.startTime.getTime();
                if (startWalk != -1) {
                    leg.walkSteps = getWalkSteps(path.edges.subList(startWalk, i + 1));
                }
                leg = null;
            }
            if (itinerary.transfers == -1) {
                itinerary.transfers = 0;
            }
        }
        return plan;
    }

    /**
     * Converts a list of street edges to a list of turn-by-turn directions.
     * 
     * @param edges
     *            : A list of street edges
     * @return
     */
    private List<WalkStep> getWalkSteps(List<SPTEdge> edges) {

        List<WalkStep> steps = new ArrayList<WalkStep>();

        WalkStep step = null;

        double lastAngle = 0, distance = 0;

        for (SPTEdge sptEdge : edges) {
            Edge edge = sptEdge.payload;
            if (edge instanceof Turn) {
                // Turns do not exist outside of routing
                continue;
            }
            Geometry geom = edge.getGeometry();
            if (geom == null) {
                continue;
            }
            String streetName = edge.getName();
            if (step == null) {
                // first step
                step = new WalkStep();
                steps.add(step);
                step.streetName = streetName;
                step.lon = edge.getFromVertex().getX();
                step.lat = edge.getFromVertex().getY();
                double thisAngle = DirectionUtils.getInstance().getFirstAngle(geom);
                step.setAbsoluteDirection(thisAngle);
                step.elevation = encodeElevationProfile(edge, 0);
                distance = edge.getDistance();
            } else if (step.streetName != streetName
                    && (step.streetName != null && !step.streetName.equals(streetName))) {
                // change of street name
                step = new WalkStep();
                steps.add(step);
                step.streetName = streetName;
                double thisAngle = DirectionUtils.getInstance().getFirstAngle(geom);
                step.setDirections(lastAngle, thisAngle);
                step.lon = edge.getFromVertex().getX();
                step.lat = edge.getFromVertex().getY();
                step.becomes = !multipleOptionsBefore(edge);
                step.elevation = encodeElevationProfile(edge, 0);
                distance = edge.getDistance();
            } else {
                /* generate turn-to-stay-on directions, where needed */
                double thisAngle = DirectionUtils.getInstance().getFirstAngle(geom);
                RelativeDirection direction = WalkStep.getRelativeDirection(lastAngle, thisAngle);
                
                if (direction == RelativeDirection.CONTINUE) {
                    // append elevation info
                    if(step.elevation != null) {
                        if(step.elevation.length() > 0) step.elevation += ",";
                        step.elevation += encodeElevationProfile(edge, distance);
                    }
                    distance += edge.getDistance();
                }
                else {
                //if (direction != RelativeDirection.CONTINUE) {
                    // figure out if there was another way we could have turned
                    boolean optionsBefore = multipleOptionsBefore(edge);
                    if (optionsBefore) {
                        step = new WalkStep();
                        steps.add(step);
                        step.streetName = streetName;
                        step.setDirections(lastAngle, thisAngle);
                        step.stayOn = true;
                        step.lon = edge.getFromVertex().getX();
                        step.lat = edge.getFromVertex().getY();
                        step.elevation = encodeElevationProfile(edge, 0);
                        distance = edge.getDistance();
                    }
                }
                
            }

            step.distance += edge.getDistance();

            lastAngle = DirectionUtils.getInstance().getLastAngle(geom);
        }
        return steps;
    }

    private String encodeElevationProfile(Edge edge, double offset) {
        if(!(edge instanceof Street)) {
            return "";
        }
        if(((Street) edge).getElevationProfile() == null) {
            return "";
        }
        String str = "";
        Coordinate[] coordArr = ((Street) edge).getElevationProfile().toCoordinateArray();
        for(int i=0; i<coordArr.length; i++) {
            str+= Math.round(coordArr[i].x+offset) + "," + Math.round(coordArr[i].y*10.0)/10.0 + 
                  (i < coordArr.length-1 ? "," : "");
        }
        return str;
    }
    
    private boolean multipleOptionsBefore(Edge edge) {
        boolean foundAlternatePaths = false;
        Vertex start = edge.getFromVertex();
        for (Edge out : start.getOutgoing()) {
            if (out == edge) {
                continue;
            }
            if (!(out instanceof Turn)) {
                continue;
            }
            // there were paths we didn't take.
            foundAlternatePaths = true;
            break;
        }
        return foundAlternatePaths;
    }
}
