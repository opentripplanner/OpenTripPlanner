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
        Vertex b = new V(graph, "B");
        Vertex c = new TempVertex(graph, "C");
        Vertex d = new V(graph, "D");
        Vertex e = new V(graph, "E");

        new FreeEdge(a, b);
        Edge edgeToTemporaryVertex = new FreeEdge(b, c);
        new FreeEdge(c, d);
        new FreeEdge(d, e);
        RoutingRequest options1 = new RoutingRequest();
        options1.setRoutingContext(graph, a, e);
        options1.rctx.temporaryVertices.add(c);
        State init1 = new State(b, options1);
        assertNotNull(edgeToTemporaryVertex.traverse(init1));
        FutureTask<State> otherThreadState = new FutureTask<>(() -> {
            RoutingRequest options2 = new RoutingRequest();
            options2.setRoutingContext(graph, a, e);
            State init2 = new State(b, options2);
            return edgeToTemporaryVertex.traverse(init2);
        });
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
