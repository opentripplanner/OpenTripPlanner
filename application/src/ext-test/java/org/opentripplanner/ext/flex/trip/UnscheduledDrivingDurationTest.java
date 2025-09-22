package org.opentripplanner.ext.flex.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model._data.StreetModelForTest.V1;
import static org.opentripplanner.street.model._data.StreetModelForTest.V2;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.LineStrings;
import org.opentripplanner.ext.flex.FlexStopTimesForTest;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.routing.api.request.framework.TimePenalty;

class UnscheduledDrivingDurationTest {

  static final FlexPathCalculator STATIC_CALCULATOR = (
    fromv,
    tov,
    boardStopPosition,
    alightStopPosition
  ) -> new FlexPath(10_000, (int) Duration.ofMinutes(10).toSeconds(), () -> LineStrings.SIMPLE);
  private static final StopTime STOP_TIME = FlexStopTimesForTest.area("10:00", "18:00");

  @Test
  void noPenalty() {
    var trip = UnscheduledTrip.of(id("1")).withStopTimes(List.of(STOP_TIME, STOP_TIME)).build();

    var calculator = trip.decorateFlexPathCalculator(STATIC_CALCULATOR);
    var path = calculator.calculateFlexPath(V1, V2, 0, 0);
    assertEquals(600, path.durationSeconds);
  }

  @Test
  void withPenalty() {
    var trip = UnscheduledTrip.of(id("1"))
      .withStopTimes(List.of(STOP_TIME, STOP_TIME))
      .withTimePenalty(TimePenalty.of(Duration.ofMinutes(2), 1.5f))
      .build();

    var calculator = trip.decorateFlexPathCalculator(STATIC_CALCULATOR);
    var path = calculator.calculateFlexPath(V1, V2, 0, 0);
    assertEquals(1020, path.durationSeconds);
  }
}
