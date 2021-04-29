package org.opentripplanner.routing.algorithm;

import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitEntranceVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * This is adapted from {@link TestCarPickup}. All tests use the same graph structure, but a part
 * of the graph is changed before running each test. The setup method runs for each test, so this
 * is not a problem.
 */
public class TestBikeRental extends GraphRoutingTest {

    private Graph graph;
    private TransitStopVertex S1;
    private TransitEntranceVertex E1;
    private StreetVertex A, B, C, D;
    private BikeRentalStationVertex B1, B2;
    private StreetEdge SE1, SE2, SE3;

    @Override
    protected void setUp() throws Exception {
        // Generate a very simple graph
        //
        //   A <-> B <-> C <-> D
        //   A <-> S1
        //   B <-> B1
        //   C <-> B2
        //   D <-> E1

        graph = graphOf(new Builder() {
            @Override
            public void build() {
                S1 = stop("S1", 0, 45);
                E1 = entrance("E1", 0.004, 45);
                A = intersection("A", 0.001, 45);
                B = intersection("B", 0.002, 45);
                C = intersection("C", 0.003, 45);
                D = intersection("D", 0.004, 45);

                B1 = bikeRentalStation("B1", 0.002, 45);
                B2 = bikeRentalStation("B2", 0.004, 45);

                biLink(A, S1);
                biLink(D, E1);

                biLink(B, B1);
                biLink(C, B2);

                SE1 = street(A, B, 50, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
                SE2 = street(B, C, 1000, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
                SE3 = street(C, D, 50, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
            }
        });
    }

    public void testBikeRentalFromStation() {
        assertPath(S1, E1,"WALK - BEFORE_RENTING - AB street (75.19, 38), BICYCLE - RENTING_FROM_STATION - BC street (400.00, 200), WALK - HAVE_RENTED - CD street (75.19, 38)");
    }

    public void testFallBackToWalking() {
        SE2.setPermission(StreetTraversalPermission.PEDESTRIAN);
        assertPath(S1, E1,"WALK - BEFORE_RENTING - AB street (75.19, 38), WALK - BEFORE_RENTING - BC street (1,503.76, 752), WALK - BEFORE_RENTING - CD street (75.19, 38)");
}

    public void testNoBikesAvailable() {
        B1.setBikesAvailable(0);
        assertPath(S1, E1,"WALK - BEFORE_RENTING - AB street (75.19, 38), WALK - BEFORE_RENTING - BC street (1,503.76, 752), WALK - BEFORE_RENTING - CD street (75.19, 38)");
    }

    public void testNoSpacesAvailable() {
        B2.setSpacesAvailable(0);
        assertPath(S1, E1,"WALK - BEFORE_RENTING - AB street (75.19, 38), WALK - BEFORE_RENTING - BC street (1,503.76, 752), WALK - BEFORE_RENTING - CD street (75.19, 38)");
    }

    public void testFloatingBike() {
        B1.getStation().isFloatingBike = true;
        assertPath(S1, E1,"WALK - BEFORE_RENTING - AB street (75.19, 38), BICYCLE - RENTING_FLOATING - BC street (400.00, 200), BICYCLE - RENTING_FLOATING - CD street (20.00, 10)");
    }

    private void assertPath(Vertex fromVertex, Vertex toVertex, String descriptor) {
        String departAt = runStreetSearchAndCreateDescriptor(fromVertex, toVertex, false);
        String arriveBy = runStreetSearchAndCreateDescriptor(fromVertex, toVertex, true);

        assertDescriptors(descriptor, descriptor, arriveBy, departAt);
    }

    private void assertDescriptors(
            String expectedDepartAt,
            String expectedArriveBy,
            String arriveBy,
            String departAt
    ) {
        String formatString = "DepartAt: %s%nArriveBy: %s";

        assertEquals(
                String.format(
                        formatString,
                        expectedDepartAt,
                        expectedArriveBy
                ),
                String.format(
                        formatString,
                        departAt,
                        arriveBy
                )
        );
    }

    private String runStreetSearchAndCreateDescriptor(
            Vertex fromVertex,
            Vertex toVertex,
            boolean arriveBy
    ) {
        var options = new RoutingRequest();
        options.arriveBy = arriveBy;
        options.worstTime = arriveBy ? Long.MIN_VALUE : Long.MAX_VALUE;

        var bikeRentalOptions = options.getStreetSearchRequest(StreetMode.BIKE_RENTAL);
        bikeRentalOptions.useBikeRentalAvailabilityInformation = true;
        bikeRentalOptions.setRoutingContext(graph, fromVertex, toVertex);
        var tree = new AStar().getShortestPathTree(bikeRentalOptions);
        var path = tree.getPath(
                arriveBy ? fromVertex : toVertex,
                false
        );

        return path != null ? path.states
            .stream()
            .filter(s -> s.getBackEdge() instanceof StreetEdge)
            .map(s -> String.format(
                Locale.ROOT,
                "%s - %s - %s (%,.2f, %d)",
                s.getBackMode(),
                s.getBikeRentalState(),
                s.getBackEdge() != null ? s.getBackEdge().getName() : null,
                s.getWeightDelta(),
                s.getTimeDeltaSeconds()
            ))
            .collect(Collectors.joining(", ")) : "path not found";
    }
}
