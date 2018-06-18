package org.opentripplanner.graph_builder.linking;


import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SimpleStreetSplitterTest {

    private SimpleStreetSplitter spySimpleStreetSplitter;


    @Before
    public void buildSpy(){
        Graph graph = new Graph();
        SimpleStreetSplitter simpleStreetSplitter = new SimpleStreetSplitter(graph, null, null,false);
        spySimpleStreetSplitter = spy(simpleStreetSplitter);
    }

    /**
     * Tests that traverse mode WALK is used when getting closest end vertex for park and ride.
     */
    @Test
    public void testFindEndVertexForParkAndRide(){
        GenericLocation genericLocation = new GenericLocation(10,23);

        RoutingRequest routingRequest = new RoutingRequest();
        routingRequest.setMode(TraverseMode.CAR);
        routingRequest.parkAndRide = true;

        spySimpleStreetSplitter.getClosestVertex(genericLocation, routingRequest, true);
        verify(spySimpleStreetSplitter).link(any(Vertex.class), eq(TraverseMode.WALK), eq(routingRequest));
    }
}
