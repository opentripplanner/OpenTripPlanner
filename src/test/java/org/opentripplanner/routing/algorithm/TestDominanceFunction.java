package org.opentripplanner.routing.algorithm;

import junit.framework.TestCase;
import org.junit.Assert;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import static org.mockito.Mockito.mock;


public class TestDominanceFunction extends TestCase {

    public void testGeneralDominanceFunction() {
        DominanceFunction minimumWeightDominanceFunction = new DominanceFunction.MinimumWeight();
        Vertex fromVertex = mock(TransitStopVertex.class);
        Vertex toVertex = mock(TransitStopVertex.class);
        RoutingRequest request = new RoutingRequest();

        // Test if domination works in the general case

        State stateA = new State(fromVertex, null, 0, request);
        State stateB = new State(toVertex, null, 0, request);
        stateA.weight = 1;
        stateB.weight = 2;

        Assert.assertTrue(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateA, stateB));
        Assert.assertFalse(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateB, stateA));
    }

    // TODO: Make unit tests for rest of dominance functionality
    // TODO: Make functional tests for concepts covered by dominance with current algorithm
    // (Specific transfers, bike rental, park and ride, turn restrictions)
}
