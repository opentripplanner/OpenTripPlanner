package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Vertex;

import java.util.Collection;

/**
 * Marker interface for temporary vertices.
 * <p/>
 * Remember to use the {@link #dispose(Vertex)} to delete the temporary vertex
 * from the main graph after use.
 */
public interface TemporaryVertex {
    boolean isEndVertex();

    /**
     * This method traverses the subgraph of temporary vertices, and cuts that subgraph off from
     * the main graph at each point it encounters a non-temporary vertexes. OTP then holds no
     * references to the temporary subgraph and it is garbage collected.
     * <p/>
     * Note! If the {@code vertex} is NOT a TemporaryVertex the method returns. No action taken.
     *
     * @param vertex Vertex part of the temporary part of the graph.
     * @return a collection of all the vertices removed from the graph.
     */
    static Collection<Vertex> dispose(Vertex vertex) {
        return TemporaryVertexDispose.dispose(vertex);
    }

    /**
     * This method traverses the subgraph of all temporary vertices connected to the collection,
     * and removes connections to the main graph. OTP then holds no references to any of the
     * temporary subgraphs and they can be garbage-collected.
     * <p/>
     * Note! Any vertices which are not instances of TemporaryVertex are ignored and no action is
     * taken.
     *
     * @param vertices All temporary vertices
     * @return a collection of all the vertices removed from the graph.
     */
    static Collection<Vertex> disposeAll(Collection<Vertex> vertices) {
        return TemporaryVertexDispose.disposeAll(vertices);
    }

    /**
     * Return the subgraph of temporary vertices which are connected to this vertex. Returns an
     * empty collection if the given vertex is not a TemporaryVertex.
     *
     * @param vertex Vertex part of the temporary vertex subgraph.
     * @return a collection of all temporary vertices in the subgraph.
     */
    static Collection<Vertex> findSubgraph(Vertex vertex) {
        return TemporaryVertexDispose.search(vertex);
    }

}
