package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;

/**
 * This interface allows us to set ONE extension for adding extra cost to an {@link StreetEdge}.
 */
public interface StreetEdgeCostExtension {
  /**
   * This is method is called from the street edge and allows an extension to add extra cost.
   *
   * @return zero(0) - no extra cost is added, or a positive value.
   */
  double calculateExtraCost(State state, int edgeLength, TraverseMode traverseMode);
}
