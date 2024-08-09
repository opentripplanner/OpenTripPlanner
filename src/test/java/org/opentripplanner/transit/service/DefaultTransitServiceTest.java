package org.opentripplanner.transit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_30;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;
import static org.opentripplanner.transit.model.basic.TransitMode.FERRY;
import static org.opentripplanner.transit.model.basic.TransitMode.RAIL;
import static org.opentripplanner.transit.model.basic.TransitMode.TRAM;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.DatedTrip;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class DefaultTransitServiceTest {

  private static final TransitModelForTest TEST_MODEL = TransitModelForTest.of();

  static TransitService service;
  static Station STATION = TEST_MODEL.station("C").build();
  static RegularStop STOP_A = TEST_MODEL
    .stop("A")
    .withVehicleType(TRAM)
    .withParentStation(STATION)
    .build();
  static RegularStop STOP_B = TEST_MODEL.stop("B").withParentStation(STATION).build();
  static TripPattern RAIL_PATTERN = TEST_MODEL.pattern(RAIL).build();
  static TripPattern FERRY_PATTERN = TEST_MODEL.pattern(FERRY).build();
  static TripPattern BUS_PATTERN = TEST_MODEL.pattern(BUS).build();

  static StopPattern REAL_TIME_STOP_PATTERN = TransitModelForTest.stopPattern(STOP_A, STOP_B);
  static TripPattern REAL_TIME_PATTERN = TEST_MODEL
    .pattern(BUS)
    .withStopPattern(REAL_TIME_STOP_PATTERN)
    .withCreatedByRealtimeUpdater(true)
    .build();

  static FeedScopedId CALENDAR_ID = TransitModelForTest.id("CAL_1");
  static Trip TRIP = TransitModelForTest
    .trip("123")
    .withHeadsign(I18NString.of("Trip Headsign"))
    .withServiceId(CALENDAR_ID)
    .build();

  @BeforeAll
  static void setup() {
    var stopModel = TEST_MODEL
      .stopModelBuilder()
      .withRegularStop(STOP_A)
      .withRegularStop(STOP_B)
      .withStation(STATION)
      .build();

    var deduplicator = new Deduplicator();
    var transitModel = new TransitModel(stopModel, deduplicator);
    var canceledStopTimes = TEST_MODEL.stopTimesEvery5Minutes(3, TRIP, T11_30);
    var canceledTripTimes = TripTimesFactory.tripTimes(TRIP, canceledStopTimes, deduplicator);
    RAIL_PATTERN.add(canceledTripTimes);
    canceledTripTimes.cancelTrip();
    transitModel.addTripPattern(RAIL_PATTERN.getId(), RAIL_PATTERN);

    // Crate a calendar (needed for testing cancelled trips)
    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(
      CALENDAR_ID,
      List.of(LocalDate.of(2024, 8, 8), LocalDate.of(2024, 8, 9))
    );
    transitModel.getServiceCodes().put(CALENDAR_ID, 0);
    transitModel.updateCalendarServiceData(true, calendarServiceData, DataImportIssueStore.NOOP);

    transitModel.index();

    transitModel.initTimetableSnapshotProvider(() -> {
      TimetableSnapshot timetableSnapshot = new TimetableSnapshot();
      RealTimeTripTimes tripTimes = RealTimeTripTimes.of(
        ScheduledTripTimes
          .of()
          .withTrip(TransitModelForTest.trip("REAL_TIME_TRIP").build())
          .withDepartureTimes(new int[] { 0, 1 })
          .build()
      );
      timetableSnapshot.update(REAL_TIME_PATTERN, tripTimes, LocalDate.now());
      timetableSnapshot.update(RAIL_PATTERN, canceledTripTimes, LocalDate.now());

      return timetableSnapshot.commit();
    });

    service =
      new DefaultTransitService(transitModel) {
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
  void getCanceledTrips() {
    Collection<DatedTrip> canceledTrips = service.getCanceledTrips(null);
    assertEquals(
      "[DatedTrip[trip=Trip{F:123 RR123}, serviceDate=2024-08-08], DatedTrip[trip=Trip{F:123 RR123}, serviceDate=2024-08-09]]",
      canceledTrips.toString()
    );
  }
}
