package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.util.OTPFeature;

/**
 * This tests that the feed for the Cobb County Flex service is processed correctly. This service
 * contains both flex zones but also scheduled stops. Inside the zone passengers can get on or off
 * anywhere so there it works more like a taxi.
 * <p>
 * Read about the details at: https://www.cobbcounty.org/transportation/cobblinc/routes-and-schedules/flex
 */
public class ScheduledDeviatedTripTest extends FlexTest {

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

        assertEquals(3, accesses.size());

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

        assertEquals(3, egresses.size());

        var egress = egresses.get(0);
        assertEquals(2, egress.fromStopIndex);
        assertEquals(2, egress.toStopIndex);
    }

    @Test
    public void calculateDirectFare() {
        OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
        var trip = getFlexTrip();

        var from = getNearbyStop(trip, "from-stop");
        var to = getNearbyStop(trip, "to-stop");

        var router = new FlexRouter(
                graph,
                new FlexParameters(300),
                OffsetDateTime.parse("2021-11-12T10:15:24-05:00").toInstant(),
                false,
                1,
                1,
                List.of(from),
                List.of(to)
        );

        var itineraries = router.createFlexOnlyItineraries();

        var itinerary = itineraries.iterator().next();
        assertFalse(itinerary.fare.fare.isEmpty());

        assertEquals(new Money(new WrappedCurrency("USD"), 250), itinerary.fare.getFare(FareType.regular));

        OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
    }

    @BeforeAll
    static void setup() throws URISyntaxException {
        graph = FlexTest.buildFlexGraph(COBB_FLEX_GTFS);
    }

    private static NearbyStop getNearbyStop(FlexTrip trip) {
        return getNearbyStop(trip, "nearby-stop");
    }

    private static NearbyStop getNearbyStop(FlexTrip trip, String id) {
        // getStops() returns a set of stops and the order doesn't correspond to the stop times
        // of the trip
        var stopLocation = trip.getStops()
                .stream()
                .filter(s -> s instanceof FlexStopLocation)
                .findFirst()
                .get();
        var r = new RoutingRequest();
        r.setRoutingContext(graph);
        return new NearbyStop(
                stopLocation,
                0,
                List.of(),
                null,
                new State(new StreetLocation(id, new Coordinate(0, 0), id), r)
        );
    }

    private static FlexTrip getFlexTrip() {
        var feedId = graph.getFeedIds().iterator().next();
        var tripId = new FeedScopedId(feedId, "a326c618-d42c-4bd1-9624-c314fbf8ecd8");
        return graph.flexTripsById.get(tripId);
    }
}