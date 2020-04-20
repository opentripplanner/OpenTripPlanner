package org.opentripplanner.model.impl;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceCalendar;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test will create a Transit service builder and then limit the service period.
 * The services defined is in period [D0, D3] with D1 and D2 inside that period. Then
 * the service pariod is limited to [D0, D1] excluding services on D2 and D3.
 *
 * All data related in the last part of the service should be removed after D1 until D3.

 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (30.10.2017)
 */
public class OtpTransitServiceBuilderLimitPeriodTest {
    private static int SEQ_NR = 0;
    private static final String FEED_ID = "F";
    private static final ServiceDate D0 = new ServiceDate(2020, 1, 1);
    private static final ServiceDate D1 = new ServiceDate(2020, 1, 8);
    private static final ServiceDate D2 = new ServiceDate(2020, 1, 15);
    private static final ServiceDate D3 = new ServiceDate(2020, 1, 31);
    private static final FeedScopedId SERVICE_C_IN = new FeedScopedId(FEED_ID, "CalSrvIn");
    private static final FeedScopedId SERVICE_D_IN = new FeedScopedId(FEED_ID, "CalSrvDIn");
    private static final FeedScopedId SERVICE_C_OUT = new FeedScopedId(FEED_ID, "CalSrvOut");
    private static final FeedScopedId SERVICE_D_OUT = new FeedScopedId(FEED_ID, "CalSrvDOut");

    private static final Stop STOP_1 = Stop.stopForTest("Stop-1", 0.0, 0.0);
    private static final Stop STOP_2 = Stop.stopForTest("Stop-2", 0.0,0.0);

    private static final Deduplicator DEDUPLICATOR = new Deduplicator();

    private final Route route = new Route();

    private final Trip tripCSIn = createTrip("TCalIn", SERVICE_C_IN);
    private final Trip tripCSOut = createTrip("TCalOut", SERVICE_C_OUT);
    private final Trip tripCSDIn = createTrip("TDateIn", SERVICE_D_IN);
    private final Trip tripCSDOut = createTrip("TDateOut", SERVICE_D_OUT);

    private TripPattern patternInT1;
    private TripPattern patternInT2;


    public static final List<StopTime> STOP_TIMES = List.of(
            createStopTime(STOP_1,0),
            createStopTime(STOP_2, 300)
    );
    private static final StopPattern STOP_PATTERN = new StopPattern(STOP_TIMES);

    private OtpTransitServiceBuilder subject;

    @Before
    public void setUp() {
        subject = new OtpTransitServiceBuilder();

        // Add a service calendar that overlap with the period limit
        subject.getCalendars().add(createServiceCalendar(SERVICE_C_IN, D1, D3));

        // Add a service calendar that is outside the period limit, expected deleted later
        subject.getCalendars().add(createServiceCalendar(SERVICE_C_OUT, D0, D1));

        // Add a service calendar date that is within the period limit
        subject.getCalendarDates().add(new ServiceCalendarDate(SERVICE_D_IN, D2, 1));

        // Add a service calendar date that is OUTSIDE the period limit, expected deleted later
        subject.getCalendarDates().add(new ServiceCalendarDate(SERVICE_D_OUT, D1, 1));

        // Add 2 stops
        subject.getStops().add(STOP_1);
        subject.getStops().add(STOP_2);

        // Add Route
        route.setId(newId());
        route.setType(3);
        route.setMode(TransitMode.BUS);
        subject.getRoutes().add(route);

        // Add trips; one for each day and calendar
        subject.getTripsById().addAll(List.of(tripCSIn, tripCSOut, tripCSDIn, tripCSDOut));

        // Pattern with trips that is partially deleted later
        patternInT1 = createTripPattern(List.of(tripCSIn, tripCSOut));
        // Pattern with trip that is inside period
        patternInT2 = createTripPattern(List.of(tripCSDIn));
        // Pattern with trip outside limiting period - pattern is deleted later
        TripPattern patternOut = createTripPattern(List.of(tripCSDOut));

        subject.getTripPatterns().put(STOP_PATTERN, patternInT1);
        subject.getTripPatterns().put(STOP_PATTERN, patternInT2);
        subject.getTripPatterns().put(STOP_PATTERN, patternOut);
    }

