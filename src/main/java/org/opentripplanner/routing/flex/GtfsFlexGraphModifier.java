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
package org.opentripplanner.routing.flex;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.apache.commons.math3.util.Pair;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.edgetype.flex.TemporaryPartialPatternHop;
import org.opentripplanner.routing.edgetype.flex.TemporaryPreAlightEdge;
import org.opentripplanner.routing.edgetype.flex.TemporaryPreBoardEdge;
import org.opentripplanner.routing.edgetype.flex.TemporaryStreetTransitLink;
import org.opentripplanner.routing.edgetype.flex.TemporaryTransitBoardAlight;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
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
import java.util.Locale;
import java.util.Map;
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

    /**
     * Subclasses can override this method to provide extra hops (not found in graph search)
     * at which to create new vertices/edges, e.g. for flag stops where start/end vertices can be
     * ON hop.
     * 
     * @param rr request for graph search
     * @param patternHopStateMap map to insert new PatternHop/State pairs into
     */
    public void findExtraHops(RoutingRequest rr, Map<PatternHop, State> patternHopStateMap) {
    }

    /**
     * Create temporary edges and vertices from the origin into the transit network.
     *
     * @param request request for graph search
     */
    public void createForwardHops(RoutingRequest request) {
        RoutingRequest forward = request.clone();
        forward.setMode(getMode());
        streetSearch(forward);
    }

    /**
     * Create temporary edges and vertices from the transit network to the destination.
     *
     * @param request request for graph search
     */
    public void createBackwardHops(RoutingRequest request) {
        RoutingRequest backward = request.clone();
        backward.setMode(getMode());
        backward.setArriveBy(!request.arriveBy);
        streetSearch(backward);
    }

    private void streetSearch(RoutingRequest rr) {
        for(Pair<State, PatternHop> p : getClosestPatternHops(rr)) {
            State s = p.getKey();
            PatternHop hop = p.getValue();
            TemporaryTransitStop flagTransitStop = getTemporaryStop(s, hop);
            if (rr.arriveBy) {
                createHopsToTemporaryStop(rr, s, flagTransitStop, hop);
            } else {
                createHopsFromTemporaryStop(rr, s, flagTransitStop, hop);
            }
        }
    }

    private TemporaryTransitStop getTemporaryStop(State s, PatternHop hop) {
        StreetVertex streetVertex = getLocationForTemporaryStop(s, hop);
        if (temporaryTransitStopsForLocation.get(streetVertex) == null) {
            String name = findName(s, streetVertex, s.getOptions().locale);
            TemporaryTransitStop stop = createTemporaryTransitStop(name, streetVertex, s.getContext());
            temporaryTransitStopsForLocation.put(streetVertex, stop);
            return stop;
        }
        return temporaryTransitStopsForLocation.get(streetVertex);
    }

    // Return a reasonable name for a vertex.
    private String findName(State state, StreetVertex vertex, Locale locale) {
        I18NString unnamed = new LocalizedString("unnamedStreet", (String[]) null);
        I18NString name = vertex.getIntersectionName(locale);
        if (!name.equals(unnamed)) {
            return name.toString();
        }
        for (Edge e : Iterables.concat(vertex.getIncoming(), vertex.getOutgoing())) {
            if (e instanceof StreetEdge) {
                return e.getName(locale);
            }
        }
        if (state.backEdge instanceof StreetEdge) { // this really assumes flag stops
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
            }

            @Override
            public void visitEnqueue(State state) {
            }
        };
        gd.setSearchTerminationStrategy(getSearchTerminationStrategy());

        Vertex initVertex = rr.arriveBy ? rr.rctx.toVertex : rr.rctx.fromVertex;
        gd.getShortestPathTree(new State(initVertex, rr));
        findExtraHops(rr, patternHopStateMap);

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

        TransitStopArrive transitStopArrive;
        if (flagTransitStop.arriveVertex == null) {
            TemporaryStreetTransitLink streetTransitLink = new TemporaryStreetTransitLink(flagTransitStop, flagTransitStop.getStreetVertex(), true);
            rr.rctx.temporaryEdges.add(streetTransitLink);

            transitStopArrive = new TemporaryTransitStopArrive(graph, flagStop, flagTransitStop);
            rr.rctx.temporaryVertices.add(transitStopArrive);
            TemporaryPreAlightEdge preAlightEdge = new TemporaryPreAlightEdge(transitStopArrive, flagTransitStop);
            rr.rctx.temporaryEdges.add(preAlightEdge);

            flagTransitStop.arriveVertex = transitStopArrive;
        } else {
            transitStopArrive = flagTransitStop.arriveVertex;
        }

        int stopIndex = originalPatternHop.getStopIndex() + 1;

        Collection<TemporaryPartialPatternHop> reverseHops = findTemporaryPatternHops(rr, originalPatternHop);
        for (TemporaryPartialPatternHop reverseHop : reverseHops) {
            // create new shortened hop
            TemporaryPatternArriveVertex patternArriveVertex =
                    new TemporaryPatternArriveVertex(graph, originalPatternHop.getPattern(), stopIndex, flagStop);
            rr.rctx.temporaryVertices.add(patternArriveVertex);

            TemporaryPartialPatternHop newHop = shortenEnd(rr, state, reverseHop, patternArriveVertex, flagStop);
            if (newHop == null || newHop.isTrivial()) {
                if (newHop != null)
                    newHop.dispose();
                continue;
            }
            rr.rctx.temporaryEdges.add(newHop);

            TemporaryTransitBoardAlight transitBoardAlight =
                    new TemporaryTransitBoardAlight(patternArriveVertex, transitStopArrive, stopIndex, newHop);
            rr.rctx.temporaryEdges.add(transitBoardAlight);
        }

        TemporaryPatternArriveVertex patternArriveVertex =
                new TemporaryPatternArriveVertex(graph, originalPatternHop.getPattern(), stopIndex, flagStop);
        rr.rctx.temporaryVertices.add(patternArriveVertex);

        TemporaryPartialPatternHop hop = makeHopNewTo(rr, state, originalPatternHop, patternArriveVertex, flagStop);
        if (hop.isTrivial()) {
            hop.dispose();
            return;
        }
        rr.rctx.temporaryEdges.add(hop);

        // todo - david's code has this comment. why don't I need it?
        //  flex point far away or is very close to the beginning or end of the hop.  Leave this hop unchanged;

        /** Alighting constructor (PatternStopVertex --> TransitStopArrive) */
        TemporaryTransitBoardAlight transitBoardAlight =
                new TemporaryTransitBoardAlight(patternArriveVertex, transitStopArrive, stopIndex, hop);
        rr.rctx.temporaryEdges.add(transitBoardAlight);

    }

    private void createHopsFromTemporaryStop(RoutingRequest rr, State state, TemporaryTransitStop flagTransitStop, PatternHop originalPatternHop) {
        Stop flagStop = flagTransitStop.getStop();

        TransitStopDepart transitStopDepart;
        if (flagTransitStop.departVertex == null) {
            TemporaryStreetTransitLink streetTransitLink = new TemporaryStreetTransitLink(flagTransitStop.getStreetVertex(), flagTransitStop, true);
            rr.rctx.temporaryEdges.add(streetTransitLink);

            transitStopDepart = new TemporaryTransitStopDepart(graph, flagStop, flagTransitStop);
            rr.rctx.temporaryVertices.add(transitStopDepart);
            TemporaryPreBoardEdge temporaryPreBoardEdge = new TemporaryPreBoardEdge(flagTransitStop, transitStopDepart);
            rr.rctx.temporaryEdges.add(temporaryPreBoardEdge);

            flagTransitStop.departVertex = transitStopDepart;
        } else {
            transitStopDepart = flagTransitStop.departVertex;
        }

        TemporaryPatternDepartVertex patternDepartVertex =
                new TemporaryPatternDepartVertex(graph, originalPatternHop.getPattern(), originalPatternHop.getStopIndex(), flagStop);
        rr.rctx.temporaryVertices.add(patternDepartVertex);

        TemporaryPartialPatternHop hop = makeHopNewFrom(rr, state, originalPatternHop, patternDepartVertex, flagStop);
        if (hop.isTrivial()) {
            hop.dispose();
            return;
        }
        rr.rctx.temporaryEdges.add(hop);

        /** TransitBoardAlight: Boarding constructor (TransitStopDepart, PatternStopVertex) */
        TemporaryTransitBoardAlight transitBoardAlight =
                new TemporaryTransitBoardAlight(transitStopDepart, patternDepartVertex, originalPatternHop.getStopIndex(), hop);
        rr.rctx.temporaryEdges.add(transitBoardAlight);
    }

    private void addStateToPatternHopStateMap(Edge edge, State s, Map<PatternHop, State> patternHopStateMap) {
        Collection<PatternHop> hops = graph.index.getHopsForEdge(edge);
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
}
