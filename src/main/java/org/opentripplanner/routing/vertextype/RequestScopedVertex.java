package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Vertex;

/**
 * Marker interface for request scoped vertices. These vertices should only be traversable by
 * the current routing request, and should be removed after the request has finished.
 * <p>
 * Remember to use the {@link #dispose(Vertex)} to delete the temporary vertex
 * from the main graph after use.
 * </p>
 */
public interface RequestScopedVertex {
    boolean isEndVertex();

    /**
     * This method traverse the subgraph of request scoped vertices, and cuts that subgraph off from
     * the main graph at each point it encounters a non-request scoped vertex. OTP then holds no
     * references to the request scoped subgraph and it is garbage collected.
     * <p>
     * Note! If the {@code vertex} is NOT a RequestScopedVertex the method returns. No action taken.
     * </p>
     *
     * @param vertex Vertex part of the request scoped part of the graph.
     */
    static void dispose(Vertex vertex) {
        TemporaryVertexDispose.dispose(vertex);
    }
}
