package org.opentripplanner.transit.model.timetable;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;

class TripTimesTest {

  private static final NonLocalizedString DIRECTION = new NonLocalizedString("DIRECTION");
  private static final StopTime EMPTY_STOPPOINT = new StopTime();
  private static final NonLocalizedString STOP_TEST_DIRECTION = new NonLocalizedString(
    "STOP TEST DIRECTION"
  );

  @Test
  void shouldHandleBothNullScenario() {
    Trip trip = TransitModelForTest.trip("TRIP").build();
    Collection<StopTime> stopTimes = List.of(EMPTY_STOPPOINT, EMPTY_STOPPOINT, EMPTY_STOPPOINT);

    TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

    I18NString headsignFirstStop = tripTimes.getHeadsign(0);
    Assertions.assertNull(headsignFirstStop);
  }

  @Test
  void shouldHandleTripOnlyHeadSignScenario() {
    Trip trip = TransitModelForTest.trip("TRIP")
      .withHeadsign(DIRECTION)
      .build();
    Collection<StopTime> stopTimes = List.of(EMPTY_STOPPOINT, EMPTY_STOPPOINT, EMPTY_STOPPOINT);

    TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

    I18NString headsignFirstStop = tripTimes.getHeadsign(0);
    Assertions.assertEquals(DIRECTION, headsignFirstStop);
  }

  @Test
  void shouldHandleStopsOnlyHeadSignScenario() {
    Trip trip = TransitModelForTest.trip("TRIP").build();
    StopTime stopWithHeadsign = new StopTime();
    stopWithHeadsign.setStopHeadsign(STOP_TEST_DIRECTION);
    Collection<StopTime> stopTimes = List.of(stopWithHeadsign, stopWithHeadsign, stopWithHeadsign);

    TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

    I18NString headsignFirstStop = tripTimes.getHeadsign(0);
    Assertions.assertEquals(STOP_TEST_DIRECTION, headsignFirstStop);
  }

  @Test
  void shouldHandleStopsEqualToTripHeadSignScenario() {
    Trip trip = TransitModelForTest.trip("TRIP")
      .withHeadsign(DIRECTION)
      .build();
    StopTime stopWithHeadsign = new StopTime();
    stopWithHeadsign.setStopHeadsign(DIRECTION);
    Collection<StopTime> stopTimes = List.of(stopWithHeadsign, stopWithHeadsign, stopWithHeadsign);

    TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

    I18NString headsignFirstStop = tripTimes.getHeadsign(0);
    Assertions.assertEquals(DIRECTION, headsignFirstStop);
  }

  @Test
  void shouldHandleDifferingTripAndStopHeadSignScenario() {
    Trip trip = TransitModelForTest.trip("TRIP")
      .withHeadsign(DIRECTION)
      .build();
    StopTime stopWithHeadsign = new StopTime();
    stopWithHeadsign.setStopHeadsign(STOP_TEST_DIRECTION);
    Collection<StopTime> stopTimes = List.of(stopWithHeadsign, EMPTY_STOPPOINT, EMPTY_STOPPOINT);

    TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

    I18NString headsignFirstStop = tripTimes.getHeadsign(0);
    Assertions.assertEquals(STOP_TEST_DIRECTION, headsignFirstStop);

    I18NString headsignSecondStop = tripTimes.getHeadsign(1);
    Assertions.assertEquals(DIRECTION, headsignSecondStop);
  }
}
