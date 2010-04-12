package org.opentripplanner.routing.spt;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;

public interface ShortestPathTree {

    /**
     * Add a vertex to the shortest path tree. If the vertex is already in the tree, this adds
     * another way to get to that vertex, if that new way is not strictly dominated in both time and
     * weight by an existing way.
     *
     * @param vertex
     *            The graph vertex
     * @param state
     *            The state on arrival
     * @param weightSum
     *            The cost to get here
     * @param options
     *            The traversal options
     * @return
     */
    public abstract SPTVertex addVertex(Vertex vertex, State state, double weightSum,
            TraverseOptions options);

    public abstract GraphPath getPath(Vertex dest);

    public abstract GraphPath getPath(Vertex dest, boolean optimize);

    public abstract void removeVertex(SPTVertex vertex);

}