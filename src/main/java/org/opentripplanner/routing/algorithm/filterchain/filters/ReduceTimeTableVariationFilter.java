package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.routing.algorithm.filterchain.groupids.GroupByMainBodyOfLegsIds;


/**
 * This filter remove itineraries with the same main body of legs, but with different
 * options in the beginning or end of the itinerary.
 */
public class ReduceTimeTableVariationFilter extends GroupByFilter<GroupByMainBodyOfLegsIds> {
  public ReduceTimeTableVariationFilter(final int minFirstLastTripDurationInSeconds) {
    super(
        "timetable-reduce-variation-filter",
        it -> new GroupByMainBodyOfLegsIds(it, minFirstLastTripDurationInSeconds),
        new SortOnGeneralizedCost(),
        1
    );
  }
}
