package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.routing.algorithm.filterchain.groupids.GroupByLongestLegsId;

public class GroupByLegDistanceFilter extends GroupByFilter<GroupByLongestLegsId> {


  /**
   * Group itineraries by the longest legs posing a given percentage of the total travel distance,
   * then reduce each group based on generalized-cost. The number of itineraries kept in each group
   * depend on the desired min-limit. If the min-limit is 10, and the number of groups is 5, then
   * each group is reduced to 2 elements.
   *
   * @see GroupByFilter
   */
public GroupByLegDistanceFilter(double groupByP, int minLimit, boolean arriveBy) {
    super(
        "groupBy-legs-filter-on-cost",
        it -> new GroupByLongestLegsId(it, groupByP),
        new SortOnGeneralizedCost(),
        minLimit
    );
  }
}
