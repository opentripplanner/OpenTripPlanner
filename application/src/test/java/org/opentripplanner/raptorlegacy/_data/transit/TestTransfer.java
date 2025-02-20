package org.opentripplanner.raptorlegacy._data.transit;

import static org.opentripplanner.raptor.api.model.RaptorCostConverter.toRaptorCost;

import org.opentripplanner.raptor.api.model.RaptorTransfer;

/**
 * Simple implementation for {@link RaptorTransfer} for use in unit-tests.
 *
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
public record TestTransfer(int stop, int durationInSeconds, int cost) implements RaptorTransfer {
  public static final double DEFAULT_WALK_RELUCTANCE = 2.0;

  public static TestTransfer transfer(int stop, int durationInSeconds) {
    return new TestTransfer(stop, durationInSeconds, walkCost(durationInSeconds));
  }

  public static int walkCost(int durationInSeconds) {
    return walkCost(durationInSeconds, DEFAULT_WALK_RELUCTANCE);
  }

  public static int walkCost(int durationInSeconds, double reluctance) {
    return toRaptorCost(durationInSeconds * reluctance);
  }

  @Override
  public int stop() {
    return stop;
  }

  @Override
  public int c1() {
    return cost;
  }

  @Override
  public int durationInSeconds() {
    return durationInSeconds;
  }

  @Override
  public String toString() {
    return asString();
  }
}
