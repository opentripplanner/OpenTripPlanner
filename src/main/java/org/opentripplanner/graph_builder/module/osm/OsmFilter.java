package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.street.model.StreetTraversalPermission;

/**
 *
 */
public class OsmFilter {

  /**
   * Check OSM tags for various one-way and one-way-by-mode tags and return a pair of permissions
   * for travel along and against the way.
   */
  public static StreetTraversalPermissionPair getPermissions(
    StreetTraversalPermission permissions,
    OSMWay way
  ) {
    StreetTraversalPermission permissionsFront = permissions;
    StreetTraversalPermission permissionsBack = permissions;

    // Check driving direction restrictions.
    if (way.isOneWayForwardDriving() || way.isRoundabout()) {
      permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE_AND_CAR);
    }
    if (way.isOneWayReverseDriving()) {
      permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE_AND_CAR);
    }

    // Check bike direction restrictions.
    if (way.isOneWayForwardBicycle()) {
      permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE);
    }
    if (way.isOneWayReverseBicycle()) {
      permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE);
    }

    // TODO(flamholz): figure out what this is for.
    String oneWayBicycle = way.getTag("oneway:bicycle");
    if (OSMWithTags.isFalse(oneWayBicycle) || way.isTagTrue("bicycle:backwards")) {
      if (permissions.allows(StreetTraversalPermission.BICYCLE)) {
        permissionsFront = permissionsFront.add(StreetTraversalPermission.BICYCLE);
        permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
      }
    }

    //This needs to be after adding permissions for oneway:bicycle=no
    //removes bicycle permission when bicycles need to use sidepath
    //TAG: bicycle:forward=use_sidepath
    if (way.isForwardDirectionSidepath()) {
      permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE);
    }

    //TAG bicycle:backward=use_sidepath
    if (way.isReverseDirectionSidepath()) {
      permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE);
    }

    if (way.isOpposableCycleway()) {
      permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
    }
    return new StreetTraversalPermissionPair(permissionsFront, permissionsBack);
  }

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
