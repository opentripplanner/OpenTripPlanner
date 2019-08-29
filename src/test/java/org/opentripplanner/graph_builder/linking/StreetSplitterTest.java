package org.opentripplanner.graph_builder.linking;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.NonLocalizedString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.opentripplanner.graph_builder.linking.StreetSplitter.DESTRUCTIVE_SPLIT;

public class StreetSplitterTest {
    private final GeometryFactory gf = GeometryUtils.getGeometryFactory();
    private StreetSplitter spyStreetSplitter;


    @Before
    public void buildSpy(){
        Graph graph = new Graph();
        StreetSplitter streetSplitter = new StreetSplitter(graph, null, null);
        spyStreetSplitter = spy(streetSplitter);
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

        spyStreetSplitter.linkOriginDestination(genericLocation, routingRequest, true);
        verify(spyStreetSplitter).linkToGraph(any(Vertex.class), eq(TraverseMode.WALK), eq(routingRequest), eq(false));
    }

    /**
     * Tests whether the proper edge is linked in a request where the original StreetEdge has been
     * split to insert a bike rental station.
     */
    @Test
    public void canLinkToEdgeSplitForBikeRental() {
        // Given:
        // - a graph with 3 intersections/vertices
        Graph g = new Graph();

        StreetVertex a = new IntersectionVertex(g, "A", 1.0, 1.0);
        StreetVertex b = new IntersectionVertex(g, "B", 0.0, 1.0);
        StreetVertex c = new IntersectionVertex(g, "C", 1.0, 0.0);

        // - And 3 streets between the vertices

        createStreetEdge(a, b, "a -> b");
        createStreetEdge(b, a, "b -> a");
        createStreetEdge(a, c, "a -> c");
        g.index(false);
        
        // When - a bike rental station between A and B that has been inserted into the graph
        BikeRentalStation brstation = new BikeRentalStation();
        brstation.id = "bike_1";
        brstation.x = 0.5;
        brstation.y = 1.001;
        brstation.name =  new NonLocalizedString("bike_1");
        BikeRentalStationVertex bikeRentalStationVertex = new BikeRentalStationVertex(g, brstation);
        StreetSplitter splitter = g.streetIndex.getStreetSplitter();
        splitter.linkToClosestWalkableEdge(bikeRentalStationVertex, DESTRUCTIVE_SPLIT);

        // - And travel *origin* is very close to the road from B to A
        GenericLocation from = new GenericLocation(0.999, 0.4);

        // - and *destination* is slightly off 0.7 degrees on road from C to A
        GenericLocation to = new GenericLocation(0.701, 1.001);

        // - And A request
        RoutingRequest request = new RoutingRequest();
        request.from = from;
        request.to = to;

        // When - the context is created
        RoutingContext subject = new RoutingContext(request, g);

        // Then - the origin and destination is set properly
        assertEquals("Origin", subject.fromVertex.getName());
        assertEquals("Destination", subject.toVertex.getName());

        // - And the from vertex should have outgoing edges
        assertTrue(
            "From vertex should have at least 1 outgoing edge",
            subject.fromVertex.getOutgoing().size() > 0
        );
    }

    private void createStreetEdge(StreetVertex v0, StreetVertex v1, String name) {
        LineString geom = gf
            .createLineString(new Coordinate[] { v0.getCoordinate(), v1.getCoordinate() });
        double dist = SphericalDistanceLibrary.distance(v0.getCoordinate(), v1.getCoordinate());
        new StreetEdge(v0, v1, geom, name, dist, StreetTraversalPermission.ALL, false);
    }
}
