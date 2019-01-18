package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is a utility class used to remove a temporary subgraph from the manin graph.
 * It traverse the subgraph of temporary vertices, and cuts that subgraph off from the
 * main graph at each point it encounters a non-temporary vertexes.
 * <p/>
 * OTP then holds no references to the temporary subgraph and it is garbage collected.
 * <p/>
 * The static {@link #dispose(Vertex)} utility method is the only way to access the logic,
 * hence preventing this class from reuse. This make the class thread safe, and simplify
 * the implementation.
 */
class TemporaryVertexDispose {
    /**
     * A list of all Vertexes not jet processed.
     */
    private List<Vertex> todo = new ArrayList<>();

    /**
     * Processed vertexes. To prevent looping and processing the same vertex twice we
     * keep all processed vertexes in the 'done' set.
     */
    private Set<Vertex> done = new HashSet<>();

    /** Intentionally private constructor to prevent instantiation outside of the class. */
    private TemporaryVertexDispose(Vertex tempVertex) {
        todo.add(tempVertex);
    }

    /**
     * Create an instance and dispose temporary subgraph.
     * @param tempVertex any temporary vertex part of the temporary subgrap.
     */
    static void dispose(Vertex tempVertex) {
        if(tempVertex instanceof TemporaryVertex) {
            new TemporaryVertexDispose(tempVertex).dispose();
        }
    }


    /* private methods */

    private void dispose() {
        // Add all connected vertexes to the TODO_list and disconnect all
        // main graph vertexes. We use a loop and not recursion to avoid
        // stack overflow in the case of deep temporary graphs.
        while (!todo.isEmpty()) {
            Vertex current = next();
            if(isNotAlreadyProcessed(current)) {
                for (Edge edge : current.getOutgoing()) {
                    disposeVertex(edge.getToVertex(), edge, true);
                }
                for (Edge edge : current.getIncoming()) {
                    disposeVertex(edge.getFromVertex(), edge, false);
                }
                done.add(current);
            }
        }
    }

    /**
     * Add the temporary vertex to processing queue OR disconnect edge from vertex if
     * vertex is part of the main graph.
     *
     * @param v the vertex to dispose
     * @param connectedEdge the connected temporary edge
     * @param incoming true if the edge is an incoming edge, false if it is an outgoing edge
     */
    private void disposeVertex(Vertex v, Edge connectedEdge, boolean incoming) {
        if(v instanceof TemporaryVertex) {
            addVertexToProcessTodoList(v);
        }
        else {
            removeEdgeFromMainGraphVertex(v, connectedEdge, incoming);
        }
    }

    /**
     * We have reached a NONE temporary Vertex and need to remove the temporary `connectedEdge`
     * from the Vertex part of the main graph.
     *
     * @param v the vertex part of the main graph
     * @param connectedEdge the connected temporary edge to be removed
     * @param incoming true if the edge is an incoming edge, false if it is an outgoing edge
     */
    private void removeEdgeFromMainGraphVertex(Vertex v, Edge connectedEdge, boolean incoming) {
        if(incoming) {
            v.removeIncoming(connectedEdge);
        }
        else {
            v.removeOutgoing(connectedEdge);
        }
    }

    private void addVertexToProcessTodoList(Vertex v) {
        if(isNotAlreadyProcessed(v)) {
            todo.add(v);
        }
    }

    private boolean isNotAlreadyProcessed(Vertex v) {
        return !done.contains(v);
    }

    private Vertex next() {
        return todo.remove(todo.size()-1);
    }
}
