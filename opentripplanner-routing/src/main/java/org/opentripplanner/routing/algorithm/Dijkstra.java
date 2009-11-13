package org.opentripplanner.routing.algorithm;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.pqueue.FibHeap;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class Dijkstra {

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
        SPTVertex spt_origin = spt.addVertex(origin, init, 0);

        // Priority Queue
        FibHeap pq = new FibHeap(gg.getVertices().size());
        pq.insert(spt_origin, spt_origin.weightSum);

        // Iteration Variables
        SPTVertex spt_u, spt_v;
        while (!pq.empty()) { // Until the priority queue is empty:
            spt_u = (SPTVertex) pq.extract_min(); // get the lowest-weightSum Vertex 'u',

            if (spt_u.mirror == target)
                break;

            for (Edge edge : spt_u.mirror.getOutgoing()) {

                TraverseResult wr = edge.traverse(spt_u.state, options);

                // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                // over.
                if (wr == null) {
                    continue;
                }

                if (wr.weight < 0) {
                    throw new NegativeWeightException(String.valueOf(wr.weight));
                }

                double new_w = spt_u.weightSum + wr.weight;
                double old_w;

                Vertex tov = edge.getToVertex();
                spt_v = spt.getVertex(tov);
                // if this is the first time edge.tov has been visited
                if (spt_v == null) {
                    old_w = Integer.MAX_VALUE;
                    spt_v = spt.addVertex(tov, wr.state, new_w);
                } else {
                    old_w = spt_v.weightSum;
                }

                // If the new way of getting there is better,
                if (new_w < old_w) {
                    // Set the State of v in the SPT to the current winner
                    spt_v.state = wr.state;
                    spt_v.weightSum = new_w;
                    pq.insert_or_dec_key(spt_v, new_w);

                    spt_v.setParent(spt_u, edge);
                }
            }
        }

        return spt;
    }

}
