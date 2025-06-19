package org.opentripplanner.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;

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
  private static final TimetableRepository TIMETABLE_REPOSITORY = new TimetableRepository(
    siteRepositoryBuilder.build(),
    new Deduplicator()
  );
  private static final Route ROUTE = TimetableRepositoryForTest.route(id(ROUTE_ID)).build();
  private static final String TRIP_ID = "t1";
  private static final Trip TRIP = TimetableRepositoryForTest.trip(TRIP_ID).build();

  private static final FeedScopedId SERVICE_ID = TimetableRepositoryForTest.id("sid1");
  private static final String START_TIME = "07:30:00";
  private static final TripTimes TRIP_TIMES = TripTimesFactory.tripTimes(
    TRIP,
    TEST_MODEL.stopTimesEvery5Minutes(5, TRIP, START_TIME),
    new Deduplicator()
  ).withServiceCode(SERVICE_CODE);
  private static final TripPattern TRIP_PATTERN = TimetableRepositoryForTest.tripPattern(
    "tp1",
    ROUTE
  )
    .withStopPattern(TimetableRepositoryForTest.stopPattern(STOP_1, STOP_2))
    .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(TRIP_TIMES))
    .build();

  @BeforeAll
  static void setup() {
    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(SERVICE_ID, List.of(SERVICE_DATE));
    TIMETABLE_REPOSITORY.addTripPattern(TRIP_PATTERN.getId(), TRIP_PATTERN);
    TIMETABLE_REPOSITORY.getServiceCodes().put(SERVICE_ID, SERVICE_CODE);
    TIMETABLE_REPOSITORY.updateCalendarServiceData(
      true,
      calendarServiceData,
      DataImportIssueStore.NOOP
    );
    TIMETABLE_REPOSITORY.index();
  }

  @Test
  void noTripId() {
    var matcher = matcher();
    TripDescriptor trip = matchingTripUpdate().build();
    assertEquals(TRIP_ID, matcher.match(FEED_ID, trip).getTripId());
  }

  @Test
  void tripIdSetButNotInSchedule() {
    var matcher = matcher();
    TripDescriptor trip = matchingTripUpdate().setTripId("does-not-exist-in-schedule").build();
    assertEquals(TRIP_ID, matcher.match(FEED_ID, trip).getTripId());
  }

  @Test
  void tripIdExistsInSchedule() {
    var matcher = matcher();
    TripDescriptor trip = matchingTripUpdate().setTripId(TRIP_ID).build();
    assertEquals(TRIP_ID, matcher.match(FEED_ID, trip).getTripId());
  }

  @Test
  void incorrectRoute() {
    var matcher = matcher();
    TripDescriptor trip = matchingTripUpdate().setRouteId("does-not-exists").build();
    assertFalse(matcher.match(FEED_ID, trip).hasTripId());
  }

  @Test
  void incorrectDateFormat() {
    var matcher = matcher();
    TripDescriptor trip = matchingTripUpdate().setStartDate("ZZZ").build();
    assertFalse(matcher.match(FEED_ID, trip).hasTripId());
  }

  @Test
  void incorrectDirection() {
    var matcher = matcher();
    TripDescriptor trip = matchingTripUpdate().setDirectionId(1).build();
    assertFalse(matcher.match(FEED_ID, trip).hasTripId());
  }

  @Test
  void noMatch() {
    // Test matching with "real time", when schedule uses time greater than 24:00
    var trip = TripDescriptor.newBuilder()
      .setRouteId("4")
      .setDirectionId(0)
      .setStartTime("12:00:00")
      .setStartDate("20090915")
      .build();
    // No departure at this time
    assertFalse(trip.hasTripId());
    trip = TripDescriptor.newBuilder()
      .setRouteId("1")
      .setStartTime("06:47:00")
      .setStartDate("20090915")
      .build();
    // Missing direction id
    assertFalse(trip.hasTripId());
  }

  @Nested
  class IncompleteData {

    @Test
    void noRouteId() {
      var td = matchingTripUpdate().clearRouteId().build();
      assertFalse(matcher().match(FEED_ID, td).hasTripId());
    }

    @Test
    void noDirectionId() {
      var td = matchingTripUpdate().clearDirectionId().build();
      assertFalse(matcher().match(FEED_ID, td).hasTripId());
    }

    @Test
    void noStartDate() {
      var td = matchingTripUpdate().clearStartDate().build();
      assertFalse(matcher().match(FEED_ID, td).hasTripId());
    }

    @Test
    void noStartTime() {
      var td = matchingTripUpdate().clearStartTime().build();
      assertFalse(matcher().match(FEED_ID, td).hasTripId());
    }
  }

  private static GtfsRealtimeFuzzyTripMatcher matcher() {
    return new GtfsRealtimeFuzzyTripMatcher(new DefaultTransitService(TIMETABLE_REPOSITORY));
  }

  private static TripDescriptor.Builder matchingTripUpdate() {
    return TripDescriptor.newBuilder()
      .setRouteId(ROUTE_ID)
      .setDirectionId(2)
      .setStartTime(START_TIME)
      .setStartDate(GTFS_SERVICE_DATE);
  }
}
