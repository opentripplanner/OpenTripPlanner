package org.opentripplanner.routing.flex;

import com.google.common.collect.Maps;
import org.apache.commons.math3.util.Pair;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.edgetype.flex.TemporaryDirectPatternHop;
import org.opentripplanner.routing.edgetype.flex.TemporaryPartialPatternHop;
import org.opentripplanner.routing.edgetype.flex.TemporaryPreAlightEdge;
import org.opentripplanner.routing.edgetype.flex.TemporaryPreBoardEdge;
import org.opentripplanner.routing.edgetype.flex.TemporaryStreetTransitLink;
import org.opentripplanner.routing.edgetype.flex.TemporaryTransitBoardAlight;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.opentripplanner.routing.vertextype.flex.TemporaryPatternArriveVertex;
import org.opentripplanner.routing.vertextype.flex.TemporaryPatternDepartVertex;
import org.opentripplanner.routing.vertextype.flex.TemporaryTransitStop;
import org.opentripplanner.routing.vertextype.flex.TemporaryTransitStopArrive;
import org.opentripplanner.routing.vertextype.flex.TemporaryTransitStopDepart;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * Create temporary vertices and edges for GTFS-flex service.
 */
public abstract class GtfsFlexGraphModifier {

    private static final Logger LOG = LoggerFactory.getLogger(GtfsFlexGraphModifier.class);

    protected Graph graph;

    private Map<StreetVertex, TemporaryTransitStop> temporaryTransitStopsForLocation = Maps.newHashMap();

    protected GtfsFlexGraphModifier(Graph graph) {
        this.graph = graph;
    }

    /*
     * Overall pattern - do a graph search from the origin and destination according to some
     * parameters, in order to find nearby PatternHops and the States where they were found.
     * Based on those PatternHops/States, make new board and alight temporary edges/vertices.
     * Note: this was originally factored out of flag stop creation code.
     */

    /**
     * Return the mode that the graph search should be in.
     */
    public abstract TraverseMode getMode();

    /**
     * Return the termination strategy which should be used during the graph search.
     */
    public abstract SearchTerminationStrategy getSearchTerminationStrategy();

    // this the core of what's different: how the hops are created

    /**
     * Create a new {@link PatternHop} with a new "to" location (from the route to a new destination).
     *
     * @param opt Options for the graph search
     * @param state State at which the original PatternHop was found during the graph search
     * @param hop Original pattern hop to modify
     * @param to New "to" location
     * @param toStop Stop for new "to" location
     * @return new pattern hop
     */
    public abstract TemporaryPartialPatternHop makeHopNewTo(RoutingRequest opt, State state, PatternHop hop, PatternArriveVertex to, Stop toStop);

    /**
     * Create a new {@link PatternHop} with a new "from" location (from a new destination to the route).
     *
     * @param opt Options for the graph search
     * @param state Options for the graph search
     * @param hop Original pattern hop to modify
     * @param from New "from" location
     * @param fromStop Stop for new "from" location
     * @return new pattern hop
     */
    public abstract TemporaryPartialPatternHop makeHopNewFrom(RoutingRequest opt, State state, PatternHop hop, PatternDepartVertex from, Stop fromStop);

    /**
     * From an existing TemporaryPartialPatternHop which has a new "from" location, create a new
     * TemporaryPartialPatternHop with the same "from" location and a new "to" location. The
     * existence of this method implies that searches from the origin should take place prior
     * to searches from the destination.
     *
     * @param opt Options for the graph search
     * @param state ptions for the graph search
     * @param hop Temporary pattern hop to modify
     * @param to New "to" location
     * @param toStop Stop for new "to" location
     * @return new pattern hop
     */
    public abstract TemporaryPartialPatternHop shortenEnd(RoutingRequest opt, State state, TemporaryPartialPatternHop hop, PatternStopVertex to, Stop toStop);

    /**
     * Returns true if the given hop can be boarded/alighted.
     */
    public abstract boolean checkHopAllowsBoardAlight(State state, PatternHop hop, boolean boarding);

