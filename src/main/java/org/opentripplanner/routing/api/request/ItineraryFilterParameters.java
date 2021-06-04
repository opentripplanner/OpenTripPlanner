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
   * The default is {@code 3600 + 2x} - 1 hours plus 2 times the lowest cost.
   */
  public DoubleFunction<Double> transitGeneralizedCostLimit;

  /**
   * This is used to filter out bike rental itineraries that contain mostly walking. The value
   * describes the ratio of the total itinerary that has to consist of bike rental to allow the
   * itinerary.
   *
   * Default value is off (0). If you want a minimum of 30% cycling, use a value of 0.3.
   */
  public double bikeRentalDistanceRatio;

  /**
   * This is used to filter out park and ride itineraries that contain only driving plus a very
   * long walk. The value describes the ratio of the total itinerary duration that has to consist
   * of driving to allow the itinerary.
   *
   * Default value is 0.3 (30%), use a value of 0 to turn off.
   */
  public double parkAndRideDurationRatio;

  /**
   * This is a a bit similar to {@link #transitGeneralizedCostLimit}, with
   * a few important differences.
   *
   * This function is used to compute a max-limit for generalized-cost. The limit
   * is applied to itineraries with no transit legs, however ALL itineraries (including those with
   * transit legs) are considered when calculating the minimum cost.
   * <p>
   * The smallest generalized-cost value is used as input to the function.
   * For example if the function is {@code f(x) = 1800 + 2.0 x} and the smallest cost is
   * {@code 5000}, then all non-transit itineraries with a cost larger than
   * {@code 1800 + 2 * 5000 = 11 800} is dropped.
   *
   * The default is {@code 3600 + 2x} - 1 hours plus 2 times the lowest cost.
   */
  public DoubleFunction<Double> nonTransitGeneralizedCostLimit;


  private ItineraryFilterParameters() {
    this.debug = false;
    this.groupSimilarityKeepOne = 0.85;
    this.groupSimilarityKeepNumOfItineraries = 0.68;
    this.minSafeTransferTimeFactor = 0.0;
    this.bikeRentalDistanceRatio = 0.0;
    this.parkAndRideDurationRatio = 0.0;
    this.transitGeneralizedCostLimit =
        RequestFunctions.createLinearFunction(3600, 2);
    this.nonTransitGeneralizedCostLimit =
        RequestFunctions.createLinearFunction(3600, 2);
  }

  public static ItineraryFilterParameters createDefault() {
    return new ItineraryFilterParameters();
  }

  public ItineraryFilterParameters(
      boolean debug,
      double groupSimilarityKeepOne,
      double groupSimilarityKeepNumOfItineraries,
      double minSafeTransferTimeFactor,
      DoubleFunction<Double> transitGeneralizedCostLimit,
      DoubleFunction<Double> nonTransitGeneralizedCostLimit,
      double bikeRentalDistanceRatio,
      double parkAndRideDurationRatio
  ) {
    this.debug = debug;
    this.groupSimilarityKeepOne = groupSimilarityKeepOne;
    this.groupSimilarityKeepNumOfItineraries = groupSimilarityKeepNumOfItineraries;
    this.minSafeTransferTimeFactor = minSafeTransferTimeFactor;
    this.transitGeneralizedCostLimit = transitGeneralizedCostLimit;
    this.nonTransitGeneralizedCostLimit = nonTransitGeneralizedCostLimit;
    this.bikeRentalDistanceRatio = bikeRentalDistanceRatio;
    this.parkAndRideDurationRatio = parkAndRideDurationRatio;
  }
}
