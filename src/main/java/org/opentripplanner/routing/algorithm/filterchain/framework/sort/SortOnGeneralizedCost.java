package org.opentripplanner.routing.algorithm.filterchain.framework.sort;

import org.opentripplanner.framework.collection.CompositeComparator;
import org.opentripplanner.model.plan.ItinerarySortKey;

/**
 * This comparator sorts itineraries based on the generalized-cost. If the cost is the same then the
 * filter pick the itinerary with the lowest number-of-transfers.
 */
public class SortOnGeneralizedCost extends CompositeComparator<ItinerarySortKey> {

  public SortOnGeneralizedCost() {
    super(SortOrderComparator.GENERALIZED_COST_COMP, SortOrderComparator.NUM_OF_TRANSFERS_COMP);
  }
}
