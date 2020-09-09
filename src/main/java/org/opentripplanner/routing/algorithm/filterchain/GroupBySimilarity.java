package org.opentripplanner.routing.algorithm.filterchain;

/**
 * Group itineraries by similarity and reduce the number of itineraries down to an approximate
 * minimum number of itineraries for each group.
 * <p>
 * THIS CLASS IS USED BY THE {@link ItineraryFilterChainBuilder} ONLY, HENCE PACKAGE LOCAL.
 */
class GroupBySimilarity {

  /**
   * The minimum similarity percentage used to group itineraries.
   * <p>
   * The value must be a number between 0.01 (1%) and 0.99 (99%).
   */
  public final double groupByP;

  /**
   * Set a guideline for the minimum total number of itineraries to keep. For example, if there is 2
   * groups and the min-limit is 3, the filter will keep 2 samples in each group, keeping up to 4
   * itineraries(approximately 3). Set this to 1 to keep ONE itinerary per group.
   */
  public final int approximateMinLimit;

  public GroupBySimilarity(double groupByP, int approximateMinLimit) {
    this.groupByP = groupByP;
    this.approximateMinLimit = approximateMinLimit;
  }
}
