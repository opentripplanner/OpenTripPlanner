package org.opentripplanner.routing.fares;

import com.google.common.collect.Lists;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.api.resource.CalendarServiceDataStub;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverterTest;
import org.opentripplanner.calendar.impl.CalendarServiceImpl;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * This is a helper test class that is used to generate Graph Paths using basic builder-strategy methods that can add
 * "legs" as seen in an output itineraries.
 */
public class GraphPathBuilder {
    private static Logger LOG = LoggerFactory.getLogger(GraphPathBuilder.class);

    public static final String FEED_ID = "feed_id";
    private static final Agency AGENCY = new Agency();
    private static final TimeZone timeZone = new SimpleTimeZone(2, "CEST");;

    static {
        AGENCY.setId("Agency");
        AGENCY.setName("Agency");
    }

    List<Leg> legs = new LinkedList<>();
    private int nextStopId = 1;
    private int nextVertexId = 1;

    /**
     * Build a graph path given the configured legs.
     */
    public GraphPath build() {
        GeometryFactory geometryFactory = new GeometryFactory();

        // create new Graph
        Graph graph = new Graph();

        // create origin and destination
        TemporaryStreetLocation origin = new TemporaryStreetLocation(
            "origin",
            new Coordinate(0, 0),
            new NonLocalizedString("origin"),
            false
        );
        TemporaryStreetLocation destination = new TemporaryStreetLocation(
            "destination",
            new Coordinate(0, 0),
            new NonLocalizedString("destination"),
            true
        );

        // create routing request and context
        RoutingRequest options = new RoutingRequest("WALK,TRANSIT");
        options.dateTime = 0L;
        RoutingContext context = new RoutingContext(options, graph, origin, destination);
        options.rctx = context;

        // Set service days in context

        // create trip IDs need to be created up-front in order to insert them in the graph. Since service codes are
        // added with increasing integers it is assumed that tripIds and service codes increase together.
        int nextTripId = 0;

        for (Leg leg : legs) {
            if (!leg.mode.isTransit()) continue;

            FeedScopedId tripId = new FeedScopedId(FEED_ID, "trip " + nextTripId);
            graph.serviceCodes.put(tripId, nextTripId);

            nextTripId++;
        }

        CalendarServiceData calendarServiceData = new CalendarServiceDataStub(graph.serviceCodes.keySet(), timeZone);
        CalendarServiceImpl calendarServiceImpl = new CalendarServiceImpl(calendarServiceData);
        ServiceDay serviceDay = new ServiceDay(graph, 0, calendarServiceImpl, FEED_ID);
        context.serviceDays = Lists.newArrayList(serviceDay);

        // initialize various things in graph
        graph.putService(CalendarServiceData.class, calendarServiceData);

        // create first state
        State curState = new State(options);
        LOG.info("Initiatlized first state. (State: {})", curState);

        // create street vertex that will be used to link origin to street network
        IntersectionVertex v1 = makeNewIntersectionVertex(graph);

        // create edge from origin to first vertex of street edge
        TemporaryFreeEdge linkToGraph = new TemporaryFreeEdge(origin, v1);

        // traverse from origin into street network
        curState = linkToGraph.traverse(curState);
        LOG.info("Traversed to street network. (State: {})", curState);
        Vertex currentVertex = v1;

        // create some helper variables to use while iterating through legs
        nextTripId = 0;
        int nextBlockId = 1;
        String blockId = null;
        int nextEdgeId = 2;
        boolean previousInterlined = false;
        Trip previousTrip = null;
        for (Leg leg : legs) {
            if (leg.mode.isTransit()) {
                // create and traverse sequence of edges that result in taking a single-stop transit trip

                if (currentVertex instanceof StreetVertex) {
                    // currently on street network, need to transition to a transit stop

                    // create transit stop vertex and associated link from street network
                    TransitStop transitStop = new TransitStop(graph, makeNewStop());
                    StreetTransitLink streetTransitLink = new StreetTransitLink(
                        (StreetVertex) currentVertex,
                        transitStop,
                        false
                    );

                    // traverse to the transit stop
                    curState = streetTransitLink.traverse(curState);
                    LOG.info("Traversed to transit stop. (State: {})", curState);
                    currentVertex = transitStop;
                }

                // resolve stops
                Stop fromStop = previousInterlined
                    ? ((PatternArriveVertex) currentVertex).getStop()
                    : ((TransitStop) currentVertex).getStop();
                Stop toStop = makeNewStop();

                // create schedule for leg's trip

                // trip
                Trip trip = new Trip();
                trip.setId(new FeedScopedId(FEED_ID, "trip " + nextTripId++));
                if (!previousInterlined && leg.interlineWithNext) {
                    // create new block ID for interlining
                    blockId = "block" + nextBlockId++;
                }
                if (leg.interlineWithNext || previousInterlined) {
                    trip.setBlockId(blockId);
                }
                trip.setRoute(leg.route);

                // stop depart
                StopTime stopDepartTime = new StopTime();
                stopDepartTime.setTrip(trip);
                stopDepartTime.setStop(fromStop);
                stopDepartTime.setStopSequence(Integer.MIN_VALUE);
                stopDepartTime.setDepartureTime(leg.departureTime);
                stopDepartTime.setPickupType(3);

                // stop arrive
                StopTime stopArriveTime = new StopTime();
                stopArriveTime.setTrip(trip);
                stopArriveTime.setStop(toStop);
                stopArriveTime.setStopSequence(1);
                stopArriveTime.setArrivalTime(leg.arrivalTime);

                // pattern
                List<StopTime> stopTimes = Lists.newArrayList(stopDepartTime, stopArriveTime);
                StopPattern stopPattern  = new StopPattern(stopTimes);
                TripPattern tripPattern  = new TripPattern(leg.route, stopPattern);
                TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());
                tripTimes.serviceCode = graph.serviceCodes.get(trip.getId());
                tripPattern.add(tripTimes);

                // create pattern depart vertex
                PatternDepartVertex patternDepartVertex = new PatternDepartVertex(graph, tripPattern, 0);

                // pattern hop edge and pattern arrive vertex (these must be created before other edges to ensure proper
                // data is available for other
                // edges)
                PatternArriveVertex patternArriveVertex = new PatternArriveVertex(graph, tripPattern, 1);
                PatternHop patternHop = new PatternHop(
                    patternDepartVertex,
                    patternArriveVertex,
                    fromStop,
                    toStop,
                    0
                );

                if (previousInterlined) {
                    // the previous leg indicated an interlining. Create the appropriate PatternInterlineDwell edge.
                    PatternInterlineDwell patternInterlineDwell = new PatternInterlineDwell(
                        (PatternArriveVertex) currentVertex,
                        patternDepartVertex
                    );
                    patternInterlineDwell.add(previousTrip, trip);
                    curState = patternInterlineDwell.traverse(curState);
                    LOG.info("Traversed interlined routes. (State: {})", curState);
                } else {
                    // pre-board edge
                    TransitStopDepart transitStopDepart = new TransitStopDepart(
                        graph,
                        fromStop,
                        (TransitStop) currentVertex
                    );
                    PreBoardEdge preBoardEdge = new PreBoardEdge((TransitStop) currentVertex, transitStopDepart);
                    curState = preBoardEdge.traverse(curState);
                    LOG.info("Traversed across pre board edge. (State: {})", curState);

                    // transit board alight edge
                    TransitBoardAlight boardEdge = new TransitBoardAlight(
                        transitStopDepart,
                        patternDepartVertex,
                        0,
                        TraverseMode.BUS
                    );
                    curState = boardEdge.traverse(curState);
                    LOG.info("Traversed board edge. (State: {})", curState);
                }

                curState = patternHop.traverse(curState);
                LOG.info("Traversed pattern hop. (State: {})", curState);

                if (leg.interlineWithNext) {
                    // the transit leg interlines, so don't create the alighting edges. The PatternInterlineDwell is
                    // created in the next loop.
                    currentVertex = patternArriveVertex;
                    previousInterlined = true;
                    previousTrip = trip;
                    continue;
                }

                // transit board alight edge
                TransitStop toTransitStopVertex = new TransitStop(graph, toStop);
                TransitStopArrive transitStopArrive = new TransitStopArrive(graph, toStop, toTransitStopVertex);
                TransitBoardAlight alightEdge = new TransitBoardAlight(
                    patternArriveVertex,
                    transitStopArrive,
                    1,
                    TraverseMode.BUS
                );
                curState = alightEdge.traverse(curState);
                LOG.info("Traversed alight edge. (State: {})", curState);

                // pre alight edge
                PreAlightEdge preAlightEdge = new PreAlightEdge(transitStopArrive, toTransitStopVertex);
                curState = preAlightEdge.traverse(curState);
                LOG.info("Traversed pre alight edge. (State: {})", curState);
                currentVertex = toTransitStopVertex;
            } else {
                TransitionResult transitionResult = transitionToStreetNetworkIfNeeded(
                    graph,
                    currentVertex,
                    curState,
                    previousInterlined
                );
                curState = transitionResult.curState;
                currentVertex = transitionResult.currentVertex;

                // create new street edge to traverse
                IntersectionVertex nextVertex = makeNewIntersectionVertex(graph);
                PackedCoordinateSequence streetEdge1Coordinates = new PackedCoordinateSequence.Double(
                    new double[]{0, 0, 1, 1, 0, 0}, 2);
                LineString streetEdgeLineString = new LineString(streetEdge1Coordinates, geometryFactory);
                StreetEdge streetEdge = new StreetEdge(
                    (StreetVertex) currentVertex,
                    nextVertex,
                    streetEdgeLineString,
                    "Street Edge " + nextEdgeId++,
                    // assuming the default 1.33 meters per second walking speed, this creates the expected walking
                    // distance that would be covered given the leg duration in minutes. This doesn't match the dummy
                    // LineString, but FIXME hopefully that's not an issue?
                    1.33 * leg.durationInMinutes * 60,
                    StreetTraversalPermission.ALL,
                    false
                );
                curState = streetEdge.traverse(curState);
                LOG.info("Traversed across a street edge. (State: {})", curState);
                currentVertex = nextVertex;
            }
            previousInterlined = false;
        }

