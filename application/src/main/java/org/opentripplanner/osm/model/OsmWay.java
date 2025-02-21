package org.opentripplanner.osm.model;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.opentripplanner.graph_builder.module.osm.StreetTraversalPermissionPair;
import org.opentripplanner.street.model.StreetTraversalPermission;

public class OsmWay extends OsmEntity {

  private static final Set<String> ESCALATOR_CONVEYING_TAGS = Set.of(
    "yes",
    "forward",
    "backward",
    "reversible"
  );

  private final TLongList nodes = new TLongArrayList();

  public void addNodeRef(long nodeRef) {
    nodes.add(nodeRef);
  }

  public void addNodeRef(long nodeRef, int index) {
    nodes.insert(index, nodeRef);
  }

  public TLongList getNodeRefs() {
    return nodes;
  }

  public String toString() {
    return "osm way " + id;
  }

  /**
   * Returns true if way geometry is a closed loop
   */
  public boolean isClosed() {
    int size = nodes.size();

    if (size > 2) {
      long a = nodes.get(0);
      long b = nodes.get(size - 1);
      return a == b;
    }
    return false;
  }

  /**
   * Returns true if way is both boarding location and closed polygon
   */
  public boolean isBoardingArea() {
    return isBoardingLocation() && isClosed();
  }

  /**
   * Returns true if these are steps.
   */
  public boolean isSteps() {
    return isTag("highway", "steps");
  }

  /**
   * Checks the wheelchair-accessibility of this way. Stairs are by default inaccessible but
   * can be made accessible if they explicitly set wheelchair=true.
   */
  public boolean isWheelchairAccessible() {
    if (isSteps()) {
      return isTagTrue("wheelchair");
    } else {
      return super.isWheelchairAccessible();
    }
  }

  /**
   * Is this way a roundabout?
   */
  public boolean isRoundabout() {
    return "roundabout".equals(getTag("junction"));
  }

  /**
   * Returns true if this is a one-way street for driving.
   */
  public boolean isOneWayForwardDriving() {
    return isTagTrue("oneway");
  }

  /**
   * Returns true if this way is one-way in the opposite direction of its definition.
   */
  public boolean isOneWayReverseDriving() {
    return isTag("oneway", "-1");
  }

  /**
   * Returns true if bikes can only go forward.
   */
  public boolean isOneWayForwardBicycle() {
    String oneWayBicycle = getTag("oneway:bicycle");
    return isTrue(oneWayBicycle) || isTagFalse("bicycle:backwards");
  }

  /**
   * Returns true if bikes can only go in the reverse direction.
   */
  public boolean isOneWayReverseBicycle() {
    String oneWayBicycle = getTag("oneway:bicycle");
    return "-1".equals(oneWayBicycle) || isTagFalse("bicycle:forward");
  }

  /**
   * Some cycleways allow contraflow biking.
   */
  public boolean isOpposableCycleway() {
    // any cycleway which is opposite* allows contraflow biking
    String cycleway = getTag("cycleway");
    String cyclewayLeft = getTag("cycleway:left");
    String cyclewayRight = getTag("cycleway:right");

    return (
      (cycleway != null && cycleway.startsWith("opposite")) ||
      (cyclewayLeft != null && cyclewayLeft.startsWith("opposite")) ||
      (cyclewayRight != null && cyclewayRight.startsWith("opposite"))
    );
  }

  public boolean isEscalator() {
    return (isTag("highway", "steps") && isOneOfTags("conveying", ESCALATOR_CONVEYING_TAGS));
  }

  public Optional<Duration> getDuration(Consumer<String> errorHandler) {
    return getTagValueAsDuration("duration", errorHandler);
  }

  public boolean isForwardEscalator() {
    return isEscalator() && "forward".equals(this.getTag("conveying"));
  }

  public boolean isBackwardEscalator() {
    return isEscalator() && "backward".equals(this.getTag("conveying"));
  }

  /**
   * Returns true if the way is considered an area.
   *
   * An area can be specified as such, or be one by default as an amenity.
   */
  public boolean isRoutableArea() {
    return (
      !isTag("area", "no") &&
      (
        isTag("area", "yes") ||
        isParking() ||
        isBikeParking() ||
        isBoardingArea() ||
        isIndoorRoutable()
      ) &&
      getNodeRefs().size() > 2
    );
  }

  /**
   * Given a set of {@code permissions} check if it can really be applied to both directions
   * of the way and return the permissions for both cases.
   */
  public StreetTraversalPermissionPair splitPermissions(StreetTraversalPermission permissions) {
    StreetTraversalPermission permissionsFront = permissions;
    StreetTraversalPermission permissionsBack = permissions;

    // Check driving direction restrictions.
    if (isOneWayForwardDriving() || isRoundabout()) {
      permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE_AND_CAR);
    }
    if (isOneWayReverseDriving()) {
      permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE_AND_CAR);
    }

    // Check bike direction restrictions.
    if (isOneWayForwardBicycle()) {
      permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE);
    }
    if (isOneWayReverseBicycle()) {
      permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE);
    }

    // TODO(flamholz): figure out what this is for.
    String oneWayBicycle = getTag("oneway:bicycle");
    if (isFalse(oneWayBicycle) || isTagTrue("bicycle:backwards")) {
      if (permissions.allows(StreetTraversalPermission.BICYCLE)) {
        permissionsFront = permissionsFront.add(StreetTraversalPermission.BICYCLE);
        permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
      }
    }

    if (isOpposableCycleway()) {
      permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
    }
    return new StreetTraversalPermissionPair(permissionsFront, permissionsBack);
  }

  @Override
  public String url() {
    return String.format("https://www.openstreetmap.org/way/%d", getId());
  }
}
