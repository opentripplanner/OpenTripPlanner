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

package org.opentripplanner.routing.edgetype.factory;

import static org.opentripplanner.common.IterableLibrary.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.NegativeWeightException;
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.BasicTripPattern;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.pqueue.FibHeap;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.SPTVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

public class LocalStopFinder {

    private final static Logger _log = LoggerFactory.getLogger(LocalStopFinder.class);

    private static final int MAX_SUBOPTIMAL_DISTANCE = 10; /* allow a slop of ~10 seconds */

    private static final double LOCAL_STOP_SEARCH_RADIUS = 1000; /* how far to search for nearby stops */

    private HashSet<TripPattern> patterns;

    private Graph graph;

    private StreetVertexIndexServiceImpl indexService;

    private TraverseOptions walkingOptions;

    private TraverseOptions bikingOptions;

    private HashMap<Stop, HashMap<TripPattern, P2<Double>>> neighborhoods;

    public LocalStopFinder(StreetVertexIndexServiceImpl indexService, Graph graph) {
        this.graph = graph;
        this.indexService = indexService;
    }

    public void markLocalStops() {
        _log.debug("Finding local stops");
        patterns = new HashSet<TripPattern>();

        int total = 0;
        for (GraphVertex gv : graph.getVertices()) {
            if (gv.vertex instanceof TransitStop) {
                ((TransitStop) gv.vertex).setLocal(true);
                total ++;
            }
            for (Edge e : gv.getOutgoing()) {
                if (e instanceof PatternBoard) {
                    TripPattern pattern = ((PatternBoard) e).getPattern();
                    patterns.add(pattern);
                }
            }
        }
        
        // For each pattern, check if each stop is local

        neighborhoods = new HashMap<Stop, HashMap<TripPattern, P2<Double>>>();

        walkingOptions = new TraverseOptions();
        bikingOptions = new TraverseOptions(new TraverseModeSet(TraverseMode.BICYCLE));
        bikingOptions.optimizeFor = OptimizeType.SAFE;

        int nonLocal = 0;
        for (TripPattern pattern : patterns) {
            List<Stop> stops = getStops(pattern);
            // a stop is local if, in order to get to all other nearby patterns, it is
            // just as good to transfer at the previous stop.
            // so, each stop in the system needs a neighborhood of patterns.

            HashMap<TripPattern, P2<Double>> previousDistances = null;
            HashMap<TripPattern, P2<Double>> distances = null;
            for (int i = 0; i < stops.size() - 1; ++i) {
                Stop stop = stops.get(i);

                TransitStop transitStop = getVertexForStop(stop);

                previousDistances = distances;
                distances = getNeighborhood(stop);
                HashMap<TripPattern, P2<Double>> nextDistances = null;
                Stop nextStop = stops.get(i + 1);
                nextDistances = getNeighborhood(nextStop);

                if (previousDistances == null) {
                    // first stop is never local
                    if (transitStop.isLocal()) {
                        nonLocal ++;
                    }
                    transitStop.setLocal(false);
                    continue;
                } else {
                    boolean local = true;
                    for (Entry<TripPattern, P2<Double>> entry : distances.entrySet()) {
                        TripPattern key = entry.getKey();
                        if (key == pattern) {
                            continue;
                        }
                        P2<Double> distance = entry.getValue();
                        P2<Double> previousDistance = previousDistances.get(key);
                        P2<Double> nextDistance = nextDistances.get(key);
                        if (previousDistance == null) {
                            if (nextDistance == null
                                    || nextDistance.getFirst() > distance.getFirst()
                                    || nextDistance.getSecond() > distance.getSecond()) {
                                local = false;
                                break;
                            }
                        } else if (distance.getFirst() + MAX_SUBOPTIMAL_DISTANCE < previousDistance
                                .getFirst()
                                && (nextDistance == null || nextDistance.getFirst() > distance
                                        .getFirst())) {
                            local = false;
                            break;
                        } else if (distance.getSecond() + MAX_SUBOPTIMAL_DISTANCE < previousDistance
                                .getSecond()
                                && (nextDistance == null || nextDistance.getSecond() > distance
                                        .getSecond())) {
                            local = false;
                            break;
                        }
                    }
                    if (local == false) {
                        if (transitStop.isLocal()) {
                            nonLocal ++;
                        }
                        transitStop.setLocal(false);
                    }
                }
            }
            // last stop is never local
            Stop stop = stops.get(stops.size() - 1);
            TransitStop transitStop = getVertexForStop(stop);
            if (transitStop.isLocal()) {
                nonLocal ++;
            }
            transitStop.setLocal(false);

        }
        _log.debug("Local stops: " + (total - nonLocal) + " / " + total);
    }

    private HashMap<TripPattern, P2<Double>> getNeighborhood(Stop stop) {
        TransitStop transitStop = getVertexForStop(stop);
        HashMap<TripPattern, P2<Double>> neighborhood = neighborhoods.get(stop);
        if (neighborhood == null) {
            Set<TripPattern> nearbyPatterns = getNearbyPatterns(stop);
            HashMap<TripPattern, Double> walkNeighborhood = getBestDistanceForPatterns(graph, transitStop, walkingOptions, nearbyPatterns);
            HashMap<TripPattern, Double> bikeNeighborhood = getBestDistanceForPatterns(graph, transitStop, bikingOptions, nearbyPatterns);
            neighborhood = new HashMap<TripPattern, P2<Double>>();
            for (TripPattern p : nearbyPatterns) {
                Double walkDistance = walkNeighborhood.get(p);
                if (walkDistance == null) {
                    continue; /* if you can't walk there, there's no point */
                }
                Double bikeDistance = bikeNeighborhood.get(p);
                if (bikeDistance == null) {
                    bikeDistance = Double.MAX_VALUE; /* wrong, but will cause stop to not be marked as local on this pattern's account, which is probably right.*/
                }
                neighborhood.put(p, new P2<Double> (walkDistance, bikeDistance));
            }
            neighborhoods.put(stop, neighborhood);
        }
        return neighborhood;
    }

