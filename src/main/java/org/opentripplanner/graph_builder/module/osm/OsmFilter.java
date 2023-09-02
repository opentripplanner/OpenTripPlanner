package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 *
 */
public class OsmFilter {

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
  static boolean isWayRoutable(OSMWithTags way) {
    if (!(way.isRoutable())) {
      return false;
    }

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
