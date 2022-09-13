package org.opentripplanner.transit.raptor.api.transit;

/**
 * This class exists purely due to performance reasons. By declaring the getters as final, we avoid
 * the itable stub to decide which RaptorTransfer implementation to use when fetching performance
 * critical fields from an transfer.
 */
public abstract class AbstractRaptorTransfer implements RaptorTransfer {

  private final int stop;
  private final int durationInSeconds;
  private final int generalizedCost;

  protected AbstractRaptorTransfer(int stop, int durationInSeconds, int generalizedCost) {
    this.stop = stop;
    this.durationInSeconds = durationInSeconds;
    this.generalizedCost = generalizedCost;
  }

  @Override
  public final int stop() {
    return stop;
  }

  @Override
  public final int durationInSeconds() {
    return durationInSeconds;
  }

  @Override
  public final int generalizedCost() {
    return generalizedCost;
  }
}
