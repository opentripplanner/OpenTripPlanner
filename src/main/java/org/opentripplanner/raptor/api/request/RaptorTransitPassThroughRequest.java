package org.opentripplanner.raptor.api.request;

import org.opentripplanner.raptor.api.model.DominanceFunction;

public class RaptorTransitPassThroughRequest {

  private final PassThroughPointsService passThroughPointsService;

  public RaptorTransitPassThroughRequest(final PassThroughPointsService passThroughPointsService) {
    this.passThroughPointsService = passThroughPointsService;
  }

  // TODO: Should this method be part of the interface?
  public PassThroughPointsService passThroughPoints() {
    return passThroughPointsService;
  }

  /**
   * This is the dominance function to use for comparing transit-priority-groupIds.
   * It is critical that the implementation is "static" so it can be inlined, since it
   * is run in the innermost loop of Raptor.
   */
  public DominanceFunction dominanceFunction() {
    return (left, right) -> left > right;
  }
}
