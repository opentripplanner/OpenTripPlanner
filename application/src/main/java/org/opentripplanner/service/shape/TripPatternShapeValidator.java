package org.opentripplanner.service.shape;

import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.transit.model.network.TripPattern;

public final class TripPatternShapeValidator {

  private TripPatternShapeValidator() {}

  /**
   * Check that the pattern has a shape, and that it has one hop for each pair of consecutive stops
   * of the pattern.
   *
   * @throws IllegalArgumentException if the shape is missing, or if the number of hops does not
   *                                  match the pattern
   */
  public static void validate(TripPattern pattern, CompactLineStringSequence tripPatternShape) {
    if (tripPatternShape == null) {
      throw new IllegalArgumentException("No shape for trip pattern " + pattern.getId());
    }
    int expectedHops = Math.max(pattern.numberOfStops() - 1, 0);
    if (tripPatternShape.size() != expectedHops) {
      throw new IllegalArgumentException(
        "Shape of trip pattern %s has %d hops, expected %d".formatted(
          pattern.getId(),
          tripPatternShape.size(),
          expectedHops
        )
      );
    }
  }
}
