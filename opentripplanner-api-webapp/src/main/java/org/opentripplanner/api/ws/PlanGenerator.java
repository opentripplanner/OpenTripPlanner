/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.RouteSpec;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Dwell;
import org.opentripplanner.routing.edgetype.EdgeWithElevation;
import org.opentripplanner.routing.edgetype.LegSwitchingEdge;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.edgetype.TinyTurnEdge;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.PathServiceFactory;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.util.PolylineEncoder;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class PlanGenerator {
    private static final Logger LOGGER = Logger.getLogger(PlanGenerator.class.getCanonicalName());

    Request request;

    private PathService pathService;

    private FareService fareService;

    public PlanGenerator(Request request, PathServiceFactory pathServiceFactory) {
        this.request = request;
        pathService = pathServiceFactory.getPathService(request.getRouterId());
        Graph graph = pathService.getGraphService().getGraph();
        fareService = graph.getService(FareService.class);
    }

    /**
     * Generates a TripPlan from a Request;
     * 
     */
    public TripPlan generate() {

        TraverseOptions options = getOptions(request);

        checkLocationsAccessible(request, options);

        /* try to plan the trip */
        List<GraphPath> paths = null;
        boolean tooSloped = false;
        try {
            List<String> intermediates = request.getIntermediatePlaces();
            if (intermediates.size() == 0) {
                paths = pathService.plan(request.getFrom(), request.getTo(), request.getDateTime(),
                        options, request.getNumItineraries());
                if (paths == null && request.getWheelchair()) {
                    // There are no paths that meet the user's slope restrictions.
                    // Try again without slope restrictions (and warn user).
                    options.maxSlope = Double.MAX_VALUE;
                    paths = pathService.plan(request.getFrom(), request.getTo(), request
                            .getDateTime(), options, request.getNumItineraries());
                    tooSloped = true;
                }
            } else {
                paths = pathService.plan(request.getFrom(), request.getTo(), intermediates, request
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

        TripPlan plan = generatePlan(paths, request);
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
    public TripPlan generatePlan(List<GraphPath> paths, Request request) {

        GraphPath exemplar = paths.get(0);
        Vertex tripStartVertex = exemplar.getStartVertex();
        Vertex tripEndVertex = exemplar.getEndVertex();
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
            Itinerary itinerary = generateItinerary(path, request.getShowIntermediateStops());
            plan.addItinerary(itinerary);
        }
        return plan;
    }

	/**
	 * Generate an itinerary from a @{link GraphPath}. The algorithm here is to
	 * walk over each state in the graph path, accumulating geometry, time, and
	 * length data from the incoming edge. When the incoming edge and outgoing
	 * edge have different modes (or when a vehicle changes names due to
	 * interlining) a new leg is generated. Street legs undergo an additional
	 * processing step to generate turn-by-turn directions.
	 *
	 * @param path
	 * @param showIntermediateStops
	 *            whether intermediate stops are included in the generated
	 *            itinerary
	 * @return itinerary
	 */
    private Itinerary generateItinerary(GraphPath path, boolean showIntermediateStops) {

        Itinerary itinerary = makeEmptyItinerary(path);

        Leg leg = null;
        CoordinateArrayListSequence coordinates = new CoordinateArrayListSequence();
        double previousElevation = Double.MAX_VALUE;
        double edgeElapsedTime;
        GeometryFactory geometryFactory = new GeometryFactory();
        int startWalk = -1;
        int i = -1;

        State prevState = null;
        EdgeNarrative backEdgeNarrative = null;
        for (State nextState : path.states) {
            i++;
            /* grab base edge and associated narrative information from SPT edge */
            if (prevState == null) {
                prevState = nextState;
                continue;
            }
            EdgeNarrative frontEdgeNarrative = nextState.getBackEdgeNarrative();
            backEdgeNarrative = prevState.getBackEdgeNarrative();
            Edge backEdge = prevState.getBackEdge();
            TraverseMode mode = frontEdgeNarrative.getMode();

            if (backEdgeNarrative == null) {
                // this is the first state, so we need to create the initial leg
                leg = makeLeg(nextState);
                leg.mode = mode.toString(); // maybe makeLeg should be setting the mode ?
                itinerary.addLeg(leg);
                if (mode.isOnStreetNonTransit()) {
                    startWalk = i;
                }
                prevState = nextState;
                continue;
            }

            edgeElapsedTime = nextState.getTimeInMillis()
                    - nextState.getBackState().getTimeInMillis();

            TraverseMode previousMode = backEdgeNarrative.getMode();
            if (previousMode == null) {
                previousMode = prevState.getBackState().getBackEdgeNarrative().getMode();
            }
            
            if (backEdgeNarrative instanceof LegSwitchingEdge) {
            	leg.walkSteps = getWalkSteps(pathService, path.states.subList(startWalk, i - 1));
            	leg = makeLeg(nextState);
            	leg.mode = mode.toString(); //may need to fix this up
            	itinerary.addLeg(leg);
            	if (mode.isOnStreetNonTransit()) {
                    startWalk = i;
                }
            	prevState = nextState;
            	continue;
            }
            
            // handle the effects of the previous edge on the leg
            /* ignore edges that should not contribute to the narrative */
            if (backEdge instanceof FreeEdge) {
            	leg.mode = frontEdgeNarrative.getMode().toString();
                prevState = nextState;
                continue;
            }

            if (previousMode == TraverseMode.BOARDING || previousMode == TraverseMode.ALIGHTING) {
                itinerary.waitingTime += edgeElapsedTime;
            }

            leg.distance += backEdgeNarrative.getDistance();

            /* for all edges with geometry, append their coordinates to the leg's. */
            Geometry edgeGeometry = backEdgeNarrative.getGeometry();
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
            addNotesToLeg(leg, backEdgeNarrative);

            /*
             * we are not boarding, alighting, etc. so are we walking/biking/driving or using
             * transit?
             */
            if (previousMode.isOnStreetNonTransit()) {
                /* we are on the street (non-transit) */
                itinerary.walkTime += edgeElapsedTime;
                itinerary.walkDistance += backEdgeNarrative.getDistance();
                if (backEdge instanceof EdgeWithElevation) {
                    PackedCoordinateSequence profile = ((EdgeWithElevation) backEdge)
                            .getElevationProfile();
                    previousElevation = applyElevation(profile, itinerary, previousElevation);
                }
                leg.endTime = new Date(nextState.getTimeInMillis());
            } else if (previousMode.isTransit()) {
                leg.endTime = new Date(nextState.getTimeInMillis());
                /* we are on a transit trip */
                itinerary.transitTime += edgeElapsedTime;
                if (showIntermediateStops) {
                    /* add an intermediate stop to the current leg */
                    if (leg.stop == null) {
                        /*
                         * first transit edge, just create the list (the initial stop is current
                         * "from" vertex)
                         */
                        leg.stop = new ArrayList<Place>();
                    }
                    /* any further transit edge, add "from" vertex to intermediate stops */
                    if (!(nextState.getBackEdge() instanceof Dwell
                            || nextState.getBackEdge() instanceof PatternDwell || nextState
                            .getBackEdge() instanceof PatternInterlineDwell)) {
                        Place stop = makePlace(nextState);
                        leg.stop.add(stop);
                    } else {
                        leg.stop.get(leg.stop.size() - 1).departure = new Date(nextState.getTime());
                    }
                }
            }

            // now, transition between legs if necessary

            boolean changingToInterlinedTrip = leg != null && leg.route != null
                    && !leg.route.equals(backEdgeNarrative.getName()) && mode.isTransit()
                    && previousMode != null && previousMode.isTransit();

            if ((mode != previousMode || changingToInterlinedTrip) && mode != TraverseMode.STL) {
                /*
                 * change in mode. make a new leg if we are entering walk or transit, otherwise just
                 * update the general itinerary info and move to next edge.
                 */
                boolean endLeg = false;
                if (previousMode == TraverseMode.STL && mode.isOnStreetNonTransit()) {
                    // switching from STL to wall or bike, so we need to fix up
                    // the start time,
                    // mode, etc

                    leg.startTime = new Date(nextState.getTimeInMillis());
                    leg.route = frontEdgeNarrative.getName();
                    leg.mode = mode.toString();
                    startWalk = i;
                } else if (mode == TraverseMode.TRANSFER) {
                    /* transferring mode is only used in transit-only planners */
                    itinerary.walkTime += edgeElapsedTime;
                    itinerary.walkDistance += backEdgeNarrative.getDistance();
                } else if (mode == TraverseMode.BOARDING) {
                    /* boarding mode */
                    itinerary.transfers++;
                    endLeg = true;
                } else if (mode == TraverseMode.ALIGHTING || changingToInterlinedTrip) {
                    endLeg = true;
                } else {
                    if (previousMode == TraverseMode.ALIGHTING) {
                        // in this case, we are changing from an alighting mode
                        // (preAlight) to
                        // an onstreetnontransit. In this case, we have already
                        // closed the
                        // transit leg with alighting, so we don't want to
                        // finalize a leg.
                    } else if (previousMode == TraverseMode.BOARDING) {
                        // we are changing from boarding to an on-transit mode,
                        // so we need to
                        // fix up the leg's route and departure time data
                        leg.startTime = new Date(prevState.getTimeInMillis());
                        leg.route = frontEdgeNarrative.getName();
                        leg.mode = mode.toString();
                        Trip trip = frontEdgeNarrative.getTrip();
                        if (trip != null) {
                            leg.headsign = trip.getTripHeadsign();
                            leg.agencyId = trip.getId().getAgencyId();
                            leg.tripShortName = trip.getTripShortName();
                            leg.routeShortName = trip.getRoute().getShortName();
                            leg.routeLongName = trip.getRoute().getLongName();

                        }
                    } else {
                        // we are probably changing between walk and bike
                        endLeg = true;
                    }
                }
                if (endLeg) {
                    /* finalize leg */
                    /* finalize prior leg if it exists */
                    if (startWalk != -1) {
                        leg.walkSteps = getWalkSteps(pathService, path.states.subList(startWalk,
                                i - 1));
                    }
                    leg.to = makePlace(frontEdgeNarrative.getFromVertex());
                    leg.endTime = new Date(prevState.getTimeInMillis());
                    Geometry geometry = geometryFactory.createLineString(coordinates);
                    leg.legGeometry = PolylineEncoder.createEncodings(geometry);
                    /* reset coordinates */
                    coordinates = new CoordinateArrayListSequence();

                    if (showIntermediateStops && leg.stop != null) {
                        // Remove the last stop -- it's the alighting one
                        leg.stop.remove(leg.stop.size() - 1);
                        if (leg.stop.isEmpty()) {
                            leg.stop = null;
                        }
                    }

                    /* initialize new leg */
                    leg = makeLeg(nextState);
                    if (changingToInterlinedTrip) {
                        leg.interlineWithPreviousLeg = true;
                    }
                    leg.mode = mode.toString();

                    startWalk = -1;
                    leg.route = backEdgeNarrative.getName();
                    if (mode.isOnStreetNonTransit()) {
                        /*
                         * on-street (walk/bike) leg mark where in edge list on-street legs begin,
                         * so step-by-step instructions can be generated for this sublist later
                         */
                        startWalk = i;
                    } else {
                        /* transit leg */
                        startWalk = -1;
                    }
                    itinerary.addLeg(leg);
                }

            } /* end handling mode changes */

            prevState = nextState;
        } /* end loop over graphPath edge list */

        if (leg != null) {
            /* finalize leg */
            leg.to = makePlace(backEdgeNarrative.getToVertex());
            State finalState = path.states.getLast();
            leg.endTime = new Date(finalState.getTimeInMillis());
            Geometry geometry = geometryFactory.createLineString(coordinates);
            leg.legGeometry = PolylineEncoder.createEncodings(geometry);
            if (startWalk != -1) {
                leg.walkSteps = getWalkSteps(pathService, path.states.subList(startWalk, i + 1));
            }
            if (showIntermediateStops && leg.stop != null) {
                // Remove the last stop -- it's the alighting one
                leg.stop.remove(leg.stop.size() - 1);
                if (leg.stop.isEmpty()) {
                    leg.stop = null;
                }
            }
        }
        if (itinerary.transfers == -1) {
            itinerary.transfers = 0;
        }
        itinerary.removeBogusLegs();
        return itinerary;
    }

    private Set<Alert> addNotesToLeg(Leg leg, EdgeNarrative edgeNarrative) {
        Set<Alert> notes = edgeNarrative.getNotes();
        if (notes != null) {
            for (Alert note : notes) {
                leg.addAlert(note);
            }
        }
        return notes;
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
    private Leg makeLeg(State s) {
        Leg leg = new Leg();

        leg.startTime = new Date(s.getBackState().getTimeInMillis());
        EdgeNarrative en = s.getBackEdgeNarrative();
        leg.route = en.getName();
        Trip trip = en.getTrip();
        if (trip != null) {
            leg.headsign = trip.getTripHeadsign();
            leg.agencyId = trip.getId().getAgencyId();
            leg.tripShortName = trip.getTripShortName();
            leg.routeShortName = trip.getRoute().getShortName();
            leg.routeLongName = trip.getRoute().getLongName();
        }
        leg.distance = 0.0;
        leg.from = makePlace(en.getFromVertex());
        return leg;
    }

    /**
     * Makes a new empty Itinerary for a given path.
     * 
     * @return
     */
    private Itinerary makeEmptyItinerary(GraphPath path) {
        Itinerary itinerary = new Itinerary();

        State startState = path.states.getFirst();
        State endState = path.states.getLast();

        itinerary.startTime = new Date(startState.getTimeInMillis());
        itinerary.endTime = new Date(endState.getTimeInMillis());
        itinerary.duration = endState.getTimeInMillis() - startState.getTimeInMillis();
        if (fareService != null) {
            itinerary.fare = fareService.getCost(path);
        }
        itinerary.transfers = -1;
        return itinerary;
    }

    /**
     * Makes a new Place from a state. Contains information about time.
     * 
     * @return
     */
    private Place makePlace(State state) {
        Coordinate endCoord = state.getVertex().getCoordinate();
        String name = state.getVertex().getName();
        AgencyAndId stopId = state.getVertex().getStopId();
        Date timeAtState = new Date(state.getTimeInMillis());
        Place place = new Place(endCoord.x, endCoord.y, name, stopId, timeAtState);
        return place;
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
            if (!pathService.isAccessible(request.getFrom(), options)
                    || !pathService.isAccessible(request.getTo(), options)) {
                throw new LocationNotAccessible();
            }

        }
    }

    /**
     * Get the traverse options for a request
     * 
     * @param request
     * @return
     */
    private TraverseOptions getOptions(Request request) {

        TraverseModeSet modeSet = request.getModeSet();
        assert (modeSet.isValid());
        TraverseOptions options = new TraverseOptions(modeSet, request.getOptimize());
        options.setArriveBy(request.isArriveBy());
        options.wheelchairAccessible = request.getWheelchair();
        if (request.getMaxSlope() > 0) {
            options.maxSlope = request.getMaxSlope();
        }
        if (request.getMaxWalkDistance() > 0) {
            options.setMaxWalkDistance(request.getMaxWalkDistance());
        }
        if (request.getWalkSpeed() > 0) {
            options.speed = request.getWalkSpeed();
        }
        options.triangleSafetyFactor = request.getTriangleSafetyFactor();
        options.triangleSlopeFactor = request.getTriangleSlopeFactor();
        options.triangleTimeFactor = request.getTriangleTimeFactor();
        if (request.getMinTransferTime() != null) {
            options.minTransferTime = request.getMinTransferTime();
        }
        if (request.getPreferredRoutes() != null) {
            for (String element : request.getPreferredRoutes()) {
                String[] routeSpec = element.split("_", 2);
                if (routeSpec.length != 2) {
                    throw new IllegalArgumentException(
                            "AgencyId or routeId not set in preferredRoutes list");
                }
                options.preferredRoutes.add(new RouteSpec(routeSpec[0], routeSpec[1]));
            }
        }
        if (request.getUnpreferredRoutes() != null) {
            for (String element : request.getUnpreferredRoutes()) {
                String[] routeSpec = element.split("_", 2);
                if (routeSpec.length != 2) {
                    throw new IllegalArgumentException(
                            "AgencyId or routeId not set in unpreferredRoutes list");
                }
                options.unpreferredRoutes.add(new RouteSpec(routeSpec[0], routeSpec[1]));
            }
        }
        if (request.getBannedRoutes() != null) {
            for (String element : request.getBannedRoutes()) {
                String[] routeSpec = element.split("_", 2);
                if (routeSpec.length != 2) {
                    throw new IllegalArgumentException(
                            "AgencyId or routeId not set in bannedRoutes list");
                }
                options.bannedRoutes.add(new RouteSpec(routeSpec[0], routeSpec[1]));
            }
        }
        if (request.getTransferPenalty() != null) {
            options.transferPenalty = request.getTransferPenalty();
        }
        return options;
    }

    /**
     * Converts a list of street edges to a list of turn-by-turn directions.
     * 
     * @param edges : A list of street edges
     * @return
     */
    private List<WalkStep> getWalkSteps(PathService pathService, List<State> states) {
        List<WalkStep> steps = new ArrayList<WalkStep>();
        WalkStep step = null;
        double lastAngle = 0, distance = 0; // distance used for appending elevation profiles
        int roundaboutExit = 0; // track whether we are in a roundabout, and if so the exit number

        for (State currState : states) {
            Edge edge = currState.getBackEdge();
            EdgeNarrative edgeNarrative = currState.getBackEdgeNarrative();
            if (edge instanceof FreeEdge) {
                continue;
            }
            if (!edgeNarrative.getMode().isOnStreetNonTransit()) {
                continue; // ignore STLs and the like
            }
            Geometry geom = edgeNarrative.getGeometry();
            if (geom == null) {
                continue;
            }
            String streetName = edgeNarrative.getName();
            if (step == null) {
                // first step
                step = createWalkStep(currState);
                steps.add(step);
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                step.setAbsoluteDirection(thisAngle);
                // new step, set distance to length of first edge
                distance = edgeNarrative.getDistance();
            } else if (step.streetName != streetName
                    && (step.streetName != null && !step.streetName.equals(streetName))) {
                /* street name has changed */
                if (roundaboutExit > 0) {
                    // if we were just on a roundabout,
                    // make note of which exit was taken in the existing step
                    step.exit = Integer.toString(roundaboutExit); // ordinal numbers from
                    // localization
                    roundaboutExit = 0;
                }
                /* start a new step */
                step = createWalkStep(currState);
                steps.add(step);
                if (edgeNarrative.isRoundabout()) {
                    // indicate that we are now on a roundabout
                    // and use one-based exit numbering
                    roundaboutExit = 1;
                }
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                step.setDirections(lastAngle, thisAngle, edgeNarrative.isRoundabout());
                // new step, set distance to length of first edge
                distance = edgeNarrative.getDistance();
            } else {
                /* street name has not changed */
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                RelativeDirection direction = WalkStep.getRelativeDirection(lastAngle, thisAngle,
                        edgeNarrative.isRoundabout());
                boolean optionsBefore = pathService.multipleOptionsBefore(edge, currState
                        .getBackState());
                if (edgeNarrative.isRoundabout()) {
                    // we are on a roundabout, and have already traversed at least one edge of it.
                    if (optionsBefore) {
                        // increment exit count if we passed one.
                        roundaboutExit += 1;
                    }
                }
                if (edgeNarrative.isRoundabout() || direction == RelativeDirection.CONTINUE) {
                    // we are continuing almost straight, or continuing along a roundabout.
                    // just append elevation info onto the existing step.
                    if (step.elevation != null) {
                        String s = encodeElevationProfile(edge, distance);
                        if (step.elevation.length() > 0 && s != null && s.length() > 0)
                            step.elevation += ",";
                        step.elevation += s;
                    }
                    // extending a step, increment the existing distance
                    distance += edgeNarrative.getDistance();
                } else {
                    // we are not on a roundabout, and not continuing straight through.

                    // figure out if there were other plausible turn options at the last
                    // intersection
                    // to see if we should generate a "left to continue" instruction.
                    boolean shouldGenerateContinue = false;
                    if (edge instanceof PlainStreetEdge) {
                        // the next edges will be TinyTurnEdges or PlainStreetEdges, we hope

                        double angleDiff = getAbsoluteAngleDiff(thisAngle, lastAngle);
                        for (DirectEdge alternative : pathService.getOutgoingEdges(currState
                                .getBackState().getVertex())) {
                            if (alternative instanceof TinyTurnEdge) {
                                alternative = pathService.getOutgoingEdges(
                                        alternative.getToVertex()).get(0);
                            }
                            double altAngle = DirectionUtils.getFirstAngle(alternative
                                    .getGeometry());
                            double altAngleDiff = getAbsoluteAngleDiff(altAngle, lastAngle);
                            if (altAngleDiff - angleDiff < Math.PI / 16) {
                                shouldGenerateContinue = true;
                                break;
                            }
                        }
                    } else if (edge instanceof TinyTurnEdge) {
                        // do nothing as this will be handled in other cases
                    } else {
                        double newAngle;
                        if (currState.getVertex() instanceof StreetVertex) {
                            newAngle = DirectionUtils.getFirstAngle(((StreetVertex) currState
                                    .getVertex()).getGeometry());
                        } else {
                            List<DirectEdge> outgoingEdges = pathService.getOutgoingEdges(currState
                                    .getVertex());
                            Edge oge = outgoingEdges.get(0);
                            newAngle = DirectionUtils.getFirstAngle(((DirectEdge) oge)
                                    .getGeometry());
                        }
                        double angleDiff = getAbsoluteAngleDiff(newAngle, thisAngle);
                        for (DirectEdge alternative : pathService.getOutgoingEdges(currState
                                .getBackState().getVertex())) {
                            if (alternative == edge) {
                                continue;
                            }
                            alternative = pathService.getOutgoingEdges(alternative.getToVertex())
                                    .get(0);
                            double altAngle = DirectionUtils.getFirstAngle(alternative
                                    .getGeometry());
                            double altAngleDiff = getAbsoluteAngleDiff(altAngle, lastAngle);
                            if (altAngleDiff - angleDiff < Math.PI / 16) {
                                shouldGenerateContinue = true;
                                break;
                            }
                        }
                    }

                    if (shouldGenerateContinue) {
                        // turn to stay on same-named street
                        step = createWalkStep(currState);
                        steps.add(step);
                        step.setDirections(lastAngle, thisAngle, false);
                        step.stayOn = true;
                        // new step, set distance to length of first edge
                        distance = edgeNarrative.getDistance();
                    }
                }
            }
            // increment the total length for this step
            step.distance += edgeNarrative.getDistance();
            step.addAlerts(edgeNarrative.getNotes());
            lastAngle = DirectionUtils.getLastAngle(geom);
        }
        return steps;
    }

    private double getAbsoluteAngleDiff(double thisAngle, double lastAngle) {
        double angleDiff = thisAngle - lastAngle;
        if (angleDiff < 0) {
            angleDiff += Math.PI * 2;
        }
        double ccwAngleDiff = Math.PI * 2 - angleDiff;
        if (ccwAngleDiff < angleDiff) {
            angleDiff = ccwAngleDiff;
        }
        return angleDiff;
    }

    private WalkStep createWalkStep(State s) {
        EdgeNarrative en = s.getBackEdgeNarrative();
        WalkStep step;
        step = new WalkStep();
        step.streetName = en.getName();
        step.lon = en.getFromVertex().getX();
        step.lat = en.getFromVertex().getY();
        step.elevation = encodeElevationProfile(s.getBackEdge(), 0);
        step.bogusName = en.hasBogusName();
        step.addAlerts(en.getNotes());
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
        StringBuilder str = new StringBuilder();
        Coordinate[] coordArr = elevEdge.getElevationProfile().toCoordinateArray();
        for (int i = 0; i < coordArr.length; i++) {
            str.append(Math.round(coordArr[i].x + offset));
            str.append(",");
            str.append(Math.round(coordArr[i].y * 10.0) / 10.0);
            str.append(i < coordArr.length - 1 ? "," : "");
        }
        return str.toString();
    }

}
