package org.opentripplanner.graph_builder.linking;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.SemiPermanentPartialStreetEdge;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.SemiPermanentSplitterVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.NonLocalizedString;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        verify(spyStreetSplitter).linkToGraph(
            any(Vertex.class),
            eq(TraverseMode.WALK),
            eq(routingRequest),
            eq(false),
            eq(false)
        );
    }

    /**
     * Tests the full lifecycle of creating and disposing of all vertices and edges associated with a floating bike
     * rental. Floating bike rentals need to be inserted into the StreetNetwork once found in updater data and then
     * deleted from the graph once they are no longer rentable.
     *
     * This test contains a the following Acts:
     * 1. Setup graph with initial conditions before bike rentals are inserted
     * 2. Verify that the graph is in the initial state
     * 3. Add a floating bike rental to the graph
     * 4. Verify that the graph was properly changed to accomodate the floating bike rental
     * 5. Create an origin and destination and link those to the graph
     * 6. Verify that the origin and destination were properly linked to the graph
     * 7. Remove the origin and destination from the graph
     * 8. Verify that the origin and destination were properly removed from the graph
     * 9. Insert another bike rental station very close to the first floating bike
     * 10. Verify that the bike rental station was properly setup in the graph
     * 11. Remove the floating bike rental and bike rental station thus triggering the removal of all associated
     *      vertices and edges
     * 12. Verify that the graph is in the initial state
     */
    public void canLinkAndDestroyAFloatingBikeRental () {
        // Begin Act 1:
        // create the base conditions of the graph with a few simple StreetEdges
        // - a graph with 3 intersections/vertices
        Graph g = new Graph();

        StreetVertex a = new IntersectionVertex(g, "A", 1.0, 1.0);
        StreetVertex b = new IntersectionVertex(g, "B", 0.0, 1.0);
        StreetVertex c = new IntersectionVertex(g, "C", 1.0, 0.0);

        // - And 3 streets between the vertices

        createStreetEdge(a, b, "a -> b");
        createStreetEdge(b, a, "b -> a");
        createStreetEdge(a, c, "a -> c");
        g.index(new DefaultStreetVertexIndexFactory());
        StreetSplitter splitter = g.streetIndex.getStreetSplitter();

        // the initial graph should look like this:
        //
        // B ==(1)== A
        //           |
        //          (2)
        //           |
        //           C
        //
        // Vertices:
        // A = StreetVertex A
        // B = StreetVertex B
        // C = StreetVertex C
        //
        // Edges:
        // 1 = StreetEdges a->b and b->a
        // 2 = StreetEdge a->c
        //
        // End Act 1

        // Begin Act 2
        // Verify that the graph is in the initial state


        // End Act 2

        // Begin Act 3
        // create a floating bike rental
        BikeRentalStation brstation = new BikeRentalStation();
        brstation.id = "bike_2";
        brstation.x = 0.5;
        brstation.y = 1.001;
        brstation.name =  new NonLocalizedString("bike_2");
        brstation.isFloatingBike = true;

        // connect the floating bike rental to the graph
        BikeRentalStationVertex bikeRentalStationVertex = new BikeRentalStationVertex(g, brstation);
        splitter.linkToClosestWalkableEdge(bikeRentalStationVertex, DESTRUCTIVE_SPLIT, true);

        // The graph should now look like this:
        //
        //
        // B =====(1)==== A
        // \\           // \
        //   =(3)=D=(4)=   (2)
        //       ||         \
        //       (5)         C
        //       ||
        //        E<(6)>
        //
        // Vertices:
        // A = StreetVertex A
        // B = StreetVertex B
        // C = StreetVertex C
        // D = SemiPermanentSplitterVertex D
        // E = BikeRentalStationVertex E
        //
        // Edges:
        // 1 = StreetEdges A->B and B->A
        // 2 = StreetEdge A->C
        // 3 = SemiPermanentPartialStreetEdge (split from A->B and B->A)
        // 4 = SemiPermanentPartialStreetEdge (split from A->B and B->A)
        // 5 = StreetBikeRentalLinks (
        //          SplitterVertex D -> BikeRentalStationVertex E
        //          and
        //          BikeRentalStationVertex E -> SplitterVertex D
        //     )
        // 6 = RentABikeOnEdge (E -> E)
        // * note that since the bike rental station is a floating bike and doesn't allow dropoffs, we do not expect
        // the graph structure to have a RentABikeOffEdge
        // End Act 3

        // Begin Act 4
        // verify that the proper edges and vertices have been created for this floating bike rental

        // verify that vertex B now has 2 outgoing and incoming edges
        assertEquals(2, b.getOutgoing().size());
        assertEquals(2, b.getIncoming().size());

        // verify that vertex A now has 3 outgoing and 2 incoming edges
        assertEquals(3, a.getOutgoing().size());
        assertEquals(2, a.getIncoming().size());

        // make sure there is exactly 1 SemiPermanentPartialStreetEdge in the following places:
        // outgoing from vertex a
        assertEquals(1, getNumSemiPermanentPartialStreetEdges(a, true));
        // incoming from vertex a
        assertEquals(1, getNumSemiPermanentPartialStreetEdges(a, false));
        // outgoing from vertex b
        assertEquals(1, getNumSemiPermanentPartialStreetEdges(b, true));
        // incoming from vertex b
        assertEquals(1, getNumSemiPermanentPartialStreetEdges(b, false));

        // find the SemiPermanentPartialStreetEdge going out from vertex B
        SemiPermanentPartialStreetEdge edgeBtoD = null;
        for (Edge edge : b.getOutgoing()) {
            if (edge instanceof SemiPermanentPartialStreetEdge) edgeBtoD = (SemiPermanentPartialStreetEdge) edge;
        }

        // verify that the toVertex is a SemiPermanentSplitterVertex type
        Vertex vertexD = edgeBtoD.getToVertex();
        assertTrue(vertexD instanceof SemiPermanentSplitterVertex);

        // verify that there are 3 outgoing and incoming edges from vertexD
        assertEquals(3, vertexD.getDegreeOut());
        assertEquals(3, vertexD.getDegreeIn());

        // find the StreetBikeRentalLink going out from vertex D
        SemiPermanentPartialStreetEdge edgeDtoE = null;
        for (Edge edge : b.getOutgoing()) {
            if (edge instanceof StreetBikeRentalLink) edgeDtoE = (SemiPermanentPartialStreetEdge) edge;
        }
        assertNotNull(edgeDtoE);

        // verify the to vertex of edgeDtoE is a BikeRentalStationVertex
        Vertex vertexE = edgeDtoE.getToVertex();
        assertTrue(vertexE instanceof BikeRentalStationVertex);

        // verify that the number of incoming and outgoing edges matches expectations
        assertEquals(2, vertexE.getDegreeOut());
        assertEquals(1, vertexD.getDegreeIn());

        // End Act 4

        // Begin Act 5
        // Create an origin and destination and link those to the graph

        // The travel *origin* is very close to the road from B to A
        GenericLocation from = new GenericLocation(0.999, 0.4);

        // The *destination* is slightly off 0.7 degrees on road from C to A
        GenericLocation to = new GenericLocation(0.701, 1.001);

        // Create a request and routing context that'll link the locations to the graph
        RoutingRequest request = new RoutingRequest();
        request.from = from;
        request.to = to;
        RoutingContext subject = new RoutingContext(request, g);

        // the graph should now look like this:
        //
        //
        // B ===========(1)=========   A -(7)-F-(8)-G
        // \\\                      / //\
        // \\ \       (9)-H        / // (2)
        // \\  \       \          / //   \
        //  \\  --(10)-I--(11)---  //     C
        //   \\         \         //
        //   \\        (12)      //
        //    \\         \      //
        //     ====(3)====D=(4)=
        //               ||
        //               (5)
        //               ||
        //                E<(6)>
        //
        // Vertices:
        // A = StreetVertex A
        // B = StreetVertex B
        // C = StreetVertex C
        // D = SemiPermanentSplitterVertex D
        // E = BikeRentalStationVertex E
        // F = TemporarySplitterVertex F (for accessing TemporaryStreetLocation G)
        // G = TemporaryStreetLocation G (destination)
        // H = TemporaryStreetLocation H (origin)
        // I = TemporarySplitterVertex I (for accessing TemporaryStreetLocation H)
        //
        // Edges:
        // 1 = StreetEdges A->B and B->A
        // 2 = StreetEdge A->C
        // 3 = SemiPermanentPartialStreetEdge (split from A->B and B->A)
        // 4 = SemiPermanentPartialStreetEdge (split from A->B and B->A)
        // 5 = StreetBikeRentalLinks (
        //          SplitterVertex D -> BikeRentalStationVertex E
        //          and
        //          BikeRentalStationVertex E -> SplitterVertex D
        //     )
        // 6 = RentABikeOnEdge (E -> E)
        // 7 = TemporaryPartialStreetEdge (split from A->C)
        // 8 = TemporaryFreeEdge (F->G)?
        // 9 = TemporaryFreeEdge (H->I)?
        // 10 = TemporaryPartialStreetEdge (split from A->B)
        // 11 = TemporaryPartialStreetEdge (split from B->A)
        // 12 = TemporaryPartialStreetEdge (split from B->D)
        // End Act 5

        // Begin Act 6
        // Verify that the origin and destination were properly linked to the graph
        assertEquals("Origin", subject.fromVertex.getName());
        assertEquals("Destination", subject.toVertex.getName());

        // - And the from vertex should have outgoing edges
        assertTrue(
            "From vertex should have at least 1 outgoing edge",
            subject.fromVertex.getOutgoing().size() > 0
        );

        // End Act 6

        // Begin Act 7
        // Remove the origin and destination from the graph


        // The graph should now look like this:
        //
        //
        // B =====(1)==== A
        // \\           // \
        //   =(3)=D=(4)=   (2)
        //       ||         \
        //       (5)         C
        //       ||
        //        E<(6)>
        //
        // Vertices:
        // A = StreetVertex A
        // B = StreetVertex B
        // C = StreetVertex C
        // D = SemiPermanentSplitterVertex D
        // E = BikeRentalStationVertex E
        //
        // Edges:
        // 1 = StreetEdges A->B and B->A
        // 2 = StreetEdge A->C
        // 3 = SemiPermanentPartialStreetEdge (split from A->B and B->A)
        // 4 = SemiPermanentPartialStreetEdge (split from A->B and B->A)
        // 5 = StreetBikeRentalLinks (
        //          SplitterVertex D -> BikeRentalStationVertex E
        //          and
        //          BikeRentalStationVertex E -> SplitterVertex D
        //     )
        // 6 = RentABikeOnEdge (E -> E)
        // End Act 7

        // Begin Act 8
        // Verify that the origin and destination were properly removed from the graph

        // End Act 8

        // Begin Act 9
        // Insert another bike rental station very close to the first floating bike

        // The graph should now look like this:
        // Graph with Another BikeRentalStationVertex BRSL, RABOnE, RABOffE

        // End Act 9

        // Begin Act 10
        // Verify that the bike rental station was properly setup in the graph

        // End Act 10

        // Begin Act 11
        // Remove the floating bike rental and bike rental station thus triggering the removal of all associated
        //  vertices and edges


        // The graph should now look like this:
        //
        // B ==(1)== A
        //           |
        //          (2)
        //           |
        //           C
        //
        // Vertices:
        // A = StreetVertex A
        // B = StreetVertex B
        // C = StreetVertex C
        //
        // Edges:
        // 1 = StreetEdges a->b and b->a
        // 2 = StreetEdge a->c
        //
        // End Act 11

        // Begin Act 12
        // Verify that the graph is in the initial state


    }

    private int getNumSemiPermanentPartialStreetEdges(StreetVertex v, boolean getOutgoing) {
        int numMathes = 0;
        for (Edge edge : getOutgoing ? v.getOutgoing() : v.getIncoming()) {
            if (edge instanceof SemiPermanentPartialStreetEdge) numMathes++;
        }
        return numMathes;
    }

    private SemiPermanentPartialStreetEdge getFirstOutgoingSemiPermanentPartialStreetEdge(StreetVertex v) {
        for (Edge edge : v.getOutgoing()) {
            if (edge instanceof SemiPermanentPartialStreetEdge) return (SemiPermanentPartialStreetEdge) edge;
        }
        return null;
    }

    private void createStreetEdge(StreetVertex v0, StreetVertex v1, String name) {
        LineString geom = gf
            .createLineString(new Coordinate[] { v0.getCoordinate(), v1.getCoordinate() });
        double dist = SphericalDistanceLibrary.distance(v0.getCoordinate(), v1.getCoordinate());
        new StreetEdge(v0, v1, geom, name, dist, StreetTraversalPermission.ALL, false);
    }
}