        // finished with adding legs.

        // transition back to the street network if needed
        TransitionResult transitionResult = transitionToStreetNetworkIfNeeded(
            graph,
            currentVertex,
            curState,
            previousInterlined
        );
        curState = transitionResult.curState;
        currentVertex = transitionResult.currentVertex;

        // create edge from last vertex to destination
        TemporaryFreeEdge linkToDestination = new TemporaryFreeEdge(currentVertex, destination);

        // traverse from street network to destination
        curState = linkToDestination.traverse(curState);
        LOG.info("Traversed from street network to destination. (State: {})", curState);

        return new GraphPath(curState, false);
    }

    private IntersectionVertex makeNewIntersectionVertex(Graph graph) {
        return new IntersectionVertex(
            graph,
            "Vertex " + nextVertexId++,
            0,
            0
        );
    }

    private TransitionResult transitionToStreetNetworkIfNeeded(
        Graph graph,
        Vertex currentVertex,
        State curState,
        boolean previousInterlined
    ) {
        TransitionResult transitionResult = new TransitionResult();

        if (currentVertex instanceof TransitStop) {
            // must transition to street network from a transit stop
            if (previousInterlined) {
                throw new IllegalStateException(
                    "Previous leg specified interlinedWithNext, but either no more legs or this leg is not a transit leg!"
                );
            }
        } else {
            // not currently on transit network, return current vertex and state
            transitionResult.currentVertex = currentVertex;
            transitionResult.curState = curState;
            return transitionResult;
        }

        IntersectionVertex streetVertex = makeNewIntersectionVertex(graph);
        StreetTransitLink streetTransitLink = new StreetTransitLink(
            (TransitStop) currentVertex,
            streetVertex,
            false
        );

        // traverse to the street network
        transitionResult.curState = streetTransitLink.traverse(curState);
        LOG.info("Traversed to from transit network to street network. (State: {})", curState);
        transitionResult.currentVertex = streetVertex;
        return transitionResult;
    }

    private Stop makeNewStop() {
        Stop stop = new Stop();
        String stopName = "Stop " + nextStopId++;
        stop.setId(new FeedScopedId(FEED_ID, stopName));
        stop.setName(stopName);
        stop.setLon(0);
        stop.setLat(0);
        return stop;
    }

    public static Route makeNewRoute(String shortName, String longName) {
        Route route = new Route();
        route.setId(new FeedScopedId(FEED_ID, shortName));
        route.setAgency(AGENCY);
        route.setShortName(shortName);
        route.setLongName(longName);
        return route;
    }

    public GraphPathBuilder addTransitLeg(Route route, int departureTime, int arrivalTime) {
        Leg transitLeg = new Leg();
        transitLeg.mode = TraverseMode.BUS;
        transitLeg.departureTime = departureTime;
        transitLeg.arrivalTime = arrivalTime;
        transitLeg.route = route;
        legs.add(transitLeg);
        return this;
    }

    public GraphPathBuilder addInterlinedTransitLeg(Route route, int departureTime, int arrivalTime) {
        Leg transitLeg = new Leg();
        transitLeg.mode = TraverseMode.BUS;
        transitLeg.departureTime = departureTime;
        transitLeg.arrivalTime = arrivalTime;
        transitLeg.interlineWithNext = true;
        transitLeg.route = route;
        legs.add(transitLeg);
        return this;
    }

    public GraphPathBuilder addWalkLeg(long durationInMinutes) {
        Leg walkLeg = new Leg();
        walkLeg.mode = TraverseMode.WALK;
        walkLeg.durationInMinutes = durationInMinutes;
        legs.add(walkLeg);
        return this;
    }

    private class Leg {
        /**
         * Only applies to transit legs
         */
        public int arrivalTime;

        /**
         * Only applies to transit legs
         */
        public int departureTime;

        /**
         * Only applies to non-transit legs
         */
        public long durationInMinutes;

        /**
         * Only applies to transit legs
         */
        public boolean interlineWithNext;

        public TraverseMode mode;

        /**
         * Only applies to transit legs
         */
        public Route route;
    }

    private class TransitionResult {
        public State curState;
        public Vertex currentVertex;
    }
}
