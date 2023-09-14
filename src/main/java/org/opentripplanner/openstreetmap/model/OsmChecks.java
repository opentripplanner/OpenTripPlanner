package org.opentripplanner.openstreetmap.model;

import java.util.Set;

/**
 * This class contains methods that are shared between {@link OSMWay} and {@link OSMRelation}.
 * <p>
 * These two classes have some things in common but not enough to create their own inheritance chain.
 */
class OsmChecks {

  private static final Set<String> NON_ROUTABLE_HIGHWAYS = Set.of(
    "proposed",
    "planned",
    "construction",
    "razed",
    "raceway",
    "abandoned",
    "historic",
    "no",
    "emergency_bay",
    "rest_area",
    "services",
    "bus_guideway",
    "escape"
  );

  /**
   * Determine whether any mode can or should ever traverse the given way. If not, we leave the way
   * out of the OTP graph. Potentially routable ways are those that have the tags : highway=*
   * public_transport=platform railway=platform
   * <p>
   * But not conveyers, proposed highways/roads or those still under construction, and raceways (as
   * well as ways where all access is specifically forbidden to the public).
   * http://wiki.openstreetmap.org/wiki/Tag:highway%3Dproposed
   * <p>
   * A whitelist for highway tags is an alternative to a blacklist.
   */
  protected static boolean isRoutable(OSMWithTags way) {
    if (way.isOneOfTags("highway", NON_ROUTABLE_HIGHWAYS)) {
      return false;
    }

    if (way.isGeneralAccessDenied()) {
      // There are exceptions.
      return (
        way.isMotorcarExplicitlyAllowed() ||
        way.isBicycleExplicitlyAllowed() ||
        way.isPedestrianExplicitlyAllowed() ||
        way.isMotorVehicleExplicitlyAllowed() ||
        way.isVehicleExplicitlyAllowed()
      );
    }
    return true;
  }
}
