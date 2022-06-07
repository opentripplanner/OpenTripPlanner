package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

public abstract class StreetCostEdge extends Edge {

  public StreetCostEdge(Vertex fromv, Vertex tov) {
    super(fromv, tov);
  }

  abstract boolean isStairs();

  abstract double getMaxSlope();

  abstract boolean isWheelchairAccessible();

  protected double addWheelchairCost(double cost, WheelchairAccessibilityRequest wheelchair) {
    var slopeExceededBy = Math.abs(getMaxSlope()) - wheelchair.maxSlope();

    if (slopeExceededBy > 0.00001) {
      double reluctance = wheelchair.slopeExceededReluctance();
      if (reluctance > 0) {
        // if we exceed the max slope the cost increases multiplied by how much you go over the maxSlope
        var excessMultiplier = 1 + slopeExceededBy * reluctance;
        cost *= excessMultiplier;
      }
    }

    if (!this.isWheelchairAccessible()) {
      cost *= wheelchair.inaccessibleStreetReluctance();
    }
    return cost;
  }

  protected double computeReluctance(
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
}
