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

import com.beust.jcommander.internal.Maps;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.edgetype.flex.TemporaryPartialPatternHop;
import org.opentripplanner.routing.edgetype.flex.TemporaryPreAlightEdge;
import org.opentripplanner.routing.edgetype.flex.TemporaryPreBoardEdge;
import org.opentripplanner.routing.edgetype.flex.TemporaryStreetTransitLink;
import org.opentripplanner.routing.edgetype.flex.TemporaryTransitBoardAlight;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.flex.TemporaryPatternArriveVertex;
import org.opentripplanner.routing.vertextype.flex.TemporaryPatternDepartVertex;
import org.opentripplanner.routing.vertextype.flex.TemporaryTransitStop;
import org.opentripplanner.routing.vertextype.flex.TemporaryTransitStopArrive;
import org.opentripplanner.routing.vertextype.flex.TemporaryTransitStopDepart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Add temporary vertices/edges for flag stops.
 */
public class FlagStopCreationService {

    private static final Logger LOG = LoggerFactory.getLogger(FlagStopCreationService.class);

    private Graph graph;

    public FlagStopCreationService(Graph graph) {
        this.graph = graph;
    }

    public void initialize(RoutingRequest request) {
        RoutingRequest forward = request.clone();
        forward.setMode(TraverseMode.WALK);
        streetSearch(forward);

        RoutingRequest backward = request.clone();
        backward.setMode(TraverseMode.WALK);
        backward.setArriveBy(!request.arriveBy);
        streetSearch(backward);
    }

