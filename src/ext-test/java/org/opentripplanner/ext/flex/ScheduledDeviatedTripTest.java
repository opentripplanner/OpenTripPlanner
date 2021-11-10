package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;

/**
 * This tests that the feed for the Cobb County Flex service is processed correctly. This service
 * contains both flex zones but also scheduled stops. Inside the zone passengers can get on or off
 * anywhere so there it works more like a taxi.
 * <p>
 * Read about the details at: https://www.cobbcounty.org/transportation/cobblinc/routes-and-schedules/flex
 */
public class ScheduledDeviatedTripTest extends FlexTest {

    static final String COBB_COUNTY_GTFS = "/flex/cobblinc-scheduled-deviated-flex.gtfs.zip";

    static Graph graph;

    @Test
    public void parseCobbCountyAsScheduledDeviatedTrip() {
        var flexTrips = graph.flexTripsById.values();
        assertFalse(flexTrips.isEmpty());
        assertEquals(72, flexTrips.size());

        assertEquals(
                Set.of(ScheduledDeviatedTrip.class),
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

        assertEquals(2, accesses.size());

        var access = accesses.get(0);
        assertEquals(1, access.fromStopIndex);
        assertEquals(1, access.toStopIndex);

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

        assertEquals(2, egresses.size());

        var egress = egresses.get(0);
        assertEquals(1, egress.fromStopIndex);
        assertEquals(1, egress.toStopIndex);
    }

    @BeforeAll
    static void setup() throws URISyntaxException {
        graph = FlexTest.buildFlexGraph(COBB_COUNTY_GTFS);
    }

    private static NearbyStop getNearbyStop(FlexTrip trip) {
        var stopLocation = trip.getStops().stream().collect(Collectors.toList()).get(2);
        assertTrue(stopLocation instanceof FlexStopLocation);
        return new NearbyStop(stopLocation, 0, List.of(), null, null);
    }

    private static FlexTrip getFlexTrip() {
        var flexTrips = graph.flexTripsById.values();
        return flexTrips.iterator().next();
    }
}