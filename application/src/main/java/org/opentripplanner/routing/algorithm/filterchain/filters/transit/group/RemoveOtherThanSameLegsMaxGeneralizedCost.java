package org.opentripplanner.routing.algorithm.filterchain.filters.transit.group;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.framework.filter.GroupByFilter;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * This filter removes itineraries, which use the same trips for most of their legs, but where some
 * itineraries have a much higher cost for the other legs. This is similar to {@link
 * org.opentripplanner.routing.algorithm.filterchain.filters.transit.TransitGeneralizedCostFilter},
 * but is used together with {@link GroupByFilter} to filter within the groups.
 *
 * <h3>Example</h3>
 *
 * Lets give 5% slack: f=1.05
 *
 * <pre>
 * Itin A:  | ### cost of legs common trips - $42 ### | ########## other legs - $41 ########## |    (Total: $83)
 * Itin B:  | ######## cost of legs common trips - $52 ######## | ### other legs - $27 ### |        (Total: $79)
 * </pre>
 *
 * <ul>
 * <li>Min cost common legs: a=$42</li>
 * <li>Min cost all itineraries: b=$79</li>
 * <li>maxLimit = a + (b - a) * f = 42 + 37 * 1.05 = 81</li>
 * <li><b>Result:</b> Keep itinerary A, and drop B ($83 > limit $81)</li>
 * </ul>
 */
public class RemoveOtherThanSameLegsMaxGeneralizedCost implements RemoveItineraryFlagger {

  /**
   * How much higher cost do we allow for the non-shared legs before we filter out the itinerary?
   */
  private final double maxCostOtherLegsFactor;

  public RemoveOtherThanSameLegsMaxGeneralizedCost(double maxCostOtherLegsFactor) {
    if (maxCostOtherLegsFactor < 1.0) {
      throw new IllegalArgumentException("maxCostOtherLegsFactor must be >= 1.0");
    }
    this.maxCostOtherLegsFactor = maxCostOtherLegsFactor;
  }

  @Override
  public String name() {
    return "other-than-same-legs-max-generalized-cost-filter";
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    if (itineraries.size() < 2) {
      return List.of();
    }

    // Get all transit trips for an itinerary
    Function<Itinerary, Set<Trip>> getTripsForItinerary = itinerary ->
      itinerary
        .legs()
        .stream()
        .filter(Leg::isTransitLeg)
        .map(Leg::trip)
        .collect(Collectors.toSet());

    // Find the trips that are shared between all itineraries
    Set<Trip> commonTrips = itineraries
      .stream()
      .map(getTripsForItinerary)
      .reduce((a, b) -> {
        a.retainAll(b);
        return a;
      })
      .get();

    if (commonTrips.isEmpty()) {
      return List.of();
    }

    // Find the lowest cost of the common legs
    int commonLegsCost = itineraries
      .stream()
      .mapToInt(itinerary ->
        itinerary
          .legs()
          .stream()
          .filter(Leg::isTransitLeg)
          .filter(leg -> commonTrips.contains(leg.trip()))
          .mapToInt(leg -> Integer.max(0, leg.generalizedCost()))
          .sum()
      )
      .min()
      .orElseThrow();

    // Find the lowest cost for any itinerary
    int minimumItineraryCost = itineraries
      .stream()
      .mapToInt(it -> it.generalizedCostIncludingPenalty().toSeconds())
      .min()
      .orElseThrow();

    // Leg costs in otp are not guaranteed to be accurate and it is possible that the sum of the
    // commonLegCosts are higher than the minimumItineraryCost. So we need to make sure this does
    // not go negative.
    int otherLegsCost = Integer.max(0, minimumItineraryCost - commonLegsCost);

    // Calculate the maximum limit allowed for itinerary cost
    Cost maxLimit = Cost.costOfSeconds(otherLegsCost * maxCostOtherLegsFactor + commonLegsCost);

    return itineraries
      .stream()
      .filter(it -> it.generalizedCostIncludingPenalty().greaterThan(maxLimit))
      .toList();
  }
}
