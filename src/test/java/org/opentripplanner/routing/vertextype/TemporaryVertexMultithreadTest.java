package org.opentripplanner.routing.vertextype;

import org.junit.Test;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TemporaryVertexMultithreadTest {

    private static final double ANY_LOC = 1;

    @Test
    public void testTemporaryVertexOnOtherThreadUnreachable() throws ExecutionException, InterruptedException {
        Graph graph = new Graph();
        Vertex a = new V(graph, "A");
        TempVertex b = new TempVertex(graph, "B");
        Edge edge = new FreeEdge(a, b);
        RoutingRequest options = new RoutingRequest();
        options.setRoutingContext(graph, a, b);
        State init = new State(a, options);
        assertNotNull(edge.traverse(init));
        FutureTask<State> otherThreadState = new FutureTask<>(() -> edge.traverse(init));
        new Thread(otherThreadState).start();
        assertNull(otherThreadState.get());
    }

    private class V extends Vertex {
        private V(Graph graph, String label) {
            super(graph, label, ANY_LOC, ANY_LOC);
        }

        @Override public String toString() {
            return getLabel();
        }
    }

    private class TempVertex extends V implements TemporaryVertex {
        private TempVertex(Graph graph, String label) {
            super(graph, label);
        }

        @Override public boolean isEndVertex() {
            throw new IllegalStateException("The `isEndVertex` is not used by dispose logic.");
        }
    }
}
