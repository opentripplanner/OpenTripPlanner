package org.opentripplanner.routing.algorithm.filterchain;

import org.opentripplanner.model.plan.Itinerary;

import java.util.List;


/**
 * Configure the filter-chain by passing in this set of parameters. Set these parameters
 * in the request or in config.
 */
public interface FilterChainParameters {

  /**
   * Used to set the correct sort order. This si the same flag as the
   * {@link org.opentripplanner.routing.api.request.RoutingRequest#arriveBy}.
   */
  boolean arriveBy();

  /**
   * The maximum number of itineraries returned. This will remove all itineraries at the
   * end of the list AFTER the final sort of the itineraries.
   * <p>
   * Default value is 20. Use {@code -1} to disable.
   */
  default int maxNumberOfItineraries() { return 20; }

  /**
   * Group itineraries by the main legs and keeping approximately the given total number of
   * itineraries. The itineraries are grouped by the legs that account for more then 'p' % for the
   * total distance.
   * <p/>
   * If the time-table-view is enabled, the result may contain similar itineraries where only the
   * first and/or last legs are different. This can happen by walking to/from another stop,
   * saving time, but getting a higher generalized-cost; Or, by taking a short ride.
   * Use {@code groupByP} in the range {@code 0.80-0.90} and {@code approximateMinLimit=1} will
   * remove these itineraries an keep only the itineraries with the lowest generalized-cost.
   * <p>
   * When this filter is enabled, itineraries are grouped by the "main" transit legs. Short legs
   * are skipped. Than for each group of itineraries the itinerary with the lowest generalized-cost
   * is kept. All other itineraries are dropped.
   * <p>
   * A good way to allow for some variation is to include several entries, relaxing the min-limit,
   * while tightening the group-by-p criteria. For example:
   * <pre>
   * groupByP | minLimit | Description
   *   0.90   |    1     | Keep 1 itinerary where 90% of the legs are the same
   *   0.80   |    2     | Keep 2 itineraries where 80% of the legs are the same
   *   0.68   |    3     | Keep 3 itineraries where 68% of the legs are the same
   * </pre>
   * Normally, we want some variation, so a good value to use for this parameter is the combined
   * cost of board- and alight-cost including indirect cost from board- and alight-slack.
   */
  List<GroupBySimilarity> groupBySimilarity();


  /**
   * The direct street search(walk, bicycle, car) is not pruning the transit search, so in some
   * cases we get "silly" transit itineraries that is marginally better on travel-duration
   * compared with a on-street-all-the-way itinerary. Use this method to turn this filter
   * on/off.
   * <p>
   * The filter remove all itineraries with a generalized-cost that is higher than the best
   * on-street-all-the-way itinerary.
   * <p>
   * This filter only have an effect, if an on-street-all-the-way(WALK, BICYCLE, CAR) itinerary
   * exist.
   * <p>
   * This filter is enabled by default.
   */
  default boolean removeTransitWithHigherCostThanBestOnStreetOnly() { return true; }

  /**
   * This will NOT delete itineraries, but tag them as deleted using the
   * {@link Itinerary#systemNotices}.
   * <p>
   * The default value is {@code false}.
   */
  default boolean debug() {
    return false;
  }


  /* inner classes */

  /**
   * Group itineraries by similarity and reduce the number of itineraries down to an approximate
   * minimum number of itineraries for each group.
   */
  class GroupBySimilarity {
    /**
     * The minimum similarity percentage used to group itineraries.
     * <p>
     * The value must be a number between 0.01 (1%) and 0.99 (99%).
     */
    final double groupByP;

    /**
     * Set a guideline for the minimum total number of itineraries to keep. For example, if there is
     * 2 groups and the min-limit is 3, the filter will keep 2 samples in each group, keeping up to
     * 4 itineraries(approximately 3). Set this to 1 to keep ONE itinerary per group.
     */
    final int approximateMinLimit;

    public GroupBySimilarity(double groupByP, int approximateMinLimit) {
      this.groupByP = groupByP;
      this.approximateMinLimit = approximateMinLimit;
    }
  }
}
