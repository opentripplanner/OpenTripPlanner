package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitEntranceVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

/**
 * Test CarPickup:
 * - it may start with (WALK - WALK_TO_PICKUP, CAR - IN_CAR)
 * - it may end with (WALK - WALK_FROM_DROP_OFF, CAR - IN_CAR)
 * - StreetTransitEntityLink require mode changes to WALK
 * - StreetEdges may contain mode changes between CAR / WALK
 *
 * arriveBy and departAt paths should be symmetric.
 */
public class CarPickupTest extends GraphRoutingTest {

    private Graph graph;
    private TransitStopVertex S1;
    private TransitEntranceVertex E1;
    private StreetVertex A, B, C, D, E;

    @BeforeEach
    protected void setUp() throws Exception {
        // Generate a very simple graph
        //
        //   A <-> B <-> C <-> D <-> E
        //   TS1 <-^           ^-> TE1

        graph = graphOf(new Builder() {
            @Override
            public void build() {
                S1 = stop("S1", 0, 45);
                E1 = entrance("E1", 0.004, 45);
                A = intersection("A", 0.001, 45);
                B = intersection("B", 0.002, 45);
                C = intersection("C", 0.003, 45);
                D = intersection("D", 0.004, 45);
                E = intersection("E", 0.005, 45);

                biLink(B, S1);
                biLink(C, E1);

                street(A, B, 87, StreetTraversalPermission.PEDESTRIAN);
                street(B, C, 87, StreetTraversalPermission.CAR);
                street(C, D, 87, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
                street(D, E, 87, StreetTraversalPermission.PEDESTRIAN);
            }
        });
    }

    @Test
    public void testCarPickupCarOnly() {
        assertPath(B, C, "null - IN_CAR - null, CAR - IN_CAR - BC street");
    }

    @Test
    public void testCarPickupCarThenWalk() {
        assertPath(
                A, C,
                "null - WALK_TO_PICKUP - null, WALK - WALK_TO_PICKUP - AB street, CAR - IN_CAR - BC street"
        );
    }

    @Test
    public void testCarPickupFromEntranceThenCar() {
        assertPath(
                S1, C,
                "null - WALK_TO_PICKUP - null, null - WALK_TO_PICKUP - S1, CAR - IN_CAR - BC street"
        );
    }

    @Test
    public void testCarPickupWalkFromEntranceThenCarThenWalk() {
        assertPath(
                S1, D,
                "null - WALK_TO_PICKUP - null, null - WALK_TO_PICKUP - S1, CAR - IN_CAR - BC street, WALK - WALK_FROM_DROP_OFF - CD street"
        );
    }

    @Test
    public void testCarPickupCarThenWalkToStop() {
        assertPath(
                B, E1,
                "null - IN_CAR - null, CAR - IN_CAR - BC street, null - WALK_FROM_DROP_OFF - E1"
        );
    }

    @Test
    public void testCarPickupWalkFromEntranceThenCarThenWalkToStop() {
        assertPath(
                S1, E1,
                "null - WALK_TO_PICKUP - null, null - WALK_TO_PICKUP - S1, CAR - IN_CAR - BC street, null - WALK_FROM_DROP_OFF - E1"
        );
    }

    @Test
    public void testCarPickupWalkThenCarThenWalk() {
        assertPath(
                A, D,
                "null - WALK_TO_PICKUP - null, WALK - WALK_TO_PICKUP - AB street, CAR - IN_CAR - BC street, WALK - WALK_FROM_DROP_OFF - CD street"
        );
    }

    @Test
    public void testWalkOnlyCarPickup() {
        // This is a special case where the reverse states differ, due to both starting in the IN_CAR
        // state and switching to walking when encountering the first edge. This is the only valid
        // path since a CarPickup must be in `IN_CAR` or `WALK_FROM_DROP_OFF` to be a final state,
        // and the path can't be traversed by car.
        assertPath(
                A, B,
                "null - IN_CAR - null, WALK - WALK_FROM_DROP_OFF - AB street",
                "null - WALK_TO_PICKUP - null, WALK - WALK_TO_PICKUP - AB street"
        );
    }

    private void assertPath(Vertex fromVertex, Vertex toVertex, String descriptor) {
        String departAt = runStreetSearchAndCreateDescriptor(fromVertex, toVertex, false);
        String arriveBy = runStreetSearchAndCreateDescriptor(fromVertex, toVertex, true);

        assertDescriptors(descriptor, descriptor, arriveBy, departAt);
    }

    private void assertPath(
            Vertex fromVertex,
            Vertex toVertex,
            String expectedDepartAt,
            String expectedArriveBy
    ) {
        String departAt = runStreetSearchAndCreateDescriptor(fromVertex, toVertex, false);
        String arriveBy = runStreetSearchAndCreateDescriptor(fromVertex, toVertex, true);

        assertDescriptors(expectedDepartAt, expectedArriveBy, arriveBy, departAt);
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

        var carPickupOptions = options.getStreetSearchRequest(StreetMode.CAR_PICKUP);
        carPickupOptions.setRoutingContext(graph, fromVertex, toVertex);
        var tree = new AStar().getShortestPathTree(carPickupOptions);
        var path = tree.getPath(
                arriveBy ? fromVertex : toVertex,
                false
        );

        return path != null ? path.states.stream().map(s -> String.format(
                "%s - %s - %s",
                s.getBackMode(),
                s.getCarPickupState(),
                s.getBackEdge() != null ? s.getBackEdge().getName() : null
        )).collect(Collectors.joining(", ")) : "path not found";
    }
}