    /**
     * Subclasses can specify where the new temporary stop should be created, given a nearby
     * PatternHop.
     *
     * @param s State at which PatternHop was found
     * @param hop PatternHop found during graph search
     * @return location for new stop
     */
    public abstract StreetVertex getLocationForTemporaryStop(State s, PatternHop hop);

    public void vertexVisitor(State state) {}

    /**
     * Create temporary edges and vertices from the origin into the transit network.
     *
     * @param request request for graph search
     */
    public void createForwardHops(RoutingRequest request) {
        RoutingRequest forward = request.clone();
        forward.excludeWalking = false;
        forward.setMode(getMode());
        forward.setArriveBy(false);
        // allow discovery of stations even with car mode
        forward.enterStationsWithCar = true;
        streetSearch(forward);
    }

    /**
     * Create temporary edges and vertices from the transit network to the destination.
     *
     * @param request request for graph search
     */
    public void createBackwardHops(RoutingRequest request) {
        RoutingRequest backward = request.clone();
        backward.excludeWalking = false;
        backward.setMode(getMode());
        backward.setArriveBy(true);
        backward.enterStationsWithCar = true;
        streetSearch(backward);
    }

    protected void streetSearch(RoutingRequest rr) {
        if (TraverseMode.CAR.equals(getMode())) {
            modifyRequestForCarAccess(rr);
        }
        for(Pair<State, PatternHop> p : getClosestPatternHops(rr)) {
            State s = p.getKey();
            PatternHop hop = p.getValue();
            TemporaryTransitStop flagTransitStop = getTemporaryStop(s, hop);
            if (flagTransitStop == null) {
                continue;
            }
            if (rr.arriveBy) {
                createHopsToTemporaryStop(rr, s, flagTransitStop, hop);
            } else {
                createHopsFromTemporaryStop(rr, s, flagTransitStop, hop);
            }
        }
    }

    private TemporaryTransitStop getTemporaryStop(State s, PatternHop hop) {
        StreetVertex streetVertex = getLocationForTemporaryStop(s, hop);
        if (streetVertex == null) {
            return null;
        }
        return getTemporaryStop(streetVertex, s, s.getContext(), s.getOptions());
    }

    protected TemporaryTransitStop getTemporaryStop(StreetVertex streetVertex, State s, RoutingContext rctx, RoutingRequest options) {
        return getTemporaryStop(streetVertex, s, rctx, options, !options.arriveBy);
    }

    protected TemporaryTransitStop getTemporaryStop(StreetVertex streetVertex, State s, RoutingContext rctx, RoutingRequest options, boolean forwards) {
        if (temporaryTransitStopsForLocation.get(streetVertex) == null) {
            String name = findName(s, streetVertex, options.locale, forwards);
            TemporaryTransitStop stop = createTemporaryTransitStop(name, streetVertex, rctx);
            temporaryTransitStopsForLocation.put(streetVertex, stop);
            return stop;
        }
        return temporaryTransitStopsForLocation.get(streetVertex);
    }

    // Return a reasonable name for a vertex.
    private String findName(State state, StreetVertex vertex, Locale locale, boolean forwards) {
        I18NString unnamed = new LocalizedString("unnamedStreet", (String[]) null);
        I18NString name = vertex.getIntersectionName(locale);
        if (!name.equals(unnamed)) {
            return name.toString();
        }
        // search for street edges but don't look too far away
        Queue<Vertex> queue = new LinkedList<>();
        queue.add(vertex);
        int n = 0;
        while (!queue.isEmpty() && n < 3) {
            Vertex v = queue.poll();
            for (Edge e : (forwards ? v.getOutgoing() : v.getIncoming())) {
                if (e instanceof StreetEdge) {
                    return e.getName(locale);
                } else {
                    queue.add(forwards ? e.getToVertex() : e.getFromVertex());
                }
            }
            n++;
        }
        if (state != null && state.backEdge instanceof StreetEdge) { // this really assumes flag stops
            return state.backEdge.getName(locale);
        }
        return unnamed.toString();
    }

