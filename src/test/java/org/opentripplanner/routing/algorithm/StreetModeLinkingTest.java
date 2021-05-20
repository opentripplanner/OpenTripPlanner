package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;

/**
 * This tests linking of GenericLocations to streets for each StreetMode. The test has 5 parallel
 * streets and a linking is performed for each mode in the middle of each street. Currently linking
 * is handled in three different ways: for CAR, CAR_PARK and everything else, which is reflected in
 * the tests.
 */
public class StreetModeLinkingTest extends GraphRoutingTest {

    private Graph graph;

    @BeforeEach
    protected void setUp() throws Exception {
        graph = graphOf(new GraphRoutingTest.Builder() {
            @Override
            public void build() {
                street(
                        intersection("A1", 47.5000, 19.00),
                        intersection("A2", 47.5020, 19.00),
                        100, StreetTraversalPermission.CAR
                );

                street(
                        intersection("B1", 47.5000, 19.01),
                        intersection("B2", 47.5020, 19.01),
                        100, StreetTraversalPermission.ALL
                );

                street(
                        intersection("C1", 47.5000, 19.02),
                        intersection("C2", 47.5020, 19.02),
                        100, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
                );

                street(
                        intersection("D1", 47.500, 19.03),
                        intersection("D2", 47.502, 19.03),
                        100, StreetTraversalPermission.PEDESTRIAN
                ).setWheelchairAccessible(false);

                street(
                        intersection("E1", 47.500, 19.04),
                        intersection("E2", 47.502, 19.04),
                        100, StreetTraversalPermission.BICYCLE_AND_CAR
                );
            }
        });
    }

    @Test
    public void testCarLinking() {
        assertLinkedFromTo(47.501, 19.00, "A1A2 street", StreetMode.CAR);
        assertLinkedFromTo(47.501, 19.01, "B1B2 street", StreetMode.CAR);
        assertLinkedFromTo(47.501, 19.02, "B1B2 street", StreetMode.CAR);
        assertLinkedFromTo(47.501, 19.03, "E1E2 street", StreetMode.CAR);
        assertLinkedFromTo(47.501, 19.04, "E1E2 street", StreetMode.CAR);
    }

    @Test
    public void testCarParkLinking() {
        var setup =
                (BiFunction<Double, Double, Consumer<RoutingRequest>>) (Double latitude, Double longitude) -> {
                    return (RoutingRequest rr) -> {
                        rr.from = new GenericLocation(latitude, longitude);
                        rr.to = new GenericLocation(latitude, longitude);
                        rr.parkAndRide = true;
                    };
                };

        assertLinking(
                setup.apply(47.501, 19.00), "A1A2 street", "B1B2 street", StreetMode.CAR_TO_PARK);
        assertLinking(
                setup.apply(47.501, 19.01), "B1B2 street", "B1B2 street", StreetMode.CAR_TO_PARK);
        assertLinking(
                setup.apply(47.501, 19.02), "B1B2 street", "C1C2 street", StreetMode.CAR_TO_PARK);
        assertLinking(
                setup.apply(47.501, 19.03), "E1E2 street", "D1D2 street", StreetMode.CAR_TO_PARK);
        assertLinking(
                setup.apply(47.501, 19.04), "E1E2 street", "D1D2 street", StreetMode.CAR_TO_PARK);
    }

    // Only CAR linking is handled specially, since walking with a bike is always a possibility,
    // and so no difference is made between BIKE/WALK:
    @Test
    public void testDefaultLinking() {
        var streetModes = new StreetMode[]{
                StreetMode.WALK,
                StreetMode.BIKE,
                StreetMode.BIKE_TO_PARK,
                StreetMode.BIKE_RENTAL,
                StreetMode.FLEXIBLE,
                StreetMode.CAR_PICKUP,
                StreetMode.CAR_RENTAL
        };

        assertLinkedFromTo(47.501, 19.00, "B1B2 street", streetModes);
        assertLinkedFromTo(47.501, 19.01, "B1B2 street", streetModes);
        assertLinkedFromTo(47.501, 19.02, "C1C2 street", streetModes);
        assertLinkedFromTo(47.501, 19.03, "D1D2 street", streetModes);
        assertLinkedFromTo(47.501, 19.04, "D1D2 street", streetModes);
    }

    // Linking to wheelchair accessible streets is currently not implemented.
    @Test
    @Disabled
    public void testWheelchairLinking() {
        assertLinking((rr) -> {
            rr.from = new GenericLocation(47.5010, 19.03);
            rr.to = new GenericLocation(47.5010, 19.03);
            rr.wheelchairAccessible = true;
        }, "C1C2 street", "C1C2 street", StreetMode.WALK);
    }

    private void assertLinkedFromTo(
            double latitude,
            double longitude,
            String streetName,
            StreetMode... streetModes
    ) {
        assertLinking(
                (rr) -> {
                    rr.from = new GenericLocation(latitude, longitude);
                    rr.to = new GenericLocation(latitude, longitude);
                },
                streetName,
                streetName,
                streetModes
        );
    }

    private void assertLinking(
            Consumer<RoutingRequest> consumer,
            String fromStreetName,
            String toStreetName,
            StreetMode... streetModes
    ) {
        for (final StreetMode streetMode : streetModes) {
            try (var routingRequest = new RoutingRequest().getStreetSearchRequest(streetMode)) {
                consumer.accept(routingRequest);

                routingRequest.setRoutingContext(graph);

                if (fromStreetName != null) {
                    assertFromLink(fromStreetName, streetMode, routingRequest);
                }

                if (toStreetName != null) {
                    assertToLink(toStreetName, streetMode, routingRequest);
                }
            }
        }
    }

    private void assertFromLink(
            String streetName,
            StreetMode streetMode,
            RoutingRequest routingRequest
    ) {
        var fromVertex = routingRequest.rctx.fromVertices.iterator().next();
        var outgoing = fromVertex.getOutgoing()
                .iterator()
                .next()
                .getToVertex()
                .getOutgoing()
                .iterator()
                .next();
        assertEquals(
                streetName,
                outgoing.getName(),
                String.format("%s should be linked to %s", streetMode, streetName)
        );
    }

    private void assertToLink(
            String streetName,
            StreetMode streetMode,
            RoutingRequest routingRequest
    ) {
        var toVertex = routingRequest.rctx.toVertices.iterator().next();
        var outgoing = toVertex.getIncoming()
                .iterator()
                .next()
                .getFromVertex()
                .getIncoming()
                .iterator()
                .next();

        assertEquals(
                streetName,
                outgoing.getName(),
                streetMode + " should be linked to " + streetName
        );
    }
}