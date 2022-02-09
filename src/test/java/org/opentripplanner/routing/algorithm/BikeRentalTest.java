package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
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
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;
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
    private TemporaryStreetLocation T1, T2;
    private TransitEntranceVertex E1;
    private StreetVertex A, B, C, D;
    private VehicleRentalStationVertex B1, B2;
    private StreetEdge SE1, SE2, SE3;

    private String NON_NETWORK = "non network";

    @BeforeEach
    public void setUp() {
        // Generate a very simple graph
        //
        //   A <-> B <-> C <-> D
        //   A <-> S1
        //   B <-> B1
        //   C <-> B2
        //   D <-> E1
        //   D <-> T2

        graph = graphOf(new Builder() {
            @Override
            public void build() {
                S1 = stop("S1", 47.500, 19.001);
                A = intersection("A", 47.500, 19.000);
                B = intersection("B", 47.510, 19.000);
                C = intersection("C", 47.520, 19.000);
                D = intersection("D", 47.530, 19.000);
                E1 = entrance("E1", 47.530, 19.001);

                T1 = streetLocation("T1", 47.500, 18.999, false);
                T2 = streetLocation("T1", 47.530, 18.999, true);

                B1 = vehicleRentalStation("B1", 47.510, 19.001);
                B2 = vehicleRentalStation("B2", 47.520, 19.001);

                biLink(A, S1);
                biLink(D, E1);

                biLink(B, B1);
                biLink(C, B2);

                link(T1, A);
                link(D, T2);

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

        assertEquals(List.of("WALK - null - BC street (3,759.40, 752)"), descriptor);
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
                "WALK - HAVE_RENTED - CD street (650.38, 333)",
                "null - HAVE_RENTED - E1 (651.38, 333)"
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
                        "WALK - BEFORE_RENTING - CD street (1,655.14, 828)",
                        "null - BEFORE_RENTING - E1 (1,656.14, 828)"
                ),
                List.of(
                        "WALK - HAVE_RENTED - AB street (76.19, 38)",
                        "WALK - HAVE_RENTED - BC street (1,579.95, 790)",
                        "WALK - HAVE_RENTED - CD street (1,655.14, 828)",
                        "null - HAVE_RENTED - E1 (1,656.14, 828)"
                )
        );
    }

    @Test
    public void testNoBikesAvailable() {
        ((VehicleRentalStation) B1.getStation()).vehiclesAvailable = 0;

        assertPath(
                S1, E1, true,
                List.of(
                        "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                        "WALK - BEFORE_RENTING - BC street (1,579.95, 790)",
                        "WALK - BEFORE_RENTING - CD street (1,655.14, 828)",
                        "null - BEFORE_RENTING - E1 (1,656.14, 828)"
                ),
                List.of(
                        "WALK - HAVE_RENTED - AB street (76.19, 38)",
                        "WALK - HAVE_RENTED - BC street (1,579.95, 790)",
                        "WALK - HAVE_RENTED - CD street (1,655.14, 828)",
                        "null - HAVE_RENTED - E1 (1,656.14, 828)"
                )
        );
    }

    @Test
    public void testNoSpacesAvailable() {
        ((VehicleRentalStation) B2.getStation()).spacesAvailable = 0;

        assertPath(
                S1, E1, true,
                List.of(
                        "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                        "WALK - BEFORE_RENTING - BC street (1,579.95, 790)",
                        "WALK - BEFORE_RENTING - CD street (1,655.14, 828)",
                        "null - BEFORE_RENTING - E1 (1,656.14, 828)"
                ),
                List.of(
                        "WALK - HAVE_RENTED - AB street (76.19, 38)",
                        "WALK - HAVE_RENTED - BC street (1,579.95, 790)",
                        "WALK - HAVE_RENTED - CD street (1,655.14, 828)",
                        "null - HAVE_RENTED - E1 (1,656.14, 828)"
                )
        );
    }

    @Test
    public void testIgnoreAvailabilityNoBikesAvailable() {
        ((VehicleRentalStation) B1.getStation()).vehiclesAvailable = 0;

        assertPath(
                S1, E1, false,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FROM_STATION - BC street (540.19, 280)",
                "WALK - HAVE_RENTED - CD street (650.38, 333)",
                "null - HAVE_RENTED - E1 (651.38, 333)"
        );
    }

    @Test
    public void testIgnoreAvailabilityNoSpacesAvailable() {
        ((VehicleRentalStation) B2.getStation()).spacesAvailable = 0;

        assertPath(
                S1, E1, false,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FROM_STATION - BC street (540.19, 280)",
                "WALK - HAVE_RENTED - CD street (650.38, 333)",
                "null - HAVE_RENTED - E1 (651.38, 333)"
        );
    }

    @Test
    public void testFloatingBike() {
        VehicleRentalPlace station = B1.getStation();
        VehicleRentalVehicle vehicle = new VehicleRentalVehicle();
        vehicle.latitude = station.getLatitude();
        vehicle.longitude = station.getLongitude();
        vehicle.id = station.getId();
        vehicle.name = station.getName();
        vehicle.vehicleType = RentalVehicleType.getDefaultType(station.getId().getFeedId());
        B1.setStation(vehicle);

        assertPath(
                S1, E1,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FLOATING - BC street (540.19, 280)",
                "BICYCLE - RENTING_FLOATING - CD street (560.19, 290)",
                "null - RENTING_FLOATING - E1 (561.19, 290)"
        );

        assertPath(
                S1, T2,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FLOATING - BC street (540.19, 280)",
                "BICYCLE - RENTING_FLOATING - CD street (560.19, 290)",
                "null - RENTING_FLOATING - null (561.19, 290)"
        );

        assertPath(
                T1, E1,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FLOATING - BC street (540.19, 280)",
                "BICYCLE - RENTING_FLOATING - CD street (560.19, 290)",
                "null - RENTING_FLOATING - E1 (561.19, 290)"
        );
    }

    @Test
    public void testBikeRentalFromStationWantToKeepCantKeep() {
        ((VehicleRentalStation) B1.getStation()).isKeepingVehicleRentalAtDestinationAllowed = false;

        assertPath(
                S1, E1, 40,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FROM_STATION - BC street (540.19, 280)",
                "WALK - HAVE_RENTED - CD street (650.38, 333)",
                "null - HAVE_RENTED - E1 (651.38, 333)"
        );

        assertPath(
                S1, T2, 40,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FROM_STATION - BC street (540.19, 280)",
                "WALK - HAVE_RENTED - CD street (650.38, 333)",
                "null - HAVE_RENTED - null (651.38, 333)"
        );

        assertPath(
                T1, E1, 40,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FROM_STATION - BC street (540.19, 280)",
                "WALK - HAVE_RENTED - CD street (650.38, 333)",
                "null - HAVE_RENTED - E1 (651.38, 333)"
        );
    }

    @Test
    public void testBikeRentalFromStationWantToKeepCanKeep() {
        ((VehicleRentalStation) B1.getStation()).isKeepingVehicleRentalAtDestinationAllowed = true;

        assertPath(
                S1, E1, 40,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FROM_STATION (may keep) - BC street (540.19, 280)",
                "BICYCLE - RENTING_FROM_STATION (may keep) - CD street (560.19, 290)",
                "null - RENTING_FROM_STATION (may keep) - E1 (601.19, 290)"
        );

        assertPath(
                S1, T2, 40,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FROM_STATION (may keep) - BC street (540.19, 280)",
                "BICYCLE - RENTING_FROM_STATION (may keep) - CD street (560.19, 290)",
                "null - RENTING_FROM_STATION (may keep) - null (601.19, 290)"
        );

        assertPath(
                T1, E1, 40,
                "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                "BICYCLE - RENTING_FROM_STATION (may keep) - BC street (540.19, 280)",
                "BICYCLE - RENTING_FROM_STATION (may keep) - CD street (560.19, 290)",
                "null - RENTING_FROM_STATION (may keep) - E1 (601.19, 290)"
        );
    }

    @Test
    public void testBikeRentalFromStationWantToKeepCanKeepButCostly() {
        ((VehicleRentalStation) B1.getStation()).isKeepingVehicleRentalAtDestinationAllowed = true;
        int keepRentedBicycleAtDestinationCost = 1000;

        assertPath(
                S1, E1, false, keepRentedBicycleAtDestinationCost,
                List.of(
                        "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                        "BICYCLE - RENTING_FROM_STATION (may keep) - BC street (540.19, 280)",
                        "WALK - HAVE_RENTED - CD street (650.38, 333)",
                        "null - HAVE_RENTED - E1 (651.38, 333)"
                ),
                List.of(
                        "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                        "BICYCLE - RENTING_FROM_STATION - BC street (540.19, 280)",
                        "WALK - HAVE_RENTED - CD street (650.38, 333)",
                        "null - HAVE_RENTED - E1 (651.38, 333)"
                )
        );

        assertPath(
                S1, T2, false, keepRentedBicycleAtDestinationCost,
                List.of(
                        "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                        "BICYCLE - RENTING_FROM_STATION (may keep) - BC street (540.19, 280)",
                        "WALK - HAVE_RENTED - CD street (650.38, 333)",
                        "null - HAVE_RENTED - null (651.38, 333)"
                ),
                List.of(
                        "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                        "BICYCLE - RENTING_FROM_STATION - BC street (540.19, 280)",
                        "WALK - HAVE_RENTED - CD street (650.38, 333)",
                        "null - HAVE_RENTED - null (651.38, 333)"
                )
        );

        assertPath(
                T1, E1, false, keepRentedBicycleAtDestinationCost,
                List.of(
                        "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                        "BICYCLE - RENTING_FROM_STATION (may keep) - BC street (540.19, 280)",
                        "WALK - HAVE_RENTED - CD street (650.38, 333)",
                        "null - HAVE_RENTED - E1 (651.38, 333)"
                ),
                List.of(
                        "WALK - BEFORE_RENTING - AB street (76.19, 38)",
                        "BICYCLE - RENTING_FROM_STATION - BC street (540.19, 280)",
                        "WALK - HAVE_RENTED - CD street (650.38, 333)",
                        "null - HAVE_RENTED - E1 (651.38, 333)"
                )
        );
    }

    @Test
    public void noPathIfNoAllowedNetworks() {
        assertNoRental(B, C, Set.of(), Set.of(NON_NETWORK));
    }

    @Test
    public void noPathIfBannedAndAllowedNetwork() {
        assertNoRental(B, C, Set.of(TEST_VEHICLE_RENTAL_NETWORK), Set.of(TEST_VEHICLE_RENTAL_NETWORK));
    }

    @Test
    public void pathIfWithOnlyAllowedNetworks() {
        assertPathWithNetwork(B, C, Set.of(), Set.of(TEST_VEHICLE_RENTAL_NETWORK), Set.of(TEST_VEHICLE_RENTAL_NETWORK));
    }

    @Test
    public void noPathIfNetworkIsBanned() {
        assertNoRental(B, C, Set.of(TEST_VEHICLE_RENTAL_NETWORK), Set.of());
    }

    @Test
    public void pathIfWithoutBannedNetworks() {
        assertPathWithNetwork(B, C, Set.of(NON_NETWORK), Set.of(), Set.of(TEST_VEHICLE_RENTAL_NETWORK));
    }

    private void assertNoRental(StreetVertex fromVertex, StreetVertex toVertex, Set<String> bannedNetworks, Set<String> allowedNetworks) {
        Consumer<RoutingRequest> setter = options -> {
            options.allowedVehicleRentalNetworks = allowedNetworks;
            options.bannedVehicleRentalNetworks = bannedNetworks;
        };

        assertEquals(
                List.of(
                        "WALK - BEFORE_RENTING - BC street (1,503.76, 752)"
                ),
                runStreetSearchAndCreateDescriptor(fromVertex, toVertex, false, setter),
                "departAt"
        );

        assertEquals(
                List.of(
                        "WALK - HAVE_RENTED - BC street (1,503.76, 752)"
                ),
                runStreetSearchAndCreateDescriptor(fromVertex, toVertex, true, setter),
                "arriveBy"
        );
    }

    private void assertPathWithNetwork(StreetVertex fromVertex, StreetVertex toVertex, Set<String> bannedNetworks, Set<String> allowedNetworks, Set<String> usedNetworks) {
        Consumer<RoutingRequest> setter = options -> {
            options.allowedVehicleRentalNetworks = allowedNetworks;
            options.bannedVehicleRentalNetworks = bannedNetworks;
        };

        assertEquals(
                List.of(
                        "BICYCLE - RENTING_FROM_STATION - BC street (464.00, 242)",
                        "null - HAVE_RENTED - B2 (499.00, 257)"
                ),
                runStreetSearchAndCreateDescriptor(fromVertex, toVertex, false, setter),
                "departAt"
        );

        assertEquals(
                List.of(
                        "BICYCLE - RENTING_FROM_STATION - BC street (464.00, 242)",
                        "null - HAVE_RENTED - B2 (499.00, 257)"
                ),
                runStreetSearchAndCreateDescriptor(fromVertex, toVertex, true, setter),
                "arriveBy"
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
            int keepRentedBicycleCost,
            String... descriptor
    ) {
        assertPath(
                fromVertex, toVertex, false, keepRentedBicycleCost, List.of(descriptor),
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
                useAvailabilityInformation, 0
        );
        List<String> arriveBy = runStreetSearchAndCreateDescriptor(fromVertex, toVertex, true,
                useAvailabilityInformation, 0
        );

        assertEquals(departAtDescriptor, departAt, "departAt path");
        assertEquals(arriveByDescriptor, arriveBy, "arriveBy path");
    }

    private void assertPath(
            Vertex fromVertex,
            Vertex toVertex,
            boolean useAvailabilityInformation,
            int keepBicycleRentalCost,
            List<String> departAtDescriptor,
            List<String> arriveByDescriptor
    ) {
        List<String> departAt = runStreetSearchAndCreateDescriptor(fromVertex, toVertex, false,
                useAvailabilityInformation, keepBicycleRentalCost
        );
        List<String> arriveBy = runStreetSearchAndCreateDescriptor(fromVertex, toVertex, true,
                useAvailabilityInformation, keepBicycleRentalCost
        );

        assertEquals(departAtDescriptor, departAt, "departAt path");
        assertEquals(arriveByDescriptor, arriveBy, "arriveBy path");
    }

    private List<String> runStreetSearchAndCreateDescriptor(
            Vertex fromVertex,
            Vertex toVertex,
            boolean arriveBy,
            boolean useAvailabilityInformation,
            int keepRentedBicycleCost
    ) {
        return runStreetSearchAndCreateDescriptor(fromVertex, toVertex, arriveBy, options -> {
            options.useVehicleRentalAvailabilityInformation = useAvailabilityInformation;
            options.allowKeepingRentedVehicleAtDestination = keepRentedBicycleCost > 0;
            options.keepingRentedVehicleAtDestinationCost = keepRentedBicycleCost;
        });
    }

    private List<String> runStreetSearchAndCreateDescriptor(
            Vertex fromVertex,
            Vertex toVertex,
            boolean arriveBy,
            Consumer<RoutingRequest> optionsSetter
    ) {
        var options = new RoutingRequest();
        options.arriveBy = arriveBy;
        options.vehicleRentalPickupTime = 42;
        options.vehicleRentalPickupCost = 62;
        options.vehicleRentalDropoffCost = 33;
        options.vehicleRentalDropoffTime = 15;

        optionsSetter.accept(options);

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
                .filter(s -> s.getBackEdge() instanceof StreetEdge || s.getVertex() == toVertex)
                .map(s -> String.format(
                        Locale.ROOT,
                        "%s - %s%s - %s (%,.2f, %d)",
                        s.getBackMode(),
                        s.getVehicleRentalState(),
                        s.mayKeepRentedVehicleAtDestination() ? " (may keep)" : "",
                        s.getBackEdge() != null ? s.getBackEdge().getDefaultName() : null,
                        s.getWeight(),
                        s.getElapsedTimeSeconds()
                ))
                .collect(Collectors.toList());
    }
}
