package org.opentripplanner.ext.flex.trip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.flex.FlexStopTimesForTest.area;
import static org.opentripplanner.ext.flex.FlexStopTimesForTest.areaWithContinuousStopping;
import static org.opentripplanner.ext.flex.FlexStopTimesForTest.regularStop;
import static org.opentripplanner.ext.flex.FlexStopTimesForTest.regularStopWithContinuousStopping;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.StopTime;

class ScheduledDeviatedTripTest {

  private static List<List<StopTime>> isScheduledDeviatedTripCases() {
    return List.of(
      List.of(
        regularStop("10:10"),
        area("10:20", "10:30"),
        regularStop("10:40"),
        area("10:50", "11:00")
      ),
      List.of(
        regularStopWithContinuousStopping("10:10"),
        area("10:20", "10:30"),
        regularStopWithContinuousStopping("10:40"),
        area("10:50", "11:00")
      )
    );
  }

  @ParameterizedTest
  @MethodSource("isScheduledDeviatedTripCases")
  void isScheduledDeviatedTrip(List<StopTime> stopTimes) {
    assertTrue(ScheduledDeviatedTrip.isScheduledDeviatedFlexTrip(stopTimes));
  }

  private static List<List<StopTime>> isNotScheduledDeviatedTripCases() {
    return List.of(
      List.of(
        areaWithContinuousStopping("10:10"),
        regularStop("10:20", "10:30"),
        areaWithContinuousStopping("10:40"),
        regularStop("10:50", "11:00")
      ),
      List.of(regularStop("10:10"), regularStop("10:20"))
    );
  }

  @ParameterizedTest
  @MethodSource("isNotScheduledDeviatedTripCases")
  void isNotScheduledDeviatedTrip(List<StopTime> stopTimes) {
    assertFalse(ScheduledDeviatedTrip.isScheduledDeviatedFlexTrip(stopTimes));
  }
}
