package org.opentripplanner.routing.algorithm.filterchain.filter;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

/**
 * An experimental feature for calculating a numeric score between 0 and 1 which indicates how
 * accessible the itinerary is as a whole. This is not a very scientific method but just a rough
 * guidance that expresses certainty or uncertainty about the accessibility.
 * <p>
 * The intended audience for this score are frontend developers wanting to show a simple UI rather
 * than having to iterate over all the stops and trips.
 * <p>
 * Note: the information to calculate this score are all available to the frontend, however
 * calculating them on the backend makes life a little easier and changes are automatically
 * applied to all frontends.
 */
public class AccessibilityScoreFilter implements ItineraryListFilter {

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries.stream().map(this::addAccessibilityScore).toList();
  }

  private Itinerary addAccessibilityScore(Itinerary i) {
    var scoredLegs = i.legs
      .stream()
      .map(leg -> {
        if (leg instanceof ScheduledTransitLeg transitLeg) {
          return transitLeg.withAccessibilityScore(compute(transitLeg));
        } else return leg;
      })
      .toList();

    i.legs = scoredLegs;
    i.accessibilityScore = compute(scoredLegs);
    return i;
  }

  public static Float compute(List<Leg> legs) {
    return legs
      .stream()
      .map(Leg::accessibilityScore)
      .filter(Objects::nonNull)
      .min(Comparator.comparingDouble(Float::doubleValue))
      .orElse(null);
  }

  public static float compute(ScheduledTransitLeg leg) {
    var fromStop = leg.getFrom().stop.getWheelchairBoarding();
    var toStop = leg.getFrom().stop.getWheelchairBoarding();
    var trip = leg.getTrip().getWheelchairBoarding();

    var values = List.of(trip, fromStop, toStop);
    var sum = (float) values
      .stream()
      .mapToDouble(AccessibilityScoreFilter::accessibilityScore)
      .sum();
    return sum / values.size();
  }

  public static double accessibilityScore(WheelChairBoarding wheelchair) {
    return switch (wheelchair) {
      case NO_INFORMATION -> 0.5;
      case POSSIBLE -> 1;
      case NOT_POSSIBLE -> 0;
    };
  }
}
