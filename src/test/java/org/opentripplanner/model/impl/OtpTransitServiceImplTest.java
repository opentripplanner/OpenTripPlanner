package org.opentripplanner.model.impl;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ServiceCalendar;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.mapping.GTFSToOtpTransitServiceMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public class OtpTransitServiceImplTest {
    private static final String FEED_ID = "Z";

    private static final FeedScopedId SERVICE_ALLDAYS_ID = new FeedScopedId(FEED_ID, "alldays");

    private static final FeedScopedId SERVICE_WEEKDAYS_ID = new FeedScopedId(FEED_ID, "weekdays");

    private static final FeedScopedId STATION_ID = new FeedScopedId(FEED_ID, "station");

    // The subject is used as read only; hence static is ok
    private static OtpTransitService subject;

    private static Agency agency;

    @BeforeClass
    public static void setup() throws IOException {
        org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl dao = new org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl();

        org.onebusaway.gtfs.serialization.GtfsReader reader = new org.onebusaway.gtfs.serialization.GtfsReader();

        reader.setInputLocation(new File(ConstantsForTests.FAKE_GTFS));
        reader.setEntityStore(dao);
        reader.setDefaultAgencyId(FEED_ID);
        reader.run();

        OtpTransitServiceBuilder builder = new OtpTransitServiceBuilder(GTFSToOtpTransitServiceMapper.mapGtfsDaoToOTPTransitService(dao));
        agency = first(builder.getAgencies());

        // Supplement test data with at least one entity in all collections
        builder.getCalendarDates().add(createAServiceCalendarDateExclution(SERVICE_WEEKDAYS_ID));
        FareRule rule = createFareRule();
        builder.getFareAttributes().add(rule.getFare());
        builder.getFareRules().add(rule);
        builder.getFeedInfos().add(new FeedInfo());

        subject = builder.build();
    }

    @Test
    public void testGetAllAgencies() {
        Collection<Agency> agencies = subject.getAllAgencies();
        Agency agency = first(agencies);

        assertEquals(1, agencies.size());
        assertEquals("agency", agency.getId());
        assertEquals("Fake Agency", agency.getName());
    }

    @Test
    public void testGetAllCalendarDates() {
        Collection<ServiceCalendarDate> calendarDates = subject.getAllCalendarDates();

        assertEquals(1, calendarDates.size());
        assertEquals(
                "<CalendarDate serviceId=Z_weekdays date=ServiceIdDate(2017-8-31) exception=2>",
                first(calendarDates).toString());
    }

    @Test
    public void testGetAllCalendars() {
        Collection<ServiceCalendar> calendars = subject.getAllCalendars();

        assertEquals(2, calendars.size());
        assertEquals("<ServiceCalendar Z_alldays [1111111]>", first(calendars).toString());
    }

    @Test
    public void testGetAllFareAttributes() {
        Collection<FareAttribute> fareAttributes = subject.getAllFareAttributes();

        assertEquals(1, fareAttributes.size());
        assertEquals("<FareAttribute agency_FA>", first(fareAttributes).toString());
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
        assertEquals("<FeedInfo 1>", first(feedInfos).toString());
    }

    @Test
    public void testGetAllFrequencies() {
        Collection<Frequency> frequencies = subject.getAllFrequencies();

        assertEquals(2, frequencies.size());
        assertEquals(
                "<Frequency trip=agency_15.1 start=06:00:00 end=10:00:01>",
                first(frequencies).toString()
        );
    }

    @Test
    public void testGetAllPathways() {
        Collection<Pathway> pathways = subject.getAllPathways();

        assertEquals(4, pathways.size());
        assertEquals("<Pathway Z_pathways_1_1>", first(pathways).toString());
    }

    @Test
    public void testGetAllRoutes() {
        Collection<Route> routes = subject.getAllRoutes();

        assertEquals(18, routes.size());
        assertEquals("<Route agency_1 1>", first(routes).toString());
    }

    @Test
    public void testGetAllTransfers() {
        Collection<Transfer> transfers = subject.getAllTransfers();

        assertEquals(9, transfers.size());
        assertEquals("<Transfer stop=Z_F..Z_E>", first(transfers).toString());
    }

    @Test
    public void testGetAllShapePoints() {
        Collection<ShapePoint> shapePoints = subject.getAllShapePoints();

        assertEquals(9, shapePoints.size());
        assertEquals("<ShapePoint Z_4 #1 (41.0,-75.0)>", first(shapePoints).toString());
    }

    @Test
    public void testGetAllStops() {
        Collection<Stop> stops = subject.getAllStops();

        assertEquals(25, stops.size());
        assertEquals("<Stop Z_A>", first(stops).toString());
    }

    @Test
    public void testGetAllStopTimes() {
        Collection<StopTime> stopTimes = subject.getAllStopTimes();

        assertEquals(80, stopTimes.size());
        assertEquals("StopTime(seq=1 stop=Z_A trip=agency_1.1 times=00:00:00-00:00:00)",
                first(stopTimes).toString());
    }

    @Test
    public void testGetAllTrips() {
        Collection<Trip> trips = subject.getAllTrips();

        assertEquals(33, trips.size());
        assertEquals("<Trip agency_1.1>", first(trips).toString());
    }

    @Test
    public void testGetStopForId() {
        Stop stop = subject.getStopForId(new FeedScopedId("Z", "P"));
        assertEquals("<Stop Z_P>", stop.toString());
    }

    @Test
    public void testGetTripAgencyIdsReferencingServiceId() {
        List<String> agencyIds;

        agencyIds = subject.getTripAgencyIdsReferencingServiceId(SERVICE_ALLDAYS_ID);
        assertEquals("[agency]", agencyIds.toString());

        agencyIds = subject.getTripAgencyIdsReferencingServiceId(SERVICE_WEEKDAYS_ID);
        assertEquals("[agency]", agencyIds.toString());
    }

    @Test
    public void testGetStopsForStation() {
        List<Stop> stops = subject.getStopsForStation(subject.getStopForId(STATION_ID));
        assertEquals("[<Stop Z_A>]", stops.toString());
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
        assertEquals("[<Stop Z_A>, <Stop Z_B>, <Stop Z_C>]",
                stopTimes.stream().map(StopTime::getStop).collect(toList()).toString());
    }

    @Test
    public void testGetAllServiceIds() {
        List<FeedScopedId> serviceIds = subject.getAllServiceIds();

        assertEquals(2, serviceIds.size());
        assertEquals("Z_alldays", first(serviceIds).toString());
    }

    @Test
    public void testGetCalendarDatesForServiceId() {
        List<ServiceCalendarDate> dates = subject.getCalendarDatesForServiceId(SERVICE_WEEKDAYS_ID);
        assertEquals(
                "[<CalendarDate serviceId=Z_weekdays date=ServiceIdDate(2017-8-31) exception=2>]",
                dates.toString());
    }

    @Test
    public void testGetCalendarForServiceId() {
        ServiceCalendar calendar = subject.getCalendarForServiceId(SERVICE_ALLDAYS_ID);
        assertEquals("<ServiceCalendar Z_alldays [1111111]>", calendar.toString());
    }

    private static FareAttribute createFareAttribute() {
        FareAttribute fa = new FareAttribute();
        fa.setId(new FeedScopedId(agency.getId(), "FA"));
        FareRule rule = new FareRule();
        rule.setFare(fa);
        return fa;
    }

    private static FareRule createFareRule() {
        FareAttribute fa = new FareAttribute();
        fa.setId(new FeedScopedId(agency.getId(), "FA"));
        FareRule rule = new FareRule();
        rule.setOriginId("Zone A");
        rule.setContainsId("Zone B");
        rule.setDestinationId("Zone C");
        rule.setFare(fa);
        return rule;
    }


    private static ServiceCalendarDate createAServiceCalendarDateExclution(FeedScopedId serviceId) {
        ServiceCalendarDate date = new ServiceCalendarDate();
        date.setServiceId(serviceId);
        date.setDate(new ServiceDate(2017, 8, 31));
        date.setExceptionType(2);
        return date;
    }

    private static <T> T first(Collection<T> collection) {
        List<T> items = new ArrayList<>(collection);
        items.sort(comparing(Object::toString));
        return items.isEmpty() ? null : items.get(0);
    }

    private static String toString(ShapePoint sp) {
        int lat = (int) sp.getLat();
        int lon = (int) sp.getLon();
        return "#" + sp.getSequence() + " (" + lat + "," + lon + ")";
    }
}