    @Test
    public void testLimitPeriod(){
        // Assert the test is set up as expected
        assertEquals(2, subject.getCalendars().size());
        assertEquals(2, subject.getCalendarDates().size());
        assertEquals(4, subject.getTripsById().size());
        assertEquals(3, subject.getTripPatterns().get(STOP_PATTERN).size());
        assertEquals(2, patternInT1.getTrips().size());
        assertEquals(2, patternInT1.scheduledTimetable.tripTimes.size());
        assertEquals(1, patternInT2.getTrips().size());
        assertEquals(1, patternInT2.scheduledTimetable.tripTimes.size());

        // Limit service to last half of month
        subject.limitServiceDays(new ServiceDateInterval(D2, D3));

        // Verify calendar
        List<ServiceCalendar> calendars = subject.getCalendars();
        assertEquals(calendars.toString(), 1, calendars.size());
        assertEquals(calendars.toString(), SERVICE_C_IN, calendars.get(0).getServiceId());

        // Verify calendar dates
        List<ServiceCalendarDate> dates = subject.getCalendarDates();
        assertEquals(dates.toString(), 1, dates.size());
        assertEquals(dates.toString(), SERVICE_D_IN, dates.get(0).getServiceId());

        // Verify trips
        EntityById<FeedScopedId, Trip> trips = subject.getTripsById();
        assertEquals(trips.toString(), 2, trips.size());
        assertTrue(trips.toString(), trips.containsKey(tripCSIn.getId()));
        assertTrue(trips.toString(), trips.containsKey(tripCSDIn.getId()));

        // Verify patterns
        Collection<TripPattern> patterns = subject.getTripPatterns().get(STOP_PATTERN);
        assertEquals(2, patterns.size());
        assertTrue(patterns.toString(), patterns.contains(patternInT1));
        assertTrue(patterns.toString(), patterns.contains(patternInT2));


        // Verify trips in pattern (one trip is removed from patternInT1)
        assertEquals(1, patternInT1.getTrips().size());
        assertEquals(tripCSIn, patternInT1.getTrips().get(0));

        // Verify trips in pattern is unchanged (one trip)
        assertEquals(1, patternInT2.getTrips().size());

        // Verify scheduledTimetable trips (one trip is removed from patternInT1)
        assertEquals(1, patternInT1.scheduledTimetable.tripTimes.size());
        assertEquals(tripCSIn, patternInT1.scheduledTimetable.tripTimes.get(0).trip);

        // Verify scheduledTimetable trips in pattern is unchanged (one trip)
        assertEquals(1, patternInT2.scheduledTimetable.tripTimes.size());
    }

    private TripPattern createTripPattern(Collection<Trip> trips) {
        TripPattern p = new TripPattern(route, STOP_PATTERN);
        p.setId(
                new FeedScopedId(
                        FEED_ID,
                        trips.stream()
                                .map(t -> t.getId().getId())
                                .collect(Collectors.joining(":")
                        )
                )
        );
        p.name = "Pattern";
        for (Trip trip : trips) {
            p.add(new TripTimes(trip, STOP_TIMES, DEDUPLICATOR));
        }
        return p;
    }

    private static ServiceCalendar createServiceCalendar(
            FeedScopedId serviceId, ServiceDate start, ServiceDate end
    ) {
        ServiceCalendar calendar = new ServiceCalendar();
        calendar.setPeriod(new ServiceDateInterval(start, end));
        calendar.setAllDays(1);
        calendar.setServiceId(serviceId);
        return calendar;
    }

    private Trip createTrip(String id, FeedScopedId serviceId) {
        Trip trip = new Trip();
        trip.setId(new FeedScopedId(FEED_ID, id));
        trip.setServiceId(serviceId);
        trip.setDirectionId("1");
        trip.setRoute(route);
        return trip;
    }

    private static StopTime createStopTime(Stop stop, int time) {
        StopTime st = new StopTime();
        st.setStop(stop);
        st.setDepartureTime(time);
        st.setArrivalTime(time);
        st.setPickupType(1);
        st.setDropOffType(1);
        return st;
    }

    private static FeedScopedId newId() {
        return new FeedScopedId(FEED_ID, Integer.toString(++SEQ_NR));
    }
}