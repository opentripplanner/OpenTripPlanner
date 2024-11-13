package org.opentripplanner.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;
import org.opentripplanner.transit.service.TimetableRepository;

public class GtfsRealtimeFuzzyTripMatcherTest {

  private static final String ROUTE_ID = "r1";
  private static final String FEED_ID = TimetableRepositoryForTest.FEED_ID;
  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 11, 13);
  private static final String GTFS_SERVICE_DATE = SERVICE_DATE.toString().replaceAll("-", "");
  private static final int SERVICE_CODE = 555;
  private static final SiteRepositoryBuilder siteRepositoryBuilder = SiteRepository.of();
  private static final TimetableRepositoryForTest TEST_MODEL = new TimetableRepositoryForTest(
    siteRepositoryBuilder
  );
  private static final RegularStop STOP_1 = TEST_MODEL.stop("s1").build();
  private static final RegularStop STOP_2 = TEST_MODEL.stop("s2").build();
  private static final TimetableRepository TT_REPO = new TimetableRepository(
    siteRepositoryBuilder.build(),
    new Deduplicator()
  );
  private static final Route ROUTE = TimetableRepositoryForTest.route(id(ROUTE_ID)).build();
  private static final Trip TRIP = TimetableRepositoryForTest.trip("t1").build();

  private static final FeedScopedId SERVICE_ID = TimetableRepositoryForTest.id("sid1");
  private static final String START_TIME = "07:30:00";
  private static final RealTimeTripTimes TRIP_TIMES = TripTimesFactory.tripTimes(
    TRIP,
    TEST_MODEL.stopTimesEvery5Minutes(5, TRIP, START_TIME),
    new Deduplicator()
  );
  private static final TripPattern TRIP_PATTERN = TimetableRepositoryForTest
    .tripPattern("tp1", ROUTE)
    .withStopPattern(TimetableRepositoryForTest.stopPattern(STOP_1, STOP_2))
    .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(TRIP_TIMES))
    .build();

  @BeforeAll
  static void setup() {
    TRIP_TIMES.setServiceCode(SERVICE_CODE);
    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(SERVICE_ID, List.of(SERVICE_DATE));
    TT_REPO.addTripPattern(TRIP_PATTERN.getId(), TRIP_PATTERN);
    TT_REPO.getServiceCodes().put(SERVICE_ID, SERVICE_CODE);
    TT_REPO.updateCalendarServiceData(true, calendarServiceData, DataImportIssueStore.NOOP);
    TT_REPO.index();
  }

  @Test
  public void simpleMatch() {
    var matcher = matcher();
    TripDescriptor trip1 = TripDescriptor
      .newBuilder()
      .setRouteId(ROUTE_ID)
      .setDirectionId(2)
      .setStartTime(START_TIME)
      .setStartDate(GTFS_SERVICE_DATE)
      .build();
    assertEquals(TRIP.getId().getId(), matcher.match(FEED_ID, trip1).getTripId());
  }

  @Test
  void noMatch() {
    // Test matching with "real time", when schedule uses time grater than 24:00
    var trip1 = TripDescriptor
      .newBuilder()
      .setRouteId("4")
      .setDirectionId(0)
      .setStartTime("12:00:00")
      .setStartDate("20090915")
      .build();
    // No departure at this time
    assertFalse(trip1.hasTripId());
    trip1 =
      TripDescriptor
        .newBuilder()
        .setRouteId("1")
        .setStartTime("06:47:00")
        .setStartDate("20090915")
        .build();
    // Missing direction id
    assertFalse(trip1.hasTripId());
  }

  private static GtfsRealtimeFuzzyTripMatcher matcher() {
    return new GtfsRealtimeFuzzyTripMatcher(new DefaultTransitService(TT_REPO));
  }
}
