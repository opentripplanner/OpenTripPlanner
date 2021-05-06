package org.opentripplanner.model.impl;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContextBuilder;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;

public class OtpTransitServiceImplTest {
    private static final String FEED_ID = "Z";

    private static final FeedScopedId STATION_ID = new FeedScopedId(FEED_ID, "station");

    // The subject is used as read only; hence static is ok
    private static OtpTransitService subject;


    @BeforeClass
    public static void setup() throws IOException {
        GtfsContextBuilder contextBuilder = contextBuilder(FEED_ID, ConstantsForTests.FAKE_GTFS);
        OtpTransitServiceBuilder builder = contextBuilder.getTransitBuilder();

        // Supplement test data with at least one entity in all collections
        FareRule rule = createFareRule();
        builder.getFareAttributes().add(rule.getFare());
        builder.getFareRules().add(rule);
        builder.getFeedInfos().add(FeedInfo.dummyForTest(FEED_ID));

        subject = builder.build();
    }

    @Test
    public void testGetAllAgencies() {
        Collection<Agency> agencies = subject.getAllAgencies();
        Agency agency = first(agencies);

        assertEquals(1, agencies.size());
        assertEquals("agency", agency.getId().getId());
        assertEquals("Fake Agency", agency.getName());
    }

    @Test
    public void testGetAllFareAttributes() {
        Collection<FareAttribute> fareAttributes = subject.getAllFareAttributes();

        assertEquals(1, fareAttributes.size());
        assertEquals("<FareAttribute Z:FA>", first(fareAttributes).toString());
    }

    @Test
    public void testGetAllFareRules() {
        Collection<FareRule> fareRules = subject.getAllFareRules();

        assertEquals(1, fareRules.size());
        assertEquals(
                "<FareRule  origin='Zone A' contains='Zone B' destination='Zone C'>",
                first(fareRules).toString()
        );
    }

    @Test
    public void testGetAllFeedInfos() {
        Collection<FeedInfo> feedInfos = subject.getAllFeedInfos();

        assertEquals(1, feedInfos.size());
        assertEquals("<FeedInfo Z>", first(feedInfos).toString());
    }

    @Test
    public void testGetAllPathways() {
        Collection<Pathway> pathways = subject.getAllPathways();

        assertEquals(3, pathways.size());
        assertEquals("<Pathway Z:pathways_1_1>", first(pathways).toString());
    }

    @Test
    public void testGetAllTransfers() {
        var result = removeFeedScope(
                subject.getAllTransfers()
                        .stream()
                        .map(Object::toString)
                        .sorted()
                        .collect(joining("\n"))
        );

        // There is 9 transfers, but because of the route to trip we get more
        // TODO TGR - Support Route to trip expansion
        assertEquals(
                //"Transfer{from: (route: 2, trip: 2.1, stopPos: 2), to: (route: 5, trip: 5.1, stopPos: 0), guaranteed}\n"
                //+ "Transfer{from: (route: 2, trip: 2.2, stopPos: 2), to: (route: 5, trip: 5.1, stopPos: 0), guaranteed}\n"
                "Transfer{from: (stop: K), to: (stop: L), priority: RECOMMENDED}\n"
                + "Transfer{from: (stop: K), to: (stop: M), priority: NOT_ALLOWED}\n"
                + "Transfer{from: (stop: L), to: (stop: K), priority: RECOMMENDED}\n"
                + "Transfer{from: (stop: M), to: (stop: K), priority: NOT_ALLOWED}\n"
                + "Transfer{from: (trip: 1.1, stopPos: 1), to: (trip: 2.2, stopPos: 0), guaranteed}",
                result
        );
    }

    @Test
    public void testGetAllStations() {
        Collection<Station> stations = subject.getAllStations();

        assertEquals(1, stations.size());
        assertEquals("<Station Z:station>", first(stations).toString());
    }

    @Test
    public void testGetAllStops() {
        Collection<Stop> stops = subject.getAllStops();

        assertEquals(22, stops.size());
        assertEquals("<Stop Z:A>", first(stops).toString());
    }

    @Test
    public void testGetAllStopTimes() {
        List<StopTime> stopTimes = new ArrayList<>();
        for (Trip trip : subject.getAllTrips()) {
            stopTimes.addAll(subject.getStopTimesForTrip(trip));
        }

        assertEquals(80, stopTimes.size());
        assertEquals("StopTime(seq=1 stop=Z:A trip=agency:1.1 times=00:00:00-00:00:00)",
                first(stopTimes).toString());
    }

    @Test
    public void testGetAllTrips() {
        Collection<Trip> trips = subject.getAllTrips();

        assertEquals(33, trips.size());
        assertEquals("<Trip agency:1.1>", first(trips).toString());
    }

    @Test
    public void testGetStopForId() {
        Stop stop = subject.getStopForId(new FeedScopedId("Z", "P"));
        assertEquals("<Stop Z:P>", stop.toString());
    }

    @Test
    public void testGetStopsForStation() {
        List<Stop> stops = new ArrayList<>(subject.getStationForId(STATION_ID).getChildStops());
        assertEquals("[<Stop Z:A>]", stops.toString());
    }

    @Test
    public void testGetShapePointsForShapeId() {
        List<ShapePoint> shapePoints = subject.getShapePointsForShapeId(new FeedScopedId("Z", "5"));
        assertEquals("[#1 (41,-72), #2 (41,-72), #3 (40,-72), #4 (41,-73), #5 (41,-74)]",
                shapePoints.stream().map(OtpTransitServiceImplTest::toString).collect(toList()).toString());
    }

    @Test
    public void testGetStopTimesForTrip() {
        List<StopTime> stopTimes = subject.getStopTimesForTrip(first(subject.getAllTrips()));
        assertEquals("[<Stop Z:A>, <Stop Z:B>, <Stop Z:C>]",
                stopTimes.stream().map(StopTime::getStop).collect(toList()).toString());
    }

    @Test
    public void testGetAllServiceIds() {
        Collection<FeedScopedId> serviceIds = subject.getAllServiceIds();

        assertEquals(2, serviceIds.size());
        assertEquals("Z:alldays", first(serviceIds).toString());
    }

    private static FareRule createFareRule() {
        FareAttribute fa = new FareAttribute(new FeedScopedId(FEED_ID, "FA"));
        FareRule rule = new FareRule();
        rule.setOriginId("Zone A");
        rule.setContainsId("Zone B");
        rule.setDestinationId("Zone C");
        rule.setFare(fa);
        return rule;
    }

    private static String removeFeedScope(String text) {
        return text.replace("agency:", "").replace("Z:", "");
    }

    private static <T> List<T> sort(Collection<? extends T> c) {
        return c.stream().sorted(comparing(T::toString)).collect(toList());
    }

    private static <T> T first(Collection<? extends T> c) {
        //noinspection ConstantConditions
        return c.stream().sorted(comparing(T::toString)).findFirst().get();
    }

    private static String toString(ShapePoint sp) {
        int lat = (int) sp.getLat();
        int lon = (int) sp.getLon();
        return "#" + sp.getSequence() + " (" + lat + "," + lon + ")";
    }
}