    private Collection<Pair<State, PatternHop>> getClosestPatternHops(RoutingRequest rr) {
        Map<PatternHop, State> patternHopStateMap = Maps.newHashMap();
        GenericDijkstra gd = new GenericDijkstra(rr);
        gd.setHeuristic(new TrivialRemainingWeightHeuristic());
        gd.traverseVisitor = new TraverseVisitor() {
            @Override
            public void visitEdge(Edge edge, State state) {
                addStateToPatternHopStateMap(edge, state, patternHopStateMap);
            }

            @Override
            public void visitVertex(State state) {
                vertexVisitor(state);
            }

            @Override
            public void visitEnqueue(State state) {
            }
        };
        gd.setSearchTerminationStrategy(getSearchTerminationStrategy());


        Vertex initVertex = rr.arriveBy ? rr.rctx.toVertex : rr.rctx.fromVertex;
        gd.getShortestPathTree(new State(initVertex, rr));

        return patternHopStateMap.entrySet()
                .stream()
                .map(e -> new Pair<>(e.getValue(), e.getKey()))
                .collect(Collectors.toList());
    }

    private TemporaryTransitStop createTemporaryTransitStop(String name, StreetVertex v, RoutingContext rctx) {
        Stop flagStop = new Stop();
        flagStop.setId(new AgencyAndId("1", "temp_" + String.valueOf(Math.random())));
        flagStop.setLat(v.getLat());
        flagStop.setLon(v.getLon());
        flagStop.setName(name);
        flagStop.setLocationType(99);
        TemporaryTransitStop flagTransitStop = new TemporaryTransitStop(graph, flagStop, v);
        rctx.temporaryVertices.add(flagTransitStop);
        return flagTransitStop;
    }

    private void createHopsToTemporaryStop(RoutingRequest rr, State state, TemporaryTransitStop flagTransitStop, PatternHop originalPatternHop) {
        Stop flagStop = flagTransitStop.getStop();

        TransitStopArrive transitStopArrive = createTransitStopArrive(rr, flagTransitStop);

        Collection<TemporaryPartialPatternHop> reverseHops = findTemporaryPatternHops(rr, originalPatternHop);
        for (TemporaryPartialPatternHop reverseHop : reverseHops) {
            // create new shortened hop
            TemporaryPatternArriveVertex patternArriveVertex = createPatternArriveVertex(rr, originalPatternHop, flagStop);

            TemporaryPartialPatternHop newHop = shortenEnd(rr, state, reverseHop, patternArriveVertex, flagStop);
            if (newHop == null || newHop.isTrivial(rr)) {
                if (newHop != null)
                    newHop.dispose();
                continue;
            }
            rr.rctx.temporaryEdges.add(newHop);

            createAlightEdge(rr, transitStopArrive,  patternArriveVertex, newHop);
        }

        TemporaryPatternArriveVertex patternArriveVertex = createPatternArriveVertex(rr, originalPatternHop, flagStop);

        TemporaryPartialPatternHop hop = makeHopNewTo(rr, state, originalPatternHop, patternArriveVertex, flagStop);
        if (hop == null || hop.isTrivial(rr)) {
            if (hop != null) {
                hop.dispose();
            }
            return;
        }
        rr.rctx.temporaryEdges.add(hop);

        // todo - david's code has this comment. why don't I need it?
        //  flex point far away or is very close to the beginning or end of the hop.  Leave this hop unchanged;

        createAlightEdge(rr, transitStopArrive, patternArriveVertex, hop);
    }

