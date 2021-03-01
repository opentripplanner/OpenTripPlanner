package org.opentripplanner.routing.algorithm.filterchain;

import org.opentripplanner.model.base.ToStringBuilder;

/**
 * Group itineraries by similarity and reduce the number of itineraries down to an approximate
 * minimum number of itineraries for each group.
 * <p>
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
 * are skipped. Than for each group of itineraries the itinerary with the lowest cost is kept.
 * All other itineraries are dropped.
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
 *
 * @see ItineraryFilterChainBuilder#addGroupBySimilarity(GroupBySimilarity)
 */
public class GroupBySimilarity {

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

  @Override
  public String toString() {
    return ToStringBuilder.of(GroupBySimilarity.class)
        .addNum("groupByP", groupByP)
        .addNum("approximateMinLimit", approximateMinLimit)
        .toString();
  }
}
