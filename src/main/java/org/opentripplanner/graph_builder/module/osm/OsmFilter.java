package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.ConflictingBikeTags;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.street.model.StreetTraversalPermission;

/**
 *
 */
public class OsmFilter {

  public static StreetTraversalPermission getPermissionsForEntity(
    OSMWithTags entity,
    StreetTraversalPermission def
  ) {
    StreetTraversalPermission permission = null;

    /*
     * Only a few tags are examined here, because we only care about modes supported by OTP
     * (wheelchairs are not of concern here)
     *
     * Only a few values are checked for, all other values are presumed to be permissive (=>
     * This may not be perfect, but is closer to reality, since most people don't follow the
     * rules perfectly ;-)
     */
    if (entity.isGeneralAccessDenied()) {
      // this can actually be overridden
      permission = StreetTraversalPermission.NONE;
    } else {
      permission = def;
    }

    if (entity.isVehicleExplicitlyDenied()) {
      permission = permission.remove(StreetTraversalPermission.BICYCLE_AND_CAR);
    } else if (entity.isVehicleExplicitlyAllowed()) {
      permission = permission.add(StreetTraversalPermission.BICYCLE_AND_CAR);
    }

    if (entity.isMotorcarExplicitlyDenied() || entity.isMotorVehicleExplicitlyDenied()) {
      permission = permission.remove(StreetTraversalPermission.CAR);
    } else if (entity.isMotorcarExplicitlyAllowed() || entity.isMotorVehicleExplicitlyAllowed()) {
      permission = permission.add(StreetTraversalPermission.CAR);
    }

    if (entity.isBicycleExplicitlyDenied()) {
      permission = permission.remove(StreetTraversalPermission.BICYCLE);
    } else if (entity.isBicycleExplicitlyAllowed()) {
      permission = permission.add(StreetTraversalPermission.BICYCLE);
    }

    if (entity.isPedestrianExplicitlyDenied()) {
      permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
    } else if (entity.isPedestrianExplicitlyAllowed()) {
      permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
    }

    if (entity.isUnderConstruction()) {
      permission = StreetTraversalPermission.NONE;
    }

    if (permission == null) {
      return def;
    }
    return permission;
  }

  /**
   * Computes permissions for an OSMWay.
   */
  public static StreetTraversalPermission getPermissionsForWay(
    OSMWay way,
    StreetTraversalPermission def,
    DataImportIssueStore issueStore
  ) {
    StreetTraversalPermission permissions = getPermissionsForEntity(way, def);

    /*
     * pedestrian rules: everything is two-way (assuming pedestrians are allowed at all) bicycle
     * rules: default: permissions;
     *
     * cycleway=dismount means walk your bike -- the engine will automatically try walking bikes
     * any time it is forbidden to ride them, so the only thing to do here is to remove bike
     * permissions
     *
     * oneway=... sets permissions for cars and bikes oneway:bicycle overwrites these
     * permissions for bikes only
     *
     * now, cycleway=opposite_lane, opposite, opposite_track can allow once oneway has been set
     * by oneway:bicycle, but should give a warning if it conflicts with oneway:bicycle
     *
     * bicycle:backward=yes works like oneway:bicycle=no bicycle:backwards=no works like
     * oneway:bicycle=yes
     */

    // Compute pedestrian permissions.
    if (way.isPedestrianExplicitlyAllowed()) {
      permissions = permissions.add(StreetTraversalPermission.PEDESTRIAN);
    } else if (way.isPedestrianExplicitlyDenied()) {
      permissions = permissions.remove(StreetTraversalPermission.PEDESTRIAN);
    }

    // Compute bike permissions, check consistency.
    boolean forceBikes = false;
    if (way.isBicycleExplicitlyAllowed()) {
      permissions = permissions.add(StreetTraversalPermission.BICYCLE);
      forceBikes = true;
    }

    if (way.isBicycleDismountForced()) {
      permissions = permissions.remove(StreetTraversalPermission.BICYCLE);
      if (forceBikes) {
        issueStore.add(new ConflictingBikeTags(way));
      }
    }

    return permissions;
  }

  public static StreetTraversalPermission getPermissionsForWay(
    OSMWay way,
    StreetTraversalPermission def
  ) {
    return getPermissionsForWay(way, def, DataImportIssueStore.NOOP);
  }

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
