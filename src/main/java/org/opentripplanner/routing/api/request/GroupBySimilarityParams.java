package org.opentripplanner.routing.api.request;

import org.opentripplanner.routing.algorithm.filterchain.AdjustedCost;


/**
 * Group by Similarity filter parameters
 */
public class GroupBySimilarityParams {

  public static GroupBySimilarityParams DEFAULT = new GroupBySimilarityParams();

  /**
   * Keep ONE itinerary for each group with at least this part of the legs in common.
   * Default value is 0.85 (85%), use a value less than 0.50 to turn off.
   * @see org.opentripplanner.routing.algorithm.filterchain.ItineraryFilterChainBuilder#addGroupBySimilarity(org.opentripplanner.routing.algorithm.filterchain.GroupBySimilarity)
   */
  public final double keepOne;

  /**
   * Keep {@link RoutingRequest#numItineraries} itineraries for each group with at least this part of the legs
   * in common.
   * Default value is 0.68 (68%), use a value less than 0.50 to turn off.
   * @see org.opentripplanner.routing.algorithm.filterchain.ItineraryFilterChainBuilder#addGroupBySimilarity(org.opentripplanner.routing.algorithm.filterchain.GroupBySimilarity)
   */
  public final double keepNumOfItineraries;


  /**
   * If set greater than zero(0.0), an {@link AdjustedCost} is used in the group-by-similarity
   * filter instead of the itinerary generalized-cost. The adjusted-cost will add a penalty
   * to short transfer times for long journeys.
   *
   * @see AdjustedCost for details on calculating the adjusted-cost.
   */
  public final double idealTransferTimeFactor;


  private GroupBySimilarityParams() {
    this.keepOne = 0.85;
    this.keepNumOfItineraries = 0.68;
    this.idealTransferTimeFactor = 0.0;
  }

  public GroupBySimilarityParams(
      double keepOne,
      double keepNumOfItineraries,
      double idealTransferTimeFactor
  ) {
    this.keepOne = keepOne;
    this.keepNumOfItineraries = keepNumOfItineraries;
    this.idealTransferTimeFactor = idealTransferTimeFactor;
  }
}
