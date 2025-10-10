package org.opentripplanner.street.model.edge;

import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleWalkingPreferences;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.street.search.TraverseMode;

public class StreetEdgeReluctanceCalculator {

  /** Utility class, private constructor to prevent instantiation */
  private StreetEdgeReluctanceCalculator() {}

  /**
   * Compute reluctance for a regular street section. Note! This does not apply if in a wheelchair,
   * see {@link #computeWheelchairReluctance(RoutingPreferences, double, boolean, boolean)}.
   */
  static double computeReluctance(
    RoutingPreferences pref,
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

  private static double computeWalkReluctance(WalkPreferences pref, boolean edgeIsStairs) {
    return pref.reluctance() * (edgeIsStairs ? pref.stairsReluctance() : 1);
  }

  private static double computeBikeWalkingReluctance(
    VehicleWalkingPreferences pref,
    boolean edgeIsStairs
  ) {
    return pref.reluctance() * (edgeIsStairs ? pref.stairsReluctance() : 1);
  }

  static double computeWheelchairReluctance(
    RoutingPreferences preferences,
    double maxSlope,
    boolean edgeWheelchairAccessible,
    boolean stairs
  ) {
    var wheelchair = preferences.wheelchair();
    // Add reluctance if street is not wheelchair accessible
    double reluctance = edgeWheelchairAccessible ? 1.0 : wheelchair.inaccessibleStreetReluctance();
    reluctance *= preferences.walk().reluctance();

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

  /**
   * Exaggerate the safety for SAFEST_STREET routing.
   * The effect is to make normal streets not safe, "reasonably safe" streets appear neutral, and
   * "very safe" streets appear safe.
   */
  public static double getSafetyForSafestStreet(double originalSafety) {
    return originalSafety * originalSafety * 2.0;
  }
}
