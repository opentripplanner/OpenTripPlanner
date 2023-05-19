package org.opentripplanner.raptor.api.request;

import java.util.HashSet;
import org.opentripplanner.raptor.api.model.DominanceFunction;

public class RaptorTransitViaRequest implements C2Request {

  HashSet<Integer> viaPoints = null;

  // TODO: 2023-05-19 we haven't decided yet how this should look
  //  Probably not a HashSet<Integer>
  public HashSet<Integer> viaPoints() {
   return viaPoints;
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
