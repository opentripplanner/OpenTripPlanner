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
import org.opentripplanner.routing.edgetype.EdgeWithElevation;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.util.PolylineEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.spring.Autowire;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

// NOTE - /ws/plan is the full path -- see web.xml

@Path("/plan")
@XmlRootElement
@Autowire
public class Planner {

    private static final Logger LOGGER = Logger.getLogger(Planner.class.getCanonicalName());

    private static final int MAX_ITINERARIES = 3;

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
     * @param numItineraries
     *            The maximum number of possible itineraries to return.
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
            @QueryParam(RequestInf.DATE) String date,
            @QueryParam(RequestInf.TIME) String time,
            @DefaultValue("false") @QueryParam(RequestInf.ARRIVE_BY) Boolean arriveBy,
            @DefaultValue("false") @QueryParam(RequestInf.WHEELCHAIR) Boolean wheelchair,
            @DefaultValue("800") @QueryParam(RequestInf.MAX_WALK_DISTANCE) Double maxWalkDistance,
            @DefaultValue("1.33") @QueryParam(RequestInf.WALK_SPEED) Double walkSpeed,
            @DefaultValue("QUICK") @QueryParam(RequestInf.OPTIMIZE) OptimizeType optimize,
            @DefaultValue("TRANSIT,WALK") @QueryParam(RequestInf.MODE) TraverseModeSet modes,
            @DefaultValue("3") @QueryParam(RequestInf.NUMBER_ITINERARIES) Integer numItineraries,
            @DefaultValue("false") @QueryParam(RequestInf.SHOW_INTERMEDIATE_STOPS) Boolean showIntermediateStops)
            throws JSONException {

        // TODO: add Lang / Locale parameter, and thus get localized content (Messages & more...)

        // TODO: test inputs, and prepare an error if we can't use said input.
        // TODO: from/to inputs should be converted / geocoded / etc... here, and maybe send coords
        // / vertext ids to planner (or error back to user)
        // TODO: org.opentripplanner.routing.impl.PathServiceImpl has COOORD parsing. Abstrct that
        // out so it's used here too...

        /* create request */
        Request request = new Request();
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
        // If no paths are returned, and the trip is transit, look at the date & time parameters, to
        // see if that was cause for an issue (eg: schedule calendar might be outside of service
        // dates; service times; etc...)
        // If planned trip is just walking, and a transit trip is requested, and the walk itself is
        // greater than the max walk requested, look at date & time parameters)

        return response;
    }

    /**
     * Generates a TripPlan from a Request;
     * 
     * @param the
     *            request
     */
    private TripPlan generatePlan(Request request) {

        TraverseOptions options = getOptions(request);

        checkLocationsAccessible(request, options);

        /* try to plan the trip */
        List<GraphPath> paths = null;
        boolean tooSloped = false;
        try {
            List<String> intermediates = request.getIntermediatePlaces();
            if (intermediates.size() == 0) {
                paths = pathservice.plan(request.getFrom(), request.getTo(), request.getDateTime(),
                        options, request.getNumItineraries());
                if (paths == null && request.getWheelchair()) {
                    // There are no paths that meet the user's slope restrictions.
                    // Try again without slope restrictions (and warn user).
                    options.maxSlope = Double.MAX_VALUE;
                    paths = pathservice.plan(request.getFrom(), request.getTo(), request
                            .getDateTime(), options, request.getNumItineraries());
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

        TripPlan plan = generatePlan(paths, request, options);
        if (plan != null) {
            for (Itinerary i : plan.itinerary) {
                i.tooSloped = tooSloped;
            }
        }
        return plan;
    }

    /**
     * Generates a TripPlan from a set of paths
     */
    public TripPlan generatePlan(List<GraphPath> paths, Request request, TraverseOptions options) {

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
            Itinerary itinerary = generateItinerary(path, request.getShowIntermediateStops(),
                    options.modes.contains(TraverseMode.BICYCLE));
            plan.addItinerary(itinerary);
        }
        return plan;
    }

    /**
     * Generate an itinerary from a @{link GraphPath}. The algorithm here is to walk over each edge
     * in the graph path, accumulating geometry, time, and length data. On mode change, a new leg is
     * generated.  Street legs undergo an additional processing step to generate turn-by-turn directions.
     * 
     * @param path
     * @param showIntermediateStops whether intermediate stops are included in the generated itinerary
     * @param options
     * @return
     */
    private Itinerary generateItinerary(GraphPath path, boolean showIntermediateStops,
            boolean biking) {

        GeometryFactory geometryFactory = new GeometryFactory();

        Itinerary itinerary = makeItinerary(path);

        Leg leg = null;
        TraverseMode mode = null;
        CoordinateArrayListSequence coordinates = new CoordinateArrayListSequence();
        String name = null;

        int startWalk = -1;
        int i = -1;
        SPTEdge previousEdge = null;
        SPTEdge finalEdge = path.edges.lastElement();
        double lastElevation = Double.MAX_VALUE;
        for (SPTEdge sptEdge : path.edges) {
            i++;
            Edge edge = sptEdge.payload;

            if (edge instanceof FreeEdge && sptEdge != finalEdge) {
                continue;
            }
            previousEdge = sptEdge;

            TraverseMode edgeMode = edge.getMode();

            // special case for bicycling on Street edges, where mode cannot be deduced from
            // edge type
            if (isStreetEdge(edge) && biking) {
                edgeMode = TraverseMode.BICYCLE;
            }

            double edgeTime = sptEdge.tov.state.getTime() - sptEdge.fromv.state.getTime();

            if (!edgeMode.isTransit() && edgeMode != TraverseMode.ALIGHTING) {
                if (edgeMode != mode || (!mode.isOnStreetNonTransit() && edge.getName() != name)) {
                    // change of mode or street name
                    name = edge.getName();
                    if (leg != null) {
                        /* finalize prior leg */
                        if (startWalk != -1) {
                            leg.walkSteps = getWalkSteps(path.edges.subList(startWalk, i));
                        }
                        leg.to = makePlace(edge.getFromVertex());

                        leg.endTime = new Date(sptEdge.fromv.state.getTime());
                        Geometry geometry = geometryFactory.createLineString(coordinates);
                        leg.legGeometry = PolylineEncoder.createEncodings(geometry);
                        
                        leg = null;
                        coordinates = new CoordinateArrayListSequence();
                    }

                    /* initialize new leg */
                    mode = edgeMode;
                    leg = makeLeg(sptEdge, mode);
                    itinerary.addLeg(leg);

                    if (mode == TraverseMode.WALK || mode == TraverseMode.BICYCLE) {
                        startWalk = i;
                    } else {
                        startWalk = -1;
                    }
                }
            }
            Geometry edgeGeometry = edge.getGeometry();

            if (edgeGeometry != null) {
                Coordinate[] edgeCoordinates = edgeGeometry.getCoordinates();
                if (coordinates.size() > 0
                        && coordinates.getCoordinate(coordinates.size() - 1).equals(
                                edgeCoordinates[0])) {
                    coordinates.extend(edgeCoordinates, 1);
                } else {
                    coordinates.extend(edgeCoordinates);
                }
            }

            leg.distance += sptEdge.getDistance();

            if (edgeMode == TraverseMode.TRANSFER) {
                itinerary.walkTime += edgeTime;
                itinerary.walkDistance += edge.getDistance();
                continue;
            } else if (edgeMode == TraverseMode.BOARDING) {
                itinerary.transfers++;
                itinerary.waitingTime += edgeTime;
                continue;
            } else if (edgeMode == TraverseMode.WALK || edgeMode == TraverseMode.BICYCLE) {
                itinerary.walkTime += edgeTime;
                itinerary.walkDistance += edge.getDistance();
                if (edge instanceof EdgeWithElevation) {
                    PackedCoordinateSequence profile = ((EdgeWithElevation) edge)
                            .getElevationProfile();
                    lastElevation = applyElevation(profile, itinerary, lastElevation);
                }
            } else if (edgeMode.isTransit()) {
                itinerary.transitTime += edgeTime;
                mode = edge.getMode();
                leg.mode = mode.toString();
                leg.route = edge.getName();
                if (showIntermediateStops) {
                    /* add intermediate stop to current leg */
                    if (leg.stop == null) {
                        // first transit edge, just create the list (the initial stop is current
                        // "from" vertex)
                        leg.stop = new ArrayList<Place>();
                    } else {
                        // any further transit edge, add "from" vertex to intermediate stops
                        Place stop = makePlace(sptEdge.fromv.mirror);
                        leg.stop.add(stop);
                    }
                }
            }
        }

        Edge graphEdge = previousEdge.payload;

        if (leg != null) {
            /* finalize leg */
            leg.to = makePlace(graphEdge.getToVertex());
            leg.endTime = new Date(previousEdge.tov.state.getTime());
            Geometry geometry = geometryFactory.createLineString(coordinates);
            leg.legGeometry = PolylineEncoder.createEncodings(geometry);

            if (startWalk != -1) {
                leg.walkSteps = getWalkSteps(path.edges.subList(startWalk, i + 1));
            }

            leg = null;
        }
        if (itinerary.transfers == -1) {
            itinerary.transfers = 0;
        }
        itinerary.removeBogusLegs();
        return itinerary;
    }

    /**
     * Adjusts an Itinerary's elevation fields from an elevation profile
     * 
     * @return the elevation at the end of the profile
     */
    private double applyElevation(PackedCoordinateSequence profile, Itinerary itinerary,
            double previousElevation) {
        if (profile != null) {
            for (Coordinate coordinate : profile.toCoordinateArray()) {
                if (previousElevation == Double.MAX_VALUE) {
                    previousElevation = coordinate.y;
                    continue;
                }
                double elevationChange = previousElevation - coordinate.y;
                if (elevationChange > 0) {
                    itinerary.elevationGained += elevationChange;
                } else {
                    itinerary.elevationLost -= elevationChange;
                }
                previousElevation = coordinate.y;
            }
        }
        return previousElevation;
    }

    /**
     * Makes a new empty leg from a starting edge
     */
    private Leg makeLeg(SPTEdge edge, TraverseMode mode) {
        Leg leg = new Leg();

        leg.startTime = new Date(edge.fromv.state.getTime());
        leg.route = edge.getName();
        Trip trip = edge.getTrip();
        if (trip != null) {
            leg.headsign = trip.getTripHeadsign();
            leg.agencyId = trip.getId().getAgencyId();
        }

        leg.mode = mode.toString();

        leg.distance = 0.0;
        leg.from = makePlace(edge.payload.getFromVertex());
        return leg;
    }

    /**
     * Makes a new empty Itinerary for a given path.
     * 
     * @return
     */
    private Itinerary makeItinerary(GraphPath path) {
        Itinerary itinerary = new Itinerary();

        SPTVertex startVertex = path.vertices.firstElement();
        State startState = startVertex.state;
        SPTVertex endVertex = path.vertices.lastElement();
        State endState = endVertex.state;

        itinerary.startTime = new Date(startState.getTime());
        itinerary.endTime = new Date(endState.getTime());
        itinerary.duration = endState.getTime() - startState.getTime();
        itinerary.fare = path.getCost();
        itinerary.transfers = -1;
        return itinerary;
    }

    /**
     * Makes a new Place from a vertex.
     * 
     * @return
     */
    private Place makePlace(Vertex vertex) {
        Coordinate endCoord = vertex.getCoordinate();
        Place place = new Place(endCoord.x, endCoord.y, vertex.getName());
        place.stopId = vertex.getStopId();
        return place;
    }

    /**
     * Throw an exception if the start and end locations are not wheelchair accessible given the
     * user's specified maximum slope.
     */
    private void checkLocationsAccessible(Request request, TraverseOptions options) {
        if (request.getWheelchair()) {
            // check if the start and end locations are accessible
            if (!pathservice.isAccessible(request.getFrom(), options)
                    || !pathservice.isAccessible(request.getTo(), options)) {
                throw new LocationNotAccessible();
            }

        }
    }

    private TraverseOptions getOptions(Request request) {

        TraverseModeSet modeSet = request.getModeSet();
        assert (modeSet.isValid());
        TraverseOptions options = new TraverseOptions(modeSet);
        options.optimizeFor = request.getOptimize();
        options.setArriveBy(request.isArriveBy());
        options.wheelchairAccessible = request.getWheelchair();
        if (request.getMaxSlope() > 0) {
            options.maxSlope = request.getMaxSlope();
        }
        if (request.getMaxWalkDistance() > 0) {
            options.maxWalkDistance = request.getMaxWalkDistance();
        }
        return options;
    }

    private boolean isStreetEdge(Edge edge) {
        return edge instanceof TurnEdge || edge instanceof FreeEdge || edge instanceof OutEdge;
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
            if (edge instanceof FreeEdge) {
                continue;
            }
            Geometry geom = edge.getGeometry();
            if (geom == null) {
                continue;
            }
            String streetName = edge.getName();
            if (step == null) {
                // first step
                step = createWalkStep(sptEdge);
                steps.add(step);

                double thisAngle = DirectionUtils.getFirstAngle(geom);
                step.setAbsoluteDirection(thisAngle);

                distance = edge.getDistance();
            } else if (step.streetName != streetName
                    && (step.streetName != null && !step.streetName.equals(streetName))) {
                // change of street name
                step = createWalkStep(sptEdge);
                steps.add(step);

                double thisAngle = DirectionUtils.getFirstAngle(geom);
                step.setDirections(lastAngle, thisAngle);
                step.becomes = !pathservice.multipleOptionsBefore(edge);

                distance = edge.getDistance();
            } else {
                /* generate turn-to-stay-on directions, where needed */
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                RelativeDirection direction = WalkStep.getRelativeDirection(lastAngle, thisAngle);

                if (direction == RelativeDirection.CONTINUE) {
                    // append elevation info
                    if (step.elevation != null) {
                        String s = encodeElevationProfile(edge, distance);
                        if (step.elevation.length() > 0 && s != null && s.length() > 0)
                            step.elevation += ",";
                        step.elevation += s;
                    }
                    distance += edge.getDistance();
                } else {
                    // figure out if there was another way we could have turned
                    boolean optionsBefore = pathservice.multipleOptionsBefore(edge);
                    if (optionsBefore) {
                        // turn to stay on
                        step = createWalkStep(sptEdge);
                        steps.add(step);
                        step.setDirections(lastAngle, thisAngle);
                        step.stayOn = true;
                        distance = edge.getDistance();
                    }
                }

            }

            step.distance += edge.getDistance();

            lastAngle = DirectionUtils.getLastAngle(geom);
        }
        return steps;
    }

    private WalkStep createWalkStep(SPTEdge edge) {
        WalkStep step;
        step = new WalkStep();
        step.streetName = edge.getName();
        step.lon = edge.getFromVertex().getX();
        step.lat = edge.getFromVertex().getY();
        step.elevation = encodeElevationProfile(edge.payload, 0);
        return step;
    }

    private String encodeElevationProfile(Edge edge, double offset) {
        if (!(edge instanceof EdgeWithElevation)) {
            return "";
        }
        EdgeWithElevation elevEdge = (EdgeWithElevation) edge;
        if (elevEdge.getElevationProfile() == null) {
            return "";
        }
        String str = "";
        Coordinate[] coordArr = elevEdge.getElevationProfile().toCoordinateArray();
        for (int i = 0; i < coordArr.length; i++) {
            str += Math.round(coordArr[i].x + offset) + "," + Math.round(coordArr[i].y * 10.0)
                    / 10.0 + (i < coordArr.length - 1 ? "," : "");
        }
        return str;
    }

}
