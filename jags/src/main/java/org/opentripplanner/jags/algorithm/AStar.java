package org.opentripplanner.jags.algorithm;

import org.opentripplanner.jags.core.Edge;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TraverseOptions;
import org.opentripplanner.jags.core.TraverseResult;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.gtfs.exception.NegativeWeightException;
import org.opentripplanner.jags.pqueue.FibHeap;
import org.opentripplanner.jags.spt.SPTVertex;
import org.opentripplanner.jags.spt.ShortestPathTree;

public class AStar {
    
    static final double MAX_SPEED = 10.0;

    public static ShortestPathTree getShortestPathTree(Graph gg, String from_label,
            String to_label, State init, TraverseOptions options) {
        // Goal Variables
        String origin_label = from_label;
        String target_label = to_label;

        // Get origin vertex to make sure it exists
        Vertex origin = gg.getVertex(origin_label);
        Vertex target = gg.getVertex(target_label);
        if (origin == null) {
            return null;
        }

        // Return Tree
        ShortestPathTree spt = new ShortestPathTree();

        double distance = origin.distance(target) / MAX_SPEED;
        SPTVertex spt_origin = spt.addVertex(origin, init, 0);

        // Priority Queue
        FibHeap pq = new FibHeap(gg.getVertices().size());
        pq.insert(spt_origin, spt_origin.weightSum + distance);

        // Iteration Variables
        SPTVertex spt_u, spt_v;
        int i = 0;
        while (!pq.empty()) { // Until the priority queue is empty:
            spt_u = (SPTVertex) pq.extract_min(); // get the lowest-weightSum Vertex 'u',

            if (spt_u.mirror == target)
                break;

            for (Edge edge : spt_u.mirror.outgoing) {
                i++;

                TraverseResult wr = edge.payload.traverse(spt_u.state, options);

                // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                // over.
                if (wr == null) {
                    continue;
                }

                if (wr.weight < 0) {
                    throw new NegativeWeightException(String.valueOf(wr.weight));
                }

                distance = edge.tov.distance(target) / MAX_SPEED;
                double new_w = spt_u.weightSum + wr.weight;
                double old_w;

                spt_v = spt.getVertex(edge.tov);
                // if this is the first time edge.tov has been visited
                if (spt_v == null) {
                    old_w = Integer.MAX_VALUE;
                    spt_v = spt.addVertex(edge.tov, wr.state, new_w);
                } else {
                    old_w = spt_v.weightSum + distance;
                }

                // If the new way of getting there is better,
                if (new_w + distance < old_w) {
                    // Set the State of v in the SPT to the current winner
                    spt_v.state = wr.state;
                    spt_v.weightSum = new_w;
                    if (old_w == Integer.MAX_VALUE) {
                        pq.insert(spt_v, new_w + distance);
                    } else {
                        pq.insert_or_dec_key(spt_v, new_w + distance);
                    }

                    spt_v.setParent(spt_u, edge.payload);
                }
            }
        }
        System.out.println("examined: " + i);
        return spt;
    }

}
