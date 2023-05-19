package org.opentripplanner.raptor.api.request;

import org.opentripplanner.raptor.api.model.DominanceFunction;

public interface C2Request {
  /**
   * C2 Dominance function to be used for the request
   */
  DominanceFunction dominanceFunction();
}
