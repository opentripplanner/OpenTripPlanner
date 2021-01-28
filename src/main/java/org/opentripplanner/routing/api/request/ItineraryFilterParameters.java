package org.opentripplanner.routing.api.request;


import org.opentripplanner.routing.algorithm.filterchain.filters.AddMinSafeTransferCostFilter;

import java.util.function.DoubleFunction;

/**
 * Group by Similarity filter parameters
 */
public class ItineraryFilterParameters {

  /**
   * Switch on to return all itineraries and mark filtered itineraries as deleted.
   */
  public boolean debug;

  /**
   * Keep ONE itinerary for each group with at least this part of the legs in common.
   * Default value is 0.85 (85%), use a value less than 0.50 to turn off.
   * @see org.opentripplanner.routing.algorithm.filterchain.ItineraryFilterChainBuilder#addGroupBySimilarity(org.opentripplanner.routing.algorithm.filterchain.GroupBySimilarity)
   */
  public double groupSimilarityKeepOne;

  /**
   * Keep {@link RoutingRequest#numItineraries} itineraries for each group with at least this part of the legs
   * in common.
   * Default value is 0.68 (68%), use a value less than 0.50 to turn off.
   * @see org.opentripplanner.routing.algorithm.filterchain.ItineraryFilterChainBuilder#addGroupBySimilarity(org.opentripplanner.routing.algorithm.filterchain.GroupBySimilarity)
   */
  public double groupSimilarityKeepNumOfItineraries;


  /**
   * If set greater than zero(0.0), an addition to the itinerary generalized-cost for "unsafe
   * transfers" is added to the generalized-cost for all itineraries. The extra cost is calculated
   * by first calculating a safe-transfer-time. Then the given factor is multiplied with the
   * difference between the actual and safe transfer time. The safe transfer time increase for
   * long long journeys.
   *
   * @see AddMinSafeTransferCostFilter for details on calculating the extra unsafe transfer cost.
   */
  public double minSafeTransferTimeFactor;

  /**
   * A relative maximum limit for the generalized cost for transit itineraries. The limit is a
   * linear function of the minimum generalized-cost. The minimum cost is lowest cost from the
   * set of all returned transit itineraries. The function is used to calculate a max-limit. The
   * max-limit is then used to to filter by generalized-cost. Transit itineraries with a cost
   * higher than the max-limit is dropped from the result set. None transit itineraries is
   * excluded from the filter.
   * <ul>
   * <li>To set a filter to be 1 hours plus 2 times the lowest cost use:
   * {@code 3600 + 2.0 x}
   * <li>To set an absolute value(3000) use: {@code 3000 + 0x}
   * </ul>
   * The default is {@code null} - no filter is applied.
   */
  public DoubleFunction<Double> transitGeneralizedCostLimit = null;


  private ItineraryFilterParameters() {
    this.debug = false;
    this.groupSimilarityKeepOne = 0.85;
    this.groupSimilarityKeepNumOfItineraries = 0.68;
    this.minSafeTransferTimeFactor = 0.0;
  }

  public static ItineraryFilterParameters createDefault() {
    return new ItineraryFilterParameters();
  }

  public ItineraryFilterParameters(
      boolean debug,
      double groupSimilarityKeepOne,
      double groupSimilarityKeepNumOfItineraries,
      double minSafeTransferTimeFactor,
      DoubleFunction<Double> transitGeneralizedCostLimit
  ) {
    this.debug = debug;
    this.groupSimilarityKeepOne = groupSimilarityKeepOne;
    this.groupSimilarityKeepNumOfItineraries = groupSimilarityKeepNumOfItineraries;
    this.minSafeTransferTimeFactor = minSafeTransferTimeFactor;
    this.transitGeneralizedCostLimit = transitGeneralizedCostLimit;
  }
}
