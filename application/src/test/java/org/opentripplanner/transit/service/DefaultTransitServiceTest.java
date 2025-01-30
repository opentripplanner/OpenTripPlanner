package org.opentripplanner.transit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;
import static org.opentripplanner.transit.model.basic.TransitMode.FERRY;
import static org.opentripplanner.transit.model.basic.TransitMode.RAIL;
import static org.opentripplanner.transit.model.basic.TransitMode.TRAM;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.RealTimeTripUpdate;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.utils.time.ServiceDateUtils;

class DefaultTransitServiceTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static TransitService service;
  private static final Station STATION = TEST_MODEL.station("C").build();
  private static final RegularStop STOP_A = TEST_MODEL
    .stop("A")
    .withVehicleType(TRAM)
    .withParentStation(STATION)
    .build();
  private static final RegularStop STOP_B = TEST_MODEL.stop("B").withParentStation(STATION).build();

  private static final FeedScopedId SERVICE_ID = new FeedScopedId("FEED", "SERVICE");
  private static final int SERVICE_CODE = 0;
  private static final TripPattern FERRY_PATTERN = TEST_MODEL.pattern(FERRY).build();
  private static final TripPattern BUS_PATTERN = TEST_MODEL.pattern(BUS).build();

  private static final StopPattern REAL_TIME_STOP_PATTERN = TimetableRepositoryForTest.stopPattern(
    STOP_A,
    STOP_B
  );
  private static final TripPattern REAL_TIME_PATTERN = TEST_MODEL
    .pattern(BUS)
    .withStopPattern(REAL_TIME_STOP_PATTERN)
    .withCreatedByRealtimeUpdater(true)
    .build();

  static FeedScopedId CALENDAR_ID = TimetableRepositoryForTest.id("CAL_1");
  static Trip TRIP = TimetableRepositoryForTest
    .trip("123")
    .withHeadsign(I18NString.of("Trip Headsign"))
    .withServiceId(CALENDAR_ID)
    .build();
  private static final Trip ADDED_TRIP = TimetableRepositoryForTest
    .trip("REAL_TIME_ADDED_TRIP")
    .withServiceId(CALENDAR_ID)
    .build();
  private static final ScheduledTripTimes SCHEDULED_TRIP_TIMES = ScheduledTripTimes
    .of()
    .withTrip(TRIP)
    .withArrivalTimes(new int[] { 0, 1 })
    .withDepartureTimes(new int[] { 0, 1 })
    .withServiceCode(SERVICE_CODE)
    .build();

  private static final TripPattern RAIL_PATTERN = TEST_MODEL
    .pattern(RAIL)
    .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(SCHEDULED_TRIP_TIMES))
    .build();

  private static final int DELAY = 120;
  private static final RealTimeTripTimes REALTIME_TRIP_TIMES = SCHEDULED_TRIP_TIMES.copyScheduledTimes();
  private static final RealTimeTripTimes ADDED_TRIP_TIMES = RealTimeTripTimes.of(
    ScheduledTripTimes
      .of()
      .withTrip(ADDED_TRIP)
      .withArrivalTimes(new int[] { 10, 11 })
      .withDepartureTimes(new int[] { 10, 11 })
      .withServiceCode(SERVICE_CODE)
      .build()
  );

  static {
    for (var i = 0; i < REALTIME_TRIP_TIMES.getNumStops(); ++i) {
      REALTIME_TRIP_TIMES.updateArrivalDelay(i, DefaultTransitServiceTest.DELAY);
      REALTIME_TRIP_TIMES.updateDepartureDelay(i, DefaultTransitServiceTest.DELAY);
    }
  }

  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 1, 1);
  private static final LocalDate NO_SERVICE_DATE = LocalDate.of(2000, 1, 1);

  @BeforeAll
  static void setup() {
    var siteRepository = TEST_MODEL
      .siteRepositoryBuilder()
      .withRegularStop(STOP_A)
      .withRegularStop(STOP_B)
      .withStation(STATION)
      .build();

    var deduplicator = new Deduplicator();
    var transitModel = new TimetableRepository(siteRepository, deduplicator);
    var canceledStopTimes = TEST_MODEL.stopTimesEvery5Minutes(3, TRIP, "11:30");
    var canceledTripTimes = TripTimesFactory.tripTimes(TRIP, canceledStopTimes, deduplicator);
    canceledTripTimes.cancelTrip();
    transitModel.addTripPattern(RAIL_PATTERN.getId(), RAIL_PATTERN);

    // Crate a calendar (needed for testing cancelled trips)
    CalendarServiceData calendarServiceData = new CalendarServiceData();
    var firstDate = LocalDate.of(2024, 8, 8);
    var secondDate = LocalDate.of(2024, 8, 9);
    calendarServiceData.putServiceDatesForServiceId(
      CALENDAR_ID,
      List.of(firstDate, secondDate, SERVICE_DATE)
    );
    transitModel.getServiceCodes().put(CALENDAR_ID, 0);
    transitModel.updateCalendarServiceData(true, calendarServiceData, DataImportIssueStore.NOOP);

    transitModel.index();
    var timetableRepository = new TimetableRepository(siteRepository, new Deduplicator());
    var calendar = new CalendarServiceData();
    calendar.putServiceDatesForServiceId(SERVICE_ID, List.of(SERVICE_DATE));
    var serviceCodes = timetableRepository.getServiceCodes();
    serviceCodes.put(SERVICE_ID, SERVICE_CODE);
    timetableRepository.updateCalendarServiceData(true, calendar, DataImportIssueStore.NOOP);
    timetableRepository.addTripPattern(RAIL_PATTERN.getId(), RAIL_PATTERN);
    timetableRepository.index();

    TimetableSnapshot timetableSnapshot = new TimetableSnapshot();
    RealTimeTripTimes tripTimes = RealTimeTripTimes.of(
      ScheduledTripTimes
        .of()
        .withTrip(TimetableRepositoryForTest.trip("123").build())
        .withDepartureTimes(new int[] { 0, 1 })
        .build()
    );
    timetableSnapshot.update(new RealTimeTripUpdate(REAL_TIME_PATTERN, tripTimes, firstDate));
    timetableSnapshot.update(new RealTimeTripUpdate(RAIL_PATTERN, canceledTripTimes, firstDate));
    timetableSnapshot.update(new RealTimeTripUpdate(RAIL_PATTERN, canceledTripTimes, secondDate));
    timetableSnapshot.update(
      new RealTimeTripUpdate(REAL_TIME_PATTERN, REALTIME_TRIP_TIMES, SERVICE_DATE)
    );
    timetableSnapshot.update(
      new RealTimeTripUpdate(REAL_TIME_PATTERN, ADDED_TRIP_TIMES, SERVICE_DATE, null, true, false)
    );

    var snapshot = timetableSnapshot.commit();
    timetableRepository.initTimetableSnapshotProvider(() -> snapshot);

    service =
      new DefaultTransitService(timetableRepository) {
        @Override
        public Collection<TripPattern> findPatterns(StopLocation stop) {
          if (stop.equals(STOP_B)) {
            return List.of(FERRY_PATTERN, FERRY_PATTERN, RAIL_PATTERN, RAIL_PATTERN, RAIL_PATTERN);
          } else {
            return List.of(BUS_PATTERN);
          }
        }
      };
  }

  @Test
  void modeFromGtfsVehicleType() {
    var modes = service.findTransitModes(STOP_A);
    assertEquals(List.of(TRAM), modes);
  }

  @Test
  void modeFromPatterns() {
    var modes = service.findTransitModes(STOP_B);
    assertEquals(List.of(RAIL, FERRY), modes);
  }

  @Test
  void stationModes() {
    var modes = service.findTransitModes(STATION);
    assertEquals(List.of(RAIL, FERRY, TRAM), modes);
  }

  @Test
  void getPatternForStopsWithoutRealTime() {
    Collection<TripPattern> patternsForStop = service.findPatterns(STOP_B, false);
    assertEquals(Set.of(FERRY_PATTERN, RAIL_PATTERN), patternsForStop);
  }

  @Test
  void getPatternForStopsWithRealTime() {
    Collection<TripPattern> patternsForStop = service.findPatterns(STOP_B, true);
    assertEquals(Set.of(FERRY_PATTERN, RAIL_PATTERN, REAL_TIME_PATTERN), patternsForStop);
  }

  @Test
  void listCanceledTrips() {
    var canceledTrips = service.listCanceledTrips();
    assertEquals("[TripOnServiceDate{F:123}, TripOnServiceDate{F:123}]", canceledTrips.toString());
  }

  @Test
  void containsTrip() {
    assertFalse(service.containsTrip(new FeedScopedId("x", "x")));
  }

  @Test
  void getScheduledTripTimes() {
    assertEquals(
      Optional.of(
        List.of(
          new TripTimeOnDate(SCHEDULED_TRIP_TIMES, 0, RAIL_PATTERN),
          new TripTimeOnDate(SCHEDULED_TRIP_TIMES, 1, RAIL_PATTERN)
        )
      ),
      service.getScheduledTripTimes(TRIP)
    );
  }

  @Test
  void getRealtimeTripTimes() {
    Instant midnight = ServiceDateUtils
      .asStartOfService(SERVICE_DATE, service.getTimeZone())
      .toInstant();

    assertEquals(
      Optional.of(
        List.of(
          new TripTimeOnDate(REALTIME_TRIP_TIMES, 0, REAL_TIME_PATTERN, SERVICE_DATE, midnight),
          new TripTimeOnDate(REALTIME_TRIP_TIMES, 1, REAL_TIME_PATTERN, SERVICE_DATE, midnight)
        )
      ),
      service.getTripTimeOnDates(TRIP, SERVICE_DATE)
    );
  }

  @Test
  void getTripTimesOnNoServiceDay() {
    assertEquals(Optional.empty(), service.getTripTimeOnDates(TRIP, NO_SERVICE_DATE));
  }

  @Test
  void getScheduledTripTimesForAddedTrip() {
    assertEquals(Optional.empty(), service.getScheduledTripTimes(ADDED_TRIP));
  }

  @Test
  void getRealtimeTripTimesForAddedTrip() {
    Instant midnight = ServiceDateUtils
      .asStartOfService(SERVICE_DATE, service.getTimeZone())
      .toInstant();

    assertEquals(
      Optional.of(
        List.of(
          new TripTimeOnDate(ADDED_TRIP_TIMES, 0, REAL_TIME_PATTERN, SERVICE_DATE, midnight),
          new TripTimeOnDate(ADDED_TRIP_TIMES, 1, REAL_TIME_PATTERN, SERVICE_DATE, midnight)
        )
      ),
      service.getTripTimeOnDates(ADDED_TRIP, SERVICE_DATE)
    );
  }
}
