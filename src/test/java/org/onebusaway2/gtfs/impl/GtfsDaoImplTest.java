package org.onebusaway2.gtfs.impl;

import org.junit.BeforeClass;
import org.junit.Test;
import org.onebusaway2.gtfs.model.*;
import org.onebusaway2.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsImport;
import org.opentripplanner.gtfs.mapping.ModelMapper;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public class GtfsDaoImplTest {

    private static final AgencyAndId SERVICE_ALLDAYS_ID = new AgencyAndId("1", "alldays");

    private static final AgencyAndId SERVICE_WEEKDAYS_ID = new AgencyAndId("1", "weekdays");

    private static final AgencyAndId STATION_ID = new AgencyAndId("1", "station");

    // The subject is used as read only; hence static is ok
    private static GtfsDaoImpl subject;

    private static Agency agency;

    @BeforeClass public static void setup() throws IOException {
        GtfsImport gtfsImport = new GtfsImport(new File(ConstantsForTests.FAKE_GTFS));

        subject = (GtfsDaoImpl) ModelMapper.mapDao(gtfsImport.getDao());
        agency = first(subject.getAllAgencies());

        // Supplement test data with at least one entity in all collections
        subject.saveEntity(createServiceCalendarDateExclution(SERVICE_WEEKDAYS_ID, 2017, 8, 31));
        subject.saveEntity(createFareAttribute());
        subject.saveEntity(new FareRule());
        subject.saveEntity(new FeedInfo());

        //        subject.clearAllCachedIndexes();
    }

    @Test public void testGetAllAgencies() {
        Collection<Agency> agencies = subject.getAllAgencies();
        Agency agency = first(agencies);

        assertEquals(1, agencies.size());
        assertEquals("agency", agency.getId());
        assertEquals("Fake Agency", agency.getName());
    }

    @Test public void testGetAllCalendarDates() {
        Collection<ServiceCalendarDate> calendarDates = subject.getAllCalendarDates();

        assertEquals(1, calendarDates.size());
        assertEquals(
                "<CalendarDate serviceId=1_weekdays date=ServiceIdDate(2017-8-31) exception=2>",
                first(calendarDates).toString());
    }

    @Test public void testGetAllCalendars() {
        Collection<ServiceCalendar> calendars = subject.getAllCalendars();

        assertEquals(2, calendars.size());
        assertEquals("<ServiceCalendar 1_alldays [1111111]>", first(calendars).toString());
    }

    @Test public void testGetAllFareAttributes() {
        Collection<FareAttribute> fareAttributes = subject.getAllFareAttributes();

        assertEquals(1, fareAttributes.size());
        assertEquals("<FareAttribute agency_FA>", first(fareAttributes).toString());
    }

    @Test public void testGetAllFareRules() {
        Collection<FareRule> fareRules = subject.getAllFareRules();

        assertEquals(1, fareRules.size());
        assertEquals("<FareRule 1>", first(fareRules).toString());
    }

    @Test public void testGetAllFeedInfos() {
        Collection<FeedInfo> feedInfos = subject.getAllFeedInfos();

        assertEquals(1, feedInfos.size());
        assertEquals("<FeedInfo 1>", first(feedInfos).toString());
    }

    @Test public void testGetAllFrequencies() {
        Collection<Frequency> frequencies = subject.getAllFrequencies();

        assertEquals(2, frequencies.size());
        assertEquals("<Frequency 1 start=06:00:00 end=10:00:01>", first(frequencies).toString());
    }

    @Test public void testGetAllPathways() {
        Collection<Pathway> pathways = subject.getAllPathways();

        assertEquals(4, pathways.size());
        assertEquals("<Pathway 1_pathways_1_2>", first(pathways).toString());
    }

    @Test public void testGetAllRoutes() {
        Collection<Route> routes = subject.getAllRoutes();

        assertEquals(18, routes.size());
        assertEquals("<Route agency_15 15>", first(routes).toString());
    }

    @Test public void testGetAllTransfers() {
        Collection<Transfer> transfers = subject.getAllTransfers();

        assertEquals(9, transfers.size());
        assertEquals("<Transfer 1>", first(transfers).toString());
    }

    @Test public void testGetAllShapePoints() {
        Collection<ShapePoint> shapePoints = subject.getAllShapePoints();

        assertEquals(9, shapePoints.size());
        assertEquals("<ShapePoint 1_4 #1 (41.0,-75.0)>", first(shapePoints).toString());
    }

    @Test public void testGetAllStops() {
        Collection<Stop> stops = subject.getAllStops();

        assertEquals(25, stops.size());
        assertEquals("<Stop 1_entrance_b>", first(stops).toString());
    }

    @Test public void testGetAllStopTimes() {
        Collection<StopTime> stopTimes = subject.getAllStopTimes();

        assertEquals(80, stopTimes.size());
        assertEquals("StopTime(seq=1 stop=1_A trip=agency_1.1 times=00:00:00-00:00:00)",
                first(stopTimes).toString());
    }

    @Test public void testGetAllTrips() {
        Collection<Trip> trips = subject.getAllTrips();

        assertEquals(33, trips.size());
        assertEquals("<Trip agency_1.1>", first(trips).toString());
    }

    @Test public void testGetStopForId() {
        Stop stop = subject.getStopForId(new AgencyAndId("1", "P"));
        assertEquals("<Stop 1_P>", stop.toString());
    }

    @Test public void testGetTripAgencyIdsReferencingServiceId() {
        List<String> agencyIds;

        agencyIds = subject.getTripAgencyIdsReferencingServiceId(SERVICE_ALLDAYS_ID);
        assertEquals("[agency]", agencyIds.toString());

        agencyIds = subject.getTripAgencyIdsReferencingServiceId(SERVICE_WEEKDAYS_ID);
        assertEquals("[agency]", agencyIds.toString());
    }

    @Test public void testGetStopsForStation() {
        List<Stop> stops = subject.getStopsForStation(subject.getStopForId(STATION_ID));
        assertEquals("[<Stop 1_A>]", stops.toString());
    }

    @Test public void testGetShapePointsForShapeId() {
        List<ShapePoint> shapePoints = subject.getShapePointsForShapeId(new AgencyAndId("1", "5"));
        assertEquals("[#1 (41,-72), #2 (41,-72), #3 (40,-72), #4 (41,-73), #5 (41,-74)]",
                shapePoints.stream().map(GtfsDaoImplTest::toString).collect(toList()).toString());
    }

    @Test public void testGetStopTimesForTrip() {
        List<StopTime> stopTimes = subject.getStopTimesForTrip(first(subject.getAllTrips()));
        assertEquals("[<Stop 1_A>, <Stop 1_B>, <Stop 1_C>]",
                stopTimes.stream().map(StopTime::getStop).collect(toList()).toString());
    }

    @Test public void testGetAllServiceIds() {
        List<AgencyAndId> serviceIds = subject.getAllServiceIds();

        assertEquals(2, serviceIds.size());
        assertEquals("1_alldays", first(serviceIds).toString());
    }

    @Test public void testGetCalendarDatesForServiceId() {
        List<ServiceCalendarDate> dates = subject.getCalendarDatesForServiceId(SERVICE_WEEKDAYS_ID);
        assertEquals(
                "[<CalendarDate serviceId=1_weekdays date=ServiceIdDate(2017-8-31) exception=2>]",
                dates.toString());
    }

    @Test public void testGetCalendarForServiceId() {
        ServiceCalendar calendar = subject.getCalendarForServiceId(SERVICE_ALLDAYS_ID);
        assertEquals("<ServiceCalendar 1_alldays [1111111]>", calendar.toString());
    }

    private static FareAttribute createFareAttribute() {
        FareAttribute fa = new FareAttribute();
        fa.setId(createAgencyAndId("FA"));
        return fa;
    }

    private static ServiceCalendarDate createServiceCalendarDateExclution(AgencyAndId serviceId,
            int year, int month, int day) {
        ServiceCalendarDate date = new ServiceCalendarDate();
        date.setServiceId(serviceId);
        date.setDate(new ServiceDate(year, month, day));
        date.setExceptionType(2);
        return date;
    }

    private static <T> T first(Collection<? extends T> collection) {
        //noinspection ConstantConditions
        return collection.stream().findFirst().get();
    }

    private static AgencyAndId createAgencyAndId(String id) {
        return new AgencyAndId(agency.getId(), id);
    }

    private static String toString(ShapePoint sp) {
        return "#" + sp.getSequence() + " (" + ((int) sp.getLat()) + "," + ((int) sp.getLon())
                + ")";
    }
}