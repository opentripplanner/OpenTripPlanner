package org.opentripplanner.routing.algorithm.transferoptimization.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.time.TimeUtils.time;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.C1_CALCULATOR;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.MinSafeTransferTimeCalculator.bound;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.TestPathBuilder;
import org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;

public class MinSafeTransferTimeCalculatorTest implements RaptorTestConstants {

  private static final int D2m = DurationUtils.durationInSeconds("2m");
  private static final int TRANSIT_TIME = 2000 - (BOARD_SLACK + ALIGHT_SLACK);
  private static final TestPathBuilder PATH_BUILDER = new TestPathBuilder(C1_CALCULATOR);

  private final MinSafeTransferTimeCalculator<TestTripSchedule> subject = new MinSafeTransferTimeCalculator<>(
    SLACK_PROVIDER
  );
  RaptorPath<TestTripSchedule> path_1_bus_leg = PATH_BUILDER
    .access(time("10:00:15"), STOP_A, D2m)
    .bus("L11", time("10:03"), TRANSIT_TIME, STOP_B)
    .egress(D2m);
  RaptorPath<TestTripSchedule> path_3_bus_legs = BasicPathTestCase.basicTripAsPath();

  @Test
  public void testMinSafeTransferTimeOneTransit() {
    assertEquals(133, subject.minSafeTransferTime(List.of(path_1_bus_leg)));
  }

  @Test
  public void testMinSafeTransferTimeThreeTransits() {
    assertEquals(276, subject.minSafeTransferTime(List.of(path_3_bus_legs)));
  }

  @Test
  public void testMinSafeTransferTimeBothPaths() {
    assertEquals(133, subject.minSafeTransferTime(List.of(path_1_bus_leg, path_3_bus_legs)));
  }

  @Test
  public void testBound() {
    assertEquals(1, bound(0, 1, 3));
    assertEquals(1, bound(1, 1, 3));
    assertEquals(2, bound(2, 1, 3));
    assertEquals(3, bound(3, 1, 3));
    assertEquals(3, bound(4, 1, 3));
  }
}
