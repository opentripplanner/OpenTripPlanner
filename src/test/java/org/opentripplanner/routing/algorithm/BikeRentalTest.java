package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

/**
 * This is adapted from {@link CarPickupTest}. All tests use the same graph structure, but a part of
 * the graph is changed before running each test. The setup method runs for each test, so this is
 * not a problem.
 */
public class BikeRentalTest extends GraphRoutingTest {

    private Graph graph;
    private TransitStopVertex S1;
    private TransitEntranceVertex E1;
    private StreetVertex A, B, C, D;
    private BikeRentalStationVertex B1, B2;
    private StreetEdge SE1, SE2, SE3;

    @BeforeEach
    public void setUp() {
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
                S1 = stop("S1", 47.500, 19.001);
                A = intersection("A", 47.500, 19.000);
                B = intersection("B", 47.510, 19.000);
                C = intersection("C", 47.520, 19.000);
                D = intersection("D", 47.530, 19.000);
                E1 = entrance("E1", 47.530, 19.001);

                B1 = bikeRentalStation("B1", 47.510, 19.001);
                B2 = bikeRentalStation("B2", 47.520, 19.001);

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

    // This tests exists to test if the cost of walking with a bike changes
    @Test
    public void testWalkBikeOnly() {
        SE2.setPermission(StreetTraversalPermission.PEDESTRIAN);

        var descriptor = runStreetSearchAndCreateDescriptor(
                B, C,
                false, new RoutingRequest(),
                StreetMode.BIKE
        );

        assertEquals(List.of("WALK - null - BC street (1,503.76, 752)"), descriptor);
    }

    // This tests exists to test if the cost of biking changes
    @Test
    public void testBikeOnly() {
        var descriptor = runStreetSearchAndCreateDescriptor(
                B, C,
                false, new RoutingRequest(),
                StreetMode.BIKE
        );

        assertEquals(List.of("BICYCLE - null - BC street (400.00, 200)"), descriptor);
    }

    @Test
    public void testBikeRentalFromStation() {
        assertPath(
                S1, E1,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FROM_STATION - BC street (540.19, 280)",
                "WALK - HAVE_RENTED - CD street (650.38, 333)"
        );
    }

    @Test
    public void testFallBackToWalking() {
        SE2.setPermission(StreetTraversalPermission.PEDESTRIAN);

        assertPath(
                S1, E1, true,
                List.of(
                        "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                        "WALK - BEFORE_RENTING - BC street (1,579.95, 790)",
                        "WALK - BEFORE_RENTING - CD street (1,655.14, 828)"
                ),
                List.of(
                        "WALK - HAVE_RENTED - AB street (76.19, 38)",
                        "WALK - HAVE_RENTED - BC street (1,579.95, 790)",
                        "WALK - HAVE_RENTED - CD street (1,655.14, 828)"
                )
        );
    }

    @Test
    public void testNoBikesAvailable() {
        B1.setBikesAvailable(0);

        assertPath(
                S1, E1, true,
                List.of(
                        "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                        "WALK - BEFORE_RENTING - BC street (1,579.95, 790)",
                        "WALK - BEFORE_RENTING - CD street (1,655.14, 828)"
                ),
                List.of(
                        "WALK - HAVE_RENTED - AB street (76.19, 38)",
                        "WALK - HAVE_RENTED - BC street (1,579.95, 790)",
                        "WALK - HAVE_RENTED - CD street (1,655.14, 828)"
                )
        );
    }

    @Test
    public void testNoSpacesAvailable() {
        B2.setSpacesAvailable(0);

        assertPath(
                S1, E1, true,
                List.of(
                        "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                        "WALK - BEFORE_RENTING - BC street (1,579.95, 790)",
                        "WALK - BEFORE_RENTING - CD street (1,655.14, 828)"
                ),
                List.of(
                        "WALK - HAVE_RENTED - AB street (76.19, 38)",
                        "WALK - HAVE_RENTED - BC street (1,579.95, 790)",
                        "WALK - HAVE_RENTED - CD street (1,655.14, 828)"
                )
        );
    }

    @Test
    public void testIgnoreAvailabilityNoBikesAvailable() {
        B1.setBikesAvailable(0);

        assertPath(
                S1, E1, false,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FROM_STATION - BC street (540.19, 280)",
                "WALK - HAVE_RENTED - CD street (650.38, 333)"
        );
    }

    @Test
    public void testIgnoreAvailabilityNoSpacesAvailable() {
        B2.setSpacesAvailable(0);

        assertPath(
                S1, E1, false,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FROM_STATION - BC street (540.19, 280)",
                "WALK - HAVE_RENTED - CD street (650.38, 333)"
        );
    }

    @Test
    public void testFloatingBike() {
        B1.getStation().isFloatingBike = true;

        assertPath(
                S1, E1,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FLOATING - BC street (540.19, 280)",
                "BICYCLE - RENTING_FLOATING - CD street (560.19, 290)"
        );
    }

    private void assertPath(Vertex fromVertex, Vertex toVertex, String... descriptor) {
        assertPath(fromVertex, toVertex, true, List.of(descriptor), List.of(descriptor));
    }

    private void assertPath(
            Vertex fromVertex,
            Vertex toVertex,
            boolean useAvailabilityInformation,
            String... descriptor
    ) {
        assertPath(
                fromVertex, toVertex, useAvailabilityInformation, List.of(descriptor),
                List.of(descriptor)
        );
    }

    private void assertPath(
            Vertex fromVertex,
            Vertex toVertex,
            boolean useAvailabilityInformation,
            List<String> departAtDescriptor,
            List<String> arriveByDescriptor
    ) {
        List<String> departAt = runStreetSearchAndCreateDescriptor(fromVertex, toVertex, false,
                useAvailabilityInformation
        );
        List<String> arriveBy = runStreetSearchAndCreateDescriptor(fromVertex, toVertex, true,
                useAvailabilityInformation
        );

        assertEquals(departAtDescriptor, departAt, "departAt path");
        assertEquals(arriveByDescriptor, arriveBy, "arriveBy path");
    }

    private List<String> runStreetSearchAndCreateDescriptor(
            Vertex fromVertex,
            Vertex toVertex,
            boolean arriveBy,
            boolean useAvailabilityInformation
    ) {
        var options = new RoutingRequest();
        options.arriveBy = arriveBy;
        options.bikeRentalPickupTime = 42;
        options.bikeRentalPickupCost = 62;
        options.bikeRentalDropoffCost = 33;
        options.bikeRentalDropoffTime = 15;
        options.useBikeRentalAvailabilityInformation = useAvailabilityInformation;
        options.worstTime = arriveBy ? Long.MIN_VALUE : Long.MAX_VALUE;

        return runStreetSearchAndCreateDescriptor(
                fromVertex, toVertex, arriveBy, options, StreetMode.BIKE_RENTAL);
    }

    private List<String> runStreetSearchAndCreateDescriptor(
            Vertex fromVertex,
            Vertex toVertex,
            boolean arriveBy,
            RoutingRequest options,
            StreetMode streetMode
    ) {
        var bikeRentalOptions = options.getStreetSearchRequest(streetMode);
        bikeRentalOptions.setRoutingContext(graph, fromVertex, toVertex);

        var tree = new AStar().getShortestPathTree(bikeRentalOptions);
        var path = tree.getPath(
                arriveBy ? fromVertex : toVertex,
                false
        );

        if (path == null) {
            return null;
        }

        return path.states
                .stream()
                .filter(s -> s.getBackEdge() instanceof StreetEdge)
                .map(s -> String.format(
                        Locale.ROOT,
                        "%s - %s - %s (%,.2f, %d)",
                        s.getBackMode(),
                        s.getBikeRentalState(),
                        s.getBackEdge() != null ? s.getBackEdge().getName() : null,
                        s.getWeight(),
                        s.getElapsedTimeSeconds()
                ))
                .collect(Collectors.toList());
    }
}
