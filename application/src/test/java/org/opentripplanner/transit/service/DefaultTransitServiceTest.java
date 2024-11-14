package org.opentripplanner.transit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;
import static org.opentripplanner.transit.model.basic.TransitMode.FERRY;
import static org.opentripplanner.transit.model.basic.TransitMode.RAIL;
import static org.opentripplanner.transit.model.basic.TransitMode.TRAM;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
  private static final Trip TRIP = TimetableRepositoryForTest
    .trip("REAL_TIME_TRIP")
    .withServiceId(SERVICE_ID)
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
  private static final int DELAY = 120;
  private static final RealTimeTripTimes REALTIME_TRIP_TIMES = SCHEDULED_TRIP_TIMES.copyScheduledTimes();

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

    var timetableRepository = new TimetableRepository(siteRepository, new Deduplicator());
    var calendar = new CalendarServiceData();
    calendar.putServiceDatesForServiceId(SERVICE_ID, List.of(SERVICE_DATE));
    var serviceCodes = timetableRepository.getServiceCodes();
    serviceCodes.put(SERVICE_ID, SERVICE_CODE);
    timetableRepository.updateCalendarServiceData(true, calendar, DataImportIssueStore.NOOP);
    timetableRepository.addTripPattern(RAIL_PATTERN.getId(), RAIL_PATTERN);
    timetableRepository.index();

    timetableRepository.initTimetableSnapshotProvider(() -> {
      TimetableSnapshot timetableSnapshot = new TimetableSnapshot();
      timetableSnapshot.update(
        new RealTimeTripUpdate(REAL_TIME_PATTERN, REALTIME_TRIP_TIMES, SERVICE_DATE)
      );

      return timetableSnapshot.commit();
    });

    service =
      new DefaultTransitService(timetableRepository) {
        @Override
        public Collection<TripPattern> getPatternsForStop(StopLocation stop) {
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
    var modes = service.getModesOfStopLocation(STOP_A);
    assertEquals(List.of(TRAM), modes);
  }

  @Test
  void modeFromPatterns() {
    var modes = service.getModesOfStopLocation(STOP_B);
    assertEquals(List.of(RAIL, FERRY), modes);
  }

  @Test
  void stationModes() {
    var modes = service.getModesOfStopLocationsGroup(STATION);
    assertEquals(List.of(RAIL, FERRY, TRAM), modes);
  }

  @Test
  void getPatternForStopsWithoutRealTime() {
    Collection<TripPattern> patternsForStop = service.getPatternsForStop(STOP_B, false);
    assertEquals(Set.of(FERRY_PATTERN, RAIL_PATTERN), patternsForStop);
  }

  @Test
  void getPatternForStopsWithRealTime() {
    Collection<TripPattern> patternsForStop = service.getPatternsForStop(STOP_B, true);
    assertEquals(Set.of(FERRY_PATTERN, RAIL_PATTERN, REAL_TIME_PATTERN), patternsForStop);
  }

  @Test
  void getScheduledTripTimes() {
    assertEquals(
      List.of(
        new TripTimeOnDate(SCHEDULED_TRIP_TIMES, 0, RAIL_PATTERN),
        new TripTimeOnDate(SCHEDULED_TRIP_TIMES, 1, RAIL_PATTERN)
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
      List.of(
        new TripTimeOnDate(REALTIME_TRIP_TIMES, 0, REAL_TIME_PATTERN, SERVICE_DATE, midnight),
        new TripTimeOnDate(REALTIME_TRIP_TIMES, 1, REAL_TIME_PATTERN, SERVICE_DATE, midnight)
      ),
      service.getTripTimeOnDates(TRIP, SERVICE_DATE)
    );
  }

  @Test
  void getTripTimesOnNoServiceDay() {
    assertEquals(List.of(), service.getTripTimeOnDates(TRIP, NO_SERVICE_DATE));
  }
}
