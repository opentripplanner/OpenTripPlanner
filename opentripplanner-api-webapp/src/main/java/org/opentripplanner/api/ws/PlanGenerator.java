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
import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.RouteSpec;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.edgetype.Dwell;
import org.opentripplanner.routing.edgetype.EdgeWithElevation;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.LegSwitchingEdge;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.ElevatorAlightEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.TinyTurnEdge;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.PathServiceFactory;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.opentripplanner.util.PolylineEncoder;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class PlanGenerator {
    private static final Logger LOGGER = Logger.getLogger(PlanGenerator.class.getCanonicalName());

    Request request;

    private PathService pathService;

    private FareService fareService;

    private GeometryFactory geometryFactory = new GeometryFactory();

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
            List<NamedPlace> intermediates = request.getIntermediatePlaces();
            if (intermediates.size() == 0) {
                paths = pathService.plan(request.getFromPlace(), request.getToPlace(), request.getDateTime(),
                        options, request.getNumItineraries());
                if (paths == null && request.getWheelchair()) {
                    // There are no paths that meet the user's slope restrictions.
                    // Try again without slope restrictions (and warn user).
                    options.maxSlope = Double.MAX_VALUE;
                    paths = pathService.plan(request.getFromPlace(), request.getToPlace(), request
                            .getDateTime(), options, request.getNumItineraries());
                    tooSloped = true;
                }
            } else {
                paths = pathService.plan(request.getFromPlace(), request.getToPlace(), intermediates,
                        request.isIntermediatePlacesOrdered(), request.getDateTime(), options);
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
                /* fix up from/to on first/last legs */
                Leg firstLeg = i.legs.get(0);
                firstLeg.from.orig = request.getFromName();
                Leg lastLeg = i.legs.get(i.legs.size() - 1);
                lastLeg.to.orig = request.getToName();
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
     * Generate an itinerary from a @{link GraphPath}. The algorithm here is to walk over each state
     * in the graph path, accumulating geometry, time, and length data from the incoming edge. When
     * the incoming edge and outgoing edge have different modes (or when a vehicle changes names due
     * to interlining) a new leg is generated. Street legs undergo an additional processing step to
     * generate turn-by-turn directions.
     * 
     * @param path
     * @param showIntermediateStops whether intermediate stops are included in the generated
     *        itinerary
     * @return itinerary
     */
    private Itinerary generateItinerary(GraphPath path, boolean showIntermediateStops) {
        Itinerary itinerary = makeEmptyItinerary(path);

        Leg leg = null;
        CoordinateArrayListSequence coordinates = new CoordinateArrayListSequence();
        double previousElevation = Double.MAX_VALUE;
        int startWalk = -1;
        int i = -1;
        PlanGenState pgstate = PlanGenState.START;
        String nextName = null;
        for (State state : path.states) {
            i += 1;
            Edge backEdge = state.getBackEdge();
            EdgeNarrative backEdgeNarrative = state.getBackEdgeNarrative();
            if (backEdge == null) {
                continue;
            }
            if (backEdge instanceof FreeEdge) {
                continue;
            }

            TraverseMode mode = backEdgeNarrative.getMode();
            if (mode == TraverseMode.BOARDING || mode == TraverseMode.ALIGHTING) {
                itinerary.waitingTime += state.getElapsedTime();
            }
            if (backEdge instanceof EdgeWithElevation) {
                PackedCoordinateSequence profile = ((EdgeWithElevation) backEdge)
                        .getElevationProfile();
                previousElevation = applyElevation(profile, itinerary, previousElevation);
            }
            if (mode != null && mode.isOnStreetNonTransit()) {
                itinerary.walkDistance += backEdgeNarrative.getDistance();
            }

            switch (pgstate) {
            case START:
                if (mode == TraverseMode.WALK) {
                    pgstate = PlanGenState.WALK;
                    leg = makeLeg(itinerary, state);
                    leg.from.orig = nextName;
                    startWalk = i;
                } else if (mode == TraverseMode.BICYCLE) {
                    pgstate = PlanGenState.BICYCLE;
                    leg = makeLeg(itinerary, state);
                    leg.from.orig = nextName;
                    startWalk = i;
                } else if (mode == TraverseMode.CAR) {
                    pgstate = PlanGenState.CAR;
                    leg = makeLeg(itinerary, state);
                    leg.from.orig = nextName;
                    startWalk = i;                    
                } else if (mode == TraverseMode.BOARDING) {
                    // this itinerary starts with transit
                    pgstate = PlanGenState.PRETRANSIT;
                    leg = makeLeg(itinerary, state);
                    leg.from.orig = nextName;
                    startWalk = -1;
                } else if (mode == TraverseMode.STL) {
                    // this comes after an alight; do nothing
                } else {
                    System.out.println("UNEXPECTED STATE: " + mode);
                }
                break;
            case WALK:
                if (leg == null) {
                    leg = makeLeg(itinerary, state);
                }
                if (mode == TraverseMode.WALK) {
                    // do nothing
                } else if (mode == TraverseMode.BICYCLE) {
                    finalizeLeg(leg, state, path.states, startWalk, i, coordinates);
                    startWalk = i;
                    leg = makeLeg(itinerary, state);
                    pgstate = PlanGenState.BICYCLE;
                } else if (mode == TraverseMode.STL) {
                    finalizeLeg(leg, state, path.states, startWalk, i, coordinates);
                    leg = null;
                    pgstate = PlanGenState.PRETRANSIT;
                } else if (mode == TraverseMode.BOARDING) {
                    //this only happens in case of a timed transfer.
                    pgstate = PlanGenState.PRETRANSIT;
                    finalizeLeg(leg, state, path.states, startWalk, i, coordinates);
                    leg = makeLeg(itinerary, state);
                    itinerary.transfers++;
                } else if (backEdgeNarrative instanceof LegSwitchingEdge) {
                    nextName = state.getBackState().getBackState().getBackState().getVertex().getName();
                    finalizeLeg(leg, state, path.states, startWalk, i - 1, coordinates);
                    leg = null;
                    pgstate = PlanGenState.START;
                } else {
                    System.out.println("UNEXPECTED STATE: " + mode);
                }
                break;
            case BICYCLE:
                if (leg == null) {
                    leg = makeLeg(itinerary, state);
                }
                if (mode == TraverseMode.BICYCLE) {
                    // do nothing
                } else if (mode == TraverseMode.WALK) {
                    finalizeLeg(leg, state, path.states, startWalk, i, coordinates);
                    leg = makeLeg(itinerary, state);
                    startWalk = i;
                    pgstate = PlanGenState.WALK;
                } else if (mode == TraverseMode.STL) {
                    finalizeLeg(leg, state, path.states, startWalk, i, coordinates);
                    leg = null;
                    pgstate = PlanGenState.PRETRANSIT;
                } else if (backEdgeNarrative instanceof LegSwitchingEdge) {
                    finalizeLeg(leg, state, path.states, startWalk, i - 1, coordinates);
                    leg = null;
                    pgstate = PlanGenState.START;
                } else {
                    System.out.println("UNEXPECTED STATE: " + mode);
                }
                break;
            case CAR:
                if (leg == null) {
                    leg = makeLeg(itinerary, state);
                }
                if (mode == TraverseMode.CAR) {
                    // do nothing
                } else if (mode == TraverseMode.STL) {
                    finalizeLeg(leg, state, path.states, startWalk, i, coordinates);
                    leg = null;
                    pgstate = PlanGenState.PRETRANSIT;
                } else if (backEdgeNarrative instanceof LegSwitchingEdge) {
                    finalizeLeg(leg, state, path.states, startWalk, i - 1, coordinates);
                    leg = null;
                    pgstate = PlanGenState.START;
                } else {
                    System.out.println("UNEXPECTED STATE: " + mode);
                }
                break;
            case PRETRANSIT:
                if (mode == TraverseMode.BOARDING) {
                    if (leg != null) {
                        System.out.println("leg unexpectedly not null");
                    }
                    leg = makeLeg(itinerary, state);
                    itinerary.transfers++;
                }
                if (backEdge instanceof Hop || backEdge instanceof PatternHop) {
                    pgstate = PlanGenState.TRANSIT;
                    fixupTransitLeg(leg, state);
                    leg.stop = new ArrayList<Place>();
                }
                break;
            case TRANSIT:
                String route = backEdgeNarrative.getName();
                if (mode == TraverseMode.ALIGHTING) {
                    if (showIntermediateStops && leg.stop != null && leg.stop.size() > 0) {
                        if (leg.stop.isEmpty()) {
                            leg.stop = null;
                        }
                    }
                    finalizeLeg(leg, state, null, -1, -1, coordinates);
                    leg = null;
                    pgstate = PlanGenState.START;
                } else if (mode.toString() == leg.mode) {
                    // no mode change, handle intermediate stops
                    if (showIntermediateStops) {
                        /*
                         * any further transit edge, add "from" vertex to intermediate stops
                         */
                        if (!(backEdge instanceof Dwell || backEdge instanceof PatternDwell || backEdge instanceof PatternInterlineDwell)) {
                            Place stop = makePlace(state.getBackState());
                            leg.stop.add(stop);
                        } else {
                            leg.stop.get(leg.stop.size() - 1).departure = new Date(
                                    state.getTimeInMillis());
                        }
                    }
                    if (!route.equals(leg.route)) {
                        // interline dwell
                        finalizeLeg(leg, state, null, -1, -1, coordinates);
                        leg = makeLeg(itinerary, state);
                        fixupTransitLeg(leg, state);
                        leg.interlineWithPreviousLeg = true;
                    }
                } else {
                    System.out.println("UNEXPECTED STATE: " + mode);
                }
                break;
            }
            if (leg != null) {
                leg.distance += backEdgeNarrative.getDistance();
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
                if (pgstate == PlanGenState.TRANSIT) {
                    itinerary.transitTime += state.getElapsedTime();
                }

            }

        } /* end loop over graphPath edge list */

        if (leg != null) {
            finalizeLeg(leg, path.states.getLast(), path.states, startWalk, i, coordinates);
        }

        itinerary.removeBogusLegs();
        return itinerary;
    }

    private void fixupTransitLeg(Leg leg, State state) {
        EdgeNarrative en = state.getBackEdgeNarrative();
        leg.route = en.getName();
        Trip trip = en.getTrip();
        if (trip != null) {
            leg.headsign = trip.getTripHeadsign();
            leg.agencyId = trip.getId().getAgencyId();
            leg.tripShortName = trip.getTripShortName();
            leg.routeShortName = trip.getRoute().getShortName();
            leg.routeLongName = trip.getRoute().getLongName();
        }
        leg.mode = en.getMode().toString();
        leg.startTime = new Date(state.getBackState().getTimeInMillis());
    }

    private void finalizeLeg(Leg leg, State state, List<State> states, int start, int end,
            CoordinateArrayListSequence coordinates) {
        if (start != -1) {
            leg.walkSteps = getWalkSteps(states.subList(start, end + 1));
        }
        leg.endTime = new Date(state.getBackState().getTimeInMillis());
        Geometry geometry = geometryFactory.createLineString(coordinates);
        leg.legGeometry = PolylineEncoder.createEncodings(geometry);
        leg.to = makePlace(state);
        coordinates.clear();
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
     * @param itinerary 
     */
    private Leg makeLeg(Itinerary itinerary, State s) {
        Leg leg = new Leg();
        itinerary.addLeg(leg);
        leg.startTime = new Date(s.getBackState().getTimeInMillis());
        EdgeNarrative en = s.getBackEdgeNarrative();
        leg.distance = 0.0;
        leg.from = makePlace(en.getFromVertex());
        leg.mode = en.getMode().toString();
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
        Vertex v = state.getVertex();
        Coordinate endCoord = v.getCoordinate();
        String name = v.getName();
        AgencyAndId stopId = null;
        if (v instanceof TransitVertex)
            stopId = ((TransitVertex)v).getStopId();
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
        if (vertex instanceof TransitVertex)
            place.stopId = ((TransitVertex)vertex).getStopId();
        return place;
    }

    /**
     * Throw an exception if the start and end locations are not wheelchair accessible given the
     * user's specified maximum slope.
     */
    private void checkLocationsAccessible(Request request, TraverseOptions options) {
        if (request.getWheelchair()) {
            // check if the start and end locations are accessible
            if (!pathService.isAccessible(request.getFromPlace(), options)
                    || !pathService.isAccessible(request.getToPlace(), options)) {
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
        options.setTriangleSafetyFactor(request.getTriangleSafetyFactor());
        options.setTriangleSlopeFactor(request.getTriangleSlopeFactor());
        options.setTriangleTimeFactor(request.getTriangleTimeFactor());
        if (request.getMinTransferTime() != null) {
            options.minTransferTime = request.getMinTransferTime();
        }
        if (request.getMaxTransfers() != null) {
            options.maxTransfers = request.getMaxTransfers();
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
    private List<WalkStep> getWalkSteps(List<State> states) {
        List<WalkStep> steps = new ArrayList<WalkStep>();
        WalkStep step = null;
        double lastAngle = 0, distance = 0; // distance used for appending elevation profiles
        int roundaboutExit = 0; // track whether we are in a roundabout, and if so the exit number
        String roundaboutPreviousStreet = null;

        for (State currState : states) {
            State backState = currState.getBackState();
            Edge edge = currState.getBackEdge();
            EdgeNarrative edgeNarrative = currState.getBackEdgeNarrative();
            boolean createdNewStep = false;
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

            // generate a step for getting off an elevator (all
            // elevator narrative generation occurs when alighting). We don't need to know what came
            // before or will come after
            if (edge instanceof ElevatorAlightEdge) {
                // don't care what came before or comes after
                step = createWalkStep(currState);
                
                // tell the user where to get off the elevator using the exit notation, so the
                // i18n interface will say 'Elevator to <exit>'
                // what happens is that the webapp sees name == null and ignores that, and it sees
                // exit != null and uses to <exit>
                // the floor name is the AlightEdge name
                // reset to avoid confusion with 'Elevator on floor 1 to floor 1'
                step.streetName =((ElevatorAlightEdge) edge).getName();

                step.relativeDirection = RelativeDirection.ELEVATOR;

                steps.add(step);
                continue;
            }

            String streetName = edgeNarrative.getName();
            if (step == null) {
                // first step
                step = createWalkStep(currState);
                createdNewStep = true;
                
                steps.add(step);
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                step.setAbsoluteDirection(thisAngle);
                // new step, set distance to length of first edge
                distance = edgeNarrative.getDistance();
            } else if (step.streetName != streetName
                    && (step.streetName != null && !step.streetName.equals(streetName)) 
                    && (!step.bogusName || !edgeNarrative.hasBogusName())) {
                /* street name has changed */
                if (roundaboutExit > 0) {
                    // if we were just on a roundabout,
                    // make note of which exit was taken in the existing step
                    step.exit = Integer.toString(roundaboutExit); // ordinal numbers from
                    if (streetName.equals(roundaboutPreviousStreet)) {
                        step.stayOn = true;
                    }
                    // localization
                    roundaboutExit = 0;
                }
                /* start a new step */
                step = createWalkStep(currState);
                createdNewStep = true;
                
                steps.add(step);
                if (edgeNarrative.isRoundabout()) {
                    // indicate that we are now on a roundabout
                    // and use one-based exit numbering
                    roundaboutExit = 1;
                    roundaboutPreviousStreet = backState.getBackEdgeNarrative().getName();
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
                boolean optionsBefore = backState.multipleOptionsBefore();
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
                	
                } else {
                    // we are not on a roundabout, and not continuing straight through.

                    // figure out if there were other plausible turn options at the last
                    // intersection
                    // to see if we should generate a "left to continue" instruction.
                    boolean shouldGenerateContinue = false;
                    if (edge instanceof PlainStreetEdge) {
                        // the next edges will be TinyTurnEdges or PlainStreetEdges, we hope
                        double angleDiff = getAbsoluteAngleDiff(thisAngle, lastAngle);
                        for (Edge alternative : backState.getVertex().getOutgoingStreetEdges()) {
                            if (alternative instanceof TinyTurnEdge) {
                                //a tiny turn edge has no geometry, but the next
                                //edge will be a TurnEdge or PSE and will have direction
                                alternative = alternative.getToVertex().getOutgoingStreetEdges().get(0);
                            }
                            if (alternative.getName().equals(streetName)) {
                                //alternatives that have the same name
                                //are usually caused by street splits
                                continue;
                            }
                            double altAngle = DirectionUtils.getFirstAngle(alternative
                                    .getGeometry());
                            double altAngleDiff = getAbsoluteAngleDiff(altAngle, lastAngle);
                            if (angleDiff > Math.PI / 4 || altAngleDiff - angleDiff < Math.PI / 16) {
                                shouldGenerateContinue = true;
                                break;
                            }
                        }
                    } else if (edge instanceof TinyTurnEdge) {
                        // do nothing as this will be handled in other cases
                    } else {
                        double angleDiff = getAbsoluteAngleDiff(lastAngle, thisAngle);
                        //in the case of a turn edge, we actually have to go back two steps to see where
                        //else we might be, as once we are on the streetvertex leading into this edge,
                        //we are stuck
                        State twoStatesBack = backState.getBackState();
                        Vertex backVertex = twoStatesBack.getVertex();
                        for (Edge alternative : backVertex.getOutgoingStreetEdges()) {
                            List<Edge> alternatives = alternative.getToVertex().getOutgoingStreetEdges();
                            if (alternatives.size() == 0) {
                                continue; //this is not an alternative
                            }
                            alternative = alternatives.get(0);
                            if (alternative.getName().equals(streetName)) {
                                //alternatives that have the same name
                                //are usually caused by street splits
                                continue;
                            }
                            double altAngle = DirectionUtils.getFirstAngle(alternative
                                    .getGeometry());
                            double altAngleDiff = getAbsoluteAngleDiff(altAngle, lastAngle);
                            if (angleDiff > Math.PI / 4 || altAngleDiff - angleDiff < Math.PI / 16) {
                                shouldGenerateContinue = true;
                                break;
                            }
                        }
                    }

                    if (shouldGenerateContinue) {
                        // turn to stay on same-named street
                        step = createWalkStep(currState);
                        createdNewStep = true;
                        steps.add(step);
                        step.setDirections(lastAngle, thisAngle, false);
                        step.stayOn = true;
                        // new step, set distance to length of first edge
                        distance = edgeNarrative.getDistance();
                    }
                }
            }
            
            if(!createdNewStep) {
            	if (step.elevation != null) {
                    String s = encodeElevationProfile(edge, distance);
                    if (step.elevation.length() > 0 && s != null && s.length() > 0)
                        step.elevation += ",";
                    step.elevation += s;
                }
                distance += edgeNarrative.getDistance();

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
