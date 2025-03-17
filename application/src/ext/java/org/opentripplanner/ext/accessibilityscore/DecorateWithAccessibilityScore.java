package org.opentripplanner.ext.accessibilityscore;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.WheelchairTraversalInformation;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.utils.tostring.ToStringBuilder;

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
public class DecorateWithAccessibilityScore implements ItineraryDecorator {

  private final double wheelchairMaxSlope;

  public DecorateWithAccessibilityScore(double wheelchairMaxSlope) {
    this.wheelchairMaxSlope = wheelchairMaxSlope;
  }

  @Override
  public Itinerary decorate(Itinerary itinerary) {
    return addAccessibilityScore(itinerary);
  }

  private Itinerary addAccessibilityScore(Itinerary i) {
    var builder = i.copyOf();
    var legs = builder
      .legs()
      .stream()
      .map(leg -> {
        if (leg instanceof ScheduledTransitLeg transitLeg) {
          return transitLeg.copy().withAccessibilityScore(compute(transitLeg)).build();
        } else if (leg instanceof StreetLeg streetLeg && leg.isWalkingLeg()) {
          return streetLeg.withAccessibilityScore(compute(streetLeg));
        } else {
          return leg;
        }
      })
      .toList();
    builder.withLegs(ignore -> legs);
    if (i.isWalkOnly() || i.hasTransit()) {
      builder.withAccessibilityScore(compute(legs));
    }
    return builder.build();
  }

  private static float compute(ScheduledTransitLeg leg) {
    var fromStop = leg.getFrom().stop.getWheelchairAccessibility();
    var toStop = leg.getTo().stop.getWheelchairAccessibility();
    var trip = leg.getTripWheelchairAccessibility();

    var values = List.of(trip, fromStop, toStop);
    var sum = (float) values
      .stream()
      .mapToDouble(DecorateWithAccessibilityScore::accessibilityScore)
      .sum();
    return sum / values.size();
  }

  private static Float compute(List<Leg> legs) {
    return legs
      .stream()
      .map(Leg::accessibilityScore)
      .filter(Objects::nonNull)
      .min(Comparator.comparingDouble(Float::doubleValue))
      .orElse(null);
  }

  private static double accessibilityScore(Accessibility wheelchair) {
    return switch (wheelchair) {
      case NO_INFORMATION -> 0.5;
      case POSSIBLE -> 1;
      case NOT_POSSIBLE -> 0;
    };
  }

  private float compute(StreetLeg leg) {
    var edges = leg.getWalkSteps().stream().map(WalkStep::getEdges).toList();
    var streetEdges = edges
      .stream()
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

    boolean allEdgesAreAccessible = edges
      .stream()
      .filter(WheelchairTraversalInformation.class::isInstance)
      .map(WheelchairTraversalInformation.class::cast)
      .allMatch(WheelchairTraversalInformation::isWheelchairAccessible);
    if (allEdgesAreAccessible) {
      score += 0.5f;
    }

    return Math.max(score, 0);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass())
      .addNum("wheelchairMaxSlope", wheelchairMaxSlope)
      .toString();
  }
}
