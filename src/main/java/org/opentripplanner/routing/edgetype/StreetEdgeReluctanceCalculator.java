package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;

class StreetEdgeReluctanceCalculator {

  /** Utility class, private constructor to prevent instantiation */
  private StreetEdgeReluctanceCalculator() {}

  /**
   * Compute reluctance for a regular street section. Note! This does not apply if in a wheelchair,
   * see {@link #computeWheelchairReluctance(RoutingRequest, double, boolean, boolean)}.
   */
  static double computeReluctance(
    RoutingRequest req,
    TraverseMode traverseMode,
    boolean walkingBike,
    boolean edgeIsStairs
  ) {
    if (edgeIsStairs) {
      return req.stairsReluctance;
    } else {
      return switch (traverseMode) {
        case WALK -> walkingBike ? req.bikeWalkingReluctance : req.walkReluctance;
        case BICYCLE -> req.bikeReluctance;
        case CAR -> req.carReluctance;
        default -> throw new IllegalArgumentException(
          "getReluctance(): Invalid mode " + traverseMode
        );
      };
    }
  }

  static double computeWheelchairReluctance(
    RoutingRequest request,
    double maxSlope,
    boolean edgeWheelchairAccessible,
    boolean stairs
  ) {
    var wheelchair = request.wheelchairAccessibility;
    // Add reluctance if street is not wheelchair accessible
    double reluctance = edgeWheelchairAccessible ? 1.0 : wheelchair.inaccessibleStreetReluctance();
    reluctance *= request.walkReluctance;

    // Add reluctance for stairs
    if (stairs) {
      reluctance *= wheelchair.stairsReluctance();
    }

    // Add reluctance for exceeding the max slope
    double slopeExceededBy = Math.abs(maxSlope) - wheelchair.maxSlope();
    if (slopeExceededBy > 0.0) {
      double slopeExceededReluctance = wheelchair.slopeExceededReluctance();
      if (slopeExceededReluctance > 0.0) {
        // if we exceed the max slope the cost increases multiplied by how much you go over the maxSlope
        reluctance *= 1.0 + (100.0 * slopeExceededBy) * slopeExceededReluctance;
      }
    }
    return reluctance;
  }
}
