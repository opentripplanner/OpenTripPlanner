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
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.edgetype.SemiPermanentPartialStreetEdge;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.edgetype.TemporaryPartialStreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.SemiPermanentSplitterVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporarySplitterVertex;
import org.opentripplanner.util.NonLocalizedString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.opentripplanner.graph_builder.linking.StreetSplitter.DESTRUCTIVE_SPLIT;
import static org.opentripplanner.graph_builder.linking.StreetSplitter.NON_DESTRUCTIVE_SPLIT;

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
    @Test
    public void canLinkAndDestroyBikeRentalStations () {
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
        g.index(false);
        
        // When - a bike rental station between A and B that has been inserted into the graph
        BikeRentalStation brstation = new BikeRentalStation();
        brstation.id = "bike_1";
        brstation.x = 0.5;
        brstation.y = 1.001;
        brstation.name =  new NonLocalizedString("bike_1");
        BikeRentalStationVertex bikeRentalStationVertex = new BikeRentalStationVertex(g, brstation);
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
        verifyInitialState(a, b, c);

        // End Act 2

        // Begin Act 3
        // create a floating bike rental
        BikeRentalStation floatingBike = new BikeRentalStation();
        floatingBike.id = "bike_2";
        floatingBike.x = 0.5;
        floatingBike.y = 1.001;
        floatingBike.name =  new NonLocalizedString("bike_2");
        floatingBike.allowDropoff = false;
        floatingBike.isFloatingBike = true;
        floatingBike.spacesAvailable = 0;
        floatingBike.bikesAvailable = 1;

        // connect the floating bike rental to the graph
        BikeRentalStationVertex bikeRentalStationVertexE = new BikeRentalStationVertex(g, floatingBike);
        splitter.linkToClosestWalkableEdge(bikeRentalStationVertexE, NON_DESTRUCTIVE_SPLIT, true);
        new RentABikeOnEdge(bikeRentalStationVertexE, bikeRentalStationVertexE, floatingBike.networks);

        // The graph should now look like this:
        //
        //
        // B =====(1)==== A
        // \\           // \
        //   =(3)=D=(4)=   (2)
        //      ||||        \
        //       (5)         C
        //      ||||
        //        E<(6)>
        //
        // Vertices:
        // A = StreetVertex A
        // B = StreetVertex B
        // C = StreetVertex C
        // D = SemiPermanentSplitterVertices D1 and D2. There will be two of these, one for the split between edge A->B
        //      and another for the split between edge B->A
        // E = BikeRentalStationVertex E
        //
        // Edges:
        // 1 = StreetEdges A->B and B->A
        // 2 = StreetEdge A->C
        // 3 = SemiPermanentPartialStreetEdges (B->D1 and D2->B) (split from A->B and B->A)
        // 4 = SemiPermanentPartialStreetEdges (A->D2 and D1->A) (split from A->B and B->A)
        // 5 = StreetBikeRentalLinks (
        //          SemiPermanentSplitterVertex D1&2 -> BikeRentalStationVertex E
        //          and
        //          BikeRentalStationVertex E -> SemiPermanentSplitterVertex D1&2
        //     )
        // 6 = RentABikeOnEdge (E -> E)
        // * note that since the bike rental station is a floating bike and doesn't allow dropoffs, we do not expect
        // the graph structure to have a RentABikeOffEdge
        // End Act 3

        // Begin Act 4
        // verify that the proper edges and vertices have been created for this floating bike rental
        verifySingleBikeRentalGraphState(a, b, c);

        // End Act 4

        // Begin Act 5
        // Create an origin and destination and link those to the graph

        // The travel *origin* is very close to the road from B to A
        GenericLocation from = new GenericLocation(0.999, 0.4);

        // The *destination* is slightly off 0.7 degrees on road from C to A
        GenericLocation to = new GenericLocation(0.701, 1.001);

        // Create a request and routing context that'll link the locations to the graph
        RoutingRequest request = new RoutingRequest(new TraverseModeSet(TraverseMode.WALK, TraverseMode.BICYCLE));
        request.from = from;
        request.to = to;
        request.setRoutingContext(g);
        RoutingContext subject = request.rctx;

        // the graph should now look like this:
        //
        //
        // B ===========(1)=========   A -(7)-F-(8)-G
        // \\\                      / //\
        // \\ \       (9)≡H        / // (2)
        // \\  \      \\\         / //   \
        //  \\  --(10)-I--(11)---  //     C
        //   \\         \         //
        //   \\        (12)      //
        //    \\         \      //
        //     ====(3)====D=(4)=
        //              ||||
        //               (5)
        //              ||||
        //                E<(6)>
        //
        // Vertices:
        // A = StreetVertex A
        // B = StreetVertex B
        // C = StreetVertex C
        // D = SemiPermanentSplitterVertices D1 and D2. There will be two of these, one for the split between edge A->B
        //      and another for the split between edge B->A
        // E = BikeRentalStationVertex E
        // F = TemporarySplitterVertex F (for accessing TemporaryStreetLocation G)
        // G = TemporaryStreetLocation G (destination)
        // H = TemporaryStreetLocation H (origin)
        // I = TemporarySplitterVertex I 1, 2 and 3 (for accessing TemporaryStreetLocation H)
        //
        // Edges:
        // 1 = StreetEdges A->B and B->A
        // 2 = StreetEdge A->C
        // 3 = SemiPermanentPartialStreetEdges (B->D1 and D2->B) (split from A->B and B->A)
        // 4 = SemiPermanentPartialStreetEdges (A->D2 and D1->A) (split from A->B and B->A)
        // 5 = StreetBikeRentalLinks (
        //          SemiPermanentSplitterVertex D1&2 -> BikeRentalStationVertex E
        //          and
        //          BikeRentalStationVertex E -> SemiPermanentSplitterVertex D1&2
        //     )
        // 6 = RentABikeOnEdge (E -> E)
        // 7 = TemporaryPartialStreetEdge (split from A->C)
        // 8 = TemporaryFreeEdge (F->G)?
        // 9 = TemporaryFreeEdges (H->I1, H->I2, H->I3)
        // 10 = TemporaryPartialStreetEdge (I->B) (split from A->B)
        // 11 = TemporaryPartialStreetEdge (I->A) (split from B->A)
        // 12 = TemporaryPartialStreetEdge (I->D) (split from B->D)
        // End Act 5

        // Begin Act 6
        // Verify that the origin and destination were properly linked to the graph
        assertEquals("Origin", subject.fromVertex.getName());
        assertEquals("Destination", subject.toVertex.getName());

        // Verify that the from vertex has 3 outgoing edges
        assertEquals(3, subject.fromVertex.getDegreeOut());

        // search from the origin and make sure the expected progression of vertices and edges occurs until vertices A,
        // B and D1 are reached.

        // get vertex D1 via finding the SemiPermanentPartialStreetEdge going out from vertex B
        SemiPermanentPartialStreetEdge edgeBtoD = null;
        for (Edge edge : b.getOutgoing()) {
            if (edge instanceof SemiPermanentPartialStreetEdge) edgeBtoD = (SemiPermanentPartialStreetEdge) edge;
        }
        Vertex vertexD1 = edgeBtoD.getToVertex();

        // get the outgoing edges from the origin
        for (Edge edge : subject.fromVertex.getOutgoing()) {
            assertTrue(edge instanceof TemporaryFreeEdge);

            // verify each next vertex is a TemporarySplitterVertex that has 1 outgoing edge
            Vertex vertexFromOriginEdge = edge.getToVertex();
            assertTrue(vertexFromOriginEdge instanceof TemporarySplitterVertex);
            assertEquals(1, vertexFromOriginEdge.getDegreeOut());

            Edge outgoingEdge = null;
            for (Edge edge1 : vertexFromOriginEdge.getOutgoing()) {
                outgoingEdge = edge1;
            }

            // verify this next edge is a TemporaryPartialStreetEdge
            assertTrue(outgoingEdge instanceof TemporaryPartialStreetEdge);

            // verify that the next edge connects to either Vertex A, B or D1
            boolean acceptableEndVertex = false;
            assertTrue(
                outgoingEdge.getToVertex() == a ||
                    outgoingEdge.getToVertex() == b ||
                    outgoingEdge.getToVertex() == vertexD1
            );
        }

        // verify that vertex A now has 4 outgoing and 3 incoming edges
        assertEquals(4, a.getDegreeOut());
        assertEquals(3, a.getDegreeIn());

        // verify that vertex A has exactly 1 incoming TemporaryPartialStreetEdge
        assertEquals(1, getNumEdgesOfType(a, TemporaryPartialStreetEdge.class, false));

        // verify that vertex B now has 2 outgoing and 3 incoming edges
        assertEquals(2, b.getDegreeOut());
        assertEquals(3, b.getDegreeIn());

        // verify that vertex B has exactly 1 incoming TemporaryPartialStreetEdge
        assertEquals(1, getNumEdgesOfType(b, TemporaryPartialStreetEdge.class, false));

        // get edge from vertex A to vertex F
        TemporaryPartialStreetEdge edgeAtoF = null;
        for (Edge edge : a.getOutgoing()) {
            if (edge instanceof TemporaryPartialStreetEdge) edgeAtoF = (TemporaryPartialStreetEdge) edge;
        }
        assertNotNull(edgeAtoF);

        // verify that the to vertex of edge A to F is a TemporarySplitterVertex
        assertTrue(edgeAtoF.getToVertex() instanceof TemporarySplitterVertex);

        // verify that vertex F has 1 outgoing edge
        Vertex vertexF = edgeAtoF.getToVertex();
        assertEquals(1, vertexF.getDegreeOut());

        // verify that the outgoing edge from vertex F is a TemporaryFreeEdge
        Edge edgeFtoG = null;
        for (Edge edge : vertexF.getOutgoing()) {
            edgeFtoG = edge;
        }
        assertTrue(edgeFtoG instanceof TemporaryFreeEdge);

        // verify that the to vertex of edgeFtoG is the destination
        assertEquals(subject.toVertex, edgeFtoG.getToVertex());

        // End Act 6

        // Begin Act 7
        // Remove the origin and destination from the graph
        request.cleanup();

        // The graph should now look like this:
        //
        //
        // B =====(1)==== A
        // \\           // \
        //   =(3)=D=(4)=   (2)
        //       ||||       \
        //        (5)        C
        //       ||||
        //        E<(6)>
        //
        // Vertices:
        // A = StreetVertex A
        // B = StreetVertex B
        // C = StreetVertex C
        // D = SemiPermanentSplitterVertices D1 and D2. There will be two of these, one for the split between edge A->B
        //      and another for the split between edge B->A
        // E = BikeRentalStationVertex E
        //
        // Edges:
        // 1 = StreetEdges A->B and B->A
        // 2 = StreetEdge A->C
        // 3 = SemiPermanentPartialStreetEdges (B->D1 and D2->B) (split from A->B and B->A)
        // 4 = SemiPermanentPartialStreetEdges (A->D2 and D1->A) (split from A->B and B->A)
        // 5 = StreetBikeRentalLinks (
        //          SemiPermanentSplitterVertex D1&2 -> BikeRentalStationVertex E
        //          and
        //          BikeRentalStationVertex E -> SemiPermanentSplitterVertex D1&2
        //     )
        // 6 = RentABikeOnEdge (E -> E)
        // End Act 7

        // Begin Act 8
        // Verify that the origin and destination were properly removed from the graph
        verifySingleBikeRentalGraphState(a, b, c);

        // End Act 8

        // Begin Act 9
        // Insert another bike rental station very close to the first floating bike
        BikeRentalStation bikeDockingStation = new BikeRentalStation();
        bikeDockingStation.id = "docking station";
        bikeDockingStation.x = 0.45;
        bikeDockingStation.y = 1.001;
        bikeDockingStation.name =  new NonLocalizedString("docking station");
        bikeDockingStation.bikesAvailable = 10;
        bikeDockingStation.spacesAvailable = 4;

        // connect the bike rental station to the graph
        BikeRentalStationVertex bikeRentalStationVertexJ = new BikeRentalStationVertex(g, bikeDockingStation);
        splitter.linkToClosestWalkableEdge(bikeRentalStationVertexJ, NON_DESTRUCTIVE_SPLIT, true);
        new RentABikeOnEdge(bikeRentalStationVertexJ, bikeRentalStationVertexJ, floatingBike.networks);
        new RentABikeOffEdge(bikeRentalStationVertexJ, bikeRentalStationVertexJ, floatingBike.networks);

        // the graph should now look like this:
        //
        //   <(7)>J<(8)>
        //       \\\\
        //        (9)
        //        \\\\
        //   ==(10)=K=(11)
        // //            \\
        // B =====(1)==== A
        // \\           // \
        //   =(3)=D=(4)=   (2)
        //       ||||       \
        //       (5)         C
        //       ||||
        //        E<(6)>
        //
        // Vertices:
        // A = StreetVertex A
        // B = StreetVertex B
        // C = StreetVertex C
        // D = SemiPermanentSplitterVertices D1 and D2. There will be two of these, one for the split between edge A->B
        //      and another for the split between edge B->A
        // E = BikeRentalStationVertex E
        // J = BikeRentalStationVertex J
        // K = SemiPermanentSplitterVertices K1 and K2. There will be two of these, one for the split between edge A->B
        //      and another for the split between edge B->A
        //
        // Edges:
        // 1 = StreetEdges A->B and B->A
        // 2 = StreetEdge A->C
        // 3 = SemiPermanentPartialStreetEdges (B->D1 and D2->B) (split from A->B and B->A)
        // 4 = SemiPermanentPartialStreetEdges (A->D2 and D1->A) (split from A->B and B->A)
        // 5 = StreetBikeRentalLinks (
        //          SemiPermanentSplitterVertex D1&2 -> BikeRentalStationVertex E
        //          and
        //          BikeRentalStationVertex E -> SemiPermanentSplitterVertex D1&2
        //     )
        // 6 = RentABikeOnEdge (E -> E)
        // 7 = RentABikeOnEdge (J -> J)
        // 8 = RentABikeOffEdge (J -> J)
        // 9 = StreetBikeRentalLinks (
        //          SemiPermanentSplitterVertex K1&2 -> BikeRentalStationVertex J
        //          and
        //          BikeRentalStationVertex J -> SemiPermanentSplitterVertex K1&2
        //     )
        // 10 = SemiPermanentPartialStreetEdges (B->K1 and K2->B) (split from A->B and B->A)
        // 11 = SemiPermanentPartialStreetEdges (A->K2 and K1->A) (split from A->B and B->A)

        // End Act 9

        // Begin Act 10
        // Verify that the bike rental station was properly setup in the graph

        // verify that vertex A now has 4 outgoing and 3 incoming edges
        assertEquals(4, a.getDegreeOut());
        assertEquals(3, a.getDegreeIn());

        // verify that vertex B now has 3 outgoing and incoming edges
        assertEquals(3, b.getDegreeOut());
        assertEquals(3, b.getDegreeIn());

        // make sure there are exactly 2 SemiPermanentPartialStreetEdge in the following places:
        // outgoing from vertex a
        assertEquals(2, getNumEdgesOfType(a, SemiPermanentPartialStreetEdge.class, true));
        // incoming from vertex a
        assertEquals(2, getNumEdgesOfType(a, SemiPermanentPartialStreetEdge.class, false));
        // outgoing from vertex b
        assertEquals(2, getNumEdgesOfType(b, SemiPermanentPartialStreetEdge.class, true));
        // incoming from vertex b
        assertEquals(2, getNumEdgesOfType(b, SemiPermanentPartialStreetEdge.class, false));

        // find the SemiPermanentPartialStreetEdge going out from vertex B to vertex K
        SemiPermanentPartialStreetEdge edgeBtoK1 = null;
        for (Edge edge : b.getOutgoing()) {
            if (edge instanceof SemiPermanentPartialStreetEdge && edge != edgeBtoD) {
                edgeBtoK1 = (SemiPermanentPartialStreetEdge) edge;
            }
        }
        assertNotNull(edgeBtoK1);

        // verify that the toVertex is a SemiPermanentSplitterVertex type
        Vertex vertexK1 = edgeBtoK1.getToVertex();
        assertTrue(vertexK1 instanceof SemiPermanentSplitterVertex);

        // verify that there are 2 outgoing and incoming edges from vertexK1
        assertEquals(2, vertexK1.getDegreeOut());
        assertEquals(2, vertexK1.getDegreeIn());

        // verify that there is 1 incoming and outgoing StreetBikeRentalLink from vertexK1
        assertEquals(1, getNumEdgesOfType(vertexK1, StreetBikeRentalLink.class, true));
        assertEquals(1, getNumEdgesOfType(vertexK1, StreetBikeRentalLink.class, false));

        // find the SemiPermanentPartialStreetEdge going out from vertex A
        SemiPermanentPartialStreetEdge edgeAtoK2 = null;
        for (Edge edge : a.getOutgoing()) {
            if (edge instanceof SemiPermanentPartialStreetEdge) edgeAtoK2 = (SemiPermanentPartialStreetEdge) edge;
        }

        // verify that the toVertex is a SemiPermanentSplitterVertex type
        Vertex vertexK2 = edgeAtoK2.getToVertex();
        assertTrue(vertexK2 instanceof SemiPermanentSplitterVertex);

        // verify that there are 2 outgoing and incoming edges from vertexK2
        assertEquals(2, vertexK2.getDegreeOut());
        assertEquals(2, vertexK2.getDegreeIn());

        // verify that there is 1 incoming and outgoing StreetBikeRentalLink from vertexK2
        assertEquals(1, getNumEdgesOfType(vertexK2, StreetBikeRentalLink.class, true));
        assertEquals(1, getNumEdgesOfType(vertexK2, StreetBikeRentalLink.class, false));

        // find the StreetBikeRentalLink going out from vertex K
        StreetBikeRentalLink edgeK1toJ = null;
        for (Edge edge : vertexK1.getOutgoing()) {
            if (edge instanceof StreetBikeRentalLink) edgeK1toJ = (StreetBikeRentalLink) edge;
        }
        assertNotNull(edgeK1toJ);

        // verify the to vertex of edgeDtoE is a BikeRentalStationVertex
        Vertex vertexJ = edgeK1toJ.getToVertex();
        assertTrue(vertexJ instanceof BikeRentalStationVertex);

        // verify that the number of incoming and outgoing edges matches expectations
        assertEquals(4, vertexJ.getDegreeOut());
        assertEquals(4, vertexJ.getDegreeIn());

        // verify that there is 1 outgoing RentABikeOnEdge and 1 incoming RentABikeOffEdge
        assertEquals(1, getNumEdgesOfType(vertexJ, RentABikeOnEdge.class, true));
        assertEquals(1, getNumEdgesOfType(vertexJ, RentABikeOffEdge.class, false));

        // End Act 10

        // Begin Act 11
        // Remove the floating bike rental and bike rental station thus triggering the removal of all associated
        //  vertices and edges

        // remove floating bike rental vertex and associated SemiPermanentSplitterVertex and SemiPermanentPartialStreetEdges
        findAndRemoveAssociatedSemiPermamentVerticesAndEdges(splitter, bikeRentalStationVertexE);
        g.removeVertexAndEdges(bikeRentalStationVertexE);

        // remove docking bike rental station vertex and associated SemiPermanentSplitterVertex and
        // SemiPermanentPartialStreetEdges
        findAndRemoveAssociatedSemiPermamentVerticesAndEdges(splitter, bikeRentalStationVertexJ);
        g.removeVertexAndEdges(bikeRentalStationVertexJ);

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
        verifyInitialState(a, b, c);
    }

    private void findAndRemoveAssociatedSemiPermamentVerticesAndEdges(
        StreetSplitter splitter,
        BikeRentalStationVertex bikeRentalStationVertex
    ) {
        for (Edge edge : bikeRentalStationVertex.getOutgoing()) {
            if (edge instanceof StreetBikeRentalLink) {
                StreetBikeRentalLink toStreetLink = (StreetBikeRentalLink) edge;
                StreetVertex streetVertex = (StreetVertex) toStreetLink.getToVertex();
                if (streetVertex != null && streetVertex instanceof SemiPermanentSplitterVertex) {
                    splitter.removeSemiPermanentVerticesAndEdges((SemiPermanentSplitterVertex) streetVertex);
                }
            }
        }
    }

    /**
     * Verifies that only the initial edges and vertices exist in the graph associated with the
     * canLinkAndDestroyBikeRentalStations test.
     *
     * the initial graph should look like this:
     *
     * B ==(1)== A
     *           |
     *          (2)
     *           |
     *           C
     *
     * Vertices:
     * A = StreetVertex A
     * B = StreetVertex B
     * C = StreetVertex C
     *
     * Edges:
     * 1 = StreetEdges a->b and b->a
     * 2 = StreetEdge a->c
     *
     * @param a Vertex A
     * @param b Vertex B
     * @param c Vertex C
     */
    private void verifyInitialState(StreetVertex a, StreetVertex b, StreetVertex c) {
        // verify that vertex A has 2 outgoing and 1 incoming edges
        assertEquals(2, a.getDegreeOut());
        assertEquals(1, a.getDegreeIn());

        // verify that vertex B has 1 outgoing and incoming edge
        assertEquals(1, b.getDegreeOut());
        assertEquals(1, b.getDegreeIn());

        // verify that vertex C has 0 outgoing and 1 incoming edges
        assertEquals(0, c.getDegreeOut());
        assertEquals(1, c.getDegreeIn());
    }

    /**
     * Verify that all of the initial edges are in place and that there is 1 bike rental station vertex setup between
     * vertices A and B.
     *
     * The graph should now look like this:
     *
     *
     * B =====(1)==== A
     * \\           // \
     *   =(3)=D=(4)=   (2)
     *       ||||       \
     *        (5)       C
     *       ||||
     *         E<(6)>
     *
     * Vertices:
     * A = StreetVertex A
     * B = StreetVertex B
     * C = StreetVertex C
     * D = SemiPermanentSplitterVertex D
     * E = BikeRentalStationVertex E
     *
     * Edges:
     * 1 = StreetEdges A->B and B->A
     * 2 = StreetEdge A->C
     * 3 = SemiPermanentPartialStreetEdges (B->D1 and D2->B) (split from A->B and B->A)
     * 4 = SemiPermanentPartialStreetEdges (A->D2 and D1->A) (split from A->B and B->A)
     * 5 = StreetBikeRentalLinks (
     *          SemiPermanentSplitterVertex D1&2 -> BikeRentalStationVertex E
     *          and
     *          BikeRentalStationVertex E -> SemiPermanentSplitterVertex D1&2
     *     )
     * 6 = RentABikeOnEdge (E -> E)
     * * note that since the bike rental station is a floating bike and doesn't allow dropoffs, we do not expect
     * the graph structure to have a RentABikeOffEdge
     *
     * @param a Vertex A
     * @param b Vertex B
     * @param c Vertex C
     */
    private void verifySingleBikeRentalGraphState(StreetVertex a, StreetVertex b, StreetVertex c) {
        // verify that vertex A now has 3 outgoing and 2 incoming edges
        assertEquals(3, a.getDegreeOut());
        assertEquals(2, a.getDegreeIn());

        // verify that vertex B now has 2 outgoing and incoming edges
        assertEquals(2, b.getDegreeOut());
        assertEquals(2, b.getDegreeIn());

        // verify that vertex C still has 0 outgoing and 1 incoming edges
        assertEquals(0, c.getDegreeOut());
        assertEquals(1, c.getDegreeIn());

        // make sure there is exactly 1 SemiPermanentPartialStreetEdge in the following places:
        // outgoing from vertex a
        assertEquals(1, getNumEdgesOfType(a, SemiPermanentPartialStreetEdge.class, true));
        // incoming from vertex a
        assertEquals(1, getNumEdgesOfType(a, SemiPermanentPartialStreetEdge.class, false));
        // outgoing from vertex b
        assertEquals(1, getNumEdgesOfType(b, SemiPermanentPartialStreetEdge.class, true));
        // incoming from vertex b
        assertEquals(1, getNumEdgesOfType(b, SemiPermanentPartialStreetEdge.class, false));

        // find the SemiPermanentPartialStreetEdge going out from vertex B
        SemiPermanentPartialStreetEdge edgeBtoD = null;
        for (Edge edge : b.getOutgoing()) {
            if (edge instanceof SemiPermanentPartialStreetEdge) edgeBtoD = (SemiPermanentPartialStreetEdge) edge;
        }

        // verify that the toVertex is a SemiPermanentSplitterVertex type
        Vertex vertexD1 = edgeBtoD.getToVertex();
        assertTrue(vertexD1 instanceof SemiPermanentSplitterVertex);

        // verify that there are 2 outgoing and incoming edges from vertexD1
        assertEquals(2, vertexD1.getDegreeOut());
        assertEquals(2, vertexD1.getDegreeIn());

        // verify that there is 1 incoming and outgoing StreetBikeRentalLink from vertexD1
        assertEquals(1, getNumEdgesOfType(vertexD1, StreetBikeRentalLink.class, true));
        assertEquals(1, getNumEdgesOfType(vertexD1, StreetBikeRentalLink.class, false));

        // find the SemiPermanentPartialStreetEdge going out from vertex A
        SemiPermanentPartialStreetEdge edgeAtoD = null;
        for (Edge edge : a.getOutgoing()) {
            if (edge instanceof SemiPermanentPartialStreetEdge) edgeAtoD = (SemiPermanentPartialStreetEdge) edge;
        }

        // verify that the toVertex is a SemiPermanentSplitterVertex type
        Vertex vertexD2 = edgeAtoD.getToVertex();
        assertTrue(vertexD2 instanceof SemiPermanentSplitterVertex);

        // verify that there are 2 outgoing and incoming edges from vertexD2
        assertEquals(2, vertexD2.getDegreeOut());
        assertEquals(2, vertexD2.getDegreeIn());

        // verify that there is 1 incoming and outgoing StreetBikeRentalLink from vertexD2
        assertEquals(1, getNumEdgesOfType(vertexD2, StreetBikeRentalLink.class, true));
        assertEquals(1, getNumEdgesOfType(vertexD2, StreetBikeRentalLink.class, false));

        // find the StreetBikeRentalLink going out from vertex D1
        StreetBikeRentalLink edgeD1toE = null;
        for (Edge edge : vertexD1.getOutgoing()) {
            if (edge instanceof StreetBikeRentalLink) edgeD1toE = (StreetBikeRentalLink) edge;
        }
        assertNotNull(edgeD1toE);

        // verify the to vertex of edgeD1toE is a BikeRentalStationVertex
        Vertex vertexE = edgeD1toE.getToVertex();
        assertTrue(vertexE instanceof BikeRentalStationVertex);

        // verify that the number of incoming and outgoing edges matches expectations
        assertEquals(3, vertexE.getDegreeOut());
        assertEquals(3, vertexE.getDegreeIn());

        // find the RentABikeOnEdge going out from vertex E
        RentABikeOnEdge edgeEtoE = null;
        for (Edge edge : vertexE.getOutgoing()) {
            if (edge instanceof RentABikeOnEdge) edgeEtoE = (RentABikeOnEdge) edge;
        }
        assertNotNull(edgeEtoE);
    }

    /**
     * Get the number of edges (either outgoing or incoming) from a vertex that are of the specified class type.
     */
    private int getNumEdgesOfType(Vertex v, Class clazz, boolean getOutgoing) {
        int numMatches = 0;
        for (Edge edge : getOutgoing ? v.getOutgoing() : v.getIncoming()) {
            if (clazz.isInstance(edge)) numMatches++;
        }
        return numMatches;
    }

    private void createStreetEdge(StreetVertex v0, StreetVertex v1, String name) {
        LineString geom = gf
            .createLineString(new Coordinate[] { v0.getCoordinate(), v1.getCoordinate() });
        double dist = SphericalDistanceLibrary.distance(v0.getCoordinate(), v1.getCoordinate());
        new StreetEdge(v0, v1, geom, name, dist, StreetTraversalPermission.ALL, false);
    }
}
