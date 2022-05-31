package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;

public interface ReluctanceEdge {
  boolean isStairs();

  /**
   * The reluctance for a specific traverse mode.
   */
  private double getDefaultReluctance(RoutingRequest req, TraverseMode mode, boolean walkingBike) {
    return switch (mode) {
      case WALK -> walkingBike ? req.bikeWalkingReluctance : req.walkReluctance;
      case BICYCLE -> req.bikeReluctance;
      case CAR -> req.carReluctance;
      default -> throw new IllegalArgumentException("getReluctance(): Invalid mode " + mode);
    };
  }

  default double computeReluctance(
    RoutingRequest req,
    TraverseMode traverseMode,
    boolean walkingBike
  ) {
    if (isStairs() && req.wheelchairAccessibility.enabled()) {
      return req.wheelchairAccessibility.stairsReluctance();
    } else if (isStairs()) {
      return req.stairsReluctance;
    } else {
      return getDefaultReluctance(req, traverseMode, walkingBike);
    }
  }
}
