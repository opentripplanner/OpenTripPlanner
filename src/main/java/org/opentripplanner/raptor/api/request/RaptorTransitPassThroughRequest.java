package org.opentripplanner.raptor.api.request;

import org.opentripplanner.raptor.api.model.DominanceFunction;

public class RaptorTransitPassThroughRequest implements C2Request {

  private final PassThroughPoints passThroughPoints;

  public RaptorTransitPassThroughRequest(final PassThroughPoints passThroughPoints) {
    this.passThroughPoints = passThroughPoints;
  }

  public PassThroughPoints passThroughPoints() {
    return passThroughPoints;
  }

  /**
   * This is the dominance function to use for comparing transit-priority-groupIds.
   * It is critical that the implementation is "static" so it can be inlined, since it
   * is run in the innermost loop of Raptor.
   */
  @Override
  public DominanceFunction dominanceFunction() {
    return (left, right) -> left > right;
  }
}
