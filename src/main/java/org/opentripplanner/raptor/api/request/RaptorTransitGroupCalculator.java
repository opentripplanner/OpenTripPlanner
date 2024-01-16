package org.opentripplanner.raptor.api.request;

import org.opentripplanner.raptor.api.model.DominanceFunction;

public interface RaptorTransitGroupCalculator {
  /**
   * Merge in the transit group id with an existing set. Note! Both the set
   * and the group id type is {@code int}.
   *
   * @param currentGroupIds the set of groupIds for all legs in a path.
   * @param boardingGroupId the transit group id to add to the given set.
   * @return the new computed set of groupIds
   */
  int mergeGroupIds(int currentGroupIds, int boardingGroupId);

  /**
   * This is the dominance function to use for comparing transit-groups.
   * It is critical that the implementation is "static" so it can be inlined, since it
   * is run in the innermost loop of Raptor.
   */
  DominanceFunction dominanceFunction();
}
