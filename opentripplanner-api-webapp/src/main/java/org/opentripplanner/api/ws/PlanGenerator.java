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
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
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
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.TinyTurnEdge;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.TrivialPathException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.opentripplanner.util.PolylineEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

@Service @Scope("singleton")
public class PlanGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(PlanGenerator.class);

    private GeometryFactory geometryFactory = new GeometryFactory();

    @Autowired public PathService pathService;
    
    /** Generates a TripPlan from a Request */
    public TripPlan generate(RoutingRequest options) {

        // TODO: this seems to only check the endpoints, which are usually auto-generated
        //if ( ! options.isAccessible())
        //    throw new LocationNotAccessible();

        /* try to plan the trip */
        List<GraphPath> paths = null;
        boolean tooSloped = false;
        try {
            paths = pathService.getPaths(options);
            if (paths == null && options.getWheelchairAccessible()) {
                // There are no paths that meet the user's slope restrictions.
                // Try again without slope restrictions (and warn user).
                options.maxSlope = Double.MAX_VALUE;
                paths = pathService.getPaths(options);
                tooSloped = true;
            }
        } catch (VertexNotFoundException e) {
            LOG.info("Vertex not found: " + options.getFrom() + " : " + options.getTo(), e);
            throw e;
        }

        if (paths == null || paths.size() == 0) {
            LOG.info("Path not found: " + options.getFrom() + " : " + options.getTo());
            throw new PathNotFoundException();
        }

        TripPlan plan = generatePlan(paths, options);
        if (plan != null) {
            for (Itinerary i : plan.itinerary) {
                i.tooSloped = tooSloped;
                /* fix up from/to on first/last legs */
                if (i.legs.size() == 0) {
                    LOG.warn("itinerary has no legs");
                    continue;
                }
                Leg firstLeg = i.legs.get(0);
                firstLeg.from.orig = options.getFromName();
                Leg lastLeg = i.legs.get(i.legs.size() - 1);
                lastLeg.to.orig = options.getToName();
            }
        }

        return plan;
    }

    /**
     * Generates a TripPlan from a set of paths
     */
    public TripPlan generatePlan(List<GraphPath> paths, RoutingRequest request) {

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
        Graph graph = path.getRoutingContext().graph;
        TransitIndexService transitIndex = graph.getService(TransitIndexService.class);

        Itinerary itinerary = makeEmptyItinerary(path);
        EdgeNarrative postponedAlerts = null;
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

            TraverseMode mode = backEdgeNarrative.getMode();
            if (mode != null) {
                long dt = state.getAbsTimeDeltaSec();
                if (mode == TraverseMode.BOARDING || mode == TraverseMode.ALIGHTING
                        || mode == TraverseMode.STL) {
                    itinerary.waitingTime += dt;
                } else if (mode.isOnStreetNonTransit()) {
                    itinerary.walkDistance += backEdgeNarrative.getDistance();
                    itinerary.walkTime += dt;
                } else if (mode.isTransit()) {
                    itinerary.transitTime += dt;
                }
            }

            if (backEdge instanceof FreeEdge) {
                if (backEdge instanceof PreBoardEdge) {
                    // Add boarding alerts to the next leg
                    postponedAlerts = backEdgeNarrative;
                } else if (backEdge instanceof PreAlightEdge) {
                    // Add alighting alerts to the previous leg
                    addNotesToLeg(itinerary.legs.get(itinerary.legs.size() - 1), backEdgeNarrative);
                }
                continue;
            }

            if (backEdge instanceof EdgeWithElevation) {
                PackedCoordinateSequence profile = ((EdgeWithElevation) backEdge)
                        .getElevationProfile();
                previousElevation = applyElevation(profile, itinerary, previousElevation);
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
                } else if (mode == TraverseMode.TRANSFER) {
                    // handle the whole thing in one step
                    leg = makeLeg(itinerary, state);
                    coordinates = new CoordinateArrayListSequence();
                    coordinates.add(state.getBackState().getVertex().getCoordinate());
                    coordinates.add(state.getVertex().getCoordinate());
                    finalizeLeg(leg, state, path.states, i, i, coordinates);
                    coordinates.clear();
                } else {
                    LOG.error("Unexpected state (in START): " + mode);
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
                    // this only happens in case of a timed transfer.
                    pgstate = PlanGenState.PRETRANSIT;
                    finalizeLeg(leg, state, path.states, startWalk, i, coordinates);
                    leg = makeLeg(itinerary, state);
                    itinerary.transfers++;
                } else if (backEdgeNarrative instanceof LegSwitchingEdge) {
                    nextName = state.getBackState().getBackState().getBackState().getVertex()
                            .getName();
                    finalizeLeg(leg, state, path.states, startWalk, i - 1, coordinates);
                    leg = null;
                    pgstate = PlanGenState.START;
                } else {
                    LOG.error("Unexpected state (in WALK): " + mode);
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
                    LOG.error("Unexpected state (in BICYCLE): " + mode);
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
                    LOG.error("Unexpected state (in CAR): " + mode);
                }
                break;
            case PRETRANSIT:
                if (mode == TraverseMode.BOARDING) {
                    if (leg != null) {
                        LOG.error("leg unexpectedly not null (boarding loop)");
                    } else {
                        leg = makeLeg(itinerary, state);
                        itinerary.transfers++;
                        leg.boardRule = (String) state.getExtension("boardAlightRule");
                    }
                } else if (backEdge instanceof Hop || backEdge instanceof PatternHop) {
                    pgstate = PlanGenState.TRANSIT;
                    fixupTransitLeg(leg, state, transitIndex);
                    leg.stop = new ArrayList<Place>();
                } else {
                    LOG.error("Unexpected state (in PRETRANSIT): " + mode);
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
                    leg.alightRule = (String) state.getExtension("boardAlightRule");
                    finalizeLeg(leg, state, null, -1, -1, coordinates);
                    leg = null;
                    pgstate = PlanGenState.START;
                } else if (mode.toString().equals(leg.mode)) {
                    // no mode change, handle intermediate stops
                    if (showIntermediateStops) {
                        /*
                         * any further transit edge, add "from" vertex to intermediate stops
                         */
                        if (!(backEdge instanceof Dwell || backEdge instanceof PatternDwell || backEdge instanceof PatternInterlineDwell)) {
                            Place stop = makePlace(state.getBackState(), true);
                            leg.stop.add(stop);
                        } else if (leg.stop.size() > 0) {
                            leg.stop.get(leg.stop.size() - 1).departure = makeCalendar(state);
                        }
                    }
                    if (!route.equals(leg.route)) {
                        // interline dwell
                        finalizeLeg(leg, state, null, -1, -1, coordinates);
                        leg = makeLeg(itinerary, state);
                        fixupTransitLeg(leg, state, transitIndex);
                        leg.startTime = makeCalendar(state);
                        leg.interlineWithPreviousLeg = true;
                    }
                } else {
                    LOG.error("Unexpected state (in TRANSIT): " + mode);
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

                if (postponedAlerts != null) {
                    addNotesToLeg(leg, postponedAlerts);
                    postponedAlerts = null;
                }

                addNotesToLeg(leg, backEdgeNarrative);

            }

        } /* end loop over graphPath edge list */

        if (leg != null) {
            finalizeLeg(leg, path.states.getLast(), path.states, startWalk, i, coordinates);
        }
        itinerary.removeBogusLegs();
        itinerary.fixupDates(graph.getService(CalendarServiceData.class));
        if (itinerary.legs.size() == 0)
            throw new TrivialPathException();
        return itinerary;
    }

    private Calendar makeCalendar(State state) {
        CalendarService service = state.getContext().calendarService;
        Collection<String> agencyIds = state.getContext().graph.getAgencyIds();
        TimeZone timeZone; 
        if (agencyIds.size() == 0) {
            timeZone = TimeZone.getTimeZone("GMT");
        } else {
            timeZone = service.getTimeZoneForAgencyId(agencyIds.iterator().next());
        }
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(state.getTimeInMillis());
        return calendar;
    }

    private void fixupTransitLeg(Leg leg, State state, TransitIndexService transitIndex) {
        EdgeNarrative en = state.getBackEdgeNarrative();
        leg.route = en.getName();
        Trip trip = en.getTrip();
        if (trip != null) {
            leg.headsign = trip.getTripHeadsign();
            leg.tripId = trip.getId().getId();
            leg.agencyId = trip.getId().getAgencyId();
            leg.tripShortName = trip.getTripShortName();
            leg.routeShortName = trip.getRoute().getShortName();
            leg.routeLongName = trip.getRoute().getLongName();
            leg.routeColor = trip.getRoute().getColor();
            leg.routeTextColor = trip.getRoute().getTextColor();
            if (transitIndex != null) {
                Agency agency = transitIndex.getAgency(leg.agencyId);
                leg.agencyName = agency.getName();
                leg.agencyUrl = agency.getUrl();
            }
        }
        leg.mode = en.getMode().toString();
        leg.startTime = makeCalendar(state.getBackState());
    }

    private void finalizeLeg(Leg leg, State state, List<State> states, int start, int end,
            CoordinateArrayListSequence coordinates) {
        if (start != -1) {
            leg.walkSteps = getWalkSteps(states.subList(start, end + 1));
        }
        leg.endTime = makeCalendar(state.getBackState());
        Geometry geometry = geometryFactory.createLineString(coordinates);
        leg.legGeometry = PolylineEncoder.createEncodings(geometry);
        leg.to = makePlace(state, true);
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
     * 
     * @param itinerary
     */
    private Leg makeLeg(Itinerary itinerary, State s) {
        Leg leg = new Leg();
        itinerary.addLeg(leg);
        leg.startTime = makeCalendar(s.getBackState());
        EdgeNarrative en = s.getBackEdgeNarrative();
        leg.distance = 0.0;
        leg.from = makePlace(s.getBackState(), false);
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

        itinerary.startTime = makeCalendar(startState);
        itinerary.endTime = makeCalendar(endState);
        itinerary.duration = endState.getTimeInMillis() - startState.getTimeInMillis();

        Graph graph = path.getRoutingContext().graph;
        FareService fareService = graph.getService(FareService.class);
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
    private Place makePlace(State state, boolean time) {
        Vertex v = state.getVertex();
        Coordinate endCoord = v.getCoordinate();
        String name = v.getName();
        Place place;
        if (time) {
            Calendar timeAtState = makeCalendar(state);
            place = new Place(endCoord.x, endCoord.y, name, timeAtState);
        } else {
            place = new Place(endCoord.x, endCoord.y, name);
        }

        if (v instanceof TransitVertex) {
            TransitVertex transitVertex = (TransitVertex) v;
            place.stopId = transitVertex.getStopId();
            place.stopCode = transitVertex.getStopCode();
            place.zoneId = state.getZone();
        }
        return place;
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
                step.streetName = ((ElevatorAlightEdge) edge).getName();

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
            } else if ((step.streetName != null && !step.streetName.equals(streetName))
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
                                // a tiny turn edge has no geometry, but the next
                                // edge will be a TurnEdge or PSE and will have direction
                                alternative = alternative.getToVertex().getOutgoingStreetEdges()
                                        .get(0);
                            }
                            if (alternative.getName().equals(streetName)) {
                                // alternatives that have the same name
                                // are usually caused by street splits
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
                        // in the case of a turn edge, we actually have to go back two steps to see
                        // where
                        // else we might be, as once we are on the streetvertex leading into this
                        // edge,
                        // we are stuck
                        State twoStatesBack = backState.getBackState();
                        Vertex backVertex = twoStatesBack.getVertex();
                        for (Edge alternative : backVertex.getOutgoingStreetEdges()) {
                            List<Edge> alternatives = alternative.getToVertex()
                                    .getOutgoingStreetEdges();
                            if (alternatives.size() == 0) {
                                continue; // this is not an alternative
                            }
                            alternative = alternatives.get(0);
                            if (alternative.getName().equals(streetName)) {
                                // alternatives that have the same name
                                // are usually caused by street splits
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

            if (!createdNewStep) {
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
