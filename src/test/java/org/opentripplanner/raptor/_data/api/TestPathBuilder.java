package org.opentripplanner.raptor._data.api;

import static org.opentripplanner.raptor.rangeraptor.transit.TripTimesSearch.findTripTimes;

import javax.annotation.Nullable;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripPattern;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.api.path.PathBuilder;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;

/**
 * Utility to help build paths for testing. The path builder is "reusable", every time the {@code
 * access(...)} methods are called the builder reset it self.
 * <p>
 * If the {@code costCalculator} is null, paths will not include cost.
 */
public class TestPathBuilder {

  private static final int BOARD_ALIGHT_OFFSET = 30;

  @Nullable
  private final CostCalculator<TestTripSchedule> costCalculator;

  private final int alightSlack;
  private PathBuilder<TestTripSchedule> builder;
  private int startTime;

  public TestPathBuilder(
    int alightSlack,
    @Nullable CostCalculator<TestTripSchedule> costCalculator
  ) {
    this.alightSlack = alightSlack;
    this.costCalculator = costCalculator;
  }

  /**
   * Create access starting at the fixed given {@code starting}. Opening hours is used to enforce
   * the access start time and prevent time-shifting it.
   */
  public TestPathBuilder access(int startTime, int duration, int toStop) {
    return access(startTime, TestAccessEgress.walk(toStop, duration, startTime, startTime));
  }

  /**
   * Create access with the given {@code startTime}, but allow the access to be time-shifted
   * according to the opening hours of the given {@code transfer}.
   */
  public TestPathBuilder access(int startTime, TestAccessEgress transfer) {
    reset(startTime);
    builder.access(transfer);
    return this;
  }

  public TestPathBuilder walk(int duration, int toStop) {
    return walk(TestTransfer.transfer(toStop, duration));
  }

  public TestPathBuilder walk(int duration, int toStop, int cost) {
    return walk(TestTransfer.transfer(toStop, duration, cost));
  }

  public TestPathBuilder walk(TestTransfer transfer) {
    builder.transfer(transfer, transfer.stop());
    return this;
  }

  public TestPathBuilder bus(TestTripSchedule trip, int alightStop) {
    int boardStop = currentStop();
    // We use the startTime as earliest-board-time, this may cause problems for
    // testing routes visiting the same stop more than once. Create a new factory
    // method if this happens.
    var baTime = findTripTimes(trip, boardStop, alightStop, startTime);
    builder.transit(trip, baTime);
    return this;
  }

  public TestPathBuilder bus(String patternName, int fromTime, int duration, int toStop) {
    int toTime = fromTime + duration;
    int fromStop = currentStop();

    TestTripSchedule trip = TestTripSchedule
      .schedule(TestTripPattern.pattern(patternName, fromStop, toStop))
      .arrDepOffset(BOARD_ALIGHT_OFFSET)
      .departures(fromTime, toTime + BOARD_ALIGHT_OFFSET)
      .build();

    return bus(trip, toStop);
  }

  public Path<TestTripSchedule> egress(int duration) {
    return egress(TestAccessEgress.walk(currentStop(), duration));
  }

  public Path<TestTripSchedule> egress(TestAccessEgress transfer) {
    builder.egress(transfer);
    return builder.build(startTime);
  }

  /* private methods */

  int currentStop() {
    return builder.tail().toStop();
  }

  private void reset(int startTime) {
    this.startTime = startTime;
    this.builder =
      PathBuilder.tailPathBuilder(
        null,
        RaptorSlackProvider.defaultSlackProvider(0, 0, alightSlack),
        costCalculator,
        RaptorStopNameResolver.nullSafe(null)
      );
  }
}
