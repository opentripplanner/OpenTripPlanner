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
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.DwellEdge;
import org.opentripplanner.routing.edgetype.EdgeWithElevation;
import org.opentripplanner.routing.edgetype.ElevatorAlightEdge;
import org.opentripplanner.routing.edgetype.ElevatorBoardEdge;
import org.opentripplanner.routing.edgetype.ElevatorEdge;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.HopEdge;
import org.opentripplanner.routing.edgetype.LegSwitchingEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.TrivialPathException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.GraphService;
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

@Service @Scope("singleton")
public class PlanGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(PlanGenerator.class);

    private static final double MAX_ZAG_DISTANCE = 30;

    @Autowired public PathService pathService;
    @Autowired GraphService graphService;
    
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
            if (paths == null && options.isWheelchairAccessible()) {
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
            Itinerary itinerary = generateItinerary(path, request.isShowIntermediateStops());
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
        Set<Alert> postponedAlerts = null;
        Leg leg = null;
        CoordinateArrayListSequence coordinates = new CoordinateArrayListSequence();
        double previousElevation = Double.MAX_VALUE;
        int startWalk = -1;
        int i = -1;
        boolean foldingElevatorLegIntoCycleLeg = false;
        PlanGenState pgstate = PlanGenState.START;
        String nextName = null;
        for (State state : path.states) {
            i += 1;
            Edge backEdge = state.getBackEdge();
            if (backEdge == null) {
                continue;
            }

// debug: push vehicle late status out to UI
//            if (backEdge instanceof PatternHop) {
//                TripTimes tt = state.getTripTimes();
//                int hop = ((PatternHop)backEdge).stopIndex;
//                LOG.info("{} {}", tt.getTrip().toString(), hop);
//                if ( ! tt.isScheduled()) {
//                    int delay = tt.getDepartureDelay(hop);
//                    String d = "on time";
//                    if (Math.abs(delay) > 10) {
//                        d = String.format("%2.1f min %s", delay / 60.0, 
//                                (delay < 0) ? "early" : "late");
//                    }
//                    d = "Using real-time delay information: ".concat(d);
//                    leg.addAlert(Alert.createSimpleAlerts(d));
//                    LOG.info(d);
//                } 
//                else {
//                    leg.addAlert(Alert.createSimpleAlerts("Using published timetables."));
//                    LOG.info("sched");
//                }
//            }

            TraverseMode mode = state.getBackMode();
            if (mode != null) {
                long dt = state.getAbsTimeDeltaSec();
                if (mode == TraverseMode.BOARDING || mode == TraverseMode.ALIGHTING
                        || mode == TraverseMode.STL) {
                    itinerary.waitingTime += dt;
                } else if (mode.isOnStreetNonTransit()) {
                    itinerary.walkDistance += backEdge.getDistance();
                    itinerary.walkTime += dt;
                } else if (mode.isTransit()) {
                    itinerary.transitTime += dt;
                }
            }

            if (backEdge instanceof FreeEdge) {
                if (backEdge instanceof PreBoardEdge) {
                    // Add boarding alerts to the next leg
                    postponedAlerts = state.getBackAlerts();
                } else if (backEdge instanceof PreAlightEdge) {
                    // Add alighting alerts to the previous leg
                    addNotesToLeg(itinerary.legs.get(itinerary.legs.size() - 1), state.getBackAlerts());
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
                    itinerary.transfers++;
                    startWalk = -1;
                } else if (mode == TraverseMode.STL) {
                    // this comes after an alight; do nothing
                } else if (mode == TraverseMode.TRANSFER) {
                    // handle the whole thing in one step
                    leg = makeLeg(itinerary, state);
                    coordinates = new CoordinateArrayListSequence();
                    coordinates.add(state.getBackState().getVertex().getCoordinate());
                    coordinates.add(state.getVertex().getCoordinate());
                    finalizeLeg(leg, state, path.states, i, i, coordinates, null);
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
                    finalizeLeg(leg, state, path.states, startWalk, i, coordinates, itinerary);
                    startWalk = i;
                    leg = makeLeg(itinerary, state);
                    pgstate = PlanGenState.BICYCLE;
                } else if (mode == TraverseMode.STL) {
                    finalizeLeg(leg, state, path.states, startWalk, i, coordinates, itinerary);
                    leg = null;
                    pgstate = PlanGenState.PRETRANSIT;
                } else if (mode == TraverseMode.BOARDING) {
                    // this only happens in case of a timed transfer.
                    pgstate = PlanGenState.PRETRANSIT;
                    finalizeLeg(leg, state, path.states, startWalk, i, coordinates, itinerary);
                    leg = makeLeg(itinerary, state);
                    itinerary.transfers++;
                } else if (backEdge instanceof LegSwitchingEdge) {
                    nextName = state.getBackState().getBackState().getBackState().getVertex()
                            .getName();
                    finalizeLeg(leg, state, path.states, startWalk, i - 1, coordinates, itinerary);
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
                
                // If there are elevator edges that have mode == BICYCLE on both sides, they should
                // be folded into the bicycle leg. But ones with walk on one side or the other should
                // not
                if (state.getBackEdge() instanceof ElevatorBoardEdge) {
                    int j = i + 1;
                    // proceed forward from the current state until we find one that isn't on an
                    // elevator, and check the traverse mode
                    while (path.states.get(j).getBackEdge() instanceof ElevatorEdge)
                        j++;
                    
                    // path.states[j] is not an elevator edge
                    if (path.states.get(j).getBackMode() == TraverseMode.BICYCLE)
                        foldingElevatorLegIntoCycleLeg = true;
                }
                
                if (foldingElevatorLegIntoCycleLeg) {
                    if (state.getBackEdge() instanceof ElevatorEdge) {
                        break; // from the case
                    }
                    else {
                        foldingElevatorLegIntoCycleLeg = false;
                        // do not break but allow it to be processed below (which will do nothing)
                    }
                }
                
                
                if (mode == TraverseMode.BICYCLE) {
                    // do nothing
                } else if (mode == TraverseMode.WALK) {
                    finalizeLeg(leg, state, path.states, startWalk, i, coordinates, itinerary);
                    leg = makeLeg(itinerary, state);
                    startWalk = i;
                    pgstate = PlanGenState.WALK;
                } else if (mode == TraverseMode.STL) {
                    finalizeLeg(leg, state, path.states, startWalk, i, coordinates, itinerary);
                    startWalk = i;
                    leg = null;
                    pgstate = PlanGenState.PRETRANSIT;
                } else if (backEdge instanceof LegSwitchingEdge) {
                    finalizeLeg(leg, state, path.states, startWalk, i - 1, coordinates, itinerary);
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
                    finalizeLeg(leg, state, path.states, startWalk, i, coordinates, itinerary);
                    leg = null;
                    pgstate = PlanGenState.PRETRANSIT;
                } else if (backEdge instanceof LegSwitchingEdge) {
                    finalizeLeg(leg, state, path.states, startWalk, i - 1, coordinates, itinerary);
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
                        leg.stop = new ArrayList<Place>();
                        itinerary.transfers++;
                        leg.boardRule = (String) state.getExtension("boardAlightRule");
                    }
                } else if (backEdge instanceof HopEdge) {
                    pgstate = PlanGenState.TRANSIT;
                    fixupTransitLeg(leg, state, transitIndex);
                    leg.stop = new ArrayList<Place>();
                } else {
                    LOG.error("Unexpected state (in PRETRANSIT): " + mode);
                }
                break;
            case TRANSIT:
                String route = backEdge.getName();
                if (mode == TraverseMode.ALIGHTING) {
                    if (showIntermediateStops && leg.stop != null && leg.stop.size() > 0) {
                        if (leg.stop.isEmpty()) {
                            leg.stop = null;
                        }
                    }
                    leg.alightRule = (String) state.getExtension("boardAlightRule");
                    finalizeLeg(leg, state, null, -1, -1, coordinates, itinerary);
                    leg = null;
                    pgstate = PlanGenState.START;
                } else if (mode.toString().equals(leg.mode)) {
                    // no mode change, handle intermediate stops
                    if (showIntermediateStops) {
                        /*
                         * any further transit edge, add "from" vertex to intermediate stops
                         */
                        if (!(backEdge instanceof DwellEdge)) {
                            Place stop = makePlace(state.getBackState(), state.getBackState().getVertex().getName(), true);
                            leg.stop.add(stop);
                        } else if (leg.stop.size() > 0) {
                            leg.stop.get(leg.stop.size() - 1).departure = makeCalendar(state);
                        }
                    }
                    if (!route.equals(leg.route)) {
                        // interline dwell
                        finalizeLeg(leg, state, null, -1, -1, coordinates, itinerary);
                        leg = makeLeg(itinerary, state);
                        leg.stop = new ArrayList<Place>();
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
                leg.distance += backEdge.getDistance();
                Geometry edgeGeometry = backEdge.getGeometry();
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

                addNotesToLeg(leg, state.getBackAlerts());

            }

        } /* end loop over graphPath edge list */

        if (leg != null) {
            finalizeLeg(leg, path.states.getLast(), path.states, startWalk, i, coordinates, itinerary);
        }
        itinerary.removeBogusLegs();
        itinerary.fixupDates(graph.getService(CalendarServiceData.class));
        if (itinerary.legs.size() == 0)
            throw new TrivialPathException();
        return itinerary;
    }

    private Calendar makeCalendar(State state) {
        RoutingContext rctx = state.getContext();
        TimeZone timeZone = rctx.graph.getTimeZone(); 
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(state.getTimeInMillis());
        return calendar;
    }

    private void fixupTransitLeg(Leg leg, State state, TransitIndexService transitIndex) {
        Edge en = state.getBackEdge();
        leg.route = en.getName();
        Trip trip = state.getBackTrip();
        leg.headsign = state.getBackDirection();
        if (trip != null) {
            // this is the stop headsign
             //leg.headsign = "This is the headsign";
            // handle no stop headsign
            if (leg.headsign == null)            
                leg.headsign = trip.getTripHeadsign();
            
            leg.tripId = trip.getId().getId();
            leg.agencyId = trip.getId().getAgencyId();
            leg.tripShortName = trip.getTripShortName();
            leg.routeShortName = trip.getRoute().getShortName();
            leg.routeLongName = trip.getRoute().getLongName();
            leg.routeColor = trip.getRoute().getColor();
            leg.routeTextColor = trip.getRoute().getTextColor();
            leg.routeId = trip.getRoute().getId().getId();
            if (transitIndex != null) {
                Agency agency = transitIndex.getAgency(leg.agencyId);
                leg.agencyName = agency.getName();
                leg.agencyUrl = agency.getUrl();
            }
        }
        leg.mode = state.getBackMode().toString();
        leg.startTime = makeCalendar(state.getBackState());
    }

    private void finalizeLeg(Leg leg, State state, List<State> states, int start, int end,
            CoordinateArrayListSequence coordinates, Itinerary itinerary) {

        //this leg has already been added to the itinerary, so we actually want the penultimate leg, if any
        if (states != null) {
            int extra = 0;
            WalkStep continuation = null;
            if (itinerary.legs.size() >= 2) {
                Leg previousLeg = itinerary.legs.get(itinerary.legs.size() - 2);
                if (previousLeg.walkSteps != null) {
                    continuation = previousLeg.walkSteps.get(previousLeg.walkSteps.size() - 1);
                    extra = 1;
                }
            }
            if (end == states.size() - 1) {
                end += 1;
            }

            leg.walkSteps = getWalkSteps(states.subList(start, end + extra), continuation);
        }
        leg.endTime = makeCalendar(state.getBackState());
        Geometry geometry = GeometryUtils.getGeometryFactory().createLineString(coordinates);
        leg.legGeometry = PolylineEncoder.createEncodings(geometry);
        Edge backEdge = state.getBackEdge();
        String name;
        if (backEdge instanceof StreetEdge) {
            name = backEdge.getName();
        } else {
            name = state.getVertex().getName();
        }
        leg.to = makePlace(state, name, true);
        coordinates.clear();
    }

    private Set<Alert> addNotesToLeg(Leg leg, Set<Alert> notes) {
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
        leg.distance = 0.0;
        String name;
        Edge backEdge = s.getBackEdge();
        if (backEdge instanceof StreetEdge) {
            name = backEdge.getName();
        } else {
            name = s.getVertex().getName();
        }
        leg.from = makePlace(s.getBackState(), name, false);
        leg.mode = s.getBackMode().toString();
        if (s.isBikeRenting()) {
            leg.rentedBike = true; 
        }
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
    private Place makePlace(State state, String name, boolean time) {
        Vertex v = state.getVertex();
        Coordinate endCoord = v.getCoordinate();
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
     * @param previous a non-transit leg that immediately precedes this one (bike-walking, say), or null
     * 
     * @param edges : A list of street edges
     * @return
     */
    private List<WalkStep> getWalkSteps(List<State> states, WalkStep previous) {
        List<WalkStep> steps = new ArrayList<WalkStep>();
        WalkStep step = null;
        double lastAngle = 0, distance = 0; // distance used for appending elevation profiles
        int roundaboutExit = 0; // track whether we are in a roundabout, and if so the exit number
        String roundaboutPreviousStreet = null;

        for (State currState : states) {
            State backState = currState.getBackState();
            Edge edge = currState.getBackEdge();
            boolean createdNewStep = false, disableZagRemovalForThisStep = false;
            if (edge instanceof FreeEdge) {
                continue;
            }
            if (currState.getBackMode() == null || !currState.getBackMode().isOnStreetNonTransit()) {
                continue; // ignore STLs and the like
            }
            Geometry geom = edge.getGeometry();
            if (geom == null) {
                continue;
            }

            // generate a step for getting off an elevator (all
            // elevator narrative generation occurs when alighting). We don't need to know what came
            // before or will come after
            if (edge instanceof ElevatorAlightEdge) {
                // don't care what came before or comes after
                step = createWalkStep(currState);
                createdNewStep = true;
                disableZagRemovalForThisStep = true;

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

            String streetName = edge.getName();
            int idx = streetName.indexOf('(');
            String streetNameNoParens;
            if (idx > 0)
                streetNameNoParens = streetName.substring(0, idx - 1);
            else
                streetNameNoParens = streetName;

            if (step == null) {
                // first step
                step = createWalkStep(currState);
                createdNewStep = true;

                steps.add(step);
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                if (previous == null) {
                    step.setAbsoluteDirection(thisAngle);
                } else {
                    step.setDirections(previous.angle, thisAngle, false);
                }
                // new step, set distance to length of first edge
                distance = edge.getDistance();
            } else if (((step.streetName != null && !step.streetNameNoParens().equals(streetNameNoParens))
                    && (!step.bogusName || !edge.hasBogusName())) ||
                    // if we are on a roundabout now and weren't before, start a new step
                    edge.isRoundabout() != (roundaboutExit > 0)) {
                /* street name has changed, or we've changed state from a roundabout to a street */
                if (roundaboutExit > 0) {
                    // if we were just on a roundabout,
                    // make note of which exit was taken in the existing step
                    step.exit = Integer.toString(roundaboutExit); // ordinal numbers from
                    if (streetNameNoParens.equals(roundaboutPreviousStreet)) {
                        step.stayOn = true;
                    }
                    // localization
                    roundaboutExit = 0;
                }
                /* start a new step */
                step = createWalkStep(currState);
                createdNewStep = true;

                steps.add(step);
                if (edge.isRoundabout()) {
                    // indicate that we are now on a roundabout
                    // and use one-based exit numbering
                    roundaboutExit = 1;
                    roundaboutPreviousStreet = backState.getBackEdge().getName();
                    idx = roundaboutPreviousStreet.indexOf('(');
                    if (idx > 0)
                        roundaboutPreviousStreet = roundaboutPreviousStreet.substring(0, idx - 1);
                }
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                step.setDirections(lastAngle, thisAngle, edge.isRoundabout());
                // new step, set distance to length of first edge
                distance = edge.getDistance();
            } else {
                /* street name has not changed */
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                RelativeDirection direction = WalkStep.getRelativeDirection(lastAngle, thisAngle,
                        edge.isRoundabout());
                boolean optionsBefore = backState.multipleOptionsBefore();
                if (edge.isRoundabout()) {
                    // we are on a roundabout, and have already traversed at least one edge of it.
                    if (optionsBefore) {
                        // increment exit count if we passed one.
                        roundaboutExit += 1;
                    }
                }
                if (edge.isRoundabout() || direction == RelativeDirection.CONTINUE) {
                    // we are continuing almost straight, or continuing along a roundabout.
                    // just append elevation info onto the existing step.

                } else {
                    // we are not on a roundabout, and not continuing straight through.

                    // figure out if there were other plausible turn options at the last
                    // intersection
                    // to see if we should generate a "left to continue" instruction.
                    boolean shouldGenerateContinue = false;
                    if (edge instanceof PlainStreetEdge) {
                        // the next edges will be PlainStreetEdges, we hope
                        double angleDiff = getAbsoluteAngleDiff(thisAngle, lastAngle);
                        for (Edge alternative : backState.getVertex().getOutgoingStreetEdges()) {
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
                    } else {
                        double angleDiff = getAbsoluteAngleDiff(lastAngle, thisAngle);
                        // FIXME: this code might be wrong with the removal of the edge-based graph
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
                        distance = edge.getDistance();
                    }
                }
            }

            if (createdNewStep && !disableZagRemovalForThisStep && currState.getBackMode() == backState.getBackMode()) {
                //check last three steps for zag
                int last = steps.size() - 1;
                if (last >= 2) {
                    WalkStep threeBack = steps.get(last - 2);
                    WalkStep twoBack = steps.get(last - 1);
                    WalkStep lastStep = steps.get(last);

                    if (twoBack.distance < MAX_ZAG_DISTANCE
                            && lastStep.streetNameNoParens().equals(threeBack.streetNameNoParens())) {
                        // total hack to remove zags.
                        steps.remove(last);
                        steps.remove(last - 1);
                        step = threeBack;
                        step.distance += twoBack.distance;
                        distance += step.distance;
                        if (twoBack.elevation != null) {
                            if (step.elevation == null) {
                                step.elevation = twoBack.elevation;
                            } else {
                                for (P2<Double> d : twoBack.elevation) {
                                    step.elevation.add(new P2<Double>(d.getFirst() + step.distance, d.getSecond()));
                                }
                            }
                        }
                    }
                }
            } else {
                if (!createdNewStep && step.elevation != null) {
                    List<P2<Double>> s = encodeElevationProfile(edge, distance);
                    if (step.elevation != null && step.elevation.size() > 0) {
                        step.elevation.addAll(s);
                    } else {
                        step.elevation = s;
                    }
                }
                distance += edge.getDistance();

            }

            // increment the total length for this step
            step.distance += edge.getDistance();
            step.addAlerts(currState.getBackAlerts());
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
        Edge en = s.getBackEdge();
        WalkStep step;
        step = new WalkStep();
        step.streetName = en.getName();
        step.lon = en.getFromVertex().getX();
        step.lat = en.getFromVertex().getY();
        step.elevation = encodeElevationProfile(s.getBackEdge(), 0);
        step.bogusName = en.hasBogusName();
        step.addAlerts(s.getBackAlerts());
        step.angle = DirectionUtils.getFirstAngle(s.getBackEdge().getGeometry());
        return step;
    }

    private List<P2<Double>> encodeElevationProfile(Edge edge, double offset) {
        if (!(edge instanceof EdgeWithElevation)) {
            return new ArrayList<P2<Double>>();
        }
        EdgeWithElevation elevEdge = (EdgeWithElevation) edge;
        if (elevEdge.getElevationProfile() == null) {
            return new ArrayList<P2<Double>>();
        }
        ArrayList<P2<Double>> out = new ArrayList<P2<Double>>();
        Coordinate[] coordArr = elevEdge.getElevationProfile().toCoordinateArray();
        for (int i = 0; i < coordArr.length; i++) {
            out.add(new P2<Double>(coordArr[i].x + offset, coordArr[i].y));
        }
        return out;
    }

    /** Returns the first trip of the service day. */
    public TripPlan generateFirstTrip(RoutingRequest request) {
        Graph graph = graphService.getGraph(request.getRouterId());

        TransitIndexService transitIndex = graph.getService(TransitIndexService.class);
        transitIndexWithBreakRequired(transitIndex);

        request.setArriveBy(false);

        TimeZone tz = graph.getTimeZone();

        GregorianCalendar calendar = new GregorianCalendar(tz);
        calendar.setTimeInMillis(request.dateTime * 1000);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.AM_PM, 0);
        calendar.set(Calendar.SECOND, transitIndex.getOvernightBreak());

        request.dateTime = calendar.getTimeInMillis() / 1000;
        return generate(request);
    }

    /** Return the last trip of the service day */
    public TripPlan generateLastTrip(RoutingRequest request) {
        Graph graph = graphService.getGraph(request.getRouterId());

        TransitIndexService transitIndex = graph.getService(TransitIndexService.class);
        transitIndexWithBreakRequired(transitIndex);

        request.setArriveBy(true);

        TimeZone tz = graph.getTimeZone();

        GregorianCalendar calendar = new GregorianCalendar(tz);
        calendar.setTimeInMillis(request.dateTime * 1000);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.AM_PM, 0);
        calendar.set(Calendar.SECOND, transitIndex.getOvernightBreak());
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        request.dateTime = calendar.getTimeInMillis() / 1000;

        return generate(request);
    }

    private void transitIndexWithBreakRequired(TransitIndexService transitIndex) {
        transitIndexRequired(transitIndex);
        if (transitIndex.getOvernightBreak() == -1) {
            throw new RuntimeException("TransitIndexBuilder could not find an overnight break "
                    + "in the transit schedule; first/last trips are undefined");
        }
    }

    private void transitIndexRequired(TransitIndexService transitIndex) {
        if (transitIndex == null) {
            throw new RuntimeException(
                    "TransitIndexBuilder is required for first/last/next/previous trip");
        }
    }

}