    private void streetSearch(RoutingRequest rr) {
        Map<TripPattern, State> tripPatternStateMap = Maps.newHashMap();
        GenericDijkstra gd = new GenericDijkstra(rr);
        gd.setHeuristic(new TrivialRemainingWeightHeuristic());
        gd.traverseVisitor = new TraverseVisitor() {
            @Override
            public void visitEdge(Edge edge, State state) {
                addStateToTripPatternStateMap(edge, state, tripPatternStateMap);
            }

            @Override
            public void visitVertex(State state) {

            }

            @Override
            public void visitEnqueue(State state) {

            }
        };
        Vertex initVertex = rr.arriveBy ? rr.rctx.toVertex : rr.rctx.fromVertex;
        gd.getShortestPathTree(new State(initVertex, rr));
        findFlagStopEdgesNearby(rr, initVertex, tripPatternStateMap);

        Map<State, List<TripPattern>> stateToTripPatternsMap = new HashMap<>();
        for(TripPattern tripPattern : tripPatternStateMap.keySet()){
            State s = tripPatternStateMap.get(tripPattern);
            if(!stateToTripPatternsMap.containsKey(s)){
                stateToTripPatternsMap.put(s, new ArrayList<>());
            }
            stateToTripPatternsMap.get(s).add(tripPattern);
        }

        int i = 0;
        for(State s : stateToTripPatternsMap.keySet()){

            Vertex v;

            if(s.getVertex() == initVertex){
                //the origin/destination lies along a flag stop route
                LOG.debug("the origin/destination lies along a flag stop route.");
                v = initVertex;
            }else{
                v = rr.arriveBy ? s.getBackEdge().getToVertex() : s.getBackEdge().getFromVertex();
            }

            //if nearby, wire stop to init vertex

            Stop flagStop = new Stop();
            flagStop.setId(new AgencyAndId("1", "temp_" + String.valueOf(Math.random())));
            flagStop.setLat(v.getLat());
            flagStop.setLon(v.getLon());
            flagStop.setName(s.getBackEdge().getName());
            flagStop.setLocationType(99);
            TemporaryTransitStop flagTransitStop = new TemporaryTransitStop(graph, flagStop);
            rr.rctx.temporaryVertices.add(flagTransitStop);

            if(rr.arriveBy) {
                //reverse search
                TemporaryStreetTransitLink streetTransitLink = new TemporaryStreetTransitLink(flagTransitStop, (StreetVertex)v, true);
                rr.rctx.temporaryEdges.add(streetTransitLink);

                TemporaryTransitStopArrive transitStopArrive = new TemporaryTransitStopArrive(graph, flagStop, flagTransitStop);
                rr.rctx.temporaryVertices.add(transitStopArrive);
                TemporaryPreAlightEdge preAlightEdge = new TemporaryPreAlightEdge(transitStopArrive, flagTransitStop);
                rr.rctx.temporaryEdges.add(preAlightEdge);

                for(TripPattern originalTripPattern : stateToTripPatternsMap.get(s)) {

                    List<PatternHop> patternHops = graph.index.getHopsForEdge(s.getBackEdge())
                            .stream()
                            .filter(e -> e.getPattern() == originalTripPattern)
                            .filter(PatternHop::hasFlagStopService)
                            .collect(Collectors.toList());

                    for(PatternHop originalPatternHop : patternHops) {

                        int stopIndex = originalPatternHop.getStopIndex() + 1;

                        TemporaryPatternArriveVertex patternArriveVertex =
                                new TemporaryPatternArriveVertex(graph, originalTripPattern, stopIndex, flagStop);
                        rr.rctx.temporaryVertices.add(patternArriveVertex);

                        Collection<TemporaryPartialPatternHop> reverseHops = findTemporaryPatternHops(rr, originalPatternHop);
                        for (TemporaryPartialPatternHop reverseHop : reverseHops) {
                            // create new shortened hop
                            TemporaryPartialPatternHop newHop = reverseHop.shortenEnd(rr, patternArriveVertex, flagStop);
                            if (newHop == null || newHop.isTrivial()) {
                                if (newHop != null)
                                    newHop.dispose();
                                continue;
                            }
                            rr.rctx.temporaryEdges.add(newHop);
                        }

                        TemporaryPartialPatternHop hop = TemporaryPartialPatternHop.startHop(rr, originalPatternHop, patternArriveVertex, flagStop);
                        if (hop.isTrivial()) {
                            hop.dispose();
                            continue;
                        }
                        rr.rctx.temporaryEdges.add(hop);

                        // todo - david's code has this comment. why don't I need it?
                        //  flex point far away or is very close to the beginning or end of the hop.  Leave this hop unchanged;

                        /** Alighting constructor (PatternStopVertex --> TransitStopArrive) */
                        TemporaryTransitBoardAlight transitBoardAlight =
                                new TemporaryTransitBoardAlight(patternArriveVertex, transitStopArrive, stopIndex, hop);
                        rr.rctx.temporaryEdges.add(transitBoardAlight);
                    }
                }

            }else{
                //forward search

                TemporaryStreetTransitLink streetTransitLink = new TemporaryStreetTransitLink((StreetVertex)v, flagTransitStop, true);
                rr.rctx.temporaryEdges.add(streetTransitLink);

                TemporaryTransitStopDepart transitStopDepart = new TemporaryTransitStopDepart(graph, flagStop, flagTransitStop);
                rr.rctx.temporaryVertices.add(transitStopDepart);
                TemporaryPreBoardEdge temporaryPreBoardEdge = new TemporaryPreBoardEdge(flagTransitStop, transitStopDepart);
                rr.rctx.temporaryEdges.add(temporaryPreBoardEdge);

                for(TripPattern originalTripPattern : stateToTripPatternsMap.get(s)) {

                    List<PatternHop> patternHops = graph.index.getHopsForEdge(s.getBackEdge())
                            .stream()
                            .filter(e -> e.getPattern() == originalTripPattern)
                            .filter(PatternHop::hasFlagStopService)
                            .collect(Collectors.toList());

                    for(PatternHop originalPatternHop : patternHops) {

                        TemporaryPatternDepartVertex patternDepartVertex =
                                new TemporaryPatternDepartVertex(graph, originalTripPattern, originalPatternHop.getStopIndex(), flagStop);
                        rr.rctx.temporaryVertices.add(patternDepartVertex);

                        TemporaryPartialPatternHop hop = TemporaryPartialPatternHop.endHop(rr, originalPatternHop, patternDepartVertex, flagStop);
                        if (hop.isTrivial()) {
                            hop.dispose();
                            continue;
                        }
                        rr.rctx.temporaryEdges.add(hop);

                        /** TransitBoardAlight: Boarding constructor (TransitStopDepart, PatternStopVertex) */
                        TemporaryTransitBoardAlight transitBoardAlight =
                                new TemporaryTransitBoardAlight(transitStopDepart, patternDepartVertex, originalPatternHop.getStopIndex(), hop);
                        rr.rctx.temporaryEdges.add(transitBoardAlight);
                    }
                }
            }
            i++;
        }
    }

