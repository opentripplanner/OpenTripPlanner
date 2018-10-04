package org.opentripplanner.routing.algorithm;

import junit.framework.TestCase;
import org.junit.Assert;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

import static org.mockito.Mockito.mock;


public class TestDominanceFunction extends TestCase {

    public void testGeneralDominanceFunction() {
        DominanceFunction minimumWeightDominanceFunction = new DominanceFunction.MinimumWeight();
        Vertex fromVertex = mock(TransitStopArrive.class);
        Vertex toVertex = mock(TransitStopDepart.class);
        RoutingRequest request = new RoutingRequest();

        // Test if domination works in the general case

        State stateA = new State(fromVertex, null, 0, request);
        State stateB = new State(toVertex, null, 0, request);
        stateA.weight = 1;
        stateB.weight = 2;

        Assert.assertTrue(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateA, stateB));
        Assert.assertFalse(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateB, stateA));

        // Simple transfers should not dominate

        SimpleTransfer simpleTransfer = mock(SimpleTransfer.class);
        State stateC = new State(fromVertex, simpleTransfer, 0, request);
        State stateD = new State(toVertex, null, 0, request);
        stateC.weight = 1;
        stateD.weight = 2;

        Assert.assertFalse(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateC, stateD));
        Assert.assertFalse(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateD, stateC));

        // Timed transfers should not dominate

        TimedTransferEdge timedTransferEdge = mock(TimedTransferEdge.class);
        State stateE = new State(fromVertex, timedTransferEdge, 0, request);
        State stateF = new State(toVertex, null, 0, request);
        stateE.weight = 1;
        stateF.weight = 2;

        Assert.assertFalse(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateE, stateF));
        Assert.assertFalse(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateF, stateE));
    }

    // TODO: Make unit tests for rest of dominance functionality
    // TODO: Make functional tests for concepts covered by dominance with current algorithm
    // (Specific transfers, bike rental, park and ride, turn restrictions)
}
