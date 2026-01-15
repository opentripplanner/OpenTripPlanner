package org.opentripplanner.street.model.edge;

import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.VehicleWalkingRequest;
import org.opentripplanner.street.search.request.WalkRequest;

class StreetEdgeReluctanceCalculator {

  /** Utility class, private constructor to prevent instantiation */
  private StreetEdgeReluctanceCalculator() {}

  /**
   * Compute reluctance for a regular street section. Note! This does not apply if in a wheelchair,
   * see {@link #computeWheelchairReluctance(RoutingPreferences, double, boolean, boolean)}.
   */
  static double computeReluctance(
    StreetSearchRequest pref,
    TraverseMode traverseMode,
    boolean walkingBike,
    boolean edgeIsStairs
  ) {
    return switch (traverseMode) {
      case WALK -> walkingBike
        ? computeBikeWalkingReluctance(pref.bike().walking(), edgeIsStairs)
        : computeWalkReluctance(pref.walk(), edgeIsStairs);
      case BICYCLE -> pref.bike().reluctance();
      case CAR -> pref.car().reluctance();
      case SCOOTER -> pref.scooter().reluctance();
      default -> throw new IllegalArgumentException(
        "getReluctance(): Invalid mode " + traverseMode
      );
    };
  }

  private static double computeWalkReluctance(WalkRequest req, boolean edgeIsStairs) {
    return req.reluctance() * (edgeIsStairs ? req.stairsReluctance() : 1);
  }

  private static double computeBikeWalkingReluctance(
    VehicleWalkingRequest pref,
    boolean edgeIsStairs
  ) {
    return pref.reluctance() * (edgeIsStairs ? pref.stairsReluctance() : 1);
  }

  static double computeWheelchairReluctance(
    StreetSearchRequest request,
    double maxSlope,
    boolean edgeWheelchairAccessible,
    boolean stairs
  ) {
    var wheelchair = request.wheelchair();
    // Add reluctance if street is not wheelchair accessible
    double reluctance = edgeWheelchairAccessible ? 1.0 : wheelchair.inaccessibleStreetReluctance();
    reluctance *= request.walk().reluctance();

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
