package org.opentripplanner.raptorlegacy._data.transit;

import static org.opentripplanner.raptor.api.model.RaptorCostConverter.toRaptorCost;

import java.util.EnumSet;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultRaptorTransfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.api.request.StreetMode;

/**
 * Simple factory to create {@link DefaultRaptorTransfer}s for unit-testing.
 * <p>
 * <b>Note!</b> The created transfer does NOT have a AStar path (list of edges).
 */
public final class TestTransfers {

  public static final double DEFAULT_WALK_RELUCTANCE = 2.0;

  /** This is a utility class, should not be instansiated */
  private TestTransfers() {}

  public static DefaultRaptorTransfer transfer(int stop, int durationInSeconds, int cost) {
    var tx = new Transfer(
      stop,
      (int) Math.round(durationInSeconds * 1.3),
      EnumSet.of(StreetMode.WALK)
    );
    return new DefaultRaptorTransfer(stop, durationInSeconds, cost, tx);
  }

  public static DefaultRaptorTransfer transfer(int stop, int durationInSeconds) {
    return transfer(stop, durationInSeconds, walkCost(durationInSeconds));
  }

  public static int walkCost(int durationInSeconds) {
    return walkCost(durationInSeconds, DEFAULT_WALK_RELUCTANCE);
  }

  public static int walkCost(int durationInSeconds, double reluctance) {
    return toRaptorCost(durationInSeconds * reluctance);
  }
}
