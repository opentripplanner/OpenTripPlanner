package org.opentripplanner.transit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;

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

  @BeforeAll
  static void setup() {
    var stopModel = TEST_MODEL
      .stopModelBuilder()
      .withRegularStop(STOP_A)
      .withRegularStop(STOP_B)
      .withStation(STATION)
      .build();

    var transitModel = new TransitModel(stopModel, new Deduplicator());
    transitModel.addTripPattern(RAIL_PATTERN.getId(), RAIL_PATTERN);
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
}
