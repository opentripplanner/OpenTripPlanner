package org.opentripplanner.raptorlegacy._data.api;

import static org.opentripplanner.raptor.rangeraptor.transit.TripTimesSearch.findTripTimes;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.path.PathBuilder;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.raptorlegacy._data.transit.TestAccessEgress;
import org.opentripplanner.raptorlegacy._data.transit.TestTransfer;
import org.opentripplanner.raptorlegacy._data.transit.TestTripPattern;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;

/**
 * Utility to help build paths for testing. The path builder is "reusable", every time the {@code
 * access(...)} methods are called the builder reset it self.
 * <p>
 * If the {@code costCalculator} is null, paths will not include cost.
 *
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
public class TestPathBuilder implements RaptorTestConstants {

  private static final int BOARD_ALIGHT_OFFSET = 30;

  @Nullable
  private final RaptorCostCalculator<TestTripSchedule> costCalculator;

  private final RaptorSlackProvider slackProvider;
  private PathBuilder<TestTripSchedule> builder;
  private int startTime;
  private int c2 = RaptorConstants.NOT_SET;

  public TestPathBuilder(
    RaptorSlackProvider slackProvider,
    @Nullable RaptorCostCalculator<TestTripSchedule> costCalculator
  ) {
    this.slackProvider = slackProvider;
    this.costCalculator = costCalculator;
  }

  /**
   * Uses the slacks in {@link RaptorTestConstants}.
   */
  public TestPathBuilder(@Nullable RaptorCostCalculator<TestTripSchedule> costCalculator) {
    this(new DefaultSlackProvider(TRANSFER_SLACK, BOARD_SLACK, ALIGHT_SLACK), costCalculator);
  }

  /** Assign c2 value for path. TODO: Add c2 value for each leg. */
  public TestPathBuilder c2(int c2) {
    this.c2 = c2;
    return this;
  }

  /**
   * Create access starting at the fixed given {@code starting}. Opening hours is used to enforce
   * the access start time and prevent time-shifting it.
   */
  public TestPathBuilder access(int startTime, int toStop, int duration) {
    return access(startTime, TestAccessEgress.walk(toStop, duration));
  }

  /** Same as {@link #access(int, int, int)} , but with a free access - duration is 0s. */
  public TestPathBuilder access(int startTime, int toStop) {
    return access(startTime, TestAccessEgress.free(toStop));
  }

  /**
   * Create access with the given {@code startTime}, but allow the access to be time-shifted
   * according to the opening hours of the given {@code transfer}.
   */
  private TestPathBuilder access(int startTime, RaptorAccessEgress transfer) {
    reset(startTime);
    builder.access(transfer);
    return this;
  }

  public TestPathBuilder walk(int duration, int toStop) {
    return walk(TestTransfer.transfer(toStop, duration));
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

  public RaptorPath<TestTripSchedule> egress(int duration) {
    return egress(
      duration == 0
        ? TestAccessEgress.free(currentStop())
        : TestAccessEgress.walk(currentStop(), duration)
    );
  }

  public RaptorPath<TestTripSchedule> egress(RaptorAccessEgress egress) {
    builder.egress(egress);
    builder.c2(c2);
    return builder.build();
  }

  /* private methods */

  int currentStop() {
    return builder.tail().toStop();
  }

  private void reset(int startTime) {
    this.startTime = startTime;
    this.builder =
      PathBuilder.tailPathBuilder(
        slackProvider,
        startTime,
        costCalculator,
        RaptorStopNameResolver.nullSafe(null),
        null
      );
  }
}
