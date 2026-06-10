package org.opentripplanner.raptorlegacy._data.transit;

import static org.opentripplanner.raptor.spi.RaptorCostConverter.toRaptorCost;

/**
 * Simple factory to create {@link TestTransfer}s for unit-testing.
 * <p>
 * <b>Note!</b> The created transfer does NOT have a AStar path (list of edges).
 */
public final class TestTransfers {

  public static final double DEFAULT_WALK_RELUCTANCE = 2.0;

  /** This is a utility class, should not be instansiated */
  private TestTransfers() {}

  public static TestTransfer transfer(int stop, int durationInSeconds, int cost) {
    return new TestTransfer(stop, durationInSeconds, cost);
  }

  public static TestTransfer transfer(int stop, int durationInSeconds) {
    return transfer(stop, durationInSeconds, walkCost(durationInSeconds));
  }

  public static int walkCost(int durationInSeconds) {
    return walkCost(durationInSeconds, DEFAULT_WALK_RELUCTANCE);
  }

  public static int walkCost(int durationInSeconds, double reluctance) {
    return toRaptorCost(durationInSeconds * reluctance);
  }
}
