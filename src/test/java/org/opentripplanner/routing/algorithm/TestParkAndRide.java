package org.opentripplanner.routing.algorithm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.astar.strategies.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.VehicleParkingEdge;
import org.opentripplanner.routing.edgetype.StreetVehicleParkingLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vertextype.VehicleParkingVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.NonLocalizedString;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test P+R (both car P+R and bike P+R).
 * 
 * @author laurent
 */
public class TestParkAndRide extends GraphRoutingTest {

    private static final String TEST_FEED_ID = "testFeed";

    private Graph graph;
    private StreetVertex A,B,C,D;

    @BeforeEach
    protected void setUp() throws Exception {
        graph = new Graph();

        graph = graphOf(new Builder() {
            @Override
            public void build() {
                A = intersection("A", 0.000, 45);
                B = intersection("B", 0.001, 45);
                C = intersection("C", 0.002, 45);
                D = intersection("D", 0.003, 45);

                street(A, B, 87, StreetTraversalPermission.CAR);
                street(B, C, 87, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
                street(C, D, 87, StreetTraversalPermission.PEDESTRIAN);
            }
        });
    }

    @Test
    public void testCar() {
        // It is impossible to get from A to C in WALK mode,
        GraphPath path = runSearch(A, C, new TraverseModeSet(TraverseMode.WALK));
        assertNull(path);

        // or CAR+WALK (no P+R).
        path = runSearch(A, C, new TraverseModeSet(TraverseMode.WALK,TraverseMode.CAR));
        assertNull(path);

        // So we Add a P+R at B.
        var vehicleParkingName = new NonLocalizedString("P+R B");
        VehicleParking vehicleParking = VehicleParking.builder()
            .id(new FeedScopedId(TEST_FEED_ID, "P+R.B"))
            .name(vehicleParkingName)
            .x(0.001)
            .y(45.00001)
            .carPlaces(true)
            .build();
        VehicleParkingVertex PRB = new VehicleParkingVertex(graph, vehicleParking);
        new VehicleParkingEdge(PRB);
        new StreetVehicleParkingLink(PRB, B);
        new StreetVehicleParkingLink(B, PRB);

        // But it is still impossible to get from A to C by WALK only
        // (AB is CAR only).
        path = runSearch(A, C, new TraverseModeSet(TraverseMode.WALK));
        assertNull(path);
        
        // Or CAR only (BC is WALK only).
        path = runSearch(A, C, new TraverseModeSet(TraverseMode.CAR));
        assertNull(path);

        // But we can go from A to C with CAR+WALK mode using P+R. arriveBy false
        path = runSearch(A, C, new TraverseModeSet(TraverseMode.WALK,TraverseMode.CAR,TraverseMode.TRANSIT), false,
            request -> request.parkAndRide = true);
        assertEquals(
            "A - AB street - B - P+R B - P+R B - P+R B - P+R B - P+R B - B - BC street - C",
            pathToString(path)
        );

        // But we can go from A to C with CAR+WALK mode using P+R. arriveBy true
        path = runSearch(A, C, new TraverseModeSet(TraverseMode.WALK,TraverseMode.CAR,TraverseMode.TRANSIT), true,
            request -> request.parkAndRide = true);
        assertEquals(
            "A - AB street - B - P+R B - P+R B - P+R B - P+R B - P+R B - B - BC street - C",
            pathToString(path)
        );


        // But we can go from A to C with CAR+WALK mode using P+R. arriveBy true interleavedBidiHeuristic
        path = runSearch(A, C, new TraverseModeSet(TraverseMode.WALK,TraverseMode.CAR,TraverseMode.TRANSIT), true,
            request -> {
                request.parkAndRide = true;
                request.rctx.remainingWeightHeuristic = new EuclideanRemainingWeightHeuristic();
            }
        );
        assertEquals(
            "A - AB street - B - P+R B - P+R B - P+R B - P+R B - P+R B - B - BC street - C",
            pathToString(path)
        );

        // But we can go from A to C with CAR+WALK mode using P+R. arriveBy false interleavedBidiHeuristic
        path = runSearch(A, C, new TraverseModeSet(TraverseMode.WALK,TraverseMode.CAR,TraverseMode.TRANSIT), false,
            request -> {
                request.parkAndRide = true;
                request.rctx.remainingWeightHeuristic = new EuclideanRemainingWeightHeuristic();
            }
        );
        assertEquals(
            "A - AB street - B - P+R B - P+R B - P+R B - P+R B - P+R B - B - BC street - C",
            pathToString(path)
        );
    }

    private GraphPath runSearch(Vertex from, Vertex to, TraverseModeSet traverseModeSet) {
        return runSearch(from, to, traverseModeSet, false, o -> {});
    }

    private GraphPath runSearch(Vertex from, Vertex to, TraverseModeSet traverseModeSet, boolean arriveBy, Consumer<RoutingRequest> options) {
        RoutingRequest request = new RoutingRequest(traverseModeSet);
        request.setArriveBy(arriveBy);
        request.setRoutingContext(graph, from, to);
        options.accept(request);
        ShortestPathTree tree = new AStar().getShortestPathTree(request);
        return tree.getPath(arriveBy ? from : to, false);
    }

    @Test
    public void testBike() {
        // Impossible to get from B to D in BIKE+WALK (no bike P+R).
        GraphPath path = runSearch(B, D, new TraverseModeSet(TraverseMode.BICYCLE,TraverseMode.TRANSIT), false,
            request -> request.parkAndRide = true
        );
        assertNull(path);

        // So we add a bike P+R at C.
        var vehicleParkingName = new NonLocalizedString("Bike Park C");
        VehicleParking bpc = VehicleParking.builder()
            .id(new FeedScopedId(TEST_FEED_ID, "bpc"))
            .name(vehicleParkingName)
            .x(0.002)
            .y(45.00001)
            .bicyclePlaces(true)
            .build();
        VehicleParkingVertex BPRC = new VehicleParkingVertex(graph, bpc);
        new VehicleParkingEdge(BPRC);
        new StreetVehicleParkingLink(BPRC, C);
        new StreetVehicleParkingLink(C, BPRC);

        // Still impossible from B to D by bike only (CD is WALK only).
        path = runSearch(B, D, new TraverseModeSet(TraverseMode.BICYCLE));
        assertEquals("B - BC street - C - CD street - D", pathToString(path));

        State s = path.states.getLast();
        assertFalse(s.isVehicleParked());
        // TODO backWalkingBike flag is broken
        // assertTrue(s.isBackWalkingBike());
        assertSame(s.getBackMode(), TraverseMode.WALK);

        // But we can go from B to D using bike P+R.
        path = runSearch(B, D, new TraverseModeSet(TraverseMode.BICYCLE,TraverseMode.WALK,TraverseMode.TRANSIT), false,
            request -> request.parkAndRide = true
        );
       assertEquals(
           "B - BC street - C - Bike Park C - Bike Park C - Bike Park C - Bike Park C - Bike Park C - C - CD street - D",
           pathToString(path)
       );
    }

    private static String pathToString(GraphPath path) {
        return path.states.stream()
            .flatMap(s -> Stream.of(
                s.getBackEdge() != null ? s.getBackEdge().getName() : null,
                s.getVertex().getName()
            ))
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" - "));
    }
}