    private void createHopsFromTemporaryStop(RoutingRequest rr, State state, TemporaryTransitStop flagTransitStop, PatternHop originalPatternHop) {
        Stop flagStop = flagTransitStop.getStop();

        TransitStopDepart transitStopDepart = createTransitStopDepart(rr, flagTransitStop);

        TemporaryPatternDepartVertex patternDepartVertex = createPatternDepartVertex(rr, originalPatternHop, flagStop);

        TemporaryPartialPatternHop hop = makeHopNewFrom(rr, state, originalPatternHop, patternDepartVertex, flagStop);
        if (hop == null || hop.isTrivial(rr)) {
            if (hop != null) {
                hop.dispose();
            }
            return;
        }
        rr.rctx.temporaryEdges.add(hop);

        createBoardEdge(rr, transitStopDepart, patternDepartVertex, hop);
    }

    public void createDirectHop(RoutingRequest rr, PatternHop originalPatternHop, TransitStop fromStop, TransitStop toStop, GraphPath path) {
        if (fromStop instanceof TemporaryTransitStop && fromStop.departVertex == null) {
            createTransitStopDepart(rr, (TemporaryTransitStop) fromStop);
        }
        if (toStop instanceof TemporaryTransitStop && toStop.arriveVertex == null) {
            createTransitStopArrive(rr, (TemporaryTransitStop) toStop);
        }

        TemporaryPatternDepartVertex patternDepartVertex = createPatternDepartVertex(rr, originalPatternHop, fromStop.getStop());
        TemporaryPatternArriveVertex patternArriveVertex = createPatternArriveVertex(rr, originalPatternHop, toStop.getStop());

        // direct hop
        TemporaryDirectPatternHop newHop = new TemporaryDirectPatternHop(originalPatternHop, patternDepartVertex, patternArriveVertex, fromStop.getStop(), toStop.getStop(),
                path.getGeometry(), path.getDuration());
        rr.rctx.temporaryEdges.add(newHop);

        createBoardEdge(rr, fromStop.departVertex, patternDepartVertex, newHop);
        createAlightEdge(rr, toStop.arriveVertex, patternArriveVertex, newHop);
    }

    private void addStateToPatternHopStateMap(Edge edge, State s, Map<PatternHop, State> patternHopStateMap) {
        Collection<PatternHop> hops = graph.flexIndex.getHopsForEdge(edge);
        for(PatternHop hop : hops){
            if(patternHopStateMap.containsKey(hop)){
                State oldState = patternHopStateMap.get(hop);
                if(oldState.getBackState().getWeight() < s.getBackState().getWeight()) {
                    continue;
                }
            }
            if (checkHopAllowsBoardAlight(s, hop, !s.getOptions().arriveBy)) {
                patternHopStateMap.put(hop, s);
            }
        }
    }

    private TransitStopDepart createTransitStopDepart(RoutingRequest rr, TemporaryTransitStop transitStop) {
        TransitStopDepart transitStopDepart;
        if (transitStop.departVertex == null) {
            TemporaryStreetTransitLink streetTransitLink = new TemporaryStreetTransitLink(transitStop.getStreetVertex(), transitStop, true);
            rr.rctx.temporaryEdges.add(streetTransitLink);

            transitStopDepart = new TemporaryTransitStopDepart(graph, transitStop.getStop(), transitStop);
            rr.rctx.temporaryVertices.add(transitStopDepart);
            TemporaryPreBoardEdge temporaryPreBoardEdge = new TemporaryPreBoardEdge(transitStop, transitStopDepart);
            rr.rctx.temporaryEdges.add(temporaryPreBoardEdge);

            transitStop.departVertex = transitStopDepart;
        } else {
            transitStopDepart = transitStop.departVertex;
        }
        return transitStopDepart;
    }

    private TemporaryPatternDepartVertex createPatternDepartVertex(RoutingRequest rr, PatternHop hop, Stop stop) {
        TemporaryPatternDepartVertex patternDepartVertex =
                new TemporaryPatternDepartVertex(graph, hop.getPattern(), hop.getStopIndex(), stop);
        rr.rctx.temporaryVertices.add(patternDepartVertex);
        return patternDepartVertex;
    }