    private void addStateToTripPatternStateMap(Edge edge, State s, Map<TripPattern, State> tripPatternStateMap) {
        Collection<TripPattern> patterns = graph.index.getPatternsForEdge(edge);
        for(TripPattern tripPattern : patterns){
            if(tripPatternStateMap.containsKey(tripPattern)){
                State oldState = tripPatternStateMap.get(tripPattern);
                if(oldState.getWeight() < s.getWeight()){
                    continue;
                }
            }
            tripPatternStateMap.put(tripPattern, s);
        }
    }

    /**
     * Find the street edges that were split at beginning of search by StreetSplitter, check whether they are served by flag stop routes.
     * This is duplicating the work done earlier by StreetSplitter, but want to minimize the number of changes introduced by flag stops.
     * @param
     * @return
     */
    private void findFlagStopEdgesNearby(RoutingRequest rr, Vertex initVertex, Map<TripPattern, State> tripPatternStateMap) {
        List<StreetEdge> flagStopEdges = getClosestStreetEdges(initVertex.getCoordinate());

       for(StreetEdge streetEdge : flagStopEdges){
           State nearbyState = new State(initVertex, rr);
           nearbyState.backEdge = streetEdge;
           Collection<TripPattern> patterns = graph.index.getPatternsForEdge(streetEdge);
           //todo find trippatterns for flag stop routes only
           for(TripPattern tripPattern : patterns){
               tripPatternStateMap.put(tripPattern, nearbyState);
           }
       }
    }

    // It's possible that the edge is a very long StreetEdge whose traversal will kill our walk budget, but it's
    // a good candidate for flex edges.
    private void checkFlexAtLongStreetEdge(State s0, Edge e, Map<TripPattern, State> tripPatternStateMap) {
        if (s0.getWalkDistance() + e.getDistance() >= s0.getOptions().getMaxWalkDistance()) {
            State s1 = s0.edit(e).makeState();
            addStateToTripPatternStateMap(e, s1, tripPatternStateMap);
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

    /**
     * Find the nearest street edges to the given point, check if they are served by flag stop routes.
     */
    private List<StreetEdge> getClosestStreetEdges(Coordinate pointLocation) {

        final double radiusDeg = SphericalDistanceLibrary.metersToDegrees(500);

        Envelope env = new Envelope(pointLocation);

        // local equirectangular projection
        double lat = pointLocation.getOrdinate(1);
        final double xscale = Math.cos(lat * Math.PI / 180);

        env.expandBy(radiusDeg / xscale, radiusDeg);

        Collection<Edge> edges = graph.streetIndex.getEdgesForEnvelope(env);
        if (edges.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Double, List<StreetEdge>> edgeDistanceMap = new TreeMap<>();
        for(Edge edge : edges){
            if(edge instanceof StreetEdge){
                LineString line = edge.getGeometry();
                double dist = SphericalDistanceLibrary.fastDistance(pointLocation, line);
                double roundOff = (double) Math.round(dist * 100) / 100;
                if(!edgeDistanceMap.containsKey(roundOff))
                    edgeDistanceMap.put(roundOff, new ArrayList<>());
                edgeDistanceMap.get(roundOff).add((StreetEdge) edge);
            }
        }

        List<StreetEdge> closestEdges = edgeDistanceMap.values().iterator().next();
        List<StreetEdge> ret = new ArrayList<>();
        for(StreetEdge closestEdge : closestEdges){
            List<PatternHop> patternHops = graph.index.getHopsForEdge(closestEdge)
                    .stream()
                    //.filter(e -> e.getPattern() == originalTripPattern)
                    //todo: check if these are flag stop hops
                    .collect(Collectors.toList());
            if(patternHops.size() > 0){
                ret.add(closestEdge);
            }
        }
        return ret;
    }
}
