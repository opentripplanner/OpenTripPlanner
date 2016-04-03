package org.opentripplanner.profile;

import com.beust.jcommander.internal.Maps;

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
    final int maxWalkMeters;
    // Flattened 2D array of (streetVertexIndex, distanceFromStop) for each TransitStop
    public final Map<TransitStop, int[]> distancesForStop = Maps.newHashMap();

    public StopTreeCache (Graph graph, int maxWalkMeters) {
        this.maxWalkMeters = maxWalkMeters;
        LOG.info("Caching distances to nearby street intersections from each transit stop...");
        graph.index.stopVertexForStop.values().parallelStream().forEach(tstop -> {
            RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
            rr.batch = (true);
            rr.setRoutingContext(graph, tstop, tstop);
            AStar astar = new AStar();
            rr.longDistance = true;
            rr.setNumItineraries(1);

            // since we're storing distances and later using them to optimize
            // (in the profile propagation code we optimize on distance / walkSpeed
            //  not the actual time including turn costs etc.),
            // we need to optimize on distance here as well.
            rr.maxWalkDistance = maxWalkMeters;
            rr.softWalkLimiting = false;
            rr.dominanceFunction = new DominanceFunction.LeastWalk();

            ShortestPathTree spt = astar.getShortestPathTree(rr, 5); // timeout in seconds
            // Copy vertex indices and distances into a flattened 2D array
            int[] distances = new int[spt.getVertexCount() * 2];
            int i = 0;
            for (Vertex vertex : spt.getVertices()) {
                State state = spt.getState(vertex);
                
                if (state == null)
                    continue;
                
                distances[i++] = vertex.getIndex();
                distances[i++] = (int) state.getWalkDistance();
            }

            rr.cleanup();

            synchronized (distancesForStop) {
                distancesForStop.put(tstop, distances);
            }
        });
        LOG.info("Done caching distances to nearby street intersections from each transit stop.");
    }

    /**
     * Given a travel time to a transit stop, fill in the array with minimum travel times to all nearby street vertices.
     * This function is meant to be called repeatedly on multiple transit stops, accumulating minima
     * into the same targetArray.
     */
    public void propagateStop(TransitStop transitStop, int baseTimeSeconds, double walkSpeed, int[] targetArray) {
        // Iterate over street intersections in the vicinity of this particular transit stop.
        // Shift the time range at this transit stop, merging it into that for all reachable street intersections.
        int[] distances = distancesForStop.get(transitStop);
        int v = 0;
        while (v < distances.length) {
            // Unravel flattened 2D array
            int vertexIndex = distances[v++];
            int distance = distances[v++];
            // distance in meters over walkspeed in meters per second --> seconds
            int egressWalkTimeSeconds = (int) (distance / walkSpeed);
            int propagated_time = baseTimeSeconds + egressWalkTimeSeconds;
            int existing_min = targetArray[vertexIndex];
            if (existing_min == 0 || existing_min > propagated_time) {
                targetArray[vertexIndex] = propagated_time;
            }
        }

    }

}
