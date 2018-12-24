package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Marker interface for temporary vertices.
 * <p/>
 * Remember to use the {@link #dispose(Vertex)} to delete the temporary vertex
 * from the main graph after use.
 */
public interface TemporaryVertex {
    boolean isEndVertex();

    /**
     * This method disposes an individual TemporaryVertex and its incident edges so that they can
     * be garbage-collected.
     * <p/>
     * Note! If the {@code vertex} is NOT a TemporaryVertex the method returns. No action taken.
     *
     * @param vertex Vertex to dispose
     */
    static void dispose(Vertex vertex) {
        if (vertex instanceof TemporaryVertex) {
            for (Edge edge : vertex.getOutgoing()) {
                vertex.removeOutgoing(edge);
                if (!(edge.getToVertex() instanceof TemporaryVertex)) {
                    edge.getToVertex().removeIncoming(edge);
                }
            }
            for (Edge edge : vertex.getIncoming()) {
                vertex.removeIncoming(edge);
                if (!(edge.getFromVertex() instanceof TemporaryVertex)) {
                    edge.getFromVertex().removeOutgoing(edge);
                }
            }
        }
    }
}
