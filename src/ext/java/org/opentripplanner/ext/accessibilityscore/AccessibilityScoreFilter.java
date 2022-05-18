package org.opentripplanner.ext.accessibilityscore;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.transit.model.basic.WheelchairAccessibility;

/**
 * An experimental feature for calculating a numeric score between 0 and 1 which indicates how
 * accessible the itinerary is as a whole. This is not a very scientific method but just a rough
 * guidance that expresses certainty or uncertainty about the accessibility.
 * <p>
 * The intended audience for this score are frontend developers wanting to show a simple UI rather
 * than having to iterate over all the stops and trips.
 * <p>
 * Note: the information to calculate this score are all available to the frontend, however
 * calculating them on the backend makes life a little easier and changes are automatically applied
 * to all frontends.
 */
public record AccessibilityScoreFilter(float wheelchairMaxSlope) implements ItineraryListFilter {
  public static Float compute(List<Leg> legs) {
    return legs
      .stream()
      .map(Leg::accessibilityScore)
      .filter(Objects::nonNull)
      .min(Comparator.comparingDouble(Float::doubleValue))
      .orElse(null);
  }

  public static float compute(ScheduledTransitLeg leg) {
    var fromStop = leg.getFrom().stop.getWheelchairAccessibility();
    var toStop = leg.getFrom().stop.getWheelchairAccessibility();
    var trip = leg.getTrip().getWheelchairBoarding();

    var values = List.of(trip, fromStop, toStop);
    var sum = (float) values
      .stream()
      .mapToDouble(AccessibilityScoreFilter::accessibilityScore)
      .sum();
    return sum / values.size();
  }

  public static double accessibilityScore(WheelchairBoarding wheelchair) {
    return switch (wheelchair) {
      case NO_INFORMATION -> 0.5;
      case POSSIBLE -> 1;
      case NOT_POSSIBLE -> 0;
    };
  }

  public float compute(StreetLeg leg) {
    var streetEdges = leg
      .getWalkSteps()
      .stream()
      .map(s -> s.edges)
      .filter(StreetEdge.class::isInstance)
      .map(StreetEdge.class::cast)
      .toList();

    var maxSlope = streetEdges
      .stream()
      .filter(StreetEdge::hasElevationExtension)
      .mapToDouble(StreetEdge::getMaxSlope)
      .max()
      .orElse(0);

    float score = 0;

    // calculate the worst percentage we go over the max slope
    // max slope is always above 0
    double maxSlopeExceeded = streetEdges
      .stream()
      .filter(s -> s.getMaxSlope() > maxSlope)
      .mapToDouble(s -> s.getMaxSlope() - maxSlope)
      .map(d -> d * 100)
      .max()
      .orElse(0);

    // for every percent of being over the max slope we decrease the score quadratically
    // so 2 percent over the max slope is 4 times as bad as being 1 percent over.
    // this quickly degrades to 0: being 3 degrees over the max slope can at best give you
    // a score 0.1, everything worse will give you a score of 0!
    double slopeMalus = (maxSlopeExceeded * maxSlopeExceeded) / 10;

    score += (0.5 - slopeMalus);

    boolean allEdgesAreAccessible = streetEdges
      .stream()
      .allMatch(StreetEdge::isWheelchairAccessible);
    if (allEdgesAreAccessible) {
      score += 0.5f;
    }


    return Math.max(score, 0);
  }
  public static double accessibilityScore(WheelchairAccessibility wheelchair) {
    return switch (wheelchair) {
      case NO_INFORMATION -> 0.5;
      case POSSIBLE -> 1;
      case NOT_POSSIBLE -> 0;
    };
  }

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
        } else if (leg instanceof StreetLeg streetLeg) {
          return streetLeg.withAccessibilityScore(compute(streetLeg));
        } else {
          return leg;
        }
      })
      .toList();

    i.legs = scoredLegs;
    i.accessibilityScore = compute(scoredLegs);
    return i;
  }
}
