package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;

/**
 * This test makes sure that one of the example feeds in the GTFS-Flex repo works. It's the City of
 * Aspen Downtown taxi service which is a completely unscheduled trip that takes you door-to-door in
 * the city.
 * <p>
 * It only contains a single stop time which in GTFS static would not work but is valid in GTFS
 * Flex.
 */
public class UnscheduledTripTest extends FlexTest {

    static Graph graph;

    @Test
    public void parseAspenTaxiAsUnscheduledTrip() {
        var flexTrips = graph.flexTripsById.values();
        assertFalse(flexTrips.isEmpty());
        assertEquals(
                Set.of("t_1289262_b_29084_tn_0", "t_1289257_b_28352_tn_0"),
                flexTrips.stream().map(FlexTrip::getId).map(FeedScopedId::getId).collect(
                        Collectors.toSet())
        );

        assertEquals(
                Set.of(UnscheduledTrip.class),
                flexTrips.stream().map(FlexTrip::getClass).collect(
                        Collectors.toSet())
        );
    }

    @Test
    public void calculateAccessTemplate() {
        var trip = getFlexTrip();
        var nearbyStop = getNearbyStop(trip);

        var accesses = trip.getFlexAccessTemplates(
                nearbyStop,
                flexDate,
                calculator,
                params
        ).collect(Collectors.toList());

        assertEquals(1, accesses.size());

        var access = accesses.get(0);
        assertEquals(0, access.fromStopIndex);
        assertEquals(0, access.toStopIndex);

    }

    @Test
    public void calculateEgressTemplate() {
        var trip = getFlexTrip();
        var nearbyStop = getNearbyStop(trip);
        var egresses = trip.getFlexEgressTemplates(
                nearbyStop,
                flexDate,
                calculator,
                params
        ).collect(Collectors.toList());

        assertEquals(1, egresses.size());

        var egress = egresses.get(0);
        assertEquals(0, egress.fromStopIndex);
        assertEquals(0, egress.toStopIndex);
    }

    @BeforeAll
    static void setup() {
        graph = FlexTest.buildFlexGraph(ASPEN_GTFS);
    }

    private static NearbyStop getNearbyStop(FlexTrip trip) {
        assertEquals(1, trip.getStops().size());
        var stopLocation = trip.getStops().iterator().next();
        return new NearbyStop(stopLocation, 0, List.of(), null, null);
    }

    private static FlexTrip getFlexTrip() {
        var flexTrips = graph.flexTripsById.values();
        return flexTrips.iterator().next();
    }
}