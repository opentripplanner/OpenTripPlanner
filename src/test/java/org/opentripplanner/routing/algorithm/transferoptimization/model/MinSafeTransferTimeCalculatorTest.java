package org.opentripplanner.routing.algorithm.transferoptimization.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.MinSafeTransferTimeCalculator.bound;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.COST_CALCULATOR;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.SLACK_PROVIDER;
import static org.opentripplanner.util.time.TimeUtils.time;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.api.TestPathBuilder;
import org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.util.time.DurationUtils;


public class MinSafeTransferTimeCalculatorTest implements RaptorTestConstants {
  private static final int D2m = DurationUtils.durationInSeconds("2m");
  private static final int TRANSIT_TIME = 2000 - (BOARD_SLACK + ALIGHT_SLACK);

  private static final TestPathBuilder
          PATH_BUILDER = new TestPathBuilder(ALIGHT_SLACK, COST_CALCULATOR);

  Path<TestTripSchedule> path_1_bus_leg = PATH_BUILDER
      .access(time("10:00:15"), D2m, STOP_A)
      .bus("L11", time("10:03"), TRANSIT_TIME, STOP_B)
      .egress(D2m);

  Path<TestTripSchedule> path_3_bus_legs = BasicPathTestCase.basicTripAsPath();

  private final MinSafeTransferTimeCalculator<TestTripSchedule> subject
      = new MinSafeTransferTimeCalculator<>(SLACK_PROVIDER);

  @Test
  public void testMinSafeTransferTimeOneTransit() {
    assertEquals(
        133,
        subject.minSafeTransferTime(List.of(path_1_bus_leg))
    );
  }

  @Test
  public void testMinSafeTransferTimeThreeTransits() {
    assertEquals(
        276,
        subject.minSafeTransferTime(List.of(path_3_bus_legs))
    );
  }

  @Test
  public void testMinSafeTransferTimeBothPaths() {
    assertEquals(
        133,
        subject.minSafeTransferTime(List.of(path_1_bus_leg, path_3_bus_legs))
    );
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