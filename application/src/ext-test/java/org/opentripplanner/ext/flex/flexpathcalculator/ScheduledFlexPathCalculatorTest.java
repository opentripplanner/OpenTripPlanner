package org.opentripplanner.ext.flex.flexpathcalculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.flex.FlexStopTimesForTest.area;
import static org.opentripplanner.ext.flex.FlexStopTimesForTest.regularStop;
import static org.opentripplanner.street.model._data.StreetModelForTest.V1;
import static org.opentripplanner.street.model._data.StreetModelForTest.V2;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.LineStrings;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;

class ScheduledFlexPathCalculatorTest {

  private static final ScheduledDeviatedTrip TRIP = ScheduledDeviatedTrip.of(id("123"))
    .withStopTimes(
      List.of(
        regularStop("10:00", "10:01"),
        area("10:10", "10:20"),
        regularStop("10:25", "10:26"),
        area("10:40", "10:50")
      )
    )
    .build();

  @Test
  void calculateTime() {
    var c = (FlexPathCalculator) (fromv, tov, boardStopPosition, alightStopPosition) ->
      new FlexPath(10_000, (int) Duration.ofMinutes(10).toSeconds(), () -> LineStrings.SIMPLE);
    var calc = new ScheduledFlexPathCalculator(c, TRIP);
    var path = calc.calculateFlexPath(V1, V2, 0, 1);
    assertEquals(Duration.ofMinutes(19), Duration.ofSeconds(path.durationSeconds));
  }
}
