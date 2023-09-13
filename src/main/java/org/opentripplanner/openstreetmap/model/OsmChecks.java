package org.opentripplanner.openstreetmap.model;

/**
 * This class contains methods that are shared between {@link OSMWay} and {@link OSMRelation}.
 * <p>
 * These two classes have some things in common but not enough to create their own inheritance chain.
 */
class OsmChecks {

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
    String highway = way.getTag("highway");
    if (highway != null) {
      if (
        highway.equals("proposed") ||
        highway.equals("planned") ||
        highway.equals("construction") ||
        highway.equals("razed") ||
        highway.equals("raceway") ||
        highway.equals("abandoned") ||
        highway.equals("historic") ||
        highway.equals("no") ||
        highway.equals("emergency_bay") ||
        highway.equals("rest_area") ||
        highway.equals("services") ||
        highway.equals("bus_guideway") ||
        highway.equals("escape")
      ) {
        return false;
      }
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
