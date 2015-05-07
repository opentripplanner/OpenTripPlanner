package org.opentripplanner.profile;

import com.beust.jcommander.internal.Maps;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Keeps travel distances from all transit stops in a particular Graph to their nearby street nodes.
 * This allows us to propagate travel times out from transit to streets much faster in one-to-many analyst queries.
 * The StopTreeCache has a fixed distance cutoff, so will be unable to provide distance information for vertices beyond
 * that cutoff distance.
 */
public class StopTreeCache {

    private static final Logger LOG = LoggerFactory.getLogger(StopTreeCache.class);
    final int timeCutoffMinutes;
    private final Map<TransitStop, TObjectIntMap<Vertex>> distancesForStop = Maps.newHashMap();

    public StopTreeCache (Graph graph, int timeCutoffMinutes) {
        this.timeCutoffMinutes = timeCutoffMinutes;
        LOG.info("Caching distances to nearby street intersections from each transit stop...");
        for (TransitStop tstop : graph.index.stopVertexForStop.values()) {
            RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
            rr.batch = (true);
            rr.setRoutingContext(graph, tstop, tstop);
            // RoutingReqeust dateTime defaults to currentTime.
            // If elapsed time is not capped, searches are very slow.
            rr.worstTime = (rr.dateTime + timeCutoffMinutes * 60);
            AStar astar = new AStar();
            rr.longDistance = true;
            rr.dominanceFunction = new DominanceFunction.EarliestArrival();
            rr.setNumItineraries(1);
            ShortestPathTree spt = astar.getShortestPathTree(rr, 5); // timeout in seconds
            TObjectIntMap<Vertex> distanceToVertex = new TObjectIntHashMap<>(1000, 0.5f, Integer.MAX_VALUE);
            for (State state : spt.getAllStates()) {
                distanceToVertex.put(state.getVertex(), (int)state.walkDistance);
            }
            distancesForStop.put(tstop, distanceToVertex);
            rr.cleanup();
        }
        LOG.info("Done caching distances to nearby street intersections from each transit stop.");
    }

    public TObjectIntMap<Vertex> getDistancesForStop(TransitStop tstop) {
        return distancesForStop.get(tstop);
    }
}
