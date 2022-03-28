package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.PolylineAssert.assertThatPolylinesAreEqual;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.algorithm.raptoradapter.router.TransitRouter;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.OTPFeature;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.TestUtils;
import org.opentripplanner.util.model.EncodedPolylineBean;

/**
 * This tests that the feed for the Cobb County Flex service is processed correctly. This service
 * contains both flex zones but also scheduled stops. Inside the zone passengers can get on or off
 * anywhere so there it works more like a taxi.
 * <p>
 * Read about the details at: https://www.cobbcounty.org/transportation/cobblinc/routes-and-schedules/flex
 */
public class ScheduledDeviatedTripTest extends FlexTest {

    static Graph graph;

    float delta = 0.01f;

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

        var trip = getFlexTrip();
        System.out.println(trip.getStops().stream().map(s -> s.getId().getId()).collect(Collectors.toList()));
        var stop = trip.getStops().stream().filter(s -> s.getId().getId().equals("cujv")).findFirst().get();
        assertEquals(33.85465, stop.getLat(), delta);
        assertEquals(-84.60039, stop.getLon(), delta);

        var flexZone = trip.getStops().stream().filter(s -> s.getId().getId().equals("zone_3")).findFirst().get();
        assertEquals(33.825846635310214, flexZone.getLat(), delta);
        assertEquals(-84.63430143459385, flexZone.getLon(), delta);
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

        assertEquals(
                new Money(new WrappedCurrency("USD"), 250),
                itinerary.fare.getFare(FareType.regular)
        );

        OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
    }


    /**
     * Trips which consist of flex and fixed-schedule stops should work in transit mode.
     * <p>
     * The flex stops will show up as intermediate stops (without a departure/arrival time) but you
     * cannot board or alight.
     */
    @Test
    public void flexTripInTransitMode() {
        var feedId = graph.getFeedIds().iterator().next();

        var router = new Router(graph, RouterConfig.DEFAULT);
        router.startup();

        // from zone 3 to zone 2
        var from = GenericLocation.fromStopId(
                "Transfer Point for Route 30",
                feedId,
                "cujv"
        );
        var to = GenericLocation.fromStopId(
                "Zone 1 - PUBLIX Super Market,Zone 1 Collection Point",
                feedId,
                "yz85"
        );

        var itineraries = getItineraries(from, to, router);

        assertEquals(2, itineraries.size());

        var itin = itineraries.get(0);
        var leg = itin.legs.get(0);

        assertEquals("cujv", leg.getFrom().stop.getId().getId());
        assertEquals("yz85", leg.getTo().stop.getId().getId());

        var intermediateStops = leg.getIntermediateStops();
        assertEquals(1, intermediateStops.size());
        assertEquals("zone_1", intermediateStops.get(0).place.stop.getId().getId());

        EncodedPolylineBean legGeometry = PolylineEncoder.createEncodings(leg.getLegGeometry());
        assertThatPolylinesAreEqual(
                legGeometry.getPoints(),
                "kfsmEjojcOa@eBRKfBfHR|ALjBBhVArMG|OCrEGx@OhAKj@a@tAe@hA]l@MPgAnAgw@nr@cDxCm@t@c@t@c@x@_@~@]pAyAdIoAhG}@lE{AzHWhAtt@t~Aj@tAb@~AXdBHn@FlBC`CKnA_@nC{CjOa@dCOlAEz@E|BRtUCbCQ~CWjD??qBvXBl@kBvWOzAc@dDOx@sHv]aIG?q@@c@ZaB\\mA"
        );

    }

    /**
     * We add flex trips, that can potentially not have a departure and arrival time, to the trip.
     * <p>
     * Normally these trip times are interpolated/repaired during the graph build but for flex this
     * is exactly what we don't want. Here we check that the interpolation process is skipped.
     *
     * @see org.opentripplanner.gtfs.RepairStopTimesForEachTripOperation#interpolateStopTimes(List)
     */
    @Test
    public void shouldNotInterpolateFlexTimes() {
        var feedId = graph.getFeedIds().iterator().next();
        var pattern = graph.tripPatternForId.get(new FeedScopedId(feedId, "090z:0:01"));

        assertEquals(3, pattern.numberOfStops());

        var tripTimes = pattern.getScheduledTimetable().getTripTimes(0);
        var arrivalTime = tripTimes.getArrivalTime(1);

        assertEquals(StopTime.MISSING_VALUE, arrivalTime);
    }

    /**
     * Checks that trips which have continuous pick up/drop off set are parsed correctly.
     */
    @Test
    public void parseContinuousPickup() {
        var lincolnGraph = FlexTest.buildFlexGraph(LINCOLN_COUNTY_GBFS);
        assertNotNull(lincolnGraph);
    }

    private static List<Itinerary> getItineraries(
            GenericLocation from,
            GenericLocation to,
            Router router
    ) {
        RoutingRequest request = new RoutingRequest();
        Instant dateTime = TestUtils.dateInstant("America/New_York", 2021, 12, 16, 12, 0, 0);
        request.setDateTime(dateTime);
        request.from = from;
        request.to = to;

        var time = dateTime.atZone(ZoneId.of("America/New_York"));
        var additionalSearchDays = AdditionalSearchDays.defaults(time);

        var result = TransitRouter.route(
                request,
                router,
                time,
                additionalSearchDays,
                new DebugTimingAggregator()
        );

        return result.getItineraries();
    }

    @BeforeAll
    static void setup() {
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