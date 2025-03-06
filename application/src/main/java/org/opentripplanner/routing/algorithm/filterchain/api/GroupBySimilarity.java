package org.opentripplanner.routing.algorithm.filterchain.api;

import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChainBuilder;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Group itineraries by similarity and reduce the number of itineraries down to an given maximum
 * number of itineraries per group.
 * <p>
 * Group itineraries by the main legs and keeping at most the given total number of itineraries. The
 * itineraries are grouped by the legs that account for more then 'p' % for the total distance.
 * <p/>
 * If the time-table-view is enabled, the result may contain similar itineraries where only the
 * first and/or last legs are different. This can happen by walking to/from another stop, saving
 * time, but getting a higher generalized-cost; Or, by taking a short ride. Use {@code groupByP} in
 * the range {@code 0.80-0.90} and {@code approximateMinLimit=1} will remove these itineraries and
 * keep only the itineraries with the lowest generalized-cost.
 * <p>
 * When this filter is enabled, itineraries are grouped by the "main" transit legs. Short legs are
 * skipped. Then for each group of itineraries the itinerary with the lowest cost is kept. All other
 * itineraries are dropped.
 * <p>
 * A good way to allow for some variation is to include several entries, relaxing the min-limit,
 * while tightening the group-by-p criteria. For example:
 * <pre>
 * groupByP | minLimit | Description
 *   0.90   |    1     | Keep 1 itinerary where 90% of the legs are the same
 *   0.80   |    2     | Keep 2 itineraries where 80% of the legs are the same
 *   0.68   |    3     | Keep 3 itineraries where 68% of the legs are the same
 * </pre>
 * Normally, we want some variation, so a good value to use for this parameter is the combined cost
 * of board- and alight-cost including indirect cost from board- and alight-slack.
 *
 * @see ItineraryListFilterChainBuilder#addGroupBySimilarity(GroupBySimilarity)
 */
public class GroupBySimilarity {

  /**
   * The minimum similarity percentage used to group itineraries.
   * <p>
   * The value must be a number between 0.01 (1%) and 0.99 (99%).
   */
  public final double groupByP;

  /**
   * Set a maximum number of itineraries to keep for each group.
   */
  public final int maxNumOfItinerariesPerGroup;

  /**
   * Should the grouped itineraries be further grouped based on the boarding and alighting stations
   * or stops of all the legs in the itinerary, so that only one itinerary that has the same
   * combination of stations/stops is shown.
   */
  public final boolean nestedGroupingByAllSameStations;

  /**
   * Remove all itineraries whose cost for the non-grouped legs is at least this much higher
   * compared to the lowest cost in the group.
   */
  public final double maxCostOtherLegsFactor;

  private GroupBySimilarity(
    double groupByP,
    int maxNumOfItinerariesPerGroup,
    boolean nestedGroupingByAllSameStations,
    double maxCostOtherLegsFactor
  ) {
    this.groupByP = groupByP;
    this.maxNumOfItinerariesPerGroup = maxNumOfItinerariesPerGroup;
    this.nestedGroupingByAllSameStations = nestedGroupingByAllSameStations;
    this.maxCostOtherLegsFactor = maxCostOtherLegsFactor;
  }

  /**
   * Get a GroupBySimilarity configured to return one itinerary per group.
   */
  public static GroupBySimilarity createWithOneItineraryPerGroup(double groupByP) {
    return new GroupBySimilarity(groupByP, 1, false, 0.0);
  }

  /**
   * Get a GroupBySimilarity configured to return more than one itinerary per group. Additional
   * filtering is configured with nestedGroupingByAllSameStations and maxCostOtherLegsFactor
   * parameters,
   */
  public static GroupBySimilarity createWithMoreThanOneItineraryPerGroup(
    double groupByP,
    int maxNumOfItinerariesPerGroup,
    boolean nestedGroupingByAllSameStations,
    double maxCostOtherLegsFactor
  ) {
    return new GroupBySimilarity(
      groupByP,
      maxNumOfItinerariesPerGroup,
      nestedGroupingByAllSameStations,
      maxCostOtherLegsFactor
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(GroupBySimilarity.class)
      .addNum("groupByP", groupByP)
      .addNum("maxNumOfItinerariesPerGroup", maxNumOfItinerariesPerGroup)
      .addBoolIfTrue("nestedGroupingByAllSameStations", nestedGroupingByAllSameStations)
      .addNum("maxCostOtherLegsFactor", maxCostOtherLegsFactor)
      .toString();
  }
}