    /**
     * TODO - Any way this can use the existing search code?  AStar or Dijkstra
     * @param graph
     * @param origin
     * @param options
     * @param nearbyPatterns
     * @return
     */
    private HashMap<TripPattern, Double> getBestDistanceForPatterns(Graph graph, Vertex origin,
            TraverseOptions options, Set<TripPattern> nearbyPatterns) {

        // Iteration Variables
        SPTVertex spt_u, spt_v;
        HashSet<Vertex> closed = new HashSet<Vertex>();
        FibHeap<SPTVertex> queue = new FibHeap<SPTVertex>(graph.getVertices().size());
        BasicShortestPathTree spt = new BasicShortestPathTree();
        State init = new State();
        SPTVertex spt_origin = spt.addVertex(origin, init, 0, options);
        queue.insert(spt_origin, spt_origin.weightSum);

        HashMap<TripPattern, Double> patternCosts = new HashMap<TripPattern, Double>();

        int patternsSeen = 0;

        while (!queue.empty()) { // Until the priority queue is empty:

            spt_u = queue.peek_min(); // get the lowest-weightSum Vertex 'u',

            Vertex fromv = spt_u.mirror;

            queue.extract_min();

            closed.add(fromv);

            if (fromv instanceof TransitStop) {
                Vertex departureVertex = null;
                for (DirectEdge e : filter(graph.getOutgoing(fromv),DirectEdge.class)) {
                    /* to departure vertex */
                    departureVertex = e.getToVertex();
                    break;
                }
                for (Edge e : graph.getOutgoing(departureVertex)) {
                    if (e instanceof PatternBoard) {
                        /* finally, a PatternBoard */
                        TripPattern pattern = ((PatternBoard) e).getPattern();
                        if (nearbyPatterns.contains(pattern)) {
                            Double cost = patternCosts.get(pattern);
                            if (cost == null) {
                                patternCosts.put(pattern, spt_u.weightSum);
                                patternsSeen++;
                                if (patternsSeen == nearbyPatterns.size()) {
                                    return patternCosts;
                                }
                            } else if (cost > spt_u.weightSum) {
                                patternCosts.put(pattern, spt_u.weightSum);
                            }
                        }
                    }
                }
            }
            
            if (fromv.distance(origin) > LOCAL_STOP_SEARCH_RADIUS) {
                /* we have now traveled far from the origin, so we know that anything we find
                 * from here on out is going to be too far
                 */
                return patternCosts;
            }

            Iterable<Edge> outgoing = graph.getOutgoing(fromv);
            State state = spt_u.state;

            for (Edge edge : outgoing) {
         

                TraverseResult wr = edge.traverse(state, options);

                // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                // over.
                if (wr == null) {
                    continue;
                }

                if (wr.weight < 0) {
                    throw new NegativeWeightException(String.valueOf(wr.weight));
                }
                
                EdgeNarrative en = wr.getEdgeNarrative();
                Vertex toVertex = en.getToVertex();

                if (closed.contains(toVertex)) {
                    continue;
                }

                double new_w = spt_u.weightSum + wr.weight;

                spt_v = spt.addVertex(toVertex, wr.state, new_w, options, spt_u.hops + 1);

                if (spt_v != null) {
                    spt_v.setParent(spt_u, edge,en);
                    queue.insert_or_dec_key(spt_v, new_w);
                }
            }
        }

        return patternCosts;
    }

    private TransitStop getVertexForStop(Stop stop) {
        String label = GtfsLibrary.convertIdToString(stop.getId());
        return (TransitStop) graph.getVertex(label);
    }

    private HashSet<TripPattern> getNearbyPatterns(Stop stop) {
        // get all transit stops within about the LOCAL_STOP_SEARCH_RADIUS
        Coordinate c = new Coordinate(stop.getLon(), stop.getLat());
        List<Vertex> localTransitStops = indexService.getLocalTransitStops(c, LOCAL_STOP_SEARCH_RADIUS);

        HashSet<TripPattern> neighborhood = new HashSet<TripPattern>();
        for (Vertex v : localTransitStops) {
            if (v instanceof TransitStop) {
                if (((TransitStop) v).isEntrance()) {
                    // enter to get to actual stop
                    for (DirectEdge e : filter(graph.getOutgoing(v),DirectEdge.class)) {
                        v = e.getToVertex();
                        break;
                    }
                }
                for (DirectEdge e : filter(graph.getOutgoing(v),DirectEdge.class)) {
                    for (Edge e2 : graph.getOutgoing(e.getToVertex())) {
                        if (e2 instanceof PatternBoard) {
                            neighborhood.add(((PatternBoard) e2).getPattern());
                        }
                    }
                }
            }
        }
        return neighborhood;
    }

    private List<Stop> getStops(TripPattern pattern) {
        return ((BasicTripPattern) pattern).stops;
    }

}