    private TemporaryTransitBoardAlight createBoardEdge(RoutingRequest rr, TransitStopDepart transitStopDepart, PatternDepartVertex patternDepartVertex,
                                                        TemporaryPartialPatternHop hop) {
        TemporaryTransitBoardAlight transitBoardAlight =
                new TemporaryTransitBoardAlight(transitStopDepart, patternDepartVertex, hop.getStopIndex(), hop);
        rr.rctx.temporaryEdges.add(transitBoardAlight);
        return transitBoardAlight;
    }

    private TemporaryTransitBoardAlight createAlightEdge(RoutingRequest rr, TransitStopArrive transitStopArrive, PatternArriveVertex patternArriveVertex, TemporaryPartialPatternHop hop) {
        TemporaryTransitBoardAlight transitBoardAlight =
                new TemporaryTransitBoardAlight(patternArriveVertex, transitStopArrive, hop.getStopIndex() + 1, hop);
        rr.rctx.temporaryEdges.add(transitBoardAlight);
        return transitBoardAlight;
    }

    private TransitStopArrive createTransitStopArrive(RoutingRequest rr, TemporaryTransitStop transitStop) {
        TransitStopArrive transitStopArrive;
        if (transitStop.arriveVertex == null) {
            TemporaryStreetTransitLink streetTransitLink = new TemporaryStreetTransitLink(transitStop, transitStop.getStreetVertex(), true);
            rr.rctx.temporaryEdges.add(streetTransitLink);

            transitStopArrive = new TemporaryTransitStopArrive(graph, transitStop.getStop(), transitStop);
            rr.rctx.temporaryVertices.add(transitStopArrive);
            TemporaryPreAlightEdge preAlightEdge = new TemporaryPreAlightEdge(transitStopArrive, transitStop);
            rr.rctx.temporaryEdges.add(preAlightEdge);

            transitStop.arriveVertex = transitStopArrive;
        } else {
            transitStopArrive = transitStop.arriveVertex;
        }
        return transitStopArrive;
    }

    private TemporaryPatternArriveVertex createPatternArriveVertex(RoutingRequest rr, PatternHop hop, Stop stop) {
        TemporaryPatternArriveVertex patternArriveVertex =
                new TemporaryPatternArriveVertex(graph, hop.getPattern(), hop.getStopIndex() + 1, stop);
        rr.rctx.temporaryVertices.add(patternArriveVertex);
        return patternArriveVertex;
    }

    private Collection<TemporaryPartialPatternHop> findTemporaryPatternHops(RoutingRequest options, PatternHop patternHop) {
        Collection<TemporaryPartialPatternHop> ret = new ArrayList<TemporaryPartialPatternHop>();
        for (TemporaryEdge e : options.rctx.temporaryEdges) {
            if (e instanceof TemporaryPartialPatternHop) {
                TemporaryPartialPatternHop hop = (TemporaryPartialPatternHop) e;
                if (hop.isOriginalHop(patternHop))
                    ret.add(hop);
            }
        }
        return ret;
    }

    private void modifyRequestForCarAccess(RoutingRequest opt) {
        Vertex fromVertex = findCarAccessibleVertex(opt, opt.rctx.fromVertex, false);
        Vertex toVertex = findCarAccessibleVertex(opt, opt.rctx.toVertex, true);
        Collection<TemporaryEdge> temporaryEdges = opt.rctx.temporaryEdges;
        Collection<Vertex> temporaryVertices = opt.rctx.temporaryVertices;
        opt.setRoutingContext(opt.rctx.graph, fromVertex, toVertex);
        opt.rctx.temporaryEdges = temporaryEdges;
        opt.rctx.temporaryVertices = temporaryVertices;
    }

    private Vertex findCarAccessibleVertex(RoutingRequest opt, Vertex vertex, boolean arriveBy) {
        if (vertex instanceof TransitStop && ((TransitStop) vertex).getModes().contains(TraverseMode.BUS)) {
            return vertex;
        }
        return new CarPermissionSearch(opt, arriveBy).findVertexWithPermission(vertex, TraverseMode.CAR);
    }
}
