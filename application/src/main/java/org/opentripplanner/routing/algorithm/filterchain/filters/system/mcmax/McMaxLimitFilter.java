package org.opentripplanner.routing.algorithm.filterchain.filters.system.mcmax;

import java.util.List;
import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.SingleCriteriaComparator;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * This filter is used to reduce a set of itineraries down to the specified limit, if possible.
 * The filter is guaranteed to keep at least the given {@code minNumItineraries} and/or the best
 * itinerary for each criterion. The criterion is defined using the list of {@code comparators}.
 * <p>
 * The main usage of this filter is to combine it with a transit grouping filter and for each group
 * make sure there is at least {@code minNumItineraries} and that the best itinerary with respect
 * to each criterion is kept. So, if the grouping is based on time and riding common trips, then
 * this filter will use the remaining criterion (transfers, generalized-cost,
 * [transit-group-priority]) to filter the grouped set of itineraries. DO NOT INCLUDE CRITERIA
 * USED TO GROUP THE ITINERARIES, ONLY THE REMAINING CRITERION USED IN THE RAPTOR SEARCH.
 * <p>
 * <b>IMPLEMENTATION DETAILS</b>
 * <p>
 * This is not a trivial problem. In most cases, the best itinerary for a given criteria is unique,
 * but there might be ties - same number of transfers, same cost, and/or different priority groups.
 * In case of a tie, we will look if an itinerary is "best-in-group" for more than one criterion,
 * if so we pick the one which is best in the highest number of groups. Again, if there is a tie
 * (best in the same number of groups), then we fall back to the given itinerary sorting order.
 * <p>
 * This filter will use the order of the input itineraries to break ties. So, make sure to call the
 * appropriate sort function before this filter is invoked.
 * <p>
 * Note! For criteria like num-of-transfers or generalized-cost, there is only one set of "best"
 * itineraries, and usually there are only one or a few itineraries. In case there is more than one,
 * picking just one is fine. But, for transit-group-priority there might be more than one optimal
 * set of itineraries. For each set, we need to pick one itinerary for the final result. Each of
 * these sets may or may not have more than one itinerary. If you group by agency, then there will
 * be at least one itinerary for each agency present in the result (simplified, an itinerary may
 * consist of legs with different agencies). The transit-group-priority pareto-function used by
 * Raptor is reused, so we do not need to worry about the logic here.
 * <p>
 * Let's discuss an example (this example also exists as a unit-test case):
 * <pre>
 *   minNumItineraries = 4
 *   comparators = [ generalized-cost, min-num-transfers, transit-group-priority ]
 *   itineraries: [
 *    #0 : [ 1000, 2, (a) ]
 *    #1 : [ 1000, 3, (a,b) ]
 *    #2 : [ 1000, 3, (b) ]
 *    #3 : [ 1200, 1, (a,b) ]
 *    #4 : [ 1200, 1, (a) ]
 *    #5 : [ 1300, 2, (c) ]
 *    #6 : [ 1300, 3, (c) ]
 *   ]
 * </pre>
 * The best itineraries by generalized-cost are (#0, #1, #2). The best itineraries by
 * min-num-transfers are (#3, #4). The best itineraries by transit-group-priority are
 * (a:(#0, #4), b:(#2), c:(#5, #6)).
 * <p>
 * So we need to pick one from each group (#0, #1, #2), (#3, #4), (#0, #4), (#2), and (#5, #6).
 * Since #2 is a single, we pick it first. Itinerary #2 is also one of the best
 * generalized-cost itineraries - so we are done with generalized-cost itineraries as well. The two
 * groups left are (#3, #4), (#0, #4), and (#5, #6). #4 exists in 2 groups, so we pick it next. Now
 * we are left with (#5, #6). To break the tie, we look at the sort-order. We pick
 * itinerary #5. Result: #2, #4, and #5.
 * <p>
 * The `minNumItineraries` limit is not met, so we need to pick another itinerary, we use the
 * sort-order again and add itinerary #0. The result returned is: [#0, #2, #4, #5]
 */
public class McMaxLimitFilter implements RemoveItineraryFlagger {

  private final String name;
  private final int minNumItineraries;
  private final List<SingleCriteriaComparator> comparators;

  public McMaxLimitFilter(
    String name,
    int minNumItineraries,
    List<SingleCriteriaComparator> comparators
  ) {
    this.name = name;
    this.minNumItineraries = minNumItineraries;
    this.comparators = comparators;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    if (itineraries.size() <= minNumItineraries) {
      return List.of();
    }
    var state = new State(itineraries, comparators);
    state.findAllSingleItemGroupsAndAddTheItemToTheResult();
    state.findTheBestItemsUntilAllGroupsAreRepresentedInTheResult();
    state.fillUpTheResultWithMinimumNumberOfItineraries(minNumItineraries);

    // We now have the itineraries we want, but we must invert this and return the
    // list of itineraries to drop - keeping the original order
    var ok = state.getResult();
    return itineraries.stream().filter(Predicate.not(ok::contains)).toList();
  }
}
